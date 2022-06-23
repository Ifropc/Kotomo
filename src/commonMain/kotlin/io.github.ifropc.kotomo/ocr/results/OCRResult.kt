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
package io.github.ifropc.kotomo.ocr.results

import io.github.ifropc.kotomo.ocr.matrix.ReferenceMatrix
import io.github.ifropc.kotomo.ocr.matrix.TargetMatrix

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
        return """$character	score:$score	black:$blackPixels	white:$whitePixels	targetHalo:${
            printArray(
                targetHaloPixels
            )
        }	referenceHalo:${printArray(referenceHaloPixels)}
	modifier:${reference.scoreModifier}	transform:${target.transform}	font:${reference.fontName}"""
    }

    /**
     * Prints array as: [a,b,c,..]
     */
    private fun printArray(array: IntArray): String {
        var ret = "["
        for (i in array.indices) {
            ret += array[i]
            if (i < array.size - 1) {
                ret += ","
            }
        }
        ret += "]"
        return ret
    }
}
