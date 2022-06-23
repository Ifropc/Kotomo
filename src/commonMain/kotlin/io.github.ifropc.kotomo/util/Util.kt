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
package io.github.ifropc.kotomo.util

internal object Util {
    /**
     * Scales sourceValue to target value range
     */
    fun scale(
        sourceValue: Float,
        minSourceValue: Float, maxSourceValue: Float,
        targetValue1: Float, targetValue2: Float
    ): Float {
        var sourceValue = sourceValue
        if (minSourceValue > maxSourceValue) {
            throw Error("minSourceValue:$minSourceValue larger than maxSourceValue:$maxSourceValue")
        }
        if (sourceValue < minSourceValue) {
            sourceValue = minSourceValue
        } else if (sourceValue > maxSourceValue) {
            sourceValue = maxSourceValue
        }
        val scale = (sourceValue - minSourceValue) / (maxSourceValue - minSourceValue)
        return targetValue1 * (1 - scale) + targetValue2 * scale
    }
}

typealias Pixel = Pair<Int, Int>

val Pixel.x : Int
    get() = this.first

val Pixel.y : Int
    get() = this.second
