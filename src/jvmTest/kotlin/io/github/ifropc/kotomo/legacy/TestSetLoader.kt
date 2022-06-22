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

import io.github.ifropc.kotomo.jvm.util.Parameters
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Loads set of tests from a file
 *
 */
class TestSetLoader {
    private val par = Parameters
    private var testDir: File? = null

    /**
     * Reads test sets from "test" directory
     * @return Test set name -> TestSet object
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readTestSets(): Map<String?, TestSet> {
        val testSets: MutableMap<String?, TestSet> = HashMap()
        testDir = par.testDir
        for (file in testDir!!.listFiles()) {
            if (file.name.endsWith(".txt") && file.name.lowercase(Locale.getDefault()) != "readme.txt") {
                val testSet = readTestSet(file)
                testSets[testSet.name] = testSet
            }
        }
        return testSets
    }

    /**
     * Reads tests from file
     *
     * Example:
     *
     * # this is a comment
     * filename1 51,325,22,114 # area test x,y,width,height
     * filename2 35,75,思  # OCR test x,y,character
     *
     * @param file Filename in base/test dir
     */
    @Throws(Exception::class)
    private fun readTestSet(testSetFile: File): TestSet {
        val testImageDirName = testSetFile.name.replace(".txt", "") + " images"
        val testImagesDir = File(testSetFile.parentFile.absolutePath + "//" + testImageDirName)
        if (!testImagesDir.exists()) {
            throw Exception("Test image directory:$testImagesDir doesn't exist")
        }

        // parse file
        println("parsing file:$testSetFile")
        val testImages: MutableList<TestImage> = ArrayList()
        var line: String?
        val `in` = BufferedReader(
            InputStreamReader(FileInputStream(testSetFile), "UTF-8")
        )
        while (`in`.readLine().also { line = it } != null) {
            val test = parseLine(testImagesDir, line!!)
            if (test != null) {
                testImages.add(test)
            }
        }
        `in`.close()
        println(testImages.size.toString() + " tests loaded")
        val testSet = TestSet()
        testSet.name = testSetFile.name.replace(".txt", "")
        testSet.images = testImages
        return testSet
    }

    /**
     * Parses one line from test definition file
     *
     * @return null if no test image found
     */
    @Throws(Exception::class)
    private fun parseLine(testImagesDir: File, line: String): TestImage? {
        var line = line
        line = line.trim { it <= ' ' }
        if (line.startsWith("#") || line.startsWith("//") || line.isBlank()) {
            return null
        }
        val tests: MutableList<Test> = ArrayList()
        var file: File? = null
        for (snip in line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (snip.startsWith("#") || snip.startsWith("//")) {
                break
            } else if (file == null) {
                file = File("$testImagesDir//$snip")
                if (!file.exists()) {
                    throw Exception("Test image file:$file doesn't exist")
                }
            } else {
                try {
                    val test = parseTest(snip)
                    test.image = file
                    tests.add(test)
                } catch (e: Exception) {
                    System.err.println("line:$line")
                    throw e
                }
            }
        }
        if (tests.size == 0) {
            return null
        }
        val testSet = TestImage()
        testSet.file = file
        testSet.tests = tests
        return testSet
    }

    companion object {
        /**
         * Parses single test. Parameters separated by commas.
         *
         * For example:
         * 51,325,22,114 -> area test (x, y, width, height)
         * 35,75,思  -> OCR test (x, y, character)
         */
        @Throws(Exception::class)
        private fun parseTest(testStr: String): Test {
            val parameters = testStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parameters.size > 4) {
                throw Exception("Syntax error at:$testStr too many parameters")
            }
            val ints: MutableList<Int> = ArrayList()
            var characters = ""
            for (parameter in parameters) {
                try {
                    ints.add(parameter.toInt())
                } catch (e: NumberFormatException) {
                    characters = parameter
                }
            }
            return if (characters.length == 0) {
                // Area test
                if (ints.size < 4) {
                    throw Exception("Syntax error at:$testStr too few parameters")
                }
                val x = ints[0]
                val y = ints[1]
                val width = ints[2]
                val height = ints[3]
                AreaTest(x, y, width, height)
            } else {
                // OCR test
                if (ints.size < 2) {
                    throw Exception("Syntax error at:$testStr too few parameters")
                }
                val x = ints[0]
                val y = ints[1]
                OCRTest(x, y, characters)
            }
        }
    }
}
