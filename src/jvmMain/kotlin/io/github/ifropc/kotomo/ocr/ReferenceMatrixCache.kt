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

/**
 * Cache of reference matrices for each character listed in Character class
 */
class ReferenceMatrixCache {
    /**
     * Font name -> list of reference matrices for each character
     */
    private val cache: MutableMap<String, List<ReferenceMatrix>>

    init {
        cache = HashMap()
        for (font in Parameters.referenceFonts) {
            cache[font] = ArrayList()
        }
    }

    /**
     * Adds matrix list to the cache
     */
    fun put(font: String, list: List<ReferenceMatrix>) {
        cache[font] = list
    }

    /**
     * Gets reference matrices for given font
     */
    
    operator fun get(font: String): List<ReferenceMatrix> {
        if (!cache.containsKey(font)) {
            throw Exception("Cache doesn't contain font:$font, regenerate cache")
        }
        return cache[font]!!
    }

    /**
     * Gets reference matrices for all fonts
     */
    val all: List<ReferenceMatrix>
        get() {
            val matrices: MutableList<ReferenceMatrix> = ArrayList()
            for (font in cache.keys) {
                matrices.addAll(cache[font]!!)
            }
            return matrices
        }

}
