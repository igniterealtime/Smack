/**
 *
 * Copyright 2006 Jerry Huxtable
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api;

/**
 * The interface for an image quantizer. The addColor method is called (repeatedly
 * if necessary) with all the image pixels. A color table can then be returned by 
 * calling the buildColorTable method.
 */
public interface Quantizer {
    /**
     * Initialize the quantizer. This should be called before adding any pixels.
     * @param numColors the number of colors we're quantizing to.
     */
    public void setup(int numColors);

    /**
     * Add pixels to the quantizer.
     * @param pixels the array of ARGB pixels
     * @param offset the offset into the array
     * @param count the count of pixels
     */
    public void addPixels(int[] pixels, int offset, int count);

    /**
     * Build a color table from the added pixels.
     * @return an array of ARGB pixels representing a color table
     */
    public int[] buildColorTable();

    /**
     * Using the previously-built color table, return the index into that table for a pixel.
     * This is guaranteed to return a valid index - returning the index of a color closer
     * to that requested if necessary. 
     * @param rgb the pixel to find
     * @return the pixel's index in the color table
     */
    public int getIndexForColor(int rgb);
}
