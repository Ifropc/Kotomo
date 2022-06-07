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

import io.github.ifropc.kotomo.ocr.ReferenceMatrixCacheBuilder
import io.github.ifropc.kotomo.util.Parameters
import org.junit.jupiter.api.Disabled
import java.awt.Point
import java.awt.Rectangle
import javax.imageio.ImageIO
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCleanOCR {
    companion object {
        var runRebuild = true
    }

    private lateinit var tomo: KanjiTomo

    @BeforeTest
    fun init() {
        if (runRebuild) {
            Parameters.instance.cacheDir.deleteRecursively()
            Parameters.instance.cacheDir.mkdir()

            val cache = ReferenceMatrixCacheBuilder()
            cache.buildCache()

            runRebuild = false
        }

        tomo = KanjiTomo()
        tomo.loadData()
    }

    @Test
    fun test1() {
        val result = testPoint("1.jpg", "俺の家", 4f)
        assertEquals(3951, result[0].scores[0])
    }

    @Test
    fun test2() {
        // TODO: should be 汗拭きシ
        testPoint("2.jpg", "汗拭念シ", 3f)
    }

    @Test
    fun test3BlackBG() {
        // TODO: investigate why only one character is detected
        testPoint("3.png", "地", 4f)
    }

    @Test
    fun test4() {
        // TODO: should be 侵略者と
        testPoint("4.png", "授略者と", 0.0f)
    }

    @Test
    @Disabled // TODO: doesn't work on this example at all
    fun test5LowQuality() {
        testPoint("5.png", "地球侵略", 4f)
    }

    @Test
    fun test6() {
        // TODO: last character is not detected
        testPoint("6.png", "厄災を回", 4f)
    }

    @Test
    @Disabled // Gray background. Investigate if it's possible to detect characters
    fun test7Gray() {
        testPoint("7.png", "", 4f)
    }

    @Test
    fun test8() {
        // TODO: last character is not detected
        testPoint("8.png", "辺境蛮", 3f)
    }

    @Test
    fun test9() {
        // TODO: should be 大烏先輩
        testPoint("9.png", "火烏先荒", 0.0f)
    }

    @Test
    @Disabled // Same as test 7
    fun test10GrayBG() {
        testPoint("10.png", "", 4f)
    }

    private fun testPoint(filename: String, expected: String, accpetedMarginPercent: Float): MutableList<io.github.ifropc.kotomo.IdentifiedCharacter> {
        val image = ImageIO.read(this::class.java.classLoader.getResourceAsStream(filename))
        tomo.setTargetImage(image)
        val results = tomo.runOCR(Point(0, 0))

        assertEquals(expected, results!!.bestMatchingCharacters)
        results!!.characters.map { it.referenceCharacters.toCharArray().zip(it.scores.normalized()).drop(1).first() }
            .forEach { assertDominance(it, accpetedMarginPercent / 100) }

        return results.characters
    }

    private fun testArea(filename: String, width: Int, height: Int, expected: String,  accpetedMarginPercent: Float) {
        val image = ImageIO.read(this::class.java.classLoader.getResourceAsStream(filename))
        tomo.setTargetImage(image)
        val results = tomo.runOCR(listOf(Rectangle(0, 0, width, height)))

        assertEquals(expected, results!!.bestMatchingCharacters)
        results!!.characters.map { it.referenceCharacters.toCharArray().zip(it.scores.normalized()).drop(1).first() }
            .forEach { assertDominance(it, accpetedMarginPercent / 100) }
    }

    private fun assertDominance(pair: Pair<Char, Float>, margin: Float) {
        assertTrue("Score for character ${pair.first} is too big. Score is ${pair.second} and accepted margin is $margin") { (1 - pair.second) > margin }

    }

    private fun List<Int>.normalized(): List<Float> {
        return this.maxOf { it }.run { this@normalized.map { it.toFloat().div(this) } }
    }

}


