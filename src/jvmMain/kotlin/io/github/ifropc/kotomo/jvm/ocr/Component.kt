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
package io.github.ifropc.kotomo.jvm.ocr

import io.github.ifropc.kotomo.ocr.KotomoRectangle
import kotlinx.serialization.Serializable


/**
 * Reference character component. Roughly equilevant to radical but automatically generated
 * and might be different from official radicals. Pixel groups that don't touch each other
 * form different components but might be divided further if large.
 *
 * Components are used in third OCR stage to fine-tune results.
 */
@Serializable
class Component {
    /**
     * Location of the component inside reference character
     */
    var bounds: KotomoRectangle? = null

    /**
     * Component's pixels. Matrix might also contain pixels that belong to other components
     * outside bounds.
     */
    lateinit var matrix: IntArray

    /**
     * Number of pixels in this component
     */
    var pixels = 0
}
