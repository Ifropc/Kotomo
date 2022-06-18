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

import io.github.ifropc.kotomo.ocr.Point
import io.github.ifropc.kotomo.ocr.Rectangle

/**
 * R-tree index used for finding intersecting rectangles (columns or areas)
 */
class RTree<T : HasRectangle> {
    /**
     * If true, this object is a leaf node (contains rectangles).
     * If false, this object contains references to child nodes.
     */
    private var isLeaf = true

    /**
     * If true, this node won't be splitted to child nodes.
     */
    private var noSplit = false

    /**
     * Contains all rectangles inserted to this node. Will grow as new values
     * are inserted but it will not shrink after values are removed
     */
    private var coverage: Rectangle? = null

    /**
     * Contains all possible rectangles that can be inserted to this node.
     *
     * Difference between coverage and bounds is that coverage represents
     * actual rectangles inserted so far, bounds represents all rectangles
     * that might be inserted in the future.
     */
    private val bounds: Rectangle

    /**
     * Rectangles in this node, only if leaf
     */
    private var values: MutableList<T>? = ArrayList()

    /**
     * Childred of this node, only if not leaf
     */
    private var nodes: MutableList<RTree<T>>? = null

    /**
     * Overflow node contains rectangles that cross node boundaries.
     */
    private var overflow: RTree<T>? = null

    /**
     * Creates a new node
     *
     * @param bound Outer limit of the rectangles that can be inserted to this node
     */
    constructor(bounds: Rectangle) {
        this.bounds = bounds
    }

    /**
     * Creates a new node that can hold all rectangles inside image
     */
    constructor(image: Array<BooleanArray>) {
        val width = image!!.size
        val height = image[0]!!.size
        bounds = Rectangle(0, 0, width, height)
    }

    /**
     * Creates a new index that can hold all rectangles inside image.
     * Populates the index with argument rectangles.
     */
    constructor(image: Array<BooleanArray>, values: List<T>?) : this(image) {
        for (value in values!!) {
            add(value)
        }
    }

    /**
     * Adds rectangle to this node
     */
    fun add(value: T) {
        if (isLeaf) {
            values!!.add(value)
            if (values!!.size > MAX_VALUES_PER_NODE && !noSplit) {
                split()
            }
        } else {
            var target: RTree<T>? = null
            for (node in nodes!!) {
                if (node.bounds.contains(value.rectangle!!)) {
                    target = node
                    break
                }
            }
            if (target == null) {
                target = overflow
            }
            target!!.add(value)
        }
        coverage = if (coverage == null) {
            value.rectangle
        } else {
            coverage!!.union(value.rectangle!!)
        }
    }

    /**
     * Adds list of rectangles to this node
     */
    fun addAll(values: List<T>) {
        for (value in values) {
            add(value)
        }
    }

    /**
     * Removes rectangle from this node. Index is not rebalanced after remove.
     *
     * @return true if rectangle was found and removed
     */
    fun remove(value: T): Boolean {
        return if (isLeaf) {
            values!!.remove(value)
        } else {
            var target: RTree<T>? = null
            for (node in nodes!!) {
                if (node.coverage != null && node.coverage!!.contains(value!!.rectangle!!)) {
                    target = node
                    break
                }
            }
            if (target == null) {
                target = overflow
            }
            target!!.remove(value)
        }
    }

    /**
     * Returns rectangles that intersect with the argument rectangle.
     *
     * @param rect Rectangle used for intersect search
     */
    operator fun get(rect: Rectangle?): MutableList<T> {
        val results: MutableList<T> = ArrayList()
        if (isLeaf) {
            for (rectangle in values!!) {
                if (rectangle.rectangle!!.intersects(rect!!)) {
                    results.add(rectangle)
                }
            }
        } else {
            for (node in nodes!!) {
                if (node.coverage != null && node.coverage!!.intersects(rect!!)) {
                    results.addAll(node[rect])
                }
            }
            if (overflow!!.coverage != null && overflow!!.coverage!!.intersects(rect!!)) {
                results.addAll(overflow!![rect])
            }
        }
        return results
    }

    /**
     * Returns rectangles that intersect with the argument rectangle.
     *
     * @param rect Rectangle used for intersect search
     * @param skip This rectangle is not included in the return list.
     */
    operator fun get(rect: Rectangle?, skip: T): MutableList<T> {
        val results = get(rect)
        val i = results.iterator()
        while (i.hasNext()) {
            if (i.next() == skip) {
                i.remove()
            }
        }
        return results
    }

    /**
     * Return true if contains at least one rectangle that intersects with argument
     * rectangle.
     *
     * @param rect Rectangle used for intersect search
     */
    operator fun contains(rect: Rectangle?): Boolean {
        return get(rect).size > 0
    }

    /**
     * Return true if contains at least one rectangle that intersects with argument
     * rectangle.
     *
     * @param rect Rectangle used for intersect search
     * @param skip This object is not included
     */
    fun contains(rect: Rectangle?, skip: T): Boolean {
        return get(rect, skip).size > 0
    }

    /**
     * Splits this node into four child nodes plus one overflow node
     */
    private fun split() {

        // distribute values around average midpoint
        val midPoint = calcAverageMidpoint()
        val leftWidth = midPoint.x - bounds.x
        val rightWidth = bounds.x + bounds.width - midPoint.x
        val upHeight = midPoint.y - bounds.y
        val downHeight = bounds.y + bounds.height - midPoint.y
        nodes = ArrayList()
        var rect = Rectangle(bounds.x, bounds.y, leftWidth, upHeight)
        nodes!!.add(RTree(rect))
        rect = Rectangle(midPoint.x, bounds.y, rightWidth, upHeight)
        nodes!!.add(RTree(rect))
        rect = Rectangle(bounds.x, midPoint.y, leftWidth, downHeight)
        nodes!!.add(RTree(rect))
        rect = Rectangle(midPoint.x, midPoint.y, rightWidth, downHeight)
        nodes!!.add(RTree(rect))
        overflow = RTree(bounds)
        for (value in values!!) {
            var target: RTree<T>? = null
            for (node in nodes!!) {
                if (node.bounds.contains(value.rectangle!!)) {
                    target = node
                    break
                }
            }
            if (target == null) {
                target = overflow
            }
            if (target!!.values!!.size == MAX_VALUES_PER_NODE) {
                noSplit()
                return
            } else {
                target.add(value)
            }
        }
        isLeaf = false
        values = null
    }

    private fun noSplit() {
        noSplit = true
        nodes = null
        overflow = null
    }

    /**
     * Calculates average midpoint of all rectangles
     */
    private fun calcAverageMidpoint(): Point {
        var x = 0
        var y = 0
        for (value in values!!) {
            val midPoint = value.midpoint
            x += midPoint!!.x
            y += midPoint.y
        }
        x /= values!!.size
        y /= values!!.size
        return Point(x, y)
    }

    /**
     * Returns all values from this node and child nodes. If called from
     * the root node, returns all values in the index.
     */
    val all: MutableList<T>
        get() {
            val collected: MutableList<T> = ArrayList()
            if (isLeaf) {
                collected.addAll(values!!)
            } else {
                for (node in nodes!!) {
                    collected.addAll(node.all)
                }
                collected.addAll(overflow!!.all)
                return collected
            }
            return collected
        }

    companion object {
        /**
         * Maximum number of rectangles in single node. If this limit is reached,
         * node is split into child nodes.
         */
        private const val MAX_VALUES_PER_NODE = 16
    }
}
