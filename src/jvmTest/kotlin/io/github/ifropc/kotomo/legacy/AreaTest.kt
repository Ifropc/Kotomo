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

import io.github.ifropc.kotomo.ocr.entities.KotomoRectangle


/**
 * Tests that represents correct area or column in the target image.
 */
class AreaTest : Test {
    val rect: KotomoRectangle

    /**
     * @param rect Expected area on target image
     */
    constructor(rect: KotomoRectangle) {
        this.rect = rect
    }

    constructor(x: Int, y: Int, width: Int, height: Int) {
        rect = KotomoRectangle(x, y, width, height)
    }

    override fun toString(): String {
        return image!!.name + " area:" + rect
    }
}
