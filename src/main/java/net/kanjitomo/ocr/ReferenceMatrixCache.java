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

package net.kanjitomo.ocr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kanjitomo.util.Parameters;

/**
 * Cache of reference matrices for each character listed in Character class
 */
public class ReferenceMatrixCache {
	
	/** 
	 * Font name -> list of reference matrices for each character 
	 */ 
	private Map<String, List<ReferenceMatrix>> cache;	
	
	public ReferenceMatrixCache() {
		
		cache = new HashMap<>();
		for (String font : Parameters.getInstance().referenceFonts) {
			cache.put(font, new ArrayList<ReferenceMatrix>());
		}
	}
	
	/**
	 * Adds matrix list to the cache
	 */
	public void put(String font, List<ReferenceMatrix> list) {
		
		cache.put(font, list);
	}
	
	/**
	 * Gets reference matrices for given font
	 */
	public List<ReferenceMatrix> get(String font) throws Exception {
		
		if (!cache.containsKey(font)) {
			throw new Exception("Cache doesn't contain font:"+font+", regenerate cache");
		}
		return cache.get(font);
	}
	
	/**
	 * Gets reference matrices for all fonts
	 */
	public List<ReferenceMatrix> getAll() {
		
		List<ReferenceMatrix> matrices = new ArrayList<>();
		for (String font : cache.keySet()) {
			matrices.addAll(cache.get(font));
		}
		return matrices;
	}
	
	/**
	 * Gets fonts in the cache
	 */
	public Set<String> getFonts() {
		
		return cache.keySet();
	}
}
