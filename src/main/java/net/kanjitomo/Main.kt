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

package net.kanjitomo

import java.awt.Point
import java.io.File
import javax.imageio.ImageIO

object Main {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val tomo = KanjiTomo()
        tomo.loadData()
        val image = ImageIO.read(File(args[0]))
        tomo.setTargetImage(image)
        val results = tomo.runOCR(Point(args[1].toInt(), args[2].toInt()))
        println(results)
    }
}
