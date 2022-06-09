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

import io.github.ifropc.kotomo.ocr.Rectangle
import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.*
import java.net.URL
import javax.imageio.ImageIO

/**
 * http://www.java2s.com/Code/Java/Advanced-Graphics/UnsharpMaskDemo.htm
 * @author Romain Guy <romain.guy></romain.guy>@mac.com>
 */
class UnsharpMaskFilter //System.out.println("UnsharpMaskFilter amount:"+amount+" radius:"+radius+" threshold:"+threshold);
constructor(val amount: Float = 0.7f, val radius: Int = 2, val threshold: Int = 2) : AbstractFilter() {

    /**
     * {@inheritDoc}
     */
    override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
        var dst = dst
        val width = src.width
        val height = src.height
        if (dst == null) {
            dst = createCompatibleDestImage(src, null)
        }
        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        val kernel = GaussianBlurFilter.createGaussianKernel(radius)
        GraphicsUtilities.getPixels(src, 0, 0, width, height, srcPixels)
        // horizontal pass
        GaussianBlurFilter.blur(srcPixels, dstPixels, width, height, kernel, radius)
        // vertical pass
        GaussianBlurFilter.blur(dstPixels, srcPixels, height, width, kernel, radius)

        // blurred image is in srcPixels, we copy the original in dstPixels
        GraphicsUtilities.getPixels(src, 0, 0, width, height, dstPixels)
        // we compare original and blurred images,
        // and store the result in srcPixels
        sharpen(dstPixels, srcPixels, width, height, amount, threshold)

        // the result is now stored in srcPixels due to the 2nd pass
        GraphicsUtilities.setPixels(dst, 0, 0, width, height, srcPixels)
        return dst
    }

    companion object {
        fun sharpen(
            original: IntArray, blurred: IntArray, width: Int, height: Int,
            amount: Float, threshold: Int
        ) {
            var amount = amount
            var index = 0
            var srcR: Int
            var srcB: Int
            var srcG: Int
            var dstR: Int
            var dstB: Int
            var dstG: Int
            amount *= 1.6f
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val srcColor = original[index]
                    srcR = srcColor shr 16 and 0xFF
                    srcG = srcColor shr 8 and 0xFF
                    srcB = srcColor and 0xFF
                    val dstColor = blurred[index]
                    dstR = dstColor shr 16 and 0xFF
                    dstG = dstColor shr 8 and 0xFF
                    dstB = dstColor and 0xFF
                    if (Math.abs(srcR - dstR) >= threshold) {
                        srcR = (amount * (srcR - dstR) + srcR).toInt()
                        srcR = if (srcR > 255) 255 else if (srcR < 0) 0 else srcR
                    }
                    if (Math.abs(srcG - dstG) >= threshold) {
                        srcG = (amount * (srcG - dstG) + srcG).toInt()
                        srcG = if (srcG > 255) 255 else if (srcG < 0) 0 else srcG
                    }
                    if (Math.abs(srcB - dstB) >= threshold) {
                        srcB = (amount * (srcB - dstB) + srcB).toInt()
                        srcB = if (srcB > 255) 255 else if (srcB < 0) 0 else srcB
                    }
                    val alpha = srcColor and -0x1000000
                    blurred[index] = alpha or (srcR shl 16) or (srcG shl 8) or srcB
                    index++
                }
            }
        }
    }
}

/**
 *
 * `GraphicsUtilities` contains a set of tools to perform
 * common graphics operations easily. These operations are divided into
 * several themes, listed below.
 * <h2>Compatible Images</h2>
 *
 * Compatible images can, and should, be used to increase drawing
 * performance. This class provides a number of methods to load compatible
 * images directly from files or to convert existing images to compatibles
 * images.
 * <h2>Creating Thumbnails</h2>
 *
 * This class provides a number of methods to easily scale down images.
 * Some of these methods offer a trade-off between speed and result quality and
 * shouuld be used all the time. They also offer the advantage of producing
 * compatible images, thus automatically resulting into better runtime
 * performance.
 *
 * All these methodes are both faster than
 * [java.awt.Image.getScaledInstance] and produce
 * better-looking results than the various `drawImage()` methods
 * in [java.awt.Graphics], which can be used for image scaling.
 * <h2>Image Manipulation</h2>
 *
 * This class provides two methods to get and set pixels in a buffered image.
 * These methods try to avoid unmanaging the image in order to keep good
 * performance.
 *
 * @author Romain Guy <romain.guy></romain.guy>@mac.com>
 */
