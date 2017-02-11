/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smack.chat2;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromTypeFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.ToTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.AbstractRosterListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

/**
 * A chat manager for 1:1 XMPP instant messaging chats.
 * <p>
 * This manager and the according {@link Chat} API implement "Resource Locking" (XEP-0296). Support for Carbon Copies
 * (XEP-0280) will be added once the XEP has progressed from experimental.
 * </p>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0296.html">XEP-0296: Best Practices for Resource Locking</a>
 */
@SuppressWarnings("FunctionalInterfaceClash")
public final class ChatManager extends Manager {

    private static final Map<XMPPConnection, ChatManager> INSTANCES = new WeakHashMap<>();

    public static synchronized ChatManager getInstanceFor(XMPPConnection connection) {
        ChatManager chatManager = INSTANCES.get(connection);
        if (chatManager == null) {
            chatManager = new ChatManager(connection);
            INSTANCES.put(connection, chatManager);
        }
        return chatManager;
    }

    // @FORMATTER:OFF
    private static final StanzaFilter MESSAGE_FILTER = new AndFilter(
                    MessageTypeFilter.NORMAL_OR_CHAT,
                    new OrFilter(MessageWithBodiesFilter.INSTANCE, new StanzaExtensionFilter(XHTMLExtension.ELEMENT, XHTMLExtension.NAMESPACE))
                    );

    private static final StanzaFilter OUTGOING_MESSAGE_FILTER = new AndFilter(
                    MESSAGE_FILTER,
                    ToTypeFilter.ENTITY_FULL_OR_BARE_JID
                    );

    private static final StanzaFilter INCOMING_MESSAGE_FILTER = new AndFilter(
                    MESSAGE_FILTER,
                    FromTypeFilter.ENTITY_FULL_JID
                    );
    // @FORMATTER:ON

    private final Map<EntityBareJid, Chat> chats = new ConcurrentHashMap<>();

    private final Set<IncomingChatMessageListener> incomingListeners = new CopyOnWriteArraySet<>();

    private final Set<OutgoingChatMessageListener> outgoingListeners = new CopyOnWriteArraySet<>();

    private boolean xhtmlIm;

