/*
 * Copyright 2022 Ifropc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package io.github.ifropc.kotomo.area

import io.github.ifropc.kotomo.ocr.Rectangle
import io.github.ifropc.kotomo.util.ImageUtil.createWhiteImage
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Util.scale
import mu.KotlinLogging
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.pow

private val log = KotlinLogging.logger {  }

/**
 * Finds areas by scanning the image for groups of touching pixels
 */
class FindAreas(task: AreaTask?) : AreaStep(task, "touching", "background", "areas") {
    private var width = 0
    private var height = 0
    private lateinit var image: Array<BooleanArray>
    private lateinit var visited: Array<BooleanArray>
    private lateinit var background: Array<BooleanArray>
    private var areaMinX = 0
    private var areaMinY = 0
    private var areaMaxX = 0
    private var areaMaxY = 0
    private var debugImage: BufferedImage? = null
    private var debugGraphics: Graphics2D? = null

    override fun runImpl() {
        findAreas()
        removeDitherAreas1()
        removeDitherAreas2()
        removeTinyAreas()
    }

    /**
     * Finds all connected areas.
     */
    private fun findAreas() {
        width = task!!.width
        height = task!!.height
        image = task!!.binaryImage
        visited = Array(width) { BooleanArray(height) }
        background = Array(width) { BooleanArray(height) }
        task!!.areas = ArrayList()
        if (addDebugImages) {
            debugImage = createWhiteImage(width, height)
            debugGraphics = debugImage!!.createGraphics()
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (image[x][y]) {
                    // mark all black pixels as belonging to background by default,
                    // pixels that belong to areas are removed from background in findArea
                    background[x][y] = true
                } else {
                    // mark all white pixels as done
                    visited[x][y] = true
                }
            }
        }