internal object GraphicsUtilities {
    // Returns the graphics configuration for the primary screen
    private val graphicsConfiguration: GraphicsConfiguration
        private get() = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration

    /**
     *
     * Returns a new `BufferedImage` using the same color model
     * as the image passed as a parameter. The returned image is only compatible
     * with the image passed as a parameter. This does not mean the returned
     * image is compatible with the hardware.
     *
     * @param image the reference image from which the color model of the new
     * image is obtained
     * @return a new `BufferedImage`, compatible with the color model
     * of `image`
     */
    fun createColorModelCompatibleImage(image: BufferedImage): BufferedImage {
        val cm = image.colorModel
        return BufferedImage(
            cm,
            cm.createCompatibleWritableRaster(
                image.width,
                image.height
            ),
            cm.isAlphaPremultiplied, null
        )
    }
    /**
     *
     * Returns a new compatible image of the specified width and height, and
     * the same transparency setting as the image specified as a parameter.
     *
     * @see java.awt.Transparency
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleTranslucentImage
     * @see .loadCompatibleImage
     * @see .toCompatibleImage
     * @param width the width of the new image
     * @param height the height of the new image
     * @param image the reference image from which the transparency of the new
     * image is obtained
     * @return a new compatible `BufferedImage` with the same
     * transparency as `image` and the specified dimension
     */
    /**
     *
     * Returns a new compatible image with the same width, height and
     * transparency as the image specified as a parameter.
     *
     * @see java.awt.Transparency
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleTranslucentImage
     * @see .loadCompatibleImage
     * @see .toCompatibleImage
     * @param image the reference image from which the dimension and the
     * transparency of the new image are obtained
     * @return a new compatible `BufferedImage` with the same
     * dimension and transparency as `image`
     */
    fun createCompatibleImage(
        image: BufferedImage,
        width: Int = image.width, height: Int = image.height
    ): BufferedImage {
        return graphicsConfiguration.createCompatibleImage(
            width, height,
            image.transparency
        )
    }

    /**
     *
     * Returns a new opaque compatible image of the specified width and
     * height.
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleTranslucentImage
     * @see .loadCompatibleImage
     * @see .toCompatibleImage
     * @param width the width of the new image
     * @param height the height of the new image
     * @return a new opaque compatible `BufferedImage` of the
     * specified width and height
     */
    fun createCompatibleImage(width: Int, height: Int): BufferedImage {
        return graphicsConfiguration.createCompatibleImage(width, height)
    }

    /**
     *
     * Returns a new translucent compatible image of the specified width
     * and height.
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .loadCompatibleImage
     * @see .toCompatibleImage
     * @param width the width of the new image
     * @param height the height of the new image
     * @return a new translucent compatible `BufferedImage` of the
     * specified width and height
     */
    fun createCompatibleTranslucentImage(
        width: Int,
        height: Int
    ): BufferedImage {
        return graphicsConfiguration.createCompatibleImage(
            width, height,
            Transparency.TRANSLUCENT
        )
    }

    /**
     *
     * Returns a new compatible image from a URL. The image is loaded from the
     * specified location and then turned, if necessary into a compatible
     * image.
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleTranslucentImage
     * @see .toCompatibleImage
     * @param resource the URL of the picture to load as a compatible image
     * @return a new translucent compatible `BufferedImage` of the
     * specified width and height
     * @throws java.io.IOException if the image cannot be read or loaded
     */
    
    fun loadCompatibleImage(resource: URL?): BufferedImage {
        val image = ImageIO.read(resource)
        return toCompatibleImage(image)
    }

    /**
     *
     * Return a new compatible image that contains a copy of the specified
     * image. This method ensures an image is compatible with the hardware,
     * and therefore optimized for fast blitting operations.
     *
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleImage
     * @see .createCompatibleTranslucentImage
     * @see .loadCompatibleImage
     * @param image the image to copy into a new compatible image
     * @return a new compatible copy, with the
     * same width and height and transparency and content, of `image`
     */
    fun toCompatibleImage(image: BufferedImage): BufferedImage {
        if (image.colorModel ==
            graphicsConfiguration.colorModel
        ) {
            return image
        }
        val compatibleImage = graphicsConfiguration.createCompatibleImage(
            image.width, image.height,
            image.transparency
        )
        val g = compatibleImage.graphics
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return compatibleImage
    }

