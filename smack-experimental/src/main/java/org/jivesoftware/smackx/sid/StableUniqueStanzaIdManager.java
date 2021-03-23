/**
 *
 * Copyright 2018 Paul Schaub, 2020 Florian Schmaus
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
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ToTypeFilter;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

/**
 * Manager class for Stable and Unique Stanza IDs.
 *
 * In order to start automatically appending origin ids to outgoing messages, use {@link #enable()}.
 * This will announce support via the {@link ServiceDiscoveryManager}. If you want to stop appending origin-ids
 * and de-announce support, call {@link #disable()}.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0359.html">XEP-0359: Stable and Unique Stanza IDs</a>
 */
public final class StableUniqueStanzaIdManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:sid:0";

    private static final Map<XMPPConnection, StableUniqueStanzaIdManager> INSTANCES = new WeakHashMap<>();

    private static boolean enabledByDefault = false;

    // Filter for outgoing stanzas.
    private static final StanzaFilter OUTGOING_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT_OR_HEADLINE,
            ToTypeFilter.ENTITY_FULL_OR_BARE_JID);

    // Filter that filters for messages with an origin id
    private static final StanzaFilter ORIGIN_ID_FILTER = new StanzaExtensionFilter(OriginIdElement.ELEMENT, NAMESPACE);

    // We need a filter for outgoing messages that do not carry an origin-id already.
    private static final StanzaFilter ADD_ORIGIN_ID_FILTER = new AndFilter(OUTGOING_FILTER, new NotFilter(ORIGIN_ID_FILTER));

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (enabledByDefault) {
                    getInstanceFor(connection).enable();
                }

                MultiUserChatManager.addDefaultMessageInterceptor((mb, muc) -> {
                    // No need to add an <origin-id/> if the MUC service supports stable IDs.
                    if (muc.serviceSupportsStableIds()) {
                        return;
                    }
                    OriginIdElement.addTo(mb);
                });
            }
        });
    }

    /**
     * Private constructor.
     * @param connection XMPP connection
     */
    private StableUniqueStanzaIdManager(XMPPConnection connection) {
        super(connection);
    }

    public static void setEnabledByDefault(boolean enabled) {
        enabledByDefault = enabled;
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
        connection().addMessageInterceptor(OriginIdElement::addTo, ADD_ORIGIN_ID_FILTER::accept);
        ServiceDiscoveryManager.getInstanceFor(connection()).addFeature(NAMESPACE);
    }

    /**
     * Stop appending origin-id elements to outgoing stanzas and remove the feature from disco.
     */
    public synchronized void disable() {
        ServiceDiscoveryManager.getInstanceFor(connection()).removeFeature(NAMESPACE);
        connection().removeMessageInterceptor(OriginIdElement::addTo);
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
