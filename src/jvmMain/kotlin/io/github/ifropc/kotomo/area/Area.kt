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

import java.awt.Color
import java.awt.Point
import java.awt.Rectangle

/**
 * Rectangle inside target image. Contains a single character after area detection
 * is completed. Might contain character fragments (radicals) during area detection.
 */
class Area(
    /**
     * Location of the area in target image
     */
    var rect: Rectangle,
    /**
     * Number of black pixels inside the area
     */
    var pixels: Int
) : HasRectangle {

    /**
     * If true, this area contains punctuation character (bracket, dot or comma)
     */
    var isPunctuation = false

    /**
     * If true, area has been changed in this step. Used for debug printing.
     */
    var isChanged = false

    /**
     * If set, this color is used in debug images
     */
    var debugColor: Color? = null

    /**
     * If true, this area was splitted (was originally connected by touching pixels)
     */
    var splitted = false
    /**
     * Minumum pixel RGB value in this area
     */
    /**
     * Mininum RGB value within area's pixels
     */
    var minRGB = 0

    /**
     * Column that contains this area
     */
    public var column: Column? = null

    /**
     * If true, this area has been removed from the image
     */
    var remove = false

    /**
     * Initial areas created during FindAreas but later merged into this area
     */
    var sourceAreas: MutableList<Area?> = mutableListOf()
    override val rectangle: Rectangle?
        get() = rect

    /**
     * Surface area.
     * @return
     */
    val size: Int
        get() = rect.width * rect.height
    val x: Int
        get() = rect.x
    val y: Int
        get() = rect.y
    val width: Int
        get() = rect.width
    val height: Int
        get() = rect.height

    /**
     * Right border.
     */
    val maxX: Int
        get() = rect.x + rect.width - 1

    /**
     * Bottom border.
     */
    val maxY: Int
        get() = rect.y + rect.height - 1

    /**
     * Min(width / height , height / width). Perfect square returns 1.0f, straigth line
     * returns close to 0.0f.
     */
    val ratio: Float
        get() {
            val r1 = 1.0f * rect.width / rect.height
            val r2 = 1.0f * rect.height / rect.width
            return if (r1 < r2) r1 else r2
        }

    // called before columns have been detected
    val majorMinorRatio: Float
        get() {
            if (column == null) {
                throw Error("Orientation not determined")
                // called before columns have been detected
            }
            return if (column!!.isVertical) {
                heightWidthRatio
            } else {
                widthHeightRatio
            }
        }
    val minorMajorRatio: Float
        get() = 1 / majorMinorRatio
    val widthHeightRatio: Float
        get() = 1.0f * rect.width / rect.height
    val heightWidthRatio: Float
        get() = 1.0f * rect.height / rect.width

    /**
     * Gets ratio of pixels to total area
     */
    val pixelDensity: Float
        get() = 1.0f * pixels / size

    /**
     * Midpoint of this area's rectangle.
     */
    override val midpoint: Point
        get() = Point(rect.x + rect.width / 2, rect.y + rect.height / 2)
    val minDim: Int
        get() = if (rect.width <= rect.height) rect.width else rect.height
    val maxDim: Int
        get() = if (rect.width >= rect.height) rect.width else rect.height// called before columns have been detected
    // TODO add orientation information to areas
    /**
     * @return vertical -> width, horizontal -> height
     */
    val minorDim: Int
        get() {
            if (column == null) {
                throw Error("Not implemented")
                // called before columns have been detected
                // TODO add orientation information to areas
            }
            return if (column!!.isVertical) {
                rect.width
            } else {
                rect.height
            }
        }

    /**
     * @return vertical -> height, horizontal -> width
     */
    val majorDim: Int
        get() {
            if (column == null) {
                throw Error("Not implemented")
            }
            return if (column!!.isVertical) {
                rect.height
            } else {
                rect.width
            }
        }
    val avgDim: Int
        get() = (rect.width + rect.height) / 2

    /**
     * @return true fn area contains Point(x,y)
     */
    fun contains(x: Int, y: Int): Boolean {
        return contains(Point(x, y))
    }

    /**
     * @return true in area contains point
     */
    operator fun contains(point: Point?): Boolean {
        if (point == null) {
            return false
        }
        return if (rect.contains(point)) {
            true
        } else {
            false
        }
    }

    /**
     * Merges the area's rectangle with argument area.
     */
    fun merge(area2: Area?): Area {
        val newArea = Area(rect.union(area2!!.rect), pixels + area2.pixels)
        newArea.pixels = pixels + area2.pixels
        newArea.column = column
        newArea.sourceAreas.addAll(sourceAreas)
        newArea.sourceAreas.addAll(area2.sourceAreas)
        newArea.minRGB = Math.min(minRGB, area2.minRGB)
        return newArea
    }

    /**
     * Splits the area into two areas
     */
    fun splitX(x: Int): List<Area> {
        val leftRect = Rectangle(rect.x, rect.y, x - rect.x, rect.height)
        val left = Area(leftRect, pixels / 2)
        left.isChanged = true
        left.column = column
        left.splitted = true
        left.minRGB = minRGB
        left.sourceAreas.addAll(sourceAreas)
        val rightRect = Rectangle(x, rect.y, rect.x + rect.width - x, rect.height)
        val right = Area(rightRect, pixels / 2)
        right.isChanged = true
        right.column = column
        right.splitted = true
        right.minRGB = minRGB
        right.sourceAreas.addAll(sourceAreas)
        val areas: MutableList<Area> = ArrayList()
        areas.add(left)
        areas.add(right)
        return areas
    }

    fun splitY(y: Int): List<Area> {
        val upRect = Rectangle(rect.x, rect.y, rect.width, y - rect.y)
        val up = Area(upRect, pixels / 2)
        up.isChanged = true
        up.column = column
        up.splitted = true
        up.sourceAreas.addAll(sourceAreas)
        val downRect = Rectangle(rect.x, y, rect.width, rect.y + rect.height - y)
        val down = Area(downRect, pixels / 2)
        down.isChanged = true
        down.column = column
        down.splitted = true
        down.sourceAreas.addAll(sourceAreas)
        val areas: MutableList<Area> = ArrayList()
        areas.add(up)
        areas.add(down)
        return areas
    }

    fun clone(): Area {
        val clone = Area(rect, pixels)
        clone.column = column
        clone.minRGB = minRGB
        clone.sourceAreas.addAll(sourceAreas)
        return clone
    }

    /**
     * @return true if area's rectangle intersect with argument rectangle
     */
    fun intersects(rect: Rectangle?): Boolean {
        return if (rect == null) {
            false
        } else {
            this.rect.intersects(rect)
        }
    }

    override fun toString(): String {
        return rect.x.toString() + "," + rect.y + "," + rect.width + ":" + rect.height
    }
}
