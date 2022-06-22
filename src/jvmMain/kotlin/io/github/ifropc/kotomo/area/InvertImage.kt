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

import io.github.ifropc.kotomo.CharacterColor
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import io.github.ifropc.kotomo.util.FixedParameters
import io.github.ifropc.kotomo.util.ImageUtil.createCopy
import io.github.ifropc.kotomo.util.Parameters
import mu.KotlinLogging
import java.awt.AlphaComposite
import java.awt.Color
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.ceil
import kotlin.math.pow

private val log = KotlinLogging.logger { }

/**
 * Inverts target image colors in regions with black background
 */
class InvertImage(task: AreaTask?) : AreaStep(task, "invert") {
    
    override fun runImpl() {
        if (FixedParameters.fixedBlackLevelEnabled || Parameters.colorTarget === CharacterColor.BLACK_ON_WHITE) {
            // don't invert
        } else if (Parameters.colorTarget === CharacterColor.AUTOMATIC) {
            detectBlackOnWhite()
        } else if (Parameters.colorTarget === CharacterColor.WHITE_ON_BLACK) {
            invertWholeImage()
        }
    }

    /**
     * Target image width divided by BLOCK_SIZE
     */
    private var width = 0

    /**
     * Target image height divided by BLOCK_SIZE
     */
    private var height = 0

    /**
     * Blocks that have already been processed. Indexed by image.xy/BLOCK_SIZE.
     */
    private lateinit var visited: Array<BooleanArray>

    /**
     * Blocks that have been marked for inversion. Indexed by image.xy/BLOCK_SIZE.
     */
    private lateinit var invert: Array<BooleanArray>

    /**
     * Number of touching neighbour blocks (top,bottom,left/right) that have been inverted.
     * Indexed by image.xy/BLOCK_SIZE.
     */
    private lateinit var neighboursInverted: Array<IntArray>

    /**
     * Image coordinates divided by BLOCK_SIZE
     */
    private open inner class Block(var x: Int, var y: Int)

