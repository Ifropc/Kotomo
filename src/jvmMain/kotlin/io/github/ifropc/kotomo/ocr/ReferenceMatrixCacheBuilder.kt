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

import io.github.ifropc.kotomo.util.ImageUtil.buildMatrix32
import io.github.ifropc.kotomo.util.ImageUtil.makeBlackAndWhite
import io.github.ifropc.kotomo.util.ImageUtil.stretchCheckRatio
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import io.github.ifropc.kotomo.util.buildMatrixHalo
import io.github.ifropc.kotomo.util.countBits
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage

/**
 * Builds the reference matrix cache.
 *
 * This needs to be run only if the character set changes.
 */
class ReferenceMatrixCacheBuilder {
    private val par = instance
    private var characters: MutableSet<Char>? = null
    private val components: ComponentBuilder

    init {
        components = ComponentBuilder()
    }

    
    fun buildCache() {
        println("Building reference cache")
        for (font in par.referenceFonts) {
            generateCharacters(font)
        }
    }

    /**
     * Generates characters for given font
     */
    
    private fun generateCharacters(font: String) {

        // skip if already generated
        val file = ReferenceMatrixHashCalculator.getReferenceFile(
            font, Parameters.targetSize,
            Parameters.ocrHaloSize, Characters.all
        )
        if (file!!.exists() && SKIP_EXISTS) {
            println("$font already generated\nfile:$file")
            return
        }

        // generate characters
        checkFont(font)
        characters = HashSet()
        println("Generating characters for $font\nfile:$file")
        var index = 0
        // using Arraylist type because List doesn't implement serializable
        val matrixList = ArrayList<ReferenceMatrix>()
        for (c in Characters.characters) {
            if (characters!!.contains(c)) {
                throw Exception("Duplicate character:$c")
            }
            val matrix = buildReferenceMatrix(c, Parameters.targetSize, font)
            matrixList.add(matrix)
            characters!!.add(c)
            if (++index % 100 == 0) {
                println("Progress:$index")
            }
        }
        println("$index characters done")

        // save to disk
        serialize(font, matrixList)
    }

    /**
     * Builds a bitmap representation of the character
     */
    
    private fun buildReferenceMatrix(character: Char, targetSize: Int, fontName: String): ReferenceMatrix {
        val image = paintCharacterBestFit(character, targetSize, fontName)
        val ref = ReferenceMatrix()
        ref.character = character
        ref.matrix = buildMatrix32(image)
        ref.halo = buildMatrixHalo(ref.matrix, Parameters.ocrHaloSize - 1)
        ref.pixels = countBits(ref.matrix)
        ref.fontName = fontName
        ref.components = components.buildComponents(ref)

        // is this the only character that is different in vertical orientation?
        if (character == '｜') {
            ref.character = 'ー'
        }
        return ref
    }

    /**
     * Paints character to targetSize image. Tries multiple font sizes and selects one that
     * fits the target size best. Resized to fit targetSize exactly.
     */
    
    private fun paintCharacterBestFit(character: Char, targetSize: Int, fontName: String): BufferedImage {

        // font size does not correspond to pixels,
        // draw with multiple sizes, select the closest one	
        var bestImage: BufferedImage? = null
        var bestFit = 100
        val style = if (isFontBold(fontName)) Font.BOLD else Font.PLAIN
        for (size in 25..44) {
            val font = Font(fontName, style, size)
            val image = paintCharacter(character, font)
            val hfit = Math.abs(image.width - targetSize)
            val vfit = Math.abs(image.height - targetSize)
            val fit = if (hfit < vfit) hfit else vfit
            if (fit < bestFit) {
                bestFit = fit
                bestImage = image
            }
        }

        // resize image to targetSize, center and surround with white border to 32x32 pixels
        bestImage = stretchCheckRatio(bestImage!!, targetSize, 32)
        bestImage = makeBlackAndWhite(bestImage, par.pixelRGBThreshold)
        checkRow(bestImage, character, 31 - (32 - targetSize) / 2)
        return bestImage
    }

