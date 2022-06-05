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

package io.github.ifropc.kotomo.area;

import io.github.ifropc.kotomo.util.Parameters;

/**
 * Single algorithm step in area detector. Each step should do only one job such
 * as merging areas in left/right direction. Debug images can be generated after
 * each step.
 */
public abstract class AreaStep {

	protected AreaTask task;
	protected Parameters par = Parameters.getInstance();
	
	/**
	 * If true, debug images are generated after this step.
	 */
	final protected boolean addDebugImages;
	
	/**
	 * @param debugImages List of debug images that can be generated from this step.
	 * Any image generated in addDebugImages must first be registered here.
	 */
	public AreaStep(AreaTask task, String ... debugImages) {
		
		this.task = task;
		
		// check if this step should write debug images
		if (par.isSaveAreaFailed()) {
			for (String s1 : par.debugImages) {
				for (String s2 : debugImages) {
					if (s1.equals(s2)) {
						addDebugImages = true;
						return;
					}
				}
			}
		}
		addDebugImages = false;
	}
	
	/**
	 * Runs the algorithm step and creates debug information if requested.
	 */
	public void run() throws Exception {
		
		long started = System.currentTimeMillis();
		
		runImpl();
		
		if (task.columns != null && task.columns.size() > 0) {
			task.collectAreas();
		}
		
		long done = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			String subclassName = this.getClass().getName();
			System.out.println(subclassName+" "+(done - started)+" ms");
		}
		if (addDebugImages) {
			addDebugImages();
		}
		
		task.clearChangedFlags();
	}
	
	/**
	 * The actual implementation of the algorithm step.
	 */
	protected abstract void runImpl() throws Exception;
	
	/**
	 * Paints and adds debug images to task
	 */
	protected abstract void addDebugImages() throws Exception;
}
