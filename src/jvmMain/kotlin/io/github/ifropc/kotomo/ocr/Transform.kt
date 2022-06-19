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

import io.github.ifropc.kotomo.util.ImageUtil.buildImage
import io.github.ifropc.kotomo.util.ImageUtil.buildMatrix32
import io.github.ifropc.kotomo.util.ImageUtil.buildScaledImage
import io.github.ifropc.kotomo.util.ImageUtil.createSquareImage
import io.github.ifropc.kotomo.util.ImageUtil.makeBlackAndWhite
import io.github.ifropc.kotomo.util.ImageUtil.sharpenImage
import io.github.ifropc.kotomo.util.ImageUtil.stretch
import io.github.ifropc.kotomo.util.ImageUtil.stretchCheckRatio
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.buildMatrixHalo
import io.github.ifropc.kotomo.util.countBits
import io.github.ifropc.kotomo.util.moveMatrix
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Applies stretch and offset transformations to target image. Caches intermediate
 * results for fast re-use.
 */
class Transform(private val task: OCRTask) {
    

    /**
     * Matrix representations of stretched target image with various stretch amounts.
     */
    private val stretchedMatrices: MutableMap<Transformation, IntArray> = HashMap()

    /**
     * Image fitted to target size and sharpened
     */
    private val image: BufferedImage

    /**
     * Iterates through translate and stretch combinations and creates transformed images.
     * Increasing argument values might produce better aligment at the cost of
     * execution time.
     *
     * @param maxTranslate Maximum number of pixels translated (up/down/left/right)
     * @param maxStretch Maximum number of pixels image is scaled
     * @param maxSteps Maximum number of translate and stretch steps allowed in total
     */
    
    fun run(maxTranslate: Int, maxStretch: Int, maxSteps: Int): List<TargetMatrix> {
        val targets: MutableList<TargetMatrix> = ArrayList()
        for (ht in -maxTranslate..maxTranslate) {
            for (vt in -maxTranslate..maxTranslate) {
                for (hs in -maxStretch..maxStretch) {
                    for (vs in -maxStretch..maxStretch) {
                        if (Math.abs(ht) + Math.abs(vt) + Math.abs(hs) + Math.abs(vs) > maxSteps) {
                            continue
                        }
                        if (Math.ceil((hs / 2.0f).toDouble()) + Math.abs(ht) > (32 - Parameters.targetSize) / 2) {
                            continue
                        }
                        if (Math.ceil((vs / 2.0f).toDouble()) + Math.abs(vt) > (32 - Parameters.targetSize) / 2) {
                            continue
                        }
                        val parameters = Transformation(ht, vt, hs, vs)
                        targets.add(transform(image, parameters))
                    }
                }
            }
        }
        return targets
    }

    private val writeDebugImages = false

    /**
     * Gets target bitmap without any transformations
     */
    var defaultTarget: TargetMatrix? = null
        private set

    init {

        // resize the image to target size 
        val resizedImage = stretchCheckRatio(task.image, Parameters.targetSize, Parameters.targetSize)

        // image has already been sharpened once during area detection but resize might bring
        // back gray edges, so sharpen again
        image = sharpenImage(resizedImage)
    }

    /**
     * Applies tranformations to the source image. Builds matrix representation.
     */
    
    private fun transform(image: BufferedImage, parameters: Transformation): TargetMatrix {
        val target = TargetMatrix()
        target.matrix = buildMatrix(image, parameters)!!
        target.halo = buildMatrixHalo(target.matrix, Parameters.ocrHaloSize - 1)
        target.pixels = countBits(target.matrix)
        target.charIndex = task.charIndex
        target.transform = parameters
        if (writeDebugImages) {
            writeDebugImage(target, parameters)
        }
        if (defaultTarget == null && parameters.contains(0, 0, 0, 0)) {
            defaultTarget = target
        }
        return target
    }

    /**
     * Builds binary matrix from image with given transformations
     */
    private fun buildMatrix(
        image: BufferedImage,
        parameters: Transformation
    ): IntArray? {
        val stretchedMatrix = stretchImage(image, parameters)
        return translateMatrix(stretchedMatrix, parameters)
    }

    /**
     * Stretches image with given transformations and fits to 32x32 square. Converts
     * image to matrix form.
     */
    private fun stretchImage(image: BufferedImage, parameters: Transformation): IntArray {
        val horizontalStretch = parameters.horizontalStretch
        val verticalStretch = parameters.verticalStretch

        // this is slow operation so cache results		
        val stretchAmount = Transformation(0, 0, horizontalStretch, verticalStretch)
        var stretchedMatrix = stretchedMatrices[stretchAmount]
        if (stretchedMatrix == null) {
            val newWidth = Parameters.targetSize + horizontalStretch
            val newHeight = Parameters.targetSize + verticalStretch
            val grayscale = stretch(image, newWidth, newHeight)
            val squareGrayscale = createSquareImage(grayscale, 32)
            val squareBWImage = makeBlackAndWhite(squareGrayscale, Parameters.pixelRGBThreshold)
            stretchedMatrix = buildMatrix32(squareBWImage)
            stretchedMatrices[stretchAmount] = stretchedMatrix
        }
        return stretchedMatrix
    }

    /**
     * Translates matrix with given transformations and crops to 32x32
     */
    private fun translateMatrix(matrix: IntArray, parameters: Transformation): IntArray? {
        return moveMatrix(
            matrix, parameters.horizontalTranslate,
            parameters.verticalTranslate
        )
    }

    
    private fun writeDebugImage(target: TargetMatrix, parameters: Transformation) {
        val file = File(
            Parameters.debugDir.absolutePath + "/" +
                    Parameters.debugFilePrefix + ".ocr.transform." + parameters + ".png"
        )
        val image = buildImage(target.matrix)
        val scaledImage = buildScaledImage(image, Parameters.debugOCRImageScale)
        ImageIO.write(scaledImage, "png", file)
    }
}
