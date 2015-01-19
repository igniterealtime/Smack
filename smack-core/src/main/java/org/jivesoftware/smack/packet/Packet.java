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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.PacketUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class for XMPP Stanzas, which are called packets in Smack.
 * <p>
 * Every packet has a unique ID (which is automatically
 * generated, but can be overridden). Optionally, the "to" and "from" fields can be set.
 * </p>
 * <p>
 * XMPP Stanzas are {@link Message}, {@link IQ} and {@link Presence}. Which therefore subclass this class.
 * </p>
 *
 * @author Matt Tucker
 */
public abstract class Packet implements TopLevelStreamElement {

    public static final String TEXT = "text";
    public static final String ITEM = "item";

    protected static final String DEFAULT_LANGUAGE =
            java.util.Locale.getDefault().getLanguage().toLowerCase(Locale.US);

    /**
     * A prefix helps to make sure that ID's are unique across multiple instances.
     */
    private static final String prefix = StringUtils.randomString(5) + "-";

    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique ID.
     */
    private static final AtomicLong id = new AtomicLong();

    private final Map<String, PacketExtension> packetExtensions = new LinkedHashMap<String, PacketExtension>(12);

    private String packetID = null;
    private String to = null;
    private String from = null;
    private XMPPError error = null;

    /**
     * Optional value of the 'xml:lang' attribute of the outermost element of
     * the stanza.
     * <p>
     * Such an attribute is defined for all stanza types. For IQ, see for
     * example XEP-50 3.7:
     * "The requester SHOULD provide its locale information using the "xml:lang
     * " attribute on either the <iq/> (RECOMMENDED) or <command/> element."
     * </p>
     */
    protected String language;

    public Packet() {
        this(prefix + Long.toString(id.incrementAndGet()));
    }

    public Packet(String packetID) {
        setPacketID(packetID);
    }

    public Packet(Packet p) {
        packetID = p.getPacketID();
        to = p.getTo();
        from = p.getFrom();
        error = p.error;

        // Copy extensions
        for (PacketExtension pe : p.getExtensions()) {
            addExtension(pe);
        }
    }

    /**
     * Returns the unique ID of the packet. The returned value could be <code>null</code>.
     *
     * @return the packet's unique ID or <code>null</code> if the packet's id is not available.
     */
    public String getPacketID() {
        return packetID;
    }

    /**
     * Sets the unique ID of the packet. To indicate that a packet has no id
     * pass <code>null</code> as the packet's id value.
     *
     * @param packetID the unique ID for the packet.
     */
    public void setPacketID(String packetID) {
        this.packetID = packetID;
    }

    /**
     * Returns who the packet is being sent "to", or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the packet is being sent to, or <tt>null</tt> if the
     *      value has not been set.
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets who the packet is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the packet is being sent to.
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Returns who the packet is being sent "from" or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the packet is being sent from, or <tt>null</tt> if the
     *      value has not been set.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets who the packet is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the packet is being sent to.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the error associated with this packet, or <tt>null</tt> if there are
     * no errors.
     *
     * @return the error sub-packet or <tt>null</tt> if there isn't an error.
     */
    public XMPPError getError() {
        return error;
    }

    /**
     * Sets the error for this packet.
     *
     * @param error the error to associate with this packet.
     */
    public void setError(XMPPError error) {
        this.error = error;
    }

