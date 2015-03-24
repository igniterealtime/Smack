/**
 *
 * Copyright 2013-2014 Georg Lukas
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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.forward.packet.Forwarded;

/**
 * Stanza(/Packet) extension for XEP-0280: Message Carbons. The extension
 * <a href="http://xmpp.org/extensions/xep-0280.html">XEP-0280</a> is
 * meant to synchronize a message flow to multiple presences of a user.
 * 
 * <p>
 * It accomplishes this by wrapping a {@link Forwarded} stanza(/packet) in a <b>sent</b>
 * or <b>received</b> element
 *
 * @author Georg Lukas
 */
public class CarbonExtension implements ExtensionElement {
    public static final String NAMESPACE = Carbon.NAMESPACE;

    private final Direction dir;
    private final Forwarded fwd;

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
        return dir.name();
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.append(fwd.toXML());
        xml.closeElement(this);
        return xml;
    }

    /**
     * Obtain a Carbon from a message, if available.
     * <p>
     * Only {@link Message} instances can contain a Carbon extensions.
     * </p>
     *
     * @param msg Message object to check for carbons
     *
     * @return a Carbon if available, null otherwise.
     * @deprecated use {@link #from(Message)} instead
     */
    @Deprecated
    public static CarbonExtension getFrom(Message msg) {
        return from(msg);
    }

    /**
     * Obtain a Carbon from a message, if available.
     * <p>
     * Only {@link Message} instances can contain a Carbon extensions.
     * </p>
     *
     * @param msg Message object to check for carbons
     *
     * @return a Carbon if available, null otherwise.
     */
    public static CarbonExtension from(Message msg) {
        CarbonExtension cc = msg.getExtension(Direction.received.name(), NAMESPACE);
        if (cc == null)
            cc = msg.getExtension(Direction.sent.name(), NAMESPACE);
        return cc;
    }

    /**
     * Defines the direction of a {@link CarbonExtension} message.
     */
    public static enum Direction {
        received,
        sent
    }

    /**
     * Stanza(/Packet) extension indicating that a message may not be carbon-copied.  Adding this
     * extension to any message will disallow that message from being copied. 
     */
    public static class Private implements ExtensionElement {
        public static final Private INSTANCE = new Private();
        public static final String ELEMENT = "private";

        private Private() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String toXML() {
            return "<" + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
        }

        /**
         * Marks a message "private", so that it will not be carbon-copied, by adding private packet
         * extension to the message.
         * 
         * @param message the message to add the private extension to
         */
        public static void addTo(Message message) {
            message.addExtension(INSTANCE);
        }
    }
}