        // find touching areas containing black pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!visited[x][y]) {
                    findArea(x, y)
                }
            }
        }
        task!!.backgroundImage = background
    }

    /**
     * Finds a new area starting from initX, initY
     */
    private fun findArea(initX: Int, initY: Int) {
        if (addDebugImages) {
            debugGraphics!!.paint = randomColor
        }
        areaMinX = initX
        areaMinY = initY
        areaMaxX = initX
        areaMaxY = initY
        var touchesBorders = false
        /** Pixels to be checked  */
        val todo: Queue<Pixel> = LinkedBlockingQueue()
        /** Confirmed pixels in single continous area  */
        val pixels: MutableList<Pixel> = ArrayList()
        todo.add(Pixel(initX, initY))
        while (!todo.isEmpty()) {
            val p = todo.remove()
            if (visited[p.x][p.y]) continue
            visited[p.x][p.y] = true
            if (task!!.getPixel(p.x, p.y)) {
                if (addDebugImages) {
                    debugGraphics!!.fillRect(p.x, p.y, 1, 1)
                }
                if (p.x <= 0 || p.x >= width - 1 || p.y <= 0 || p.y >= height - 1 ||
                    task!!.getBorderPixel(p.x, p.y)
                ) {
                    touchesBorders = true
                }
                pixels.add(p)
                if (p.x < areaMinX) areaMinX = p.x
                if (p.y < areaMinY) areaMinY = p.y
                if (p.x > areaMaxX) areaMaxX = p.x
                if (p.y > areaMaxY) areaMaxY = p.y
                for (newY in p.y - 1..p.y + 1) {
                    for (newX in p.x - 1..p.x + 1) {
                        if (newX < 0 || newX >= width || newY < 0 || newY >= height) {
                            continue
                        }
                        if (visited[newX][newY]) {
                            continue
                        }
                        todo.add(Pixel(newX, newY))
                    }
                }
            }
        }
        if (touchesBorders) {
            return
        }
        val width = areaMaxX - areaMinX + 1
        val height = areaMaxY - areaMinY + 1
        val rect = Rectangle(areaMinX, areaMinY, width, height)
        val area = Area(rect, pixels.size)
        area.sourceAreas.add(area)

        // TODO relative to resolution
        if (height > 120 || width > 120) {
            return
        }

        // TODO find worst case ratio for valid characters			
        val ratio = 1.0f * pixels.size / (height * width)
        if (area.size > 300 && ratio < 0.09f) {
            return
        }

        // reject area if it represents a speech bubble
        if (isSpeechBubble(area)) {
            return
        }

        // calculate area's minimum rgb value
        var minRGB = 255
        for (p in pixels) {
            val rgb = task!!.getPixelRGB(p.x, p.y)
            if (rgb < minRGB) {
                minRGB = rgb
            }
        }
        //		if (minRGB > Parameters.pixelRGBThreshold) {
//			return;
//		}
        area.minRGB = minRGB

        // remove area's pixels from background
        for (p in pixels) {
            background[p.x][p.y] = false
        }
        task!!.areas!!.add(area)
        log.debug { "area:" + area + " rgb:" + area.minRGB }
    }

    private inner class Pixel(var x: Int, var y: Int)

    /**
     * Checks if area represents a speech bubble
     */
    private fun isSpeechBubble(area: Area): Boolean {

        // speech bubbles should be filtered because they might confuse column detector
        // this check is only needed for small bubbles that enclose a few characters,
        // large bubbles are rejected because their pixel/area ratio is too low 
        if (area.width < 80 || area.height < 80) {
            // areas smaller than these are likely to represent single characters
            return false
        }

        // ellipse parameters
        val size = 0.88f
        val a2 = (1.0f * area.width / 2 * size).toDouble().pow(2.0).toFloat()
        val b2 = (1.0f * area.height / 2 * size).toDouble().pow(2.0).toFloat()
        val centerX = area.midpoint.x
        val centerY = area.midpoint.y

        // count pixels that are outside the ellipse
        var pixels = 0
        for (y in area.y..area.maxY) {
            for (x in area.x..area.maxX) {
                val value = (1.0f * (x - centerX).toDouble().pow(2.0) / a2 +
                        1.0f * (y - centerY).toDouble().pow(2.0) / b2).toFloat()
                if (value <= 1) {
                    // inside ellipse
                    continue
                }
                if (image[x][y]) {
                    ++pixels
                }
            }
        }

        // reject area if most of the pixels are located outside the ellipse
        val ratio = 1.0f * pixels / area.pixels
        return ratio > 0.92f
    }

    /**
     * Removes areas that have less than MIN_PIXELS_AREA pixels
     */
    private fun removeTinyAreas() {

        // these are not included in the background so that they
        // don't confuse column detection
        val i = task!!.areas!!.iterator()
        while (i.hasNext()) {
            val area = i.next()
            if (area.pixels < MIN_PIXELS_AREA) {
                i.remove()
                // actual pixels should not be removed since they often contains 
                // radical fragments in small resolution images
            }
        }
    }

    /**
     * Removes areas that contain many unconnected individual pixels,
     * most likely representing dither patterns in the background
     */
    private fun removeDitherAreas1() {
        for (area in task!!.areas!!) {
            if (area.pixels <= 15) {
                continue
            }
            val neighbourCounts = IntArray(5)
            for (x in area.x..area.maxX) {
                for (y in area.y..area.maxY) {
                    if (!task!!.getPixel(x, y)) continue
                    var neighbours = 0
                    if (task!!.getPixel(x - 1, y)) ++neighbours
                    if (task!!.getPixel(x + 1, y)) ++neighbours
                    if (task!!.getPixel(x, y - 1)) ++neighbours
                    if (task!!.getPixel(x, y + 1)) ++neighbours
                    neighbourCounts[neighbours]++
                }
            }
            if (neighbourCounts[0] > area.pixels / 2) {
                area.remove = true
                continue
            }
            val score = (neighbourCounts[0] * 2f + neighbourCounts[1]) / area.pixels
            val rgbQuality = 1.0f * area.minRGB / Parameters.pixelRGBThreshold
            val rgbFactor = scale(rgbQuality, 0.5f, 0.7f, 1.0f, 0.6f)
            //			float pixelsFactor = Util.scale(area.pixels, 6, 15, 1.25f, 0.8f);
            val threshold = 1.3f * rgbFactor

//			if (area.x == 28 && area.y == 14) {
//				System.err.println("area      :"+area);
//				System.err.println("0         :"+neighbourCounts[0]);
//				System.err.println("1         :"+neighbourCounts[1]);
//				System.err.println("score     :"+score);
//				System.err.println("minRGB    :"+area.getMinRGB());
//				log.debug { "rgbQuality:"+rgbQuality+"\t("+rgbFactor+")" } ;
//				log.debug { "pixels    :"+area.pixels+"\t("+pixelsFactor+")" } ;
//				System.err.println("threshold :"+threshold);
//			}
            if (score >= threshold) {
                //markedAreas.add(area);
                area.remove = true
                continue
            }
        }
        val i = task!!.areas!!.iterator()
        while (i.hasNext()) {
            val area = i.next()
            if (area.remove) {
                i.remove()
            }
        }
    }

    /**
     * Removes regions that contain many unconnected small areas in close proximity,
     * most likely representing dither patterns in the background
     */
    private fun removeDitherAreas2() {

        // this version finds small dense areas close together
        val index = RTree(image, task!!.areas)

        // TODO scale with resolution
        val probeSize = 80
        val probeOverlap = 8
        var x = 0
        while (x < width) {
            var y = 0
            while (y < height) {
                val probe = Rectangle(x, y, probeSize, probeSize)
                val smallAreas: MutableList<Area?> = ArrayList()
                for (area in index[probe]) {
                    // TODO scale with resolution
                    if (area.pixels <= 6 && area.ratio > 0.6f) {
                        smallAreas.add(area)
                    }
                }
                if (smallAreas.size >= 80) {
                    for (area in smallAreas) {
                        area!!.remove = true
                    }
                }
                y += probeSize - probeOverlap
            }
            x += probeSize - probeOverlap
        }
        val i = task!!.areas!!.iterator()
        while (i.hasNext()) {
            val area = i.next()
            if (area.remove) {
                i.remove()
            }
        }
    }

    private val rand = Random()
    private val randomColor: Color
        private get() {
            val red = rand.nextInt(240)
            val green = rand.nextInt(240)
            val blue = rand.nextInt(240)
            return Color(red, green, blue)
        }

    
    override fun addDebugImages() {
        task!!.addDebugImage(debugImage, "touching")
        task!!.addDebugImage(task!!.backgroundImage, "background")
        task!!.addDefaultDebugImage("areas")
    }

    companion object {
        /**
         * Areas that contains less than this number of pixels are rejected.
         */
        private const val MIN_PIXELS_AREA = 3 // TODO relative to resolution
    }
}
