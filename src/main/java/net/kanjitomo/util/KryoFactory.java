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

package net.kanjitomo.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;

import net.kanjitomo.Word;
import net.kanjitomo.ocr.Component;
import net.kanjitomo.ocr.ReferenceMatrix;
import net.kanjitomo.ocr.Transformation;

/**
 * Build Kryo objects and registers classes
 */
public abstract class KryoFactory {

	/**
	 * Gets Kryo object and registers classes
	 */
	public static Kryo getKryo() {
		
		Kryo kryo = new Kryo();
		
		// classes in serializable object trees must be defined here
		// all files serialized by Kryo must be rebuild if this list changes!
		kryo.register(Word.class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
		kryo.register(ReferenceMatrix.class);
		kryo.register(Component.class);
		kryo.register(Transformation.class);
		kryo.register(Rectangle.class);
		kryo.register(String.class);
		kryo.register(Character.class);
		kryo.register(Integer.class);
		kryo.register(Float.class);
		kryo.register(int[].class);
		
		return kryo;
	}
}
