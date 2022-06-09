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

import kotlin.math.sqrt

data class Point(var x: Int, var y: Int) {
    constructor(): this(0, 0)

    fun distance(pt: Point): Double {
        val px: Double = (pt.x - this.x).toDouble()
        val py: Double = (pt.y - this.y).toDouble()
        return sqrt(px * px + py * py)
    }
}
