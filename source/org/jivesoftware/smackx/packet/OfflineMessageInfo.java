/**
 * $RCSfile$
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
 * OfflineMessageInfo is an extension included in the retrieved offline messages requested by
 * the {@link org.jivesoftware.smackx.OfflineMessageManager}. This extension includes a stamp
 * that uniquely identifies the offline message. This stamp may be used for deleting the offline
 * message. The stamp may be of the form UTC timestamps but it is not required to have that format.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessageInfo implements PacketExtension {

    private String node = null;

    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "offline"
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return "offline";
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "http://jabber.org/protocol/offline"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return "http://jabber.org/protocol/offline";
    }

    /**
     * Returns the stamp that uniquely identifies the offline message. This stamp may
     * be used for deleting the offline message. The stamp may be of the form UTC timestamps
     * but it is not required to have that format.
     *
     * @return the stamp that uniquely identifies the offline message.
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the stamp that uniquely identifies the offline message. This stamp may
     * be used for deleting the offline message. The stamp may be of the form UTC timestamps
     * but it is not required to have that format.
     *
     * @param node the stamp that uniquely identifies the offline message.
     */
    public void setNode(String node) {
        this.node = node;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        if (getNode() != null)
            buf.append("<item node=\"").append(getNode()).append("\"/>");
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        /**
         * Creates a new Provider.
         * ProviderManager requires that every PacketExtensionProvider has a public,
         * no-argument constructor
         */
        public Provider() {
        }

        /**
         * Parses a OfflineMessageInfo packet (extension sub-packet).
         *
         * @param parser the XML parser, positioned at the starting element of the extension.
         * @return a PacketExtension.
         * @throws Exception if a parsing error occurs.
         */
        public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
            OfflineMessageInfo info = new OfflineMessageInfo();
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("item"))
                        info.setNode(parser.getAttributeValue("", "node"));
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("offline")) {
                        done = true;
                    }
                }
            }

            return info;
        }

    }
}
