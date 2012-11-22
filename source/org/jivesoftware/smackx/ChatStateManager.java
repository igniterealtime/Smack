/**
 * $RCSfile$
 * $Revision: 2407 $
 * $Date: 2004-11-02 15:37:00 -0800 (Tue, 02 Nov 2004) $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.collections.ReferenceMap;
import org.jivesoftware.smackx.packet.ChatStateExtension;

/**
 * Handles chat state for all chats on a particular Connection. This class manages both the
 * packet extensions and the disco response neccesary for compliance with
 * <a href="http://www.xmpp.org/extensions/xep-0085.html">XEP-0085</a>.
 *
 * NOTE: {@link org.jivesoftware.smackx.ChatStateManager#getInstance(org.jivesoftware.smack.Connection)}
 * needs to be called in order for the listeners to be registered appropriately with the connection.
 * If this does not occur you will not receive the update notifications.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.ChatState
 * @see org.jivesoftware.smackx.packet.ChatStateExtension
 */
public class ChatStateManager {

    private static final Map<Connection, WeakReference<ChatStateManager>> managers =
            new WeakHashMap<Connection, WeakReference<ChatStateManager>>();

    private static final PacketFilter filter = new NotFilter(
                new PacketExtensionFilter("http://jabber.org/protocol/chatstates"));

    /**
     * Returns the ChatStateManager related to the Connection and it will create one if it does
     * not yet exist.
     *
     * @param connection the connection to return the ChatStateManager
     * @return the ChatStateManager related the the connection.
     */
    public static ChatStateManager getInstance(final Connection connection) {
        if(connection == null) {
            return null;
        }
        synchronized (managers) {
            ChatStateManager manager;
            WeakReference<ChatStateManager> ref = managers.get(connection);
            
            if (ref == null) {
                manager = new ChatStateManager(connection);
                manager.init();
                managers.put(connection, new WeakReference<ChatStateManager>(manager));
            }
            else
            	manager = ref.get();

            return manager;
        }
    }

    private final Connection connection;

    private final OutgoingMessageInterceptor outgoingInterceptor = new OutgoingMessageInterceptor();

    private final IncomingMessageInterceptor incomingInterceptor = new IncomingMessageInterceptor();

    /**
     * Maps chat to last chat state.
     */
    private final Map<Chat, ChatState> chatStates =
            new ReferenceMap<Chat, ChatState>(ReferenceMap.WEAK, ReferenceMap.HARD);

    private ChatStateManager(Connection connection) {
        this.connection = connection;
    }

    private void init() {
        connection.getChatManager().addOutgoingMessageInterceptor(outgoingInterceptor,
                filter);
        connection.getChatManager().addChatListener(incomingInterceptor);

        ServiceDiscoveryManager.getInstanceFor(connection)
                .addFeature("http://jabber.org/protocol/chatstates");
    }

    /**
     * Sets the current state of the provided chat. This method will send an empty bodied Message
     * packet with the state attached as a {@link org.jivesoftware.smack.packet.PacketExtension}, if
     * and only if the new chat state is different than the last state.
     *
     * @param newState the new state of the chat
     * @param chat the chat.
     * @throws org.jivesoftware.smack.XMPPException
     *          when there is an error sending the message
     *          packet.
     */
    public void setCurrentState(ChatState newState, Chat chat) throws XMPPException {
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


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChatStateManager that = (ChatStateManager) o;

        return connection.equals(that.connection);

    }

    public int hashCode() {
        return connection.hashCode();
    }

    private boolean updateChatState(Chat chat, ChatState newState) {
        ChatState lastChatState = chatStates.get(chat);
        if (lastChatState != newState) {
            chatStates.put(chat, newState);
            return true;
        }
        return false;
    }

    private void fireNewChatState(Chat chat, ChatState state) {
        for (MessageListener listener : chat.getListeners()) {
            if (listener instanceof ChatStateListener) {
                ((ChatStateListener) listener).stateChanged(chat, state);
            }
        }
    }

    private class OutgoingMessageInterceptor implements PacketInterceptor {

        public void interceptPacket(Packet packet) {
            Message message = (Message) packet;
            Chat chat = connection.getChatManager().getThreadChat(message.getThread());
            if (chat == null) {
                return;
            }
            if (updateChatState(chat, ChatState.active)) {
                message.addExtension(new ChatStateExtension(ChatState.active));
            }
        }
    }

    private class IncomingMessageInterceptor implements ChatManagerListener, MessageListener {

        public void chatCreated(final Chat chat, boolean createdLocally) {
            chat.addMessageListener(this);
        }

        public void processMessage(Chat chat, Message message) {
            PacketExtension extension
                    = message.getExtension("http://jabber.org/protocol/chatstates");
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

            fireNewChatState(chat, state);
        }
    }
}