    /**
     *
     * Returns a thumbnail of a source image. `newSize` defines
     * the length of the longest dimension of the thumbnail. The other
     * dimension is then computed according to the dimensions ratio of the
     * original picture.
     *
     * This method favors speed over quality. When the new size is less than
     * half the longest dimension of the source image,
     * [.createThumbnail] or
     * [.createThumbnail] should be used instead
     * to ensure the quality of the result without sacrificing too much
     * performance.
     *
     * @see .createThumbnailFast
     * @see .createThumbnail
     * @see .createThumbnail
     * @param image the source image
     * @param newSize the length of the largest dimension of the thumbnail
     * @return a new compatible `BufferedImage` containing a
     * thumbnail of `image`
     * @throws IllegalArgumentException if `newSize` is larger than
     * the largest dimension of `image` or &lt;= 0
     */
    fun createThumbnailFast(
        image: BufferedImage,
        newSize: Int
    ): BufferedImage {
        val ratio: Float
        var width = image.width
        var height = image.height
        if (width > height) {
            require(newSize < width) {
                "newSize must be lower than" +
                        " the image width"
            }
            require(newSize > 0) {
                "newSize must" +
                        " be greater than 0"
            }
            ratio = width.toFloat() / height.toFloat()
            width = newSize
            height = (newSize / ratio).toInt()
        } else {
            require(newSize < height) {
                "newSize must be lower than" +
                        " the image height"
            }
            require(newSize > 0) {
                "newSize must" +
                        " be greater than 0"
            }
            ratio = height.toFloat() / width.toFloat()
            height = newSize
            width = (newSize / ratio).toInt()
        }
        val temp = createCompatibleImage(image, width, height)
        val g2 = temp.createGraphics()
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g2.drawImage(image, 0, 0, temp.width, temp.height, null)
        g2.dispose()
        return temp
    }

    /**
     *
     * Returns a thumbnail of a source image.
     *
     * This method favors speed over quality. When the new size is less than
     * half the longest dimension of the source image,
     * [.createThumbnail] or
     * [.createThumbnail] should be used instead
     * to ensure the quality of the result without sacrificing too much
     * performance.
     *
     * @see .createThumbnailFast
     * @see .createThumbnail
     * @see .createThumbnail
     * @param image the source image
     * @param newWidth the width of the thumbnail
     * @param newHeight the height of the thumbnail
     * @return a new compatible `BufferedImage` containing a
     * thumbnail of `image`
     * @throws IllegalArgumentException if `newWidth` is larger than
     * the width of `image` or if code>newHeight is larger
     * than the height of `image` or if one of the dimensions
     * is &lt;= 0
     */
    fun createThumbnailFast(
        image: BufferedImage,
        newWidth: Int, newHeight: Int
    ): BufferedImage {
        require(
            !(newWidth >= image.width ||
                    newHeight >= image.height)
        ) {
            "newWidth and newHeight cannot" +
                    " be greater than the image" +
                    " dimensions"
        }
        require(!(newWidth <= 0 || newHeight <= 0)) {
            "newWidth and newHeight must" +
                    " be greater than 0"
        }
        val temp = createCompatibleImage(image, newWidth, newHeight)
        val g2 = temp.createGraphics()
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g2.drawImage(image, 0, 0, temp.width, temp.height, null)
        g2.dispose()
        return temp
    }

