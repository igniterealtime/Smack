/**
 *
 * Copyright 2009 Robin Collier.
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * 
 * @author Robin Collier
 *
 */
class CarExtension implements PacketExtension
{
	private String color;
	private int numTires;

	public CarExtension(String col, int num)
	{
		color = col;
		numTires = num;
	}
	
	public String getColor()
	{
		return color;
	}

	public int getNumTires()
	{
		return numTires;
	}

	public String getElementName()
	{
		return "car";
	}

	public String getNamespace()
	{
		return "pubsub:test:vehicle";
	}

	public String toXML()
	{
		return "<" + getElementName() + " xmlns='" + getNamespace() + "'><paint color='" + 
			getColor() + "'/><tires num='" + getNumTires() + "'/></" + getElementName() + ">";
	}
	
}