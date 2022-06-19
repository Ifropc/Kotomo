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

import io.github.ifropc.kotomo.util.addBits
import io.github.ifropc.kotomo.util.countBits

/**
 * Builds reference components from ReferenceMatrix.
 */
class ComponentBuilder {
    private val unconnected = ComponentFindUnconnected()
    private val split = ComponentSplit()

    /**
     * Extracts components from reference matrix
     */
    fun buildComponents(reference: ReferenceMatrix?): ArrayList<Component> {
        val base = Component()
        base.bounds = Rectangle(0, 0, 32, 32)
        base.matrix = reference!!.matrix
        val components = findSubComponents(base)
        compressComponents(components, reference.matrix)
        return components
    }

    private fun findSubComponents(parent: Component): ArrayList<Component> {

        // find unconnected components
        val components = unconnected.run(parent)

        // split large components into smaller sub-components
        val componentsSplitted = ArrayList<Component>()
        for (component in components!!) {
            componentsSplitted.addAll(split.run(component))
        }
        return componentsSplitted
    }

    /**
     * Compresses component matrices into fewer matrices. This is done by assigning
     * multiple components into singe matrix if their bounding boxes don't overlap.
     *
     * @param baseLayer Original reference matrix that contains the whole character.
     */
    private fun compressComponents(components: List<Component?>, baseLayer: IntArray) {
        val layers: MutableList<IntArray> = ArrayList()

        // add components to layers
        components@ for (component in components) {

            // check if base layer can be used
            if (addToBaseLayer(component, baseLayer)) {
                continue
            }

            // check if any existing layer can be used
            for (i in layers.indices) {
                if (addToLayer(component, layers[i])) {
                    continue@components
                }
            }

            // create a new layer
            val newLayer = IntArray(32)
            layers.add(newLayer)
            addToLayer(component, newLayer)
        }
    }

    /**
     * Tries to add component into base layer. Base layer contains pixels from all
     * components (original reference matrix). Only use it for components that don't
     * contain any other pixels within their bounding boxes.
     */
    private fun addToBaseLayer(component: Component?, baseLayer: IntArray): Boolean {

        // check if there are any pixels that belong to other components
        if (countBits(baseLayer, component!!.bounds!!) > component.pixels) {
            return false
        }

        // use the base layer
        component.matrix = baseLayer
        return true
    }

    /**
     * Tries to add component into argument layer. Checks that the target
     * layer has free space (bounding box doesn't contain pixels from other components)
     *
     * @return true if component was added, false if not
     */
    private fun addToLayer(component: Component?, layer: IntArray): Boolean {

        // check if the layer contains pixels from other components within bounds
        if (countBits(layer, component!!.bounds!!) > 0) {
            return false
        }

        // add to this layer
        addBits(component.matrix, layer, component.bounds!!)
        component.matrix = layer
        return true
    }
}
