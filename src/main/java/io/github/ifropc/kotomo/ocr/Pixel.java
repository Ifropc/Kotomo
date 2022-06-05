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

public class Pixel {
	
	final int x;
	final int y;
	
	public Pixel(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public int hashCode() {
		return x + 100000*y;
	}
	
	@Override
	public boolean equals(Object obj) {
		return ((Pixel)obj).x == x && ((Pixel)obj).y == y;
	}
	
	public boolean isNeighbour(Pixel px2) {
		int deltaX = Math.abs(x - px2.x);
		int deltaY = Math.abs(y - px2.y);
		if (deltaX <= 1 && deltaY <= 1) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return x+","+y;
	}
}
