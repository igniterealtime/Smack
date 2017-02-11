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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.EntityJid;

/**
 * The chat manager keeps track of references to all current chats. It will not hold any references
 * in memory on its own so it is necessary to keep a reference to the chat object itself. To be
 * made aware of new chats, register a listener by calling {@link #addChatListener(ChatManagerListener)}.
 *
 * @author Alexander Wenckus
 * @deprecated use <code>org.jivesoftware.smack.chat2.ChatManager</code> from <code>smack-extensions</code> instead.
 */
@Deprecated
public final class ChatManager extends Manager{

    private static final Logger LOGGER = Logger.getLogger(ChatManager.class.getName());

    private static final Map<XMPPConnection, ChatManager> INSTANCES = new WeakHashMap<XMPPConnection, ChatManager>();

    /**
     * Sets the default behaviour for allowing 'normal' messages to be used in chats. As some clients don't set
     * the message type to chat, the type normal has to be accepted to allow chats with these clients.
     */
    private static boolean defaultIsNormalInclude = true;

    /**
     * Sets the default behaviour for how to match chats when there is NO thread id in the incoming message.
     */
    private static MatchMode defaultMatchMode = MatchMode.BARE_JID;

    /**
     * Returns the ChatManager instance associated with a given XMPPConnection.
     *
     * @param connection the connection used to look for the proper ServiceDiscoveryManager.
     * @return the ChatManager associated with a given XMPPConnection.
     */
    public static synchronized ChatManager getInstanceFor(XMPPConnection connection) {
        ChatManager manager = INSTANCES.get(connection);
        if (manager == null)
            manager = new ChatManager(connection);
        return manager;
    }

    /**
     * Defines the different modes under which a match will be attempted with an existing chat when
     * the incoming message does not have a thread id.
     */
    public enum MatchMode {
        /**
         * Will not attempt to match, always creates a new chat.
         */
        NONE,
        /**
         * Will match on the JID in the from field of the message. 
         */
        SUPPLIED_JID,
        /**
         * Will attempt to match on the JID in the from field, and then attempt the base JID if no match was found.
         * This is the most lenient matching.
         */
        BARE_JID;
    }

    private final StanzaFilter packetFilter = new OrFilter(MessageTypeFilter.CHAT, new FlexibleStanzaTypeFilter<Message>() {

        @Override
        protected boolean acceptSpecific(Message message) {
            return normalIncluded ? message.getType() == Type.normal : false;
        }

    });

    /**
     * Determines whether incoming messages of type normal can create chats. 
     */
    private boolean normalIncluded = defaultIsNormalInclude;

    /**
     * Determines how incoming message with no thread will be matched to existing chats.
     */
    private MatchMode matchMode = defaultMatchMode;

    /**
     * Maps thread ID to chat.
     */
    private Map<String, Chat> threadChats = new ConcurrentHashMap<>();

    /**
     * Maps jids to chats
     */
    private Map<Jid, Chat> jidChats = new ConcurrentHashMap<>();

    /**
     * Maps base jids to chats
     */
    private Map<EntityBareJid, Chat> baseJidChats = new ConcurrentHashMap<>();

    private Set<ChatManagerListener> chatManagerListeners
            = new CopyOnWriteArraySet<ChatManagerListener>();

    private Map<MessageListener, StanzaFilter> interceptors
            = new WeakHashMap<MessageListener, StanzaFilter>();

