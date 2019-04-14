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
package org.jivesoftware.smackx.sid;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ToTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

public final class StableUniqueStanzaIdManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:sid:0";

    private static final Map<XMPPConnection, StableUniqueStanzaIdManager> INSTANCES = new WeakHashMap<>();

    // Filter for outgoing stanzas.
    private static final StanzaFilter OUTGOING_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT_OR_HEADLINE,
            ToTypeFilter.ENTITY_FULL_OR_BARE_JID);

    // Listener for outgoing stanzas that adds origin-ids to outgoing stanzas.
    private static final StanzaListener ADD_ORIGIN_ID_INTERCEPTOR = new StanzaListener() {
        @Override
        public void processStanza(Stanza stanza) {
            Message message = (Message) stanza;
            OriginIdElement.addOriginId(message);
        }
    };

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Private constructor.
     * @param connection
     */
    private StableUniqueStanzaIdManager(XMPPConnection connection) {
        super(connection);
        enable();
    }

    /**
     * Return an instance of the StableUniqueStanzaIdManager for the given connection.
     *
     * @param connection xmpp-connection
     * @return manager instance for the connection
     */
    public static synchronized StableUniqueStanzaIdManager getInstanceFor(XMPPConnection connection) {
        StableUniqueStanzaIdManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new StableUniqueStanzaIdManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Start appending origin-id elements to outgoing stanzas and add the feature to disco.
     */
    public synchronized void enable() {
        ServiceDiscoveryManager.getInstanceFor(connection()).addFeature(NAMESPACE);
        StanzaFilter filter = new AndFilter(OUTGOING_FILTER, new NotFilter(OUTGOING_FILTER));
        connection().addStanzaInterceptor(ADD_ORIGIN_ID_INTERCEPTOR, filter);
    }

    /**
     * Stop appending origin-id elements to outgoing stanzas and remove the feature from disco.
     */
    public synchronized void disable() {
        ServiceDiscoveryManager.getInstanceFor(connection()).removeFeature(NAMESPACE);
        connection().removeStanzaInterceptor(ADD_ORIGIN_ID_INTERCEPTOR);
    }

    /**
     * Return true, if we automatically append origin-id elements to outgoing stanzas.
     *
     * @return true if functionality is enabled, otherwise false.
     */
    public synchronized boolean isEnabled() {
        ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection());
        return disco.includesFeature(NAMESPACE);
    }
}
