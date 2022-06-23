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

package io.github.ifropc.kotomo.legacy

import io.github.ifropc.kotomo.ocr.matrix.Component
import io.github.ifropc.kotomo.ocr.component.ComponentBuilder
import io.github.ifropc.kotomo.ocr.matrix.ReferenceMatrix
import io.github.ifropc.kotomo.jvm.ocr.ReferenceMatrixCacheLoader
import io.github.ifropc.kotomo.util.addBits
import io.github.ifropc.kotomo.util.debugPrintMatrix
import kotlin.test.Test

class ComponentBuilderTest {
    @Test
    fun test() {
        val testCharacter = 'り' // り人化新港無

        // select reference matrix
        val loader = ReferenceMatrixCacheLoader
        loader.load()
        val cache = loader.cache
        var reference: ReferenceMatrix? = null
        for (matrix in cache!!["MS Gothic"]) {
            if (matrix.character == testCharacter) {
                reference = matrix
            }
        }
        println("Reference")
        debugPrintMatrix(reference!!.matrix)

        // build components
        val builder = ComponentBuilder()
        val components: List<Component?> = builder.buildComponents(reference)

        // print components
        println(components.size.toString() + " components")
        for (component in components) {
            println(component!!.bounds.toString() + " pixels:" + component.pixels)
            val pixels = IntArray(32)
            addBits(component.matrix, pixels, component.bounds!!)
            debugPrintMatrix(pixels, component.matrix)
        }
    }
}
