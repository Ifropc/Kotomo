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

import io.github.ifropc.kotomo.util.*
import io.github.ifropc.kotomo.util.ImageUtil.buildScaledImage
import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import java.io.File
import java.util.*
import javax.imageio.ImageIO

/**
 * Compares character components (radicals) against target bitmap. Iterates through
 * different component transformations and selects best matches.
 */
class OCRAlignComponents {
    private val par = instance
    private val calculator = OCRScoreCalculator()

    /** Extra debug images are saved for this character  */
    private val debugCharacter: Char? = null

    /**
     * Runs component alignment for each result.
     *
     * @return Improved results in descending score order.
     */
    
    fun run(results: List<OCRResult>?): List<OCRResult> {
        val started = System.currentTimeMillis()
        if (par.isPrintDebug) {
            print("Component alignment ")
        }
        val newResults: MutableList<OCRResult> = ArrayList()
        for (result in results!!) {
            newResults.add(alignComponents(result))
        }
        Collections.sort(newResults) { o1, o2 ->

            // TODO define comparator in OCRResult object and re-use
            val score1 = o1.score
            val score2 = o2.score
            -1 * score1.compareTo(score2)
        }
        val done = System.currentTimeMillis()
        if (par.isPrintDebug) {
            println((done - started).toString() + " ms")
        }
        return newResults
    }

    /**
     * Improve result by applying transformations to reference character components.
     */
    
    fun alignComponents(result: OCRResult): OCRResult {
        val maxDelta = 1
        val maxStretch = 4

        // current best score represents default (no-op) transformation
        var bestScore = result.score.toFloat()
        val reference = buildNewReferenceMatrix(result.reference)

        // find best translation for each component. this doesn't guarantee globally optimal solution 
        // but is close and it's not possible to consider all combinations.
        // TODO try also reverse order?
        for (i in result.reference.components!!.indices) {
            var bestTransformation: Transformation? = reference.transformations!![i]

            // generate transformations
            for (deltaX in -maxDelta..maxDelta) {
                for (deltaY in -maxDelta..maxDelta) {
                    for (stretchX in 0..maxStretch) {
                        for (stretchY in 0..maxStretch) {

                            // skip combined strech
                            if (stretchX != 0 && stretchY != 0) {
                                continue  // TODO implement in MatrixUtil
                            }

                            // run alignment with generated transformation
                            val transformation = Transformation(deltaX, deltaY, stretchX, stretchY)
                            reference.transformations!![i] = transformation
                            val tempResult = align(result.target, reference)

                            // check if new result is better than old
                            if (tempResult.score > bestScore) {
                                bestScore = tempResult.score.toFloat()
                                bestTransformation = transformation
                            }

                            // debug if test character
                            if (debugCharacter != null && reference.character == debugCharacter) {
                                writeDebugImage(tempResult, transformation)
                            }
                        }
                    }
                }
            }

            // restore best transformation for this component
            reference.transformations!![i] = bestTransformation!!
        }

        // restore best result
        return align(result.target, reference)
    }

    /**
     * Creates a new reference matrix that includes default (no-op) transformations
     */
    private fun buildNewReferenceMatrix(reference: ReferenceMatrix): ReferenceMatrix {
        val newReference = reference.clone()
        newReference.transformations = ArrayList()
        for (i in reference.components!!.indices) {
            newReference.transformations!!.add(Transformation())
        }
        return newReference
    }

    /**
     * Aligns target and reference using component tranformations
     */
    private fun align(target: TargetMatrix, reference: ReferenceMatrix): OCRResult {

        // build new matrix by applying transformations to each component
        reference.matrix = IntArray(32)
        for (i in reference.components!!.indices) {
            val component = reference.components!![i]
            val transform = reference.transformations!![i]
            applyTransformation(reference, component, transform)
        }

        // halo and pixel count can change after translations
        reference.halo = buildMatrixHalo(reference.matrix, Parameters.ocrHaloSize - 1)
        reference.pixels = countBits(reference.matrix)

        // calculate score for new matrix
        return calculator.calcScore(target, reference, true)
    }

    /**
     * Updates reference matrix by applying transformation to component
     */
    private fun applyTransformation(reference: ReferenceMatrix, component: Component, transform: Transformation) {
        if (transform.horizontalStretch == 0 && transform.verticalStretch == 0) {
            copyBits(
                component.matrix, reference.matrix, component.bounds!!.toAwt(),
                transform.horizontalTranslate, transform.verticalTranslate, false
            )
        } else {
            val stretchedMatrix = IntArray(32)
            val newBounds = stretchBits(
                component.matrix, stretchedMatrix,
                component.bounds!!.toAwt(), transform.horizontalStretch, transform.verticalStretch
            )
            copyBits(
                stretchedMatrix, reference.matrix, newBounds!!,
                transform.horizontalTranslate, transform.verticalTranslate, false
            )
        }
    }

    
    private fun writeDebugImage(result: OCRResult, transform: Transformation) {
        val transformStr = transform.horizontalTranslate.toString() + "," + transform.verticalTranslate
        val file = File(
            par.debugDir.absolutePath + "/" +
                    par.getDebugFilePrefix(result.target.charIndex) + ".step3." + result.character + "." +
                    result.score + ".(" + transformStr + ").png"
        )
        val scaledImage = buildScaledImage(result.buildDebugImage(), par.debugOCRImageScale)
        ImageIO.write(scaledImage, "png", file)
    }
}
