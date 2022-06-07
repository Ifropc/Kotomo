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
package io.github.ifropc.kotomo.util

/**
 * How much information is printed to STDOUT
 */
enum class PrintLevel {
    /**
     * No information is printed to stdout. This should be used in production code.
     */
    OFF,

    /**
     * Basic information is printed to stdout.
     */
    BASIC,

    /**
     * Extra debug information is printed to stdout.
     */
    DEBUG;

    private val level: Int

    init {
        level = ordinal
    }

    /**
     * @return true if this debug level is greater or equal than argument level
     */
    fun isGE(arg: PrintLevel): Boolean {
        return if (level >= arg.level) {
            true
        } else {
            false
        }
    }
}
