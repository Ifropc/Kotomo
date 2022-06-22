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

import io.github.ifropc.kotomo.ocr.KotomoRectangle
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

fun moveMatrix(matrix: IntArray, horizontal: Int, vertical: Int): IntArray {
    val matrix2 = IntArray(32)
    for (y in matrix.indices) {
        val newY = y + vertical
        if (newY < 0 || newY > 31) {
            continue
        }
        if (horizontal >= 0) {
            matrix2[newY] = matrix[y] ushr horizontal
        } else {
            matrix2[newY] = matrix[y] shl -1 * horizontal
        }
    }
    return matrix2
}

/**
 * Builds a matrix that represents bits around the argument matrix
 *
 * @param layers how many halo layers are generated
 */
fun buildMatrixHalo(matrix: IntArray, layers: Int): ArrayList<IntArray> {
    var matrix = matrix
    val halo = ArrayList<IntArray>()
    if (layers > 1) {
        matrix = matrix.clone()
    }
    for (i in 1..layers) {
        val layer = IntArray(32)
        for (y in 0..31) {
            for (x in 0..31) {
                if (isHaloBit(x, y, matrix)) {
                    layer[y] = layer[y] or 1
                }
                if (x < 31) {
                    layer[y] = layer[y] shl 1
                }
            }
        }
        halo.add(layer)
        if (i < layers) {
            for (y in 0..31) {
                matrix[y] = matrix[y] or layer[y]
            }
        }
    }
    return halo
}

/**
 * Is the bit off by one from matrix
 */
private fun isHaloBit(x: Int, y: Int, matrix: IntArray): Boolean {
    if (isBitSet(x, y, matrix)) {
        return false
    }
    for (y2 in y - 1..y + 1) {
        for (x2 in x - 1..x + 1) {
            if (isBitSet(x2, y2, matrix)) {
                return true
            }
        }
    }
    return false
}

/**
 * True if argument bit is set in matrix. false if outside matrix.
 */
fun isBitSet(x: Int, y: Int, matrix: IntArray): Boolean {
    if (x < 0 || x >= 32 || y < 0 || y >= 32) {
        return false
    }
    val row = matrix[y]
    return (row and (1 shl (31 - x))) != 0
}

/**
 * Sets bit inside matrix. returns if outside matrix.
 */
fun setBit(x: Int, y: Int, matrix: IntArray) {

    // https://stackoverflow.com/questions/12015598/how-to-set-unset-a-bit-at-specific-position-of-a-long
    if (x < 0 || x >= 32 || y < 0 || y >= 32) {
        return
    }
    matrix[y] = matrix[y] or (1 shl (31 - x))
}

/**
 * Clears bit inside matrix. returns if outside matrix.
 */
fun clearBit(x: Int, y: Int, matrix: IntArray) {

    // https://stackoverflow.com/questions/12015598/how-to-set-unset-a-bit-at-specific-position-of-a-long
    if (x < 0 || x >= 32 || y < 0 || y >= 32) {
        return
    }
    matrix[y] = matrix[y] and (1 shl (31 - x)).inv()
}

/**
 * Counts set bits in matrix
 */
fun countBits(matrix: IntArray): Int {
    var bits = 0
    for (row in matrix) {
        bits += Integer.bitCount(row)
    }
    return bits
}

/**
 * Counts set bits inside bounds in matrix
 */
fun countBits(matrix: IntArray, bounds: KotomoRectangle): Int {
    val boundMatrix = IntArray(32)
    addBits(matrix, boundMatrix, bounds)
    var bits = 0
    for (y in bounds.y until bounds.y + bounds.height) {
        bits += Integer.bitCount(boundMatrix[y])
    }
    return bits
}

/**
 * Prints matrix to stdout
 */
fun debugPrintMatrix(matrix: IntArray?) {
    for (y in 0..31) {
        var s =""
        for (x in 0..31) {
            s += if (isBitSet(x, y, matrix!!)) {
                "x"
            } else {
                "."
            }
        }
        log.debug { s }
    }
}

