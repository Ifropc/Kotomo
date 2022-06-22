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
import io.github.ifropc.kotomo.util.Parameters
import mu.KotlinLogging
import java.awt.Color
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

private val log = KotlinLogging.logger { }

/**
 * Merges thin neighbour areas in up/down direction (vertical orientation) or
 * left/right direction (horizontal orientation)
 */
class MergeAreas(task: AreaTask?) : AreaStep(task, "mergeareas") {
    /**
     * Generates debug images only for columns containing this rectangle
     */
    private val debugRect: KotomoRectangle? = null // new Rectangle(320,32,90,149);

    /**
     * Generates debug images for all columns.
     *
     * Warning! This will create large amount of data, use only for small target images.
     */
    private val debugAll = false
    
    override fun runImpl() {
        for (col in task!!.columns!!) {
            mergeAreas(col)
        }
    }

    /**
     * Target area size (major axis). Most areas in the column are expected to be
     * close to this size.
     */
    private var targetSize = 0

    /**
     * Maximum area size (major axis). Areas are not allowed to be larger than this.
     */
    private var maxSize = 0

    /**
     * Loop and merge adjacent areas inside column until no merges can be done
     */
    
    private fun mergeAreas(col: Column) {

        // split areas into chunks delimited by punctuation and large areas that 
        // can't be merged. for areas inside each chunk, try all merge combinations
        // and selected the best
        val scale = calcScale(col)
        targetSize = ceil((col.minorDim * scale).toDouble()).toInt()
        maxSize = ceil((col.minorDim * MAX_AREA_SIZE * scale).toDouble()).toInt()
        val chunk: MutableList<Area> = ArrayList()
        var chunkStart = -1
        var i = 0
        while (i < col.areas.size) {
            val area = col.areas[i]
            val punctuation = area.isPunctuation
            val lastAreaInCol = i == col.areas.size - 1
            var lastAreaInChunk = punctuation || lastAreaInCol
            if (!punctuation) {
                // add new area to chunk
                chunk.add(area)
                if (chunk.size == 1) {
                    chunkStart = i
                }
                // check if chunk should end
                if (chunk.size == MAX_CHUNK_SIZE) {
                    lastAreaInChunk = true
                } else if (!lastAreaInCol) {
                    val next = col.areas[i + 1]
                    val test = area.merge(next)
                    if (test.majorDim > maxSize) {
                        lastAreaInChunk = true
                    }
                }
            }
            if (lastAreaInChunk && chunk.size > 0) {
                // find best merge combination for chunk's areas
                val mergedAreas = findBestMerge(col, chunk)
                // replace original areas with merged areas
                for (j in chunk.indices) {
                    col.areas.removeAt(chunkStart)
                }
                col.areas.addAll(chunkStart, mergedAreas)
                i = chunkStart + mergedAreas.size - 1
                // start next iteration from this iteration's last area,
                // possibly continuing merge
                if (!punctuation && !lastAreaInCol && chunk.size > 1) {
                    i--
                }
                chunk.clear()
            }
            i++
        }
    }

    /**
     * Finds best merge by checking all combinations for area list. Merges are scored
     * by comparing area heights to column width (in vertical orientation).
     *
     * @areas This list should be relatively short and not contain any punctuation
     * @nextCombination Which areas should be merged next? Example: 212 -> combine first and
     * last two areas.
     *
     * @return List of merged areas with best score
     */
    
    private fun findBestMerge(col: Column, areas: List<Area>): List<Area> {
        if (areas.size == 1) {
            return areas
        }
        if (isDebug(areas)) {
            log.debug { "findBestMerge" } 
            log.debug { "col:$col" } 
            log.debug {  "areas:" + toString(areas) }
        }
        val combination = BooleanArray(areas.size)
        var bestScore = 0f
        var bestAreas = areas
        var bestCombination = if (debug) toString(combination) else null
        do {
            val mergedAreas = mergeAreas(areas, combination)
            if (mergedAreas != null) {
                if (debug) {
                    addIntermediateDebugImage(col, areas, mergedAreas)
                    log.debug (task!!.debugImages[task!!.debugImages.size - 1]!!.filename)
                    log.debug("  combination:" + toString(combination))
                }
                val score = calcScore(mergedAreas)
                if (score != null && score > bestScore) {
                    bestScore = score
                    bestAreas = mergedAreas
                    if (debug) bestCombination = toString(combination)
                }
                if (debug) log.debug { "  score:$score" } 
            }
            nextCombination(combination)
        } while (!combination[combination.size - 1]) // last boolean must be false
        if (debug) {
            log.debug { "best:$bestCombination" } 
            log.debug { "score:$bestScore" } 
            log.debug { "areas:" + toString(bestAreas) }
        }
        return bestAreas
    }

    /**
     * Distance between merged areas that resulted in indexed area. 0 if no merges were done
     */
    private var mergeDistances: MutableMap<Area?, Int>? = null

    /**
     * Merges areas with given combination
     *
     * @param combination If set, merges the indexed area with next area.
     *
     * @return List of areas merged by combination. null if invalid combination.
     * mergeDistances map is also updated as a side effect.
     */
    private fun mergeAreas(areas: List<Area>, combination: BooleanArray): List<Area>? {
        val mergedAreas: MutableList<Area> = ArrayList()
        mergeDistances = HashMap()
        var prev: Area? = null
        var maxDistance = 0
        for (i in combination.indices) {
            var area = areas[i]
            if (prev != null) {
                var distance: Int
                distance = if (area.column!!.isVertical) {
                    area.y - prev.maxY
                } else {
                    area.x - prev.maxX
                }
                if (distance > maxDistance) {
                    maxDistance = distance
                }
                area = prev.merge(area)
                if (area.majorDim > maxSize) {
                    return null
                }
                area.isChanged = true
            }
            if (combination[i]) {
                prev = area
            } else {
                mergedAreas.add(area)
                mergeDistances!![area] = maxDistance
                prev = null
            }
        }
        // last boolean is always false
        return mergedAreas
    }

