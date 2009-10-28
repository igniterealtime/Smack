/*
 * Created on 2009-05-05
 */
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.packet.PacketExtension;

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