    private ChatManager(final XMPPConnection connection) {
        super(connection);
        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                Message message = (Message) stanza;
                if (!shouldAcceptMessage(message)) {
                    return;
                }

                final Jid from = message.getFrom();
                final EntityFullJid fullFrom = from.asEntityFullJidOrThrow();
                final EntityBareJid bareFrom = fullFrom.asEntityBareJid();
                final Chat chat = chatWith(bareFrom);
                chat.lockedResource = fullFrom;

                for (IncomingChatMessageListener listener : incomingListeners) {
                    listener.newIncomingMessage(bareFrom, message, chat);
                }
            }
        }, INCOMING_MESSAGE_FILTER);

        connection.addPacketInterceptor(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
                Message message = (Message) stanza;
                if (!shouldAcceptMessage(message)) {
                    return;
                }

                final EntityBareJid to = message.getTo().asEntityBareJidOrThrow();
                final Chat chat = chatWith(to);

                for (OutgoingChatMessageListener listener : outgoingListeners) {
                    listener.newOutgoingMessage(to, message, chat);
                }
            }
        }, OUTGOING_MESSAGE_FILTER);

        Roster roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(new AbstractRosterListener() {
            @Override
            public void presenceChanged(Presence presence) {
                final Jid from = presence.getFrom();
                final EntityBareJid bareFrom = from.asEntityBareJidIfPossible();
                if (bareFrom == null) {
                    return;
                }

                final Chat chat = chats.get(bareFrom);
                if (chat == null) {
                    return;
                }

                if (chat.lockedResource == null) {
                    // According to XEP-0296, no action is required for resource locking upon receiving a presence if no
                    // resource is currently locked.
                    return;
                }

                final EntityFullJid fullFrom = from.asEntityFullJidIfPossible();
                if (!chat.lockedResource.equals(fullFrom)) {
                    return;
                }

                if (chat.lastPresenceOfLockedResource == null) {
                    // We have no last known presence from the locked resource.
                    chat.lastPresenceOfLockedResource = presence;
                    return;
                }

                if (chat.lastPresenceOfLockedResource.getMode() != presence.getMode()
                                || chat.lastPresenceOfLockedResource.getType() != presence.getType()) {
                    chat.unlockResource();
                }
            }
        });
    }

    private boolean shouldAcceptMessage(Message message) {
        if (!message.getBodies().isEmpty()) {
            return true;
        }

        // Message has no XMPP-IM bodies, abort here if xhtmlIm is not enabled.
        if (!xhtmlIm) {
            return false;
        }

        XHTMLExtension xhtmlExtension = XHTMLExtension.from(message);
        if (xhtmlExtension == null) {
            // Message has no XHTML-IM extension, abort.
            return false;
        }
        return true;
    }

    /**
     * Add a new listener for incoming chat messages.
     *
     * @param listener the listener to add.
     * @return <code>true</code> if the listener was not already added.
     */
    public boolean addIncomingListener(IncomingChatMessageListener listener) {
        return incomingListeners.add(listener);
    }

    /**
     * Add a new listener for incoming chat messages.
     *
     * @param listener the listener to add.
     * @return <code>true</code> if the listener was not already added.
     */
    @Deprecated
    @SuppressWarnings("FunctionalInterfaceClash")
    public boolean addListener(IncomingChatMessageListener listener) {
        return addIncomingListener(listener);
    }

    /**
     * Remove an incoming chat message listener.
     *
     * @param listener the listener to remove.
     * @return <code>true</code> if the listener was active and got removed.
     */
    @SuppressWarnings("FunctionalInterfaceClash")
    public boolean removeListener(IncomingChatMessageListener listener) {
        return incomingListeners.remove(listener);
    }

    /**
     * Add a new listener for outgoing chat messages.
     *
     * @param listener the listener to add.
     * @return <code>true</code> if the listener was not already added.
     */
    public boolean addOutgoingListener(OutgoingChatMessageListener listener) {
        return outgoingListeners.add(listener);
    }

    /**
     * Add a new listener for incoming chat messages.
     *
     * @param listener the listener to add.
     * @return <code>true</code> if the listener was not already added.
     * @deprecated use {@link #addOutgoingListener(OutgoingChatMessageListener)} instead.
     */
    @Deprecated
    @SuppressWarnings("FunctionalInterfaceClash")
    public boolean addListener(OutgoingChatMessageListener listener) {
        return addOutgoingListener(listener);
    }

    /**
     * Remove an outgoing chat message listener.
     *
     * @param listener the listener to remove.
     * @return <code>true</code> if the listener was active and got removed.
     */
    public boolean removeListener(OutgoingChatMessageListener listener) {
        return outgoingListeners.remove(listener);
    }

    /**
     * Remove an outgoing chat message listener.
     *
     * @param listener the listener to remove.
     * @return <code>true</code> if the listener was active and got removed.
     * @deprecated use {@link #removeListener(OutgoingChatMessageListener)} instead.
     */
    @Deprecated
    public boolean removeOutoingLIstener(OutgoingChatMessageListener listener) {
        return removeListener(listener);
    }

    /**
     * Start a new or retrieve the existing chat with <code>jid</code>.
     *
     * @param jid the XMPP address of the other entity to chat with.
     * @return the Chat API for the given XMPP address.
     */
    public Chat chatWith(EntityBareJid jid) {
        Chat chat = chats.get(jid);
        if (chat == null) {
            synchronized (chats) {
                // Double-checked locking.
                chat = chats.get(jid);
                if (chat != null) {
                    return chat;
                }
                chat = new Chat(connection(), jid);
                chats.put(jid, chat);
            }
        }
        return chat;
    }

    /**
     * Also notify about messages containing XHTML-IM.
     *
     * @param xhtmlIm
     */
    public void setXhmtlImEnabled(boolean xhtmlIm) {
        this.xhtmlIm = xhtmlIm;
    }
}
