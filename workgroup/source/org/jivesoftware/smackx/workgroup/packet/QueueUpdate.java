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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * An IQ packet that encapsulates both types of workgroup queue
 * status notifications -- position updates, and estimated time
 * left in the queue updates.
 */
public class QueueUpdate implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "queue-status";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private int position;
    private int remainingTime;

    public QueueUpdate(int position, int remainingTime) {
        this.position = position;
        this.remainingTime = remainingTime;
    }

    /**
     * Returns the user's position in the workgroup queue, or -1 if the
     * value isn't set on this packet.
     *
     * @return the position in the workgroup queue.
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Returns the user's estimated time left in the workgroup queue, or
     * -1 if the value isn't set on this packet.
     *
     * @return the estimated time left in the workgroup queue.
     */
    public int getRemaingTime() {
        return remainingTime;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<queue-status xmlns=\"http://jabber.org/protocol/workgroup\">");
        if (position != -1) {
            buf.append("<position>").append(position).append("</position>");
        }
        if (remainingTime != -1) {
            buf.append("<time>").append(remainingTime).append("</time>");
        }
        buf.append("</queue-status>");
        return buf.toString();
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            boolean done = false;
            int position = -1;
            int timeRemaining = -1;
            while (!done) {
                parser.next();
                String elementName = parser.getName();
                if (parser.getEventType() == XmlPullParser.START_TAG && "position".equals(elementName)) {
                    try {
                        position = Integer.parseInt(parser.nextText());
                    }
                    catch (NumberFormatException nfe) {
                    }
                }
                else if (parser.getEventType() == XmlPullParser.START_TAG && "time".equals(elementName)) {
                    try {
                        timeRemaining = Integer.parseInt(parser.nextText());
                    }
                    catch (NumberFormatException nfe) {
                    }
                }
                else if (parser.getEventType() == XmlPullParser.END_TAG && "queue-status".equals(elementName)) {
                    done = true;
                }
            }
            return new QueueUpdate(position, timeRemaining);
        }
    }
}