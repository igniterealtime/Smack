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
package org.jivesoftware.smackx.forward.packet;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

/**
 * Stanza(/Packet) extension for >XEP-0297: Stanza Forwarding.
 * 
 * @author Georg Lukas
 * @see <a href="http://xmpp.org/extensions/xep-0297.html">XEP-0297: Stanza Forwarding</a>
 */
public class Forwarded implements ExtensionElement {
    public static final String NAMESPACE = "urn:xmpp:forward:0";
    public static final String ELEMENT = "forwarded";

    private final DelayInformation delay;
    private final Stanza forwardedPacket;

    /**
     * Creates a new Forwarded stanza(/packet) extension.
     *
     * @param delay an optional {@link DelayInformation} timestamp of the packet.
     * @param fwdPacket the stanza(/packet) that is forwarded (required).
     */
    public Forwarded(DelayInformation delay, Stanza fwdPacket) {
        this.delay = delay;
        this.forwardedPacket = fwdPacket;
    }

    /**
     * Creates a new Forwarded stanza(/packet) extension.
     *
     * @param fwdPacket the stanza(/packet) that is forwarded (required).
     */
    public Forwarded(Stanza fwdPacket) {
        this(null, fwdPacket);
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
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement(getDelayInformation());
        xml.append(forwardedPacket.toXML());
        xml.closeElement(this);
        return xml;
    }

    /**
     * get the stanza(/packet) forwarded by this stanza.
     *
     * @return the {@link Stanza} instance (typically a message) that was forwarded.
     */
    public Stanza getForwardedPacket() {
        return forwardedPacket;
    }

    /**
     * get the timestamp of the forwarded packet.
     *
     * @return the {@link DelayInformation} representing the time when the original stanza(/packet) was sent. May be null.
     */
    public DelayInformation getDelayInformation() {
        return delay;
    }

    /**
     * 
     * @param packet
     * @return the Forwarded extension or null
     */
    public static Forwarded from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }
}
