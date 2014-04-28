/**
 *
 * Copyright 2013 Georg Lukas
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
package org.jivesoftware.smackx.carbons.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.forward.Forwarded;

/**
 * Packet extension for XEP-0280: Message Carbons. The extension
 * <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> is
 * meant to synchronize a message flow to multiple presences of a user.
 * 
 * <p>
 * It accomplishes this by wrapping a {@link Forwarded} packet in a <b>sent</b>
 * or <b>received</b> element
 *
 * @author Georg Lukas
 */
public class CarbonExtension implements PacketExtension {
    public static final String NAMESPACE = "urn:xmpp:carbons:2";

    private Direction dir;
    private Forwarded fwd;

    /**
     * Construct a Carbon message extension.
     * 
     * @param dir Determines if the carbon is being sent/received
     * @param fwd The forwarded message.
     */
    public CarbonExtension(Direction dir, Forwarded fwd) {
        this.dir = dir;
        this.fwd = fwd;
    }

    /**
     * Get the direction (sent or received) of the carbon.
     *
     * @return the {@link Direction} of the carbon.
     */
    public Direction getDirection() {
        return dir;
    }

    /**
     * Get the forwarded packet.
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
     * Defines the direction of a {@link CarbonExtension} message.
     */
    public static enum Direction {
        received,
        sent
    }

    /**
     * Packet extension indicating that a message may not be carbon-copied.  Adding this
     * extension to any message will disallow that message from being copied. 
     */
    public static class Private implements PacketExtension {
        public static final String ELEMENT = "private";

        public String getElementName() {
            return ELEMENT;
        }

        public String getNamespace() {
            return CarbonExtension.NAMESPACE;
        }

        public String toXML() {
            return "<" + ELEMENT + " xmlns=\"" + CarbonExtension.NAMESPACE + "\"/>";
        }
    }
}
