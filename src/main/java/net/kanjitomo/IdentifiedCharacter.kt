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

package net.kanjitomo

import java.awt.Rectangle

/**
 * OCR results for a single target character
 */
class IdentifiedCharacter     // TODO normalized scores
    (
    /**
     * List of reference characters that match the target character best,
     * ordered by OCR score (first character is the closest match).
     */
    val referenceCharacters: String,
    /**
     * Location of the character in target image's coordinates
     */
    val location: Rectangle,
    /**
     * OCR scores for each reference character. Same order as in referenceCharacters.
     * Higher score is better but reference characters might have been re-ordered if
     * first match didn't result in a valid dictionary word.
     */
    val scores: List<Int>
)
