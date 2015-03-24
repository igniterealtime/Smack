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

package org.jivesoftware.smack.chat;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.JidWithLocalpart;

import java.util.Set;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A chat is a series of messages sent between two users. Each chat has a unique
 * thread ID, which is used to track which messages are part of a particular
 * conversation. Some messages are sent without a thread ID, and some clients
 * don't send thread IDs at all. Therefore, if a message without a thread ID
 * arrives it is routed to the most recently created Chat with the message
 * sender.
 * 
 * @author Matt Tucker
 */
public class Chat {

    private ChatManager chatManager;
    private String threadID;
    private JidWithLocalpart participant;
    private final Set<ChatMessageListener> listeners = new CopyOnWriteArraySet<ChatMessageListener>();

    /**
     * Creates a new chat with the specified user and thread ID.
     *
     * @param chatManager the chatManager the chat will use.
     * @param participant the user to chat with.
     * @param threadID the thread ID to use.
     */
    Chat(ChatManager chatManager, JidWithLocalpart participant, String threadID) {
        if (StringUtils.isEmpty(threadID)) {
            throw new IllegalArgumentException("Thread ID must not be null");
        }
        this.chatManager = chatManager;
        this.participant = participant;
        this.threadID = threadID;
    }

    /**
     * Returns the thread id associated with this chat, which corresponds to the
     * <tt>thread</tt> field of XMPP messages. This method may return <tt>null</tt>
     * if there is no thread ID is associated with this Chat.
     *
     * @return the thread ID of this chat.
     */
    public String getThreadID() {
        return threadID;
    }

    /**
     * Returns the name of the user the chat is with.
     *
     * @return the name of the user the chat is occuring with.
     */
    public JidWithLocalpart getParticipant() {
        return participant;
    }

    /**
     * Sends the specified text as a message to the other chat participant.
     * This is a convenience method for:
     *
     * <pre>
     *     Message message = chat.createMessage();
     *     message.setBody(messageText);
     *     chat.sendMessage(message);
     * </pre>
     *
     * @param text the text to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendMessage(String text) throws NotConnectedException, InterruptedException {
        Message message = new Message();
        message.setBody(text);
        sendMessage(message);
    }

    /**
     * Sends a message to the other chat participant. The thread ID, recipient,
     * and message type of the message will automatically set to those of this chat.
     *
     * @param message the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendMessage(Message message) throws NotConnectedException, InterruptedException {
        // Force the recipient, message type, and thread ID since the user elected
        // to send the message through this chat object.
        message.setTo(participant);
        message.setType(Message.Type.chat);
        message.setThread(threadID);
        chatManager.sendMessage(this, message);
    }

    /**
     * Adds a packet listener that will be notified of any new messages in the
     * chat.
     *
     * @param listener a packet listener.
     */
    public void addMessageListener(ChatMessageListener listener) {
        if(listener == null) {
            return;
        }
        // TODO these references should be weak.
        listeners.add(listener);
    }

    public void removeMessageListener(ChatMessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Closes the Chat and removes all references to it from the {@link ChatManager}. The chat will
     * be unusable when this method returns, so it's recommend to drop all references to the
     * instance right after calling {@link #close()}.
     */
    public void close() {
        chatManager.closeChat(this);
        listeners.clear();
    }

    /**
     * Returns an unmodifiable set of all of the listeners registered with this chat.
     *
     * @return an unmodifiable set of all of the listeners registered with this chat.
     */
    public Set<ChatMessageListener> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Creates a {@link org.jivesoftware.smack.PacketCollector} which will accumulate the Messages
     * for this chat. Always cancel PacketCollectors when finished with them as they will accumulate
     * messages indefinitely.
     *
     * @return the PacketCollector which returns Messages for this chat.
     */
    public PacketCollector createCollector() {
        return chatManager.createPacketCollector(this);
    }

    /**
     * Delivers a message directly to this chat, which will add the message
     * to the collector and deliver it to all listeners registered with the
     * Chat. This is used by the XMPPConnection class to deliver messages
     * without a thread ID.
     *
     * @param message the message.
     */
    void deliver(Message message) {
        // Because the collector and listeners are expecting a thread ID with
        // a specific value, set the thread ID on the message even though it
        // probably never had one.
        message.setThread(threadID);

        for (ChatMessageListener listener : listeners) {
            listener.processMessage(this, message);
        }
    }

    @Override
    public String toString() {
        return "Chat [(participant=" + participant + "), (thread=" + threadID + ")]";
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + threadID.hashCode();
        hash = hash * 31 + participant.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Chat
                && threadID.equals(((Chat)obj).getThreadID())
                && participant.equals(((Chat)obj).getParticipant());
    }
}
