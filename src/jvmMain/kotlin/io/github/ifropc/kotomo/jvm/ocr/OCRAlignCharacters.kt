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
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * Compares reference characters against target bitmap. Iterates through different alignments
 * and selects best matches.
 */
class OCRAlignCharacters(task: OCRTask?, private val transform: Transform) {

    private val scoreCalculator: OCRScoreCalculator
    private var topN = 0
    private var expectedCharacter: Char? = null

    /** If true, expected character is always included in results  */
    private val forceExpectedCharacter = false

    /**
     * Finds best matching characters
     *
     * @param characters Limits search to these characters. If null, considers all characters.
     * @param refined If true, uses more accurate but slower algorithm
     * @param maxTranslate Maximum number of pixels translated (up/down/left/right)
     * @param maxStretch Maximum number of pixels image is scaled
     * @param maxSteps Maximum number of translate and stretch steps allowed in total
     * @param topN Returns only the best N results
     */

    fun run(
        characters: Set<Char?>?, refined: Boolean,
        maxTranslate: Int, maxStretch: Int, maxSteps: Int, topN: Int
    ): List<OCRResult>? {
        var bestResults: List<OCRResult>? = null
        this.topN = topN
        val started = System.currentTimeMillis()
        log.debug { "${if (!refined) "Basic" else "Refined"} alignment" }

        // generate OCR targets by applying offset and stretch transformations to target bitmap
        val targets = transform.run(maxTranslate, maxStretch, maxSteps)

        // align reference bitmaps with target bitmaps
        // calculate score for each aligment and keep topN best
        for (font in getFonts(refined)) {
            val references = loadReferences(font, characters)
            val results = findBestAlignment(targets, references, refined)
            bestResults = combineResults(bestResults, results, topN)
        }
        log.debug { ((System.currentTimeMillis() - started).toString() + " ms") }
        return bestResults
    }

    /**
     * Aligns all targets and references, calculates score for each alignment
     * and returns best results.
     *
     * @param refined Include pixels that are close to target image but not
     * an exact match. This is slower but produces more accurate results.
     */

    private fun findBestAlignment(
        targets: List<TargetMatrix>,
        references: List<ReferenceMatrix>, refined: Boolean
    ): List<OCRResult> {
        val queue = OCRResultPriorityQueue(topN)
        if (forceExpectedCharacter) {
            queue.setExpectedCharacter(expectedCharacter)
        }

        // align all target and reference combinations
        for (reference in references) {
            var bestResult: OCRResult? = null
            for (target in targets) {
                val result = scoreCalculator.calcScore(target, reference, refined)
                if (bestResult == null || result.score > bestResult.score) {
                    bestResult = result
                }
            }
            queue.add(bestResult!!)
        }
        return queue.results
    }

    /**
     * Gets fonts that should be used as OCR references.
     *
     * @param refined If false, only returns the primary font. If true, also returns
     * secondary fonts.
     */
    private fun getFonts(refined: Boolean): List<String> {
        val fonts: MutableList<String> = ArrayList()

        // add primary font
        fonts.add(Parameters.referenceFonts[0])

        // add secondary fonts
        if (refined) {
            for (i in 1 until Parameters.referenceFonts.size) {
                fonts.add(Parameters.referenceFonts[i])
            }
        }
        return fonts
    }

    /**
     * Loads reference bitmaps
     *
     * @param character Returns bitmaps only for these characters. If null, all characters.
     */

    private fun loadReferences(font: String, characters: Set<Char?>?): List<ReferenceMatrix> {
        if (characters == null) {
            return loadReferences(font)
        }
        val references: MutableList<ReferenceMatrix> = ArrayList()
        for (reference in loadReferences(font)) {
            if (characters.contains(reference.character)) {
                references.add(reference)
            }
        }
        return references
    }

    init {
        scoreCalculator = OCRScoreCalculator()
        if (Parameters.expectedCharacters != null) {
            expectedCharacter = Parameters.expectedCharacters!![task!!.charIndex!!]
        }
    }

    /**
     * Loads reference bitmaps from cache file or from memory if called before.
     */

    private fun loadReferences(font: String): List<ReferenceMatrix> {
        if (cache == null) {
            val loader = ReferenceMatrixCacheLoader
            ReferenceMatrixCacheLoader.load()
            cache = ReferenceMatrixCacheLoader.cache
        }
        return cache!![font]
    }

    /**
     * Combines OCR results into single list. Keeps only the best score for each character.
     */
    private fun combineResults(
        results1: List<OCRResult>?, results2: List<OCRResult>?,
        maxSize: Int
    ): List<OCRResult>? {
        if (results1 == null && results2 == null) {
            throw Error("Both results are null")
        } else if (results1 == null) {
            return results2
        } else if (results2 == null) {
            return results1
        }
        val bestScores: MutableMap<Char, OCRResult> = HashMap()
        for (result1 in results1) {
            bestScores[result1.character] = result1
        }
        for (result2 in results2) {
            val c = result2.character
            val result1 = bestScores[c]
            if (result1 == null || result1.score < result2.score) {
                bestScores[c] = result2
            }
        }
        val results: MutableList<OCRResult> = ArrayList()
        results.addAll(bestScores.values)
        results.sortWith(Comparator { o1, o2 -> -1 * o1.score.compareTo(o2.score) })
        if (forceExpectedCharacter) {
            var removedExpectedResult: OCRResult? = null
            while (results.size > maxSize) {
                val removed = results.removeAt(results.size - 1)
                if (removed.character.equals(expectedCharacter)) {
                    removedExpectedResult = removed
                }
            }
            if (removedExpectedResult != null) {
                results.add(removedExpectedResult)
            }
        }
        return results
    }

    companion object {
        private var cache: ReferenceMatrixCache? = null
    }
}
