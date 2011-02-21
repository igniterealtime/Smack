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

package org.jivesoftware.smack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.collections.ReferenceMap;

/**
 * The chat manager keeps track of references to all current chats. It will not hold any references
 * in memory on its own so it is neccesary to keep a reference to the chat object itself. To be
 * made aware of new chats, register a listener by calling {@link #addChatListener(ChatManagerListener)}.
 *
 * @author Alexander Wenckus
 */
public class ChatManager {

    /**
     * Returns the next unique id. Each id made up of a short alphanumeric
     * prefix along with a unique numeric value.
     *
     * @return the next id.
     */
    private static synchronized String nextID() {
        return prefix + Long.toString(id++);
    }

    /**
     * A prefix helps to make sure that ID's are unique across mutliple instances.
     */
    private static String prefix = StringUtils.randomString(5);

    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique ID.
     */
    private static long id = 0;

    /**
     * Maps thread ID to chat.
     */
    private Map<String, Chat> threadChats = Collections.synchronizedMap(new ReferenceMap<String, Chat>(ReferenceMap.HARD,
            ReferenceMap.WEAK));

    /**
     * Maps jids to chats
     */
    private Map<String, Chat> jidChats = Collections.synchronizedMap(new ReferenceMap<String, Chat>(ReferenceMap.HARD,
            ReferenceMap.WEAK));

    /**
     * Maps base jids to chats
     */
    private Map<String, Chat> baseJidChats = Collections.synchronizedMap(new ReferenceMap<String, Chat>(ReferenceMap.HARD,
	    ReferenceMap.WEAK));

    private Set<ChatManagerListener> chatManagerListeners
            = new CopyOnWriteArraySet<ChatManagerListener>();

    private Map<PacketInterceptor, PacketFilter> interceptors
            = new WeakHashMap<PacketInterceptor, PacketFilter>();

    private Connection connection;

    ChatManager(Connection connection) {
        this.connection = connection;

        PacketFilter filter = new PacketFilter() {
            public boolean accept(Packet packet) {
                if (!(packet instanceof Message)) {
                    return false;
                }
                Message.Type messageType = ((Message) packet).getType();
                return messageType != Message.Type.groupchat &&
                        messageType != Message.Type.headline;
            }
        };
        // Add a listener for all message packets so that we can deliver errant
        // messages to the best Chat instance available.
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                Chat chat;
                if (message.getThread() == null) {
                	chat = getUserChat(message.getFrom());
                }
                else {
                    chat = getThreadChat(message.getThread());
                    if (chat == null) {
                        // Try to locate the chat based on the sender of the message
                    	chat = getUserChat(message.getFrom());
                    }
                }

                if(chat == null) {
                    chat = createChat(message);
                }
                deliverMessage(chat, message);
            }
        }, filter);
    }

    /**
     * Creates a new chat and returns it.
     *
     * @param userJID the user this chat is with.
     * @param listener the listener which will listen for new messages from this chat.
     * @return the created chat.
     */
    public Chat createChat(String userJID, MessageListener listener) {
        String threadID;
        do  {
            threadID = nextID();
        } while (threadChats.get(threadID) != null);

        return createChat(userJID, threadID, listener);
    }

    /**
     * Creates a new chat using the specified thread ID, then returns it.
     * 
     * @param userJID the jid of the user this chat is with
     * @param thread the thread of the created chat.
     * @param listener the listener to add to the chat
     * @return the created chat.
     */
    public Chat createChat(String userJID, String thread, MessageListener listener) {
        if(thread == null) {
            thread = nextID();
        }
        Chat chat = threadChats.get(thread);
        if(chat != null) {
            throw new IllegalArgumentException("ThreadID is already used");
        }
        chat = createChat(userJID, thread, true);
        chat.addMessageListener(listener);
        return chat;
    }

    private Chat createChat(String userJID, String threadID, boolean createdLocally) {
        Chat chat = new Chat(this, userJID, threadID);
        threadChats.put(threadID, chat);
        jidChats.put(userJID, chat);
        baseJidChats.put(StringUtils.parseBareAddress(userJID), chat);

        for(ChatManagerListener listener : chatManagerListeners) {
            listener.chatCreated(chat, createdLocally);
        }

        return chat;
    }

    private Chat createChat(Message message) {
        String threadID = message.getThread();
        if(threadID == null) {
            threadID = nextID();
        }
        String userJID = message.getFrom();

        return createChat(userJID, threadID, false);
    }

    /**
     * Try to get a matching chat for the given user JID.  Try the full
     * JID map first, the try to match on the base JID if no match is
     * found.
     * 
     * @param userJID
     * @return
     */
    private Chat getUserChat(String userJID) {
	Chat match = jidChats.get(userJID);
	
	if (match == null) {
	    match = baseJidChats.get(StringUtils.parseBareAddress(userJID));
	}
	return match;
    }

    public Chat getThreadChat(String thread) {
        return threadChats.get(thread);
    }

    /**
     * Register a new listener with the ChatManager to recieve events related to chats.
     *
     * @param listener the listener.
     */
    public void addChatListener(ChatManagerListener listener) {
        chatManagerListeners.add(listener);
    }

    /**
     * Removes a listener, it will no longer be notified of new events related to chats.
     *
     * @param listener the listener that is being removed
     */
    public void removeChatListener(ChatManagerListener listener) {
        chatManagerListeners.remove(listener);
    }

    /**
     * Returns an unmodifiable collection of all chat listeners currently registered with this
     * manager.
     *
     * @return an unmodifiable collection of all chat listeners currently registered with this
     * manager.
     */
    public Collection<ChatManagerListener> getChatListeners() {
        return Collections.unmodifiableCollection(chatManagerListeners);
    }

    private void deliverMessage(Chat chat, Message message) {
        // Here we will run any interceptors
        chat.deliver(message);
    }

    void sendMessage(Chat chat, Message message) {
        for(Map.Entry<PacketInterceptor, PacketFilter> interceptor : interceptors.entrySet()) {
            PacketFilter filter = interceptor.getValue();
            if(filter != null && filter.accept(message)) {
                interceptor.getKey().interceptPacket(message);
            }
        }
        // Ensure that messages being sent have a proper FROM value
        if (message.getFrom() == null) {
            message.setFrom(connection.getUser());
        }
        connection.sendPacket(message);
    }

    PacketCollector createPacketCollector(Chat chat) {
        return connection.createPacketCollector(new AndFilter(new ThreadFilter(chat.getThreadID()), 
                new FromContainsFilter(chat.getParticipant())));
    }

    /**
     * Adds an interceptor which intercepts any messages sent through chats.
     *
     * @param packetInterceptor the interceptor.
     */
    public void addOutgoingMessageInterceptor(PacketInterceptor packetInterceptor) {
        addOutgoingMessageInterceptor(packetInterceptor, null);
    }

    public void addOutgoingMessageInterceptor(PacketInterceptor packetInterceptor, PacketFilter filter) {
        if (packetInterceptor != null) {
            interceptors.put(packetInterceptor, filter);
        }
    }
}
