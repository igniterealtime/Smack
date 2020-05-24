/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.fallback_indication;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.fallback_indication.element.FallbackIndicationElement;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

/**
 * Smacks API for XEP-0428: Fallback Indication.
 * In some scenarios it might make sense to mark the body of a message as fallback for legacy clients.
 * Examples are encryption mechanisms where the sender might include a hint for legacy clients stating that the
 * body (eg. "This message is encrypted") should be ignored.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0428.html">XEP-0428: Fallback Indication</a>
 */
public final class FallbackIndicationManager extends Manager {

    private static final Map<XMPPConnection, FallbackIndicationManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private final Set<FallbackIndicationListener> listeners = new CopyOnWriteArraySet<>();
    private final AsyncButOrdered<BareJid> asyncButOrdered = new AsyncButOrdered<>();
    private final StanzaFilter fallbackIndicationElementFilter = new AndFilter(StanzaTypeFilter.MESSAGE,
            new StanzaExtensionFilter(FallbackIndicationElement.ELEMENT, FallbackIndicationElement.NAMESPACE));

    private final StanzaListener fallbackIndicationElementListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza packet) {
            Message message = (Message) packet;
            FallbackIndicationElement indicator = FallbackIndicationElement.fromMessage(message);
            String body = message.getBody();
            asyncButOrdered.performAsyncButOrdered(message.getFrom().asBareJid(), () -> {
                for (FallbackIndicationListener l : listeners) {
                    l.onFallbackIndicationReceived(message, indicator, body);
                }
            });
        }
    };

    private FallbackIndicationManager(XMPPConnection connection) {
        super(connection);
        connection.addAsyncStanzaListener(fallbackIndicationElementListener, fallbackIndicationElementFilter);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(FallbackIndicationElement.NAMESPACE);
    }

    public static synchronized FallbackIndicationManager getInstanceFor(XMPPConnection connection) {
        FallbackIndicationManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new FallbackIndicationManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Determine, whether or not a user supports Fallback Indications.
     *
     * @param jid BareJid of the user.
     * @return feature support
     *
     * @throws XMPPException.XMPPErrorException if a protocol level error happens
     * @throws SmackException.NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is being interrupted
     * @throws SmackException.NoResponseException if the server doesn't send a response in time
     */
    public boolean userSupportsFallbackIndications(EntityBareJid jid)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection())
                .supportsFeature(jid, FallbackIndicationElement.NAMESPACE);
    }

    /**
     * Determine, whether or not the server supports Fallback Indications.
     *
     * @return server side feature support
     *
     * @throws XMPPException.XMPPErrorException if a protocol level error happens
     * @throws SmackException.NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is being interrupted
     * @throws SmackException.NoResponseException if the server doesn't send a response in time
     */
    public boolean serverSupportsFallbackIndications()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection())
                .serverSupportsFeature(FallbackIndicationElement.NAMESPACE);
    }

    /**
     * Set the body of the message to the provided fallback message and add a {@link FallbackIndicationElement}.
     *
     * @param messageBuilder message builder
     * @param fallbackMessageBody fallback message body
     * @return builder with set body and added fallback element
     */
    public static MessageBuilder addFallbackIndicationWithBody(MessageBuilder messageBuilder, String fallbackMessageBody) {
        return addFallbackIndication(messageBuilder).setBody(fallbackMessageBody);
    }

    /**
     * Add a {@link FallbackIndicationElement} to the provided message builder.
     *
     * @param messageBuilder message builder
     * @return message builder with added fallback element
     */
    public static MessageBuilder addFallbackIndication(MessageBuilder messageBuilder) {
        return messageBuilder.addExtension(new FallbackIndicationElement());
    }

    /**
     * Register a {@link FallbackIndicationListener} that gets notified whenever a message that contains a
     * {@link FallbackIndicationElement} is received.
     *
     * @param listener listener to be registered.
     */
    public synchronized void addFallbackIndicationListener(FallbackIndicationListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a {@link FallbackIndicationListener}.
     *
     * @param listener listener to be unregistered.
     */
    public synchronized void removeFallbackIndicationListener(FallbackIndicationListener listener) {
        listeners.remove(listener);
    }
}
