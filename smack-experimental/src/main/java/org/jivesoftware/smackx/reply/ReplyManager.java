/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reply;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.reply.element.ReplyElement;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

/**
 * Smacks API for XEP-0461: Message Replies.
 * This extension defines a method for replying to XMPP messages in a standardized way.
 * It allows senders to explicitly acknowledge receipt of a message or provide a reply,
 * which can be especially useful in scenarios where the original sender expects a response.
 * The reply may include metadata or content that clarifies the context of the message reply.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0461.html">XEP-0461: Message Replies</a>
 */
public final class ReplyManager extends Manager {

    private static final Map<XMPPConnection, ReplyManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private final Set<ReplyListener> listeners = new CopyOnWriteArraySet<>();
    private final AsyncButOrdered<BareJid> asyncButOrdered = new AsyncButOrdered<>();
    private final StanzaFilter replyElementFilter = new AndFilter(StanzaTypeFilter.MESSAGE,
                    new StanzaExtensionFilter(ReplyElement.ELEMENT, ReplyElement.NAMESPACE));

    private void replyElementListener(Stanza packet) {
        Message message = (Message) packet;
        ReplyElement reply = ReplyElement.fromMessage(message);
        String body = message.getBody();
        asyncButOrdered.performAsyncButOrdered(message.getFrom().asBareJid(), () -> {
            for (ReplyListener l : listeners) {
                l.onReplyReceived(message, reply, body);
            }
        });
    }

    private ReplyManager(XMPPConnection connection) {
        super(connection);
        connection.addAsyncStanzaListener(this::replyElementListener, replyElementFilter);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(ReplyElement.NAMESPACE);
    }

    public static synchronized ReplyManager getInstanceFor(XMPPConnection connection) {
        ReplyManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new ReplyManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Checks if the user associated with the given JID (Jabber ID) supports message reply functionality.
     *
     * @param jid The JID of the user to check for reply support.
     * @return {@code true} if the user supports replies, {@code false} otherwise.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the XMPP connection is not established.
     * @throws InterruptedException If the process is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public boolean userSupportsReplies(EntityBareJid jid) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
                    SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection())
                        .supportsFeature(jid, ReplyElement.NAMESPACE);

    }

    /**
     * Checks if the XMPP server supports replies for messages.
     *
     * @return {@code true} if the server supports replies, {@code false} otherwise.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the XMPP connection is not established.
     * @throws InterruptedException If the process is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public boolean serverSupportsReplies()
                    throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
                    SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(ReplyElement.NAMESPACE);
    }

    /**
     * Adds a reply extension to the message builder with the specified reply details.
     *
     * This method creates a `ReplyElement` with the given `to` and `id` attributes,
     * and adds it as an extension to the provided `messageBuilder`.
     *
     * @param messageBuilder The message builder that will receive the reply extension.
     * @param replyTo The 'to' attribute of the reply, representing the recipient of the original message.
     * @param replyId The 'id' attribute of the reply, representing the ID of the original message being replied to.
     * @return The message builder with the reply extension added, including the specified `to` and `id` attributes.
     */
    public static MessageBuilder addReply(MessageBuilder messageBuilder, String replyTo, String replyId) {
        ReplyElement replyElement = new ReplyElement(replyTo, replyId);

        return messageBuilder.addExtension(replyElement);
    }


    /**
     * Adds a reply listener for message replies.
     *
     * @param listener The listener to be added.
     * @return {@code true} if the listener was successfully added, {@code false} otherwise.
     */
    public synchronized boolean addReplyListener(ReplyListener listener) {
       return listeners.add(listener);
    }

    /**
     * Removes a reply listener for message replies.
     *
     * @param listener The listener to be removed.
     * @return {@code true} if the listener was successfully removed, {@code false} otherwise.
     */
    public synchronized boolean removeReplyListener(ReplyListener listener) {
       return listeners.remove(listener);
    }

}