    /**
     *
     * Returns a thumbnail of a source image. `newSize` defines
     * the length of the longest dimension of the thumbnail. The other
     * dimension is then computed according to the dimensions ratio of the
     * original picture.
     *
     * This method offers a good trade-off between speed and quality.
     * The result looks better than
     * [.createThumbnailFast] when
     * the new size is less than half the longest dimension of the source
     * image, yet the rendering speed is almost similar.
     *
     * @see .createThumbnailFast
     * @see .createThumbnailFast
     * @see .createThumbnail
     * @param image the source image
     * @param newSize the length of the largest dimension of the thumbnail
     * @return a new compatible `BufferedImage` containing a
     * thumbnail of `image`
     * @throws IllegalArgumentException if `newSize` is larger than
     * the largest dimension of `image` or &lt;= 0
     */
    fun createThumbnail(
        image: BufferedImage,
        newSize: Int
    ): BufferedImage? {
        var width = image.width
        var height = image.height
        val isWidthGreater = width > height
        if (isWidthGreater) {
            require(newSize < width) {
                "newSize must be lower than" +
                        " the image width"
            }
        } else require(newSize < height) {
            "newSize must be lower than" +
                    " the image height"
        }
        require(newSize > 0) {
            "newSize must" +
                    " be greater than 0"
        }
        val ratioWH = width.toFloat() / height.toFloat()
        val ratioHW = height.toFloat() / width.toFloat()
        var thumb: BufferedImage? = image
        var temp: BufferedImage? = null
        var g2: Graphics2D? = null
        var previousWidth = width
        var previousHeight = height
        do {
            if (isWidthGreater) {
                width /= 2
                if (width < newSize) {
                    width = newSize
                }
                height = (width / ratioWH).toInt()
            } else {
                height /= 2
                if (height < newSize) {
                    height = newSize
                }
                width = (height / ratioHW).toInt()
            }
            if (temp == null) {
                temp = createCompatibleImage(image, width, height)
                g2 = temp.createGraphics()
                g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
            }
            g2!!.drawImage(
                thumb, 0, 0, width, height,
                0, 0, previousWidth, previousHeight, null
            )
            previousWidth = width
            previousHeight = height
            thumb = temp
        } while (newSize != if (isWidthGreater) width else height)
        g2!!.dispose()
        if (width != thumb!!.width || height != thumb.height) {
            temp = createCompatibleImage(image, width, height)
            g2 = temp.createGraphics()
            g2.drawImage(thumb, 0, 0, null)
            g2.dispose()
            thumb = temp
        }
        return thumb
    }

    /**
     *
     * Returns a thumbnail of a source image.
     *
     * This method offers a good trade-off between speed and quality.
     * The result looks better than
     * [.createThumbnailFast] when
     * the new size is less than half the longest dimension of the source
     * image, yet the rendering speed is almost similar.
     *
     * @see .createThumbnailFast
     * @see .createThumbnailFast
     * @see .createThumbnail
     * @param image the source image
     * @param newWidth the width of the thumbnail
     * @param newHeight the height of the thumbnail
     * @return a new compatible `BufferedImage` containing a
     * thumbnail of `image`
     * @throws IllegalArgumentException if `newWidth` is larger than
     * the width of `image` or if code>newHeight is larger
     * than the height of `image or if one the dimensions is not > 0`
     */
    fun createThumbnail(
        image: BufferedImage,
        newWidth: Int, newHeight: Int
    ): BufferedImage? {
        var width = image.width
        var height = image.height
        require(!(newWidth >= width || newHeight >= height)) {
            "newWidth and newHeight cannot" +
                    " be greater than the image" +
                    " dimensions"
        }
        require(!(newWidth <= 0 || newHeight <= 0)) {
            "newWidth and newHeight must" +
                    " be greater than 0"
        }
        var thumb: BufferedImage? = image
        var temp: BufferedImage? = null
        var g2: Graphics2D? = null
        var previousWidth = width
        var previousHeight = height
        do {
            if (width > newWidth) {
                width /= 2
                if (width < newWidth) {
                    width = newWidth
                }
            }
            if (height > newHeight) {
                height /= 2
                if (height < newHeight) {
                    height = newHeight
                }
            }
            if (temp == null) {
                temp = createCompatibleImage(image, width, height)
                g2 = temp.createGraphics()
                g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
            }
            g2!!.drawImage(
                thumb, 0, 0, width, height,
                0, 0, previousWidth, previousHeight, null
            )
            previousWidth = width
            previousHeight = height
            thumb = temp
        } while (width != newWidth || height != newHeight)
        g2!!.dispose()
        if (width != thumb!!.width || height != thumb.height) {
            temp = createCompatibleImage(image, width, height)
            g2 = temp.createGraphics()
            g2.drawImage(thumb, 0, 0, null)
            g2.dispose()
            thumb = temp
        }
        return thumb
    }