    /**
     * Determines the scale factor used for size parameters. This is done
     * to account for compressed fonts that are not perfect squares.
     */
    private fun calcScale(col: Column): Float {
        if (col.isVertical) {
            // it seems that only horizontal title columns have compressed fonts
            return 1.0f
        }
        if (col.areas.size < 15) {
            // scale detection is not reliable for small columns, use the default
            return 1.0f
        }

        // calculate upper quartile width
        val sortedAreas: MutableList<Area> = ArrayList()
        sortedAreas.addAll(col.areas)
        sortedAreas.sortWith(Comparator { o1, o2 ->
            val i1 = o1.width
            val i2 = o2.width
            i1.compareTo(i2)
        })
        val floor = floor((col.areas.size * 0.75f).toDouble()).toInt()
        val ceil = ceil((col.areas.size * 0.75f).toDouble()).toInt()
        val width1 = sortedAreas[floor].width
        val width2 = sortedAreas[ceil].width
        val width = (width1 + width2) / 2

        // calculate scale
        val reference = col.height
        var scale = width.toFloat() / reference
        if (scale > 0.8f) scale = 1.0f
        if (scale < 0.6f) scale = 0.6f
        if (debug && scale != 1.0f) log.debug { "col:$col scale:$scale" } 
        return scale
    }

    private fun toString(areas: List<Area?>): String {
        var s = ""
        for (area in areas) {
            s += area.toString() + " "
        }
        return s
    }

    private fun calcScore(areas: List<Area>): Float {
        if (debug) log.debug { "  targetSize:$targetSize maxSize:$maxSize" } 
        var scoreSum = 0f
        for (i in areas.indices) {
            val area = areas[i]
            val size = area.majorDim
            var score: Float
            score = if (size <= targetSize) {
                val ratio = size.toFloat() / targetSize
                //if (ratio < 0.1f) ratio = 0.1f;
                ratio.toDouble().pow(1.0).toFloat()
            } else {
                val ratio = 1 - (size - targetSize).toFloat() / (maxSize - targetSize)
                ratio.toDouble().pow(1.5).toFloat()
            }
            var distance = mergeDistances!![area]!!
            if (distance > maxSize) distance = maxSize
            val distanceRatio = 1.0f * distance / maxSize
            score *= (1f - distanceRatio).toDouble().pow(1.0).toFloat()

            //if (debug) System.err.println("  area:"+area+" size:"+size+" score:"+score);
            if (debug) log.debug { "  area:$area size:$size distance:$distance score:$score" } 
            scoreSum += score
        }
        return scoreSum / areas.size
    }

    /**
     * true if debugging is on at this moment
     */
    private var debug = false

    /**
     * @return true if area list should be debugged. debug state is saved to debug variable.
     */
    private fun isDebug(areas: List<Area?>): Boolean {
        if (debugAll) {
            debug = true
            return debug
        }
        if (debugRect != null) {
            for (area in areas) {
                if (area!!.intersects(debugRect)) {
                    debug = true
                    return debug
                }
            }
        }
        debug = false
        return debug
    }

    /**
     * Generates debug image from the current algorithm state
     *
     * @param chunkAreas Original areas in the current chunk
     * @param mergedAreas Merged areas in the current chunk
     */
    
    private fun addIntermediateDebugImage(col: Column, chunkAreas: List<Area>, mergedAreas: List<Area>) {
        task!!.collectAreas()

        // paint merged areas inside chunk
        val areas: MutableList<Area> = ArrayList()
        for (area in task!!.areas!!) {
            if (!chunkAreas.contains(area)) {
                areas.add(area)
            }
        }
        areas.addAll(mergedAreas)
        val image = task!!.createDefaultDebugImage(areas, task!!.columns)

        // paint rectangle around chunk
        val chunkRect: KotomoRectangle
        val firstArea = chunkAreas[0]
        val lastArea = chunkAreas[chunkAreas.size - 1]
        chunkRect = if (col.isVertical) {
            KotomoRectangle(
                col.x - 1, firstArea.y,
                col.width + 2, lastArea.maxY - firstArea.y + 1
            )
        } else {
            KotomoRectangle(
                firstArea.x, col.y - 1,
                lastArea.maxX - firstArea.x + 1, col.height + 2
            )
        }
        paintRectangle(image, chunkRect, Colors.GREEN)
        task!!.addDebugImage(image, "mergeareas", col.isVertical)
    }

    
    override fun addDebugImages() {
        task!!.addDefaultDebugImage("mergeareas", Parameters.vertical)
    }

    companion object {
        /**
         * Maximum area size along major dimension compared to minor dimensions.
         *
         * (vertical) area height must be below (column width * MAX_AREA_SIZE)
         */
        private const val MAX_AREA_SIZE = 1.5f

        /**
         * Maximum number of areas that are tested for combinations at once.
         */
        private const val MAX_CHUNK_SIZE = 10

        /**
         * Advance combination by one. For example: ftff -> ttff -> fftf -> tftf
         */
        private fun nextCombination(combination: BooleanArray) {
            for (i in combination.indices) {
                combination[i] = !combination[i]
                if (combination[i]) {
                    break
                }
            }
        }

        private fun toString(array: BooleanArray): String {
            var s = ""
            for (b in array) {
                s += if (b) {
                    "t"
                } else {
                    "f"
                }
            }
            return s
        }
    }
}
