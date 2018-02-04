/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.reference.element.ReferenceElement;

import org.jxmpp.jid.BareJid;

public final class ReferenceManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:reference:0";

    private static final Map<XMPPConnection, ReferenceManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private ReferenceManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
    }

    /**
     * Return a new instance of the ReferenceManager for the given connection.
     *
     * @param connection xmpp connection
     * @return reference manager instance
     */
    public static ReferenceManager getInstanceFor(XMPPConnection connection) {
        ReferenceManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new ReferenceManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Add a reference to another users bare jid to a stanza.
     *
     * @param stanza stanza.
     * @param begin start index of the mention in the messages body.
     * @param end end index of the mention in the messages body.
     * @param jid referenced jid.
     */
    public static void addMention(Stanza stanza, int begin, int end, BareJid jid) {
        ReferenceElement reference = new ReferenceElement(begin, end, ReferenceElement.Type.mention, null,
                "xmpp:" + jid.toString());
        stanza.addExtension(reference);
    }

    /**
     * Return a list of all reference extensions contained in a stanza.
     * If there are no reference elements, return an empty list.
     *
     * @param stanza stanza
     * @return list of all references contained in the stanza
     */
    public static List<ReferenceElement> getReferencesFromStanza(Stanza stanza) {
        List<ReferenceElement> references = new ArrayList<>();
        List<ExtensionElement> extensions = stanza.getExtensions(ReferenceElement.ELEMENT, NAMESPACE);
        for (ExtensionElement e : extensions) {
            references.add((ReferenceElement) e);
        }
        return references;
    }

    /**
     * Return true, if the stanza contains at least one reference extension.
     *
     * @param stanza stanza
     * @return true if stanza contains references
     */
    public static boolean containsReferences(Stanza stanza) {
        return getReferencesFromStanza(stanza).size() > 0;
    }
}
