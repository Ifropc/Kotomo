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

import io.github.ifropc.kotomo.util.Parameters.Companion.instance
import java.awt.image.BufferedImage
import java.util.concurrent.LinkedBlockingQueue

/**
 * Handles OCR threads.
 */
class OCRManager {
    private val par = instance
    private val threads: MutableList<OCRThread> = ArrayList()
    private val tasks = LinkedBlockingQueue<OCRTask>(10)
    private val results = LinkedBlockingQueue<OCRTask?>(10)
    private var taskCount = 0

    /**
     * Creates threads for running ocr. This should be created once when the
     * program launches, not for each ocr run.
     */
    init {
        for (i in 0 until par.ocrThreads) {
            val thread = OCRThread()
            thread.start()
            threads.add(thread)
        }
    }

    /**
     * Loads reference matrices into memory. This should be called in background thread
     * before user interaction.
     */
    
    fun loadReferenceData() {
        val cache = ReferenceMatrixCacheLoader()
        cache.load()
    }

    /**
     * Adds a new task to the queue
     * @param task
     * @throws InterruptedException
     */
    
    fun addTask(task: OCRTask) {
        tasks.put(task)
        ++taskCount
    }

    /**
     * Waits until all pending tasks have been completed.
     * The results are written to each submitted task.
     */
    
    fun waitUntilDone() {
        while (taskCount > 0) {
            results.take()
            --taskCount
        }
    }

    /**
     * Thread that executes OCR tasks
     */
    private inner class OCRThread : Thread() {
        private val ocr: OCR
        private var task: OCRTask? = null

        init {
            ocr = OCR()
        }

        override fun run() {
            try {
                while (true) {

                    // wait for next task
                    task = tasks.take()
                    if (task is OCRTaskSentinel) {
                        return
                    }

                    // execute the task
                    ocr.run(task)
                    //superOcr.run(task); // TODO remove
                    results.put(task)
                    task = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stops all threads. This should be called before closing the program.
     */
    fun stopThreads() {
        for (i in threads.indices) {
            tasks.add(OCRTaskSentinel(null))
        }
    }

    /**
     * Signals threads that all work is done and they can stop waiting
     */
    private inner class OCRTaskSentinel(image: BufferedImage?) : OCRTask(image!!)
}
