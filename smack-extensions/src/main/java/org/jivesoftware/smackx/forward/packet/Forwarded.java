/**
 *
 * Copyright 2013-2014 Georg Lukas, 2020 Florian Schmaus
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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.delay.packet.DelayInformation;

/**
 * Stanza extension for XEP-0297: Stanza Forwarding.
 *
 * @author Georg Lukas
 * @see <a href="http://xmpp.org/extensions/xep-0297.html">XEP-0297: Stanza Forwarding</a>
 */
public class Forwarded<S extends Stanza> implements ExtensionElement {
    public static final String NAMESPACE = "urn:xmpp:forward:0";
    public static final String ELEMENT = "forwarded";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final DelayInformation delay;
    private final S forwardedStanza;

    /**
     * Creates a new Forwarded stanza extension.
     *
     * @param delay an optional {@link DelayInformation} timestamp of the packet.
     * @param forwardedStanza the stanza that is forwarded (required).
     * @deprecated use {@link #Forwarded(Stanza, DelayInformation)} instead.
     */
    @Deprecated
    public Forwarded(DelayInformation delay, S forwardedStanza) {
        this(forwardedStanza, delay);
    }

    /**
     * Creates a new Forwarded stanza extension.
     *
     * @param fwdPacket the stanza that is forwarded (required).
     */
    public Forwarded(S fwdPacket) {
        this(fwdPacket, null);
    }

    /**
     * Creates a new Forwarded stanza extension.
     *
     * @param forwardedStanza the stanza that is forwarded (required).
     * @param delay an optional {@link DelayInformation} timestamp of the packet.
     */
    public Forwarded(S forwardedStanza, DelayInformation delay) {
        this.forwardedStanza = Objects.requireNonNull(forwardedStanza);
        this.delay = delay;
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
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        xml.rightAngleBracket();
        xml.optElement(getDelayInformation());
        xml.append(forwardedStanza);
        xml.closeElement(this);
        return xml;
    }

    /**
     * Get the forwarded Stanza found in this extension.
     *
     * @return the {@link Stanza} (typically a message) that was forwarded.
     */
    public S getForwardedStanza() {
        return forwardedStanza;
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
     * Check if this is forwarding a stanza of the provided class.
     *
     * @param stanzaClass the class to check for.
     * @return <code>true</code> if this is forwarding a stanza of the provided class.
     * @since 4.4
     */
    public boolean isForwarded(Class<? extends Stanza> stanzaClass) {
        return stanzaClass.isAssignableFrom(forwardedStanza.getClass());
    }

    /**
     * Get the forwarded extension.
     * @param packet TODO javadoc me please
     * @return the Forwarded extension or null
     */
    public static Forwarded<?> from(Stanza packet) {
        return packet.getExtension(Forwarded.class);
    }

    /**
     * Extract messages in a collection of forwarded elements. Note that it is required that the {@link Forwarded} in
     * the given collection only contain {@link Message} stanzas.
     *
     * @param forwardedCollection the collection to extract from.
     * @return a list a the extracted messages.
     * @since 4.3.0
     */
    public static List<Message> extractMessagesFrom(Collection<Forwarded<Message>> forwardedCollection) {
        List<Message> res = new ArrayList<>(forwardedCollection.size());
        for (Forwarded<Message> forwarded : forwardedCollection) {
            Message message =  forwarded.getForwardedStanza();
            res.add(message);
        }
        return res;
    }
}
