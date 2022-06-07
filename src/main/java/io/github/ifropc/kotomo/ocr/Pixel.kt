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

class Pixel(public val x: Int, val y: Int) {
    override fun hashCode(): Int {
        return x + 100000 * y
    }

    override fun equals(obj: Any?): Boolean {
        return (obj as Pixel?)!!.x == x && obj!!.y == y
    }

    fun isNeighbour(px2: Pixel): Boolean {
        val deltaX = Math.abs(x - px2.x)
        val deltaY = Math.abs(y - px2.y)
        return if (deltaX <= 1 && deltaY <= 1) {
            true
        } else {
            false
        }
    }

    override fun toString(): String {
        return "$x,$y"
    }
}