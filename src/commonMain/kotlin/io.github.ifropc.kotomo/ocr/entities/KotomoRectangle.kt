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

import kotlinx.serialization.Serializable

// Copy of java awt rectangle
@Serializable
data class KotomoRectangle(var x: Int, var y: Int, var width: Int, var height: Int) {
    constructor() : this(0, 0, 0, 0)

    constructor(r: KotomoRectangle) : this(r.x, r.y, r.width, r.height)


    operator fun contains(p: Point): Boolean {
        return this.contains(p.x, p.y)
    }

    fun contains(x: Int, y: Int): Boolean {
        return this.inside(x, y)
    }

    operator fun contains(r: KotomoRectangle): Boolean {
        return this.contains(r.x, r.y, r.width, r.height)
    }

    fun contains(X: Int, Y: Int, W: Int, H: Int): Boolean {
        var W = W
        var H = H
        var w = width
        var h = height
        return if (w or h or W or H < 0) {
            false
        } else {
            val x = x
            val y = y
            if (X >= x && Y >= y) {
                w += x
                W += X
                if (W <= X) {
                    if (w >= x || W > w) {
                        return false
                    }
                } else if (w >= x && W > w) {
                    return false
                }
                h += y
                H += Y
                if (H <= Y) {
                    if (h >= y || H > h) {
                        return false
                    }
                } else if (h >= y && H > h) {
                    return false
                }
                true
            } else {
                false
            }
        }
    }

    fun inside(X: Int, Y: Int): Boolean {
        var w = width
        var h = height
        return if (w or h < 0) {
            false
        } else {
            val x = x
            val y = y
            if (X >= x && Y >= y) {
                w += x
                h += y
                (w < x || w > X) && (h < y || h > Y)
            } else {
                false
            }
        }
    }

    fun union(r: KotomoRectangle): KotomoRectangle {
        var tx2 = width.toLong()
        var ty2 = height.toLong()
        return if (tx2 or ty2 < 0L) {
            KotomoRectangle(r)
        } else {
            var rx2: Long = r.width.toLong()
            var ry2: Long = r.height.toLong()
            if (rx2 or ry2 < 0L) {
                KotomoRectangle(this)
            } else {
                var tx1 = x
                var ty1 = y
                tx2 += tx1.toLong()
                ty2 += ty1.toLong()
                val rx1: Int = r.x
                val ry1: Int = r.y
                rx2 += rx1.toLong()
                ry2 += ry1.toLong()
                if (tx1 > rx1) {
                    tx1 = rx1
                }
                if (ty1 > ry1) {
                    ty1 = ry1
                }
                if (tx2 < rx2) {
                    tx2 = rx2
                }
                if (ty2 < ry2) {
                    ty2 = ry2
                }
                tx2 -= tx1.toLong()
                ty2 -= ty1.toLong()
                if (tx2 > 2147483647L) {
                    tx2 = 2147483647L
                }
                if (ty2 > 2147483647L) {
                    ty2 = 2147483647L
                }
                KotomoRectangle(tx1, ty1, tx2.toInt(), ty2.toInt())
            }
        }
    }

    fun intersects(r: KotomoRectangle): Boolean {
        var tw = width
        var th = height
        var rw: Int = r.width
        var rh: Int = r.height
        return if (rw > 0 && rh > 0 && tw > 0 && th > 0) {
            val tx = x
            val ty = y
            val rx: Int = r.x
            val ry: Int = r.y
            rw += rx
            rh += ry
            tw += tx
            th += ty
            (rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) && (th < ty || th > ry)
        } else {
            false
        }
    }

    fun intersection(r: KotomoRectangle): KotomoRectangle {
        var tx1 = x
        var ty1 = y
        val rx1: Int = r.x
        val ry1: Int = r.y
        var tx2 = tx1.toLong()
        tx2 += width.toLong()
        var ty2 = ty1.toLong()
        ty2 += height.toLong()
        var rx2 = rx1.toLong()
        rx2 += r.width.toLong()
        var ry2 = ry1.toLong()
        ry2 += r.height.toLong()
        if (tx1 < rx1) {
            tx1 = rx1
        }
        if (ty1 < ry1) {
            ty1 = ry1
        }
        if (tx2 > rx2) {
            tx2 = rx2
        }
        if (ty2 > ry2) {
            ty2 = ry2
        }
        tx2 -= tx1.toLong()
        ty2 -= ty1.toLong()
        if (tx2 < -2147483648L) {
            tx2 = -2147483648L
        }
        if (ty2 < -2147483648L) {
            ty2 = -2147483648L
        }
        return KotomoRectangle(tx1, ty1, tx2.toInt(), ty2.toInt())
    }
}
