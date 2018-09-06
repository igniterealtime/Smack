/**
 *
 * Copyright © 2016 Fernando Ramirez, 2018 Florian Schmaus
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
package org.jivesoftware.smackx.chat_markers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PossibleFromTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.chat_markers.filter.ChatMarkersFilter;
import org.jivesoftware.smackx.chat_markers.filter.EligibleForChatMarkerFilter;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.jid.EntityBareJid;

/**
 * Chat Markers Manager class (XEP-0333).
 *
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 *      Markers</a>
 * @author Miguel Hincapie
 * @author Fernando Ramirez
 *
 */
public final class ChatMarkersManager extends Manager {

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, ChatMarkersManager> INSTANCES = new WeakHashMap<>();

    // @FORMATTER:OFF
    private static final StanzaFilter INCOMING_MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            new StanzaExtensionFilter(ChatMarkersElements.NAMESPACE),
            PossibleFromTypeFilter.ENTITY_BARE_JID,
            EligibleForChatMarkerFilter.INSTANCE
    );

    private static final StanzaFilter OUTGOING_MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            MessageWithBodiesFilter.INSTANCE,
            new NotFilter(ChatMarkersFilter.INSTANCE),
            EligibleForChatMarkerFilter.INSTANCE
    );
    // @FORMATTER:ON

    private final Set<ChatMarkersListener> incomingListeners = new HashSet<>();

    private final AsyncButOrdered<Chat> asyncButOrdered = new AsyncButOrdered<>();

    private final ChatManager chatManager;

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private boolean enabled;

    /**
     * Get the singleton instance of ChatMarkersManager.
     *
     * @param connection the connection used to get the ChatMarkersManager instance.
     * @return the instance of ChatMarkersManager
     */
    public static synchronized ChatMarkersManager getInstanceFor(XMPPConnection connection) {
        ChatMarkersManager chatMarkersManager = INSTANCES.get(connection);

        if (chatMarkersManager == null) {
            chatMarkersManager = new ChatMarkersManager(connection);
            INSTANCES.put(connection, chatMarkersManager);
        }

        return chatMarkersManager;
    }

    private ChatMarkersManager(XMPPConnection connection) {
        super(connection);

        chatManager = ChatManager.getInstanceFor(connection);

        connection.addStanzaInterceptor(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                    throws
                    NotConnectedException,
                    InterruptedException,
                    SmackException.NotLoggedInException {
                Message message = (Message) packet;
                // add a markable extension
                message.addExtension(ChatMarkersElements.MarkableExtension.INSTANCE);
            }
        }, OUTGOING_MESSAGE_FILTER);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                    throws
                    NotConnectedException,
                    InterruptedException,
                    SmackException.NotLoggedInException {
                final Message message = (Message) packet;

                // Note that this listener is used together with a PossibleFromTypeFilter.ENTITY_BARE_JID filter, hence
                // every message is guaranteed to have a from address which is representable as bare JID.
                EntityBareJid bareFrom = message.getFrom().asEntityBareJidOrThrow();

                final Chat chat = chatManager.chatWith(bareFrom);

                asyncButOrdered.performAsyncButOrdered(chat, new Runnable() {
                    @Override
                    public void run() {
                        for (ChatMarkersListener listener : incomingListeners) {
                            if (ChatMarkersElements.MarkableExtension.from(message) != null) {
                                listener.newChatMarkerMessage(ChatMarkersState.markable, message, chat);
                            }
                            else if (ChatMarkersElements.ReceivedExtension.from(message) != null) {
                                listener.newChatMarkerMessage(ChatMarkersState.received, message, chat);
                            }
                            else if (ChatMarkersElements.DisplayedExtension.from(message) != null) {
                                listener.newChatMarkerMessage(ChatMarkersState.displayed, message, chat);
                            }
                            else if (ChatMarkersElements.AcknowledgedExtension.from(message) != null) {
                                listener.newChatMarkerMessage(ChatMarkersState.acknowledged, message, chat);
                            }
                        }
                    }
                });

            }
        }, INCOMING_MESSAGE_FILTER);

        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    /**
     * Returns true if Chat Markers is supported by the server.
     *
     * @return true if Chat Markers is supported by the server.
     * @throws NotConnectedException if the connection is not connected.
     * @throws XMPPErrorException in case an error response was received.
     * @throws NoResponseException if no response was received.
     * @throws InterruptedException if the connection is interrupted.
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection())
                .serverSupportsFeature(ChatMarkersElements.NAMESPACE);
    }

    /**
     * Register a ChatMarkersListener. That listener will be informed about new
     * incoming markable messages.
     *
     * @param listener ChatMarkersListener
     * @return true, if the listener was not registered before
     */
    public synchronized boolean addIncomingChatMarkerMessageListener(ChatMarkersListener listener) {
        boolean res = incomingListeners.add(listener);
        if (!enabled) {
            serviceDiscoveryManager.addFeature(ChatMarkersElements.NAMESPACE);
            enabled = true;
        }
        return res;
    }

    /**
     * Unregister a ChatMarkersListener.
     *
     * @param listener ChatMarkersListener
     * @return true, if the listener was registered before
     */
    public synchronized boolean removeIncomingChatMarkerMessageListener(ChatMarkersListener listener) {
        boolean res = incomingListeners.remove(listener);
        if (incomingListeners.isEmpty() && enabled) {
            serviceDiscoveryManager.removeFeature(ChatMarkersElements.NAMESPACE);
            enabled = false;
        }
        return res;
    }
}