/**
 * Prints two matrices side by side to stdout
 */
fun debugPrintMatrix(matrix1: IntArray?, matrix2: IntArray?) {
    for (y in 0..31) {
        var matrix1Line = ""
        var matrix2Line = ""
        for (x in 0..31) {
            matrix1Line += if (isBitSet(x, y, matrix1!!)) {
                "x"
            } else {
                "."
            }
            matrix2Line += if (isBitSet(x, y, matrix2!!)) {
                "x"
            } else {
                "."
            }
        }
        log.debug { "$matrix1Line $matrix2Line" } 
    }
}

/**
 * Copies bits from source to target matrix. Restricted to rect area.
 * Bits are translated by deltaX/Y amount.
 *
 * @param clearTargetRect If true, target rectangle is first cleared (set to zero bits)). If false,
 * bits are added but not removed.
 */
fun copyBits(
    source: IntArray, target: IntArray, rect: KotomoRectangle, deltaX: Int, deltaY: Int,
    clearTargetRect: Boolean
) {

    // clear target bits
    if (clearTargetRect) {
        var targetMinX = rect.x + deltaX
        var targetMaxX = rect.x + rect.width - 1 + deltaX
        var targetMinY = rect.y + deltaY
        var targetMaxY = rect.y + rect.height - 1 + deltaY
        if (targetMinX < 0) targetMinX = 0
        if (targetMaxX > 31) targetMaxX = 31
        if (targetMinY < 0) targetMinY = 0
        if (targetMaxY > 31) targetMaxY = 31
        val targetWidth = targetMaxX - targetMinX + 1
        val mask = ((0.inv() shl (32 - targetWidth)) ushr targetMinX).inv()
        for (y in targetMinY..targetMaxY) {
            target[y] = target[y] and mask
        }
    }

    // mask source bits (copy only from selected area)
    val sourceMinX = rect.x
    var sourceMaxX = rect.x + rect.width - 1
    val sourceMinY = rect.y
    var sourceMaxY = rect.y + rect.height - 1
    if (sourceMaxX > 31) sourceMaxX = 31
    if (sourceMaxY > 31) sourceMaxY = 31
    val sourceWidth = sourceMaxX - sourceMinX + 1
    val mask = (0.inv() shl (32 - sourceWidth)) ushr sourceMinX

    // copy bits
    for (sourceY in sourceMinY..sourceMaxY) {
        val targetY = sourceY + deltaY
        if (targetY < 0 || targetY > 31) {
            continue
        }
        if (deltaX >= 0) {
            target[targetY] = target[targetY] or ((source[sourceY] and mask) ushr deltaX)
        } else {
            target[targetY] = target[targetY] or ((source[sourceY] and mask) shl -1 * deltaX)
        }
    }
}

/**
 * Stretches matrix by copying bits in the middle
 */
fun stretchBits(
    source: IntArray?, target: IntArray?, rect: KotomoRectangle,
    horizontalAmount: Int, verticalAmount: Int
): KotomoRectangle {
    return if (horizontalAmount != 0 && verticalAmount != 0) {
        throw Error("Not implemented")
        // TODO combined stretch
    } else if (horizontalAmount != 0) {
        stretchBitsX(source, target, rect, horizontalAmount)
    } else {
        stretchBitsY(source, target, rect, verticalAmount)
    }
}

/**
 * Stretches matrix by copying bits in the middle to X direction
 *
 * @param amount how many pixels source is stretched
 * @return new bounds
 */
