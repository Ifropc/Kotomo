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

import java.awt.Point

class OCRTest : Test {
    var point: Point
    var characters: String

    /**
     * @param point Target image coordinates
     * @param characters Expected character
     */
    constructor(point: Point, characters: String) {
        this.point = point
        this.characters = characters
    }

    /**
     * @param x target image coordinates
     * @param y
     * @param c expected character
     */
    constructor(x: Int, y: Int, characters: String) {
        point = Point(x, y)
        this.characters = characters
    }

    override fun toString(): String {
        return image!!.name + " ocr:" + point + ":" + characters
    }
}
