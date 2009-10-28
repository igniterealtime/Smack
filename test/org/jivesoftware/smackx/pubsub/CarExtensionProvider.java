/*
 * Created on 2009-05-05
 */
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class CarExtensionProvider implements PacketExtensionProvider
{

	public PacketExtension parseExtension(XmlPullParser parser) throws Exception
	{
		String color = null;
		int numTires = 0;
		
		for (int i=0; i<2; i++)
		{
			while (parser.next() != XmlPullParser.START_TAG);
			
			if (parser.getName().equals("paint"))
			{
				color = parser.getAttributeValue(0);
			}
			else
			{
				numTires = Integer.parseInt(parser.getAttributeValue(0));
			}
		}
		while (parser.next() != XmlPullParser.END_TAG);
		return new CarExtension(color, numTires);
	}
	
}