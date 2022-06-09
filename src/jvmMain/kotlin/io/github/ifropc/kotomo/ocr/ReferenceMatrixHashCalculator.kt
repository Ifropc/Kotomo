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

import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import java.io.File
import java.util.*

/**
 * Calculates reference matrix hash values and cache file names.
 */
object ReferenceMatrixHashCalculator {
    /**
     * Gets filename that represents set of reference matrices
     */
    
    fun getReferenceFileName(
        font: String, targetSize: Int, ocrHaloSize: Int,
        characters: String?
    ): String {
        val hashCode = calcHashCode(
            font, Parameters.targetSize,
            Parameters.ocrHaloSize, Characters.all
        )
        return "CHARACTERS_" + Integer.toHexString(hashCode).uppercase(Locale.getDefault()) + ".cache"
    }

    /**
     * Gets file that represents set of reference matrices
     */
    
    fun getReferenceFile(
        font: String, targetSize: Int, ocrHaloSize: Int,
        characters: String?
    ): File {
        return File(
            instance.cacheDir.toString() + "/" +
                    getReferenceFileName(font, targetSize, ocrHaloSize, characters)
        )
    }

    private fun calcHashCode(
        font: String, targetSize: Int, ocrHaloSize: Int,
        characters: String
    ): Int {
        var hashCode = smear(font.hashCode())
        hashCode += smear(targetSize * 1000)
        hashCode += smear(ocrHaloSize * 1000000)
        for (c in characters.toCharArray()) {
            hashCode += smear(c.code)
        }
        return hashCode
    }

    /*
	 * This method was written by Doug Lea with assistance from members of JCP
	 * JSR-166 Expert Group and released to the public domain, as explained at
	 * http://creativecommons.org/licenses/publicdomain
	 * 
	 * As of 2010/06/11, this method is identical to the (package private) hash
	 * method in OpenJDK 7's java.util.HashMap class.
	 */
    private fun smear(hashCode: Int): Int {

        // https://stackoverflow.com/questions/9624963/java-simplest-integer-hash
        var hashCode = hashCode
        hashCode = hashCode xor ((hashCode ushr 20) xor (hashCode ushr 12))
        return hashCode xor (hashCode ushr 7) xor (hashCode ushr 4)
    }
}
