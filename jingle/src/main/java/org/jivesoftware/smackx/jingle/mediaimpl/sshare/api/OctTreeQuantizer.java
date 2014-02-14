/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import java.io.PrintStream;
import java.util.Vector;

import org.jivesoftware.smackx.jingle.SmackLogger;

/**
 * An image Quantizer based on the Octree algorithm. This is a very basic implementation
 * at present and could be much improved by picking the nodes to reduce more carefully 
 * (i.e. not completely at random) when I get the time.
 */
public class OctTreeQuantizer implements Quantizer {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(OctTreeQuantizer.class);

	/**
	 * The greatest depth the tree is allowed to reach
	 */
	final static int MAX_LEVEL = 5;

	/**
	 * An Octtree node.
	 */
	class OctTreeNode {
		int children;
		int level;
		OctTreeNode parent;
		OctTreeNode leaf[] = new OctTreeNode[8];
		boolean isLeaf;
		int count;
		int	totalRed;
		int	totalGreen;
		int	totalBlue;
		int index;
		
		/**
		 * A debugging method which prints the tree out.
		 */
		public void list(PrintStream s, int level) {
			String indentStr = "";
			for (int i = 0; i < level; i++)
				indentStr += " ";
			if (count == 0)
				LOGGER.debug(indentStr + index + ": count=" + count);
			else
				LOGGER.debug(indentStr + index + ": count=" + count + " red=" + (totalRed/count) + " green=" + (totalGreen / count) + " blue=" + (totalBlue / count));
			for (int i = 0; i < 8; i++)
				if (leaf[i] != null)
					leaf[i].list(s, level+2);
		}
	}

	private int nodes = 0;
	private OctTreeNode root;
	private int reduceColors;
	private int maximumColors;
	private int colors = 0;
	private Vector<OctTreeNode>[] colorList;
	
	public OctTreeQuantizer() {
		setup(256);
		colorList = new Vector[MAX_LEVEL+1];
		for (int i = 0; i < MAX_LEVEL+1; i++)
			colorList[i] = new Vector<OctTreeNode>();
		root = new OctTreeNode();
	}

	/**
	 * Initialize the quantizer. This should be called before adding any pixels.
	 * @param numColors the number of colors we're quantizing to.
	 */
	public void setup(int numColors) {
		maximumColors = numColors;
		reduceColors = Math.max(512, numColors * 2);
	}
	
	/**
	 * Add pixels to the quantizer.
	 * @param pixels the array of ARGB pixels
	 * @param offset the offset into the array
	 * @param count the count of pixels
	 */
	public void addPixels(int[] pixels, int offset, int count) {
		for (int i = 0; i < count; i++) {
			insertColor(pixels[i+offset]);
			if (colors > reduceColors)
				reduceTree(reduceColors);
		}
	}	

    /**
     * Get the color table index for a color.
     * @param rgb the color
     * @return the index
     */
	public int getIndexForColor(int rgb) {
		int red = (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = rgb & 0xff;

		OctTreeNode node = root;

		for (int level = 0; level <= MAX_LEVEL; level++) {
			OctTreeNode child;
			int bit = 0x80 >> level;

			int index = 0;
			if ((red & bit) != 0)
				index += 4;
			if ((green & bit) != 0)
				index += 2;
			if ((blue & bit) != 0)
				index += 1;

			child = node.leaf[index];

			if (child == null)
				return node.index;
			else if (child.isLeaf)
				return child.index;
			else
				node = child;
		}
		LOGGER.debug("getIndexForColor failed");
		return 0;
	}

	private void insertColor(int rgb) {
		int red = (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = rgb & 0xff;

		OctTreeNode node = root;

//		LOGGER.debug("insertColor="+Integer.toHexString(rgb));
		for (int level = 0; level <= MAX_LEVEL; level++) {
			OctTreeNode child;
			int bit = 0x80 >> level;

			int index = 0;
			if ((red & bit) != 0)
				index += 4;
			if ((green & bit) != 0)
				index += 2;
			if ((blue & bit) != 0)
				index += 1;

			child = node.leaf[index];

			if (child == null) {
				node.children++;

				child = new OctTreeNode();
				child.parent = node;
				node.leaf[index] = child;
				node.isLeaf = false;
				nodes++;
				colorList[level].addElement(child);

				if (level == MAX_LEVEL) {
					child.isLeaf = true;
					child.count = 1;
					child.totalRed = red;
					child.totalGreen = green;
					child.totalBlue = blue;
					child.level = level;
					colors++;
					return;
				}

				node = child;
			} else if (child.isLeaf) {
				child.count++;
				child.totalRed += red;
				child.totalGreen += green;
				child.totalBlue += blue;
				return;
			} else
				node = child;
		}
		LOGGER.debug("insertColor failed");
	}

	private void reduceTree(int numColors) {
		for (int level = MAX_LEVEL-1; level >= 0; level--) {
			Vector<OctTreeNode> v = colorList[level];
			if (v != null && v.size() > 0) {
				for (int j = 0; j < v.size(); j++) {
					OctTreeNode node = v.elementAt(j);
					if (node.children > 0) {
						for (int i = 0; i < 8; i++) {
							OctTreeNode child = node.leaf[i];
							if (child != null) {
								if (!child.isLeaf)
									LOGGER.debug("not a leaf!");
								node.count += child.count;
								node.totalRed += child.totalRed;
								node.totalGreen += child.totalGreen;
								node.totalBlue += child.totalBlue;
								node.leaf[i] = null;
								node.children--;
								colors--;
								nodes--;
								colorList[level+1].removeElement(child);
							}
						}
						node.isLeaf = true;
						colors++;
						if (colors <= numColors)
							return;
					}
				}
			}
		}

		LOGGER.debug("Unable to reduce the OctTree");
	}

    /**
     * Build the color table.
     * @return the color table
     */
	public int[] buildColorTable() {
		int[] table = new int[colors];
		buildColorTable(root, table, 0);
		return table;
	}
	
	/**
	 * A quick way to use the quantizer. Just create a table the right size and pass in the pixels.
     * @param inPixels the input colors
     * @param table the output color table
     */
	public void buildColorTable(int[] inPixels, int[] table) {
		int count = inPixels.length;
		maximumColors = table.length;
		for (int i = 0; i < count; i++) {
			insertColor(inPixels[i]);
			if (colors > reduceColors)
				reduceTree(reduceColors);
		}
		if (colors > maximumColors)
			reduceTree(maximumColors);
		buildColorTable(root, table, 0);
	}

	private int buildColorTable(OctTreeNode node, int[] table, int index) {
		if (colors > maximumColors)
			reduceTree(maximumColors);

		if (node.isLeaf) {
			int count = node.count;
			table[index] = 0xff000000 |
				((node.totalRed/count) << 16) |
				((node.totalGreen/count) << 8) |
				node.totalBlue/count;
			node.index = index++;
		} else {
			for (int i = 0; i < 8; i++) {
				if (node.leaf[i] != null) {
					node.index = index;
					index = buildColorTable(node.leaf[i], table, index);
				}
			}
		}
		return index;
	}
	
}

