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

import com.esotericsoftware.kryo.io.Input
import io.github.ifropc.kotomo.ocr.Characters.getScoreModifier
import io.github.ifropc.kotomo.util.KryoFactory.kryo
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Loads the cache of reference matrices
 */
class ReferenceMatrixCacheLoader {
    private val par = instance

    /**
     * Loads the cache from serialized data file (unless already done)
     */
    @Throws(Exception::class)
    fun load() {
        if (Companion.cache != null) {
            return
        }
        val started = System.currentTimeMillis()
        deserialize()
        applyScoreModifiers()
        val done = System.currentTimeMillis()
        if (par.isPrintDebug) {
            println("ReferenceMatrixCache " + (done - started) + " ms")
        }
    }

    val cache: ReferenceMatrixCache?
        get() = Companion.cache

    /**
     * Gets reference matrices for given font
     */
    @Throws(Exception::class)
    fun getReferences(font: String): List<ReferenceMatrix?>? {
        return Companion.cache!![font]
    }

    /**
     * Reads serialized matrices from disk using Kryo library
     *
     * https://github.com/EsotericSoftware/kryo
     * https://www.baeldung.com/kryo
     */
    @Throws(Exception::class)
    private fun deserialize() {
        Companion.cache = ReferenceMatrixCache()
        for (font in par.referenceFonts) {
            val fileName = ReferenceMatrixHashCalculator.getReferenceFileName(
                font, Parameters.targetSize,
                Parameters.ocrHaloSize, Characters.all
            )
            if (deserializeJar(font, fileName)) {
                continue
            }
            val file = ReferenceMatrixHashCalculator.getReferenceFile(
                font, Parameters.targetSize,
                Parameters.ocrHaloSize, Characters.all
            )
            if (deserializeFile(font, file)) {
                continue
            }
            throw Exception(file!!.name + " not found, rebuild cache")
        }
    }

    @Throws(Exception::class)
    private fun deserializeJar(font: String, resourceName: String?): Boolean {
        val `in` = javaClass.getResourceAsStream("/$resourceName") ?: return false
        if (par.isPrintOutput) {
            println("Deserializing references:$resourceName from jar")
        }
        deserializeStream(font, `in`)
        return true
    }

    @Throws(Exception::class)
    private fun deserializeFile(font: String, file: File?): Boolean {
        if (!file!!.exists()) {
            return false
        }
        if (par.isPrintOutput) {
            println("Deserializing references from file:$file")
        }
        deserializeStream(font, FileInputStream(file))
        return true
    }

    private fun deserializeStream(font: String, `in`: InputStream) {
        val kryo = kryo
        val input = Input(`in`)
        val list = kryo.readClassAndObject(input) as ArrayList<ReferenceMatrix>
        Companion.cache!!.put(font, list)
        input.close()
    }

    private fun applyScoreModifiers() {
        for (matrix in Companion.cache!!.all) {
            matrix!!.scoreModifier = getScoreModifier(matrix!!.character)
        }
    }

    companion object {
        private var cache: ReferenceMatrixCache? = null
    }
}
