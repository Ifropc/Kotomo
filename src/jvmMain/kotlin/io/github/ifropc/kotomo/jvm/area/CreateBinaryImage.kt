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
package io.github.ifropc.kotomo.jvm.area

import io.github.ifropc.kotomo.config.FixedParameters
import io.github.ifropc.kotomo.jvm.util.ImageUtil.createImageFromMatrix
import io.github.ifropc.kotomo.jvm.util.ImageUtil.createMatrixFromImage
import io.github.ifropc.kotomo.jvm.util.ImageUtil.makeBlackAndWhite
import io.github.ifropc.kotomo.jvm.util.JVMUtil.toBufferedImage
import io.github.ifropc.kotomo.jvm.util.JVMUtil.withDebuggable
import io.github.ifropc.kotomo.config.Parameters
import java.awt.image.BufferedImage

/**
 * Creates a binary (black and white) image from grayscale image.
 */
class CreateBinaryImage constructor(task: AreaTask?) : AreaStep(task, "binary") {
    
    override fun runImpl() {

        // TODO instead of static blackThreshold calculate a histogram?
        val bwImage: BufferedImage = makeBlackAndWhite(
            (task!!.sharpenedImage?.toBufferedImage())!!,
            if (FixedParameters.fixedBlackLevelEnabled) null else Parameters.pixelRGBThreshold
        )
        task!!.binaryImage = createMatrixFromImage(bwImage)
    }

    
    override fun addDebugImages() {
        val image: BufferedImage = createImageFromMatrix(task!!.binaryImage)
        withDebuggable(task!!) { it.addDebugImage(image, "binary") }
    }
}
