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

import io.github.ifropc.kotomo.util.Parameters


/**
 * Single algorithm step in area detector. Each step should do only one job such
 * as merging areas in left/right direction. Debug images can be generated after
 * each step.
 */
abstract class AreaStep(protected var task: AreaTask?, vararg debugImages: String) {
    protected var par = Parameters.instance

    /**
     * If true, debug images are generated after this step.
     */
    protected var addDebugImages: Boolean

    /**
     * @param debugImages List of debug images that can be generated from this step.
     * Any image generated in addDebugImages must first be registered here.
     */
    init {
        addDebugImages = false

        // check if this step should write debug images
        if (par.isSaveAreaFailed) {
            for (s1 in par.debugImages) {
                for (s2 in debugImages) {
                    if (s1 == s2) {
                        addDebugImages = true
                    }
                }
            }
        }
    }

    /**
     * Runs the algorithm step and creates debug information if requested.
     */
    @Throws(Exception::class)
    fun run() {
        val started = System.currentTimeMillis()
        runImpl()
        if (task!!.columns != null && task!!.columns!!.size > 0) {
            task!!.collectAreas()
        }
        val done = System.currentTimeMillis()
        if (par.isPrintDebug) {
            val subclassName = this.javaClass.name
            println(subclassName + " " + (done - started) + " ms")
        }
        if (addDebugImages) {
            addDebugImages()
        }
        task!!.clearChangedFlags()
    }

    /**
     * The actual implementation of the algorithm step.
     */
    @Throws(Exception::class)
    protected abstract fun runImpl()

    /**
     * Paints and adds debug images to task
     */
    @Throws(Exception::class)
    protected abstract fun addDebugImages()
}
