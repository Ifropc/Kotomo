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

package net.kanjitomo.area;

import java.awt.image.BufferedImage;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;

/**
 * Creates a binary (black and white) image from grayscale image.
 */
public class CreateBinaryImage extends AreaStep {
		
	public CreateBinaryImage(AreaTask task) {
		super(task, "binary");
	}
	
	@Override
	protected void runImpl() throws Exception {

		// TODO instead of static blackThreshold calculate a histogram?
		BufferedImage bwImage = ImageUtil.makeBlackAndWhite(task.sharpenedImage,
				Parameters.fixedBlackLevelEnabled ? null : par.pixelRGBThreshold); 
		
		task.binaryImage = ImageUtil.createMatrixFromImage(bwImage); 
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		
		BufferedImage image = ImageUtil.createImageFromMatrix(task.binaryImage);
		task.addDebugImage(image, "binary");
	}
}
