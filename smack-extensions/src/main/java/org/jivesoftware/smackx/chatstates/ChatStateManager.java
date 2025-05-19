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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ToTypeFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;

import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
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
 * @author Eng Chong Meng
 * @see org.jivesoftware.smackx.chatstates.ChatState
 * @see org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
 */
public final class ChatStateManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(ChatStateManager.class.getName());

    public static final String NAMESPACE = "http://jabber.org/protocol/chatstates";

    private static final Map<XMPPConnection, ChatStateManager> INSTANCES = new WeakHashMap<>();

    private static final StanzaFilter INCOMING_MESSAGE_FILTER =
            new AndFilter(new OrFilter(MessageTypeFilter.NORMAL_OR_CHAT, MessageTypeFilter.GROUPCHAT), FromTypeFilter.ENTITY_FULL_JID);
    private static final StanzaFilter INCOMING_CHAT_STATE_FILTER =
            new AndFilter(INCOMING_MESSAGE_FILTER, new StanzaExtensionFilter(NAMESPACE));

    private static final StanzaFilter MESSAGE_FILTER = new AndFilter(
            new OrFilter(MessageTypeFilter.NORMAL_OR_CHAT, MessageTypeFilter.GROUPCHAT),
            new OrFilter(MessageWithBodiesFilter.INSTANCE, new StanzaExtensionFilter(XHTMLExtension.ELEMENT, XHTMLExtension.NAMESPACE)));

    private static final StanzaFilter OUTGOING_MESSAGE_FILTER =
            new AndFilter(MESSAGE_FILTER, ToTypeFilter.ENTITY_FULL_OR_BARE_JID);

    /**
     * Registered ChatStateListeners
     */
    private final Set<ChatStateListener> chatStateListeners = new HashSet<>();

    /**
     * Maps chat/mucChat to last chat state.
     */
    private final Map<Object, ChatState> chatStates = new WeakHashMap<>();

    private final AsyncButOrdered<Chat> asyncButOrdered = new AsyncButOrdered<>();

    /**
     * Returns the ChatStateManager related to the XMPPConnection and it will create one if it does
     * not yet exist.
     *
     * @param connection the connection to return the ChatStateManager
     * @return the ChatStateManager related the connection.
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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(connection);

        connection.addMessageInterceptor(messageBuilder -> {
            Message message = messageBuilder.build();

            // if message already has a chatStateExtension, then do nothing,
            if (message.hasExtension(ChatStateExtension.NAMESPACE)) {
                return;
            }

            Object chat;
            EntityBareJid entityBareJid = message.getTo().asEntityBareJidIfPossible();
            if (message.getType() == Message.Type.groupchat) {
                chat = multiUserChatManager.getMultiUserChat(entityBareJid);
            }
            else {
                chat = chatManager.chatWith(entityBareJid);
            }

            // otherwise add a chatState extension if necessary.
            if (updateChatState(chat, ChatState.active)) {
                messageBuilder.addExtension(new ChatStateExtension(ChatState.active));
            }
        }, OUTGOING_MESSAGE_FILTER::accept);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                final Message message = (Message) stanza;

                EntityFullJid fullFrom = message.getFrom().asEntityFullJidIfPossible();
                EntityBareJid bareFrom = fullFrom.asEntityBareJid();

                final Chat chat = ChatManager.getInstanceFor(connection()).chatWith(bareFrom);
                ExtensionElement extension = message.getExtension(NAMESPACE);
                String chatStateElementName = extension.getElementName();

                ChatState state;
                try {
                    state = ChatState.valueOf(chatStateElementName);
                }
                catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Invalid chat state element name: " + chatStateElementName, ex);
                    return;
                }
                final ChatState finalState = state;

                List<ChatStateListener> listeners;
                synchronized (chatStateListeners) {
                    listeners = new ArrayList<>(chatStateListeners.size());
                    listeners.addAll(chatStateListeners);
                }

                final List<ChatStateListener> finalListeners = listeners;
                asyncButOrdered.performAsyncButOrdered(chat, new Runnable() {
                    @Override
                    public void run() {
                        for (ChatStateListener listener : finalListeners) {
                            listener.stateChanged(chat, finalState, message);
                        }
                    }
                });
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setCurrentState(ChatState newState, Chat chat) throws NotConnectedException, InterruptedException {
        if (chat == null || newState == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if (!updateChatState(chat, newState)) {
            return;
        }
        Message message = StanzaBuilder.buildMessage().build();
        ChatStateExtension extension = new ChatStateExtension(newState);
        message.addExtension(extension);

        chat.send(message);
    }

    /**
     * Sets the current state of the provided mucChat. This method will send an empty bodied Message
     * stanza with the state attached as a {@link org.jivesoftware.smack.packet.ExtensionElement},
     * if and only if the new chat state is different from the last state.
     *
     * @param newState  the new state of the chat
     * @param mucChat the MultiUserChat instance
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setCurrentState(ChatState newState, MultiUserChat mucChat) throws NotConnectedException, InterruptedException {
        if (mucChat == null || newState == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if (!updateChatState(mucChat, newState)) {
            return;
        }
        MessageBuilder message = StanzaBuilder.buildMessage()
                .addExtension(new ChatStateExtension(newState));
        mucChat.sendMessage(message);
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

    private synchronized boolean updateChatState(Object chat, ChatState newState) {
        ChatState lastChatState = chatStates.get(chat);
        if (lastChatState != newState) {
            chatStates.put(chat, newState);
            return true;
        }
        return false;
    }
}
