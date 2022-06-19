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
package io.github.ifropc.kotomo.ocr

import io.github.ifropc.kotomo.util.addBits
import io.github.ifropc.kotomo.util.countBits
import io.github.ifropc.kotomo.util.findBounds

/**
 * Splits large components into subcomponents along x and y axis.
 */
class ComponentSplit {
    /**
     * Splits the component
     */
    fun run(component: Component): List<Component> {
        if (!SPLIT) {
            val noSplit: MutableList<Component> = ArrayList()
            noSplit.add(component)
            return noSplit
        }

        // find split points in each axis. if null, don't split
        val splitX = findSplitPointX(component)
        val splitY = findSplitPointY(component)

        // split in each direction
        val splittedX = splitX(component, splitX)
        val splittedY: MutableList<Component> = ArrayList()
        for (comp in splittedX) {
            splittedY.addAll(splitY(comp, splitY))
        }
        return splittedY
    }

    /**
     * Finds split point in x-axis. Null if component is too small.
     */
    private fun findSplitPointX(component: Component?): Int? {
        val width = component!!.bounds!!.width
        return if (width < MIN_SPLIT_SIZE) {
            null
        } else component.bounds!!.x + width / 2
    }

    /**
     * Finds split point in y-axis. Null if component is too small.
     */
    private fun findSplitPointY(component: Component?): Int? {
        val height = component!!.bounds!!.height
        return if (height < MIN_SPLIT_SIZE) {
            null
        } else component.bounds!!.y + height / 2
    }

    /**
     * Splits the component along x axis
     *
     * @param splitX Split location. Returns the component without splitting if null.
     */
    private fun splitX(component: Component, splitX: Int?): List<Component> {
        val bounds = component.bounds
        val splitted: MutableList<Component> = ArrayList()
        if (splitX == null) {
            splitted.add(component)
            return splitted
        }
        val left = Component()
        left.bounds = Rectangle(bounds!!.x, bounds.y, splitX - bounds.x + 1, bounds.height)
        left.matrix = IntArray(32)
        addBits(component.matrix, left.matrix, left.bounds!!)
        left.pixels = countBits(left.matrix)
        val right = Component()
        right.bounds = Rectangle(splitX + 1, bounds.y, bounds.width - left.bounds!!.width, bounds.height)
        right.matrix = IntArray(32)
        addBits(component.matrix, right.matrix, right.bounds!!)
        right.pixels = countBits(right.matrix)
        if (left.pixels > 0) {
            left.bounds = findBounds(left.matrix)
            splitted.add(left)
        }
        if (right.pixels > 0) {
            right.bounds = findBounds(right.matrix)
            splitted.add(right)
        }
        return splitted
    }

    /**
     * Splits the component along y axis
     *
     * @param splitY Split location. Returns the component without splitting if null.
     */
    private fun splitY(component: Component, splitY: Int?): List<Component> {
        val bounds = component.bounds
        val splitted: MutableList<Component> = ArrayList()
        if (splitY == null) {
            splitted.add(component)
            return splitted
        }
        val up = Component()
        up.bounds = Rectangle(bounds!!.x, bounds.y, bounds.width, splitY - bounds.y + 1)
        up.matrix = IntArray(32)
        addBits(component.matrix, up.matrix, up.bounds!!)
        up.pixels = countBits(up.matrix)
        val down = Component()
        down.bounds = Rectangle(bounds.x, splitY + 1, bounds.width, bounds.height - up.bounds!!.height)
        down.matrix = IntArray(32)
        addBits(component.matrix, down.matrix, down.bounds!!)
        down.pixels = countBits(down.matrix)
        if (up.pixels > 0) {
            up.bounds = findBounds(up.matrix)
            splitted.add(up)
        }
        if (down.pixels > 0) {
            down.bounds = findBounds(down.matrix)
            splitted.add(down)
        }
        return splitted
    }

    companion object {
        /**
         * Component axis must be larger or equal to be splitted
         */
        private const val MIN_SPLIT_SIZE = 20
        private const val SPLIT = false // experimental
    }
}
