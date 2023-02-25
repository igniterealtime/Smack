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

package org.jivesoftware.smackx.workgroup.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smackx.workgroup.QueueUser;
import org.jivesoftware.smackx.workgroup.WorkgroupInvitation;
import org.jivesoftware.smackx.workgroup.WorkgroupInvitationListener;
import org.jivesoftware.smackx.workgroup.ext.history.AgentChatHistory;
import org.jivesoftware.smackx.workgroup.ext.history.ChatMetadata;
import org.jivesoftware.smackx.workgroup.ext.macros.MacroGroup;
import org.jivesoftware.smackx.workgroup.ext.macros.Macros;
import org.jivesoftware.smackx.workgroup.ext.notes.ChatNotes;
import org.jivesoftware.smackx.workgroup.packet.AgentStatus;
import org.jivesoftware.smackx.workgroup.packet.DepartQueuePacket;
import org.jivesoftware.smackx.workgroup.packet.MonitorPacket;
import org.jivesoftware.smackx.workgroup.packet.OccupantsInfo;
import org.jivesoftware.smackx.workgroup.packet.OfferRequestProvider;
import org.jivesoftware.smackx.workgroup.packet.OfferRevokeProvider;
import org.jivesoftware.smackx.workgroup.packet.QueueDetails;
import org.jivesoftware.smackx.workgroup.packet.QueueOverview;
import org.jivesoftware.smackx.workgroup.packet.RoomInvitation;
import org.jivesoftware.smackx.workgroup.packet.RoomTransfer;
import org.jivesoftware.smackx.workgroup.packet.SessionID;
import org.jivesoftware.smackx.workgroup.packet.Transcript;
import org.jivesoftware.smackx.workgroup.packet.Transcripts;
import org.jivesoftware.smackx.workgroup.settings.GenericSettings;
import org.jivesoftware.smackx.workgroup.settings.SearchSettings;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * This class embodies the agent's active presence within a given workgroup. The application
 * should have N instances of this class, where N is the number of workgroups to which the
 * owning agent of the application belongs. This class provides all functionality that a
 * session within a given workgroup is expected to have from an agent's perspective -- setting
 * the status, tracking the status of queues to which the agent belongs within the workgroup, and
 * dequeuing customers.
 *
 * @author Matt Tucker
 * @author Derek DeMoro
 */
public class AgentSession {
    private static final Logger LOGGER = Logger.getLogger(AgentSession.class.getName());

    private final XMPPConnection connection;

    private final EntityBareJid workgroupJID;

    private boolean online = false;
    private Presence.Mode presenceMode;
    private int maxChats;
    private final Map<String, List<String>> metaData;

    private final Map<Resourcepart, WorkgroupQueue> queues = new HashMap<>();

    private final List<OfferListener> offerListeners;
    private final List<WorkgroupInvitationListener> invitationListeners;
    private final List<QueueUsersListener> queueUsersListeners;

    private AgentRoster agentRoster = null;
    private final TranscriptManager transcriptManager;
    private final TranscriptSearchManager transcriptSearchManager;
    private final Agent agent;
    private final StanzaListener packetListener;

    /**
     * Constructs a new agent session instance. Note, the {@link #setOnline(boolean)}
     * method must be called with an argument of <code>true</code> to mark the agent
     * as available to accept chat requests.
     *
     * @param connection   a connection instance which must have already gone through
     *                     authentication.
     * @param workgroupJID the fully qualified JID of the workgroup.
     */
    public AgentSession(EntityBareJid workgroupJID, XMPPConnection connection) {
        // Login must have been done before passing in connection.
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must login to server before creating workgroup.");
        }

        this.workgroupJID = workgroupJID;
        this.connection = connection;
        this.transcriptManager = new TranscriptManager(connection);
        this.transcriptSearchManager = new TranscriptSearchManager(connection);

        this.maxChats = -1;

        this.metaData = new HashMap<>();

        offerListeners = new ArrayList<>();
        invitationListeners = new ArrayList<>();
        queueUsersListeners = new ArrayList<>();

        // Create a filter to listen for packets we're interested in.
        OrFilter filter = new OrFilter(
                        new StanzaTypeFilter(Presence.class),
                        new StanzaTypeFilter(Message.class));

        packetListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                try {
                    handlePacket(packet);
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing packet", e);
                }
            }
        };
        connection.addAsyncStanzaListener(packetListener, filter);

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(
                OfferRequestProvider.OfferRequestPacket.ELEMENT,
                OfferRequestProvider.OfferRequestPacket.NAMESPACE, IQ.Type.set,
                Mode.async) {

            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                // Acknowledge the IQ set.
                IQ reply = IQ.createResultIQ(iqRequest);

                fireOfferRequestEvent((OfferRequestProvider.OfferRequestPacket) iqRequest);
                return reply;
            }
        });

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(
                OfferRevokeProvider.OfferRevokePacket.ELEMENT,
                OfferRevokeProvider.OfferRevokePacket.NAMESPACE, IQ.Type.set,
                Mode.async) {

            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                // Acknowledge the IQ set.
                IQ reply = IQ.createResultIQ(iqRequest);

                fireOfferRevokeEvent((OfferRevokeProvider.OfferRevokePacket) iqRequest);
                return reply;
            }
        });

        // Create the agent associated to this session
        agent = new Agent(connection, workgroupJID);
    }

    /**
     * Close the agent session. The underlying connection will remain opened but the
     * stanza listeners that were added by this agent session will be removed.
     */
    public void close() {
        connection.removeAsyncStanzaListener(packetListener);
    }

    /**
     * Returns the agent roster for the workgroup, which contains.
     *
     * @return the AgentRoster
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public AgentRoster getAgentRoster() throws NotConnectedException, InterruptedException {
        if (agentRoster == null) {
            agentRoster = new AgentRoster(connection, workgroupJID);
        }

        // This might be the first time the user has asked for the roster. If so, we
        // want to wait up to 2 seconds for the server to send back the list of agents.
        // This behavior shields API users from having to worry about the fact that the
        // operation is asynchronous, although they'll still have to listen for changes
        // to the roster.
        int elapsed = 0;
        while (!agentRoster.rosterInitialized && elapsed <= 2000) {
            try {
                Thread.sleep(500);
            }
            catch (Exception e) {
                // Ignore
            }
            elapsed += 500;
        }
        return agentRoster;
    }

    /**
     * Returns the agent's current presence mode.
     *
     * @return the agent's current presence mode.
     */
    public Presence.Mode getPresenceMode() {
        return presenceMode;
    }

    /**
     * Returns the maximum number of chats the agent can participate in.
     *
     * @return the maximum number of chats the agent can participate in.
     */
    public int getMaxChats() {
        return maxChats;
    }

    /**
     * Returns true if the agent is online with the workgroup.
     *
     * @return true if the agent is online with the workgroup.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Allows the addition of a new key-value pair to the agent's meta data, if the value is
     * new data, the revised meta data will be rebroadcast in an agent's presence broadcast.
     *
     * @param key the meta data key
     * @param val the non-null meta data value
     * @throws XMPPException if an exception occurs.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setMetaData(String key, String val) throws XMPPException, SmackException, InterruptedException {
        synchronized (this.metaData) {
            List<String> oldVals = metaData.get(key);

            if (oldVals == null || !oldVals.get(0).equals(val)) {
                oldVals.set(0, val);

                setStatus(presenceMode, maxChats);
            }
        }
    }

    /**
     * Allows the removal of data from the agent's meta data, if the key represents existing data,
     * the revised meta data will be rebroadcast in an agent's presence broadcast.
     *
     * @param key the meta data key.
     * @throws XMPPException if an exception occurs.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void removeMetaData(String key) throws XMPPException, SmackException, InterruptedException {
        synchronized (this.metaData) {
            List<String> oldVal = metaData.remove(key);

            if (oldVal != null) {
                setStatus(presenceMode, maxChats);
            }
        }
    }

    /**
     * Allows the retrieval of meta data for a specified key.
     *
     * @param key the meta data key
     * @return the meta data value associated with the key or <code>null</code> if the meta-data
     *         doesn't exist..
     */
    public List<String> getMetaData(String key) {
        return metaData.get(key);
    }

    /**
     * Sets whether the agent is online with the workgroup. If the user tries to go online with
     * the workgroup but is not allowed to be an agent, an XMPPError with error code 401 will
     * be thrown.
     *
     * @param online true to set the agent as online with the workgroup.
     * @throws XMPPException if an error occurs setting the online status.
     * @throws SmackException             assertEquals(SmackException.Type.NO_RESPONSE_FROM_SERVER, e.getType());
            return;
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setOnline(boolean online) throws XMPPException, SmackException, InterruptedException {
        // If the online status hasn't changed, do nothing.
        if (this.online == online) {
            return;
        }

        Presence presence;

        // If the user is going online...
        if (online) {
            presence = connection.getStanzaFactory().buildPresenceStanza()
                    .ofType(Presence.Type.available)
                    .to(workgroupJID)
                    .build();

            presence.addExtension(new StandardExtensionElement(AgentStatus.ELEMENT_NAME,
                    AgentStatus.NAMESPACE));

            StanzaCollector collector = this.connection.createStanzaCollectorAndSend(new AndFilter(
                            new StanzaTypeFilter(Presence.class), FromMatchesFilter.create(workgroupJID)), presence);

            presence = collector.nextResultOrThrow();

            // We can safely update this iv since we didn't get any error
            this.online = online;
        }
        // Otherwise the user is going offline...
        else {
            // Update this iv now since we don't care at this point of any error
            this.online = online;

            presence = connection.getStanzaFactory().buildPresenceStanza()
                    .ofType(Presence.Type.unavailable)
                    .to(workgroupJID)
                    .build();
            presence.addExtension(new StandardExtensionElement(AgentStatus.ELEMENT_NAME,
                    AgentStatus.NAMESPACE));
            connection.sendStanza(presence);
        }
    }

    /**
     * Sets the agent's current status with the workgroup. The presence mode affects
     * how offers are routed to the agent. The possible presence modes with their
     * meanings are as follows:<ul>
     *
     * <li>Presence.Mode.AVAILABLE -- (Default) the agent is available for more chats
     * (equivalent to Presence.Mode.CHAT).
     * <li>Presence.Mode.DO_NOT_DISTURB -- the agent is busy and should not be disturbed.
     * However, special case, or extreme urgency chats may still be offered to the agent.
     * <li>Presence.Mode.AWAY -- the agent is not available and should not
     * have a chat routed to them (equivalent to Presence.Mode.EXTENDED_AWAY).</ul>
     *
     * The max chats value is the maximum number of chats the agent is willing to have
     * routed to them at once. Some servers may be configured to only accept max chat
     * values in a certain range; for example, between two and five. In that case, the
     * maxChats value the agent sends may be adjusted by the server to a value within that
     * range.
     *
     * @param presenceMode the presence mode of the agent.
     * @param maxChats     the maximum number of chats the agent is willing to accept.
     * @throws XMPPException         if an error occurs setting the agent status.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws IllegalStateException if the agent is not online with the workgroup.
     */
    public void setStatus(Presence.Mode presenceMode, int maxChats) throws XMPPException, SmackException, InterruptedException {
        setStatus(presenceMode, maxChats, null);
    }

    /**
     * Sets the agent's current status with the workgroup. The presence mode affects how offers
     * are routed to the agent. The possible presence modes with their meanings are as follows:<ul>
     *
     * <li>Presence.Mode.AVAILABLE -- (Default) the agent is available for more chats
     * (equivalent to Presence.Mode.CHAT).
     * <li>Presence.Mode.DO_NOT_DISTURB -- the agent is busy and should not be disturbed.
     * However, special case, or extreme urgency chats may still be offered to the agent.
     * <li>Presence.Mode.AWAY -- the agent is not available and should not
     * have a chat routed to them (equivalent to Presence.Mode.EXTENDED_AWAY).</ul>
     *
     * The max chats value is the maximum number of chats the agent is willing to have routed to
     * them at once. Some servers may be configured to only accept max chat values in a certain
     * range; for example, between two and five. In that case, the maxChats value the agent sends
     * may be adjusted by the server to a value within that range.
     *
     * @param presenceMode the presence mode of the agent.
     * @param maxChats     the maximum number of chats the agent is willing to accept.
     * @param status       sets the status message of the presence update.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws IllegalStateException if the agent is not online with the workgroup.
     */
    public void setStatus(Presence.Mode presenceMode, int maxChats, String status)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (!online) {
            throw new IllegalStateException("Cannot set status when the agent is not online.");
        }

        if (presenceMode == null) {
            presenceMode = Presence.Mode.available;
        }
        this.presenceMode = presenceMode;
        this.maxChats = maxChats;

        PresenceBuilder presenceBuilder = connection.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.available)
                .setMode(presenceMode)
                .to(workgroupJID)
                .setStatus(status)
                ;

        // Send information about max chats and current chats as a packet extension.
        StandardExtensionElement.Builder builder = StandardExtensionElement.builder(AgentStatus.ELEMENT_NAME,
                AgentStatus.NAMESPACE);
        builder.addElement("max_chats", Integer.toString(maxChats));
        presenceBuilder.addExtension(builder.build());
        presenceBuilder.addExtension(new MetaData(this.metaData));

        Presence presence = presenceBuilder.build();
        StanzaCollector collector = this.connection.createStanzaCollectorAndSend(new AndFilter(
                        new StanzaTypeFilter(Presence.class),
                        FromMatchesFilter.create(workgroupJID)), presence);

        collector.nextResultOrThrow();
    }

    /**
     * Sets the agent's current status with the workgroup. The presence mode affects how offers
     * are routed to the agent. The possible presence modes with their meanings are as follows:<ul>
     *
     * <li>Presence.Mode.AVAILABLE -- (Default) the agent is available for more chats
     * (equivalent to Presence.Mode.CHAT).
     * <li>Presence.Mode.DO_NOT_DISTURB -- the agent is busy and should not be disturbed.
     * However, special case, or extreme urgency chats may still be offered to the agent.
     * <li>Presence.Mode.AWAY -- the agent is not available and should not
     * have a chat routed to them (equivalent to Presence.Mode.EXTENDED_AWAY).</ul>
     *
     * @param presenceMode the presence mode of the agent.
     * @param status       sets the status message of the presence update.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws IllegalStateException if the agent is not online with the workgroup.
     */
    public void setStatus(Presence.Mode presenceMode, String status) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (!online) {
            throw new IllegalStateException("Cannot set status when the agent is not online.");
        }

        if (presenceMode == null) {
            presenceMode = Presence.Mode.available;
        }
        this.presenceMode = presenceMode;

        PresenceBuilder presenceBuilder = connection.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.available)
                .setMode(presenceMode)
                .to(getWorkgroupJID());

        if (status != null) {
            presenceBuilder.setStatus(status);
        }

        Presence presence = presenceBuilder.build();
        presence.addExtension(new MetaData(this.metaData));

        StanzaCollector collector = this.connection.createStanzaCollectorAndSend(new AndFilter(new StanzaTypeFilter(Presence.class),
                FromMatchesFilter.create(workgroupJID)), presence);

        collector.nextResultOrThrow();
    }

    /**
     * Removes a user from the workgroup queue. This is an administrative action that the
     *
     * The agent is not guaranteed of having privileges to perform this action; an exception
     * denying the request may be thrown.
     *
     * @param userID the ID of the user to remove.
     * @throws XMPPException if an exception occurs.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void dequeueUser(EntityJid userID) throws XMPPException, NotConnectedException, InterruptedException {
        // todo: this method simply won't work right now.
        DepartQueuePacket departPacket = new DepartQueuePacket(workgroupJID, userID);

        // PENDING
        this.connection.sendStanza(departPacket);
    }

    /**
     * Returns the transcripts of a given user. The answer will contain the complete history of
     * conversations that a user had.
     *
     * @param userID the id of the user to get his conversations.
     * @return the transcripts of a given user.
     * @throws XMPPException if an error occurs while getting the information.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Transcripts getTranscripts(Jid userID) throws XMPPException, SmackException, InterruptedException {
        return transcriptManager.getTranscripts(workgroupJID, userID);
    }

    /**
     * Returns the full conversation transcript of a given session.
     *
     * @param sessionID the id of the session to get the full transcript.
     * @return the full conversation transcript of a given session.
     * @throws XMPPException if an error occurs while getting the information.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Transcript getTranscript(String sessionID) throws XMPPException, SmackException, InterruptedException {
        return transcriptManager.getTranscript(workgroupJID, sessionID);
    }

    /**
     * Returns the Form to use for searching transcripts. It is unlikely that the server
     * will change the form (without a restart) so it is safe to keep the returned form
     * for future submissions.
     *
     * @return the Form to use for searching transcripts.
     * @throws XMPPException if an error occurs while sending the request to the server.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Form getTranscriptSearchForm() throws XMPPException, SmackException, InterruptedException {
        return transcriptSearchManager.getSearchForm(workgroupJID.asDomainBareJid());
    }

    /**
     * Submits the completed form and returns the result of the transcript search. The result
     * will include all the data returned from the server so be careful with the amount of
     * data that the search may return.
     *
     * @param completedForm the filled out search form.
     * @return the result of the transcript search.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ReportedData searchTranscripts(FillableForm completedForm) throws XMPPException, SmackException, InterruptedException {
        return transcriptSearchManager.submitSearch(workgroupJID.asDomainBareJid(),
                completedForm);
    }

    /**
     * Asks the workgroup for information about the occupants of the specified room. The returned
     * information will include the real JID of the occupants, the nickname of the user in the
     * room as well as the date when the user joined the room.
     *
     * @param roomID the room to get information about its occupants.
     * @return information about the occupants of the specified room.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public OccupantsInfo getOccupantsInfo(String roomID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        OccupantsInfo request = new OccupantsInfo(roomID);
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);

        OccupantsInfo response = (OccupantsInfo) connection.sendIqRequestAndWaitForResponse(request);
        return response;
    }

    /**
     * Get workgroup JID.
     * @return the fully-qualified name of the workgroup for which this session exists
     */
    public Jid getWorkgroupJID() {
        return workgroupJID;
    }

    /**
     * Returns the Agent associated to this session.
     *
     * @return the Agent associated to this session.
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * Get queue.
     *
     * @param queueName the name of the queue
     * @return an instance of WorkgroupQueue for the argument queue name, or null if none exists
     */
    public WorkgroupQueue getQueue(String queueName) {
        Resourcepart queueNameResourcepart;
        try {
            queueNameResourcepart = Resourcepart.from(queueName);
        }
        catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }
        return getQueue(queueNameResourcepart);
    }

    /**
     * Get queue.
     *
     * @param queueName the name of the queue
     * @return an instance of WorkgroupQueue for the argument queue name, or null if none exists
     */
    public WorkgroupQueue getQueue(Resourcepart queueName) {
        return queues.get(queueName);
    }

    public Iterator<WorkgroupQueue> getQueues() {
        return Collections.unmodifiableMap(new HashMap<>(queues)).values().iterator();
    }

    public void addQueueUsersListener(QueueUsersListener listener) {
        synchronized (queueUsersListeners) {
            if (!queueUsersListeners.contains(listener)) {
                queueUsersListeners.add(listener);
            }
        }
    }

    public void removeQueueUsersListener(QueueUsersListener listener) {
        synchronized (queueUsersListeners) {
            queueUsersListeners.remove(listener);
        }
    }

    /**
     * Adds an offer listener.
     *
     * @param offerListener the offer listener.
     */
    public void addOfferListener(OfferListener offerListener) {
        synchronized (offerListeners) {
            if (!offerListeners.contains(offerListener)) {
                offerListeners.add(offerListener);
            }
        }
    }

    /**
     * Removes an offer listener.
     *
     * @param offerListener the offer listener.
     */
    public void removeOfferListener(OfferListener offerListener) {
        synchronized (offerListeners) {
            offerListeners.remove(offerListener);
        }
    }

    /**
     * Adds an invitation listener.
     *
     * @param invitationListener the invitation listener.
     */
    public void addInvitationListener(WorkgroupInvitationListener invitationListener) {
        synchronized (invitationListeners) {
            if (!invitationListeners.contains(invitationListener)) {
                invitationListeners.add(invitationListener);
            }
        }
    }

    /**
     * Removes an invitation listener.
     *
     * @param invitationListener the invitation listener.
     */
    public void removeInvitationListener(WorkgroupInvitationListener invitationListener) {
        synchronized (invitationListeners) {
            invitationListeners.remove(invitationListener);
        }
    }

    private void fireOfferRequestEvent(OfferRequestProvider.OfferRequestPacket requestPacket) {
        Offer offer = new Offer(this.connection, this, requestPacket.getUserID(),
                requestPacket.getUserJID(), this.getWorkgroupJID(),
                                new Date(new Date().getTime() + (requestPacket.getTimeout() * 1000)),
                requestPacket.getSessionID(), requestPacket.getMetaData(), requestPacket.getContent());

        synchronized (offerListeners) {
            for (OfferListener listener : offerListeners) {
                listener.offerReceived(offer);
            }
        }
    }

    private void fireOfferRevokeEvent(OfferRevokeProvider.OfferRevokePacket orp) {
        RevokedOffer revokedOffer = new RevokedOffer(orp.getUserJID(), orp.getUserID(),
                this.getWorkgroupJID(), orp.getSessionID(), orp.getReason(), new Date());

        synchronized (offerListeners) {
            for (OfferListener listener : offerListeners) {
                listener.offerRevoked(revokedOffer);
            }
        }
    }

    private void fireInvitationEvent(Jid groupChatJID, String sessionID, String body,
                                     Jid from, Map<String, List<String>> metaData) {
        WorkgroupInvitation invitation = new WorkgroupInvitation(connection.getUser(), groupChatJID,
                workgroupJID, sessionID, body, from, metaData);

        synchronized (invitationListeners) {
            for (WorkgroupInvitationListener listener : invitationListeners) {
                listener.invitationReceived(invitation);
            }
        }
    }

    private void fireQueueUsersEvent(WorkgroupQueue queue, WorkgroupQueue.Status status,
                                     int averageWaitTime, Date oldestEntry, Set<QueueUser> users) {
        synchronized (queueUsersListeners) {
            for (QueueUsersListener listener : queueUsersListeners) {
                if (status != null) {
                    listener.statusUpdated(queue, status);
                }
                if (averageWaitTime != -1) {
                    listener.averageWaitTimeUpdated(queue, averageWaitTime);
                }
                if (oldestEntry != null) {
                    listener.oldestEntryUpdated(queue, oldestEntry);
                }
                if (users != null) {
                    listener.usersUpdated(queue, users);
                }
            }
        }
    }

    // PacketListener Implementation.

    private void handlePacket(Stanza packet) {
        if (packet instanceof Presence) {
            Presence presence = (Presence) packet;

            // The workgroup can send us a number of different presence packets. We
            // check for different packet extensions to see what type of presence
            // packet it is.

            Resourcepart queueName = presence.getFrom().getResourceOrNull();
            WorkgroupQueue queue = queues.get(queueName);
            // If there isn't already an entry for the queue, create a new one.
            if (queue == null) {
                queue = new WorkgroupQueue(queueName);
                queues.put(queueName, queue);
            }

            // QueueOverview packet extensions contain basic information about a queue.
            QueueOverview queueOverview = (QueueOverview) presence.getExtensionElement(QueueOverview.ELEMENT_NAME, QueueOverview.NAMESPACE);
            if (queueOverview != null) {
                if (queueOverview.getStatus() == null) {
                    queue.setStatus(WorkgroupQueue.Status.CLOSED);
                }
                else {
                    queue.setStatus(queueOverview.getStatus());
                }
                queue.setAverageWaitTime(queueOverview.getAverageWaitTime());
                queue.setOldestEntry(queueOverview.getOldestEntry());
                // Fire event.
                fireQueueUsersEvent(queue, queueOverview.getStatus(),
                        queueOverview.getAverageWaitTime(), queueOverview.getOldestEntry(),
                        null);
                return;
            }

            // QueueDetails packet extensions contain information about the users in
            // a queue.
            QueueDetails queueDetails = (QueueDetails) packet.getExtensionElement(QueueDetails.ELEMENT_NAME, QueueDetails.NAMESPACE);
            if (queueDetails != null) {
                queue.setUsers(queueDetails.getUsers());
                // Fire event.
                fireQueueUsersEvent(queue, null, -1, null, queueDetails.getUsers());
                return;
            }

            // Notify agent packets gives an overview of agent activity in a queue.
            StandardExtensionElement notifyAgents = (StandardExtensionElement) presence.getExtensionElement("notify-agents", "http://jabber.org/protocol/workgroup");
            if (notifyAgents != null) {
                int currentChats = Integer.parseInt(notifyAgents.getFirstElement("current-chats", "http://jabber.org/protocol/workgroup").getText());
                int maxChats = Integer.parseInt(notifyAgents.getFirstElement("max-chats", "http://jabber.org/protocol/workgroup").getText());
                queue.setCurrentChats(currentChats);
                queue.setMaxChats(maxChats);
                // Fire event.
                // TODO: might need another event for current chats and max chats of queue
                return;
            }
        }
        else if (packet instanceof Message) {
            Message message = (Message) packet;

            // Check if a room invitation was sent and if the sender is the workgroup
            MUCUser mucUser = MUCUser.from(message);
            MUCUser.Invite invite = mucUser != null ? mucUser.getInvite() : null;
            if (invite != null && workgroupJID.equals(invite.getFrom())) {
                String sessionID = null;
                Map<String, List<String>> metaData = null;

                SessionID sessionIDExt = (SessionID) message.getExtensionElement(SessionID.ELEMENT_NAME,
                        SessionID.NAMESPACE);
                if (sessionIDExt != null) {
                    sessionID = sessionIDExt.getSessionID();
                }

                MetaData metaDataExt = (MetaData) message.getExtensionElement(MetaData.ELEMENT_NAME,
                        MetaData.NAMESPACE);
                if (metaDataExt != null) {
                    metaData = metaDataExt.getMetaData();
                }

                this.fireInvitationEvent(message.getFrom(), sessionID, message.getBody(),
                        message.getFrom(), metaData);
            }
        }
    }

    /**
     * Creates a ChatNote that will be mapped to the given chat session.
     *
     * @param sessionID the session id of a Chat Session.
     * @param note      the chat note to add.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setNote(String sessionID, String note) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        ChatNotes notes = new ChatNotes();
        notes.setType(IQ.Type.set);
        notes.setTo(workgroupJID);
        notes.setSessionID(sessionID);
        notes.setNotes(note);
        connection.sendIqRequestAndWaitForResponse(notes);
    }

    /**
     * Retrieves the ChatNote associated with a given chat session.
     *
     * @param sessionID the sessionID of the chat session.
     * @return the <code>ChatNote</code> associated with a given chat session.
     * @throws XMPPErrorException if an error occurs while retrieving the ChatNote.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ChatNotes getNote(String sessionID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ChatNotes request = new ChatNotes();
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);
        request.setSessionID(sessionID);

        ChatNotes response = connection.sendIqRequestAndWaitForResponse(request);
        return response;
    }

    /**
     * Retrieves the AgentChatHistory associated with a particular agent jid.
     *
     * @param jid the jid of the agent.
     * @param maxSessions the max number of sessions to retrieve.
     * @param startDate point in time from which on history should get retrieved.
     * @return the chat history associated with a given jid.
     * @throws XMPPException if an error occurs while retrieving the AgentChatHistory.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NoResponseException if there was no response from the remote entity.
     */
    public AgentChatHistory getAgentHistory(EntityBareJid jid, int maxSessions, Date startDate) throws XMPPException, NotConnectedException, InterruptedException, NoResponseException {
        AgentChatHistory request;
        if (startDate != null) {
            request = new AgentChatHistory(jid, maxSessions, startDate);
        }
        else {
            request = new AgentChatHistory(jid, maxSessions);
        }

        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);

        AgentChatHistory response = connection.sendIqRequestAndWaitForResponse(
                        request);

        return response;
    }

    /**
     * Asks the workgroup for it's Search Settings.
     *
     * @return SearchSettings the search settings for this workgroup.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public SearchSettings getSearchSettings() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        SearchSettings request = new SearchSettings();
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);

        SearchSettings response = connection.sendIqRequestAndWaitForResponse(request);
        return response;
    }

    /**
     * Asks the workgroup for it's Global Macros.
     *
     * @param global true to retrieve global macros, otherwise false for personal macros.
     * @return MacroGroup the root macro group.
     * @throws XMPPErrorException if an error occurs while getting information from the server.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public MacroGroup getMacros(boolean global) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Macros request = new Macros();
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);
        request.setPersonal(!global);

        Macros response = connection.sendIqRequestAndWaitForResponse(request);
        return response.getRootGroup();
    }

    /**
     * Persists the Personal Macro for an agent.
     *
     * @param group the macro group to save.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void saveMacros(MacroGroup group) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Macros request = new Macros();
        request.setType(IQ.Type.set);
        request.setTo(workgroupJID);
        request.setPersonal(true);
        request.setPersonalMacroGroup(group);

        connection.sendIqRequestAndWaitForResponse(request);
    }

    /**
     * Query for metadata associated with a session id.
     *
     * @param sessionID the sessionID to query for.
     * @return Map a map of all metadata associated with the sessionID.
     * @throws XMPPException if an error occurs while getting information from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NoResponseException if there was no response from the remote entity.
     */
    public Map<String, List<String>> getChatMetadata(String sessionID) throws XMPPException, NotConnectedException, InterruptedException, NoResponseException {
        ChatMetadata request = new ChatMetadata();
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);
        request.setSessionID(sessionID);

        ChatMetadata response = connection.sendIqRequestAndWaitForResponse(request);

        return response.getMetadata();
    }

    /**
     * Invites a user or agent to an existing session support. The provided invitee's JID can be of
     * a user, an agent, a queue or a workgroup. In the case of a queue or a workgroup the workgroup service
     * will decide the best agent to receive the invitation.<p>
     *
     * This method will return either when the service returned an ACK of the request or if an error occurred
     * while requesting the invitation. After sending the ACK the service will send the invitation to the target
     * entity. When dealing with agents the common sequence of offer-response will be followed. However, when
     * sending an invitation to a user a standard MUC invitation will be sent.<p>
     *
     * The agent or user that accepted the offer <b>MUST</b> join the room. Failing to do so will make
     * the invitation to fail. The inviter will eventually receive a message error indicating that the invitee
     * accepted the offer but failed to join the room.
     *
     * Different situations may lead to a failed invitation. Possible cases are: 1) all agents rejected the
     * offer and there are no agents available, 2) the agent that accepted the offer failed to join the room or
     * 2) the user that received the MUC invitation never replied or joined the room. In any of these cases
     * (or other failing cases) the inviter will get an error message with the failed notification.
     *
     * @param type type of entity that will get the invitation.
     * @param invitee JID of entity that will get the invitation.
     * @param sessionID ID of the support session that the invitee is being invited.
     * @param reason the reason of the invitation.
     * @throws XMPPErrorException if the sender of the invitation is not an agent or the service failed to process
     *         the request.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendRoomInvitation(RoomInvitation.Type type, Jid invitee, String sessionID, String reason) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final RoomInvitation invitation = new RoomInvitation(type, invitee, sessionID, reason);
        IQ iq = new RoomInvitation.RoomInvitationIQ(invitation);
        iq.setType(IQ.Type.set);
        iq.setTo(workgroupJID);
        iq.setFrom(connection.getUser());

        connection.sendIqRequestAndWaitForResponse(iq);
    }

    /**
     * Transfer an existing session support to another user or agent. The provided invitee's JID can be of
     * a user, an agent, a queue or a workgroup. In the case of a queue or a workgroup the workgroup service
     * will decide the best agent to receive the invitation.<p>
     *
     * This method will return either when the service returned an ACK of the request or if an error occurred
     * while requesting the transfer. After sending the ACK the service will send the invitation to the target
     * entity. When dealing with agents the common sequence of offer-response will be followed. However, when
     * sending an invitation to a user a standard MUC invitation will be sent.<p>
     *
     * Once the invitee joins the support room the workgroup service will kick the inviter from the room.<p>
     *
     * Different situations may lead to a failed transfers. Possible cases are: 1) all agents rejected the
     * offer and there are no agents available, 2) the agent that accepted the offer failed to join the room
     * or 2) the user that received the MUC invitation never replied or joined the room. In any of these cases
     * (or other failing cases) the inviter will get an error message with the failed notification.
     *
     * @param type type of entity that will get the invitation.
     * @param invitee JID of entity that will get the invitation.
     * @param sessionID ID of the support session that the invitee is being invited.
     * @param reason the reason of the invitation.
     * @throws XMPPErrorException if the sender of the invitation is not an agent or the service failed to process
     *         the request.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendRoomTransfer(RoomTransfer.Type type, String invitee, String sessionID, String reason) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final RoomTransfer transfer = new RoomTransfer(type, invitee, sessionID, reason);
        IQ iq = new RoomTransfer.RoomTransferIQ(transfer);
        iq.setType(IQ.Type.set);
        iq.setTo(workgroupJID);
        iq.setFrom(connection.getUser());

        connection.sendIqRequestAndWaitForResponse(iq);
    }

    /**
     * Returns the generic metadata of the workgroup the agent belongs to.
     *
     * @param con   the XMPPConnection to use.
     * @param query an optional query object used to tell the server what metadata to retrieve. This can be null.
     * @return the settings for the workgroup.
     * @throws XMPPErrorException if an error occurs while sending the request to the server.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public GenericSettings getGenericSettings(XMPPConnection con, String query) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        GenericSettings setting = new GenericSettings();
        setting.setType(IQ.Type.get);
        setting.setTo(workgroupJID);

        GenericSettings response = connection.sendIqRequestAndWaitForResponse(
                        setting);
        return response;
    }

    public boolean hasMonitorPrivileges(XMPPConnection con) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        MonitorPacket request = new MonitorPacket();
        request.setType(IQ.Type.get);
        request.setTo(workgroupJID);

        MonitorPacket response = connection.sendIqRequestAndWaitForResponse(request);
        return response.isMonitor();
    }

    public void makeRoomOwner(XMPPConnection con, String sessionID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        MonitorPacket request = new MonitorPacket();
        request.setType(IQ.Type.set);
        request.setTo(workgroupJID);
        request.setSessionID(sessionID);

        connection.sendIqRequestAndWaitForResponse(request);
    }
}
