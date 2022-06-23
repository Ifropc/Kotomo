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

package io.github.ifropc.kotomo.jvm.util

import java.io.File

object FileParameters {
    /**
     * Directory relative to package root that contains the data files
     */
    private var dataDirName = "data"

    /**
     * Directory inside data dir that contains cache files
     */
    private var cacheDirName = "cache"


    val cacheDir: File
        get() = File(Util.findFile(dataDirName).toString() + "/" + cacheDirName)

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
        get() = Util.findFile(testDirName)
}
