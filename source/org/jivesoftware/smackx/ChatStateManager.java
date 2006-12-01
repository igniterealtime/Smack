/**
 * $RCSfile$
 * $Revision: 2407 $
 * $Date: 2004-11-02 15:37:00 -0800 (Tue, 02 Nov 2004) $
 *
 * Copyright 2003-2004 Jive Software.
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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.ChatStateExtension;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collection;

/**
 * Handles chat state for all chats on a particular XMPPConnection. This class manages both the
 * packet extensions and the disco response neccesary for compliance with
 * <a href="http://www.xmpp.org/extensions/xep-0085.html">XEP-0085</a>.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.ChatState
 * @see org.jivesoftware.smackx.packet.ChatStateExtension
 */
public class ChatStateManager {

    private static Map<XMPPConnection, ChatStateManager> managers =
            new WeakHashMap<XMPPConnection, ChatStateManager>();

    /**
     * Returns the ChatStateManager related to the XMPPConnection and it will create one if it does
     * not yet exist.
     *
     * @param connection the connection to return the ChatStateManager
     * @return the ChatStateManager related the the connection.
     */
    public static ChatStateManager getInstance(final XMPPConnection connection) {
        synchronized (connection) {
            ChatStateManager manager = managers.get(connection);
            if (manager == null) {
                manager = new ChatStateManager(connection);
                manager.init();
                managers.put(connection, manager);
            }

            return manager;
        }
    }

    private XMPPConnection connection;

    private OutgoingMessageInterceptor outgoingInterceptor = new OutgoingMessageInterceptor();

    private IncomingMessageInterceptor incomingInterceptor = new IncomingMessageInterceptor();

    private ChatStateManager(XMPPConnection connection) {
        this.connection = connection;
    }

    private void init() {
        PacketFilter filter = new NotFilter(
                new PacketExtensionFilter("http://jabber.org/protocol/chatstates"));
        connection.getChatManager().addOutgoingMessageInterceptor(outgoingInterceptor,
                filter);
        connection.getChatManager().addChatListener(incomingInterceptor);
    }

    /**
     * Sets the current state of the provided chat. This method will send an empty bodied Message
     * packet with the state attached as a {@link org.jivesoftware.smack.packet.PacketExtension}.
     *
     * @param newState the new state of the chat
     * @param chat the chat.
     * @throws org.jivesoftware.smack.XMPPException
     *          when there is an error sending the message
     *          packet.
     */
    public void setCurrentState(ChatState newState, Chat chat) throws XMPPException {
        Message message = new Message();
        ChatStateExtension extension = new ChatStateExtension(newState);
        message.addExtension(extension);

        chat.sendMessage(message);
    }

    private void fireNewChatState(Chat chat, ChatState state) {
        Collection<MessageListener> listeners = chat.getListeners();
        for (MessageListener listener : listeners) {
            if (listener instanceof ChatStateListener) {
                ((ChatStateListener) listener).stateChanged(chat, state);
            }
        }
    }

    private class OutgoingMessageInterceptor implements PacketInterceptor {

        public void interceptPacket(Packet packet) {
            if (!(packet instanceof Message)) {
                return;
            }
            Message message = (Message) packet;
            message.addExtension(new ChatStateExtension(ChatState.active));
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
