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
package io.github.ifropc.kotomo.jvm.ocr

import io.github.ifropc.kotomo.jvm.util.isBitSet
import io.github.ifropc.kotomo.jvm.util.setBit
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import java.util.*

/**
 * Finds unconnected components from characters (pixel groups that are not touching)
 */
class ComponentFindUnconnected {
    private lateinit  var matrix: IntArray
    private var bounds: KotomoRectangle? = null
    private lateinit var visited: Array<BooleanArray>
    private var todo: Stack<Pixel>? = null
    private var pixels: MutableList<Pixel>? = null

    /**
     * Finds unconnected components (groups of pixels that are not touching)
     */
    fun run(component: Component): List<Component> {
        matrix = component.matrix
        bounds = component.bounds
        visited = Array(32) { BooleanArray(32) }
        todo = Stack()
        pixels = ArrayList()
        val components: MutableList<Component> = ArrayList()
        for (x in bounds!!.x until bounds!!.x + bounds!!.width) {
            for (y in bounds!!.y until bounds!!.y + bounds!!.height) {
                todo!!.add(Pixel(x, y))
                while (!todo!!.isEmpty()) {
                    checkPixel(todo!!.pop())
                }
                if (pixels!!.size == 0) {
                    continue
                }
                components.add(buildNewComponent())
                pixels!!.clear()
            }
        }
        return components
    }

    /**
     * Checks if this pixel is set, then add to current list of pixels
     */
    private fun checkPixel(pixel: Pixel) {
        if (pixel.x < bounds!!.x || pixel.x >= bounds!!.x + bounds!!.width || pixel.y < bounds!!.y || pixel.y >= bounds!!.y + bounds!!.height) {
            return
        }
        if (visited[pixel.x][pixel.y]) {
            return
        }
        if (isBitSet(pixel.x, pixel.y, matrix)) {
            pixels!!.add(pixel)
            todo!!.add(Pixel(pixel.x - 1, pixel.y))
            todo!!.add(Pixel(pixel.x + 1, pixel.y))
            todo!!.add(Pixel(pixel.x, pixel.y - 1))
            todo!!.add(Pixel(pixel.x, pixel.y + 1))
        }
        visited[pixel.x][pixel.y] = true
    }

    /**
     * Create a new component object from current list of pixels
     */
    private fun buildNewComponent(): Component {
        val component = Component()
        var minX = 31
        var minY = 31
        var maxX = 0
        var maxY = 0
        component.matrix = IntArray(32)
        for (pixel in pixels!!) {
            setBit(pixel.x, pixel.y, component.matrix)
            if (pixel.x < minX) minX = pixel.x
            if (pixel.y < minY) minY = pixel.y
            if (pixel.x > maxX) maxX = pixel.x
            if (pixel.y > maxY) maxY = pixel.y
        }
        component.pixels = pixels!!.size
        component.bounds = KotomoRectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
        return component
    }
}
