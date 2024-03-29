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

import java.io.File

/**
 * Test image and it's associated tests
 */
class TestImage {
    /**
     * Target image for tests
     */
    var file: File? = null

    /**
     * Test definitions to be run agains target image
     */
    var tests: MutableList<Test>? = null
    override fun toString(): String {
        var value = file!!.name + " "
        for (test in tests!!) {
            value += "$test "
        }
        return value
    }
}
