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

import io.github.ifropc.kotomo.jvm.util.toKotomoImage
import io.github.ifropc.kotomo.ocr.entities.KotomoImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO

actual object ImageLoader {
    /**
     * Loads image from file
     */
    actual suspend fun loadFromFile(path: String): KotomoImage {
        return withContext(Dispatchers.IO) {
            ImageIO.read(this::class.java.classLoader.getResourceAsStream(path))
        }.toKotomoImage()
    }

    // TODO: better API for suspend functions
    /**
     * Loads image from file (sync)
     */
    fun loadFromFileSync(path: String): KotomoImage {
        return runBlocking { loadFromFile(path) }
    }
}
