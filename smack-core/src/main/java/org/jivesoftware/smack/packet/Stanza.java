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

import static org.jivesoftware.smack.util.StringUtils.requireNotNullNorEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.PacketUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.XmppElementUtil;

import org.jxmpp.jid.Jid;

/**
 * Base class for XMPP Stanzas, which are called Stanza in older versions of Smack (i.e. &lt; 4.1).
 * <p>
 * Every stanza has a unique ID (which is automatically generated, but can be overridden). Stanza
 * IDs are required for IQ stanzas and recommended for presence and message stanzas. Optionally, the
 * "to" and "from" fields can be set.
 * </p>
 * <p>
 * XMPP Stanzas are {@link Message}, {@link IQ} and {@link Presence}. Which therefore subclass this
 * class. <b>If you think you need to subclass this class, then you are doing something wrong.</b>
 * </p>
 * <p>
 * Use {@link StanzaBuilder} to construct a stanza instance. All instance mutating methods of this
 * class are deprecated, although not all of them are currently marked as such, and must not be used.
 * </p>
 *
 * @author Matt Tucker
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/rfcs/rfc6120.html#stanzas">RFC 6120 § 8. XML Stanzas</a>
 */
public abstract class Stanza implements StanzaView, TopLevelStreamElement {

    public static final String TEXT = "text";
    public static final String ITEM = "item";

    protected static final String DEFAULT_LANGUAGE =
            java.util.Locale.getDefault().getLanguage().toLowerCase(Locale.US);

    private final MultiMap<QName, ExtensionElement> extensionElements;

    // Assume that all stanzas Smack handles are in the client namespace, since Smack is an XMPP client library. We can
    // change this behavior later if it is required.
    private final String namespace = StreamOpen.CLIENT_NAMESPACE;

    private final StanzaIdSource usedStanzaIdSource;

    private String id = null;
    private Jid to;
    private Jid from;
    private StanzaError error = null;

    /**
     * Optional value of the 'xml:lang' attribute of the outermost element of
     * the stanza.
     * <p>
     * Such an attribute is defined for all stanza types. For IQ, see for
     * example XEP-50 3.7:
     * "The requester SHOULD provide its locale information using the "xml:lang
     * " attribute on either the &lt;iq/&gt; (RECOMMENDED) or &lt;command/&gt; element."
     * </p>
     */
    protected String language;

    protected Stanza() {
        extensionElements = new MultiMap<>();
        usedStanzaIdSource = null;
        id = StandardStanzaIdSource.DEFAULT.getNewStanzaId();
    }

    protected Stanza(StanzaBuilder<?> stanzaBuilder) {
        if (stanzaBuilder.stanzaIdSource != null) {
            id = stanzaBuilder.stanzaIdSource.getNewStanzaId();
            // Note that some stanza ID sources, e.g. StanzaBuilder.PresenceBuilder.EMPTY return null here. Hence we
            // only check that the returned string is not empty.
            assert StringUtils.isNullOrNotEmpty(id);
            usedStanzaIdSource = stanzaBuilder.stanzaIdSource;
        } else {
            // N.B. It is ok if stanzaId here is null.
            id = stanzaBuilder.stanzaId;
            usedStanzaIdSource = null;
        }

        to = stanzaBuilder.to;
        from = stanzaBuilder.from;

        error = stanzaBuilder.stanzaError;

        language = stanzaBuilder.language;

        extensionElements = stanzaBuilder.extensionElements.clone();
    }

    protected Stanza(Stanza p) {
        usedStanzaIdSource = p.usedStanzaIdSource;

        id = p.getStanzaId();
        to = p.getTo();
        from = p.getFrom();
        error = p.error;

        extensionElements = p.extensionElements.clone();
    }

    @Override
    public final String getStanzaId() {
        return id;
    }

    /**
     * Sets the unique ID of the packet. To indicate that a stanza has no id
     * pass <code>null</code> as the packet's id value.
     *
     * @param id the unique ID for the packet.
     */
    public void setStanzaId(String id) {
        if (id != null) {
            requireNotNullNorEmpty(id, "id must either be null or not the empty String");
        }
        this.id = id;
    }

    /**
     * Check if this stanza has an ID set.
     *
     * @return true if the stanza ID is set, false otherwise.
     * @since 4.1
     */
    public final boolean hasStanzaIdSet() {
        // setStanzaId ensures that the id is either null or not empty,
        // so we can assume that it is set if it's not null.
        return id != null;
    }

