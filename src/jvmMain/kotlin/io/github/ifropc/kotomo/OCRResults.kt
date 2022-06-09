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

package io.github.ifropc.kotomo

import io.github.ifropc.kotomo.ocr.Rectangle

/**
 * Results of the OCR run
 */
class OCRResults(
    characters: List<String>,
    locations: List<Rectangle>,
    scores: List<List<Int>>,
    vertical: Boolean
) {
    /**
     * String of best matches starting from OCR target point.
     */

    val bestMatchingCharacters: String

    /**
     * List of characters identified by OCR algorithm. First character in each element
     * is added to bestMatchingCharacters.
     */
    val characters: MutableList<io.github.ifropc.kotomo.IdentifiedCharacter>

    /**
     * If true, vertical orientation was used as reading direction.
     * If false, horizontal orientation was used.
     */
    val vertical: Boolean

    init {
        this.characters = ArrayList()
        var bestMatchingCharacters = ""
        for (i in characters.indices) {
            val character = io.github.ifropc.kotomo.IdentifiedCharacter(characters[i], locations[i], scores[i])
            bestMatchingCharacters += character.referenceCharacters[0]
            this.characters.add(character)
        }
        this.bestMatchingCharacters = bestMatchingCharacters
        this.vertical = vertical
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("\nCharacters:\n")
        for (character in characters) {
            sb.append(
                """
    ${character.referenceCharacters}
    
    """.trimIndent()
            )
        }
        sb.append("\nLocations:\n")
        for (character in characters) {
            sb.append(
                """
    ${character.location}
    
    """.trimIndent()
            )
        }
        return sb.toString()
    }
}
