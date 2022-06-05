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
 * List of areas inside a single column (or row in horizontal orientation)
 */
class Column {
    // this is a simplified version of net.kanjitomo.area.Column intended to be used
    // as a result object from KanjiTomo class
    /**
     * Rectangles around characters in this column.
     * Ordered in reading direction (top-down or left-right).
     */
	@JvmField
	var areas: MutableList<Rectangle>? = null

    /**
     * Bounding box around areas
     */
	@JvmField
	var rect: Rectangle? = null

    /**
     * Next column in reading direction
     */
    var nextColumn: Column? = null

    /**
     * Previous column in reading direction
     */
    var previousColumn: Column? = null

    /**
     * If true, this column has vertical reading direction. If false, horizontal.
     */
	@JvmField
	var vertical = false

    /**
     * If true, this column contains furigana characters
     */
	@JvmField
	var furigana = false

    /**
     * Furigana columns next to this column
     */
    var furiganaColumns: List<Column> = ArrayList()
    override fun toString(): String {
        return "rect:" + rect + " areas:" + areas!!.size + " vertical:" + vertical + " furigana:" + furigana
    }
}
