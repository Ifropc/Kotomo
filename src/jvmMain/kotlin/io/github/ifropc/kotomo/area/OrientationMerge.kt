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

import io.github.ifropc.kotomo.Orientation
import io.github.ifropc.kotomo.ocr.Rectangle
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Util.scale
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * Finds columns that overlap with other columns in the same area but with different
 * orientation. Calculates score for each orientation and keeps columns with best score.
 * This is done locally so different regions of the image can have different orientation.
 */
class OrientationMerge(task: AreaTask?) : AreaStep(task, "combined") {
    /**
     * Generates debug images for columns containing this rectangle
     */
    private val debugRect: Rectangle? = null // new Rectangle(604,544,62,123);
    private val debugAll = false

    /**
     * Index of all columns in both orientations
     */
    private var index: RTree<Column>? = null

    /**
     * Index of vertical furigana areas
     */
    private var verticalFuriganaIndex: RTree<Area>? = null

    /**
     * Index of horizontal furigana areas
     */
    private var horizontalFuriganaIndex: RTree<Area>? = null

    /**
     * Columns that have already been visited (and can be skipped)
     */
    private val visited: MutableSet<Column?> = HashSet()

    /**
     * Finds areas where columns in different orientations overlap,
     * calculates average score for each group of columns and keeps the best orientation.
     *
     * After this method each character in target image belongs to only one column in one orientation.
     * It's still possible (and correct) that different regions have different orientation.
     *
     * Results are stored in task.columns list.
     */
    
    override fun runImpl() {
        if (Parameters.orientationTarget === Orientation.VERTICAL) {
            task!!.columns = task!!.verticalColumns
            return
        }
        if (Parameters.orientationTarget === Orientation.HORIZONTAL) {
            task!!.columns = task!!.horizontalColumns
            return
        }
        task!!.columns = ArrayList()

        // populate index with columns from both orientations
        createIndexes()

        // create groups from columns
        processColumns()

        // remove small extra columns
        removeSmallColumns()
    }

    /**
     * Creates R-tree index for columns in each orientation
     */
    private fun createIndexes() {
        index = RTree(task!!.binaryImage)
        index!!.addAll((task!!.verticalColumns)!!)
        index!!.addAll((task!!.horizontalColumns)!!)
        verticalFuriganaIndex = RTree(task!!.binaryImage)
        addToFuriganaIndex(task!!.verticalColumns, (verticalFuriganaIndex)!!)
        horizontalFuriganaIndex = RTree(task!!.binaryImage)
        addToFuriganaIndex(task!!.horizontalColumns, (horizontalFuriganaIndex)!!)
    }

    private fun addToFuriganaIndex(cols: List<Column>?, index: RTree<Area>) {
        for (col: Column in cols!!) {
            if (!col.isFurigana) {
                continue
            }
            for (area: Area? in col!!.areas) {
                index.add((area)!!)
            }
        }
    }

    /**
     * Creates groups from columns, calculates scores and keeps the best groups
     */
    private fun processColumns() {
        val cols = ArrayList<Column>()
        cols.addAll((task!!.verticalColumns)!!)
        cols.addAll((task!!.horizontalColumns)!!)
        Collections.sort(cols, Comparator { o1, o2 ->

            // iterate from large to small so that small individual areas get included
            // in larger groups
            val s1 = o1.size
            val s2 = o2.size
            -1 * s1.compareTo(s2)
        })

        // reset scores created during FindColumns
        for (col: Column in cols) {
            col!!.score = null
        }
        for (col: Column in cols) {
            processColumn(col)
        }
    }

