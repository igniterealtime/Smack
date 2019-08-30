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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.delay.packet.DelayInformation;

/**
 * Stanza extension for XEP-0297: Stanza Forwarding.
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
     * Creates a new Forwarded stanza extension.
     *
     * @param delay an optional {@link DelayInformation} timestamp of the packet.
     * @param fwdPacket the stanza that is forwarded (required).
     */
    public Forwarded(DelayInformation delay, Stanza fwdPacket) {
        this.delay = delay;
        this.forwardedPacket = fwdPacket;
    }

    /**
     * Creates a new Forwarded stanza extension.
     *
     * @param fwdPacket the stanza that is forwarded (required).
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
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement(getDelayInformation());
        xml.append(forwardedPacket.toXML(NAMESPACE));
        xml.closeElement(this);
        return xml;
    }

    /**
     * get the stanza forwarded by this stanza.
     *
     * @return the {@link Stanza} instance (typically a message) that was forwarded.
     * @deprecated use @{link {@link #getForwardedStanza()}} instead.
     */
    @Deprecated
    public Stanza getForwardedPacket() {
        return forwardedPacket;
    }

    /**
     * Get the forwarded Stanza found in this extension.
     *
     * @return the {@link Stanza} (typically a message) that was forwarded.
     */
    public Stanza getForwardedStanza() {
        return forwardedPacket;
    }

    /**
     * get the timestamp of the forwarded packet.
     *
     * @return the {@link DelayInformation} representing the time when the original stanza was sent. May be null.
     */
    public DelayInformation getDelayInformation() {
        return delay;
    }

    /**
     * Get the forwarded extension.
     * @param packet TODO javadoc me please
     * @return the Forwarded extension or null
     */
    public static Forwarded from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * Extract messages in a collection of forwarded elements. Note that it is required that the {@link Forwarded} in
     * the given collection only contain {@link Message} stanzas.
     *
     * @param forwardedCollection the collection to extract from.
     * @return a list a the extracted messages.
     * @since 4.3.0
     */
    public static List<Message> extractMessagesFrom(Collection<Forwarded> forwardedCollection) {
        List<Message> res = new ArrayList<>(forwardedCollection.size());
        for (Forwarded forwarded : forwardedCollection) {
            Message message =  (Message) forwarded.forwardedPacket;
            res.add(message);
        }
        return res;
    }
}
