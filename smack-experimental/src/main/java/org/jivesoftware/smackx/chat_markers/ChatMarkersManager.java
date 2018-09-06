/**
 *
 * Copyright © 2016 Fernando Ramirez
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

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
import org.jivesoftware.smackx.chat_markers.filter.EligibleForChatMarker;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;

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
            MessageTypeFilter.NORMAL_OR_CHAT_OR_GROUPCHAT,
            new StanzaExtensionFilter(ChatMarkersElements.NAMESPACE),
            PossibleFromTypeFilter.ENTITY_BARE_JID,
            EligibleForChatMarker.INSTANCE
    );

    private static final StanzaFilter OUTGOING_MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT_OR_GROUPCHAT,
            MessageWithBodiesFilter.INSTANCE,
            new NotFilter(ChatMarkersFilter.INSTANCE),
            EligibleForChatMarker.INSTANCE
    );
    // @FORMATTER:ON

    private final Set<ChatMarkersListener> incomingListeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<Chat> asyncButOrdered = new AsyncButOrdered<>();

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
        connection.addStanzaInterceptor(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                    throws
                    NotConnectedException,
                    InterruptedException,
                    SmackException.NotLoggedInException {
                Message message = (Message) packet;
                // add a markable extension
                message.addExtension(new ChatMarkersElements.MarkableExtension());
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

                EntityFullJid fullFrom = message.getFrom().asEntityFullJidIfPossible();
                EntityBareJid bareFrom = fullFrom.asEntityBareJid();
                final Chat chat = ChatManager.getInstanceFor(connection()).chatWith(bareFrom);

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

        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(ChatMarkersElements.NAMESPACE);
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
    public boolean addIncomingChatMarkerMessageListener(ChatMarkersListener listener) {
        return incomingListeners.add(listener);
    }

    /**
     * Unregister a ChatMarkersListener.
     *
     * @param listener ChatMarkersListener
     * @return true, if the listener was registered before
     */
    public boolean removeIncomingChatMarkerMessageListener(ChatMarkersListener listener) {
        return incomingListeners.remove(listener);
    }

    public void markMessageAsReceived(Message message)
            throws
            NotConnectedException,
            InterruptedException,
            IllegalArgumentException {
        if (message == null) {
            throw new IllegalArgumentException("To and From needed");
        }
        message.addExtension(new ChatMarkersElements.ReceivedExtension(message.getStanzaId()));
        sendChatMarkerMessage(message);
    }

    public void markMessageAsDisplayed(Message message)
            throws
            NotConnectedException,
            InterruptedException,
            IllegalArgumentException {
        if (message == null) {
            throw new IllegalArgumentException("To and From needed");
        }
        message.addExtension(new ChatMarkersElements.DisplayedExtension(message.getStanzaId()));
        sendChatMarkerMessage(message);
    }

    public void markMessageAsAcknowledged(Message message)
            throws
            NotConnectedException,
            InterruptedException,
            IllegalArgumentException {
        if (message == null) {
            throw new IllegalArgumentException("To and From needed");
        }
        message.addExtension(new ChatMarkersElements.AcknowledgedExtension(message.getStanzaId()));
        sendChatMarkerMessage(message);
    }

    private void sendChatMarkerMessage(Message message) throws NotConnectedException, InterruptedException {
        connection().sendStanza(message);
    }
}
