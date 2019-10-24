/**
 *
 * Copyright 2019 Florian Schmaus
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

    <E extends ExtensionElement> E getExtension(QName qname);

    @SuppressWarnings("unchecked")
    default <E extends ExtensionElement> E getExtension(Class<E> extensionElementClass) {
        QName qname = XmppElementUtil.getQNameFor(extensionElementClass);
        return (E) getExtension(qname);
    }

    /**
     * Returns a list of all extension elements of this stanza.
     *
     * @return a list of all extension elements of this stanza.
     */
    List<ExtensionElement> getExtensions();

    List<ExtensionElement> getExtensions(QName qname);

    <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass);
}
