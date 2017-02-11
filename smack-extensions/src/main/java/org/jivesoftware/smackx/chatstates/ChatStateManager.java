/**
 *
 * Copyright 2003-2007 Jive Software.
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

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * Handles chat state for all chats on a particular XMPPConnection. This class manages both the
 * stanza(/packet) extensions and the disco response necessary for compliance with
 * <a href="http://www.xmpp.org/extensions/xep-0085.html">XEP-0085</a>.
 *
 * NOTE: {@link org.jivesoftware.smackx.chatstates.ChatStateManager#getInstance(org.jivesoftware.smack.XMPPConnection)}
 * needs to be called in order for the listeners to be registered appropriately with the connection.
 * If this does not occur you will not receive the update notifications.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.chatstates.ChatState
 * @see org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
 */
// TODO Migrate to new chat2 API on Smack 4.3.
@SuppressWarnings("deprecation")
public final class ChatStateManager extends Manager {
    public static final String NAMESPACE = "http://jabber.org/protocol/chatstates";

    private static final Map<XMPPConnection, ChatStateManager> INSTANCES =
            new WeakHashMap<XMPPConnection, ChatStateManager>();

    private static final StanzaFilter filter = new NotFilter(new StanzaExtensionFilter(NAMESPACE));

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
            }
            return manager;
    }

    private final OutgoingMessageInterceptor outgoingInterceptor = new OutgoingMessageInterceptor();

    private final IncomingMessageInterceptor incomingInterceptor = new IncomingMessageInterceptor();

    /**
     * Maps chat to last chat state.
     */
    private final Map<org.jivesoftware.smack.chat.Chat, ChatState> chatStates = new WeakHashMap<>();

    private final org.jivesoftware.smack.chat.ChatManager chatManager;

    private ChatStateManager(XMPPConnection connection) {
        super(connection);
        chatManager = org.jivesoftware.smack.chat.ChatManager.getInstanceFor(connection);
        chatManager.addOutgoingMessageInterceptor(outgoingInterceptor, filter);
        chatManager.addChatListener(incomingInterceptor);

        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
        INSTANCES.put(connection, this);
    }


    /**
     * Sets the current state of the provided chat. This method will send an empty bodied Message
     * stanza(/packet) with the state attached as a {@link org.jivesoftware.smack.packet.ExtensionElement}, if
     * and only if the new chat state is different than the last state.
     *
     * @param newState the new state of the chat
     * @param chat the chat.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void setCurrentState(ChatState newState, org.jivesoftware.smack.chat.Chat chat) throws NotConnectedException, InterruptedException {
        if(chat == null || newState == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if(!updateChatState(chat, newState)) {
            return;
        }
        Message message = new Message();
        ChatStateExtension extension = new ChatStateExtension(newState);
        message.addExtension(extension);

        chat.sendMessage(message);
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

    private synchronized boolean updateChatState(org.jivesoftware.smack.chat.Chat chat, ChatState newState) {
        ChatState lastChatState = chatStates.get(chat);
        if (lastChatState != newState) {
            chatStates.put(chat, newState);
            return true;
        }
        return false;
    }

    private static void fireNewChatState(org.jivesoftware.smack.chat.Chat chat, ChatState state, Message message) {
        for (ChatMessageListener listener : chat.getListeners()) {
            if (listener instanceof ChatStateListener) {
                ((ChatStateListener) listener).stateChanged(chat, state, message);
            }
        }
    }

    private class OutgoingMessageInterceptor implements MessageListener {

        @Override
        public void processMessage(Message message) {
            org.jivesoftware.smack.chat.Chat chat = chatManager.getThreadChat(message.getThread());
            if (chat == null) {
                return;
            }
            if (updateChatState(chat, ChatState.active)) {
                message.addExtension(new ChatStateExtension(ChatState.active));
            }
        }
    }

    private static class IncomingMessageInterceptor implements ChatManagerListener, ChatMessageListener {

        @Override
        public void chatCreated(final org.jivesoftware.smack.chat.Chat chat, boolean createdLocally) {
            chat.addMessageListener(this);
        }

        @Override
        public void processMessage(org.jivesoftware.smack.chat.Chat chat, Message message) {
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

            fireNewChatState(chat, state, message);
        }
    }
}
