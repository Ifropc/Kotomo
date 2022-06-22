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

import io.github.ifropc.kotomo.ocr.KotomoColor
import io.github.ifropc.kotomo.ocr.KotomoImage
import io.github.ifropc.kotomo.ocr.KotomoImageImpl
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import java.awt.Color
import java.awt.image.BufferedImage

internal object JVMUtil {
    fun KotomoRectangle.toAwt(): java.awt.Rectangle{
        return java.awt.Rectangle(this.x, this.y, this.width, this.height)
    }

    fun KotomoColor.toAwt(): Color {
        return Color(this.red, this.green, this.blue, this.alpha)
    }

    fun Color.toKotomo(): KotomoColor {
        return KotomoColor(this.red, this.green, this.blue, this.alpha)
    }

    fun KotomoImage.toBufferedImage(): BufferedImage {
        return (this as KotomoImageImpl).bufferedImage
    }

    fun BufferedImage.toKotomoImage(): KotomoImage {
        return KotomoImageImpl(this)
    }
}