    /**
     * Find columns that are connected to each other in either orientation,
     * starting from argument column. Keeps the group that has lower average score.
     */
    private fun processColumn(col: Column) {
        if (visited.contains(col)) {
            return
        }
        val verticalGroup: MutableList<Column> = ArrayList()
        val horizontalGroup: MutableList<Column> = ArrayList()
        val todo = Stack<Column>()
        todo.add(col)
        var bounds = col.rectangle
        while (!todo.empty()) {
            val next = todo.pop()
            if (visited.contains(next)) {
                continue
            }
            if (next.isVertical) {
                verticalGroup.add(next)
            } else {
                horizontalGroup.add(next)
            }

            // find columns that intersect with next
            val candidates: MutableList<Column?> = ArrayList()
            candidates.addAll(index!![next.rectangle, (next)!!])
            for (furigana: Column in next!!.furiganaColumns) {
                candidates.addAll(index!![furigana.rectangle])
            }

            // check rgb values, skip background areas
            val colRGB = col.minRGBValue.toFloat()
            val i = candidates.iterator()
            while (i.hasNext()) {
                val candidate = i.next()
                val candRGB = candidate!!.minRGBValue.toFloat()
                val delta = Math.abs(colRGB - candRGB).toInt()
                if (delta > 100) {
                    i.remove()
                }
            }

            // TODO check average area sizes, skip if target areas are clearly too small 
            // to be part of the same column group

            // check intersect size
            for (cand: Column? in candidates) {
                val intersect = next.rectangle!!.intersection(cand!!.rectangle!!)
                val intSize = intersect.width * intersect.height
                val refSize1 = Math.ceil(Math.pow(next.minorDim.toDouble(), 2.0) / 4).toInt()
                val refSize2 = Math.ceil(Math.pow(cand.minorDim.toDouble(), 2.0) / 4).toInt()
                if (intSize >= refSize1 || intSize >= refSize2) {
                    todo.add(cand)
                }
            }
            visited.add(next)
            bounds = bounds!!.union(next.rectangle!!)
        }

        // score is calculated for the whole group of columns because this way
        // small errors in single column's score don't affect the result
        val verticalScore = calcScore(verticalGroup, bounds)
        val horizontalScore = calcScore(horizontalGroup, bounds)
        if (horizontalScore == null && verticalScore != null) {
            task!!.columns!!.addAll(verticalGroup)
        } else if (horizontalScore != null && verticalScore == null) {
            task!!.columns!!.addAll(horizontalGroup)
        } else if (horizontalScore == null && verticalScore == null) {
            task!!.columns!!.addAll(verticalGroup)
        } else if (verticalScore!! <= (horizontalScore)!!) {
            task!!.columns!!.addAll(verticalGroup)
        } else {
            task!!.columns!!.addAll(horizontalGroup)
        }
    }

    /**
     * Calculates score based on number and distance between areas.
     *
     * @return Lower score is better. null if score can't be calculated.
     */
    private fun calcScore(cols: List<Column>, bounds: Rectangle?): Float? {
        if (cols.size == 0) {
            return null
        }
        if (isDebug(cols)) {
            if (cols[0].isVertical) {
                log.debug { "vertical" } 
            } else {
                log.debug { "horizontal" } 
            }
            log.debug { "bounds:$bounds" } 
        }

        // calculate score based on average distance between areas
        val areaDistanceScore = calcScoreAreaDistance(cols)

        // distance between columns could also be used but it only works with 
        // multiple columns

        // calculate score based on number of areas. correct orienation often contains
        // columns that are aligned in reading direction resulting in connected
        // column chains
        val areaConnectionsScore = calcScoreConnected(cols)

        // calculate penalty for null columns (less than two valid areas
        // so distance can't be calculated)
        val nullColsScore: Float = calcScoreNullColumns(cols)
        var score: Float? = null
        if ((areaDistanceScore != null) && (areaConnectionsScore != null) && (nullColsScore != null)) {
            score = areaDistanceScore * areaConnectionsScore * nullColsScore
        }
        if (isDebug(cols)) log.debug { "  score:$score" } 
        return score
    }

