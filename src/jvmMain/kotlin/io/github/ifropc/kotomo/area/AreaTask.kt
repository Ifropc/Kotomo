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
import io.github.ifropc.kotomo.ocr.Point
import io.github.ifropc.kotomo.ocr.Rectangle
import io.github.ifropc.kotomo.util.DebugImage
import io.github.ifropc.kotomo.util.FixedParameters
import io.github.ifropc.kotomo.util.ImageUtil.buildScaledImage
import io.github.ifropc.kotomo.util.ImageUtil.createGrayImage
import io.github.ifropc.kotomo.util.ImageUtil.createImageFromMatrix
import io.github.ifropc.kotomo.util.ImageUtil.crop
import io.github.ifropc.kotomo.util.ImageUtil.paintAreas
import io.github.ifropc.kotomo.util.ImageUtil.paintColumn
import io.github.ifropc.kotomo.util.ImageUtil.setClipboard
import io.github.ifropc.kotomo.util.Parameters
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Area detection algorithm input and output values.
 */
class AreaTask(targetImage: BufferedImage) {
    

    /**
     * Target image width
     */
    var width: Int

    /**
     * Target image height
     */
    var height: Int

    /**
     * Original image (read from file or captured from screen).
     */
    var originalImage: BufferedImage

    /**
     * Target image after unsharp mask filter. Used to increase contranst.
     */
    var sharpenedImage: BufferedImage? = null

    /**
     * Image is divided into InvertImage.BLOCK_SIZE^2 blocks. Colors in each block can be inverted
     * if it contains white text on dark background. This matrix marks the inverted blocks as true.
     */
    lateinit var inverted: Array<BooleanArray>

    /**
     * Image that contains only black or white pixels (true = black).
     * Parameters.blackThreshold is used as a threshold for determining which pixels are considered black.
     */
    lateinit var binaryImage: Array<BooleanArray>

    /**
     * Binary image of the background outside areas
     */
    lateinit var backgroundImage: Array<BooleanArray>

    /**
     * Pixels that define black/white background border
     */
    lateinit var borderPixels: Array<BooleanArray>

    /**
     * Connected groups on pixels in target image that represent (candidate) characters
     */
    var areas: MutableList<Area>? = null

    /**
     * Areas that are close to each other in reading direction are grouped into columns.
     */
    var columns: MutableList<Column>? = null

    /**
     * Column in vertical orientation.
     */
    var verticalColumns: MutableList<Column>? = null

    /**
     * Column in horizontal orientation.
     */
    var horizontalColumns: MutableList<Column>? = null

    /**
     * List of images used during development to visualize processing steps.
     * Only generated if debugMode = true;
     */
    var debugImages: MutableList<DebugImage?> = ArrayList()

    init {
        width = targetImage.width
        height = targetImage.height
        originalImage = targetImage
    }

    /**
     * Collects areas from columns into areas list
     */
    fun collectAreas() {
        areas = ArrayList()
        for (col in columns!!) {
            areas!!.addAll(col!!.areas)
        }
    }

    /**
     * Clears all changed flags from areas and columns.
     * Used to highlight changes in debug images.
     */
    fun clearChangedFlags() {
        if (areas != null) {
            for (area in areas!!) {
                area.isChanged = false
            }
        }
        if (columns != null) {
            for (column in columns!!) {
                column.isChanged = false
            }
        }
    }

    /**
     * Gets the closest area near point
     *
     * @param point Mouse cursor location relative to target image
     */
    fun getArea(point: Point): Area? {
        var minDistance = 1000000
        var closestArea: Area? = null
        for (area in areas!!) {
            if (area!!.isPunctuation) {
                continue
            }
            val distance = area.midpoint.distance(point).toInt()
            if (distance < minDistance) {
                minDistance = distance
                closestArea = area
            }
        }
        return if (closestArea == null) {
            // no valid areas found
            null
        } else if (minDistance > closestArea.maxDim) {
            // closest area is too far away
            null
        } else {
            closestArea
        }
    }

    /**
     * Checks if the binary image has black pixel at x,y
     */
    fun getPixel(x: Int, y: Int): Boolean {
        return if (x < 0 || y < 0 || x >= width || y >= height) {
            false
        } else binaryImage[x][y]
    }

