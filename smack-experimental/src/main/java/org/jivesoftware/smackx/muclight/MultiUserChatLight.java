/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.muclight;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.muclight.element.MUCLightAffiliationsIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightChangeAffiliationsIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightConfigurationIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightCreateIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightDestroyIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightGetAffiliationsIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightGetConfigsIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightGetInfoIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightInfoIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightSetConfigsIQ;

import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;

/**
 * MUCLight class.
 *
 * @author Fernando Ramirez
 */
public class MultiUserChatLight {

    public static final String NAMESPACE = "urn:xmpp:muclight:0";

    public static final String AFFILIATIONS = "#affiliations";
    public static final String INFO = "#info";
    public static final String CONFIGURATION = "#configuration";
    public static final String CREATE = "#create";
    public static final String DESTROY = "#destroy";
    public static final String BLOCKING = "#blocking";

    private final XMPPConnection connection;
    private final EntityJid room;

    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<MessageListener>();

    /**
     * This filter will match all stanzas send from the groupchat or from one if
     * the groupchat occupants.
     */
    private final StanzaFilter fromRoomFilter;

    /**
     * Same as {@link #fromRoomFilter} together with
     * {@link MessageTypeFilter#GROUPCHAT}.
     */
    private final StanzaFilter fromRoomGroupChatFilter;

    private final StanzaListener messageListener;

    private StanzaCollector messageCollector;

    MultiUserChatLight(XMPPConnection connection, EntityJid room) {
        this.connection = connection;
        this.room = room;

        fromRoomFilter = FromMatchesFilter.create(room);
        fromRoomGroupChatFilter = new AndFilter(fromRoomFilter, MessageTypeFilter.GROUPCHAT);

        messageListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException {
                Message message = (Message) packet;
                for (MessageListener listener : messageListeners) {
                    listener.processMessage(message);
                }
            }
        };

