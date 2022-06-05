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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.ifropc.kotomo.util.Parameters;

/**
 * Handles OCR threads.
 */
public class OCRManager {

	private Parameters par = Parameters.getInstance();
	private List<OCRThread> threads = new ArrayList<OCRThread>();
	private LinkedBlockingQueue<OCRTask> tasks = new LinkedBlockingQueue<OCRTask>(10);
	private LinkedBlockingQueue<OCRTask> results = new LinkedBlockingQueue<OCRTask>(10);
	private int taskCount = 0;
	
	/**
	 * Creates threads for running ocr. This should be created once when the
	 * program launches, not for each ocr run.
	 */
	public OCRManager() {
		
		for (int i=0 ; i<par.ocrThreads ; i++) {
			OCRThread thread = new OCRThread();
			thread.start();
			threads.add(thread);
		}
	}
	
	/**
	 * Loads reference matrices into memory. This should be called in background thread
	 * before user interaction.
	 */
	public void loadReferenceData() throws Exception {
	
		ReferenceMatrixCacheLoader cache = new ReferenceMatrixCacheLoader();
		cache.load();
	}
	
	/**
	 * Adds a new task to the queue
	 * @param task
	 * @throws InterruptedException
	 */
	public void addTask(OCRTask task) throws InterruptedException {
		
		tasks.put(task);
		++taskCount;
	}
	
	/**
	 * Waits until all pending tasks have been completed. 
	 * The results are written to each submitted task.
	 */
	public void waitUntilDone() throws InterruptedException {
		
		while (taskCount > 0) {
			results.take();
			--taskCount;
		}
	}
	
	/**
	 * Thread that executes OCR tasks
	 */
	private class OCRThread extends Thread {
		
		private OCR ocr;
		private OCRTask task;
		
		public OCRThread() {
			this.ocr = new OCR();
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					
					// wait for next task
					task = tasks.take();
					if (task instanceof OCRTaskSentinel) {
						return;
					}
								
					// execute the task
					ocr.run(task);
					//superOcr.run(task); // TODO remove
					results.put(task);
					task = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops all threads. This should be called before closing the program.
	 */
	public void stopThreads() {
		
		for (int i=0 ; i<threads.size() ; i++) {
			tasks.add(new OCRTaskSentinel(null));
		}
	}
	
	/**
	 * Signals threads that all work is done and they can stop waiting
	 */
	private class OCRTaskSentinel extends OCRTask {

		public OCRTaskSentinel(BufferedImage image) {
			super(image);
		}
	}
}
