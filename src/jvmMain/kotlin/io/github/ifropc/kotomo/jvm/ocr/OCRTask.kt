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
package io.github.ifropc.kotomo.jvm.ocr

import java.awt.image.BufferedImage

/**
 * Input and output for the OCR algorithm
 */
open class OCRTask
/**
 * @param image Target sub-image around single character. OCR is run
 * agains this image. This should be black and white sharpened image.
 */(
    /**
     * Target sub-image around single character. OCR is run against this image.
     * This should be black and white sharpened image.
     */
    val image: BufferedImage
) {
    /**
     * Index of the target character (0 = character closest to mouse cursor)
     */

	var charIndex: Int? = null

    /**
     * Best results (characters that are closest match to the target image)
     * sorted by score (best match first).
     */

	@Deprecated("Replace with following pipeline submit task -> process -> transform into results")
    var results: List<OCRResult>? = null

    /**
     * If true, this sub-image is from column that does not contain the original target point.
     * This is used to restrict dictionary search to characters within single column.
     */
    @Deprecated("Not used anymore")
    var columnChanged = false

    /**
     * Gets results as a string of characters. Best match first.
     */
    val resultString: String
        get() {
            val resultStr = StringBuilder()
            for (result in results!!) {
                resultStr.append(result.reference.character)
            }
            return resultStr.toString()
        }

    /**
     * Gets the identified character (best match).
     */
    val character: Char?
        get() = if (results!!.size > 0) {
            results!![0].character
        } else {
            null
        }
}
