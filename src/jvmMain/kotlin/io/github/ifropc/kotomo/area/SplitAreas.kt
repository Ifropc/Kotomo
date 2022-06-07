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

/**
 * Splits too long areas
 */
class SplitAreas(task: AreaTask?) : AreaStep(task, "splitareas") {
    /** Area must be longer than this to be considered for splitting  */
    private val splitMinLength = 1.25f

    /** Split is considered starting from this line  */
    private val scanFrom = 0.25f

    /** Split is considered until this line (% of major dimension)  */
    private val scanTo = 0.75f

    /** Split is possible in line with this many or less ratio of pixels  */
    private val maxPixelsPrct = 0.14f
    @Throws(Exception::class)
    override fun runImpl() {
        for (col in task!!.columns!!) {
            val minLength = Math.ceil((col.minDim * splitMinLength).toDouble()).toInt()
            var i = 0
            while (i < col!!.areas.size) {
                val area = col.areas[i]
                if (area.height < 10 && area.width < 10) {
                    i++
                    continue
                }
                val refLength = if (par.vertical) area.height else area.width
                if (refLength > minLength) {
                    if (splitArea(area, col)) {
                        i-- // splitted areas are added to the end of list
                    }
                }
                i++
            }
        }
    }

    private fun splitArea(area: Area, col: Column): Boolean {
        return if (par.vertical) {
            splitVertical(area, col)
        } else {
            splitHorizontal(area, col)
        }
    }

    private fun splitVertical(area: Area, col: Column): Boolean {

        // find line with least amount of pixels 
        val minY = area.y + Math.floor((area.height * scanFrom).toDouble()).toInt()
        val maxY = area.y + Math.ceil((area.height * scanTo).toDouble()).toInt()
        var minPixels = Math.ceil((area.width * maxPixelsPrct).toDouble()).toInt() + 1
        var splitAt = -1
        if (minY <= 0 || maxY >= task!!.height - 1) {
            return false
        }

        // iterate from the middle: 5,6,4,7,3,8,...
        var delta = 0
        var y = minY + (maxY - minY) / 2
        while (y >= minY && y <= maxY) {
            val pixels = task!!.countPixelsHorizontal(area.x, area.maxX, y)
            if (pixels < minPixels) {
                minPixels = pixels
                splitAt = y
            }
            ++delta
            if (delta == (maxY - minY) / 4) {
                // give priority to center
                minPixels = Math.floor((minPixels * 0.9f).toDouble()).toInt()
            }
            y = if (delta % 2 == 0) y + delta else y - delta
        }
        return if (splitAt > -1) {

            // check which area should splitAt line be assigned
            var up = 0
            var down = 0
            for (x in col.x..col.maxX) {
                if (task!!.getPixel(x, splitAt)) {
                    if (task!!.getPixel(x, splitAt - 1)) {
                        ++up
                    }
                    if (task!!.getPixel(x, splitAt + 1)) {
                        ++down
                    }
                }
            }
            if (up > down) {
                splitAt++
            }
            val index = col!!.areas.indexOf(area)
            col.areas.remove(area)
            col.areas.addAll(index, area!!.splitY(splitAt))
            true
        } else {
            false
        }
    }

    private fun splitHorizontal(area: Area, col: Column): Boolean {

        // find line with least amount of pixels 
        val minX = area.x + Math.floor((area.width * scanFrom).toDouble()).toInt()
        val maxX = area.x + Math.ceil((area.width * scanTo).toDouble()).toInt()
        var minPixels = Math.ceil((area.height * maxPixelsPrct).toDouble()).toInt()
        var splitAt = -1
        if (minX <= 0 || maxX >= task!!.width - 1) {
            return false
        }

        // iterate from the middle: 5,6,4,7,3,8,...
        var delta = 0
        var x = minX + (maxX - minX) / 2
        while (x >= minX && x <= maxX) {
            val pixels = task!!.countPixelsVertical(x, area.y, area.maxY)
            if (pixels < minPixels) {
                minPixels = pixels
                splitAt = x
            }
            ++delta
            if (delta == (maxX - minX) / 4) {
                // give priority to center
                minPixels = Math.floor((minPixels * 0.9f).toDouble()).toInt()
            }
            x = if (delta % 2 == 0) x + delta else x - delta
        }

//		// iterate pairs of lines, find pair that don't have any touching pixels 
//		
//		if (splitAt == -1) {
//			pairs: for (int x = minX ; x < maxX-1 ; x++) {
//				for (int y = area.y ; y <= area.maxY ; y++) {
//					if (task.hasPixel(x, y) && task.hasPixel(x+1, y)) {
//						continue pairs;
//					}
//				}
//				splitAt = x;
//				break pairs;
//			}
//		}
        return if (splitAt > -1) {

            // check which area should splitAt line be assigned
            var left = 0
            var right = 0
            for (y in col.y..col.maxY) {
                if (task!!.getPixel(splitAt, y)) {
                    if (task!!.getPixel(splitAt - 1, y)) {
                        ++left
                    }
                    if (task!!.getPixel(splitAt + 1, y)) {
                        ++right
                    }
                }
            }
            if (left > right) {
                splitAt++
            }
            val index = col!!.areas.indexOf(area)
            col.areas.remove(area)
            col.areas.addAll(index, area!!.splitX(splitAt))
            true
        } else {
            false
        }
    }

    @Throws(Exception::class)
    override fun addDebugImages() {
        task!!.addDefaultDebugImage("splitareas", par.vertical)
    }
}
