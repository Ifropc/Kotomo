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

/**
 * Are area debug images saved to disk?
 */
public enum SaveAreaImages {
	
	/** 
	 * No debug images are saved. This should be used in production code.
	 */
	OFF,
	
	/**
	 * Area debug images are generated and saved for failed tests.
	 * This will slow down execution and should not be used in production.
	 */
	FAILED,
	
	/**
	 * Area debug images are generated and saved for all tests (failed and successful). 
	 * This will slow down execution and should not be used in production.
	 */
	ALL;
	
	SaveAreaImages() {
		this.level = ordinal();
	}
	
	private final int level;
	
	/**
	 * @return true if this debug level is greater or equal than argument level
	 */
	public boolean isGE(SaveAreaImages arg) {
		if (this.level >= arg.level) {
			return true;
		} else {
			return false;
		}
	}
}
