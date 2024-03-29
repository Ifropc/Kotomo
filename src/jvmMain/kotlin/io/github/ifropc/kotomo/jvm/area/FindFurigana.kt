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

import io.github.ifropc.kotomo.ocr.entities.KotomoRectangle
import io.github.ifropc.kotomo.jvm.util.JVMUtil
import io.github.ifropc.kotomo.config.Parameters

/**
 * Find columns that contain furigana
 */
class FindFurigana(task: AreaTask?) : AreaStep(task, "furigana") {
    private var index: RTree<Column>? = null
    
    override fun runImpl() {

        // create column index
        index = RTree(task!!.binaryImage, task!!.columns)

        // find furigana columns
        for (col in task!!.columns!!) {
            findFurigana(col)
        }
    }

    /**
     * Finds argument column's furigana columns.
     */
    private fun findFurigana(col: Column) {

        // Furigana column must be close (right side if vertical, above if horizontal) 
        // but much thinner than main column.
        val probe: KotomoRectangle
        probe = if (col.isVertical) {
            KotomoRectangle(
                col.maxX + 1, col.y,
                col.width / 2, col.height
            )
        } else {
            KotomoRectangle(
                col.x, col.y - col.height / 2 - 1,
                col.width, col.height / 2
            )
        }
        if (checkBackground(probe)) {
            return
        }
        val furiganaCols: MutableList<Column> = ArrayList()
        for (col2 in index!![probe, col]) {
            if (col2.minorDim < col.minorDim * 0.55f && col2.minorDim > col.minorDim * 0.20f && col2.majorDim < col.majorDim * 1.05f && col2.medianAreaSize < col.medianAreaSize * 0.5f) {
                furiganaCols.add(col2)
            }
        }
        for (furigana in furiganaCols) {
            furigana.isFurigana = true
            furigana.isChanged = true
            col.furiganaColumns.add(furigana)
        }
    }

    /**
     * @return true if probe intersects with the background
     */
    private fun checkBackground(probe: KotomoRectangle): Boolean {
        val pixels = task!!.countPixels(probe, true, false)
        return pixels >= 2
    }

    
    override fun addDebugImages() {
        JVMUtil.withDebuggable(task!!) { task ->
            task!!.addDefaultDebugImage("furigana", Parameters.vertical)
        }
    }
}
