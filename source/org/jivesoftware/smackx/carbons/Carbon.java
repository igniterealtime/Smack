/**
 * Copyright 2013 Georg Lukas
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

package org.jivesoftware.smackx.carbons;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the packet extension and a {@link PacketExtensionProvider} to parse
 * message carbon copies from a packet. The extension
 * <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> is
 * meant to synchronize a message flow to multiple presences of a user.
 *
 * <p>The {@link Carbon.Provider} must be registered in the
 * <b>smack.properties</b> file for the elements <b>sent</b> and
 * <b>received</b> with namespace <b>urn:xmpp:carbons:2</b></p> to be used.
 *
 * @author Georg Lukas
 */
public class Carbon implements PacketExtension {
    public static final String NAMESPACE = "urn:xmpp:carbons:2";

    private Direction dir;
    private Forwarded fwd;

    public Carbon(Direction dir, Forwarded fwd) {
        this.dir = dir;
        this.fwd = fwd;
    }

    /**
     * get the direction (sent or received) of the carbon.
     *
     * @return the {@link Direction} of the carbon.
     */
    public Direction getDirection() {
        return dir;
    }

    /**
     * get the forwarded packet.
     *
     * @return the {@link Forwarded} message contained in this Carbon.
     */
    public Forwarded getForwarded() {
        return fwd;
    }

    @Override
    public String getElementName() {
        return dir.toString();
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"")
                .append(getNamespace()).append("\">");

        buf.append(fwd.toXML());

        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * An enum to display the direction of a {@link Carbon} message.
     */
    public static enum Direction {
        received,
        sent
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            Direction dir = Direction.valueOf(parser.getName());
            Forwarded fwd = null;

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("forwarded")) {
                    fwd = (Forwarded)new Forwarded.Provider().parseExtension(parser);
                }
                else if (eventType == XmlPullParser.END_TAG && dir == Direction.valueOf(parser.getName()))
                    done = true;
            }
            if (fwd == null)
                throw new Exception("sent/received must contain exactly one <forwarded> tag");
            return new Carbon(dir, fwd);
        }
    }

    /**
     * Packet extension indicating that a message may not be carbon-copied.
     */
    public static class Private implements PacketExtension {
        public static final String ELEMENT = "private";

        public String getElementName() {
            return ELEMENT;
        }

        public String getNamespace() {
            return Carbon.NAMESPACE;
        }

        public String toXML() {
            return "<" + ELEMENT + " xmlns=\"" + Carbon.NAMESPACE + "\"/>";
        }
    }
}