    /**
     * Set the stanza id if none is set.
     *
     * @return the stanza id.
     * @since 4.2
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public String setStanzaId() {
        if (!hasStanzaIdSet()) {
            setNewStanzaId();
        }
        return getStanzaId();
    }

    /**
     * Throws an {@link IllegalArgumentException} if this stanza has no stanza ID set.
     *
     * @throws IllegalArgumentException if this stanza has no stanza ID set.
     * @since 4.4.
     */
    public final void throwIfNoStanzaId() {
        if (hasStanzaIdSet()) {
            return;
        }

        throw new IllegalArgumentException("The stanza has no RFC stanza ID set, although one is required");
    }

    /**
     * Ensure that a stanza ID is set.
     *
     * @return the stanza ID.
     * @since 4.4
     */
    // TODO: Remove this method once StanzaBuilder is ready.
    protected String setNewStanzaId() {
        if (usedStanzaIdSource != null) {
            id = usedStanzaIdSource.getNewStanzaId();
        }
        else {
            id = StandardStanzaIdSource.DEFAULT.getNewStanzaId();
        }

        return getStanzaId();
    }

    @Override
    public final Jid getTo() {
        return to;
    }

    /**
     * Sets who the packet is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the packet is being sent to.
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public void setTo(Jid to) {
        this.to = to;
    }

    @Override
    public final Jid getFrom() {
        return from;
    }

    /**
     * Sets who the packet is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the packet is being sent to.
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public void setFrom(Jid from) {
        this.from = from;
    }

    @Override
    public final StanzaError getError() {
        return error;
    }

    /**
     * Sets the error for this stanza.
     *
     * @param stanzaError the error that this stanza carries and hence signals.
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public void setError(StanzaError stanzaError) {
        error = stanzaError;
    }

    /**
     * Deprecated.
     * @param stanzaError the stanza error.
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public void setError(StanzaError.Builder stanzaError) {
        setError(stanzaError.build());
    }

    @Override
    public final String getLanguage() {
        return language;
    }

    /**
     * Sets the xml:lang of this Stanza.
     *
     * @param language the xml:lang of this Stanza.
     * @deprecated use {@link StanzaBuilder#setLanguage(String)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public final List<ExtensionElement> getExtensions() {
        synchronized (extensionElements) {
            // No need to create a new list, values() will already create a new one for us
            return extensionElements.values();
        }
    }

    public final MultiMap<QName, ExtensionElement> getExtensionsMap() {
        return cloneExtensionsMap();
    }

    final MultiMap<QName, ExtensionElement> cloneExtensionsMap() {
        synchronized (extensionElements) {
            return extensionElements.clone();
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
     * @since 4.1
     */
    public final List<ExtensionElement> getExtensions(String elementName, String namespace) {
        requireNotNullNorEmpty(elementName, "elementName must not be null nor empty");
        requireNotNullNorEmpty(namespace, "namespace must not be null nor empty");
        QName key = new QName(namespace, elementName);
        return getExtensions(key);
    }

    @Override
    public final List<ExtensionElement> getExtensions(QName qname) {
        List<ExtensionElement> res;
        synchronized (extensionElements) {
            res = extensionElements.getAll(qname);
        }
        return Collections.unmodifiableList(res);
    }

