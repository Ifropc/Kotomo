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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Image that contains single character cropped from original image
 */
public class SubImage {
	
	/**
	 * Cropped around single character
	 */
	public BufferedImage image;
	
	/**
	 * Location of the character in original image.
	 */
	public Rectangle location;
	
	/**
	 * Column that contains the rectangle. This can be null if manual rectangles
	 * are used. 
	 */
	public Column column;
	
	public SubImage(BufferedImage binaryImage, Rectangle location, Column column) {
		
		this.image = binaryImage;
		this.location = location;
		this.column = column;
	}
	
	public boolean isVertical() {
		
		if (column != null) {
			return column.vertical;
		} else {
			return true;
		}
	}
	
	public int getMinX() {
		return location.x;
	}
	
	public int getMaxX() {
		return location.x + location.width-1;
	}
	
	public int getMinY() {
		return location.y;
	}
	
	public int getMaxY() {
		return location.y + location.height-1;
	}
	
	public int getMidX() {
		return location.x + location.width/2;
	}
	
	public int getMidY() {
		return location.y + location.height/2;
	}
	
	@Override
	public String toString() {
		return location.toString();
	}	
}