    private ChatManager(XMPPConnection connection) {
        super(connection);

        // Add a listener for all message packets so that we can deliver
        // messages to the best Chat instance available.
        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Message message = (Message) packet;
                Chat chat;
                if (message.getThread() == null) {
                    // CHECKSTYLE:OFF
                	chat = getUserChat(message.getFrom());
                    // CHECKSTYLE:ON
                }
                else {
                    chat = getThreadChat(message.getThread());
                }

                if(chat == null) {
                    chat = createChat(message);
                }
                // The chat could not be created, abort here
                if (chat == null)
                    return;
                deliverMessage(chat, message);
            }
        }, packetFilter);
        INSTANCES.put(connection, this);
    }

    /**
     * Determines whether incoming messages of type <i>normal</i> will be used for creating new chats or matching
     * a message to existing ones.
     * 
     * @return true if normal is allowed, false otherwise.
     */
    public boolean isNormalIncluded() {
        return normalIncluded;
    }

    /**
     * Sets whether to allow incoming messages of type <i>normal</i> to be used for creating new chats or matching
     * a message to an existing one.
     * 
     * @param normalIncluded true to allow normal, false otherwise.
     */
    public void setNormalIncluded(boolean normalIncluded) {
        this.normalIncluded = normalIncluded;
    }

    /**
     * Gets the current mode for matching messages with <b>NO</b> thread id to existing chats.
     * 
     * @return The current mode.
     */
    public MatchMode getMatchMode() {
        return matchMode;
    }

    /**
     * Sets the mode for matching messages with <b>NO</b> thread id to existing chats.
     * 
     * @param matchMode The mode to set.
     */
    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    /**
     * Creates a new chat and returns it.
     *
     * @param userJID the user this chat is with.
     * @return the created chat.
     */
    public Chat createChat(EntityJid userJID) {
        return createChat(userJID, null);
    }

    /**
     * Creates a new chat and returns it.
     *
     * @param userJID the user this chat is with.
     * @param listener the optional listener which will listen for new messages from this chat.
     * @return the created chat.
     */
    public Chat createChat(EntityJid userJID, ChatMessageListener listener) {
        return createChat(userJID, null, listener);
    }

    /**
     * Creates a new chat using the specified thread ID, then returns it.
     * 
     * @param userJID the jid of the user this chat is with
     * @param thread the thread of the created chat.
     * @param listener the optional listener to add to the chat
     * @return the created chat.
     */
    public Chat createChat(EntityJid userJID, String thread, ChatMessageListener listener) {
        if (thread == null) {
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

    private Chat createChat(EntityJid userJID, String threadID, boolean createdLocally) {
        Chat chat = new Chat(this, userJID, threadID);
        threadChats.put(threadID, chat);
        jidChats.put(userJID, chat);
        baseJidChats.put(userJID.asEntityBareJid(), chat);

        for(ChatManagerListener listener : chatManagerListeners) {
            listener.chatCreated(chat, createdLocally);
        }

        return chat;
    }

    void closeChat(Chat chat) {
        threadChats.remove(chat.getThreadID());
        EntityJid userJID = chat.getParticipant();
        jidChats.remove(userJID);
        baseJidChats.remove(userJID.asEntityBareJid());
    }

    /**
     * Creates a new {@link Chat} based on the message. May returns null if no chat could be
     * created, e.g. because the message comes without from.
     *
     * @param message
     * @return a Chat or null if none can be created
     */
    private Chat createChat(Message message) {
        Jid from = message.getFrom();
        // According to RFC6120 8.1.2.1 4. messages without a 'from' attribute are valid, but they
        // are of no use in this case for ChatManager
        if (from == null) {
            return null;
        }

        EntityJid userJID = from.asEntityJidIfPossible();
        if (userJID == null) {
            LOGGER.warning("Message from JID without localpart: '" +message.toXML() + "'");
            return null;
        }
        String threadID = message.getThread();
        if(threadID == null) {
            threadID = nextID();
        }

        return createChat(userJID, threadID, false);
    }

    /**
     * Try to get a matching chat for the given user JID, based on the {@link MatchMode}.
     * <li>NONE - return null
     * <li>SUPPLIED_JID - match the jid in the from field of the message exactly.
     * <li>BARE_JID - if not match for from field, try the bare jid. 
     * 
     * @param userJID jid in the from field of message.
     * @return Matching chat, or null if no match found.
     */
    private Chat getUserChat(Jid userJID) {
        if (matchMode == MatchMode.NONE) {
            return null;
        }
        // According to RFC6120 8.1.2.1 4. messages without a 'from' attribute are valid, but they
        // are of no use in this case for ChatManager
        if (userJID == null) {
            return null;
        }
        Chat match = jidChats.get(userJID);

        if (match == null && (matchMode == MatchMode.BARE_JID)) {
            EntityBareJid entityBareJid = userJID.asEntityBareJidIfPossible();
            if (entityBareJid != null) {
                match = baseJidChats.get(entityBareJid);
            }
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
     * Returns an unmodifiable set of all chat listeners currently registered with this
     * manager.
     *
     * @return an unmodifiable collection of all chat listeners currently registered with this
     * manager.
     */
    public Set<ChatManagerListener> getChatListeners() {
        return Collections.unmodifiableSet(chatManagerListeners);
    }

    private static void deliverMessage(Chat chat, Message message) {
        // Here we will run any interceptors
        chat.deliver(message);
    }

    void sendMessage(Chat chat, Message message) throws NotConnectedException, InterruptedException {
        for(Map.Entry<MessageListener, StanzaFilter> interceptor : interceptors.entrySet()) {
            StanzaFilter filter = interceptor.getValue();
            if(filter != null && filter.accept(message)) {
                interceptor.getKey().processMessage(message);
            }
        }
        connection().sendStanza(message);
    }

    StanzaCollector createStanzaCollector(Chat chat) {
        return connection().createStanzaCollector(new AndFilter(new ThreadFilter(chat.getThreadID()), 
                        FromMatchesFilter.create(chat.getParticipant())));
    }

    /**
     * Adds an interceptor which intercepts any messages sent through chats.
     *
     * @param messageInterceptor the interceptor.
     */
    public void addOutgoingMessageInterceptor(MessageListener messageInterceptor) {
        addOutgoingMessageInterceptor(messageInterceptor, null);
    }

    public void addOutgoingMessageInterceptor(MessageListener messageInterceptor, StanzaFilter filter) {
        if (messageInterceptor == null) {
            return;
        }
        interceptors.put(messageInterceptor, filter);
    }

    /**
     * Returns a unique id.
     *
     * @return the next id.
     */
    private static String nextID() {
        return UUID.randomUUID().toString();
    }

    public static void setDefaultMatchMode(MatchMode mode) {
        defaultMatchMode = mode;
    }

    public static void setDefaultIsNormalIncluded(boolean allowNormal) {
        defaultIsNormalInclude = allowNormal;
    }
}
