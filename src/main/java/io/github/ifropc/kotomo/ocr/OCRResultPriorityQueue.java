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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Pritority queue with limited size that contains best OCR results.
 */
public class OCRResultPriorityQueue {

	/**
	 * Maximum number of best results stored in the queue
	 */
	private final int capacity;
	
	/**
	 * Best results
	 */
	private PriorityQueue<OCRResult> queue;
	
	/**
	 * Score for the worst result currently in the queue 
	 */
	private Integer worstScore = null; 
	
	/**
	 * If set, this character is never removed from the results
	 */
	private Character expectedCharacter = null;
	
	/**
	 * Result that contains the expected character
	 */
	private OCRResult expectedResult = null;
	
	/**
	 * @param capacity Number of best results stored in the queue
	 */
	public OCRResultPriorityQueue(int capacity) {
		
		this.capacity = capacity;
		queue = initQueue(capacity);
	}
	
	/**
	 * Sets the expected character. Never removed from results. Used for debug
	 * purposes.
	 */
	public void setExpectedCharacter(Character character) {
		
		expectedCharacter = character;
	}
	
	/**
	 * Adds a new result to the queue. If the queue is full, checks first 
	 * if this result is better than the worst existing result.
	 */
	public void add(OCRResult result) {
		
		if (result.getCharacter().equals(expectedCharacter)) {
			expectedResult = result;
		}
	
		if (worstScore != null && result.score < worstScore) {
			return;
		}
		
		queue.add(result);
		
		if (queue.size() > capacity) {
			queue.poll();
			worstScore = queue.peek().score;
		}
	}
	
	/**
	 * Gets results in descending score order (from best to worst)
	 */
	public List<OCRResult> getResults() {
		
		List<OCRResult> results = new ArrayList<OCRResult>();
		results.addAll(queue);
		
		if (expectedResult != null) {
			//results.remove(results.size()-1);
			if (!results.contains(expectedResult)) {
				results.add(expectedResult);
			}
		}
		
		Collections.reverse(results);
		
		return results;
	}
	
	/**
	 * Creates empty priority queue
	 */
	private static PriorityQueue<OCRResult> initQueue(int capacity) {
		
		return new PriorityQueue<OCRResult>(
				capacity,
				new Comparator<OCRResult>() {
	
					@Override
					public int compare(OCRResult o1, OCRResult o2) {
						return o1.getScore().compareTo(o2.getScore());
					}
				});
	}
}