    /**
     * Calculates score based on average distance between areas
     *
     * @return null if score can't be calculated
     */
    private fun calcScoreAreaDistance(cols: List<Column?>): Float? {
        var distanceSum = 0f
        var weightSum = 0f
        for (col: Column? in cols) {
            val areaDistance = calcAreaDistance(col)
            col!!.areaDistance = areaDistance
            if (areaDistance == null) {
                continue
            }
            var weight = Math.sqrt(col.areaSizeSum.toDouble()).toFloat()
            if (col.areas.size == 2) {
                weight *= Math.pow(col.avgAreaRatio.toDouble(), 2.0).toFloat()
            }
            distanceSum += areaDistance * weight
            weightSum += weight
            if (isDebug(cols)) log.debug { "  col:$col dist:$areaDistance weight:$weight" } 
        }
        var areaDistanceScore: Float? = null
        if (weightSum > 0) {
            areaDistanceScore = distanceSum / weightSum
        }
        if (isDebug(cols)) log.debug { "  areaDistanceScore:$areaDistanceScore" } 
        return areaDistanceScore
    }

    /**
     * Calculates average distance between areas inside column
     *
     * @return null if score can't be calculated
     */
    private fun calcAreaDistance(col: Column?): Float? {
        var distanceSum = 0f
        var pairs = 0
        val areas = filterFurigana(col!!.areas, col.isVertical)
        for (i in 0 until (areas.size - 1)) {
            val prev = areas[i]
            val next = areas[i + 1]

            // ignore all suspected special cases, concentrate only on good pairs.
            // most of the time there are enough good examples and this prevents
            // random problems with wrong orientation

            // ignore punctuation
            if (prev!!.isPunctuation || next!!.isPunctuation) {
                continue
            }

            // ignore splitted areas
            if (prev.splitted || next!!.splitted) {
                continue
            }

            // ignore small square areas
            // for example か in wrong orientation
            var minAreaSize: Float
            var minAreaShape: Float
            if (prev.size < next.size) {
                minAreaSize = prev.size.toFloat()
                minAreaShape = prev.ratio
            } else {
                minAreaSize = next.size.toFloat()
                minAreaShape = next.ratio
            }
            val targetSize = Math.pow(col.minorDim.toDouble(), 2.0).toFloat()
            val minAreaRatio = 1.0f * minAreaSize / targetSize
            if (minAreaRatio <= 0.3f && minAreaShape >= 0.5f) {
                continue
            }

            // ignore two thin areas
            // for example い in wrong orientation
            val avgRatio = (prev.majorMinorRatio + next.majorMinorRatio) / 2
            if (avgRatio <= 0.7f) {
                continue
            }

            // ignore too long areas
            val maxLength = Math.max(prev.majorDim, next.majorDim).toFloat()
            if (maxLength > col.minorDim * 1.5f) {
                continue
            }

            // calculate distance between areas
            val distance: Float =
                if (col.isVertical) (next.midpoint.y - prev.midpoint.y).toFloat() else next.midpoint.x - prev.midpoint.x.toFloat()

            // ignore too large gaps
            if (distance > col.minorDim * 2) {
                continue
            }
            distanceSum += distance
            ++pairs
        }
        return if (pairs > 0) {
            1.0f * distanceSum / pairs
        } else {
            null
        }
    }

    /**
     * Calculates score based on number of null areas (contain less than two valid
     * areas).
     *
     * @return null if score can't be calculated
     */
    private fun calcScoreNullColumns(cols: List<Column>): Float {
        var nullScoreWeight = 0f
        var totalWeight = 0f
        for (col: Column in cols) {
            val weight = Math.sqrt(col.areaSizeSum.toDouble()).toFloat()
            if (col!!.areaDistance == null) {
                nullScoreWeight += weight
            }
            totalWeight += weight
        }
        val nullRatio = nullScoreWeight / totalWeight
        val nullColsScore: Float
        if (nullRatio < 0.5f) {
            nullColsScore = scale(nullRatio, 0.0f, 0.5f, 1.0f, 1.1f)
        } else {
            nullColsScore = scale(nullRatio, 0.5f, 1.0f, 1.1f, 10.0f)
        }
        if (isDebug(cols)) log.debug { "  nullColsScore:$nullColsScore" } 
        return nullColsScore
    }

