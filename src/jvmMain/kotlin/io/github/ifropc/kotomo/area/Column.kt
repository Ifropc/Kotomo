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
package io.github.ifropc.kotomo.area

import io.github.ifropc.kotomo.ocr.KotomoRectangle
import java.awt.Color
import io.github.ifropc.kotomo.ocr.Point
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * List of areas inside a single column (or row in horizontal orientation).
 * Column is enclosed by a border that doesn't touch other areas.
 */
class Column : HasRectangle {
    /**
     * Areas inside this column. Ordered in reading direction (top-down or left-right).
     */
    var areas: MutableList<Area> = mutableListOf()

    /**
     * Bounding box around areas
     */
    override var rectangle: KotomoRectangle? = null
    /**
     * @return true if column has vertical orientation
     */
    /**
     * If true, this column has vertical reading direction. If false, horizontal.
     */
    var isVertical = true
    /**
     * @return true if this column contains furigana
     */
    /**
     * If true, this is a furigana column
     */
    var isFurigana = false

    /**
     * Furigana columns next to this column
     */
    var furiganaColumns: MutableList<Column> = ArrayList()

    /**
     * Average distance between areas
     */
    var areaDistance: Float? = null

    /**
     * Used to compare this column to corresponding columns
     */
    var score: Float? = null

    /**
     * Next column in reading direction
     */
    var nextColumn: Column? = null

    /**
     * Previous column in reading direction
     */
    var previousColumn: Column? = null
    /**
     * @return Color used to paint this column in debug images
     */
    /**
     * If set, this color will be used in debug images
     */
    var debugColor: Color? = null
    /**
     * @return true if this column has been changed in previous AreaStep.
     * Changes columns are painted blue.
     */
    /**
     * If true, column has been changed in this step. Used for debug printing.
     */
    var isChanged = false

    /**
     * If true, column has been marked for removal
     */
    var remove = false

    /**
     * Surface area of the column
     */
    val size: Int
        get() = rectangle!!.width * rectangle!!.height

    /**
     * Surface area of column's areas
     */
    val areaSizeSum: Int
        get() {
            var sum = 0
            for (area in areas) {
                sum += area.size
            }
            return sum
        }

    /**
     * min(width, heighrt)
     */
    val minDim: Int
        get() = if (rectangle!!.width < rectangle!!.height) {
            rectangle!!.width
        } else {
            rectangle!!.height
        }

    /**
     * @return true if column's rectangle contains point
     */
    operator fun contains(point: Point?): Boolean {
        return if (point == null) {
            false
        } else {
            rectangle!!.contains(point)
        }
    }

    /**
     * @return true if column's rectangle intersect with argument rectangle
     */
    fun intersects(rect: KotomoRectangle?): Boolean {
        return if (rect == null) {
            false
        } else {
            rectangle!!.intersects(rect)
        }
    }

    /**
     * @return true if column's rectangle contains point
     */
    fun contains(x: Int, y: Int): Boolean {
        return contains(Point(x, y))
    }

    /**
     * @return true if column's rectangle contains col2's rectangle
     */
    operator fun contains(col2: Column?): Boolean {
        return rectangle!!.contains(col2!!.rectangle!!)
    }

    /**
     * Calculates the ratio of common pixels to total area with argument column
     */
    fun getIntersectRatio(col2: Column?): Float {
        if (!col2!!.rectangle!!.intersects(rectangle!!)) {
            return 0f
        }
        val intersect = col2.rectangle!!.intersection(rectangle!!)
        val intersectSize = intersect.height * intersect.width
        val refSize = min(size, col2.size)
        return 1.0f * intersectSize / refSize
    }

    /**
     * Calculates the ratio of common x coordinates between columns
     */
    fun getHorizontalIntersectRatio(col2: Column?): Float {
        if (col2!!.x > maxX || col2.maxX < x) {
            return 0f
        }
        val intersectMinX = max(x, col2.x)
        val intersectMaxX = min(maxX, col2.maxX)
        val commonWidth = intersectMaxX - intersectMinX + 1
        val refWidth = min(rectangle!!.width, col2.rectangle!!.width)
        return 1.0f * commonWidth / refWidth
    }

    /**
     * Merges this column with argument column
     *
     * @param mergeAreas If true, overlapping areas inside this column are merged
     * along minor dimension (left/right in vertical columns)
     * @return Merged column (this column is not affected)
     */
    fun merge(col2: Column?): Column {
        val mergedCol = Column()
        mergedCol.rectangle = rectangle!!.union(col2!!.rectangle!!)
        mergedCol.addAreas(areas)
        mergedCol.addAreas(col2.areas)
        mergedCol.isVertical = isVertical
        Collections.sort(mergedCol.areas) { o1, o2 ->
            if (isVertical) {
                val y1 = o1.midpoint.y
                val y2 = o2.midpoint.y
                y1.compareTo(y2)
            } else {
                val x1 = o1.midpoint.x
                val x2 = o2.midpoint.x
                x1.compareTo(x2)
            }
        }
        mergeAreasMinorDim(mergedCol.areas)
        return mergedCol
    }

