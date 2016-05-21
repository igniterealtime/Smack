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

import static org.jivesoftware.smack.util.StringUtils.requireNotNullOrEmpty;

import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.PacketUtil;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Base class for XMPP Stanzas, which are called Stanza(/Packet) in older versions of Smack (i.e. &lt; 4.1).
 * <p>
 * Every stanza has a unique ID (which is automatically generated, but can be overridden). Stanza
 * IDs are required for IQ stanzas and recommended for presence and message stanzas. Optionally, the
 * "to" and "from" fields can be set.
 * </p>
 * <p>
 * XMPP Stanzas are {@link Message}, {@link IQ} and {@link Presence}. Which therefore subclass this
 * class. <b>If you think you need to subclass this class, then you are doing something wrong.</b>
 * </p>
 *
 * @author Matt Tucker
 * @see <a href="http://xmpp.org/rfcs/rfc6120.html#stanzas">RFC 6120 § 8. XML Stanzas</a>
 */
public abstract class Stanza implements TopLevelStreamElement {

    public static final String TEXT = "text";
    public static final String ITEM = "item";
    public static final Set<String> REGISTERED_ATTRIBUTES = new HashSet<String>(Arrays.asList("xml:lang", "id", "to", "from", "type"));

    protected static final String DEFAULT_LANGUAGE =
            java.util.Locale.getDefault().getLanguage().toLowerCase(Locale.US);

    private static boolean customAttributesEnabled = false;

    private final MultiMap<String, ExtensionElement> packetExtensions = new MultiMap<>();

    private String id = null;
    private Jid to;
    private Jid from;
    private XMPPError error = null;
    private LinkedHashMap<String, String> customAttributes;

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

    protected Stanza() {
        this(StanzaIdUtil.newStanzaId());
    }

    protected Stanza(String stanzaId) {
        setStanzaId(stanzaId);
    }

    protected Stanza(Stanza p) {
        id = p.getStanzaId();
        to = p.getTo();
        from = p.getFrom();
        error = p.error;

        // Copy extensions
        for (ExtensionElement pe : p.getExtensions()) {
            addExtension(pe);
        }

        if (p.customAttributes != null) {
            customAttributes = new LinkedHashMap<>(p.customAttributes);
        }
    }

    /**
     * Returns the unique ID of the stanza. The returned value could be <code>null</code>.
     *
     * @return the packet's unique ID or <code>null</code> if the id is not available.
     */
    public String getStanzaId() {
        return id;
    }

    /**
     * Get the Stanza ID.
     * @return the stanza id.
     * @deprecated use {@link #getStanzaId()} instead.
     */
    @Deprecated
    public String getPacketID() {
        return getStanzaId();
    }

    /**
     * Sets the unique ID of the packet. To indicate that a stanza(/packet) has no id
     * pass <code>null</code> as the packet's id value.
     *
     * @param id the unique ID for the packet.
     */
    public void setStanzaId(String id) {
        if (id != null) {
            requireNotNullOrEmpty(id, "id must either be null or not the empty String");
        }
        this.id = id;
    }

    /**
     * Set the stanza ID.
     * @param packetID
     * @deprecated use {@link #setStanzaId(String)} instead.
     */
    @Deprecated
    public void setPacketID(String packetID) {
        setStanzaId(packetID);
    }

    /**
     * Check if this stanza has an ID set.
     *
     * @return true if the stanza ID is set, false otherwise.
     * @since 4.1
     */
    public boolean hasStanzaIdSet() {
        // setStanzaId ensures that the id is either null or not empty,
        // so we can assume that it is set if it's not null.
        return id != null;
    }

    /**
     * Returns who the stanza(/packet) is being sent "to", or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza(/packet) is being sent to, or <tt>null</tt> if the
     *      value has not been set.
     */
    public Jid getTo() {
        return to;
    }

