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

package io.github.ifropc.kotomo.ocr.entities

interface KotomoImage {
    val width: Int
    val height: Int

    fun getRGB(x: Int, y: Int): KotomoColor
}

data class KotomoColor(val red: Int, val green: Int, val blue: Int, val alpha: Int) {
    constructor(red: Int, green: Int, blue: Int) : this(red, green, blue, 255)

    @Deprecated("Backward compatiblity with awt color")
    fun toInt(): Int {
        return (alpha and 255 shl 24) or (red and 255 shl 16) or (green and 255 shl 8) or (blue and 255 shl 0)
    }

    @Deprecated("Backward compatibility with awt color")
    val rgb = toInt()
}

object Colors {
    val WHITE = KotomoColor(255, 255, 255)
    val BLACK = KotomoColor(0, 0, 0)
    val LIGHT_GRAY = KotomoColor(192, 192, 192);
    val GRAY = KotomoColor(128, 128, 128);
    val DARK_GRAY = KotomoColor(64, 64, 64);
    val RED = KotomoColor(255, 0, 0);
    val PINK = KotomoColor(255, 175, 175);
    val ORANGE = KotomoColor(255, 200, 0);
    val YELLOW = KotomoColor(255, 255, 0);
    val GREEN = KotomoColor(0, 255, 0);
    val MAGENTA = KotomoColor(255, 0, 255);
    val CYAN = KotomoColor(0, 255, 255);
    val BLUE = KotomoColor(0, 0, 255);
}

