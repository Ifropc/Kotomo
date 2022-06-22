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
package io.github.ifropc.kotomo.util

import io.github.ifropc.kotomo.area.Area
import io.github.ifropc.kotomo.area.Column
import io.github.ifropc.kotomo.ocr.Colors
import io.github.ifropc.kotomo.ocr.KotomoColor
import io.github.ifropc.kotomo.ocr.Point
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import io.github.ifropc.kotomo.util.JVMUtil.toAwt
import io.github.ifropc.kotomo.util.JVMUtil.toKotomo
import io.github.ifropc.kotomo.util.JVMUtil.toKotomoImage
import mu.KotlinLogging
import org.imgscalr.Scalr
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val log = KotlinLogging.logger { }

object ImageUtil {
    /**
     * Returns true if pixel is determined to be black.
     * @param rgb
     * @param blackThreshold If null, uses fixedBlackLevel values instead of single threshold
     * @return
     */
    private fun containsPixel(rgb: Int, blackThreshold: Int?): Boolean {
        if (blackThreshold == null) {
            return containsPixelFixedBlackLevel(rgb)
        }
        val red = rgb and 0x00ff0000 shr 16 < blackThreshold
        val green = rgb and 0x0000ff00 shr 8 < blackThreshold
        val blue = rgb and 0x000000ff < blackThreshold
        return red && green || green && blue || red && blue // 2 of 3		
    }

    /**
     * Returns true if pixel is determined to be black.
     * Uses fixed black level specified in Parameters.fixedBlackLevel*
     */
    private fun containsPixelFixedBlackLevel(rgb: Int): Boolean {
        val red = rgb and 0x00ff0000 shr 16
        val green = rgb and 0x0000ff00 shr 8
        val blue = rgb and 0x000000ff

        //System.err.println("red  :"+red+" <-> "+Parameters.fixedBlackLevelRed);
        //System.err.println("green:"+green+" <-> "+Parameters.fixedBlackLevelGreen);
        //System.err.println("blue :"+blue+" <-> "+Parameters.fixedBlackLevelBlue);
        if (red < FixedParameters.fixedBlackLevelRed - FixedParameters.fixedBlackLevelRange) {
            return false
        }
        if (red > FixedParameters.fixedBlackLevelRed + FixedParameters.fixedBlackLevelRange) {
            return false
        }
        if (green <FixedParameters.fixedBlackLevelGreen - FixedParameters.fixedBlackLevelRange) {
            return false
        }
        if (green > FixedParameters.fixedBlackLevelGreen + FixedParameters.fixedBlackLevelRange) {
            return false
        }
        if (blue < FixedParameters.fixedBlackLevelBlue - FixedParameters.fixedBlackLevelRange) {
            return false
        }
        return blue <= FixedParameters.fixedBlackLevelBlue + FixedParameters.fixedBlackLevelRange
    }

    /**
     * Builds a larger image, scaled by scale factor. Used for created
     * larger debug images.
     */

