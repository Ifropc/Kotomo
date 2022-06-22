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

import io.github.ifropc.kotomo.ocr.Colors
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import io.github.ifropc.kotomo.util.ImageUtil.paintRectangle
import io.github.ifropc.kotomo.util.JVMUtil.runWithDebuggable
import io.github.ifropc.kotomo.util.JVMUtil.withDebuggable
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Util.scale
import mu.KotlinLogging
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private val log = KotlinLogging.logger { }

/**
 * Merges areas into columns.
 */
class FindColumns(task: AreaTask?) : AreaStep(task, "columns") {
    /**
     * Generates debug images only for columns containing this rectangle
     */
    private val debugRect: KotomoRectangle? = null // new Rectangle(273,96,4,9);

    /**
     * Generates debug images for all columns.
     *
     * Warning! This will create large amount of data, use only for small target images.
     */
    private val debugAll = false

    /**
     * Areas are only combined into columns if their RGB values are closer than this
     */
    private val rgbMaxDelta = 100
    private var index: RTree<Column>? = null

    override fun runImpl() {
        if (task!!.areas!!.size == 0) {
            task!!.columns = ArrayList()
            return
        }

        // merge areas into columns
        index = RTree(task!!.binaryImage)
        createInitialColumns()
        for (i in 1..3) {
            mergeColumns(true, i)
        }
        mergeColumns(false, 1)
        task!!.columns = index!!.all
    }

    /**
     * Creates new column for each area
     */
    private fun createInitialColumns() {
        for (area in task!!.areas!!) {
            index!!.add(createColumn(area))
        }
    }

    /**
     * Creates new column that represents single area
     */
    private fun createColumn(area: Area?): Column {
        val column = Column()
        column.addArea(area)
        column.rectangle = KotomoRectangle(
            area!!.rect.x,
            area.rect.y,
            area.rect.width,
            area.rect.height
        )
        column.isVertical = Parameters.vertical
        column.score = column.ratio
        return column
    }

    /**
     * Merges close by areas into columns. Columns are scored and expansion is done only if
     * column score increases.
     *
     * @param expandLength If true, columns length is expanded (in reading direction).
     * If false, width is expanded.
     *
     * @param iteration Column length in expanded in stages. First by small amount
     * (iteration 1), then in larger increments.
     */

    private fun mergeColumns(expandLength: Boolean, iteration: Int) {
        if (debugAll) log.debug { "mergeColumn expandLength:$expandLength iteration:$iteration" }

        // process areas in order that prioritizes column growth in reading direction
        val cols = index!!.all
        val todo = PriorityQueue<Column?>(
            cols.size
        ) { o1, o2 ->
            val size1 = o1.minorDim * o1.size
            val size2 = o2.minorDim * o2.size
            size1.compareTo(size2)
        }
        todo.addAll(cols)

        // merge columns until nothing can be done
        //while(!todo.empty()) {
        while (!todo.isEmpty()) {

            //Column col = todo.pop();
            val col = todo.remove()
            if (col!!.remove) {
                continue
            }

            // find close by target columns that are candidates for merging
            val probe = createProbe(col, expandLength, iteration)
            val targets = index!![probe, col]
            if (isDebug(col)) {
                addIntermediateDebugImage(col, probe)
                log.debug {
                    withDebuggable(task!!) { task ->
                        task!!.debugImages[task!!.debugImages.size - 1]!!.filename
                    }
                }
                log.debug { "  col:   " + col + " score:" + col.score + " rgb:" + col.avgRGBValue }
                log.debug { "  probe: $probe" }
            }

            // find the largest column (not always col, can be target)
            var largest = col
            for (target in targets) {
                if (target.minorDim > largest!!.minorDim) {
                    largest = target
                }
            }

            // filter columns that have too high rgb value compared to largest column
            // this is done to prevent expansion into unrelated background areas
            if (col.avgRGBValue - largest!!.avgRGBValue > rgbMaxDelta) {
                log.trace { "  skip rgb" }
                continue
            }
            val rejected = filterTargetsByRGB(targets, largest)
            if (targets.isEmpty()) {
                log.trace("  skip empty")
                continue
            }

            // merge col with targets
            var merged = col
            for (target in targets) {
                merged = merged!!.merge(target)
            }
            if (log.isDebugEnabled) {
                for (target in targets) {
                    log.debug { "  target:" + target + " score:" + target.score + " rgb:" + target.avgRGBValue }
                }
                log.debug { "  merged:$merged" }
            }

            // sideways expansion is only allowed between adjacent columns, check
            // that merged column doesn't contain any columns not in original target list
            if (!expandLength) {
                if (checkNewTargets(merged, col, targets, rejected)) {
                    continue
                }
            }

            // check that merged column is valid and average column score increases
            if (checkMerge(merged, col, targets, largest)) {
                for (target in targets) {
                    index!!.remove(target)
                    target.remove = true
                }
                index!!.remove(col)
                index!!.add(merged!!)
                todo.add(merged)
            }
        }
    }

