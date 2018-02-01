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
import java.util.UUID;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ToTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.sid.element.OriginIdElement;
import org.jivesoftware.smackx.sid.element.StanzaIdElement;

public final class StableUniqueStanzaIdManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:sid:0";

    private static final Map<XMPPConnection, StableUniqueStanzaIdManager> INSTANCES = new WeakHashMap<>();

    // Filter for outgoing stanzas.
    private static final StanzaFilter OUTGOING_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT_OR_HEADLINE,
            ToTypeFilter.ENTITY_FULL_OR_BARE_JID);

    // Listener for outgoing stanzas that adds origin-ids to outgoing stanzas.
    private final StanzaListener stanzaListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza packet) {
            addOriginId(packet);
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
    public static StableUniqueStanzaIdManager getInstanceFor(XMPPConnection connection) {
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
    public void enable() {
        ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection());
        if (disco.includesFeature(NAMESPACE)) {
            return;
        }
        disco.addFeature(NAMESPACE);
        connection().addPacketInterceptor(stanzaListener, OUTGOING_FILTER);
    }

    /**
     * Stop appending origin-id elements to outgoing stanzas and remove the feature from disco.
     */
    public void disable() {
        ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection());
        if (!disco.includesFeature(NAMESPACE)) {
            return;
        }
        ServiceDiscoveryManager.getInstanceFor(connection()).removeFeature(NAMESPACE);
        connection().removePacketInterceptor(stanzaListener);
    }

    /**
     * Return true, if we automatically append origin-id elements to outgoing stanzas.
     *
     * @return true if functionality is enabled, otherwise false.
     */
    public boolean isEnabled() {
        ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection());
        return disco.includesFeature(NAMESPACE);
    }

    /**
     * Create an origin-id element with a random UUID.
     *
     * @return origin-id element.
     */
    public static OriginIdElement createOriginId() {
        return new OriginIdElement(UUID.randomUUID().toString());
    }

    /**
     * Add an origin-id element to a stanza and set the stanzas id to the same id as in the origin-id element.
     *
     * @param stanza stanza.
     */
    public static void addOriginId(Stanza stanza) {
        OriginIdElement originId = createOriginId();
        stanza.addExtension(originId);
        stanza.setStanzaId(originId.getId());
    }

    /**
     * Return true, if a stanza contains a stanza-id element.
     *
     * @param stanza stanza
     * @return true if message contains stanza-id element, otherwise false.
     */
    public static boolean hasStanzaId(Stanza stanza) {
        return getStanzaId(stanza) != null;
    }

    /**
     * Return the stanza-id element of a stanza.
     *
     * @param stanza stanza
     * @return stanza-id element of a jid, or null if absent.
     */
    public static StanzaIdElement getStanzaId(Stanza stanza) {
        return stanza.getExtension(StanzaIdElement.ELEMENT, NAMESPACE);
    }

    /**
     * Return true, if the stanza contains a origin-id element.
     *
     * @param stanza stanza
     * @return true if the stanza contains a origin-id, false otherwise.
     */
    public static boolean hasOriginId(Stanza stanza) {
        return getOriginId(stanza) != null;
    }

    /**
     * Return the origin-id element of a stanza or null, if absent.
     *
     * @param stanza stanza
     * @return origin-id element
     */
    public static OriginIdElement getOriginId(Stanza stanza) {
        return stanza.getExtension(OriginIdElement.ELEMENT, NAMESPACE);
    }
}
