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

import java.awt.Point
import java.awt.Rectangle

/**
 * Finds and marks areas that represent punctuation.
 * These areas are not merged with other areas.
 */
class FindPunctuation(task: AreaTask?) : AreaStep(task, "punctuation") {
    
    override fun runImpl() {
        for (col in task!!.columns!!) {
            markBrackets(col)
            markDotCom(col)
        }
    }

    private fun markBrackets(col: Column?) {
        for (area in col!!.areas) {
            if (isBracket(area, col)) {
                area.isPunctuation = true
                area.isChanged = true
            }
        }
    }

    /** Sub-area that must contain at least one pixel  */
    private val square = Rectangle()
    private var squareSize = 0

    /** Sub-area that must be empty  */
    private val triangle = Triangle()
    private var triangleWidth = 0
    private var triangleHeight = 0

    // area coordinates
    private var minX = 0
    private var midX = 0
    private var maxX = 0
    private var minY = 0
    private var midY = 0
    private var maxY = 0

    // area corner squares (true -> square has at least one pixel)
    var ne = false
    var nw = false
    var se = false
    var sw = false

    /**
     * Tests if area contains a bracket:｢［【〈｛(   or rotational equivalent
     */
    private fun isBracket(area: Area, col: Column): Boolean {
        if (area.majorMinorRatio > 0.44f || area.majorDim < 0.1f) {
            return false
        }
        if (area.minDim <= 2) {
            // TODO low-resolution version for [ and ｢ brackets
            return false
        }

        // calculate test polygon sizes
        squareSize = Math.ceil((area.minDim * TEST_SQUARE_SIZE).toDouble()).toInt()
        square.width = squareSize
        square.height = squareSize
        triangleWidth = Math.floor((area.width * TEST_TRIANGLE_SIZE).toDouble()).toInt()
        triangleHeight = Math.floor((area.height * TEST_TRIANGLE_SIZE).toDouble()).toInt()

        // mark area extremes for easy reference
        minX = area.x
        midX = area.midpoint.x
        maxX = area.maxX
        minY = area.y
        midY = area.midpoint.y
        maxY = area.maxY

        // check corner squares
        square.x = maxX - squareSize + 1
        square.y = minY
        ne = testSquare(square)
        square.x = minX
        square.y = minY
        nw = testSquare(square)
        square.x = maxX - squareSize + 1
        square.y = maxY - squareSize + 1
        se = testSquare(square)
        square.x = minX
        square.y = maxY - squareSize + 1
        sw = testSquare(square)
        return if (col.isVertical) {
            isBracketHorizontal(area)
        } else {
            isBracketVertical(area)
        }
    }

    /**
     * Tests if area contains a horizontal bracket (for example: ﹁ or ﹂)
     */
    private fun isBracketHorizontal(area: Area): Boolean {
        val triangleMinX = minX + (area.width - triangleWidth) / 2
        val triangleMaxX = maxX - (area.width - triangleWidth) / 2

        // top bracket ﹁  or horizontal (
        if (se && (ne || sw)) {
            triangle.v1 = Point(midX, maxY - triangleHeight)
            triangle.v2 = Point(triangleMaxX, maxY)
            triangle.v3 = Point(triangleMinX, maxY)
            if (!testTriangle(triangle)) {
                return true
            }
        }

        // bottom bracket ﹂ or horizontal )
        if (nw && (sw || ne)) {
            triangle.v1 = Point(midX, minY + triangleHeight)
            triangle.v2 = Point(triangleMinX, minY)
            triangle.v3 = Point(triangleMaxX, minY)
            if (!testTriangle(triangle)) {
                return true
            }
        }
        return false
    }

    /**
     * Tests if area contains a vertical bracket (for example: ｢ or ｣)
     */
    private fun isBracketVertical(area: Area): Boolean {
        val triangleMinY = minY + (area.height - triangleHeight) / 2
        val triangleMaxY = maxY - (area.height - triangleHeight) / 2

        // left bracket ｢ or (
        if (ne && (nw || se)) {
            triangle.v1 = Point(maxX - triangleWidth, midY)
            triangle.v2 = Point(maxX, triangleMinY)
            triangle.v3 = Point(maxX, triangleMaxY)
            if (!testTriangle(triangle)) {
                return true
            }
        }

        // right bracket ｣ or )
        if (sw && (se || nw)) {
            triangle.v1 = Point(minX + triangleWidth, midY)
            triangle.v2 = Point(minX, triangleMaxY)
            triangle.v3 = Point(minX, triangleMinY)
            if (!testTriangle(triangle)) {
                return true
            }
        }
        return false
    }

    private inner class Triangle {
        var v1: Point? = null
        var v2: Point? = null
        var v3: Point? = null
        override fun toString(): String {
            return "v1:$v1 v2:$v2 v3:$v3"
        }
    }

    /**
     * Tests if there is at least one pixel inside triangle
     */
    private fun testTriangle(t: Triangle): Boolean {
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val pt = Point(x, y)
                if (!pointInsideTriangle(pt, t)) {
                    continue
                }
                if (task!!.getPixel(x, y)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if pt is inside triangle t
     */
    private fun pointInsideTriangle(pt: Point, t: Triangle): Boolean {

        // https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
        val d1: Float
        val d2: Float
        val d3: Float
        val has_neg: Boolean
        val has_pos: Boolean
        d1 = sign(pt, t.v1, t.v2)
        d2 = sign(pt, t.v2, t.v3)
        d3 = sign(pt, t.v3, t.v1)
        has_neg = d1 < 0 || d2 < 0 || d3 < 0
        has_pos = d1 > 0 || d2 > 0 || d3 > 0
        return !(has_neg && has_pos)
    }

    private fun sign(p1: Point, p2: Point?, p3: Point?): Float {
        return ((p1.x - p3!!.x) * (p2!!.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)).toFloat()
    }

    /**
     * Tests if square contains at least one pixel
     */
    private fun testSquare(square: Rectangle): Boolean {
        for (x in square.x until square.x + square.width) {
            for (y in square.y until square.y + square.height) {
                if (task!!.getPixel(x, y)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Mark areas that represent dot or comma in bottom right corner
     */
    private fun markDotCom(col: Column?) {
        for (i in 1 until col!!.areas.size) {
            val prev = col.areas[i - 1]
            val area = col.areas[i]
            var next: Area? = null
            if (i < col.areas.size - 1) {
                next = col.areas[i + 1]
            }
            var size: Boolean
            var location: Boolean
            var distance: Boolean
            if (par.vertical) {
                size = area.maxDim <= Math.ceil((0.35f * col.width).toDouble())
                location = col.maxX - area.maxX < col.width * 0.25f
                distance = if (next == null) {
                    true
                } else {
                    val prevDist = area.y - prev.maxY
                    val nextDist = next.y - area.maxY
                    prevDist < nextDist
                }
            } else {
                size = area.maxDim <= Math.ceil((0.35f * col.height).toDouble())
                location = col.maxY - area.maxY < col.height * 0.25f
                distance = if (next == null) {
                    true
                } else {
                    val prevDist = area.x - prev.maxX
                    val nextDist = next.x - area.maxX
                    prevDist < nextDist
                }
            }
            if (size && location && distance) {
                area.isPunctuation = true
                area.isChanged = true
            }
        }
    }

    
    override fun addDebugImages() {
        task!!.addDefaultDebugImage("punctuation", par.vertical)
    }

    companion object {
        /** square width and height (relative to area size).  */
        private const val TEST_SQUARE_SIZE = 0.15f

        /** triangle width and height (relative to area size).  */
        private const val TEST_TRIANGLE_SIZE = 0.55f
    }
}
