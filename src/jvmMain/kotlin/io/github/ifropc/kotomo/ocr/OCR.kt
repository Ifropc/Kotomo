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

import io.github.ifropc.kotomo.util.ImageUtil.buildImage
import io.github.ifropc.kotomo.util.ImageUtil.buildScaledImage
import io.github.ifropc.kotomo.util.ImageUtil.colorizeImage
import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import java.io.File
import javax.imageio.ImageIO

/**
 * Runs all OCR algorithm stages. Each stage is more accurate but slower than last, only a
 * subset of best results become input of next stage.
 */
class OCR {
    private val par = instance
    
    fun run(task: OCRTask?) {
        val started = System.currentTimeMillis()

        // holds target image transformations (target is stretched and compressed to different
        // sizes). these are shared between first two stages and should not be re-generated 
        val transform = Transform(task!!)

        // stage 1, find common pixels, consider basic transformations
        val stage1 = OCRAlignCharacters(task, transform)
        task.results = stage1.run(null, true, 1, 1, 1, par.ocrKeepResultsStage1)
        if (debugStages) {
            debug(task, System.currentTimeMillis() - started, 1)
        }

        // stage 2, find common pixels, consider more transformations per character
        val stage2 = OCRAlignCharacters(task, transform)
        task.results = stage2.run(getCharacters(task.results), true, 2, 2, 4, par.ocrkeepResultsStage2)
        if (debugStages) {
            debug(task, System.currentTimeMillis() - started, 2)
        }

        // stage 3, align individual components
        val alignComponents = OCRAlignComponents()
        task.results = alignComponents.run(task.results)
        val time = System.currentTimeMillis() - started
        totalOCRTime += time
        ++totalOCRCount
        debug(task, time, 3)
    }

    private fun getCharacters(results: List<OCRResult>?): Set<Char?> {
        val set: MutableSet<Char?> = HashSet()
        for (result in results!!) {
            set.add(result.character)
        }
        return set
    }

    /**
     * Prints debug information and saves debug images if needed
     */
    
    private fun debug(task: OCRTask?, time: Long, stage: Int) {
        if (par.isPrintDebug) {
            println("OCR total $time ms")
            if (task!!.results!!.size == 0) {
                System.err.println("No results")
                return
            }
            println("\nOCR results stage $stage")
            for (result in task.results!!) {
                println(result)
            }
        }
        if (par.isSaveOCRFailed) {
            val bestMatch = task!!.results!![0].character
            var expectedCharacter: Char? = null
            if (par.expectedCharacters != null) {
                expectedCharacter = par.expectedCharacters!![task.charIndex!!]
            }
            if (par.isSaveOCRAll || bestMatch != expectedCharacter) {
                writeDebugImages(task, stage)
            }
        }
    }

    /**
     * If true, target image is written to file
     */
    private val writeTarget = true

    /**
     * If true, reference image is written to file
     */
    private val writeReference = true
    
    private fun writeDebugImages(task: OCRTask?, stage: Int) {
        for (result in task!!.results!!) {
            if (writeTarget) {
                writeTargetImage(result, stage)
            }
            if (writeReference) {
                writeReferenceImage(result, stage)
            }
            writeDebugImage(result, stage)
        }
    }

    
    private fun writeTargetImage(result: OCRResult, stage: Int) {
        val file = File(
            par.debugDir.absolutePath + "/" +
                    par.getDebugFilePrefix(result.target.charIndex) + ".ocr." + stage + ".ori.png"
        )
        val targetImage = buildImage(result.target.matrix)
        val colorImage = colorizeImage(targetImage, par.ocrTargetHaloLastColor)
        val scaledImage = buildScaledImage(colorImage, par.debugOCRImageScale)
        ImageIO.write(scaledImage, "png", file)
    }

    
    private fun writeReferenceImage(result: OCRResult, stage: Int) {
        val file = File(
            par.debugDir.absolutePath + "/" +
                    par.getDebugFilePrefix(result.target.charIndex) + ".ocr." + stage + ".ref." + result.character + ".png"
        )
        val referenceImage = buildImage(result.reference.matrix)
        val colorImage = colorizeImage(referenceImage, par.ocrReferenceHaloLastColor)
        val scaledImage = buildScaledImage(colorImage, par.debugOCRImageScale)
        ImageIO.write(scaledImage, "png", file)
    }

    
    private fun writeDebugImage(result: OCRResult, stage: Int) {
        val file = File(
            par.debugDir.absolutePath + "/" +
                    par.getDebugFilePrefix(result.target.charIndex) + ".ocr." + stage + ".res." + result.character + "." +
                    result.score + "." + result.target.transform + ".png"
        )
        val scaledImage = buildScaledImage(result.buildDebugImage(), par.debugOCRImageScale)
        ImageIO.write(scaledImage, "png", file)
    }

    companion object {
        /**
         * Gets how many times OCR has been run accross all threads
         */
        var totalOCRCount = 0
            private set
        private var totalOCRTime: Long = 0
        private const val debugStages = false

        /**
         * Gets the average total OCR time in ms
         */
        val averageOCRTime: Float
            get() = 1.0f * totalOCRTime / totalOCRCount
    }
}
