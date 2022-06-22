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

import java.util.*

/**
 * Pritority queue with limited size that contains best OCR results.
 */
class OCRResultPriorityQueue(
    /**
     * Maximum number of best results stored in the queue
     */
    private val capacity: Int
) {
    /**
     * Best results
     */
    private val queue: PriorityQueue<OCRResult>

    /**
     * Score for the worst result currently in the queue
     */
    private var worstScore: Int? = null

    /**
     * If set, this character is never removed from the results
     */
    private var expectedCharacter: Char? = null

    /**
     * Result that contains the expected character
     */
    private var expectedResult: OCRResult? = null

    /**
     * @param capacity Number of best results stored in the queue
     */
    init {
        queue = initQueue(capacity)
    }

    /**
     * Sets the expected character. Never removed from results. Used for debug
     * purposes.
     */
    fun setExpectedCharacter(character: Char?) {
        expectedCharacter = character
    }

    /**
     * Adds a new result to the queue. If the queue is full, checks first
     * if this result is better than the worst existing result.
     */
    fun add(result: OCRResult) {
        if (result.character == expectedCharacter) {
            expectedResult = result
        }
        if (worstScore != null && result.score < worstScore!!) {
            return
        }
        queue.add(result)
        if (queue.size > capacity) {
            queue.poll()
            worstScore = queue.peek()!!.score
        }
    }//results.remove(results.size()-1);

    /**
     * Gets results in descending score order (from best to worst)
     */
    val results: List<OCRResult>
        get() {
            val results: MutableList<OCRResult> = ArrayList()
            results.addAll(queue)
            if (expectedResult != null) {
                //results.remove(results.size()-1);
                if (!results.contains(expectedResult)) {
                    results.add(expectedResult!!)
                }
            }
            results.reverse()
            return results
        }

    companion object {
        /**
         * Creates empty priority queue
         */
        private fun initQueue(capacity: Int): PriorityQueue<OCRResult> {
            return PriorityQueue(
                capacity
            ) { o1, o2 -> o1.score.compareTo(o2.score) }
        }
    }
}
