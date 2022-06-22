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

import io.github.ifropc.kotomo.ocr.KotomoRectangle

/**
 * List of areas inside a single column (or row in horizontal orientation)
 */
data class Column(
    /**
     * Rectangles around characters in this column.
     * Ordered in reading direction (top-down or left-right).
     */
    val areas: List<KotomoRectangle>,
    /**
     * Bounding box around areas
     */

    val rect: KotomoRectangle,

    /**
     * If true, this column has vertical reading direction. If false, horizontal.
     */

    val vertical: Boolean = false,

    /**
     * If true, this column contains furigana characters
     */

    val furigana: Boolean = false,
    /**
     * Next column in reading direction
     */
    var nextColumn: Column? = null,

    /**
     * Previous column in reading direction
     */
    var previousColumn: Column? = null

) {
    // this is a simplified version of Column intended to be used
    // as a result object from KanjiTomo class


}
