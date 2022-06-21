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

package io.github.ifropc.kotomo

import io.github.ifropc.kotomo.ocr.ImageLoader
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageLoaderTest {
    @Test
    fun loadFromFile() = runTest {
        var path = "TestImage1.jpg"
        if (isJs()) {
            path = "kotlin/TestImage1.jpg"
        }
        val img = ImageLoader.loadFromFile(path)

        assertEquals(254, img.getRGB(1, 0).red)
        assertEquals(255, img.getRGB(0, 0).green)
        assertEquals(254, img.getRGB(2, 0).blue)
        assertEquals(-3421237, img.getRGB(10, 10).toInt())
    }
}
