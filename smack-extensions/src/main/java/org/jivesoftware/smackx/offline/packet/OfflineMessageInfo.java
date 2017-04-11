/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.offline.packet;

import java.io.IOException;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * OfflineMessageInfo is an extension included in the retrieved offline messages requested by
 * the {@link org.jivesoftware.smackx.offline.OfflineMessageManager}. This extension includes a stamp
 * that uniquely identifies the offline message. This stamp may be used for deleting the offline
 * message. The stamp may be of the form UTC timestamps but it is not required to have that format.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessageInfo implements ExtensionElement {

    private String node = null;

    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "offline"
    *
    * @return the XML element name of the stanza(/packet) extension.
    */
    @Override
    public String getElementName() {
        return "offline";
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "http://jabber.org/protocol/offline"
     *
     * @return the XML namespace of the stanza(/packet) extension.
     */
    @Override
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

    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        if (getNode() != null)
            buf.append("<item node=\"").append(getNode()).append("\"/>");
        buf.append("</").append(getElementName()).append('>');
        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<OfflineMessageInfo> {

        /**
         * Parses a OfflineMessageInfo stanza(/packet) (extension sub-packet).
         *
         * @param parser the XML parser, positioned at the starting element of the extension.
         * @return a PacketExtension.
         * @throws IOException 
         * @throws XmlPullParserException 
         */
        @Override
        public OfflineMessageInfo parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException {
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
