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

import io.github.ifropc.kotomo.CharacterColor
import io.github.ifropc.kotomo.Orientation
import io.github.ifropc.kotomo.ocr.Rectangle
import io.github.ifropc.kotomo.util.Util.findFile
import java.awt.Color
import java.io.File

object Parameters {
    /**
     * Directory relative to package root that contains the data files
     */
    private var dataDirName = "data"

    /**
     * Directory inside data dir that contains cache files
     */
    private var cacheDirName = "cache"

    /**
     * Current orientation.
     *
     * If true, use vertical reading direction.
     * If false, use horizontal reading direction.
     */
    var vertical = true

    /**
     * Target orientation
     */
    var orientationTarget = Orientation.AUTOMATIC

    /**
     * Target character color.
     */
    var colorTarget = CharacterColor.AUTOMATIC

    /**
     * Fonts used to generate reference characters.
     * First font is the primary font used for initial alignment. Other fonts
     * are used for refined alignments.
     */

    var referenceFonts = arrayOf("MS Gothic") // MS Gothic, SimSun, Meiryo UI, SimHei, SimSun bold

    /**
     * If true, font should be bold
     */

    var referenceFontsBold = booleanArrayOf(false, true)

    // image sharpening parameters
    var unsharpAmount = 4.0f // 4.0f
    var unsharpRadius = 2 // 2
    var unsharpThreshold = 2 // 2

    /**
     * Minimum pixel RGB value
     */

    var pixelRGBThreshold = 140

    /**
     * Color used for OCR debug images. First halo color is the layer closes to matching pixels.
     * In-between colors are iterpolated
     */

    var ocrTargetHaloFirstColor = Color(255, 0, 0)


    var ocrTargetHaloLastColor = Color(255, 175, 175)


    var ocrReferenceHaloFirstColor = Color(100, 100, 100)


    var ocrReferenceHaloLastColor = Color(195, 195, 195)

    /**
     * Score for common pixel that is found in both target and reference images.
     */

    var ocrBlackPixelScore = 4f

    /**
     * Score for white pixels that are not part of target or reference images.
     */

    var ocrWhiteScore = 4f

    /**
     * Score for pixels in target but not in reference image. Indexed by halo layer.
     */

    var ocrTargetHaloScores = floatArrayOf(-1f, -5f, -12f)

    /**
     * Score for pixels in reference but not in target image. Indexed by halo layer.
     */

    var ocrReferenceHaloScores = floatArrayOf(-1f, -4f, -10f)

    /**
     * This is added to each score
     */

    var ocrBaseScore = 1000f

    /**
     * Number of best results that are returned from OCR stage 1
     */

    var ocrKeepResultsStage1 = 30

    /**
     * Number of best results that are returned from OCR stage 2
     */

    var ocrkeepResultsStage2 = 10

    /**
     * Maximum number of target characters that can be returned by
     * single OCR run. 4 is a good number since it restricts the
     * dictionary search nicely and matches most users CPU count.
     */
    var ocrMaxCharacters = 4

    /**
     * Number of threads used to run OCR in parallel. This should
     * not be larger than user's CPU count and not larger than
     * ocrMaxCharacters
     */

    var ocrThreads = 4

    val cacheDir: File
        get() = File(findFile(dataDirName).toString() + "/" + cacheDirName)

    // debug-related parameters
    var saveAreaImages = SaveAreaImages.OFF
    var saveOCRImages = SaveOCRImages.OFF

    /**
     * saveAreaImages >= SAVE_FAILED
     */
    val isSaveAreaFailed: Boolean
        get() = saveAreaImages.isGE(SaveAreaImages.FAILED)

    /**
     * saveAreaImages >= SAVE_ALL
     */
    val isSaveAreaAll: Boolean
        get() = saveAreaImages.isGE(SaveAreaImages.ALL)

    /**
     * saveOCRImages >= SAVE_FAILED
     */
    val isSaveOCRFailed: Boolean
        get() = saveOCRImages.isGE(SaveOCRImages.FAILED)

    /**
     * saveOCRImages >= SAVE_ALL
     */
    val isSaveOCRAll: Boolean
        get() = saveOCRImages.isGE(SaveOCRImages.ALL)

    /**
     * Selects which area debug images are saved.
     * Comment out unneeded images.
     */
    var debugImages = arrayOf(
        "original",
        "sharpened",
        "binary",
        "invert",
        "touching",
        "background",
        "areas",
        "columns",
        "punctuation",
        "splitareas",
        "mergeareas",
        "furigana",
        "connections",
        "combined"
    )

    /**
     * Small debug images are enlarged by this amount
     */
    var smallDebugAreaImageScale = 1

    /**
     * If image size (larger dimension) is smaller than this, smallDebugAreaImageScale is used
     */
    var smallDebugAreaImageThreshold = 500

    /**
     * If true, writes area debug image to clipboard instead of file.
     * Should be used only if there's single debug image.
     */
    var debugAreaImageToClipboard = false

    /**
     * OCR debug images are enlarged by this amount
     */

    var debugOCRImageScale = 5

    /**
     * Maximum number of debug images generated
     */
    var maxDebugImages = 1000

    /**
     * If set, these characters are the expected (correct) OCR results in test image.
     * These are kept in the result queue even if bad score.
     */

    var expectedCharacters: String? = null

    /**
     * Areas or columns that should be present in test image
     */
    var expectedRectangles: MutableList<Rectangle> = ArrayList()

    /**
     * Directory relative to package root where debug images are stored
     */
    private var debugDirName = "test results"

    /**
     * Directory relative to package root where debug images are stored
     */
    val debugDir: File
        get() = File(testDir.absolutePath + "//" + debugDirName)

    /**
     * Directory relative to package root where test set specifications are stored
     */
    private var testDirName = "test"

    /**
     * Directory relative to package root where test set specifications are stored
     */
    val testDir: File
        get() = findFile(testDirName)

    // rest of the parameters are for internal use and should not be edited
    var tempDebugFilePrefix: String? = null
    private var tempDebugFileIndex = 1

    /**
     * String added in front of debug file names. Contains test reference and image sequence number
     */
    val debugFilePrefix: String
        get() = if (tempDebugFilePrefix == null) {
            "0." + tempDebugFileIndex++
        } else {
            tempDebugFilePrefix + "." + tempDebugFileIndex++
        }

    fun getDebugFilePrefix(charIndex: Int?): String {
        return if (charIndex == null) {
            debugFilePrefix
        } else if (tempDebugFilePrefix == null) {
            charIndex.toString() + "." + tempDebugFileIndex++
        } else {
            tempDebugFilePrefix + "." + tempDebugFileIndex++
        }
    }

    /**
     * Target size for reference characters. Target characters are scaled to this size.
     * Should be below 32 to make room for transformations. If modified reference cache
     * must to be regenerated.
     */
    const val targetSize = 30


    /**
     * How many halo layers are generated around reference and target characters.
     * If this is increased ReferenceMatrixCacheBuilder must be run again. Most layers are
     * one pixel wide, last layer contains all remaining pixels.
     */
    const val ocrHaloSize = 3
}

object FixedParameters {
    var fixedBlackLevelEnabled = false
    var fixedBlackLevelRed = 0
    var fixedBlackLevelGreen = 0
    var fixedBlackLevelBlue = 0
    var fixedBlackLevelRange = 50
}