        connection.addSyncStanzaListener(messageListener, fromRoomGroupChatFilter);
    }

    /**
     * Returns the JID of the room.
     *
     * @return the MUCLight room JID.
     */
    public EntityJid getRoom() {
        return room;
    }

    /**
     * Sends a message to the chat room.
     *
     * @param text TODO javadoc me please
     *            the text of the message to send.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendMessage(String text) throws NotConnectedException, InterruptedException {
        MessageBuilder message = buildMessage();
        message.setBody(text);
        connection.sendStanza(message.build());
    }

    /**
     * Returns a new Chat for sending private messages to a given room occupant.
     * The Chat's occupant address is the room's JID (i.e.
     * roomName@service/nick). The server service will change the 'from' address
     * to the sender's room JID and delivering the message to the intended
     * recipient's full JID.
     *
     * @param occupant TODO javadoc me please
     *            occupant unique room JID (e.g.
     *            'darkcave@macbeth.shakespeare.lit/Paul').
     * @param listener TODO javadoc me please
     *            the listener is a message listener that will handle messages
     *            for the newly created chat.
     * @return new Chat for sending private messages to a given room occupant.
     */
    @Deprecated
    // Do not re-use Chat API, which was designed for XMPP-IM 1:1 chats and not MUClight private chats.
    public org.jivesoftware.smack.chat.Chat createPrivateChat(EntityJid occupant, ChatMessageListener listener) {
        return org.jivesoftware.smack.chat.ChatManager.getInstanceFor(connection).createChat(occupant, listener);
    }

    /**
     * Creates a new Message to send to the chat room.
     *
     * @return a new Message addressed to the chat room.
     * @deprecated use {@link #buildMessage()} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public Message createMessage() {
        return connection.getStanzaFactory().buildMessageStanza()
                .ofType(Message.Type.groupchat)
                .to(room)
                .build();
    }

    /**
     * Constructs a new message builder for messages send to this MUC room.
     *
     * @return a new message builder.
     */
    public MessageBuilder buildMessage() {
        return connection.getStanzaFactory()
                .buildMessageStanza()
                .ofType(Message.Type.groupchat)
                .to(room)
                ;
    }

    /**
     * Sends a Message to the chat room.
     *
     * @param message TODO javadoc me please
     *            the message.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @deprecated use {@link #sendMessage(MessageBuilder)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public void sendMessage(Message message) throws NotConnectedException, InterruptedException {
        sendMessage(message.asBuilder());
    }

    /**
     * Sends a Message to the chat room.
     *
     * @param messageBuilder the message.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendMessage(MessageBuilder messageBuilder) throws NotConnectedException, InterruptedException {
        Message message = messageBuilder.to(room).ofType(Message.Type.groupchat).build();
        connection.sendStanza(message);
    }

    /**
     * Polls for and returns the next message.
     *
     * @return the next message if one is immediately available
     */
    public Message pollMessage() {
        return messageCollector.pollResult();
    }

    /**
     * Returns the next available message in the chat. The method call will
     * block (not return) until a message is available.
     *
     * @return the next message.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Message nextMessage() throws InterruptedException {
        return messageCollector.nextResult();
    }

    /**
     * Returns the next available message in the chat.
     *
     * @param timeout TODO javadoc me please
     *            the maximum amount of time to wait for the next message.
     * @return the next message, or null if the timeout elapses without a
     *         message becoming available.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Message nextMessage(long timeout) throws InterruptedException {
        return messageCollector.nextResult(timeout);
    }

    /**
     * Adds a stanza listener that will be notified of any new messages
     * in the group chat. Only "group chat" messages addressed to this group
     * chat will be delivered to the listener.
     *
     * @param listener TODO javadoc me please
     *            a stanza listener.
     * @return true if the listener was not already added.
     */
    public boolean addMessageListener(MessageListener listener) {
        return messageListeners.add(listener);
    }

    /**
     * Removes a stanza listener that was being notified of any new
     * messages in the MUCLight. Only "group chat" messages addressed to this
     * MUCLight were being delivered to the listener.
     *
     * @param listener TODO javadoc me please
     *            a stanza listener.
     * @return true if the listener was removed, otherwise the listener was not
     *         added previously.
     */
    public boolean removeMessageListener(MessageListener listener) {
        return messageListeners.remove(listener);
    }

    /**
     * Remove the connection callbacks used by this MUC Light from the
     * connection.
     */
    private void removeConnectionCallbacks() {
        connection.removeSyncStanzaListener(messageListener);
        if (messageCollector != null) {
            messageCollector.cancel();
            messageCollector = null;
        }
    }

    @Override
    public String toString() {
        return "MUC Light: " + room + "(" + connection.getUser() + ")";
    }

    /**
     * Create new MUCLight.
     *
     * @param roomName TODO javadoc me please
     * @param subject TODO javadoc me please
     * @param customConfigs TODO javadoc me please
     * @param occupants TODO javadoc me please
     * @throws Exception TODO javadoc me please
     */
    public void create(String roomName, String subject, HashMap<String, String> customConfigs, List<Jid> occupants)
            throws Exception {
        MUCLightCreateIQ createMUCLightIQ = new MUCLightCreateIQ(room, roomName, occupants);

        messageCollector = connection.createStanzaCollector(fromRoomGroupChatFilter);

        try {
            connection.sendIqRequestAndWaitForResponse(createMUCLightIQ);
        } catch (NotConnectedException | InterruptedException | NoResponseException | XMPPErrorException e) {
            removeConnectionCallbacks();
            throw e;
        }
    }

    /**
     * Create new MUCLight.
     *
     * @param roomName TODO javadoc me please
     * @param occupants TODO javadoc me please
     * @throws Exception TODO javadoc me please
     */
    public void create(String roomName, List<Jid> occupants) throws Exception {
        create(roomName, null, null, occupants);
    }

    /**
     * Leave the MUCLight.
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     */
    public void leave() throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
        affiliations.put(connection.getUser(), MUCLightAffiliation.none);

        MUCLightChangeAffiliationsIQ changeAffiliationsIQ = new MUCLightChangeAffiliationsIQ(room, affiliations);
        IQ responseIq = connection.sendIqRequestAndWaitForResponse(changeAffiliationsIQ);
        boolean roomLeft = responseIq.getType().equals(IQ.Type.result);

        if (roomLeft) {
            removeConnectionCallbacks();
        }
    }

    /**
     * Get the MUC Light info.
     *
     * @param version TODO javadoc me please
     * @return the room info
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public MUCLightRoomInfo getFullInfo(String version)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightGetInfoIQ mucLightGetInfoIQ = new MUCLightGetInfoIQ(room, version);

        IQ responseIq = connection.sendIqRequestAndWaitForResponse(mucLightGetInfoIQ);
        MUCLightInfoIQ mucLightInfoResponseIQ = (MUCLightInfoIQ) responseIq;

        return new MUCLightRoomInfo(mucLightInfoResponseIQ.getVersion(), room,
                mucLightInfoResponseIQ.getConfiguration(), mucLightInfoResponseIQ.getOccupants());
    }

    /**
     * Get the MUC Light info.
     *
     * @return the room info
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public MUCLightRoomInfo getFullInfo()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getFullInfo(null);
    }

    /**
     * Get the MUC Light configuration.
     *
     * @param version TODO javadoc me please
     * @return the room configuration
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public MUCLightRoomConfiguration getConfiguration(String version)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightGetConfigsIQ mucLightGetConfigsIQ = new MUCLightGetConfigsIQ(room, version);
        IQ responseIq = connection.sendIqRequestAndWaitForResponse(mucLightGetConfigsIQ);
        MUCLightConfigurationIQ mucLightConfigurationIQ = (MUCLightConfigurationIQ) responseIq;
        return mucLightConfigurationIQ.getConfiguration();
    }

    /**
     * Get the MUC Light configuration.
     *
     * @return the room configuration
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public MUCLightRoomConfiguration getConfiguration()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getConfiguration(null);
    }

    /**
     * Get the MUC Light affiliations.
     *
     * @param version TODO javadoc me please
     * @return the room affiliations
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public HashMap<Jid, MUCLightAffiliation> getAffiliations(String version)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightGetAffiliationsIQ mucLightGetAffiliationsIQ = new MUCLightGetAffiliationsIQ(room, version);

        IQ responseIq = connection.sendIqRequestAndWaitForResponse(mucLightGetAffiliationsIQ);
        MUCLightAffiliationsIQ mucLightAffiliationsIQ = (MUCLightAffiliationsIQ) responseIq;

        return mucLightAffiliationsIQ.getAffiliations();
    }

    /**
     * Get the MUC Light affiliations.
     *
     * @return the room affiliations
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public HashMap<Jid, MUCLightAffiliation> getAffiliations()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getAffiliations(null);
    }

    /**
     * Change the MUC Light affiliations.
     *
     * @param affiliations TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void changeAffiliations(HashMap<Jid, MUCLightAffiliation> affiliations)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightChangeAffiliationsIQ changeAffiliationsIQ = new MUCLightChangeAffiliationsIQ(room, affiliations);
        connection.sendIqRequestAndWaitForResponse(changeAffiliationsIQ);
    }

    /**
     * Destroy the MUC Light. Only will work if it is requested by the owner.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void destroy() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightDestroyIQ mucLightDestroyIQ = new MUCLightDestroyIQ(room);
        IQ responseIq = connection.sendIqRequestAndWaitForResponse(mucLightDestroyIQ);
        boolean roomDestroyed = responseIq.getType().equals(IQ.Type.result);

        if (roomDestroyed) {
            removeConnectionCallbacks();
        }
    }

    /**
     * Change the subject of the MUC Light.
     *
     * @param subject TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void changeSubject(String subject)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightSetConfigsIQ mucLightSetConfigIQ = new MUCLightSetConfigsIQ(room, null, subject, null);
        connection.sendIqRequestAndWaitForResponse(mucLightSetConfigIQ);
    }

    /**
     * Change the name of the room.
     *
     * @param roomName TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void changeRoomName(String roomName)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightSetConfigsIQ mucLightSetConfigIQ = new MUCLightSetConfigsIQ(room, roomName, null);
        connection.sendIqRequestAndWaitForResponse(mucLightSetConfigIQ);
    }

    /**
     * Set the room configurations.
     *
     * @param customConfigs TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setRoomConfigs(HashMap<String, String> customConfigs)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        setRoomConfigs(null, customConfigs);
    }

    /**
     * Set the room configurations.
     *
     * @param roomName TODO javadoc me please
     * @param customConfigs TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setRoomConfigs(String roomName, HashMap<String, String> customConfigs)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightSetConfigsIQ mucLightSetConfigIQ = new MUCLightSetConfigsIQ(room, roomName, customConfigs);
        connection.sendIqRequestAndWaitForResponse(mucLightSetConfigIQ);
    }

}
