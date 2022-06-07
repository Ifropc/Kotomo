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

import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Image that contains single character cropped from original image
 */
class SubImage(
    /**
     * Cropped around single character
     */
    var image: BufferedImage,
    /**
     * Location of the character in original image.
     */
    var location: Rectangle?,
    /**
     * Column that contains the rectangle. This can be null if manual rectangles
     * are used.
     */
    var column: Column?
) {
    val isVertical: Boolean
        get() = column?.isVertical ?: true
    val minX: Int
        get() = location!!.x
    val maxX: Int
        get() = location!!.x + location!!.width - 1
    val minY: Int
        get() = location!!.y
    val maxY: Int
        get() = location!!.y + location!!.height - 1
    val midX: Int
        get() = location!!.x + location!!.width / 2
    val midY: Int
        get() = location!!.y + location!!.height / 2

    override fun toString(): String {
        return location.toString()
    }
}
