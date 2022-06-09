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
 * Parameters used to modify bitmap before aligment.
 */
@Serializable
class Transformation  {
    // positive translate moves image right or down by one pixel

    val horizontalTranslate // TODO rename to deltaX?
            : Int

    val verticalTranslate: Int

    // positive stretch makes image larger by one pixel

    val horizontalStretch: Int

    val verticalStretch: Int

    /**
     * Creates default no-op transformation (all parameters zero).
     */
    constructor() {
        horizontalTranslate = 0
        verticalTranslate = 0
        horizontalStretch = 0
        verticalStretch = 0
    }

    constructor(
        horizontalTranslate: Int, verticalTranslate: Int,
        horizontalStretch: Int, verticalStretch: Int
    ) {
        this.horizontalTranslate = horizontalTranslate
        this.verticalTranslate = verticalTranslate
        this.horizontalStretch = horizontalStretch
        this.verticalStretch = verticalStretch
    }

    /**
     * @return true if this transformation contains argument parameters
     */
    fun contains(
        horizontalTranslate: Int, verticalTranslate: Int,
        horizontalStretch: Int, verticalStretch: Int
    ): Boolean {
        return if (this.horizontalTranslate == horizontalTranslate && this.verticalTranslate == verticalTranslate && this.horizontalStretch == horizontalStretch && this.verticalStretch == verticalStretch
        ) {
            true
        } else {
            false
        }
    }

    override fun equals(obj: Any?): Boolean {
        val p = obj as Transformation?
        return p!!.horizontalTranslate == horizontalTranslate && p.verticalTranslate == verticalTranslate && p.horizontalStretch == horizontalStretch && p.verticalStretch == verticalStretch
    }

    override fun hashCode(): Int {
        return horizontalTranslate + verticalTranslate +
                horizontalStretch + verticalStretch
    }

    override fun toString(): String {
        return horizontalTranslate.toString() + "." + verticalTranslate + "." +
                horizontalStretch + "." + verticalStretch
    }
}
