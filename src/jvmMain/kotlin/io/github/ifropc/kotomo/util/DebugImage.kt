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

import java.awt.image.BufferedImage

/**
 * Debug images are used during development to visualize processing steps
 */
class DebugImage(
    val image: BufferedImage,
    /**
     * Short one-word description of the processing step. For example: "binary"
     */
    val step: String,
    /**
     * True if this image has vertical columns, false if horizontal,
     * null if both or no columns.
     */
    val vertical: Boolean?, prefix: String
) {

    val filename: String

    /**
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     *
     * @param vertical If set, orientation is diplayed in the file name
     *
     * @param prefix String added in front of debug file names. Contains test reference
     * and image sequence number
     */
    init {
        var verticalStr = ""
        if (vertical != null) {
            verticalStr = if (vertical) ".vertical" else ".horizontal"
        }
        filename = "$prefix.$step$verticalStr.png"
    }
}
