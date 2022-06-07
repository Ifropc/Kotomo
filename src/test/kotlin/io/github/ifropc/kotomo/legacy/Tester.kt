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
package io.github.ifropc.kotomo.legacy

import io.github.ifropc.kotomo.KanjiTomo
import io.github.ifropc.kotomo.ocr.OCR
import io.github.ifropc.kotomo.util.Parameters
import io.github.ifropc.kotomo.util.PrintLevel
import io.github.ifropc.kotomo.util.SaveAreaImages
import io.github.ifropc.kotomo.util.SaveOCRImages
import java.io.File
import java.util.regex.Pattern
import javax.imageio.ImageIO

/**
 * Runs tests
 */
class Tester {
    private val par = Parameters.getInstance()
    private val dataLoaded = false
    private var tomo: KanjiTomo? = null
    private var loader: TestSetLoader? = null
    private var runAreaTests = false
    private var runOCRTests = false

    /** Test set name -> TestSet object  */
    private var testSets: Map<String?, TestSet?>? = null

    /**
     * Loads data structures used by OCR. Must be called before any tests are run.
     */
    @Throws(Exception::class)
    fun loadData() {
        if (dataLoaded) {
            return
        }
        tomo = KanjiTomo()
        tomo!!.loadData()
        loader = TestSetLoader()
        testSets = loader!!.readTestSets()
    }

    /**
     * Runs all tests
     */
    @Throws(Exception::class)
    fun runTests(areaTests: Boolean, ocrTests: Boolean) {
        for (testSet in testSets!!.values) {
            runTests(testSet!!.name, areaTests, ocrTests, null, *arrayOf<String>())
        }
    }

    /**
     * Runs tests
     *
     * @param testSetName Test set name, for example: "default" or "local". Tests are loaded
     * from file: "test/testSetName.txt"
     * @param characters Character filter. Run only tests with target character in this list
     * @param images Filename filter. If empty, runs all tests. if set, runs tests that have matching
     * image filename. Regular expression syntax
     */
    @Throws(Exception::class)
    fun runTests(
        testSetName: String?,
        areaTests: Boolean,
        ocrTests: Boolean,
        characters: String?,
        vararg images: String?
    ) {
        testDebugImageDirExists()
        runAreaTests = areaTests
        runOCRTests = ocrTests
        val filterPatterns: MutableList<Pattern> = ArrayList()
        for (filter in images) {
            filterPatterns.add(Pattern.compile(filter))
        }

        // run tests that match any filter
        val failedTests = mutableListOf<Test>()
        if (!testSets!!.containsKey(testSetName)) {
            throw Exception("Test set:$testSetName not found")
        }
        val testSet = testSets!![testSetName]
        tests@ for (testImage in testSet!!.images!!) {
            if (images.size == 0) {
                runTestSet(testImage, characters, failedTests)
            } else {
                for (filter in filterPatterns) {
                    val m = filter.matcher(testImage!!.file!!.name)
                    if (m.matches()) {
                        runTestSet(testImage, characters, failedTests)
                        continue@tests
                    }
                }
            }
        }

        // print failed tests
        if (failedTests.size > 0) {
            System.err.println("Failed tests:")
            for (test in failedTests) {
                System.err.println(test)
            }
        }
    }

    /**
     * Analyzes all image files in directory. Finds areas and columns but doesn't run any OCR.
     * Debug images are saved. Check Parameters.debugImages.
     *
     * @param suffix For example: ".png", only these files are processed.
     */
    @Throws(Exception::class)
    fun runAreasDirectory(directory: File, suffix: String?) {
        par.saveAreaImages = SaveAreaImages.ALL
        for (file in directory.listFiles()) {
            if (!file.name.endsWith(suffix!!)) {
                continue
            }
            val image = ImageIO.read(file)
            tomo!!.setTargetImage(image)
        }
    }

    @Throws(Exception::class)
    private fun runTestSet(testImage: TestImage?, characters: String?, failedTests: MutableList<Test>) {
        println(
            """
    
    Image:${testImage!!.file!!.name}
    """.trimIndent()
        )

        // load test image
        val testImageName = testImage.file!!.name.replace(".png", "")
        par.tempDebugFilePrefix = testImageName

        // set expected areas
        if (runAreaTests) {
            par.expectedRectangles.clear()
            for (test in testImage.tests!!) {
                if (test is AreaTest) {
                    par.expectedRectangles.add(test.rect)
                }
            }
        }

        // find areas 
        tomo!!.setTargetImage(ImageIO.read(testImage.file))

        // run test
        for (test in testImage.tests!!) {
            if (!runAreaTests && test is AreaTest) {
                continue
            }
            if (!runOCRTests && test is OCRTest) {
                continue
            }
            if (runOCRTests && characters != null && test is OCRTest) {
                var found = false
                for (testCharacter in test.characters.toCharArray()) {
                    if (characters.contains(testCharacter.toString() + "")) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    continue
                }
            }
            println("Test:$test")
            val passed = runTest(test)
            if (passed) {
                println("Passed")
            } else {
                println("Failed")
                failedTests!!.add(test)
            }
        }
    }

    @Throws(Exception::class)
    private fun runTest(test: Test?): Boolean {
        return if (test is AreaTest) {
            runAreaTest(test)
        } else {
            runOCRTest(test as OCRTest?)
        }
    }

    /**
     * Runs area test
     *
     * @return true if test passed, else false
     */
    @Throws(Exception::class)
    private fun runAreaTest(test: AreaTest): Boolean {
        for (column in tomo!!.columns) {
            if (column.rect == test.rect) {
                return true
            }
            for (area in column.areas!!) {
                if (area == test.rect) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Runs OCR test
     *
     * @return true if test passed, else false
     */
    @Throws(Exception::class)
    private fun runOCRTest(test: OCRTest?): Boolean {
        par.ocrMaxCharacters = test!!.characters.length
        par.expectedCharacters = test.characters
        val results = tomo!!.runOCR(test.point)
        if (results == null || results.bestMatchingCharacters.length == 0) {
            return false
        }
        return if (results.bestMatchingCharacters == test.characters) {
            true
        } else {
            false
        }
    }

    fun close() {
        if (tomo != null) {
            tomo!!.close()
        }
    }

    /**
     * Checks if debug image dir exits. If not, creates it.
     */
    @Throws(Exception::class)
    private fun testDebugImageDirExists() {
        val debugDir = par.debugDir
        if (!debugDir.exists()) {
            debugDir.mkdir()
        }
    }

    /**
     * Deletes old debug images
     */
    @Throws(Exception::class)
    private fun clearDebugImages() {
        testDebugImageDirExists()
        for (file in par.debugDir.listFiles()) {
            if (file.name.endsWith(".png")) {
                file.delete()
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val par = Parameters.getInstance()
            par.printLevel = PrintLevel.DEBUG
            par.saveAreaImages = SaveAreaImages.OFF
            par.saveOCRImages = SaveOCRImages.OFF
            par.ocrThreads = 1
            val tester = Tester()
            try {
                tester.loadData()
                tester.clearDebugImages()
                tester.runTests(false, true)
                //			tester.runTests("default", false, true, "今", "1.png");
//			tester.runTests("local", false, true, "想", "487.png");
//			tester.runTestSearchDefault("日", true);
//			tester.runTestSearchNames("あきら", true);
//			tester.runTestSearchCombined("希一", true);
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (OCR.getTotalOCRCount() > 0) {
                println("Average OCR time:" + Math.round(OCR.getAverageOCRTime()) + " ms")
            }
            tester.close()
            System.exit(0)
        }
    }
}
