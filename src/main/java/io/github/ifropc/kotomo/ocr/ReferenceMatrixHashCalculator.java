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

import java.io.File;

import io.github.ifropc.kotomo.util.Parameters;

/**
 * Calculates reference matrix hash values and cache file names.
 */
public class ReferenceMatrixHashCalculator {

	/**
	 * Gets filename that represents set of reference matrices
	 */
	public static String getReferenceFileName(String font, int targetSize, int ocrHaloSize,
			String characters) throws Exception {
		
		int hashCode = calcHashCode(font, Parameters.targetSize,
				Parameters.ocrHaloSize, Characters.all);
		return "CHARACTERS_"+Integer.toHexString(hashCode).toUpperCase()+".cache";
	}
	
	/**
	 * Gets file that represents set of reference matrices
	 */
	public static File getReferenceFile(String font, int targetSize, int ocrHaloSize,
			String characters) throws Exception {
		
		return new File(Parameters.getInstance().getCacheDir()+"/"+
				getReferenceFileName(font, targetSize, ocrHaloSize, characters));
	}
	
	private static int calcHashCode(String font, int targetSize, int ocrHaloSize,
			String characters) {
		
		int hashCode = smear(font.hashCode());
		hashCode += smear(targetSize*1000);
		hashCode += smear(ocrHaloSize*1000000);
		
		for (Character c : characters.toCharArray()) {
			hashCode += smear(c);
		}
		
		return hashCode;
	}
	
	/*
	 * This method was written by Doug Lea with assistance from members of JCP
	 * JSR-166 Expert Group and released to the public domain, as explained at
	 * http://creativecommons.org/licenses/publicdomain
	 * 
	 * As of 2010/06/11, this method is identical to the (package private) hash
	 * method in OpenJDK 7's java.util.HashMap class.
	 */
	private static int smear(int hashCode) {
		
		// https://stackoverflow.com/questions/9624963/java-simplest-integer-hash
	    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
	    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
	}
}
