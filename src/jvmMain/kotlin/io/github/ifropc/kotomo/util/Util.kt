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

import io.github.ifropc.kotomo.ocr.Rectangle
import mu.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger { }

object Util {
    /**
     * Finds file reference
     */

	
    fun findFile(fileName: String): File {

        // this is needed because location can be relative to class or jar file,
        // depending on if the program is launched directly from Eclipse or packaged first
        var file: File
        val projectDir = File("")
        file = File(projectDir.absolutePath + "/" + fileName)
        if (file.exists()) {
            return file
        }
        file = File(projectDir.absolutePath + "/../" + fileName)
        return if (file.exists()) {
            file
        } else {
            throw Exception("File not found:$fileName")
        }
    }

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

    /**
     * Prints array as: [a,b,c,..]
     */

	fun printArray(array: IntArray): String {
        var ret = "["
        for (i in array.indices) {
            ret += array[i]
            if (i < array.size - 1) {
                ret += ","
            }
        }
        ret += "]"
        return ret
    }

    /**
     * Creates boolean matrix from bitmap matrix
     */
    fun createBinaryMatrix32(matrix: IntArray): Array<BooleanArray> {
        if (matrix.size != 32) {
            Error("Invalid length")
        }
        val boolMatrix = Array(32) { BooleanArray(32) }
        for (y in 0..31) {
            for (x in 0..31) {
                if (isBitSet(x, y, matrix)) {
                    boolMatrix[x][y] = true
                }
            }
        }
        return boolMatrix
    }

    /**
     * Prints java memory statistics
     */
    fun printMemoryUsage() {
        System.gc()
        val heapSize = Runtime.getRuntime().totalMemory()
        val heapMaxSize = Runtime.getRuntime().maxMemory()
        val heapFreeSize = Runtime.getRuntime().freeMemory()
        val heapUsedSize = heapSize - heapFreeSize
        log.debug { "\nMemory usage:" } 
        log.debug { "heapSize    :$heapSize" } 
        log.debug { "heapMaxSize :$heapMaxSize" } 
        log.debug { "heapFreeSize:$heapFreeSize" } 
        log.debug { "heapUsedSize:$heapUsedSize" } 
    }
}

fun Rectangle.toAwt(): java.awt.Rectangle{
    return java.awt.Rectangle(this.x, this.y, this.width, this.height)
}

fun java.awt.Rectangle.toKotomo(): Rectangle {
   return Rectangle(this.x, this.y, this.width, this.height)
}
