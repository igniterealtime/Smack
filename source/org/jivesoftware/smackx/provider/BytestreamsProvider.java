/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.Bytestream;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses a bytestream packet.
 * 
 * @author Alexander Wenckus
 */
public class BytestreamsProvider implements IQProvider {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.jivesoftware.smack.provider.IQProvider#parseIQ(org.xmlpull.v1.XmlPullParser)
	 */
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		boolean done = false;

		Bytestream toReturn = new Bytestream();

		String id = parser.getAttributeValue("", "sid");
		String mode = parser.getAttributeValue("", "mode");

		// streamhost
		String JID = null;
		String host = null;
		String port = null;

		int eventType;
		String elementName;
		// String namespace;
		while (!done) {
			eventType = parser.next();
			elementName = parser.getName();
			// namespace = parser.getNamespace();
			if (eventType == XmlPullParser.START_TAG) {
				if (elementName.equals(Bytestream.StreamHost.ELEMENTNAME)) {
					JID = parser.getAttributeValue("", "jid");
					host = parser.getAttributeValue("", "host");
					port = parser.getAttributeValue("", "port");
				} else if (elementName
						.equals(Bytestream.StreamHostUsed.ELEMENTNAME)) {
					toReturn.setUsedHost(parser.getAttributeValue("", "jid"));
				} else if (elementName.equals(Bytestream.Activate.ELEMENTNAME)) {
					toReturn.setToActivate(parser.getAttributeValue("", "jid"));
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (elementName.equals("streamhost")) {
					if (port == null) {
						toReturn.addStreamHost(JID, host);
					} else {
						toReturn.addStreamHost(JID, host, Integer
								.parseInt(port));
					}
					JID = null;
					host = null;
					port = null;
				} else if (elementName.equals("query")) {
					done = true;
				}
			}
		}

		toReturn.setMode((Bytestream.Mode.fromName(mode)));
		toReturn.setSessionID(id);
		return toReturn;
	}

}
