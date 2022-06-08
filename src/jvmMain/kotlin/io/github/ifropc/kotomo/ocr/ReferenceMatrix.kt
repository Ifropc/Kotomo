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

import kotlinx.serialization.Serializable

/**
 * Matrix that represents a preloaded reference character.
 * All common Japanese characters have their own matrix.
 */
@Serializable
class ReferenceMatrix {
    /** Character represented by this matrix  */
    @JvmField
    var character = 0.toChar()

    /** Pixels of the reference image. 32x32 bitmap  */

    lateinit var matrix: IntArray

    /** Number of pixels (set bits) in the matrix  */
    @JvmField
    var pixels = 0

    /**
     * Pixels around the target image (32x32 bitmaps). First matrix (index 0) represents pixels
     * that are off by one compared to reference image (neighbour pixels), further
     * levels increase the distance. Last level includes all the remaining pixels.
     * Number of halo levels is determined by Parameters.ocrHaloSize.
     */
    @JvmField
    var halo: ArrayList<IntArray>? = null

    /**
     * Some characters are given a OCR score boost based on rarity
     */
    @JvmField
    var scoreModifier = 1f

    /**
     * Font name used to generate the matrix
     */
    @JvmField
    var fontName: String? = null

    /**
     * List of individual character components (radicals)
     */
    @JvmField
    var components: ArrayList<Component>? = null

    /**
     * Transformations applied to individual components. Same order as components list.
     */
    @JvmField
    var transformations: ArrayList<Transformation>? = null

    /**
     * Creates a copy of this object
     */
     fun clone(): ReferenceMatrix {
        val newReference = ReferenceMatrix()
        newReference.character = character
        newReference.components = components
        newReference.fontName = fontName
        newReference.halo = halo
        newReference.matrix = matrix
        newReference.pixels = pixels
        newReference.scoreModifier = scoreModifier

        // transformations will change so create a new list instead of referencing old
        if (transformations != null) {
            newReference.transformations = ArrayList()
            newReference.transformations!!.addAll(transformations!!)
        }
        return newReference
    }
}
