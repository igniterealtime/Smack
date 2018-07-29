/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.element;

import static org.jivesoftware.smack.util.StringUtils.requireNotNullNorEmpty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.PacketUtil;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppDateTime;
import org.jxmpp.util.XmppStringUtils;

/**
 * This class describes an OpenPGP content element. It defines the elements and fields that OpenPGP content elements
 * do have in common.
 */
public abstract class OpenPgpContentElement implements ExtensionElement {

    public static final String ELEM_TO = "to";
    public static final String ATTR_JID = "jid";
    public static final String ELEM_TIME = "time";
    public static final String ATTR_STAMP = "stamp";
    public static final String ELEM_PAYLOAD = "payload";

    private final Set<Jid> to;
    private final Date timestamp;
    private final MultiMap<String, ExtensionElement> payload;

    private String timestampString;

    protected OpenPgpContentElement(Set<Jid> to, Date timestamp, List<ExtensionElement> payload) {
        this.to = to;
        this.timestamp = Objects.requireNonNull(timestamp);
        this.payload = new MultiMap<>();
        for (ExtensionElement e : payload) {
            this.payload.put(XmppStringUtils.generateKey(e.getElementName(), e.getNamespace()), e);
        }
    }

    /**
     * Return the set of recipients.
     *
     * @return recipients.
     */
    public final Set<Jid> getTo() {
        return to;
    }

    /**
     * Return the timestamp on which the encrypted element has been created.
     * This should be checked for sanity by the client.
     *
     * @return timestamp.
     */
    public final Date getTimestamp() {
        return timestamp;
    }

    /**
     * Return the payload of the message.
     *
     * @return payload.
     */
    public final List<ExtensionElement> getExtensions() {
        synchronized (payload) {
            return payload.values();
        }
    }

    /**
     * Return a list of all extensions with the given element name <em>and</em> namespace.
     * <p>
     * Changes to the returned set will update the stanza extensions, if the returned set is not the empty set.
     * </p>
     *
     * @param elementName the element name, must not be null.
     * @param namespace the namespace of the element(s), must not be null.
     * @return a set of all matching extensions.
     */
    public List<ExtensionElement> getExtensions(String elementName, String namespace) {
        requireNotNullNorEmpty(elementName, "elementName must not be null or empty");
        requireNotNullNorEmpty(namespace, "namespace must not be null or empty");
        String key = XmppStringUtils.generateKey(elementName, namespace);
        return payload.getAll(key);
    }

    /**
     * Returns the first extension of this stanza that has the given namespace.
     * <p>
     * When possible, use {@link #getExtension(String,String)} instead.
     * </p>
     *
     * @param namespace the namespace of the extension that is desired.
     * @return the stanza extension with the given namespace.
     */
    public ExtensionElement getExtension(String namespace) {
        return PacketUtil.extensionElementFrom(getExtensions(), null, namespace);
    }

    /**
     * Returns the first extension that matches the specified element name and
     * namespace, or <tt>null</tt> if it doesn't exist. If the provided elementName is null,
     * only the namespace is matched. Extensions are
     * are arbitrary XML elements in standard XMPP stanzas.
     *
     * @param elementName the XML element name of the extension. (May be null)
     * @param namespace the XML element namespace of the extension.
     * @param <PE> type of the ExtensionElement.
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public <PE extends ExtensionElement> PE getExtension(String elementName, String namespace) {
        if (namespace == null) {
            return null;
        }
        String key = XmppStringUtils.generateKey(elementName, namespace);
        ExtensionElement packetExtension;
        synchronized (payload) {
            packetExtension = payload.getFirst(key);
        }
        if (packetExtension == null) {
            return null;
        }
        return (PE) packetExtension;
    }


    @Override
    public String getNamespace() {
        return OpenPgpElement.NAMESPACE;
    }

    protected void ensureTimestampStringSet() {
        if (timestampString != null) return;

        timestampString = XmppDateTime.formatXEP0082Date(timestamp);
    }

    protected void addCommonXml(XmlStringBuilder xml) {
        for (Jid toJid : (to != null ? to : Collections.<Jid>emptySet())) {
            xml.halfOpenElement(ELEM_TO).attribute(ATTR_JID, toJid).closeEmptyElement();
        }

        ensureTimestampStringSet();
        xml.halfOpenElement(ELEM_TIME).attribute(ATTR_STAMP, timestampString).closeEmptyElement();

        xml.openElement(ELEM_PAYLOAD);
        for (ExtensionElement element : payload.values()) {
            xml.append(element.toXML(getNamespace()));
        }
        xml.closeElement(ELEM_PAYLOAD);
    }

    /**
     * Return a {@link ByteArrayInputStream} that reads the bytes of the XML representation of this element.
     *
     * @return InputStream over xml.
     */
    public InputStream toInputStream() {
        byte[] encoded = toXML(null).toString().getBytes(Charset.forName("UTF-8"));
        return new ByteArrayInputStream(encoded);
    }
}
