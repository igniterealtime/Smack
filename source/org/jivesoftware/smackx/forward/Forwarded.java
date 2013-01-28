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

package org.jivesoftware.smackx.forward;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Packet extension for XEP-0297: Stanza Forwarding. This class implements
 * the packet extension and a {@link PacketExtensionProvider} to parse
 * forwarded messages from a packet. The extension
 * <a href="http://xmpp.org/extensions/xep-0297.html">XEP-0297</a> is
 * a prerequisite for XEP-0280 (Message Carbons).
 *
 * <p>The {@link Forwarded.Provider} must be registered in the
 * <b>smack.properties</b> file for the element <b>forwarded</b> with
 * namespace <b>urn:xmpp:forwarded:0</b></p> to be used.
 *
 * @author Georg Lukas
 */
public class Forwarded implements PacketExtension {
    public static final String NAMESPACE = "urn:xmpp:forward:0";
    public static final String ELEMENT_NAME = "forwarded";

    private DelayInfo delay;
    private Packet forwardedPacket;

    /**
     * Creates a new Forwarded packet extension.
     *
     * @param delay an optional {@link DelayInfo} timestamp of the packet.
     * @param fwdPacket the packet that is forwarded (required).
     */
    public Forwarded(DelayInfo delay, Packet fwdPacket) {
        this.delay = delay;
        this.forwardedPacket = fwdPacket;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
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

        if (delay != null)
            buf.append(delay.toXML());
        buf.append(forwardedPacket.toXML());

        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * get the packet forwarded by this stanza.
     *
     * @return the {@link Packet} instance (typically a message) that was forwarded.
     */
    public Packet getForwardedPacket() {
        return forwardedPacket;
    }

    /**
     * get the timestamp of the forwarded packet.
     *
     * @return the {@link DelayInfo} representing the time when the original packet was sent. May be null.
     */
    public DelayInfo getDelayInfo() {
        return delay;
    }

    public static class Provider implements PacketExtensionProvider {
        DelayInfoProvider dip = new DelayInfoProvider();

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            DelayInfo di = null;
            Packet packet = null;

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("delay"))
                        di = (DelayInfo)dip.parseExtension(parser);
                    else if (parser.getName().equals("message"))
                        packet = PacketParserUtils.parseMessage(parser);
                    else throw new Exception("Unsupported forwarded packet type: " + parser.getName());
                }
                else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(ELEMENT_NAME))
                    done = true;
            }
            if (packet == null)
                throw new Exception("forwarded extension must contain a packet");
            return new Forwarded(di, packet);
        }
    }
}
