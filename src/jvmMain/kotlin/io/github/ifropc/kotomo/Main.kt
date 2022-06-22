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

@file:Suppress("unused")

package io.github.ifropc.kotomo

import io.github.ifropc.kotomo.ocr.ImageLoader
import io.github.ifropc.kotomo.ocr.Point
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val tomo = KanjiTomo()
    tomo.loadData()
    val image = runBlocking { ImageLoader.loadFromFile(args[0]) }
    tomo.setTargetImage(image)
    val results = runBlocking { tomo.runOCR(Point(args[1].toInt(), args[2].toInt())) }
    log.info { results }
}