    /**
     * Returns the xml:lang of this Stanza, or null if one has not been set.
     *
     * @return the xml:lang of this Stanza, or null.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the xml:lang of this Stanza.
     *
     * @param language the xml:lang of this Stanza.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Returns a copy of the packet extensions attached to the packet.
     *
     * @return the packet extensions.
     */
    public List<PacketExtension> getExtensions() {
        synchronized (packetExtensions) {
            if (packetExtensions.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<PacketExtension>(packetExtensions.values());
        }
    }

    /**
     * Returns the first extension of this packet that has the given namespace.
     * <p>
     * When possible, use {@link #getExtension(String,String)} instead.
     * </p>
     *
     * @param namespace the namespace of the extension that is desired.
     * @return the packet extension with the given namespace.
     */
    public PacketExtension getExtension(String namespace) {
        return PacketUtil.extensionElementFrom(getExtensions(), null, namespace);
    }

    /**
     * Returns the first packet extension that matches the specified element name and
     * namespace, or <tt>null</tt> if it doesn't exist. If the provided elementName is null,
     * only the namespace is matched. Packet extensions are
     * are arbitrary XML sub-documents in standard XMPP packets. By default, a 
     * DefaultPacketExtension instance will be returned for each extension. However, 
     * PacketExtensionProvider instances can be registered with the 
     * {@link org.jivesoftware.smack.provider.ProviderManager ProviderManager}
     * class to handle custom parsing. In that case, the type of the Object
     * will be determined by the provider.
     *
     * @param elementName the XML element name of the packet extension. (May be null)
     * @param namespace the XML element namespace of the packet extension.
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public <PE extends PacketExtension> PE getExtension(String elementName, String namespace) {
        if (namespace == null) {
            return null;
        }
        String key = XmppStringUtils.generateKey(elementName, namespace);
        PacketExtension packetExtension;
        synchronized (packetExtensions) {
            packetExtension = packetExtensions.get(key);
        }
        if (packetExtension == null) {
            return null;
        }
        return (PE) packetExtension;
    }

    /**
     * Adds a packet extension to the packet. Does nothing if extension is null.
     *
     * @param extension a packet extension.
     */
    public void addExtension(PacketExtension extension) {
        if (extension == null) return;
        String key = XmppStringUtils.generateKey(extension.getElementName(), extension.getNamespace());
        synchronized (packetExtensions) {
            packetExtensions.put(key, extension);
        }
    }

    /**
     * Adds a collection of packet extensions to the packet. Does nothing if extensions is null.
     * 
     * @param extensions a collection of packet extensions
     */
    public void addExtensions(Collection<PacketExtension> extensions) {
        if (extensions == null) return;
        for (PacketExtension packetExtension : extensions) {
            addExtension(packetExtension);
        }
    }

    /**
     * Check if a packet extension with the given element and namespace exists.
     * <p>
     * The argument <code>elementName</code> may be null.
     * </p>
     *
     * @param elementName
     * @param namespace
     * @return true if a packet extension exists, false otherwise.
     */
    public boolean hasExtension(String elementName, String namespace) {
        if (elementName == null) {
            return hasExtension(namespace);
        }
        String key = XmppStringUtils.generateKey(elementName, namespace);
        synchronized (packetExtensions) {
            return packetExtensions.containsKey(key);
        }
    }

    /**
     * Check if a packet extension with the given namespace exists.
     * 
     * @param namespace
     * @return true if a packet extension exists, false otherwise.
     */
    public boolean hasExtension(String namespace) {
        synchronized (packetExtensions) {
            for (PacketExtension packetExtension : packetExtensions.values()) {
                if (packetExtension.getNamespace().equals(namespace)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the packet extension with the given elementName and namespace.
     *
     * @param elementName
     * @param namespace
     * @return the removed packet extension or null.
     */
    public PacketExtension removeExtension(String elementName, String namespace) {
        String key = XmppStringUtils.generateKey(elementName, namespace);
        synchronized (packetExtensions) {
            return packetExtensions.remove(key);
        }
    }

    /**
     * Removes a packet extension from the packet.
     *
     * @param extension the packet extension to remove.
     * @return the removed packet extension or null.
     */
    public PacketExtension removeExtension(PacketExtension extension)  {
        return removeExtension(extension.getElementName(), extension.getNamespace());
    }

    @Override
    // NOTE When Smack is using Java 8, then this method should be moved in Element as "Default Method".
    public String toString() {
        return toXML().toString();
    }

    /**
     * Returns the extension sub-packets (including properties data) as an XML
     * String, or the Empty String if there are no packet extensions.
     *
     * @return the extension sub-packets as XML or the Empty String if there
     * are no packet extensions.
     */
    protected final XmlStringBuilder getExtensionsXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        // Add in all standard extension sub-packets.
        for (PacketExtension extension : getExtensions()) {
            xml.append(extension.toXML());
        }
        return xml;
    }

    /**
     * Returns the default language used for all messages containing localized content.
     * 
     * @return the default language
     */
    public static String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }

    /**
     * Add to, from, id and 'xml:lang' attributes
     *
     * @param xml
     */
    protected void addCommonAttributes(XmlStringBuilder xml) {
        xml.optAttribute("to", getTo());
        xml.optAttribute("from", getFrom());
        xml.optAttribute("id", getPacketID());
        xml.xmllangAttribute(getLanguage());
    }

    /**
     * Append an XMPPError is this packet has one set.
     *
     * @param xml the XmlStringBuilder to append the error to.
     */
    protected void appendErrorIfExists(XmlStringBuilder xml) {
        XMPPError error = getError();
        if (error != null) {
            xml.append(error.toXML());
        }
    }
}