    fun setPixel(x: Int, y: Int, value: Boolean) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return
        }
        binaryImage[x][y] = value
    }

    /**
     * Checks if the image has border pixel (between black and white areas) at x,y
     */
    fun getBorderPixel(x: Int, y: Int): Boolean {
        return if (x < 0 || y < 0 || x >= width || y >= height || borderPixels == null) {
            false
        } else borderPixels!![x][y]
    }

    /**
     * Sets pixel on black/white region border
     */
    fun setBorderPixel(x: Int, y: Int, value: Boolean) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return
        }
        borderPixels!![x][y] = value
    }

    /**
     * Checks if the binary image has black background pixel at x,y
     */
    fun getBackgroundPixel(x: Int, y: Int): Boolean {
        return if (x < 0 || y < 0 || x >= width || y >= height) {
            false
        } else backgroundImage[x][y]
    }

    /**
     * Returns true if image contains any pixels in vertical line
     * @param black If true, scans for black pixels, else white pixels
     */
    fun containsPixelsVertical(black: Boolean, x: Int, startY: Int, endY: Int): Boolean {
        for (y in startY..endY) {
            if (binaryImage[x][y] == black) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if image contains any pixels in horizontal line
     * @param black If true, scans for black pixels, else white pixels
     */
    fun containsPixelsHorizontal(black: Boolean, startX: Int, endX: Int, y: Int): Boolean {
        for (x in startX..endX) {
            if (binaryImage[x][y] == black) {
                return true
            }
        }
        return false
    }

    /**
     * Return true if image contains any black pixels within rect
     */
    fun containsPixels(rect: Rectangle): Boolean {
        for (y in rect.y..rect.y + rect.height - 1) {
            for (x in rect.x..rect.x + rect.width - 1) {
                if (getPixel(x, y)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Counts the number of pixels along horizontal line.
     */
    fun countPixelsHorizontal(minX: Int, maxX: Int, y: Int): Int {
        var pixels = 0
        for (x in minX..maxX) {
            if (getPixel(x, y)) {
                ++pixels
            }
        }
        return pixels
    }

    /**
     * Counts the number of pixels along vertical line.
     */
    fun countPixelsVertical(x: Int, minY: Int, maxY: Int): Int {
        var pixels = 0
        for (y in minY..maxY) {
            if (getPixel(x, y)) {
                ++pixels
            }
        }
        return pixels
    }

    /**
     * Counts the number of pixels in rectangle
     *
     * @param background If true, checks only the background image
     * @param inside If true, checks also pixels inside the rectangle. If false, only
     * checks the border pixels
     */
    fun countPixels(rect: Rectangle?, background: Boolean, inside: Boolean): Int {
        var pixels = 0
        for (y in rect!!.y..rect.y + rect.height - 1) {
            for (x in rect.x..rect.x + rect.width - 1) {
                if (inside || y == rect.y || y == rect.y + rect.height - 1 || x == rect.x || x == rect.x + rect.width - 1) {
                    if (x < 0 || x >= width || y < 0 || y >= height) {
                        continue
                    }
                    if (background) {
                        if (getBackgroundPixel(x, y)) {
                            ++pixels
                        }
                    } else {
                        if (getPixel(x, y)) {
                            ++pixels
                        }
                    }
                }
            }
        }
        return pixels
    }

    /**
     * Gets RGB value of pixel x,y (minimum from single channel)
     */
    fun getPixelRGB(x: Int, y: Int): Int {
        val rgb = originalImage.getRGB(x, y)
        val color = Color(rgb)
        val red = color.red
        val green = color.green
        val blue = color.blue
        var min = Math.min(Math.min(red, green), blue)
        if (isPixelInverted(x, y)) {
            min = 255 - min
        }
        return min
    }

    /**
     * @return true if x,y pixels have been inverted.
     */
    protected fun isPixelInverted(x: Int, y: Int): Boolean {
        return if (FixedParameters.fixedBlackLevelEnabled || Parameters.colorTarget === CharacterColor.BLACK_ON_WHITE) {
            false
        } else if (Parameters.colorTarget === CharacterColor.AUTOMATIC) {
            inverted[x / InvertImage.Companion.BLOCK_SIZE][y / InvertImage.Companion.BLOCK_SIZE]
        } else {
            true
        }
    }

    /**
     * Gets at most Parameters.ocrMaxCharacters images containing single characters closest
     * to the point in reading direction. Returns empty list if none is found.
     *
     * @param point Mouse cursor location relative to target image
     */
    fun getSubImages(point: Point): List<SubImage> {
        val subImages: MutableList<SubImage> = ArrayList()

        // find closest area to the point
        val firstArea = getArea(point) ?: return subImages

        // find next areas by following columns
        val areas: MutableList<Area?> = ArrayList()
        var found = false
        var column = firstArea.column
        loop@ while (true) {
            for (area in column!!.areas) {
                if (area === firstArea) {
                    found = true
                }
                if (found && !area!!.isPunctuation) {
                    areas.add(area)
                }
                if (areas.size == Parameters.ocrMaxCharacters) {
                    break@loop
                }
            }
            if (column.nextColumn == null) {
                // last column reached
                break
            }
            column = column.nextColumn
            if (column === firstArea.column) {
                // column loop detected, this might happen with intersecting columns
                // (most likely in wrong orientation)
                break
            }
        }

        // create subimages from areas
        for (area in areas) {

            // crop from binary image
            val croppedImage = crop(binaryImage, area!!.rectangle!!)
            val subImage = SubImage(croppedImage, area!!.rectangle, area!!.column)
            subImages.add(subImage)
        }
        return subImages
    }

    /**
     * Gets subimages from list of rectangles.
     */
    fun getSubImages(areas: List<Rectangle?>): List<SubImage> {
        val subImages: MutableList<SubImage> = ArrayList()
        for (area in areas) {
            val croppedImage = crop(binaryImage, area!!)
            val subImage = SubImage(croppedImage, area, null)
            cropBorder(subImage)
            subImages.add(subImage)
        }
        return subImages
    }

    /**
     * Crops empty border around subimage
     */
    private fun cropBorder(subImage: SubImage) {
        try {
            var minX: Int
            minX = subImage.minX
            while (minX < subImage.midX) {
                if (containsPixelsVertical(true, minX, subImage.minY, subImage.maxY)) {
                    break
                }
                minX++
            }
            var maxX: Int
            maxX = subImage.maxX
            while (maxX > subImage.midX) {
                if (containsPixelsVertical(true, maxX, subImage.minY, subImage.maxY)) {
                    break
                }
                maxX--
            }
            var minY: Int
            minY = subImage.minY
            while (minY < subImage.midY) {
                if (containsPixelsHorizontal(true, subImage.minX, subImage.maxX, minY)) {
                    break
                }
                minY++
            }
            var maxY: Int
            maxY = subImage.maxY
            while (maxY > subImage.midY) {
                if (containsPixelsHorizontal(true, subImage.minX, subImage.maxX, maxY)) {
                    break
                }
                maxY--
            }
            subImage.location = Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
            subImage.image = crop(binaryImage, subImage.location!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     */
    fun addDebugImage(image: Array<BooleanArray>, step: String?) {
        val bufferedImage = createImageFromMatrix(image)
        addDebugImage(bufferedImage, step)
    }

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     */
    fun addDebugImage(image: BufferedImage?, step: String?) {
        addDebugImage(DebugImage(image!!, step!!, null, Parameters.debugFilePrefix))
    }

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     *
     * @param vertical If set, orientation is diplayed in the file name
     */
    fun addDebugImage(image: BufferedImage?, step: String?, vertical: Boolean?) {
        addDebugImage(DebugImage(image!!, step!!, vertical, Parameters.debugFilePrefix))
    }

    /**
     * Adds image to list of debug images
     */
    private fun addDebugImage(image: DebugImage) {
        if (debugImages.size < Parameters.maxDebugImages) {
            debugImages.add(image)
        } else {
            System.err.println("maxDebugImages reached")
        }
    }

    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */
    
    fun addDefaultDebugImage(name: String?, vertical: Boolean?) {
        addDefaultDebugImage(name, areas, columns, vertical)
    }
    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */
    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */
    
    fun addDefaultDebugImage(
        name: String?, areas: List<Area>? = this.areas, columns: List<Column?>? = this.columns,
        vertical: Boolean? = null
    ) {
        val image = createDefaultDebugImage(areas, columns)
        addDebugImage(image, name, vertical)
    }
    /**
     * Paints a default debug image displaying columns and areas and gray background.
     */
    /**
     * Paints a default debug image displaying columns and areas and gray background.
     */
    
    fun createDefaultDebugImage(
        areas: List<Area>? = this.areas,
        columns: List<Column?>? = this.columns
    ): BufferedImage {
        var image = createGrayImage(binaryImage, backgroundImage)
        image = paintAreas(image, areas!!)
        if (columns != null) {
            for (col in columns) {
                paintColumn(image, col!!)
            }
        }
        return image
    }

    /**
     * Writes debug images to target directory. filenameBase is included in each file name.
     */
    
    fun writeDebugImages() {
        if (!Parameters.isSaveAreaFailed) {
            return
        }
        println("Writing area debug images")
        for (image in debugImages) {
            writeDebugImage(image)
        }
    }

    /**
     * Writes debug image to disk in Parameters.debugDir directory. Files are
     * named as "test number.index number.algorithm step.png"
     */
    
    private fun writeDebugImage(image: DebugImage?) {
        if (image == null) {
            return
        }
        val targetDir = Parameters.debugDir
        val filename = image.filename
        if (Parameters.debugImages != null && Parameters.debugImages.size > 0) {
            for (debugImage in Parameters.debugImages) {
                if (filename.contains(debugImage)) {
                    break
                }
            }
            return
        }
        val file = File("$targetDir/$filename")
        var scale = 1
        val minDim = Math.max(image.image.width, image.image.height)
        if (minDim < Parameters.smallDebugAreaImageThreshold) {
            scale = Parameters.smallDebugAreaImageScale
        }
        val scaledImage = buildScaledImage(image.image, scale)
        if (!Parameters.debugAreaImageToClipboard) {
            ImageIO.write(scaledImage, "png", file)
        } else {
            setClipboard(scaledImage)
        }
    }
}
