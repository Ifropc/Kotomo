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
package io.github.ifropc.kotomo.config

/**
 * Are OCR debug images saved to disk?
 */
enum class SaveOCRImages {
    /**
     * No debug images are saved. This should be used in production code.
     */
    OFF,

    /**
     * OCR debug images are generated and saved for failed tests.
     * This will slow down execution and should not be used in production.
     */
    FAILED,

    /**
     * OCR debug images are generated and saved for all tests (failed and successful).
     * This will slow down execution and should not be used in production.
     */
    ALL;

    private val level: Int

    init {
        level = ordinal
    }

    /**
     * @return true if this debug level is greater or equal than argument level
     */
    fun isGE(arg: SaveOCRImages): Boolean {
        return level >= arg.level
    }
}
