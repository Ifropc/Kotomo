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
package io.github.ifropc.kotomo.jvm.area

import io.github.ifropc.kotomo.ocr.entities.Point
import io.github.ifropc.kotomo.ocr.entities.KotomoRectangle
import io.github.ifropc.kotomo.jvm.util.ImageUtil.createRectangle
import io.github.ifropc.kotomo.jvm.util.JVMUtil
import io.github.ifropc.kotomo.jvm.util.JVMUtil.runWithDebuggable
import io.github.ifropc.kotomo.config.Parameters
import mu.KotlinLogging
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.ceil

private val log = KotlinLogging.logger { }

/**
 * Finds columns that are connected to each other (continue text) in reading direction.
 */
class FindConnections(task: AreaTask?) : AreaStep(task, "connections") {
    private var index: RTree<Column>? = null

    override fun runImpl() {

        // initialize index
        index = RTree(task!!.binaryImage, task!!.columns)

        // find next column for each column
        for (col1 in task!!.columns!!) {
            findNextColumn(col1)
        }
    }

    /**
     * Finds columns that are closest to argument column in reading direction.
     * Normally this is only one column.
     */
    private fun findNextColumn(column: Column) {
        log.debug { "column:$column" }

        // find closest column in reading direction
        val probeSizeFactor = 1.75f
        val probe: KotomoRectangle
        probe = if (Parameters.vertical) {
            val probeSize = ceil((column.width * probeSizeFactor).toDouble()).toInt()
            KotomoRectangle(
                column.x - probeSize - 1, column.y - probeSize / 2,
                probeSize, probeSize
            )
        } else {
            val probeSize = ceil((column.height * probeSizeFactor).toDouble()).toInt()
            KotomoRectangle(
                column.x - probeSize / 2, column.maxY + 1,
                probeSize, probeSize
            )
        }
        log.debug { "  probe:$probe" }
        val targetColumns = index!![probe]

        // find closest column
        var target: Column? = null
        var distance = 100000f
        for (tempCol in targetColumns) {
            if (column === tempCol) {
                continue
            }
            if (tempCol.isFurigana) {
                continue
            }
            if (Parameters.vertical) {
                if (!probe.contains(tempCol.maxX, tempCol.y)) {
                    continue
                }
            } else {
                if (!probe.contains(tempCol.x, tempCol.y)) {
                    continue
                }
            }
            val start = getConnectionStartPoint(column)
            val end = getConnectionEndPoint(tempCol)
            val tempDist = start.distance(end).toFloat()
            if (tempDist < distance) {
                target = tempCol
                distance = tempDist
            }
        }
        if (target == null) {
            log.debug { "  no target found" }
            return
        }
        log.debug { "  target:$target distance:$distance" }

        // connected columns should not form a tree
        if (target.previousColumn != null) {
            log.debug { "  already connected" }
            return
        }

        // make sure that there's no divider between columns
        // test from endpoints
        var testBackground = createRectangle(
            getConnectionStartPoint(column),
            getConnectionEndPoint(target)
        )
        var backgroundPixels = task!!.countPixels(testBackground, true, true)
        if (backgroundPixels >= 2) {
            log.debug { "  rejected from endpoint" }
            return
        }
        // test from midpoint
        testBackground = createRectangle(
            column.midpoint,
            getConnectionEndPoint(target)
        )
        backgroundPixels = task!!.countPixels(testBackground, true, true)
        if (backgroundPixels >= 2) {
            log.debug { "  rejected from midpoint" }
            return
        }

        // check that columns have roughly the same width
        val columnWidth = column.minorDim
        val targetWidth = target.minorDim
        val limit = 0.75f
        if (columnWidth < targetWidth * limit || targetWidth < columnWidth * limit) {
            log.debug { "  rejected from width" }
            return
        }

        // check that there's not another column between
        val testAnotherColumn = createRectangle(
            getConnectionStartPoint(column),
            getConnectionEndPoint(target)
        )
        for (col in index!![testAnotherColumn]) {
            if (col === column || col === target) {
                continue
            }
            log.debug { "  rejected from crossing another column" }
            return
        }

        // mark the connection
        column.nextColumn = target
        target.previousColumn = column
    }


    override fun addDebugImages() {
        val image = runWithDebuggable(task!!) { task -> task!!.createDefaultDebugImage() }
        val g = image.createGraphics()
        g.paint = Color.BLUE
        for (column in task!!.columns!!) {
            paintNextColumn(column, g)
        }
        JVMUtil.withDebuggable(task!!) { task ->
            task!!.addDebugImage(image, "connections", Parameters.vertical)
        }
    }

    private fun getConnectionStartPoint(column: Column): Point {
        return if (column.isVertical) {
            Point(column.x, column.y)
        } else {
            Point(column.x, column.maxY)
        }
    }

    private fun getConnectionEndPoint(column: Column): Point {
        return if (column.isVertical) {
            Point(column.maxX, column.y)
        } else {
            Point(column.x, column.y)
        }
    }

    /**
     * Paints a line from column to nextColumn
     */
    private fun paintNextColumn(column: Column?, g: Graphics2D) {
        if (column == null) {
            return
        }
        val nextColumn = column.nextColumn ?: return
        val start = getConnectionStartPoint(column)
        val end = getConnectionEndPoint(nextColumn)
        g.drawLine(start.x, start.y, end.x, end.y)
    }
}