    /**
     *
     * Returns an array of pixels, stored as integers, from a
     * `BufferedImage`. The pixels are grabbed from a rectangular
     * area defined by a location and two dimensions. Calling this method on
     * an image of type different from `BufferedImage.TYPE_INT_ARGB`
     * and `BufferedImage.TYPE_INT_RGB` will unmanage the image.
     *
     * @param img the source image
     * @param x the x location at which to start grabbing pixels
     * @param y the y location at which to start grabbing pixels
     * @param w the width of the rectangle of pixels to grab
     * @param h the height of the rectangle of pixels to grab
     * @param pixels a pre-allocated array of pixels of size w*h; can be null
     * @return `pixels` if non-null, a new array of integers
     * otherwise
     * @throws IllegalArgumentException is `pixels` is non-null and
     * of length &lt; w*h
     */
    fun getPixels(
        img: BufferedImage,
        x: Int, y: Int, w: Int, h: Int, pixels: IntArray?
    ): IntArray {
        var pixels = pixels
        if (w == 0 || h == 0) {
            return IntArray(0)
        }
        if (pixels == null) {
            pixels = IntArray(w * h)
        } else require(pixels.size >= w * h) {
            "pixels array must have a length" +
                    " >= w*h"
        }
        val imageType = img.type
        if (imageType == BufferedImage.TYPE_INT_ARGB ||
            imageType == BufferedImage.TYPE_INT_RGB
        ) {
            val raster: Raster = img.raster
            return raster.getDataElements(x, y, w, h, pixels) as IntArray
        }

        // Unmanages the image
        return img.getRGB(x, y, w, h, pixels, 0, w)
    }

    /**
     *
     * Writes a rectangular area of pixels in the destination
     * `BufferedImage`. Calling this method on
     * an image of type different from `BufferedImage.TYPE_INT_ARGB`
     * and `BufferedImage.TYPE_INT_RGB` will unmanage the image.
     *
     * @param img the destination image
     * @param x the x location at which to start storing pixels
     * @param y the y location at which to start storing pixels
     * @param w the width of the rectangle of pixels to store
     * @param h the height of the rectangle of pixels to store
     * @param pixels an array of pixels, stored as integers
     * @throws IllegalArgumentException is `pixels` is non-null and
     * of length &lt; w*h
     */
    fun setPixels(
        img: BufferedImage,
        x: Int, y: Int, w: Int, h: Int, pixels: IntArray?
    ) {
        if (pixels == null || w == 0 || h == 0) {
            return
        } else require(pixels.size >= w * h) {
            "pixels array must have a length" +
                    " >= w*h"
        }
        val imageType = img.type
        if (imageType == BufferedImage.TYPE_INT_ARGB ||
            imageType == BufferedImage.TYPE_INT_RGB
        ) {
            val raster = img.raster
            raster.setDataElements(x, y, w, h, pixels)
        } else {
            // Unmanages the image
            img.setRGB(x, y, w, h, pixels, 0, w)
        }
    }
}

internal class GaussianBlurFilter constructor(radius: Int = 3) : AbstractFilter() {
    /**
     *
     * Returns the radius used by this filter, in pixels.
     *
     * @return the radius of the blur
     */
    val radius: Int
    /**
     *
     * Creates a new blur filter with the specified radius. If the radius
     * is lower than 0, a radius of 0.1 will be used automatically.
     *
     * @param radius the radius, in pixels, of the blur
     */
    /**
     *
     * Creates a new blur filter with a default radius of 3.
     */
    init {
        var radius = radius
        if (radius < 1) {
            radius = 1
        }
        this.radius = radius
    }

