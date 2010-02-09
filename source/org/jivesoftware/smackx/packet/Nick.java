/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A minimalistic implementation of a {@link PacketExtension} for nicknames.
 * 
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="http://xmpp.org/extensions/xep-0172.html">XEP-0172: User Nickname</a>
 */
public class Nick implements PacketExtension {

	public static final String NAMESPACE = "http://jabber.org/protocol/nick";

	public static final String ELEMENT_NAME = "nick";

	private String name = null;

	public Nick(String name) {
		this.name = name;
	}

	/**
	 * The value of this nickname
	 * 
	 * @return the nickname
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of this nickname
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jivesoftware.smack.packet.PacketExtension#getElementName()
	 */
	public String getElementName() {
		return ELEMENT_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jivesoftware.smack.packet.PacketExtension#getNamespace()
	 */
	public String getNamespace() {
		return NAMESPACE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jivesoftware.smack.packet.PacketExtension#toXML()
	 */
	public String toXML() {
		final StringBuilder buf = new StringBuilder();

		buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(
				NAMESPACE).append("\">");
		buf.append(getName());
		buf.append("</").append(ELEMENT_NAME).append('>');

		return buf.toString();
	}

	public static class Provider implements PacketExtensionProvider {

		public PacketExtension parseExtension(XmlPullParser parser)
				throws Exception {
			
            parser.next();
			final String name = parser.getText();

			// Advance to end of extension.
			while(parser.getEventType() != XmlPullParser.END_TAG) {
				parser.next();
			}

			return new Nick(name);
		}
	}
}