    /**
     * True if font should be bold
     */
    
    private fun isFontBold(fontName: String): Boolean {
        for (i in par.referenceFonts.indices) {
            if (par.referenceFonts[i] == fontName) {
                return par.referenceFontsBold[i]
            }
        }
        throw Exception("Font:$fontName not found")
    }

    /**
     * Some character (山 for example) contain sharp edges at bottom corners in some fonts.
     * Reference images should be at neutral as possible so these edges are cut by one pixe.
     * row.
     */
    private fun checkRow(image: BufferedImage, character: Char, bottomRow: Int) {
        var bottomLeftPixels = 0
        var bottomMiddlePixels = 0
        var bottomRightPixels = 0
        for (x in 0..31) {
            if (image.getRGB(x, bottomRow) == Color.BLACK.rgb) {
                if (x <= 12) {
                    ++bottomLeftPixels
                } else if (x >= 20) {
                    ++bottomRightPixels
                } else {
                    ++bottomMiddlePixels
                }
            }
        }
        if (bottomMiddlePixels > 0) {
            return
        }
        if (bottomLeftPixels > 5 || bottomRightPixels > 5) {
            return
        }
        if (bottomLeftPixels == 0 || bottomRightPixels == 0) {
            return
        }

//		System.err.println("row:"+bottomRow);
//		System.err.println("pixels:"+bottomLeftPixels+","+bottomMiddlePixels+","+bottomRightPixels);
//		System.err.println("cut:"+character);
        for (x in 0..31) {
            image.setRGB(x, bottomRow, Color.WHITE.rgb)
        }
    }

    /**
     * Paints character to empty canvas
     */
    
    private fun paintCharacter(character: Char, font: Font): BufferedImage {
        val image = BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height) // fill with white
        graphics.background = Color.WHITE
        graphics.color = Color.BLACK
        graphics.font = font
        graphics.drawString(
            character.toString() + "",
            35,
            35
        ) // location unpredictable, draw to large canvas and center later
        return cutBorders(image)
    }

    /**
     * Serializes cache to disk using Kryo library
     *
     * https://github.com/EsotericSoftware/kryo
     * https://www.baeldung.com/kryo
     */
    
    private fun serialize(font: String, matrixList: ArrayList<ReferenceMatrix>) {
        val file = ReferenceMatrixHashCalculator.getReferenceFile(
            font, Parameters.targetSize,
            Parameters.ocrHaloSize, Characters.all
        )
        file.writeText(Json.encodeToString(matrixList))
    }

    /**
     * Checks that font supports Japanese characters. Throws exception if not.
     */
    
    private fun checkFont(fontName: String) {
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
            if (font.name == fontName) {
                if (font.canDisplay('新') && font.canDisplay('を') && font.canDisplay('ア')) {
                    return
                } else {
                    throw Exception("Font:$fontName doesn't support Japanese characters")
                }
            }
        }
        throw Exception("Font:$fontName not found")
    }

    companion object {
        /**
         * Skips cache files that already exists
         */
        private const val SKIP_EXISTS = false

        /**
         * Cut white pixels around character
         */
        private fun cutBorders(image: BufferedImage): BufferedImage {
            var xMin = image.width
            var xMax = 0
            var yMin = image.height
            var yMax = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if (image.getRGB(x, y) == Color.BLACK.rgb) {
                        if (x > xMax) xMax = x
                        if (x < xMin) xMin = x
                        if (y > yMax) yMax = y
                        if (y < yMin) yMin = y
                    }
                }
            }
            val width = xMax - xMin + 1
            val height = yMax - yMin + 1
            return image.getSubimage(xMin, yMin, width, height)
        }


        fun main(args: Array<String>) {
            try {
                val cache = ReferenceMatrixCacheBuilder()
                cache.buildCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
