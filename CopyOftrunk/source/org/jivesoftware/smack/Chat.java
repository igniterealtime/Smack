/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.filter.*;

/**
 * A chat is a series of messages sent between two users. Each chat can have
 * a unique thread ID, which is used to track which messages are part of a particular
 * conversation.<p>
 *
 * In some situations, it is better to have all messages from the other user delivered
 * to a Chat rather than just the messages that have a particular thread ID. To
 * enable this behavior, call {@link #setFilteredOnThreadID(boolean)} with
 * <tt>false</tt> as the parameter.
 *
 * @see XMPPConnection#createChat(String)
 * @author Matt Tucker
 */
public class Chat {

    /**
     * A prefix helps to make sure that ID's are unique across mutliple instances.
     */
    private static String prefix = StringUtils.randomString(5);
    
    /**
     * True if only messages that have a matching threadID will be delivered to a Chat. When
     * false, any message from the other participant will be delivered to a Chat.
     */
    private static boolean filteredOnThreadID = true;
    
    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique ID.
     */
    private static long id = 0;

    /**
     * Returns the next unique id. Each id made up of a short alphanumeric
     * prefix along with a unique numeric value.
     *
     * @return the next id.
     */
    private static synchronized String nextID() {
        return prefix + Long.toString(id++);
    }

    private XMPPConnection connection;
    private String threadID;
    private String participant;
    private PacketFilter messageFilter;
    private PacketCollector messageCollector;

    /**
     * Creates a new chat with the specified user.
     *
     * @param connection the connection the chat will use.
     * @param participant the user to chat with.
     */
    public Chat(XMPPConnection connection, String participant) {
        // Automatically assign the next chat ID.
        this(connection, participant, nextID());
        // If not filtering on thread ID, force the thread ID for this Chat to be null.
        if (!filteredOnThreadID) {
            this.threadID = null;
        }
    }

    /**
     * Creates a new chat with the specified user and thread ID.
     *
     * @param connection the connection the chat will use.
     * @param participant the user to chat with.
     * @param threadID the thread ID to use.
     */
    public Chat(XMPPConnection connection, String participant, String threadID) {
        this.connection = connection;
        this.participant = participant;
        this.threadID = threadID;

        if (filteredOnThreadID) {
            // Filter the messages whose thread equals Chat's id
            messageFilter = new ThreadFilter(threadID);
        }
        else {
            // Filter the messages of type "chat" and sender equals Chat's participant
            messageFilter =
                new OrFilter(
                    new AndFilter(
                        new MessageTypeFilter(Message.Type.CHAT),
                        new FromContainsFilter(participant)),
                    new ThreadFilter(threadID));
        }
        messageCollector = connection.createPacketCollector(messageFilter);
    }

    /**
     * Returns true if only messages that have a matching threadID will be delivered to Chat
     * instances. When false, any message from the other participant will be delivered to Chat instances.
     *
     * @return true if messages delivered to Chat instances are filtered on thread ID.
     */
    public static boolean isFilteredOnThreadID() {
        return filteredOnThreadID;
    }

    /**
     * Sets whether only messages that have a matching threadID will be delivered to Chat instances.
     * When false, any message from the other participant will be delivered to a Chat instances.
     *
     * @param value true if messages delivered to Chat instances are filtered on thread ID.
     */
    public static void setFilteredOnThreadID(boolean value) {
        filteredOnThreadID = value;
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
    public String getParticipant() {
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
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(String text) throws XMPPException {
        Message message = createMessage();
        message.setBody(text);
        connection.sendPacket(message);
    }

    /**
     * Creates a new Message to the chat participant. The message returned
     * will have its thread property set with this chat ID.
     *
     * @return a new message addressed to the chat participant and
     *      using the correct thread value.
     * @see #sendMessage(Message)
     */
    public Message createMessage() {
        Message message = new Message(participant, Message.Type.CHAT);
        message.setThread(threadID);
        return message;
    }

    /**
     * Sends a message to the other chat participant. The thread ID, recipient,
     * and message type of the message will automatically set to those of this chat
     * in case the Message was not created using the {@link #createMessage() createMessage}
     * method.
     *
     * @param message the message to send.
     * @throws XMPPException if an error occurs sending the message.
     */
    public void sendMessage(Message message) throws XMPPException {
        // Force the recipient, message type, and thread ID since the user elected
        // to send the message through this chat object.
        message.setTo(participant);
        message.setType(Message.Type.CHAT);
        message.setThread(threadID);
        connection.sendPacket(message);
    }

    /**
     * Polls for and returns the next message, or <tt>null</tt> if there isn't
     * a message immediately available. This method provides significantly different
     * functionalty than the {@link #nextMessage()} method since it's non-blocking.
     * In other words, the method call will always return immediately, whereas the
     * nextMessage method will return only when a message is available (or after
     * a specific timeout).
     *
     * @return the next message if one is immediately available and
     *      <tt>null</tt> otherwise.
     */
    public Message pollMessage() {
        return (Message)messageCollector.pollResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a message is available.
     *
     * @return the next message.
     */
    public Message nextMessage() {
        return (Message)messageCollector.nextResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a packet is available or the <tt>timeout</tt> has elapased.
     * If the timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the maximum amount of time to wait for the next message.
     * @return the next message, or <tt>null</tt> if the timeout elapses without a
     *      message becoming available.
     */
    public Message nextMessage(long timeout) {
        return (Message)messageCollector.nextResult(timeout);
    }

    /**
     * Adds a packet listener that will be notified of any new messages in the
     * chat.
     *
     * @param listener a packet listener.
     */
    public void addMessageListener(PacketListener listener) {
        connection.addPacketListener(listener, messageFilter);
    }

    public void finalize() throws Throwable {
        super.finalize();
        try {
            if (messageCollector != null) {
                messageCollector.cancel();
            }
        }
        catch (Exception e) {}
    }
}