	fun buildScaledImage(image: BufferedImage, scale: Int): BufferedImage {
        val target = BufferedImage(
            image.width * scale,
            image.height * scale,
            BufferedImage.TYPE_INT_RGB
        )
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                for (ty in (y * scale until (y + 1) * scale)) {
                    for (tx in (x * scale until (x + 1) * scale)) {
                        target.setRGB(tx, ty, rgb)
                    }
                }
            }
        }
        return target
    }

    /**
     * Changes image's color (replaces black pixels)
     */

	fun colorizeImage(image: BufferedImage, color: KotomoColor): BufferedImage {
        val target = BufferedImage(
            image.width,
            image.height,
            BufferedImage.TYPE_INT_RGB
        )
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) == Colors.BLACK.rgb) {
                    target.setRGB(x, y, color.rgb)
                } else if (image.getRGB(x, y) == Colors.WHITE.rgb) {
                    target.setRGB(x, y, Colors.WHITE.rgb)
                } else {
                    throw Error("Unknown color")
                }
            }
        }
        return target
    }

    /**
     * Creates empty copy of argument image with same dimensions and type
     */
    private fun createEmptyCopy(image: BufferedImage): BufferedImage {
        var type = image.type
        if (type == 0) {
            type = BufferedImage.TYPE_INT_BGR
        }
        return BufferedImage(image.width, image.height, type)
    }

    /**
     * Creates a copy of argument image
     */
    fun createCopy(image: BufferedImage): BufferedImage {
        val copy = BufferedImage(image.width, image.height, image.type)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getRGB(x, y)
                copy.setRGB(x, y, rgb)
            }
        }
        return copy
    }

    /**
     * Creates a copy of argument image
     */
    fun createCopy(image: Array<BooleanArray>): BufferedImage {
        val width = image.size
        val height = image[0].size
        val copy = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!image[x][y]) {
                    copy.setRGB(x, y, Colors.WHITE.rgb)
                }
            }
        }
        return copy
    }

    /**
     * Creates BufferedImage from boolean matrix
     */
    fun createImageFromMatrix(image: Array<BooleanArray>): BufferedImage {
        val width = image.size
        val height = image[0].size
        val bImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!image[x][y]) {
                    bImage.setRGB(x, y, Colors.WHITE.rgb)
                }
            }
        }
        return bImage
    }

    /**
     * Creates boolean matrix from BufferedImage
     */
    fun createMatrixFromImage(image: BufferedImage): Array<BooleanArray> {
        val width = image.width
        val height = image.height
        val matrix = Array(width) { BooleanArray(height) }
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (image.getRGB(x, y) == Colors.BLACK.rgb) {
                    matrix[x][y] = true
                }
            }
        }
        return matrix
    }

    /**
     * Creates BufferedImage where foreground pixels are black and background pixels are gray
     */
    fun createGrayImage(image: Array<BooleanArray>, backgroundImage: Array<BooleanArray>): BufferedImage {
        val width = image.size
        val height = image[0].size
        val bImage = createImageFromMatrix(image)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = image[x][y]
                val backgroundPixel = backgroundImage[x][y]
                if (pixel && !backgroundPixel) {
                    bImage.setRGB(x, y, Colors.BLACK.rgb)
                } else if (backgroundPixel) {
                    bImage.setRGB(x, y, Colors.GRAY.rgb)
                } else {
                    bImage.setRGB(x, y, Colors.WHITE.rgb)
                }
            }
        }
        return bImage
    }

    /**
     * Creates empty white image of given size.
     */

	fun createWhiteImage(width: Int, height: Int): BufferedImage {
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = newImage.createGraphics()
        g.paint = Colors.WHITE.toAwt()
        g.fillRect(0, 0, newImage.width, newImage.height)
        return newImage
    }

    /**
     * Paints areas into argument image using default colors
     */
    
    fun paintAreas(image: BufferedImage, areas: List<Area>): BufferedImage {
        val newImage = createCopy(image)
        for (area in areas) {
            var col = Colors.RED
            if (area.isPunctuation) {
                col = Colors.LIGHT_GRAY
            }
            if (area.column != null && area.column!!.isFurigana) {
                col = Colors.LIGHT_GRAY
            }
            if (area.isChanged) {
                col = Colors.BLUE
            }
            if (area.debugColor != null) {
                col = area.debugColor!!.toKotomo()
            }
            paintRectangle(newImage, area.rectangle, col)
        }
        return newImage
    }

    fun paintColumn(image: BufferedImage, column: Column) {
        val color: KotomoColor?
        color = if (column.debugColor != null) {
            column.debugColor?.toKotomo()
        } else if (column.isChanged) {
            Colors.BLUE
        } else if (column.isFurigana) {
            Colors.LIGHT_GRAY
        } else if (column.isVertical) {
            Colors.ORANGE
        } else {
            Colors.CYAN
        }
        paintRectangle(image, column.rectangle, color)
        for (furigana in column.furiganaColumns) {
            paintColumn(image, furigana)
        }
    }

    /**
     * Paints a rectangle to image.
     */
    fun paintRectangle(image: BufferedImage, rect: KotomoRectangle?, color: KotomoColor?) {

        // TODO AlphaComposite with background? see InvertImage for example

        // top
        for (x in rect!!.x..rect.x + rect.width - 1) {
            val y = rect.y
            paintPixel(image, color, x, y)
        }

        // bottom
        for (x in rect.x..rect.x + rect.width - 1) {
            val y = rect.y + rect.height - 1
            paintPixel(image, color, x, y)
        }

        // left
        for (y in rect.y + 1..rect.y + rect.height - 1) {
            val x = rect.x
            paintPixel(image, color, x, y)
        }

        // right
        for (y in rect.y + 1..rect.y + rect.height - 1) {
            val x = rect.x + rect.width - 1
            paintPixel(image, color, x, y)
        }
    }

    /**
     * Creates a rectangle that spans argument points. Order doesn't matter.
     */
    fun createRectangle(p1: Point, p2: Point): KotomoRectangle {
        val minX = min(p1.x, p2.x)
        val maxX = max(p1.x, p2.x)
        val minY = min(p1.y, p2.y)
        val maxY = max(p1.y, p2.y)
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        return KotomoRectangle(minX, minY, width, height)
    }

    /**
     * Debug image pixels ordered by painting priority. Pixels on
     * the end of the list replace earlier colors.
     */
    private val debugColorPriority = arrayOf(
        Colors.WHITE,
        Colors.LIGHT_GRAY,
        Colors.GRAY,
        Colors.ORANGE,
        Colors.CYAN,
        Colors.GREEN,
        Colors.RED,
        Colors.BLUE,
        Colors.BLACK
    )
    private val debugColorPriorityMap: MutableMap<KotomoColor, Int> = HashMap()

    init {
        var priority = 0
        for (col in debugColorPriority) {
            debugColorPriorityMap[col] = priority++
        }
    }

    /**
     * Paints pixel at x,y coordinates. If pixel is already painted, it is replaced
     * if new color has higher priority.
     */
    private fun paintPixel(image: BufferedImage, color: KotomoColor?, x: Int, y: Int) {

        // this version keeps single color according to priority
        val priority = debugColorPriorityMap[color]
            ?: throw Error("Unknown color:$color")
        if (x < 0 || x >= image.width || y < 0 || y >= image.height) {
            return
        }
        val oldColor = image.toKotomoImage().getRGB(x, y)
        var oldPriority = debugColorPriorityMap[oldColor]
        if (oldPriority == null) {
            oldPriority = -1
        }
        if (priority > oldPriority) {
            image.setRGB(x, y, color!!.rgb)
        }
    }

    private val quality = Scalr.Method.BALANCED

    /**
     * Stretches or compresses image to target width and height.
     * Doesn't keep proportions.
     */

	fun stretch(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val scaledImage = Scalr.resize(image, quality, Scalr.Mode.FIT_EXACT, width, height)
        image.flush()
        return scaledImage
    }

    /**
     * Stretches or compresses image to target size. Checks image ratio, limits stretch amount.
     * This is used to prevent 一｜ー characters from filling the whole block. Extra space in final
     * image is filled with white and target image is positioned in the center.
     */

	fun stretchCheckRatio(image: BufferedImage, targetSize: Int, finalSize: Int): BufferedImage {

        // calculate image minor/major dimension ratio
        var ratio = 1.0f * image.width / image.height
        if (ratio > 1.0f) {
            ratio = 1 / ratio
        }

        // calculate target size, targetSize*targetSize unless ratio is below 0.4f
        var targetHeight = targetSize
        var targetWidth = targetSize
        val targetMinDim = Util.scale(ratio, 0.1f, 0.4f, 8f, targetSize.toFloat()).roundToInt()
        if (image.width > image.height) {
            targetHeight = targetMinDim
        } else {
            targetWidth = targetMinDim
        }

        // stretch image to target size
        val stretchedImage = stretch(image, targetWidth, targetHeight)

        // create final image and move stretched image to center
        return createSquareImage(stretchedImage, finalSize)
    }

    /**
     * Creates square image and positions sourceImage to center. Pixels may be cut
     * if sourceImage is larget than final image
     */

	fun createSquareImage(sourceImage: BufferedImage, size: Int): BufferedImage {
        val sourceWidth = sourceImage.width
        val sourceHeight = sourceImage.height
        val blockImage = createWhiteImage(size, size)
        val deltaX = (size - sourceWidth) / 2
        val deltaY = (size - sourceHeight) / 2
        for (y in 0 until sourceHeight) {
            val targetY = y + deltaY
            if (targetY < 0 || targetY >= size) {
                continue
            }
            for (x in 0 until sourceWidth) {
                val targetX = x + deltaX
                if (targetX < 0 || targetX >= size) {
                    continue
                }
                val rgb = sourceImage.getRGB(x, y)
                blockImage.setRGB(targetX, targetY, rgb)
            }
        }
        return blockImage
    }

    /**
     * Creates a sub-image defined by rectangle.
     */
    fun crop(image: Array<BooleanArray>, rect: KotomoRectangle): BufferedImage {
        val bImage = BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB)
        for (x in rect.x until rect.x + rect.width) {
            for (y in rect.y until rect.y + rect.height) {
                if (!image[x][y]) {
                    bImage.setRGB(x - rect.x, y - rect.y, Colors.WHITE.rgb)
                }
            }
        }
        return bImage
    }

    /**
     * Creates image that has only black and white pixels, determined by
     * blackThreshold
     * @param blackThreshold If null, uses fixedBlackLevel values instead of single threshold
     */

	fun makeBlackAndWhite(image: BufferedImage, blackThreshold: Int?): BufferedImage {
        val bwImage = createEmptyCopy(image)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val pixel = containsPixel(rgb, blackThreshold)
                if (pixel) {
                    bwImage.setRGB(x, y, Colors.BLACK.rgb)
                } else {
                    bwImage.setRGB(x, y, Colors.WHITE.rgb)
                }
            }
        }
        return bwImage
    }

    /**
     * Builds black and white image from 32x32 bit matrix
     */

	fun buildImage(matrix: IntArray): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        for (y in 0..31) {
            for (x in 0..31) {
                if (isBitSet(x, y, matrix)) {
                    image.setRGB(x, y, Colors.BLACK.rgb)
                } else {
                    image.setRGB(x, y, Colors.WHITE.rgb)
                }
            }
        }
        return image
    }

    /**
     * Builds 32x32 bit matrix from 32x32 image.
     */

    fun buildMatrix32(image: BufferedImage): IntArray {
        val matrix = IntArray(32)
        for (y in 0..31) {
            for (x in 0..31) {
                if (image.getRGB(x, y) == Colors.BLACK.rgb) {
                    matrix[y] = matrix[y] or 1
                }
                if (x < 31) matrix[y] = matrix[y] shl 1
            }
        }
        return matrix
    }


    /**
     * Sharpens the image
     */
	fun sharpenImage(image: BufferedImage): BufferedImage {
        if (FixedParameters.fixedBlackLevelEnabled) {
            return createCopy(image)
        }
        val sharpened: BufferedImage? = null
        val filter = UnsharpMaskFilter(
            Parameters.unsharpAmount, Parameters.unsharpRadius, Parameters.unsharpThreshold
        )
        return filter.filter(image, sharpened)
    }

    /**
     * Writes image to clipboard
     */
    fun setClipboard(image: Image) {

        // https://alvinalexander.com/java/java-copy-image-to-clipboard-example
        val imgSel = ImageSelection(image)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(imgSel, null)
    }


    fun main(args: Array<String>) {
        val matrix = IntArray(32)
        setBit(0, 0, matrix)
        setBit(30, 30, matrix)
        setBit(31, 30, matrix)
        setBit(31, 31, matrix)
        setBit(25, 25, matrix)
        setBit(25, 26, matrix)
        setBit(26, 25, matrix)
        setBit(26, 26, matrix)
        log.debug { "before:" } 
        debugPrintMatrix(matrix)
        val source = IntArray(32)
        setBit(29, 28, source)
        setBit(29, 29, source)
        log.debug { "source:" } 
        debugPrintMatrix(source)
        addBits(source, matrix)
        log.debug { "after:" } 
        debugPrintMatrix(matrix)
    }
} //This class is used to hold an image while on the clipboard.

//https://alvinalexander.com/java/java-copy-image-to-clipboard-example
internal class ImageSelection(private val image: Image) : Transferable {
    // Returns supported flavors
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    // Returns true if flavor is supported
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return DataFlavor.imageFlavor.equals(flavor)
    }

    // Returns image
    
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!DataFlavor.imageFlavor.equals(flavor)) {
            throw UnsupportedFlavorException(flavor)
        }
        return image
    }
}
