/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smackx.workgroup.agent;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;

import org.jivesoftware.smackx.workgroup.*;
import org.jivesoftware.smackx.workgroup.packet.*;

/**
 * This class embodies the agent's active presence within a given workgroup.
 * The application should have N instances of this class, where N is the
 * number of workgroups to which the owning agent of the application belongs.
 * This class provides all functionality that a session within a given workgroup
 * is expected to have from an agent's perspective -- setting the status, tracking
 * the status of queues to which the agent belongs within the workgroup, and
 * dequeuing customers.
 *
 * @author Matt Tucker
 */
public class WorkgroupSession {

    private XMPPConnection connection;

    private String workgroupName;

    private boolean online = false;
    private Presence.Mode presenceMode;
    private int currentChats;
    private int maxChats;
    private Map metaData;

    private Map queues;

    private List offerListeners;
    private List queueUsersListeners;
    private List queueAgentsListeners;

    /**
     * Creates a new agent session instance.
     *
     * @param connection a connection instance which must have already gone through authentication.
     * @param workgroupName the fully qualified name of the workgroup.
     */
    public WorkgroupSession(String workgroupName, XMPPConnection connection) {
        // Login must have been done before passing in connection.
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must login to server before creating workgroup.");
        }

        this.workgroupName = workgroupName;
        this.connection = connection;

        this.maxChats = -1;

        this.metaData = new HashMap();

        this.queues = new HashMap();

        offerListeners = new ArrayList();
        queueUsersListeners = new ArrayList();
        queueAgentsListeners = new ArrayList();

        // Create a filter to listen for packets we're interested in.
        OrFilter filter = new OrFilter();
        filter.addFilter(new PacketTypeFilter(OfferRequestProvider.OfferRequestPacket.class));
        filter.addFilter(new PacketTypeFilter(OfferRevokeProvider.OfferRevokePacket.class));
        filter.addFilter(new PacketTypeFilter(Presence.class));
        filter.addFilter(new PacketTypeFilter(Message.class));

        // only interested in packets from the workgroup or from operators also running the
        //      operator client -- for example peer-to-peer invites wouldn't come from
        //      the workgroup, but would come from the operator client
        //OrFilter froms = new OrFilter(new FromContainsFilter(this.workgroupName),
        //                            new FromContainsFilter(clientResource));

        connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    handlePacket(packet);
                }
            }, filter);
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
     * Returns the current number of chats the agent is in.
     *
     * @return the current number of chats the agent is in.
     */
    public int getCurrentChats() {
        return currentChats;
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
     *  new data, the revised meta data will be rebroadcast in an agent's presence broadcast.
     *
     * @param key the meta data key
     * @param val the non-null meta data value
     */
    public void setMetaData(String key, String val) throws XMPPException {
        synchronized (this.metaData) {
            String oldVal = (String)this.metaData.get(key);

            if ((oldVal == null) || (! oldVal.equals(val))) {
                metaData.put(key, val);

                setStatus(presenceMode, currentChats, maxChats);
            }
        }
    }

    /**
     * Allows the removal of data from the agent's meta data, if the key represents existing data,
     *  the revised meta data will be rebroadcast in an agent's presence broadcast.
     *
     * @param key the meta data key
     */
    public void removeMetaData(String key)
        throws XMPPException {
        synchronized (this.metaData) {
            String oldVal = (String)metaData.remove(key);

            if (oldVal != null) {
                setStatus(presenceMode, currentChats, maxChats);
            }
        }
    }

    /**
     * Allows the retrieval of meta data for a specified key.
     *
     * @param key the meta data key
     * @return the meta data value associated with the key or <tt>null</tt> if the meta-data
     *      doesn't exist..
     */
    public String getMetaData(String key) {
        return (String)metaData.get(key);
    }

    /**
     * Sets whether the agent is online with the workgroup. If the user tries to go online with
     * the workgroup but is not allowed to be an agent, an XMPPError with error code 401 will
     * be thrown.
     *
     * @param online true to set the agent as online with the workgroup.
     * @throws XMPPException if an error occurs setting the online status.
     */
    public void setOnline( boolean online ) throws XMPPException {
        // If the online status hasn't changed, do nothing.
        if (this.online == online) {
            return;
        }
        this.online = online;

        Presence presence = null;

        // If the user is going online...
        if (online) {
            presence = new Presence(Presence.Type.AVAILABLE);
            presence.setTo(workgroupName);

            PacketCollector collector = this.connection.createPacketCollector(new AndFilter(
                new PacketTypeFilter(Presence.class), new FromContainsFilter(workgroupName)));

            connection.sendPacket(presence);

            presence = (Presence)collector.nextResult(5000);
            collector.cancel();
            if (presence == null) {
                throw new XMPPException("No response from server on status set.");
            }

            if (presence.getError() != null) {
                throw new XMPPException(presence.getError());
            }
        }
        // Otherwise the user is going offline...
        else {
            presence = new Presence(Presence.Type.UNAVAILABLE);
            presence.setTo(workgroupName);
            connection.sendPacket(presence);
        }
    }
    
    /**
     * Sets the agent's current status with the workgroup. The presence mode affects how offers
     * are routed to the agent. The possible presence modes with their meanings are as follows:<ul>
     *
     *      <li>Presence.Mode.AVAILABLE -- (Default) the agent is available for more chats
     *          (equivalent to Presence.Mode.CHAT).
     *      <li>Presence.Mode.DO_NOT_DISTURB -– the agent is busy and should not be disturbed.
     *          However, special case, or extreme urgency chats may still be offered to the agent.
     *      <li>Presence.Mode.AWAY -- the agent is not available and should not
     *          have a chat routed to them (equivalent to Presence.Mode.EXTENDED_AWAY).</ul>
     *
     * The current chats value indicates how many chats the agent is currently in. Because the agent
     * is responsible for reporting the current chats value to the server, this value <b>must</b>
     * be set every time it changes.<p>
     *
     * The max chats value is the maximum number of chats the agent is willing to have routed to
     * them at once. Some servers may be configured to only accept max chat values in a certain
     * range; for example, between two and five. In that case, the maxChats value the agent sends
     * may be adjusted by the server to a value within that range.
     *
     * @param presenceMode the presence mode of the agent.
     * @param currentChats the current number of chats the agent is in.
     * @param maxChats the maximum number of chats the agent is willing to accept.
     * @throws XMPPException if an error occurs setting the agent status.
     * @throws IllegalStateException if the agent is not online with the workgroup.
     */
    public void setStatus(Presence.Mode presenceMode, int currentChats, int maxChats )
            throws XMPPException
    {
      setStatus( presenceMode, currentChats, maxChats, null );
    }
    

    /**
     * Sets the agent's current status with the workgroup. The presence mode affects how offers
     * are routed to the agent. The possible presence modes with their meanings are as follows:<ul>
     *
     *      <li>Presence.Mode.AVAILABLE -- (Default) the agent is available for more chats
     *          (equivalent to Presence.Mode.CHAT).
     *      <li>Presence.Mode.DO_NOT_DISTURB -– the agent is busy and should not be disturbed.
     *          However, special case, or extreme urgency chats may still be offered to the agent.
     *      <li>Presence.Mode.AWAY -- the agent is not available and should not
     *          have a chat routed to them (equivalent to Presence.Mode.EXTENDED_AWAY).</ul>
     *
     * The current chats value indicates how many chats the agent is currently in. Because the agent
     * is responsible for reporting the current chats value to the server, this value <b>must</b>
     * be set every time it changes.<p>
     *
     * The max chats value is the maximum number of chats the agent is willing to have routed to
     * them at once. Some servers may be configured to only accept max chat values in a certain
     * range; for example, between two and five. In that case, the maxChats value the agent sends
     * may be adjusted by the server to a value within that range.
     *
     * @param presenceMode the presence mode of the agent.
     * @param currentChats the current number of chats the agent is in.
     * @param maxChats the maximum number of chats the agent is willing to accept.
     * @param status sets the status message of the presence update.
     * @throws XMPPException if an error occurs setting the agent status.
     * @throws IllegalStateException if the agent is not online with the workgroup.
     */
    public void setStatus(Presence.Mode presenceMode, int currentChats, int maxChats, String status )
            throws XMPPException
    {
        if (!online) {
            throw new IllegalStateException("Cannot set status when the agent is not online.");
        }

        if (presenceMode == null) {
            presenceMode = Presence.Mode.AVAILABLE;
        }
        this.presenceMode = presenceMode;
        this.currentChats = currentChats;
        this.maxChats = maxChats;

        Presence presence = new Presence(Presence.Type.AVAILABLE);
        presence.setMode(presenceMode);
        presence.setTo(this.getWorkgroupName());
        
        if( status != null ) {
          presence.setStatus( status );
        }
        // Send information about max chats and current chats as a packet extension.
        DefaultPacketExtension agentStatus = new DefaultPacketExtension(AgentStatus.ELEMENT_NAME,
                AgentStatus.NAMESPACE);
        agentStatus.setValue("current-chats", ""+currentChats);
        agentStatus.setValue("max-chats", ""+maxChats);
        presence.addExtension(agentStatus);
        presence.addExtension(new MetaData(this.metaData));

        PacketCollector collector = this.connection.createPacketCollector(new AndFilter(
                new PacketTypeFilter(Presence.class), new FromContainsFilter(workgroupName)));

        this.connection.sendPacket(presence);

        presence = (Presence)collector.nextResult(5000);
        collector.cancel();
        if (presence == null) {
            throw new XMPPException("No response from server on status set.");
        }

        if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
    }

    /**
     * Removes a user from the workgroup queue. This is an administrative action that the
     *
     * The agent is not guaranteed of having privileges to perform this action; an exception
     * denying the request may be thrown.
     */
    public void dequeueUser(String userID) throws XMPPException {
        // todo: this method simply won't work right now.
        DepartQueuePacket departPacket = new DepartQueuePacket(this.workgroupName);

        // PENDING
        this.connection.sendPacket(departPacket);
    }

    /**
     * @return the fully-qualified name of the workgroup for which this session exists
     */
    public String getWorkgroupName() {
        return workgroupName;
    }

    /**
     * @param queueName the name of the queue
     * @return an instance of WorkgroupQueue for the argument queue name, or null if none exists
     */
    public WorkgroupQueue getQueue(String queueName) {
        return (WorkgroupQueue)queues.get(queueName);
    }

    public Iterator getQueues() {
        return Collections.unmodifiableMap((new HashMap(queues))).values().iterator();
    }

    public void addQueueUsersListener(QueueUsersListener listener) {
        synchronized(queueUsersListeners) {
            if (!queueUsersListeners.contains(listener)) {
                queueUsersListeners.add(listener);
            }
        }
    }

    public void removeQueueUsersListener(QueueUsersListener listener) {
        synchronized(queueUsersListeners) {
            queueUsersListeners.remove(listener);
        }
    }

    public void addQueueAgentsListener(QueueAgentsListener listener) {
        synchronized(queueAgentsListeners) {
            if (!queueAgentsListeners.contains(listener)) {
                queueAgentsListeners.add(listener);
            }
        }
    }

    public void removeQueueAgentsListener(QueueAgentsListener listener) {
        synchronized(queueAgentsListeners) {
            queueAgentsListeners.remove(listener);
        }
    }

    /**
     * Adds an offer listener.
     *
     * @param offerListener the offer listener.
     */
    public void addOfferListener(OfferListener offerListener) {
        synchronized(offerListeners) {
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
        synchronized(offerListeners) {
            offerListeners.remove(offerListener);
        }
    }

    /**
     * Adds an invitation listener.
     *
     * @param invitationListener the invitation listener.
     */
    public void addInvitationListener(InvitationListener invitationListener) {
        synchronized(invitationListeners) {
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
    public void removeInvitationListener(InvitationListener invitationListener) {
        synchronized(invitationListeners) {
            offerListeners.remove(invitationListener);
        }
    }

    private void fireOfferRequestEvent(OfferRequestProvider.OfferRequestPacket requestPacket) {
        Offer offer = new Offer(this.connection, this, requestPacket.getUserID(),
                                this.getWorkgroupName(),
                                new Date((new Date()).getTime()
                                            + (requestPacket.getTimeout() * 1000)),
                                requestPacket.getSessionID(), requestPacket.getMetaData());

        synchronized (offerListeners) {
            for (Iterator i=offerListeners.iterator(); i.hasNext(); ) {
                OfferListener listener = (OfferListener)i.next();
                listener.offerReceived(offer);
            }
        }
    }

    private void fireOfferRevokeEvent(OfferRevokeProvider.OfferRevokePacket orp) {
        RevokedOffer revokedOffer = new RevokedOffer(orp.getUserID(), this.getWorkgroupName(),
                                                     orp.getSessionID(), orp.getReason(),
                                                     new Date());

        synchronized (offerListeners) {
            for (Iterator i=offerListeners.iterator(); i.hasNext(); ) {
                OfferListener listener = (OfferListener)i.next();
                listener.offerRevoked(revokedOffer);
            }
        }
    }

    private void fireInvitationEvent(String groupChatJID, String sessionID, String body,
            String from, Map metaData)
    {
        Invitation invitation = new Invitation(connection.getUser(), groupChatJID,
                workgroupName, sessionID, body, from, metaData);

        synchronized(invitationListeners) {
            for (Iterator i=invitationListeners.iterator(); i.hasNext(); ) {
                InvitationListener listener = (InvitationListener)i.next();
                listener.invitationReceived(invitation);
            }
        }
    }

    private void fireQueueUsersEvent(WorkgroupQueue queue, WorkgroupQueue.Status status,
            int averageWaitTime, Date oldestEntry, Set users)
    {
        synchronized(queueUsersListeners) {
            for (Iterator i=queueUsersListeners.iterator(); i.hasNext(); ) {
                QueueUsersListener listener = (QueueUsersListener)i.next();
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

    private void fireQueueAgentsEvent(WorkgroupQueue queue, int currentChats,
            int maxChats, Set agents)
    {
        synchronized(queueAgentsListeners) {
            for (Iterator i=queueAgentsListeners.iterator(); i.hasNext(); ) {
                QueueAgentsListener listener = (QueueAgentsListener)i.next();
                if (currentChats != -1) {
                    listener.currentChatsUpdated(queue, currentChats);
                }
                if (maxChats != -1) {
                    listener.maxChatsUpdated(queue, maxChats);
                }
                if (agents != null) {
                    listener.agentsUpdated(queue, agents);
                }
            }
        }
    }

    // PacketListener Implementation.

    private void handlePacket(Packet packet) {
        if (packet instanceof OfferRequestProvider.OfferRequestPacket) {
            fireOfferRequestEvent((OfferRequestProvider.OfferRequestPacket)packet);
        }
        else if (packet instanceof Presence) {
            Presence presence = (Presence)packet;

            // The workgroup can send us a number of different presence packets. We
            // check for different packet extensions to see what type of presence
            // packet it is.

            String queueName = StringUtils.parseResource(presence.getFrom());
            WorkgroupQueue queue = (WorkgroupQueue)queues.get(queueName);
            // If there isn't already an entry for the queue, create a new one.
            if (queue == null) {
                queue = new WorkgroupQueue(queueName);
                queues.put(queueName, queue);
            }

            // QueueOverview packet extensions contain basic information about a queue.
            QueueOverview queueOverview = (QueueOverview)presence.getExtension(
                    QueueOverview.ELEMENT_NAME, QueueOverview.NAMESPACE);
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
            QueueDetails queueDetails = (QueueDetails)packet.getExtension(
                    QueueDetails.ELEMENT_NAME, QueueDetails.NAMESPACE);
            if (queueDetails != null) {
                queue.setUsers(queueDetails.getUsers());
                // Fire event.
                fireQueueUsersEvent(queue, null, -1, null, queueDetails.getUsers());
                return;
            }

            // Notify agent packets gives an overview of agent activity in a queue.
            DefaultPacketExtension notifyAgents = (DefaultPacketExtension)presence.getExtension(
                    "notify-agents", "xmpp:workgroup");
            if (notifyAgents != null) {
                int currentChats = Integer.parseInt(notifyAgents.getValue("current-chats"));
                int maxChats = Integer.parseInt(notifyAgents.getValue("max-chats"));
                queue.setCurrentChats(currentChats);
                queue.setMaxChats(maxChats);
                // Fire event.
                fireQueueAgentsEvent(queue, currentChats, maxChats, null);
                return;
            }

            // Agent status
            AgentStatus agentStatus = (AgentStatus)presence.getExtension(AgentStatus.ELEMENT_NAME,
                    AgentStatus.NAMESPACE);
            if (agentStatus != null) {
                Set agents = agentStatus.getAgents();
                // Look for information about the agent that created this session and
                // update local status fields accordingly.
                for (Iterator i=agents.iterator(); i.hasNext(); ) {
                    Agent agent = (Agent)i.next();
                    if (agent.getUser().equals(StringUtils.parseBareAddress(
                            connection.getUser())))
                    {
                        maxChats = agent.getMaxChats();
                        currentChats = agent.getCurrentChats();
                    }
                }
                // Set the list of agents for the queue.
                queue.setAgents(agents);
                // Fire event.
                fireQueueAgentsEvent(queue, -1, -1, agentStatus.getAgents());
                return;
            }
        }
        else if (packet instanceof Message) {
            Message message = (Message)packet;

            GroupChatInvitation invitation = (GroupChatInvitation)message.getExtension(
                    GroupChatInvitation.ELEMENT_NAME, GroupChatInvitation.NAMESPACE);

            if (invitation != null) {
                String roomAddress = invitation.getRoomAddress();
                String sessionID = null;
                Map metaData = null;

                SessionID sessionIDExt = (SessionID)message.getExtension(SessionID.ELEMENT_NAME,
                        SessionID.NAMESPACE);
                if (sessionIDExt != null) {
                    sessionID = sessionIDExt.getSessionID();
                }

                MetaData metaDataExt = (MetaData)message.getExtension(MetaData.ELEMENT_NAME,
                        MetaData.NAMESPACE);
                if (metaDataExt != null) {
                    metaData = metaDataExt.getMetaData();
                }

                this.fireInvitationEvent(roomAddress, sessionID, message.getBody(),
                        message.getFrom(), metaData);
            }
        }
        else if (packet instanceof OfferRevokeProvider.OfferRevokePacket) {
            fireOfferRevokeEvent((OfferRevokeProvider.OfferRevokePacket)packet);
        }
    }
}