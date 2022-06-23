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

package io.github.ifropc.kotomo.jvm.util

import io.github.ifropc.kotomo.jvm.area.AreaTask
import io.github.ifropc.kotomo.jvm.area.AreaTaskDebuggable
import io.github.ifropc.kotomo.ocr.entities.KotomoColor
import io.github.ifropc.kotomo.ocr.entities.KotomoImage
import io.github.ifropc.kotomo.jvm.ocr.KotomoImageImpl
import io.github.ifropc.kotomo.ocr.entities.KotomoRectangle
import mu.KotlinLogging
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File

private val log = KotlinLogging.logger { }

internal object JVMUtil {
    /**
     * Finds file reference
     */
    fun findFile(fileName: String): File {

        // this is needed because location can be relative to class or jar file,
        // depending on if the program is launched directly from Eclipse or packaged first
        var file: File
        val projectDir = File("")
        file = File(projectDir.absolutePath + "/" + fileName)
        if (file.exists()) {
            return file
        }
        file = File(projectDir.absolutePath + "/../" + fileName)
        return if (file.exists()) {
            file
        } else {
            throw Exception("File not found:$fileName")
        }
    }

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

    inline fun withDebuggable(t: AreaTask, block: (AreaTaskDebuggable) -> Unit) {
        when (t) {
            is AreaTaskDebuggable -> block(t)
            else -> log.warn(Exception()) { "WithDebuggable was called on non-debuggable image" }
        }
    }

    inline fun <T> runWithDebuggable(t: AreaTask, block: (AreaTaskDebuggable) -> T): T{
        return block(t as AreaTaskDebuggable)
    }
}
