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
package io.github.ifropc.kotomo.ocr

import io.github.ifropc.kotomo.util.ImageUtil.createWhiteImage
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Util.printArray
import java.awt.Color
import java.awt.image.BufferedImage
import java.math.BigInteger

/**
 * Result after alignment between target image and reference character
 */
class OCRResult(
    /**
     * Matrix that represents pixels in target image
     */
    var target: TargetMatrix,
    /**
     * Matrix that represents pixels in reference character
     */
    var reference: ReferenceMatrix
) {
    

    /**
     * Common pixels that are found in both target and reference images
     */
    var blackPixels = 0

    /**
     * Pixels not in either target or reference image
     */
    var whitePixels = 0

    /**
     * Pixels in target but not in reference image. Indexed by halo layer.
     */
    lateinit var targetHaloPixels: IntArray

    /**
     * Pixels in reference but not in targe image. Indexed by halo layer.
     */
    lateinit var referenceHaloPixels: IntArray
    /**
     * Get score for this alignment. Higher is better.
     */
    /**
     * Aligment score calculated from weighted sum of pixel types. Higher score is better.
     */
	var score = 0
    /**
     * Gets average score across all alignments. Higher is better.
     */
    /**
     * Average score across all transformations. Higher score is better.
     */
    var avgScore = 0f

    /**
     * If true, refined aligment was used that includes halo pixels (pink and gray).
     * If false, only normal alignment was used.
     */
    var refinedAligment = false

    /**
     * Get the reference character for this aligment.
     */
    val character: Char
        get() = reference.character

    override fun toString(): String {
        return """${character}	score:$score	black:$blackPixels	white:$whitePixels	targetHalo:${
            printArray(
                targetHaloPixels
            )
        }	referenceHalo:${printArray(referenceHaloPixels)}
	modifier:${reference.scoreModifier}	transform:${target.transform}	font:${reference.fontName}"""
    }

    fun buildDebugImage(): BufferedImage {
        return if (refinedAligment) {
            buildDebugImageRefined()
        } else {
            buildDebugImageBasic()
        }
    }

    fun buildDebugImageBasic(): BufferedImage {
        val image = createWhiteImage(32, 32)
        addLayer(image, Color.BLACK, target.matrix, reference.matrix)
        addLayer(image, Parameters.ocrTargetHaloFirstColor, target.matrix)
        addLayer(image, Parameters.ocrReferenceHaloFirstColor, reference.matrix)
        return image
    }

    fun buildDebugImageRefined(): BufferedImage {
        val image = createWhiteImage(32, 32)
        addLayer(image, Color.BLACK, target.matrix, reference.matrix)
        for (i in 1 until Parameters.ocrHaloSize) {
            val col = interpolate(Parameters.ocrTargetHaloFirstColor, Parameters.ocrTargetHaloLastColor, i)
            addLayer(image, col, target.matrix, reference.halo!![i - 1])
        }
        for (i in 1 until Parameters.ocrHaloSize) {
            val col = interpolate(Parameters.ocrReferenceHaloFirstColor, Parameters.ocrReferenceHaloLastColor, i)
            addLayer(image, col, reference.matrix, target.halo!![i - 1])
        }
        addLayer(image, Parameters.ocrTargetHaloLastColor, target.matrix)
        addLayer(image, Parameters.ocrReferenceHaloLastColor, reference.matrix)
        return image
    }

    /**
     * Paints pixels that can be found in both matrixes with color.
     * Only overwrites white pixels.
     */
    private fun addLayer(image: BufferedImage, color: Color, matrix1: IntArray, matrix2: IntArray) {
        for (y in 0..31) {
            val paintPixels = BigInteger.valueOf((matrix1[y] and matrix2[y]).toLong())
            for (x in 0..31) {
                if (paintPixels.testBit(31 - x) && image.getRGB(x, y) == Color.WHITE.rgb) {
                    image.setRGB(x, y, color.rgb)
                }
            }
        }
    }

    /**
     * Paints pixels that can be found in the matrix.
     * Only overwrites white pixels.
     */
    private fun addLayer(image: BufferedImage, color: Color, matrix: IntArray) {
        for (y in 0..31) {
            val paintPixels = BigInteger.valueOf(matrix[y].toLong())
            for (x in 0..31) {
                if (paintPixels.testBit(31 - x) && image.getRGB(x, y) == Color.WHITE.rgb) {
                    image.setRGB(x, y, color.rgb)
                }
            }
        }
    }

    /**
     * Interpolates halo colors
     */
    private fun interpolate(col1: Color, col2: Color, layer: Int): Color {
        if (layer == 1) {
            return col1
        }
        if (layer >= Parameters.ocrHaloSize) {
            return col2
        }
        val red = interpolate(col1.red, col2.red, layer)
        val green = interpolate(col1.green, col2.green, layer)
        val blue = interpolate(col1.blue, col2.blue, layer)
        return Color(red, green, blue)
    }

    /**
     * Interpolates halo colors (single channel)
     */
    private fun interpolate(rgb1: Int, rgb2: Int, layer: Int): Int {
        val delta = rgb2 - rgb1
        val ratio = 1.0f * (layer - 1) / (Parameters.ocrHaloSize - 1)
        return rgb1 + Math.round(ratio * delta)
    }
}
