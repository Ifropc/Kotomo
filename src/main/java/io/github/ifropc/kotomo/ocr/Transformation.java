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

package io.github.ifropc.kotomo.ocr;

import java.io.Serializable;

/**
 * Parameters used to modify bitmap before aligment.
 */
public class Transformation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// positive translate moves image right or down by one pixel
	final int horizontalTranslate; // TODO rename to deltaX?
	final int verticalTranslate;
	
	// positive stretch makes image larger by one pixel
	final int horizontalStretch;
	final int verticalStretch;
	
	/**
	 * Creates default no-op transformation (all parameters zero).
	 */
	public Transformation() {
		
		horizontalTranslate = 0;
		verticalTranslate = 0;
		horizontalStretch = 0;
		verticalStretch = 0;
	}
	
	public Transformation(int horizontalTranslate, int verticalTranslate,
			int horizontalStretch, int verticalStretch) {
		
		this.horizontalTranslate = horizontalTranslate;
		this.verticalTranslate = verticalTranslate;
		this.horizontalStretch = horizontalStretch;
		this.verticalStretch = verticalStretch;		
	}
	
	/**
	 * @return true if this transformation contains argument parameters
	 */
	public boolean contains(int horizontalTranslate, int verticalTranslate,
			int horizontalStretch, int verticalStretch) {
		
		if (this.horizontalTranslate == horizontalTranslate &&
			this.verticalTranslate == verticalTranslate &&
			this.horizontalStretch == horizontalStretch &&
			this.verticalStretch == verticalStretch) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		
		Transformation p = (Transformation)obj;
		
		return p.horizontalTranslate == horizontalTranslate &&
			p.verticalTranslate == verticalTranslate &&
			p.horizontalStretch == horizontalStretch &&
			p.verticalStretch == verticalStretch;
	}
	
	@Override
	public int hashCode() {
		
		return horizontalTranslate + verticalTranslate + 
			horizontalStretch + verticalStretch;
	}
	
	@Override
	public String toString() {
		
		return horizontalTranslate+"."+verticalTranslate+"."+
				horizontalStretch+"."+verticalStretch;
	}
}
