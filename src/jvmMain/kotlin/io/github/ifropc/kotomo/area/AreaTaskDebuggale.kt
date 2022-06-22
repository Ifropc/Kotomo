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
package io.github.ifropc.kotomo.area

import io.github.ifropc.kotomo.CharacterColor
import io.github.ifropc.kotomo.ocr.KotomoImage
import io.github.ifropc.kotomo.ocr.Point
import io.github.ifropc.kotomo.ocr.KotomoRectangle
import io.github.ifropc.kotomo.util.DebugImage
import io.github.ifropc.kotomo.util.FixedParameters
import io.github.ifropc.kotomo.util.ImageUtil.buildScaledImage
import io.github.ifropc.kotomo.util.ImageUtil.createGrayImage
import io.github.ifropc.kotomo.util.ImageUtil.createImageFromMatrix
import io.github.ifropc.kotomo.util.ImageUtil.crop
import io.github.ifropc.kotomo.util.ImageUtil.paintAreas
import io.github.ifropc.kotomo.util.ImageUtil.paintColumn
import io.github.ifropc.kotomo.util.ImageUtil.setClipboard
import io.github.ifropc.kotomo.util.Parameters
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger { }

// TODO: think of a better way of using DEBUG images. Should not be ported to common library
class AreaTaskDebuggable(targetImage: KotomoImage): AreaTask(targetImage) {
    /**
     * List of images used during development to visualize processing steps.
     * Only generated if debugMode = true;
     */
    var debugImages: MutableList<DebugImage?> = ArrayList()

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     */
    fun addDebugImage(image: Array<BooleanArray>, step: String?) {
        val bufferedImage = createImageFromMatrix(image)
        addDebugImage(bufferedImage, step)
    }

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     */
    fun addDebugImage(image: BufferedImage?, step: String?) {
        addDebugImage(DebugImage(image!!, step!!, null, Parameters.debugFilePrefix))
    }

    /**
     * Adds image to list of debug images
     *
     * @param image Image to be added to list of debug images
     *
     * @param step Short one-word description of the image. For example: "binary"
     * This appears in file name and can be referenced in Parameters.filterDebugImages
     *
     * @param vertical If set, orientation is diplayed in the file name
     */
    fun addDebugImage(image: BufferedImage?, step: String?, vertical: Boolean?) {
        addDebugImage(DebugImage(image!!, step!!, vertical, Parameters.debugFilePrefix))
    }

    /**
     * Adds image to list of debug images
     */
    private fun addDebugImage(image: DebugImage) {
        if (debugImages.size < Parameters.maxDebugImages) {
            debugImages.add(image)
        } else {
            log.error { ("maxDebugImages reached") }
        }
    }

    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */

    fun addDefaultDebugImage(name: String?, vertical: Boolean?) {
        addDefaultDebugImage(name, areas, columns, vertical)
    }
    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */
    /**
     * Paints and adds a default debug image displaying columns and areas
     * and gray background.
     *
     * @param vertical if set, orientation is displayed in debug file name
     */

    fun addDefaultDebugImage(
        name: String?, areas: List<Area>? = this.areas, columns: List<Column?>? = this.columns,
        vertical: Boolean? = null
    ) {
        val image = createDefaultDebugImage(areas, columns)
        addDebugImage(image, name, vertical)
    }
    /**
     * Paints a default debug image displaying columns and areas and gray background.
     */
    /**
     * Paints a default debug image displaying columns and areas and gray background.
     */

    fun createDefaultDebugImage(
        areas: List<Area>? = this.areas,
        columns: List<Column?>? = this.columns
    ): BufferedImage {
        var image = createGrayImage(binaryImage, backgroundImage)
        image = paintAreas(image, areas!!)
        if (columns != null) {
            for (col in columns) {
                paintColumn(image, col!!)
            }
        }
        return image
    }

    /**
     * Writes debug images to target directory. filenameBase is included in each file name.
     */

    fun writeDebugImages() {
        if (!Parameters.isSaveAreaFailed) {
            return
        }
        log.info { "Writing area debug images" }
        for (image in debugImages) {
            writeDebugImage(image)
        }
    }

    /**
     * Writes debug image to disk in Parameters.debugDir directory. Files are
     * named as "test number.index number.algorithm step.png"
     */

    private fun writeDebugImage(image: DebugImage?) {
        if (image == null) {
            return
        }
        val targetDir = Parameters.debugDir
        val filename = image.filename
        if (Parameters.debugImages != null && Parameters.debugImages.size > 0) {
            for (debugImage in Parameters.debugImages) {
                if (filename.contains(debugImage)) {
                    break
                }
            }
            return
        }
        val file = File("$targetDir/$filename")
        var scale = 1
        val minDim = max(image.image.width, image.image.height)
        if (minDim < Parameters.smallDebugAreaImageThreshold) {
            scale = Parameters.smallDebugAreaImageScale
        }
        val scaledImage = buildScaledImage(image.image, scale)
        if (!Parameters.debugAreaImageToClipboard) {
            ImageIO.write(scaledImage, "png", file)
        } else {
            setClipboard(scaledImage)
        }
    }
}
