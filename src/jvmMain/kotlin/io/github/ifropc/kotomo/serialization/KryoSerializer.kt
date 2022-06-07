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

package io.github.ifropc.kotomo.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import io.github.ifropc.kotomo.Word
import io.github.ifropc.kotomo.ocr.Component
import io.github.ifropc.kotomo.ocr.ReferenceMatrix
import io.github.ifropc.kotomo.ocr.ReferenceMatrixCacheLoader
import io.github.ifropc.kotomo.ocr.Transformation
import io.github.ifropc.kotomo.serialization.KryoFactory.kryo
import java.awt.Rectangle
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Deprecated("To be replaced with kotlin serialization")
object KryoSerializer : Serializer {
    override fun <T> writeToFile(value: T, path: String) {
        Output(FileOutputStream(File(path))).use {
            kryo.writeClassAndObject(it, value)
        }
    }

    override fun <T> readFromFile(path: String): T {
        val kryo = kryo
        val input = Input(FileInputStream(File(path)))
        return input.use {
            kryo.readClassAndObject(it) as T
        }
    }
}

/**
 * Build Kryo objects and registers classes
 */
private object KryoFactory {// classes in serializable object trees must be defined here
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
