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

/**
 * Matrix that represents pixels in target image around single character, possibly after
 * transformed by streching.
 */
class TargetMatrix {
    /** Pixels of the target image. 32x32 bitmap  */
    lateinit var matrix: IntArray

    /** Number of pixels (set bits) in the matrix  */

    var pixels = 0

    /**
     * Pixels around the target image (32x32 bitmaps). First matrix (index 0) represents pixels
     * that are off by one compared to reference image (neighbour pixels), further
     * levels increase the distance. Last level includes all the remaining pixels.
     * Number of halo levels is determined by Parameters.ocrHaloSize.
     */

    var halo: List<IntArray>? = null

    /** Character index in source image (0 = character closest to mouse cursor)  */

    var charIndex: Int? = null

    /** Transformation used to modify the bitmap from original image  */

    var transform: Transformation? = null
}
