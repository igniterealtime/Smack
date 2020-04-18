/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.util.XmppElementUtil;

import org.jxmpp.jid.Jid;

public interface StanzaView extends XmlLangElement {

    /**
     * Returns the unique ID of the stanza. The returned value could be <code>null</code>.
     *
     * @return the packet's unique ID or <code>null</code> if the id is not available.
     */
    String getStanzaId();

    /**
     * Returns who the stanza is being sent "to", or <code>null</code> if
     * the value is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza is being sent to, or <code>null</code> if the
     *      value has not been set.
     */
    Jid getTo();

    /**
     * Returns who the stanza is being sent "from" or <code>null</code> if
     * the value is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.<p>
     *
     * @return who the stanza is being sent from, or <code>null</code> if the
     *      value has not been set.
     */
    Jid getFrom();

    /**
     * Returns the error associated with this packet, or <code>null</code> if there are
     * no errors.
     *
     * @return the error sub-packet or <code>null</code> if there isn't an error.
     */
    StanzaError getError();

    ExtensionElement getExtension(QName qname);

    default boolean hasExtension(QName qname) {
        return getExtension(qname) != null;
    }

    default boolean hasExtension(Class<? extends ExtensionElement> extensionElementClass) {
        return getExtension(extensionElementClass) != null;
    }

    /**
     * Check if a extension element with the given namespace exists.
     *
     * @param namespace the namespace of the extension element to check for.
     * @return true if a stanza extension exists, false otherwise.
     */
    default boolean hasExtension(String namespace) {
        for (ExtensionElement packetExtension : getExtensions()) {
            if (packetExtension.getNamespace().equals(namespace)) {
                return true;
            }
        }

        return false;
    }

    default <E extends ExtensionElement> E getExtension(Class<E> extensionElementClass) {
        QName qname = XmppElementUtil.getQNameFor(extensionElementClass);
        ExtensionElement extensionElement = getExtension(qname);
        if (!extensionElementClass.isInstance(extensionElement)) {
            return null;
        }

        return extensionElementClass.cast(extensionElement);
    }

    /**
     * Returns a list of all extension elements of this stanza.
     *
     * @return a list of all extension elements of this stanza.
     */
    List<ExtensionElement> getExtensions();

    List<ExtensionElement> getExtensions(QName qname);

    /**
     * Return all extension elements of the given type. Returns the empty list if there a none.
     *
     * @param <E> the type of extension elements.
     * @param extensionElementClass the class of the type of extension elements.
     * @return a list of extension elements of that type, which may be empty.
     */
    <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass);
}