    /**
     * {@inheritDoc}
     */
    override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
        var dst = dst
        val width = src.width
        val height = src.height
        if (dst == null) {
            dst = createCompatibleDestImage(src, null)
        }
        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        val kernel = createGaussianKernel(radius)
        GraphicsUtilities.getPixels(src, 0, 0, width, height, srcPixels)
        // horizontal pass
        blur(srcPixels, dstPixels, width, height, kernel, radius)
        // vertical pass
        blur(dstPixels, srcPixels, height, width, kernel, radius)
        // the result is now stored in srcPixels due to the 2nd pass
        GraphicsUtilities.setPixels(dst, 0, 0, width, height, srcPixels)
        return dst
    }

    companion object {
        /**
         *
         * Blurs the source pixels into the destination pixels. The force of
         * the blur is specified by the radius which must be greater than 0.
         *
         * The source and destination pixels arrays are expected to be in the
         * INT_ARGB format.
         *
         * After this method is executed, dstPixels contains a transposed and
         * filtered copy of srcPixels.
         *
         * @param srcPixels the source pixels
         * @param dstPixels the destination pixels
         * @param width the width of the source picture
         * @param height the height of the source picture
         * @param kernel the kernel of the blur effect
         * @param radius the radius of the blur effect
         */
        fun blur(
            srcPixels: IntArray, dstPixels: IntArray,
            width: Int, height: Int,
            kernel: FloatArray, radius: Int
        ) {
            var a: Float
            var r: Float
            var g: Float
            var b: Float
            var ca: Int
            var cr: Int
            var cg: Int
            var cb: Int
            for (y in 0 until height) {
                var index = y
                val offset = y * width
                for (x in 0 until width) {
                    b = 0.0f
                    g = b
                    r = g
                    a = r
                    for (i in -radius..radius) {
                        var subOffset = x + i
                        if (subOffset < 0 || subOffset >= width) {
                            subOffset = (x + width) % width
                        }
                        val pixel = srcPixels[offset + subOffset]
                        val blurFactor = kernel[radius + i]
                        a += blurFactor * (pixel shr 24 and 0xFF)
                        r += blurFactor * (pixel shr 16 and 0xFF)
                        g += blurFactor * (pixel shr 8 and 0xFF)
                        b += blurFactor * (pixel and 0xFF)
                    }
                    ca = (a + 0.5f).toInt()
                    cr = (r + 0.5f).toInt()
                    cg = (g + 0.5f).toInt()
                    cb = (b + 0.5f).toInt()
                    dstPixels[index] = (if (ca > 255) 255 else ca) shl 24 or
                            ((if (cr > 255) 255 else cr) shl 16) or
                            ((if (cg > 255) 255 else cg) shl 8) or if (cb > 255) 255 else cb
                    index += height
                }
            }
        }

        fun createGaussianKernel(radius: Int): FloatArray {
            require(radius >= 1) { "Radius must be >= 1" }
            val data = FloatArray(radius * 2 + 1)
            val sigma = radius / 3.0f
            val twoSigmaSquare = 2.0f * sigma * sigma
            val sigmaRoot = Math.sqrt(twoSigmaSquare * Math.PI).toFloat()
            var total = 0.0f
            for (i in -radius..radius) {
                val distance = (i * i).toFloat()
                val index = i + radius
                data[index] = Math.exp((-distance / twoSigmaSquare).toDouble()).toFloat() / sigmaRoot
                total += data[index]
            }
            for (i in data.indices) {
                data[i] /= total
            }
            return data
        }
    }
}

/**
 *
 * Provides an abstract implementation of the `BufferedImageOp`
 * interface. This class can be used to created new image filters based
 * on `BufferedImageOp`.
 *
 * @author Romain Guy <romain.guy></romain.guy>@mac.com>
 */
abstract class AbstractFilter : BufferedImageOp {
    abstract override fun filter(src: BufferedImage, dest: BufferedImage?): BufferedImage

    /**
     * {@inheritDoc}
     */
    override fun getBounds2D(src: BufferedImage): Rectangle2D {
        return Rectangle(0, 0, src.width, src.height).toAwt()
    }

    /**
     * {@inheritDoc}
     */
    override fun createCompatibleDestImage(
        src: BufferedImage,
        destCM: ColorModel?
    ): BufferedImage {
        var destCM: ColorModel? = destCM
        if (destCM == null) {
            destCM = src.colorModel
        }
        return BufferedImage(
            destCM,
            destCM!!.createCompatibleWritableRaster(
                src.width, src.height
            ),
            destCM.isAlphaPremultiplied, null
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getPoint2D(srcPt: Point2D, dstPt: Point2D): Point2D {
        return srcPt.clone() as Point2D
    }

    /**
     * {@inheritDoc}
     */
    override fun getRenderingHints(): RenderingHints? {
        return null
    }
}
