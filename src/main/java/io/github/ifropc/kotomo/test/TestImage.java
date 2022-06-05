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

package io.github.ifropc.kotomo.test;

import java.io.File;
import java.util.List;

/**
 * Test image and it's associated tests
 */
public class TestImage {

	/**
	 * Target image for tests
	 */
	File file;
	
	/**
	 * Test definitions to be run agains target image
	 */
	List<Test> tests;
	
	public String toString() {
		
		String value = file.getName()+" ";
		
		for (Test test : tests) {
			value += test.toString()+" ";
		}
		
		return value;
	}
}