fun stretchBitsX(source: IntArray?, target: IntArray?, rect: KotomoRectangle, amount: Int): KotomoRectangle {
    if (amount <= 0) {
        throw Error("amount must be positive")
        // TODO shrink
    }
    val dividerX = rect.x + rect.width / 2
    val rightBlock = KotomoRectangle(
        dividerX, rect.y,
        rect.x + rect.width - 1 - dividerX + 1, rect.height
    )
    val leftBlock = KotomoRectangle(rect.x, rect.y, dividerX - rect.x + 1, rect.height)
    val divider = KotomoRectangle(dividerX, rect.y, 1, rect.height)
    val moveRight = (amount + 1) / 2
    val moveLeft = amount - moveRight

    // move block
    copyBits(source!!, target!!, rightBlock, moveRight, 0, false)
    copyBits(source, target, leftBlock, -moveLeft, 0, false)

    // fill gap
    for (deltaX in -moveLeft + 1 until moveRight) {
        copyBits(source, target, divider, deltaX, 0, false)
    }

    // check bounds
    val unbounded = KotomoRectangle(rect.x - moveLeft, rect.y, rect.width + amount, rect.height)
    val limits = KotomoRectangle(0, 0, 32, 32)
    return unbounded.intersection(limits)
}

/**
 * Stretches matrix by copying bits in the middle to Y direction
 *
 * @param amount how many pixels source is stretched
 * @return new bounds
 */
fun stretchBitsY(source: IntArray?, target: IntArray?, rect: KotomoRectangle, amount: Int): KotomoRectangle {
    if (amount <= 0) {
        throw Error("amount must be positive")
        // TODO shrink
    }
    val dividerY = rect.y + rect.height / 2
    val bottomBlock = KotomoRectangle(
        rect.x, dividerY,
        rect.width, rect.y + rect.height - 1 - dividerY + 1
    )
    val upBlock = KotomoRectangle(rect.x, rect.y, rect.width, dividerY - rect.y + 1)
    val divider = KotomoRectangle(rect.x, dividerY, rect.width, 1)
    val moveDown = (amount + 1) / 2
    val moveUp = amount - moveDown

    // move block
    copyBits(source!!, target!!, bottomBlock, 0, moveDown, false)
    copyBits(source, target, upBlock, 0, -moveUp, false)

    // fill gap
    for (deltaY in -moveUp + 1 until moveDown) {
        copyBits(source, target, divider, 0, deltaY, false)
    }

    // check bounds
    val unbounded = KotomoRectangle(rect.x, rect.y - moveUp, rect.width, rect.height + amount)
    val limits = KotomoRectangle(0, 0, 32, 32)
    return unbounded.intersection(limits)
}


/**
 * Adds all source bits into target. 0 in source doesn't overwrite target.
 */
fun addBits(source: IntArray, target: IntArray) {
    for (y in 0..31) {
        target[y] = target[y] or source[y]
    }
}

/**
 * Adds all source bits into target within bounds. 0 in source doesn't overwrite target.
 */
fun addBits(source: IntArray, target: IntArray, bounds: KotomoRectangle) {
    val mask = (0.inv() shl (32 - bounds.width)) ushr bounds.x
    for (y in bounds.y until bounds.y + bounds.height) {
        target[y] = target[y] or (source[y] and mask)
    }
}

/**
 * Finds bounds around set bits
 *
 * @return null if no bits found
 */
fun findBounds(matrix: IntArray?): KotomoRectangle? {
    if (countBits(matrix!!) == 0) {
        return null
    }
    var left = 0
    left@ for (x in 0..31) {
        for (y in 0..31) {
            if (isBitSet(x, y, matrix)) {
                break@left
            }
        }
        ++left
    }
    var right = 31
    right@ for (x in 31 downTo 0) {
        for (y in 0..31) {
            if (isBitSet(x, y, matrix)) {
                break@right
            }
        }
        --right
    }
    var up = 0
    up@ for (y in 0..31) {
        for (x in 0..31) {
            if (isBitSet(x, y, matrix)) {
                break@up
            }
        }
        ++up
    }
    var down = 31
    down@ for (y in 31 downTo 0) {
        for (x in 0..31) {
            if (isBitSet(x, y, matrix)) {
                break@down
            }
        }
        --down
    }
    val width = right - left + 1
    val height = down - up + 1
    return KotomoRectangle(left, up, width, height)
}
