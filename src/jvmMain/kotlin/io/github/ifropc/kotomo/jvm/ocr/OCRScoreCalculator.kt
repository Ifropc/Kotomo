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
package io.github.ifropc.kotomo.jvm.ocr

import io.github.ifropc.kotomo.config.Parameters
import io.github.ifropc.kotomo.ocr.matrix.ReferenceMatrix
import io.github.ifropc.kotomo.ocr.matrix.TargetMatrix
import io.github.ifropc.kotomo.ocr.results.OCRResult
import kotlin.math.floor

/**
 * Calculates OCR score by comparing target and reference matrices for a given alignment
 * and calculating the number of common and nearby pixels.
 */
class OCRScoreCalculator {
    
    private var target: TargetMatrix? = null
    private var reference: ReferenceMatrix? = null
    private var blackPixels = 0
    private var whitePixels = 0
    private lateinit var targetHaloPixels: IntArray
    private lateinit var referenceHaloPixels: IntArray
    private var score = 0
    private var refined = false

    /**
     * Calculates OCR score for given alignment
     *
     * @param halo If false, considers only common pixels between matrices.
     * If true, considers also nearby pixels (halo matrices). This is slower but more accurate.
     */
    fun calcScore(target: TargetMatrix?, reference: ReferenceMatrix?, halo: Boolean): OCRResult {
        this.target = target
        this.reference = reference
        refined = halo
        align()
        if (halo) {
            refine()
        }
        calcScore()
        return buildResult()
    }

    /**
     * Aligns target and reference matrices.
     */
    private fun align() {
        blackPixels = 0
        for (y in 0..31) {
            val merged = target!!.matrix[y] and reference!!.matrix[y]
            val pixels = Integer.bitCount(merged)
            blackPixels += pixels
        }
        targetHaloPixels = intArrayOf(target!!.pixels - blackPixels)
        referenceHaloPixels = intArrayOf(reference!!.pixels - blackPixels)
        whitePixels = 32 * 32 - blackPixels - targetHaloPixels[0] - referenceHaloPixels[0]
    }

    /**
     * Aligns halo matrices
     */
    private fun refine() {
        val targetMatrix = target!!.matrix.copyOf()
        val referenceMatrix = reference!!.matrix.copyOf()
        var targetHaloRemaining = targetHaloPixels[0]
        var referenceHaloRemaining = referenceHaloPixels[0]
        targetHaloPixels = IntArray(Parameters.ocrHaloSize)
        referenceHaloPixels = IntArray(Parameters.ocrHaloSize)
        for (i in 0 until Parameters.ocrHaloSize - 1) {
            val referenceHalo = reference!!.halo!![i]
            for (y in 0..31) {
                val merged = targetMatrix[y] and referenceHalo[y]
                val pixels = Integer.bitCount(merged)
                targetHaloPixels[i] += pixels
                targetHaloRemaining -= pixels
                targetMatrix[y] = targetMatrix[y] or referenceHalo[y]
            }
            val targetHalo = target!!.halo!![i]
            for (y in 0..31) {
                val merged = referenceMatrix[y] and targetHalo[y]
                val pixels = Integer.bitCount(merged)
                referenceHaloPixels[i] += pixels
                referenceHaloRemaining -= pixels
                referenceMatrix[y] = referenceMatrix[y] or targetHalo[y]
            }
        }
        targetHaloPixels[Parameters.ocrHaloSize - 1] = targetHaloRemaining
        referenceHaloPixels[Parameters.ocrHaloSize - 1] = referenceHaloRemaining
    }

    /**
     * Calculates score from pixel values
     */
    private fun calcScore() {
        score =
            floor((Parameters.ocrBaseScore + blackPixels * Parameters.ocrBlackPixelScore + whitePixels * Parameters.ocrWhiteScore).toDouble())
                .toInt()
        for (i in targetHaloPixels.indices) {
            score += floor((targetHaloPixels[i] * Parameters.ocrTargetHaloScores[i]).toDouble()).toInt()
        }
        for (i in referenceHaloPixels.indices) {
            score += floor((referenceHaloPixels[i] * Parameters.ocrReferenceHaloScores[i]).toDouble()).toInt()
        }
        if (score > 1f) {
            score = (reference!!.scoreModifier * score).toInt()
        }
    }

    /**
     * Builds result object from components
     */
    private fun buildResult(): OCRResult {
        val result = OCRResult(target!!, reference!!)
        result.blackPixels = blackPixels
        result.whitePixels = whitePixels
        result.targetHaloPixels = targetHaloPixels
        result.referenceHaloPixels = referenceHaloPixels
        result.score = score
        result.refinedAligment = refined
        return result
    }
}