    /**
     * Sets who the stanza(/packet) is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the stanza(/packet) is being sent to.
     * @throws IllegalArgumentException if to is not a valid JID String.
     * @deprecated use {@link #setTo(Jid)} instead.
     */
    @Deprecated
    public void setTo(String to) {
        Jid jid;
        try {
            jid = JidCreate.from(to);
        }
        catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }
        setTo(jid);
    }

    /**
     * Sets who the packet is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the packet is being sent to.
     */
    public void setTo(Jid to) {
        this.to = to;
    }

    /**
     * Returns who the stanza(/packet) is being sent "from" or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza(/packet) is being sent from, or <tt>null</tt> if the
     *      value has not been set.
     */
    public Jid getFrom() {
        return from;
    }

    /**
     * Sets who the stanza(/packet) is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the stanza(/packet) is being sent to.
     * @throws IllegalArgumentException if from is not a valid JID String.
     * @deprecated use {@link #setFrom(Jid)} instead.
     */
    @Deprecated
    public void setFrom(String from) {
        Jid jid;
        try {
            jid = JidCreate.from(from);
        }
        catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }
        setFrom(jid);
    }

    /**
     * Sets who the packet is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the packet is being sent to.
     */
    public void setFrom(Jid from) {
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
     * @deprecated use {@link #setError(org.jivesoftware.smack.packet.XMPPError.Builder)} instead.
     */
    @Deprecated
    public void setError(XMPPError error) {
        this.error = error;
    }

    /**
     * Sets the error for this stanza.
     *
     * @param xmppErrorBuilder the error to associate with this stanza.
     */
    public void setError(XMPPError.Builder xmppErrorBuilder) {
        if (xmppErrorBuilder == null) {
            return;
        }
        xmppErrorBuilder.setStanza(this);
        error = xmppErrorBuilder.build();
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
     * Returns a list of all extension elements of this stanza.
     *
     * @return a list of all extension elements of this stanza.
     */
    public List<ExtensionElement> getExtensions() {
        synchronized (packetExtensions) {
            // No need to create a new list, values() will already create a new one for us
            return packetExtensions.values();
        }
    }

    /**
     * Return a list of all extensions with the given element name <em>and</em> namespace.
     * <p>
     * Changes to the returned set will update the stanza(/packet) extensions, if the returned set is not the empty set.
     * </p>
     *
     * @param elementName the element name, must not be null.
     * @param namespace the namespace of the element(s), must not be null.
     * @return a set of all matching extensions.
     * @since 4.1
     */
    public List<ExtensionElement> getExtensions(String elementName, String namespace) {
        requireNotNullOrEmpty(elementName, "elementName must not be null or empty");
        requireNotNullOrEmpty(namespace, "namespace must not be null or empty");
        String key = XmppStringUtils.generateKey(elementName, namespace);
        return packetExtensions.getAll(key);
    }

    /**
     * Returns the first extension of this stanza(/packet) that has the given namespace.
     * <p>
     * When possible, use {@link #getExtension(String,String)} instead.
     * </p>
     *
     * @param namespace the namespace of the extension that is desired.
     * @return the stanza(/packet) extension with the given namespace.
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
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public <PE extends ExtensionElement> PE getExtension(String elementName, String namespace) {
        if (namespace == null) {
            return null;
        }
        String key = XmppStringUtils.generateKey(elementName, namespace);
        ExtensionElement packetExtension;
        synchronized (packetExtensions) {
            packetExtension = packetExtensions.getFirst(key);
        }
        if (packetExtension == null) {
            return null;
        }
        return (PE) packetExtension;
    }

    /**
     * Adds a stanza(/packet) extension to the packet. Does nothing if extension is null.
     *
     * @param extension a stanza(/packet) extension.
     */
    public void addExtension(ExtensionElement extension) {
        if (extension == null) return;
        String key = XmppStringUtils.generateKey(extension.getElementName(), extension.getNamespace());
        synchronized (packetExtensions) {
            packetExtensions.put(key, extension);
        }
    }

    /**
     * Add the given extension and override eventually existing extensions with the same name and
     * namespace.
     *
     * @param extension the extension element to add.
     * @return one of the removed extensions or <code>null</code> if there are none.
     * @since 4.1.2
     */
    public ExtensionElement overrideExtension(ExtensionElement extension) {
        if (extension == null) return null;
        synchronized (packetExtensions) {
            ExtensionElement removedExtension = removeExtension(extension);
            addExtension(extension);
            return removedExtension;
        }
    }

    /**
     * Adds a collection of stanza(/packet) extensions to the packet. Does nothing if extensions is null.
     * 
     * @param extensions a collection of stanza(/packet) extensions
     */
    public void addExtensions(Collection<ExtensionElement> extensions) {
        if (extensions == null) return;
        for (ExtensionElement packetExtension : extensions) {
            addExtension(packetExtension);
        }
    }

    /**
     * Check if a stanza(/packet) extension with the given element and namespace exists.
     * <p>
     * The argument <code>elementName</code> may be null.
     * </p>
     *
     * @param elementName
     * @param namespace
     * @return true if a stanza(/packet) extension exists, false otherwise.
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
     * Check if a stanza(/packet) extension with the given namespace exists.
     * 
     * @param namespace
     * @return true if a stanza(/packet) extension exists, false otherwise.
     */
    public boolean hasExtension(String namespace) {
        synchronized (packetExtensions) {
            for (ExtensionElement packetExtension : packetExtensions.values()) {
                if (packetExtension.getNamespace().equals(namespace)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the stanza(/packet) extension with the given elementName and namespace.
     *
     * @param elementName
     * @param namespace
     * @return the removed stanza(/packet) extension or null.
     */
    public ExtensionElement removeExtension(String elementName, String namespace) {
        String key = XmppStringUtils.generateKey(elementName, namespace);
        synchronized (packetExtensions) {
            return packetExtensions.remove(key);
        }
    }

    /**
     * Removes a stanza(/packet) extension from the packet.
     *
     * @param extension the stanza(/packet) extension to remove.
     * @return the removed stanza(/packet) extension or null.
     */
    public ExtensionElement removeExtension(ExtensionElement extension)  {
        return removeExtension(extension.getElementName(), extension.getNamespace());
    }

    /**
     * Returns <tt>true</tt> if custom attributes management is enabled.
     * @return <tt>true</tt> if custom attributes management is enabled.
     */
    public static boolean customAttributesEnabled() {
        return customAttributesEnabled;
    }

    /**
     * Enables the possibility to add custom attributes to the stanza.
     * <br/><b>Enabling this feature breaks the XMPP standard and may induce unexpected behaviors!
     * <br/>If your use-case allow it, prefer using ExtensionElement instead, and if not, be sure to triple-check the result.</b>
     */
    public static void enableCustomAttributes() {
        customAttributesEnabled = true;
    }

    private static void requireCustomAttributesEnabled() {
        if (!customAttributesEnabled()) {
            throw new IllegalStateException(
                    "You need to enable this feature first by calling enableCustomAttributes. " +
                    "Ensure you are aware of the consequences of doing so.");
        }
    }

    private void requireCustomAttributes() {
        if (customAttributes == null) {
            customAttributes = new LinkedHashMap<>();
        }
    }

    private void failIfRegistered(String attributeName) {
        if (REGISTERED_ATTRIBUTES.contains(attributeName)) {
            throw new IllegalArgumentException(attributeName + " is a registered attribute.");
        }
    }

    /**
     * Replaces all custom attributes of the stanza.
     * @param attributes a map (name/value) of all the custom attributes of this stanza. Should not be {@null}
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public void setCustomAttributes(Map<String, String> attributes) {
        requireCustomAttributesEnabled();
        assert attributes != null;
        for (String attr : attributes.keySet()) {
            failIfRegistered(attr);
        }
        requireCustomAttributes();

        customAttributes.clear();
        customAttributes.putAll(attributes);
    }

    /**
     * Adds a custom attribute to the stanza. If a custom attribute with the same name is already present, its value is updated.
     * @param attributeName the custom attribute name.
     * @param attributeValue the custom attribute value.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public void setCustomAttribute(String attributeName, String attributeValue) {
        requireCustomAttributesEnabled();
        requireNotNullOrEmpty(attributeName, "attributeName must not be null or empty");
        failIfRegistered(attributeName);
        requireCustomAttributes();

        customAttributes.put(attributeName, attributeValue);
    }

    /**
     * Removes all custom attributes of the stanza.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public void removeCustomAttributes() {
        requireCustomAttributesEnabled();
        requireCustomAttributes();

        customAttributes.clear();
    }

    /**
     * Removes a custom attribute of the stanza if it exists.
     * @param attributeName the custom attribute name.
     * @return the removed custom attribute value or <tt>null</tt> if is was not present.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public String removeCustomAttribute(String attributeName) {
        requireCustomAttributesEnabled();
        requireNotNullOrEmpty(attributeName, "attributeName must not be null or empty");
        requireCustomAttributes();

        return customAttributes.remove(attributeName);
    }

    /**
     * Returns <tt>true</tt> if this stanza contains custom attributes.
     * @return <tt>true</tt> if this stanza contains custom attributes.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public boolean hasCustomAttributes() {
        requireCustomAttributesEnabled();
        requireCustomAttributes();

        return !customAttributes.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this stanza contains a custom attribute with this name.
     * @param attributeName the custom attribute name.
     * @return <tt>true</tt> if this stanza contains a custom attribute with this name.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public boolean hasCustomAttribute(String attributeName) {
        requireCustomAttributesEnabled();
        requireNotNullOrEmpty(attributeName, "attributeName must not be null or empty");
        requireCustomAttributes();

        return customAttributes.containsKey(attributeName);
    }

    /**
     * Returns a map (name/value) of all the custom attributes of this stanza.
     * @return an unmodifiable map (name/value) of all the custom attributes of this stanza.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public Map<String, String> getCustomAttributes() {
        requireCustomAttributesEnabled();
        requireCustomAttributes();

        return Collections.unmodifiableMap(customAttributes);
    }

    /**
     * Returns the value of a custom attribute, or {@code null} if this stanza does not contain any custom attribute with this name.
     * @param attributeName the custom attribute name.
     * @return the value of a custom attribute, or {@code null} if this stanza does not contain any custom attribute with this name.
     * @throws IllegalStateException if custom attributes management is not enabled.
     */
    public String getCustomAttribute(String attributeName) {
        requireCustomAttributesEnabled();
        requireNotNullOrEmpty(attributeName, "attributeName must not be null or empty");
        requireCustomAttributes();

        return customAttributes.get(attributeName);
    }

    @Override
    // NOTE When Smack is using Java 8, then this method should be moved in Element as "Default Method".
    public String toString() {
        return toXML().toString();
    }

    /**
     * Returns the extension sub-packets (including properties data) as an XML
     * String, or the Empty String if there are no stanza(/packet) extensions.
     *
     * @return the extension sub-packets as XML or the Empty String if there
     * are no stanza(/packet) extensions.
     */
    protected final XmlStringBuilder getExtensionsXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        // Add in all standard extension sub-packets.
        for (ExtensionElement extension : getExtensions()) {
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
    protected void addAttributes(XmlStringBuilder xml) {
        xml.optAttribute("to", getTo());
        xml.optAttribute("from", getFrom());
        xml.optAttribute("id", getStanzaId());
        xml.xmllangAttribute(getLanguage());

        if(customAttributes == null) {
            return;
        }

        for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
            xml.attribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Append an XMPPError is this stanza(/packet) has one set.
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
