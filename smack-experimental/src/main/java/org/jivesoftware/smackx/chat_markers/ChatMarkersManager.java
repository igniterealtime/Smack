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

import java.util.ArrayList;
import java.util.List;
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
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

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
    private static final StanzaFilter FILTER = new NotFilter(new StanzaExtensionFilter(ChatMarkersElements.NAMESPACE));
    private static final StanzaFilter CHAT_STATE_FILTER = new NotFilter(new StanzaExtensionFilter(ChatStateManager.NAMESPACE));

    private static final StanzaFilter MESSAGE_FILTER = new OrFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            MessageTypeFilter.GROUPCHAT
    );

    private static final StanzaFilter INCOMING_MESSAGE_FILTER = new AndFilter(
            MESSAGE_FILTER,
            new StanzaExtensionFilter(ChatMarkersElements.NAMESPACE)
    );
    // @FORMATTER:ON

    private final Set<ChatMarkersListener> incomingListeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<Chat> asyncButOrdered = new AsyncButOrdered<>();

    /**
     * Get the singleton instance of ChatMarkersManager.
     *
     * @param connection
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
                if (shouldDiscardMessage(message)) {
                    return;
                }

                if (message.getBodies().isEmpty()) {
                    return;
                }

                // if message already has a chatMarkerExtension, then do nothing,
                if (!FILTER.accept(message)) {
                    return;
                }

                // otherwise add a markable extension,
                message.addExtension(new ChatMarkersElements.MarkableExtension());
            }
        }, MESSAGE_FILTER);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                    throws
                    NotConnectedException,
                    InterruptedException,
                    SmackException.NotLoggedInException {
                final Message message = (Message) packet;
                if (shouldDiscardMessage(message)) {
                    return;
                }

                EntityFullJid fullFrom = message.getFrom().asEntityFullJidIfPossible();
                EntityBareJid bareFrom = fullFrom.asEntityBareJid();
                final Chat chat = ChatManager.getInstanceFor(connection()).chatWith(bareFrom);

                List<ChatMarkersListener> listeners;
                synchronized (incomingListeners) {
                    listeners = new ArrayList<>(incomingListeners.size());
                    listeners.addAll(incomingListeners);
                }

                final List<ChatMarkersListener> finalListeners = listeners;
                asyncButOrdered.performAsyncButOrdered(chat, new Runnable() {
                    @Override
                    public void run() {
                        for (ChatMarkersListener listener : finalListeners) {
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
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
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
        synchronized (incomingListeners) {
            return incomingListeners.add(listener);
        }
    }

    /**
     * Unregister a ChatMarkersListener.
     *
     * @param listener ChatMarkersListener
     * @return true, if the listener was registered before
     */
    public boolean removeIncomingChatMarkerMessageListener(ChatMarkersListener listener) {
        synchronized (incomingListeners) {
            return incomingListeners.remove(listener);
        }
    }

    /**
     * From XEP-0333, Protocol Format: The Chat Marker MUST have an 'id' which is the 'id' of the
     * message being marked.<br>
     * In order to make Chat Markers works together with XEP-0085 as it said in
     * 8.5 Interaction with Chat States, only messages with <tt>active</tt> chat
     * state are accepted.
     *
     * @param message to be analyzed.
     * @return true if the message contains a stanza Id.
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat Markers</a>
     */
    private boolean shouldDiscardMessage(Message message) {
        if (StringUtils.isNullOrEmpty(message.getStanzaId())) {
            return true;
        }

        if (!CHAT_STATE_FILTER.accept(message)) {
            ExtensionElement extension = message.getExtension(ChatStateManager.NAMESPACE);
            String chatStateElementName = extension.getElementName();

            ChatState state;
            try {
                state = ChatState.valueOf(chatStateElementName);
                return !(state == ChatState.active);
            }
            catch (Exception ex) {
                return true;
            }
        }

        return false;
    }

    public void markMessageAsReceived(String id, Jid to, Jid from, String thread)
            throws
            NotConnectedException,
            InterruptedException {
        Message message = createMessage(id, to, from, thread);
        message.addExtension(new ChatMarkersElements.ReceivedExtension(id));
        sendChatMarkerMessage(message);
    }

    public void markMessageAsDisplayed(String id, Jid to, Jid from, String thread)
            throws
            NotConnectedException,
            InterruptedException {
        Message message = createMessage(id, to, from, thread);
        message.addExtension(new ChatMarkersElements.DisplayedExtension(id));
        sendChatMarkerMessage(message);
    }

    public void markMessageAsAcknowledged(String id, Jid to, Jid from, String thread)
            throws
            NotConnectedException,
            InterruptedException {
        Message message = createMessage(id, to, from, thread);
        message.addExtension(new ChatMarkersElements.AcknowledgedExtension(id));
        sendChatMarkerMessage(message);
    }

    private Message createMessage(String id, Jid to, Jid from, String thread) {
        Message message = new Message();
        message.setStanzaId(id);
        message.setTo(to);
        message.setFrom(from);
        message.setThread(thread);
        return message;
    }

    private void sendChatMarkerMessage(Message message) throws NotConnectedException, InterruptedException {
        connection().sendStanza(message);
    }
}
