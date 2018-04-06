/**
 *
 * Copyright 2003-2007 Jive Software, 2018 Paul Schaub.
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

package org.jivesoftware.smackx.chatstates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromTypeFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;

/**
 * Handles chat state for all chats on a particular XMPPConnection. This class manages both the
 * stanza extensions and the disco response necessary for compliance with
 * <a href="http://www.xmpp.org/extensions/xep-0085.html">XEP-0085</a>.
 *
 * NOTE: {@link org.jivesoftware.smackx.chatstates.ChatStateManager#getInstance(org.jivesoftware.smack.XMPPConnection)}
 * needs to be called in order for the listeners to be registered appropriately with the connection.
 * If this does not occur you will not receive the update notifications.
 *
 * @author Alexander Wenckus
 * @author Paul Schaub
 * @see org.jivesoftware.smackx.chatstates.ChatState
 * @see org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
 */
public final class ChatStateManager extends Manager {

    public static final String NAMESPACE = "http://jabber.org/protocol/chatstates";

    private static final Map<XMPPConnection, ChatStateManager> INSTANCES = new WeakHashMap<>();

    private static final StanzaFilter filter = new NotFilter(new StanzaExtensionFilter(NAMESPACE));
    private static final StanzaFilter INCOMING_MESSAGE_FILTER =
            new AndFilter(MessageTypeFilter.NORMAL_OR_CHAT, FromTypeFilter.ENTITY_FULL_JID);
    private static final StanzaFilter INCOMING_CHAT_STATE_FILTER = new AndFilter(INCOMING_MESSAGE_FILTER, new StanzaExtensionFilter(NAMESPACE));

    /**
     * Registered ChatStateListeners
     */
    private final Set<ChatStateListener> chatStateListeners = new HashSet<>();

    /**
     * Maps chat to last chat state.
     */
    private final Map<Chat, ChatState> chatStates = new WeakHashMap<>();

    /**
     * Returns the ChatStateManager related to the XMPPConnection and it will create one if it does
     * not yet exist.
     *
     * @param connection the connection to return the ChatStateManager
     * @return the ChatStateManager related the the connection.
     */
    public static synchronized ChatStateManager getInstance(final XMPPConnection connection) {
            ChatStateManager manager = INSTANCES.get(connection);
            if (manager == null) {
                manager = new ChatStateManager(connection);
                INSTANCES.put(connection, manager);
            }
            return manager;
    }

    /**
     * Private constructor to create a new ChatStateManager.
     * This adds ChatMessageListeners as interceptors to the connection and adds the namespace to the disco features.
     *
     * @param connection xmpp connection
     */
    private ChatStateManager(XMPPConnection connection) {
        super(connection);
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addOutgoingListener(new OutgoingChatMessageListener() {
            @Override
            public void newOutgoingMessage(EntityBareJid to, Message message, Chat chat) {
                if (chat == null) {
                    return;
                }

                // if message already has a chatStateExtension, then do nothing,
                if (!filter.accept(message)) {
                    return;
                }

                // otherwise add a chatState extension if necessary.
                if (updateChatState(chat, ChatState.active)) {
                    message.addExtension(new ChatStateExtension(ChatState.active));
                }
            }
        });

        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                Message message = (Message) stanza;

                EntityFullJid fullFrom = message.getFrom().asEntityFullJidIfPossible();
                EntityBareJid bareFrom = fullFrom.asEntityBareJid();

                Chat chat = ChatManager.getInstanceFor(connection()).chatWith(bareFrom);
                ExtensionElement extension = message.getExtension(NAMESPACE);

                if (extension == null) {
                    return;
                }

                ChatState state;
                try {
                    state = ChatState.valueOf(extension.getElementName());
                }
                catch (Exception ex) {
                    return;
                }

                List<ChatStateListener> listeners;
                synchronized (chatStateListeners) {
                    listeners = new ArrayList<>(chatStateListeners.size());
                    listeners.addAll(chatStateListeners);
                }

                for (ChatStateListener listener : listeners) {
                    listener.stateChanged(chat, state, message);
                }
            }
        }, INCOMING_CHAT_STATE_FILTER);

        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
    }

    /**
     * Register a ChatStateListener. That listener will be informed about changed chat states.
     *
     * @param listener chatStateListener
     * @return true, if the listener was not registered before
     */
    public boolean addChatStateListener(ChatStateListener listener) {
        synchronized (chatStateListeners) {
            return chatStateListeners.add(listener);
        }
    }

    /**
     * Unregister a ChatStateListener.
     *
     * @param listener chatStateListener
     * @return true, if the listener was registered before
     */
    public boolean removeChatStateListener(ChatStateListener listener) {
        synchronized (chatStateListeners) {
            return chatStateListeners.remove(listener);
        }
    }


    /**
     * Sets the current state of the provided chat. This method will send an empty bodied Message
     * stanza with the state attached as a {@link org.jivesoftware.smack.packet.ExtensionElement}, if
     * and only if the new chat state is different than the last state.
     *
     * @param newState the new state of the chat
     * @param chat the chat.
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void setCurrentState(ChatState newState, Chat chat) throws NotConnectedException, InterruptedException {
        if (chat == null || newState == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if (!updateChatState(chat, newState)) {
            return;
        }
        Message message = new Message();
        ChatStateExtension extension = new ChatStateExtension(newState);
        message.addExtension(extension);

        chat.send(message);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChatStateManager that = (ChatStateManager) o;

        return connection().equals(that.connection());

    }

    @Override
    public int hashCode() {
        return connection().hashCode();
    }

    private synchronized boolean updateChatState(Chat chat, ChatState newState) {
        ChatState lastChatState = chatStates.get(chat);
        if (lastChatState != newState) {
            chatStates.put(chat, newState);
            return true;
        }
        return false;
    }

}