    /**
     * Filter targets that have too high RGB value compared to largest column
     *
     * @return list of rejected columns
     */
    private fun filterTargetsByRGB(targets: MutableList<Column>, largest: Column?): List<Column?> {
        val rejected: MutableList<Column?> = ArrayList()
        val refRGBValue = largest!!.avgRGBValue
        val i = targets.iterator()
        while (i.hasNext()) {
            val target = i.next()
            // allow targets that are contained inside col
            if (largest.contains(target)) {
                continue
            }
            // allow dakuten,
            // these are sometimes gray instead of black and can fail rgb check
            if (target.areas.size == 1 && target.ratio >= 0.6f && target.maxY >= largest.y - largest.width / 4 && target.maxY < largest.maxY && target.midpoint.x > largest.midpoint.x && target.pixelAreaRatio >= 0.5f && largest.getHorizontalIntersectRatio(
                    target
                ) >= 0.7f
            ) {
                continue
            }

            // check rgb values
            if (target.avgRGBValue - refRGBValue > rgbMaxDelta) {
                log.debug { "  skip target:" + target + " rgb:" + target.avgRGBValue }
                i.remove()
                rejected.add(target)
            }
        }
        return rejected
    }

    /**
     * Checks that merged column's rectangle doesn't contain any new columns not
     * in original targets list
     *
     * @return true if merged rectangle contains extra columns
     */
    private fun checkNewTargets(
        merged: Column?, original: Column?, targets: List<Column?>?,
        rejected: List<Column?>
    ): Boolean {
        val colSet: MutableSet<Column?> = HashSet()
        colSet.add(original)
        colSet.addAll(targets!!)
        colSet.addAll(rejected)
        for (col in index!![merged!!.rectangle]) {
            if (!colSet.contains(col)) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a probe rectangle that is used to find expansion targets
     *
     * @param expandLength If true, column's length is expanded (in reading direction).
     * If false, column's width is expanded.
     */
    private fun createProbe(col: Column, expandLength: Boolean, iteration: Int): KotomoRectangle {
        return if (expandLength) {
            createLongProbe(col, iteration)
        } else {
            createThickProbe(col)
        }
    }

    /**
     * Creates a probe that is used to expand column in reading direction
     */
    private fun createLongProbe(col: Column, iteration: Int): KotomoRectangle {
        val width = col.width
        val height = col.height
        val extra = ceil((min(width, height) * 0.5f * iteration).toDouble()).toInt()
        return if (col.isVertical) {
            KotomoRectangle(
                col.x, col.y - extra,
                width, col.height + extra * 2
            )
        } else {
            KotomoRectangle(
                col.x - extra, col.y,
                col.width + extra * 2, height
            )
        }
    }

    /**
     * Creates a probe that is used to expand column sideways
     */
    private fun createThickProbe(col: Column): KotomoRectangle {
        val width = col.width
        val height = col.height
        val extra = floor(min(width, height).toDouble()).toInt()
        return if (col.isVertical) {
            KotomoRectangle(
                col.x - extra, col.y,
                width + extra * 2, col.height
            )
        } else {
            KotomoRectangle(
                col.x, col.y - extra,
                width, col.height + extra * 2
            )
        }
    }

    /**
     * Checks that merged column is better that old columns
     *
     * @return true if merged is better
     */
    private fun checkMerge(
        merged: Column?, col: Column, targets: List<Column>?,
        largest: Column
    ): Boolean {

        // calculate score for merged column
        merged!!.score = calcScore(merged)

        // check if all columns are contained inside largest column
        val intersect = checkContains(largest, merge(col, targets))
        if (intersect) {
            log.debug { "  all columns contained" }
            return true
        }

        // check if column is expanding along minor dimension, decrease score if 
        // large expansion and long column (often invalid expansions into furigana)  
        val minorDimExpansion = 1.0f * merged.minorDim / largest.minorDim
        val maxPenalty = scale(col.areas.size.toFloat(), 2f, 4f, 1.0f, 0.8f)
        val expansionScore = scale(minorDimExpansion, 1.15f, 1.4f, 1.0f, maxPenalty)
        merged.score = merged.score!! * expansionScore

        log.debug { "  minorDimExp:$minorDimExpansion" }
        log.debug { "  expScore:   $expansionScore" }
        log.debug { "  score:      " + merged.score }

        // calculate weighted average score from old columns
        var scoreSum = 0f
        var weightSum = 0f
        var lowestScore = 10000f
        for (target in merge(col, targets)) {
            if (target.score!! < lowestScore) {
                lowestScore = target.score!!
            }
        }
        for (target in merge(col, targets)) {
            var weight = target.size.toDouble().pow(0.58).toFloat()
            if (target.score == lowestScore) {
                weight *= 1.25f
            }
            weight *= scale(
                target.minRGBValue.toFloat(), 0f, Parameters.pixelRGBThreshold.toFloat(),
                1.0f, 0.5f
            )
            scoreSum += target.score!! * weight
            weightSum += weight
        }
        val oldScore = scoreSum / weightSum

        // check if merged column is better than average
        log.debug { " " + (if (merged.score!! >= oldScore) ">=" else "<") + " old score:" + oldScore }
        if (merged.score!! >= oldScore) {
            // check that column doesn't cross background elements
            if (!checkBackground(merged)) {
                log.debug { "  skip background" }
                return false
            }
            // check that column ends are no expanded into small area fragments
            if (!checkColumnEnds(merged)) {
                log.debug { "  skip column ends" }
                return false
            }
            return true
        }
        return false
    }

    /**
     * Merges col and cols into new list
     */
    private fun merge(col: Column, cols: List<Column>?): List<Column> {
        val joined: MutableList<Column> = ArrayList()
        joined.addAll(cols!!)
        joined.add(col)
        return joined
    }

    /**
     * Checks that column ends are not nearly empty
     *
     * @return true if column is valid (pixel to area ratio at ends is high enough)
     */
    private fun checkColumnEnds(col: Column): Boolean {

        // this prevents expansion into small unrelated fragments in the background
        val firstProbe: KotomoRectangle
        val secondProbe: KotomoRectangle
        val probeLength = col.minorDim
        if (col.isVertical) {
            firstProbe = KotomoRectangle(
                col.x, col.y,
                col.width, probeLength
            )
            secondProbe = KotomoRectangle(
                col.x, col.maxY - probeLength,
                col.width, probeLength
            )
        } else {
            firstProbe = KotomoRectangle(
                col.x, col.y,
                probeLength, col.height
            )
            secondProbe = KotomoRectangle(
                col.maxX - probeLength, col.y,
                probeLength, col.height
            )
        }
        if (!checkColumnEnd(col, firstProbe)) {
            return false
        }
        return checkColumnEnd(col, secondProbe)
    }

    /**
     * Checks that column end is not nearly empty
     *
     * @return true if column is valid (pixel to area ratio at end is high enough)
     */
    private fun checkColumnEnd(col: Column?, probe: KotomoRectangle): Boolean {
        var pixelsSum = 0f
        for (area in col!!.areas) {
            if (!probe.intersects(area.rect)) {
                continue
            }
            val intersect = probe.intersection(area.rect)
            val ratio = 1.0f * intersect.height * intersect.width / area.size
            var pixels = area.pixels * ratio
            // thin lines along major dimension are fine, give them priority
            pixels *= scale(area.majorMinorRatio, 0.5f, 1.5f, 0.5f, 1.5f)
            pixelsSum += pixels
        }
        val ratio = pixelsSum / (probe.width * probe.height)
        return ratio >= 0.05f
    }

    /**
     * Checks if largest column contains all targets
     */
    private fun checkContains(largest: Column?, targets: List<Column?>): Boolean {

        // check that majority of target's columns area is contained within largest column
        for (target in targets) {
            if (largest!!.getIntersectRatio(target) < 0.65) {
                return false
            }
        }
        return true
    }

    /**
     * Calculates score based on column's areas (higher score is better).
     */
    private fun calcScore(col: Column?): Float {
        var scoreSum = 0f
        for (area in col!!.areas) {
            log.debug { "  area:$area" }
            val sizeScore = calcScoreSize(area, col)
            val shapeScore = calcScoreShape(area, col)
            val locationScore = calcScoreLocation(area, col, sizeScore)
            val rgbScore = calcScoreRGB(area, col)
            val score = sizeScore * shapeScore * locationScore * rgbScore
            log.debug { "    score:        $score" }
            scoreSum += score
        }
        log.debug { "  scoreSum:   $scoreSum" }
        val scoreSqr = sqrt(scoreSum.toDouble()).toFloat()
        log.debug { "  scoreSqr:   $scoreSqr" }
        return scoreSqr
    }

    /**
     * Calculates size score (large is better)
     */
    private fun calcScoreSize(area: Area, col: Column): Float {
        var ratio = 1.0f * area.maxDim / (col.minorDim * 0.9f)
        if (ratio > 1.0f) ratio = 1.0f
        val score = ratio.toDouble().pow(2.0).toFloat()
        log.debug { "    sizeRatio:    $ratio" }
        log.debug { "    sizeScore:    $score" }
        return score
    }

    /**
     * Calculates shape score (square is better)
     */
    private fun calcScoreShape(area: Area, col: Column?): Float {
        val ratio = area.ratio
        // allow small deviation from ideal square without penalty
        var score = scale(ratio, 0.0f, 0.9f, 0.0f, 1.0f)
        val exponent = 1.2f
        score = score.toDouble().pow(exponent.toDouble()).toFloat()
        log.debug { "    shapeRatio:   $ratio" }
        log.debug { "    shapeScore:   $score" }
        return score
    }

    /**
     * Calculates location score (center is better)
     */
    private fun calcScoreLocation(area: Area, col: Column, sizeScore: Float): Float {
        val firstEdge: Int
        val secondEdge: Int
        if (col.isVertical) {
            firstEdge = area.x - col.x
            secondEdge = col.maxX - area.maxX
        } else {
            firstEdge = area.y - col.y
            secondEdge = col.maxY - area.maxY
        }
        val diff = abs(firstEdge - secondEdge)
        var diffPrct = 1.0f * diff / col.minorDim
        diffPrct = scale(diffPrct, 0.1f, 1.0f, 0.0f, 1.0f)
        val exponent = scale(sizeScore, 0.2f, 0.8f, 6f, 3f)
        val locationScore = (1.0f - diffPrct).toDouble().pow(exponent.toDouble()).toFloat()
        log.debug { "    diffPrct:     $diffPrct" }
        log.debug { "    locationScore:$locationScore" }
        return locationScore
    }

    /**
     * Calculates RGB value difference (smaller is better)
     */
    private fun calcScoreRGB(area: Area?, col: Column): Float {
        val rgbDelta = (area!!.minRGB - col.minRGBValue)
        val rgbScore = scale(rgbDelta.toFloat(), 50f, 100f, 1.0f, 0.4f)
        log.debug { "    rgbDelta:     $rgbDelta" }
        log.debug { "    rgbScore:     $rgbScore" }
        return rgbScore
    }

    /**
     * Prevents column expansion into background elements such as speech bubbles and
     * divider lines
     *
     * @return true if column is valid
     */
    private fun checkBackground(col: Column): Boolean {

        // first check only border pixels since it is faster,
        // only check inside the column if necessary
        val borderPixels = task!!.countPixels(col.rectangle, true, false)
        if (borderPixels >= 2) {
            val insidePixels = task!!.countPixels(col.rectangle, true, true)
            if (insidePixels >= col.minorDim) {
                return false
            }
        }
        return true
    }

    /**
     * @return true if column should be debugged. debug state is saved to debug variable.
     */
    private fun isDebug(col: Column?): Boolean {
        return if (debugAll) {
            true
        } else if (debugRect != null) {
            col!!.intersects(debugRect)
        } else {
            false
        }
    }

    /**
     * Generates debug image from the current algorithm state
     */

    private fun addIntermediateDebugImage(col: Column, probe: KotomoRectangle?) {
        col.isChanged = true
        val columns = index!!.all
        val areas: MutableList<Area> = ArrayList()
        for (column in columns) {
            areas.addAll(column.areas)
        }

        ///task.addDefaultDebugImage("columns", areas, columns, Parameters.vertical);
        val image = runWithDebuggable(task!!) { task -> task.createDefaultDebugImage(areas, columns) }
        if (probe != null) {
            paintRectangle(image, probe, Colors.GREEN)
        }
        withDebuggable(task!!) { task ->   task!!.addDebugImage(image, "columns", col.isVertical) }
        col.isChanged = false
    }


    override fun addDebugImages() {
        withDebuggable(task!!) { task -> task!!.addDefaultDebugImage("columns", Parameters.vertical) }
    }
}
