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
package io.github.ifropc.kotomo.jvm.util

import java.io.File

internal object Util {
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
}
