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

import com.esotericsoftware.kryo.Kryo
import io.github.ifropc.kotomo.Word
import io.github.ifropc.kotomo.ocr.Component
import io.github.ifropc.kotomo.ocr.ReferenceMatrix
import io.github.ifropc.kotomo.ocr.Transformation
import java.awt.Rectangle

/**
 * Build Kryo objects and registers classes
 */
object KryoFactory {// classes in serializable object trees must be defined here
    // all files serialized by Kryo must be rebuild if this list changes!
    /**
     * Gets Kryo object and registers classes
     */
	@JvmStatic
	val kryo: Kryo
        get() {
            val kryo = Kryo()

            // classes in serializable object trees must be defined here
            // all files serialized by Kryo must be rebuild if this list changes!
            kryo.register(Word::class.java)
            kryo.register(HashMap::class.java)
            kryo.register(ArrayList::class.java)
            kryo.register(ReferenceMatrix::class.java)
            kryo.register(Component::class.java)
            kryo.register(Transformation::class.java)
            kryo.register(Rectangle::class.java)
            kryo.register(String::class.java)
            kryo.register(Char::class.java)
            kryo.register(Int::class.java)
            kryo.register(Float::class.java)
            kryo.register(IntArray::class.java)
            return kryo
        }
}
