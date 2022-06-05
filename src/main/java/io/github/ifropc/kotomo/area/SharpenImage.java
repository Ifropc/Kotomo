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

import io.github.ifropc.kotomo.util.ImageUtil;

/**
 * Runs unsharp mask to the original image
 */
public class SharpenImage extends AreaStep {
	
	public SharpenImage(AreaTask task) {
		super(task, "original", "sharpened");
	}

	@Override
	protected void runImpl() throws Exception {
		task.sharpenedImage = ImageUtil.sharpenImage(task.originalImage, par);
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		task.addDebugImage(task.originalImage, "original");
		task.addDebugImage(task.sharpenedImage, "sharpened");
	}
}
