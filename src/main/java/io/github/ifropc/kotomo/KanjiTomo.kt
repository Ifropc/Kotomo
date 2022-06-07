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

import io.github.ifropc.kotomo.area.AreaDetector
import io.github.ifropc.kotomo.area.AreaTask
import io.github.ifropc.kotomo.area.SubImage
import io.github.ifropc.kotomo.ocr.OCRManager
import io.github.ifropc.kotomo.ocr.OCRTask
import io.github.ifropc.kotomo.ocr.ReferenceMatrixCacheLoader
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.PrintLevel
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Main class of the KanjiTomo OCR library
 */
class KanjiTomo {
    private val par = Parameters.getInstance()
    private var ocr: OCRManager? = null
    private var areaTask: AreaTask? = null
    private var subImages: List<SubImage>? = null
    private var results: OCRResults? = null

    /**
     * Loads data structures into memory. This should be called first on it's own
     * thread as the program starts because loading data can take couple of seconds.
     * It's allowed to call this multiple times, results are cached and further calls
     * don't take any more time unless dictionary is changed.
     */
    @Throws(Exception::class)
    fun loadData() {
        if (ocr == null) {
            ocr = OCRManager()
            ocr!!.loadReferenceData()
            ReferenceMatrixCacheLoader().load()
        }
    }

    /**
     * Sets the target image. This can be a screenshot around target characters or a whole page.
     */
    @Throws(Exception::class)
    fun setTargetImage(image: BufferedImage) {
        if (ocr == null) {
            loadData()
        }
        val started = System.currentTimeMillis()
        detectAreas(image)
        val time = System.currentTimeMillis() - started
        if (par.isPrintDebug) {
            println("Target image processed, $time ms\n")
        }
        if (par.isPrintOutput && !par.isPrintDebug) {
            println("Target image processed\n")
        }
    }

    /**
     * Gets columns detected from target image
     */
    val columns: List<Column>
        get() {
            if (areaTask == null) {
                return ArrayList()
            }
            val simpleColumns: MutableList<Column> = ArrayList()
            for (column in areaTask!!.columns!!) {
                val simpleColumn = column.getSimpleColumn()
                if (column.previousColumn != null) {
                    simpleColumn.previousColumn = column.previousColumn!!.getSimpleColumn()
                }
                if (column.nextColumn != null) {
                    simpleColumn.nextColumn = column.nextColumn!!.getSimpleColumn()
                }
                simpleColumns.add(simpleColumn)
            }
            return simpleColumns
        }

    /**
     * Runs OCR starting from a point.
     *
     * @param point Coordinates inside target image. Closest character near this point
     * will be selected as the first target character. Point should correspond to mouse
     * cursor position relative to target image.
     *
     * @return null if no characters found near point
     */
    @Throws(Exception::class)
    fun runOCR(point: Point): OCRResults? {
        if (areaTask == null) {
            throw Exception("Target image not set")
        }
        if (par.isPrintOutput) {
            println("Run OCR at point:" + point.getX().toInt() + "," + point.getY().toInt())
        }

        // select areas near point
        subImages = areaTask!!.getSubImages(point)
        return runOCR()
    }

    /**
     * Runs OCR inside pre-defined areas where each rectangle contains single characters.
     * This can be used if area detection is done externally and KanjiTomo is only used for final OCR.
     */
    @Throws(Exception::class)
    fun runOCR(areas: List<Rectangle?>): OCRResults? {
        if (areaTask == null) {
            throw Exception("Target image not set")
        }
        if (par.isPrintOutput) {
            println("Run OCR rectangle list")
        }

        // build subimages from argument areas
        subImages = areaTask!!.getSubImages(areas)
        return runOCR()
    }

    /**
     * Runs OCR for target areas (SubImages)
     */
    @Throws(Exception::class)
    private fun runOCR(): OCRResults? {
        val started = System.currentTimeMillis()

        // get target locations
        val locations = mutableListOf<Rectangle>()
        for (subImage in subImages!!) {
            locations.add(subImage.location!!)
            verticalOrientation = subImage.isVertical
        }
        if (subImages!!.size == 0) {
            if (par.isPrintOutput) {
                println("No characters identified")
            }
            return null
        }

        // run ocr for each character
        val ocrTasks = mutableListOf<OCRTask>()
        var charIndex = 0
        var lastColumn: io.github.ifropc.kotomo.area.Column? = null
        for (subImage in subImages!!) {
            val ocrTask = OCRTask(subImage.image)
            ocrTask.charIndex = charIndex++
            ocrTasks.add(ocrTask)
            if (lastColumn == null) {
                lastColumn = subImage.column
            } else {
                if (lastColumn !== subImage.column) {
                    ocrTask.columnChanged = true
                }
            }
            ocr!!.addTask(ocrTask)
        }
        ocr!!.waitUntilDone()

        // collect identified characters
        val characters = mutableListOf<String>()
        val ocrScores = mutableListOf<List<Int>>()
        for (ocrTask in ocrTasks) {
            characters.add(ocrTask.resultString)
            val scores: MutableList<Int> = ArrayList()
            for (result in ocrTask.results) {
                scores.add(result.score)
            }
            ocrScores.add(scores)
        }

        results = OCRResults(characters, locations, ocrScores, verticalOrientation)

        val time = System.currentTimeMillis() - started
        if (par.isPrintOutput) {
            println(
                """
    ${results.toString()}
    
    """.trimIndent()
            )
        }
        if (par.isPrintDebug) {
            println("OCR runtime $time ms\n")
        }
        return results
    }

    /**
     * Analyzes the image and detects areas that might contain characters.
     */
    @Throws(Exception::class)
    private fun detectAreas(image: BufferedImage) {
        areaTask = AreaTask(image)
        AreaDetector().run(areaTask!!)
    }

    /**
     * Vertical orientation was used in the area closest to selected point
     */
    private var verticalOrientation = true

    /**
     * Sets the reading direction. Default is automatic.
     *
     * Target image needs to be re-analyzed after changing the orientation by calling setTargetImage again.
     */
    fun setOrientation(orientation: Orientation?) {
        par.orientationTarget = orientation
    }

    /**
     * Sets character and background color. Black and white characters work best,
     * but coloured characters might also work if there's enough contrast.
     *
     * Target image needs to be re-analyzed after changing the color by calling setTargetImage again.
     *
     * Default: CharacterColor.AUTOMATIC
     */
    fun setCharacterColor(color: CharacterColor?) {
        par.colorTarget = color
    }

    /**
     * If true, OCR results are printed to stdout.
     * If false, nothing is printed.
     * Default: false
     */
    fun setPrintOutput(printOutput: Boolean) {
        if (printOutput == false) {
            par.printLevel = PrintLevel.OFF
        } else {
            par.printLevel = PrintLevel.BASIC
        }
    }

    /**
     * Stops all threads. This should be called before closing the program.
     */
    fun close() {
        ocr!!.stopThreads()
    }
}