    /**
     * Calculates score based on connected areas. Lower is better.
     *
     * @return null if score can't be calculated
     */
    private fun calcScoreConnected(cols: List<Column?>): Float? {
        val colSet = HashSet<Column?>()
        colSet.addAll(cols)

        // find longest chain of connected areas
        var bestScore: Float? = null
        for (col: Column? in cols) {

            // chain starts with a column that has no previous column
            // or the column is outside orientation calculation group (rare but possible)
            if (col!!.previousColumn != null && colSet.contains(col.previousColumn)) {
                continue
            }
            val chain: MutableList<Column?> = ArrayList()
            var next = col
            do {
                chain.add(next)
            } while ((next!!.nextColumn.also { next = it }) != null)

            // calculate score for the chain
            val score = calcScoreChain(chain)
            if (score != null) {
                if (bestScore == null || score < bestScore) {
                    bestScore = score
                }
            }
        }
        if (isDebug(cols)) log.debug { "  areaConnectionsScore:$bestScore" } 
        return bestScore
    }

    /**
     * Calculates score based on single connected area chain. Lower is better.
     *
     * @return null if score can't be calculated
     */
    private fun calcScoreChain(cols: List<Column?>): Float? {
        var areaConnectionsScore: Float? = null
        for (col: Column? in cols) {
            for (area: Area? in col!!.areas) {
                if (area!!.isPunctuation) {
                    continue
                }
                if (areaConnectionsScore == null) {
                    areaConnectionsScore = 0f
                }
                // prefer square areas, sometimes wrong orientation results in
                // valid area to be splitted in two, like い
                areaConnectionsScore += area.ratio
            }
        }
        return if (areaConnectionsScore == null) {
            null
        } else {
            (1.0f / Math.pow(areaConnectionsScore.toDouble(), 0.2)).toFloat()
        }
    }

    /**
     * @return true if any column in the list intersects with debugRect
     */
    private fun isDebug(cols: List<Column?>): Boolean {
        if (debugAll) {
            return true
        } else {
            for (col: Column? in cols) {
                if (col!!.intersects(debugRect)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Removes areas that look like furigana columns in other orientation
     */
    private fun filterFurigana(areas: List<Area>, vertical: Boolean): List<Area?> {
        val validAreas: MutableList<Area> = ArrayList()

        // check if furigana areas in other orientation match exactly
        for (area: Area in areas!!) {
            if (!isAreaFurigana(area, vertical)) {
                validAreas.add(area)
            }
        }
        Collections.sort(validAreas, object : Comparator<Area> {
            override fun compare(o1: Area, o2: Area): Int {
                val i1: Int
                val i2: Int
                if (vertical) {
                    i1 = o1.y
                    i2 = o2.y
                } else {
                    i1 = o1.x
                    i2 = o2.x
                }
                return i1.compareTo(i2)
            }
        })
        return validAreas
    }

    /**
     * Checks if area looks like furigana column in other orientation
     */
    private fun isAreaFurigana(area: Area, vertical: Boolean): Boolean {
        val pixels = area.pixels
        val furiAreas: List<Area>?
        if (vertical) {
            furiAreas = horizontalFuriganaIndex!![area!!.rect]
        } else {
            furiAreas = verticalFuriganaIndex!![area!!.rect]
        }
        var furiPixels = 0
        for (furiArea: Area in furiAreas) {
            furiPixels += furiArea.pixels
        }
        return if (pixels == furiPixels) {
            true
        } else {
            false
        }
    }

    /**
     * Removes too small columns
     */
    private fun removeSmallColumns() {
        val i = task!!.columns!!.iterator()
        while (i.hasNext()) {
            val column = i.next()
            if (column.minDim <= 7) { // TODO relative to resolution
                //if (column.size < 20) {
                i.remove()
            }
        }
    }

    
    override fun addDebugImages() {
        task!!.addDefaultDebugImage("combined")
    }
}