    /**
     * Detects and inverts regions of the target image with black background
     */
    private fun detectBlackOnWhite() {

        // divide image into blocks and count the number of black pixels in each block
        // if pixel count is high enough, invert pixels 
        width = ceil((1.0f * task!!.width / BLOCK_SIZE).toDouble()).toInt()
        height = ceil((1.0f * task!!.height / BLOCK_SIZE).toDouble()).toInt()
        visited = Array(width) { BooleanArray(height) }
        invert = Array(width) { BooleanArray(height) }
        neighboursInverted = Array(width) { IntArray(height) }
        task!!.borderPixels = Array(task!!.width) { BooleanArray(task!!.height) }

        // mark image blocks that needs to be inverted
        for (x in 0 until width) {
            for (y in 0 until height) {
                checkBlock(x, y)
            }
        }

        // invert marked blocks
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (invert[x][y]) {
                    invertBlock(x, y)
                }
            }
        }
        task!!.inverted = invert
    }

    /**
     * Calculates black pixel ratio in the block
     */
    private fun calcPixelAreaRatio(block: Block): Float {
        var blackPixels = 0
        var allPixels = 0
        var px = block.x * BLOCK_SIZE
        while (px < (block.x + 1) * BLOCK_SIZE && px < task!!.width) {
            var py = block.y * BLOCK_SIZE
            while (py < (block.y + 1) * BLOCK_SIZE && py < task!!.height) {
                if (task!!.getPixel(px, py)) {
                    ++blackPixels
                }
                ++allPixels
                py++
            }
            px++
        }
        return 1.0f * blackPixels / allPixels
    }

    private val debugEllipse = false

    private inner class Pixel(x: Int, y: Int) : Block(x, y)

    private val debugEllipsePixels: MutableList<Pixel> = ArrayList()

    /**
     * Calcuates black pixel ratio within ellipse
     */
    private fun calcPixelAreaRatioEllipse(
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        size: Float
    ): Float {
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val pWidth = width * BLOCK_SIZE
        val pHeight = height * BLOCK_SIZE
        val a2 = (1.0f * pWidth / 2 * size).toDouble().pow(2.0).toFloat()
        val b2 = (1.0f * pHeight / 2 * size).toDouble().pow(2.0).toFloat()
        val pxMin = minX * BLOCK_SIZE
        val pxMax = (maxX + 1) * BLOCK_SIZE - 1
        val pyMin = minY * BLOCK_SIZE
        val pyMax = (maxY + 1) * BLOCK_SIZE - 1
        val pxCenter = pxMin + pWidth / 2
        val pyCenter = pyMin + pHeight / 2
        var blackPixels = 0
        var allPixels = 0
        var px = pxMin
        while (px <= pxMax && px < task!!.width) {
            var py = pyMin
            while (py <= pyMax && py < task!!.height) {
                val value = (1.0f * (px - pxCenter).toDouble().pow(2.0) / a2 +
                        1.0f * (py - pyCenter).toDouble().pow(2.0) / b2).toFloat()
                if (value > 1) {
                    // outside the ellipse
                    py++
                    continue
                }
                if (debugEllipse && value > 0.95f) {
                    debugEllipsePixels.add(Pixel(px, py))
                }
                if (task!!.getPixel(px, py)) {
                    ++blackPixels
                }
                ++allPixels
                py++
            }
            px++
        }
        //System.err.println("blackPixels:"+blackPixels+" allPixels:"+allPixels+" ratio:"+ratio);
        return 1.0f * blackPixels / allPixels
    }

    /**
     * Checks if block x,y needs to be inverted. If yes, marks the block and flood-fills
     * it's neighbours that also need to be inverted.
     */
    private fun checkBlock(x: Int, y: Int) {

        // blocks marked to be inverted
        val marked: MutableList<Block> = ArrayList()

        // blocks where large majority of pixels are black
        var blackBlocks = 0

        // outer bounds of marked area
        var minX = x
        var maxX = x
        var minY = y
        var maxY = y

        // find connected region of blocks with large number of black pixels,
        // mark blocks to be inverted
        val todo: Queue<Block> = LinkedBlockingQueue()
        todo.add(Block(x, y))
        while (!todo.isEmpty()) {
            val block = todo.remove()
            if (visited[block.x][block.y]) {
                continue
            }
            visited[block.x][block.y] = true
            val pixelRatio = calcPixelAreaRatio(block)
            val threshold = 0.95f - neighboursInverted[block.x][block.y] * 0.25f
            if (pixelRatio >= threshold) {
                markBlock(block, todo)
                marked.add(block)
            }
            if (pixelRatio >= 0.95f) {
                ++blackBlocks
            }
            if (block.x < minX) minX = block.x
            if (block.x > maxX) maxX = block.x
            if (block.y < minY) minY = block.y
            if (block.y > maxY) maxY = block.y
        }

        // calculate number of marked quad (2x2) blocks
        var quadBlocks = 0
        for (block in marked) {
            try {
                if (invert[block.x + 1][block.y] &&
                    invert[block.x][block.y + 1] &&
                    invert[block.x + 1][block.y + 1]
                ) {
                    ++quadBlocks
                    // quads might overlap but that's fine, this is not a packing problem
                }
            } catch (e: Exception) {
                // TODO: check this WARN. tst3BlackBG
                log.warn(e) {}
            }
        }

        // rollback inverted blocks if they don't form large enough continous region
        // this is done to prevent inversion of strokes inside large thick characters
        if (quadBlocks < 4 || blackBlocks < 4) {
            for (block in marked) {
                invert[block.x][block.y] = false
            }
            return
        }

        // fill gaps within marked region
        val region = KotomoRectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
        fillGaps(region)
    }

    /**
     * Marks bloc to be inverted and adds neighbour blocks to todo list
     */
    private fun markBlock(block: Block, todo: Queue<Block>) {
        val x = block.x
        val y = block.y

        // invert block
        invert[x][y] = true

        // check neighbors
        if (x > 0) {
            neighboursInverted[x - 1][y]++
            todo.add(Block(x - 1, y))
        }
        if (x < width - 1) {
            neighboursInverted[x + 1][y]++
            todo.add(Block(x + 1, y))
        }
        if (y > 0) {
            neighboursInverted[x][y - 1]++
            todo.add(Block(x, y - 1))
        }
        if (y < height - 1) {
            neighboursInverted[x][y + 1]++
            todo.add(Block(x, y + 1))
        }
    }

    /**
     * Fills gaps within region
     *
     * @param marked List of inverted blocks that belong to region
     * @param region Region bounds
     */
    private fun fillGaps(region: KotomoRectangle) {

        // rectangle that is one block smaller in every direction
        val internal = KotomoRectangle(
            region.x + 1, region.y + 1,
            region.width - 2, region.height - 2
        )
        val visited = Array(width) { BooleanArray(height) }

        // find gaps that are enclosed in inverted blocks
        for (x in internal.x until internal.x + internal.width) {
            for (y in internal.y until internal.y + internal.height) {
                if (invert[x][y] || visited[x][y]) {
                    continue
                }

                // outer bounds of the gap
                var minX = x
                var maxX = x
                var minY = y
                var maxY = y

                // flood-fill non-inverted blocks
                val marked: MutableList<Block> = ArrayList()
                var touchesBorder = false
                val todo: Queue<Block> = LinkedBlockingQueue()
                todo.add(Block(x, y))
                while (!todo.isEmpty()) {
                    val block = todo.remove()
                    if (invert[block.x][block.y] || visited[block.x][block.y]) {
                        continue
                    }
                    if (!internal.contains(block.x, block.y)) {
                        touchesBorder = true
                        continue
                    }
                    visited[block.x][block.y] = true
                    marked.add(block)
                    todo.add(Block(block.x, block.y - 1))
                    todo.add(Block(block.x, block.y + 1))
                    todo.add(Block(block.x - 1, block.y))
                    todo.add(Block(block.x + 1, block.y))
                    if (block.x < minX) minX = block.x
                    if (block.x > maxX) maxX = block.x
                    if (block.y < minY) minY = block.y
                    if (block.y > maxY) maxY = block.y
                }
                if (touchesBorder) {
                    continue
                }

                // check that the gap is not too large
                val width = maxX - minX + 1
                val height = maxY - minY + 1
                val maxGapSize = 8
                if (width > maxGapSize && height > maxGapSize) {
                    continue
                }

                // check if the gap contains an ellipse with low pixel density,
                // then it's most likely a small speech bubble on a black background
                val minCheckEllipse = 3
                if (width >= minCheckEllipse && height >= minCheckEllipse) {
                    val ratio = calcPixelAreaRatioEllipse(minX, maxX, minY, maxY, 0.9f)
                    if (ratio < 0.1f) {
                        continue
                    }
                }

                // TODO check also 90% square?

                // invert blocks inside the gap
                for (block in marked) {
                    invert[block.x][block.y] = true
                }
            }
        }
    }

    /**
     * Inverts block x,y
     */
    private fun invertBlock(x: Int, y: Int) {

        // determine if this block is at the edge of inverted region,
        // line is drawn around region's border. this way the border is included
        // in the background and doesn't confuse column detector
        var top = false
        if (y > 0) {
            if (!invert[x][y - 1]) top = true
        }
        var bottom = false
        if (y < height - 1) {
            if (!invert[x][y + 1]) bottom = true
        }
        var left = false
        if (x > 0) {
            if (!invert[x - 1][y]) left = true
        }
        var right = false
        if (x < width - 1) {
            if (!invert[x + 1][y]) right = true
        }
        val rect = KotomoRectangle(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE)
        invertRegion(rect, top, bottom, left, right)
    }

    /**
     * Inverts pixels inside rectangle. Paints argument borders black
     */
    private fun invertRegion(
        rect: KotomoRectangle, top: Boolean, bottom: Boolean,
        left: Boolean, right: Boolean
    ) {
        val minX = rect.x
        val maxX = rect.x + rect.width - 1
        val minY = rect.y
        val maxY = rect.y + rect.height - 1

        // invert pixels
        var x = minX
        while (x <= maxX && x < task!!.width) {
            var y = minY
            while (y <= maxY && y < task!!.height) {
                task!!.setPixel(x, y, !task!!.getPixel(x, y))
                y++
            }
            x++
        }

        // draw lines around border
        if (top) drawHorizontalLine(minX, maxX, minY)
        if (bottom) drawHorizontalLine(minX, maxX, maxY)
        if (left) drawVerticalLine(minX, minY, maxY)
        if (right) drawVerticalLine(maxX, minY, maxY)
    }

    /**
     * Draws straight vertical line
     */
    private fun drawVerticalLine(x: Int, startY: Int, endY: Int) {
        for (y in startY..endY) {
            task!!.setPixel(x, y, true)
            task!!.setBorderPixel(x, y, true)
        }
    }

    /**
     * Draws straight horizontal line
     */
    private fun drawHorizontalLine(startX: Int, endX: Int, y: Int) {
        for (x in startX..endX) {
            task!!.setPixel(x, y, true)
            task!!.setBorderPixel(x, y, true)
        }
    }

    /**
     * Inverts pixels in the whole image
     */
    private fun invertWholeImage() {
        val rect = KotomoRectangle(0, 0, task!!.width, task!!.height)
        invertRegion(rect, false, false, false, false)
    }

    
    override fun addDebugImages() {
        val markInvertedRegions = true
        val image = createCopy(task!!.binaryImage)
        if (markInvertedRegions) {
            val g = image.createGraphics()
            g.paint = Color.BLUE
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (invert[x][y]) {
                        g.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE)
                    }
                }
            }
            if (debugEllipse) {
                for (pixel in debugEllipsePixels) {
                    image.setRGB(pixel.x, pixel.y, Color.RED.rgb)
                }
            }
        }
        task!!.addDebugImage(image, "invert")
    }

    companion object {
        /**
         * Image is divided into blocks. BLOCK_SIZE specifies block width and height in pixels.
         */
        const val BLOCK_SIZE = 15 // TODO scale with resolution
    }
}