    private fun addAreas(areas: List<Area?>) {
        for (area in areas) {
            addArea(area)
        }
    }

    fun addArea(area: Area?) {
        var area = area
        area = area!!.clone()
        area.column = this
        areas.add(area)
    }

    /**
     * Merges areas that are overlapping in left/right direction (vertical orientation)
     * or up/down direction (horizontal orientation).
     */
    private fun mergeAreasMinorDim(areas: MutableList<Area>) {
        var i = 0
        while (i < areas.size - 1) {
            val a1 = areas[i]
            val a2 = areas[i + 1]
            var intersect = false
            if (isVertical) {
                if (a1.maxY >= a2.y && a1.y <= a2.maxY) {
                    intersect = true
                }
            } else {
                if (a1.maxX >= a2.x && a1.x <= a2.maxX) {
                    intersect = true
                }
            }
            if (intersect) {
                areas.removeAt(i)
                areas.removeAt(i)
                areas.add(i, a1.merge(a2))
                --i
            }
            i++
        }
    }

    val x: Int
        get() = rectangle!!.x
    val y: Int
        get() = rectangle!!.y
    val width: Int
        get() = rectangle!!.width
    val height: Int
        get() = rectangle!!.height

    /**
     * Column thickness
     *
     * @return vertical -> width, horizontal -> height
     */
    val minorDim: Int
        get() = if (isVertical) {
            rectangle!!.width
        } else {
            rectangle!!.height
        }

    /**
     * Column length in reading direction
     *
     * @return vertical -> height, horizontal -> width
     */
    val majorDim: Int
        get() = if (isVertical) {
            rectangle!!.height
        } else {
            rectangle!!.width
        }

    /**
     * Midpoint of this column's rectangle.
     */
    override val midpoint: Point
        get() = Point(rectangle!!.x + rectangle!!.width / 2, rectangle!!.y + rectangle!!.height / 2)

    /**
     * Right border.
     */
    val maxX: Int
        get() = rectangle!!.x + rectangle!!.width - 1


    /**
     * Bottom border.
     */
    val maxY: Int
        get() = rectangle!!.y + rectangle!!.height - 1

    /**
     * Number of pixels inside column
     */
    val pixels: Int
        get() {
            var pixels = 0
            for (area in areas) {
                pixels += area.pixels
            }
            return pixels
        }

    /**
     * Ratio of pixels / column area.
     * @return
     */
    val pixelAreaRatio: Float
        get() = 1.0f * pixels / size

    /**
     * Ratio between smaller and larger dimensions.
     */
    val ratio: Float
        get() {
            val min = min(rectangle!!.width, rectangle!!.height)
            val max = max(rectangle!!.width, rectangle!!.height)
            return 1.0f * min / max
        }

    /**
     * Average area shape
     */
    val avgAreaRatio: Float
        get() {
            if (areas.size == 0) {
                return 1f
            }
            var sum = 0f
            for (area in areas) {
                sum += area.ratio
            }
            return sum / areas.size
        }

    /**
     * Median of area sizes
     */
    val medianAreaSize: Float
        get() {
            if (areas.size == 0) {
                return 0f
            }
            val areasBySize: MutableList<Area?> = ArrayList()
            areasBySize.addAll(areas)
            areasBySize.sortWith(Comparator { o1, o2 ->
                val s1 = o1!!.size
                val s2 = o2!!.size
                s1.compareTo(s2)
            })
            return if (areas.size % 2 == 1) {
                areasBySize[areas.size / 2]!!.size.toFloat()
            } else {
                val size1 = areasBySize[areas.size / 2 - 1]!!.size
                val size2 = areasBySize[areas.size / 2]!!.size
                1.0f * (size1 + size2) / 2
            }
        }

    /**
     * Gets the mininum RGB value among areas pixels
     */
    val minRGBValue: Int
        get() {
            var minRGB = 255
            for (area in areas) {
                if (area.minRGB < minRGB) {
                    minRGB = area.minRGB
                }
            }
            return minRGB
        }

    /**
     * Gets the average minimum RGB value among areas
     */
    val avgRGBValue: Float
        get() {
            var rgbSum = 0
            var rgbWeight = 0
            for (area in areas) {
                val weight = area.pixels
                rgbSum += area.minRGB * weight
                rgbWeight += weight
            }
            return 1.0f * rgbSum / rgbWeight
        }

    override fun toString(): String {
        return (if (isVertical) "v" else "h") + ":" + rectangle!!.x + "," + rectangle!!.y + "," + rectangle!!.width + ":" + rectangle!!.height
    }

    /**
     * Simplified Column representation intended for API users.
     */
    private var simpleColumn: io.github.ifropc.kotomo.Column? = null

    /**
     * Simplified Column representation intended for API users.
     */
    fun getSimpleColumn(): io.github.ifropc.kotomo.Column {
        if (simpleColumn != null) {
            return simpleColumn!!
        }
        simpleColumn = io.github.ifropc.kotomo.Column(
            areas.filter { !it.isPunctuation }.map { it.rectangle },
            rectangle!!,
            vertical = isVertical,
            furigana = isFurigana
        )

        return simpleColumn!!
    }
}
