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
