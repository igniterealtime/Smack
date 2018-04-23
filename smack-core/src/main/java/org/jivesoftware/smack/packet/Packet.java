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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Deprecated interface of pre Smack 4.1 Stanzas.
 * @deprecated use {@link Stanza} instead
 */
@Deprecated
public interface Packet extends TopLevelStreamElement {

    String TEXT = "text";
    String ITEM = "item";

    /**
     * Returns the unique ID of the stanza. The returned value could be <code>null</code>.
     *
     * @return the packet's unique ID or <code>null</code> if the id is not available.
     */
    String getStanzaId();

    /**
     * Get the stanza id.
     * @return the stanza id.
     * @deprecated use {@link #getStanzaId()} instead.
     */
    @Deprecated
    String getPacketID();

    /**
     * Sets the unique ID of the packet. To indicate that a stanza has no id
     * pass <code>null</code> as the packet's id value.
     *
     * @param id the unique ID for the packet.
     */
    void setStanzaId(String id);

    /**
     * Set the stanza ID.
     * @param packetID
     * @deprecated use {@link #setStanzaId(String)} instead.
     */
    @Deprecated
    void setPacketID(String packetID);

    /**
     * Returns who the stanza is being sent "to", or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza is being sent to, or <tt>null</tt> if the
     *      value has not been set.
     */
    String getTo();

    /**
     * Sets who the stanza is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the stanza is being sent to.
     */
    void setTo(String to);

    /**
     * Returns who the stanza is being sent "from" or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza is being sent from, or <tt>null</tt> if the
     *      value has not been set.
     */
    String getFrom();

    /**
     * Sets who the stanza is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the stanza is being sent to.
     */
    void setFrom(String from);

    /**
     * Returns the error associated with this packet, or <tt>null</tt> if there are
     * no errors.
     *
     * @return the error sub-packet or <tt>null</tt> if there isn't an error.
     */
    StanzaError getError();
    /**
     * Sets the error for this packet.
     *
     * @param error the error to associate with this packet.
     */
    void setError(StanzaError error);

    /**
     * Returns the xml:lang of this Stanza, or null if one has not been set.
     *
     * @return the xml:lang of this Stanza, or null.
     */
    String getLanguage();

    /**
     * Sets the xml:lang of this Stanza.
     *
     * @param language the xml:lang of this Stanza.
     */
    void setLanguage(String language);

    /**
     * Returns a copy of the stanza extensions attached to the packet.
     *
     * @return the stanza extensions.
     */
    List<ExtensionElement> getExtensions();

    /**
     * Return a set of all extensions with the given element name <i>and</i> namespace.
     * <p>
     * Changes to the returned set will update the stanza extensions, if the returned set is not the empty set.
     * </p>
     *
     * @param elementName the element name, must not be null.
     * @param namespace the namespace of the element(s), must not be null.
     * @return a set of all matching extensions.
     * @since 4.1
     */
    Set<ExtensionElement> getExtensions(String elementName, String namespace);

    /**
     * Returns the first extension of this stanza that has the given namespace.
     * <p>
     * When possible, use {@link #getExtension(String,String)} instead.
     * </p>
     *
     * @param namespace the namespace of the extension that is desired.
     * @return the stanza extension with the given namespace.
     */
    ExtensionElement getExtension(String namespace);

    /**
     * Returns the first stanza extension that matches the specified element name and
     * namespace, or <tt>null</tt> if it doesn't exist. If the provided elementName is null,
     * only the namespace is matched. Stanza extensions are
     * are arbitrary XML sub-documents in standard XMPP packets. By default, a 
     * DefaultPacketExtension instance will be returned for each extension. However, 
     * PacketExtensionProvider instances can be registered with the 
     * {@link org.jivesoftware.smack.provider.ProviderManager ProviderManager}
     * class to handle custom parsing. In that case, the type of the Object
     * will be determined by the provider.
     *
     * @param elementName the XML element name of the stanza extension. (May be null)
     * @param namespace the XML element namespace of the stanza extension.
     * @param <PE> type of the ExtensionElement.
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    <PE extends ExtensionElement> PE getExtension(String elementName, String namespace);
    /**
     * Adds a stanza extension to the packet. Does nothing if extension is null.
     *
     * @param extension a stanza extension.
     */
    void addExtension(ExtensionElement extension);

    /**
     * Adds a collection of stanza extensions to the packet. Does nothing if extensions is null.
     * 
     * @param extensions a collection of stanza extensions
     */
    void addExtensions(Collection<ExtensionElement> extensions);

    /**
     * Check if a stanza extension with the given element and namespace exists.
     * <p>
     * The argument <code>elementName</code> may be null.
     * </p>
     *
     * @param elementName
     * @param namespace
     * @return true if a stanza extension exists, false otherwise.
     */
    boolean hasExtension(String elementName, String namespace);

    /**
     * Check if a stanza extension with the given namespace exists.
     * 
     * @param namespace
     * @return true if a stanza extension exists, false otherwise.
     */
    boolean hasExtension(String namespace);

    /**
     * Remove the stanza extension with the given elementName and namespace.
     *
     * @param elementName
     * @param namespace
     * @return the removed stanza extension or null.
     */
    ExtensionElement removeExtension(String elementName, String namespace);

    /**
     * Removes a stanza extension from the packet.
     *
     * @param extension the stanza extension to remove.
     * @return the removed stanza extension or null.
     */
    ExtensionElement removeExtension(ExtensionElement extension);

    @Override
    // NOTE When Smack is using Java 8, then this method should be moved in Element as "Default Method".
    String toString();

}
