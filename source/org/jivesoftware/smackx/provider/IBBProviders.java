/*
 * Created on Jul 5, 2005
 */
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.IBBExtensions;
import org.xmlpull.v1.XmlPullParser;

/**
 * 
 * Parses an IBB packet.
 * 
 * @author Alexander Wenckus
 */
public class IBBProviders {

	/**
	 * Parses an open IBB packet.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Open implements IQProvider {
		public IQ parseIQ(XmlPullParser parser) throws Exception {
			final String sid = parser.getAttributeValue("", "sid");
			final int blockSize = Integer.parseInt(parser.getAttributeValue("",
					"block-size"));

			return new IBBExtensions.Open(sid, blockSize);
		}
	}

	/**
	 * Parses a data IBB packet.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Data implements PacketExtensionProvider {
		public PacketExtension parseExtension(XmlPullParser parser)
				throws Exception {
			final String sid = parser.getAttributeValue("", "sid");
			final long seq = Long
					.parseLong(parser.getAttributeValue("", "seq"));
			final String data = parser.nextText();

			return new IBBExtensions.Data(sid, seq, data);
		}
	}

	/**
	 * Parses a close IBB packet.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Close implements IQProvider {
		public IQ parseIQ(XmlPullParser parser) throws Exception {
			final String sid = parser.getAttributeValue("", "sid");

			return new IBBExtensions.Close(sid);
		}
	}

}