    @Override
    public final <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass) {
        synchronized (extensionElements) {
            return XmppElementUtil.getElementsFrom(extensionElements, extensionElementClass);
        }
    }

    /**
     * Returns the first extension of this stanza that has the given namespace.
     * <p>
     * When possible, use {@link #getExtension(String, String)} instead.
     * </p>
     *
     * @param namespace the namespace of the extension that is desired.
     * @return the stanza extension with the given namespace.
     */
    public final ExtensionElement getExtension(String namespace) {
        return PacketUtil.extensionElementFrom(getExtensions(), null, namespace);
    }

    /**
     * Returns the first extension that matches the specified element name and
     * namespace, or <code>null</code> if it doesn't exist. If the provided elementName is null,
     * only the namespace is matched. Extensions are
     * are arbitrary XML elements in standard XMPP stanzas.
     *
     * @param elementName the XML element name of the extension. (May be null)
     * @param namespace the XML element namespace of the extension.
     * @return the extension, or <code>null</code> if it doesn't exist.
     */
    public final ExtensionElement getExtension(String elementName, String namespace) {
        if (namespace == null) {
            return null;
        }
        QName key = new QName(namespace, elementName);
        ExtensionElement packetExtension = getExtension(key);
        if (packetExtension == null) {
            return null;
        }
        return packetExtension;
    }

    @Override
    public final ExtensionElement getExtension(QName qname) {
        synchronized (extensionElements) {
            return extensionElements.getFirst(qname);
        }
    }

    /**
     * Adds a stanza extension to the packet. Does nothing if extension is null.
     * <p>
     * Please note that although this method is not yet marked as deprecated, it is recommended to use
     * {@link StanzaBuilder#addExtension(ExtensionElement)} instead.
     * </p>
     *
     * @param extension a stanza extension.
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public final void addExtension(ExtensionElement extension) {
        if (extension == null) return;
        QName key = extension.getQName();
        synchronized (extensionElements) {
            extensionElements.put(key, extension);
        }
    }

    /**
     * Add the given extension and override eventually existing extensions with the same name and
     * namespace.
     * <p>
     * Please note that although this method is not yet marked as deprecated, it is recommended to use
     * {@link StanzaBuilder#overrideExtension(ExtensionElement)} instead.
     * </p>
     *
     * @param extension the extension element to add.
     * @return one of the removed extensions or <code>null</code> if there are none.
     * @since 4.1.2
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public final ExtensionElement overrideExtension(ExtensionElement extension) {
        if (extension == null) return null;
        synchronized (extensionElements) {
            // Note that we need to use removeExtension(String, String) here. If would use
            // removeExtension(ExtensionElement) then we would remove based on the equality of ExtensionElement, which
            // is not what we want in this case.
            ExtensionElement removedExtension = removeExtension(extension.getElementName(), extension.getNamespace());
            addExtension(extension);
            return removedExtension;
        }
    }

    /**
     * Adds a collection of stanza extensions to the packet. Does nothing if extensions is null.
     *
     * @param extensions a collection of stanza extensions
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public final void addExtensions(Collection<ExtensionElement> extensions) {
        if (extensions == null) return;
        for (ExtensionElement packetExtension : extensions) {
            addExtension(packetExtension);
        }
    }

    /**
     * Check if a stanza extension with the given element and namespace exists.
     * <p>
     * The argument <code>elementName</code> may be null.
     * </p>
     *
     * @param elementName TODO javadoc me please
     * @param namespace TODO javadoc me please
     * @return true if a stanza extension exists, false otherwise.
     */
    public final boolean hasExtension(String elementName, String namespace) {
        if (elementName == null) {
            return hasExtension(namespace);
        }
        QName key = new QName(namespace, elementName);
        synchronized (extensionElements) {
            return extensionElements.containsKey(key);
        }
    }

    // Overridden in order to avoid an extra copy.
    @Override
    public final boolean hasExtension(String namespace) {
        synchronized (extensionElements) {
            for (ExtensionElement packetExtension : extensionElements.values()) {
                if (packetExtension.getNamespace().equals(namespace)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the stanza extension with the given elementName and namespace.
     *
     * @param elementName TODO javadoc me please
     * @param namespace TODO javadoc me please
     * @return the removed stanza extension or null.
     */
    // TODO: Mark this as deprecated once StanzaBuilder is ready and all call sites are gone.
    public final ExtensionElement removeExtension(String elementName, String namespace) {
        QName key = new QName(namespace, elementName);
        synchronized (extensionElements) {
            return extensionElements.remove(key);
        }
    }

    /**
     * Removes a stanza extension from the packet.
     *
     * @param extension the stanza extension to remove.
     * @return the removed stanza extension or null.
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public final ExtensionElement removeExtension(ExtensionElement extension)  {
        QName key = extension.getQName();
        synchronized (extensionElements) {
            List<ExtensionElement> list = extensionElements.getAll(key);
            boolean removed = list.remove(extension);
            if (removed) {
                return extension;
            }
        }
        return null;
    }

    /**
     * Returns a short String describing the Stanza. This method is suited for log purposes.
     */
    @Override
    public abstract String toString();

    @Override
    public final String getNamespace() {
        return namespace;
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
     * Add to, from, and id attributes.
     *
     * @param xml the {@link XmlStringBuilder}.
     */
    protected final void addCommonAttributes(XmlStringBuilder xml) {
        xml.optAttribute("to", getTo());
        xml.optAttribute("from", getFrom());
        xml.optAttribute("id", getStanzaId());
    }

    protected void logCommonAttributes(StringBuilder sb) {
        if (getTo() != null) {
            sb.append("to=").append(to).append(',');
        }
        if (getFrom() != null) {
            sb.append("from=").append(from).append(',');
        }
        if (hasStanzaIdSet()) {
            sb.append("id=").append(id).append(',');
        }
    }

    /**
     * Append an XMPPError is this stanza has one set.
     *
     * @param xml the XmlStringBuilder to append the error to.
     */
    protected final void appendErrorIfExists(XmlStringBuilder xml) {
        StanzaError error = getError();
        if (error != null) {
            xml.append(error);
        }
    }
}
