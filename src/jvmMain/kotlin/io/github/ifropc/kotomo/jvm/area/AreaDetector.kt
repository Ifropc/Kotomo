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
package io.github.ifropc.kotomo.jvm.area

import io.github.ifropc.kotomo.config.Orientation
import io.github.ifropc.kotomo.jvm.util.Parameters
import mu.KotlinLogging

private val log = KotlinLogging.logger {  }

/**
 * Area detection coordinator.
 */
class AreaDetector {
    
    private var task: AreaTask? = null
    
    fun run(task: AreaTask) {
        this.task = task
        val started = System.currentTimeMillis()

        // apply unsharp mask filter to image
        SharpenImage(task).run()

        // create binary black and white image
        CreateBinaryImage(task).run()

        // invert regions with white text on black background
        InvertImage(task).run()

        // group touching pixels into areas
        FindAreas(task).run()

        // areas list is modified during vertical columns detection, make a copy so that 
        // original list can be used as input for horizontal columns detection
        val areas = task.areas

        // find vertical columns
        if (Parameters.orientationTarget === Orientation.AUTOMATIC || Parameters.orientationTarget === Orientation.VERTICAL) {
            findColumns(true)
            task.verticalColumns = task.columns
        } else {
            task.columns = ArrayList()
        }

        // find horizontal columns
        if (Parameters.orientationTarget === Orientation.AUTOMATIC || Parameters.orientationTarget === Orientation.HORIZONTAL) {
            task.areas = areas
            findColumns(false)
            task.horizontalColumns = task.columns
        } else {
            task.columns = ArrayList()
        }

        // filter columns by score so that each area in target image corresponds to only 
        // one column in one orientation.
        OrientationMerge(task).run()
        log.debug { "AreaDetector total " + (System.currentTimeMillis() - started) + " ms" }
        checkDebugImages()
    }

    /**
     * Groups areas into columns
     *
     * @vertical If true, uses vertical orientation. If false, uses horizontal
     * orientation.
     */
    
    private fun findColumns(vertical: Boolean) {
        task!!.columns = null
        Parameters.vertical = vertical

        // find columns
        FindColumns(task).run()

        // find dot or comma areas
        FindPunctuation(task).run()

        // split too large areas
        SplitAreas(task).run()

        // merge too small areas 
        //new MergeAreas2(task).run();
        MergeAreas(task).run()

        // find furigana columns
        FindFurigana(task).run()

        // find connections (continuing text) between columns
        FindConnections(task).run()
    }

    /**
     * Saves debug images if needed
     */
    
    private fun checkDebugImages() {
        when (val t = task) {
            is AreaTaskDebuggable -> {
                if (Parameters.isSaveAreaAll) {
                    t.writeDebugImages()
                } else if (Parameters.isSaveAreaFailed) {
                    // check that expected rectangles (areas or columns) are present
                    rect@ for (rect in Parameters.expectedRectangles) {
                        for (col in task!!.columns!!) {
                            if (col.rectangle == rect) {
                                continue@rect
                            }
                            for (area in col.areas) {
                                if (area.rect == rect) {
                                    continue@rect
                                }
                            }
                        }
                        t.writeDebugImages()
                        break
                    }
                }
            }
        }
    }
}
