/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.user;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.GroupChatInvitation;

import org.jivesoftware.smackx.workgroup.*;
import org.jivesoftware.smackx.workgroup.packet.*;
import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

/**
 * Provides workgroup services for users. Users can join the workgropu queue, depart the
 * queue, find status information about their placement in the queue, and register to
 * be notified when they are routed to an agent.<p>
 *
 * This class only provides a user's perspective into a workgroup and is not intended
 * for use by agents.
 *
 * @author Matt Tucker
 * @author loki der quaeler
 */
public class Workgroup {

    private String workgroupName;
    private XMPPConnection connection;
    private boolean inQueue;
    private List invitationListeners;
    private List queueListeners;

    private int queuePosition = -1;
    private int queueRemainingTime = -1;

    /**
     * Creates a new workgroup instance using the specified workgroup name
     * (eg support@example.com) and XMPP connection. The connection must have
     * undergone a successful login before being used to construct an instance of
     * this class.
     *
     * @param workgroupName the fully qualified name of the workgroup.
     * @param connection an XMPP connection which must have already undergone a
     *      successful login.
     */
    public Workgroup(String workgroupName, XMPPConnection connection) {
        // Login must have been done before passing in connection.
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must login to server before creating workgroup.");
        }

        this.workgroupName = workgroupName;
        this.connection = connection;
        inQueue = false;
        invitationListeners = new ArrayList();
        queueListeners = new ArrayList();

        // Register as a queue listener for internal usage by this instance.
        addQueueListener(new QueueListener() {
            public void joinedQueue() {
                inQueue = true;
            }

            public void departedQueue() {
                inQueue = false;
                queuePosition = -1;
                queueRemainingTime = -1;
            }

            public void queuePositionUpdated(int currentPosition) {
                queuePosition = currentPosition;
            }

            public void queueWaitTimeUpdated(int secondsRemaining) {
                queueRemainingTime = secondsRemaining;
            }
        });

        // Register an invitation listener for internal usage by this instance.
        addInvitationListener(new InvitationListener() {
            public void invitationReceived(Invitation invitation) {
                inQueue = false;
                queuePosition = -1;
                queueRemainingTime = -1;
            }
        });

        // Register a packet listener for all queue events.
        PacketFilter orFilter = new OrFilter(new PacketTypeFilter(Message.class),
                new PacketTypeFilter(QueueUpdate.class));

        PacketFilter filter = new AndFilter(new FromContainsFilter(this.workgroupName), orFilter);

        connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    handlePacket(packet);
                }
            }, filter);
    }

    /**
     * Returns the name of this workgroup (eg support@example.com).
     *
     * @return the name of the workgroup.
     */
    public String getWorkgroupName() {
        return workgroupName;
    }

    /**
     * Returns true if the user is currently waiting in the workgroup queue.
     *
     * @return true if currently waiting in the queue.
     */
    public boolean isInQueue() {
        return inQueue;
    }

    /**
     * Returns the user's current position in the workgroup queue. A value of 0 means
     * the user is next in line to be routed; therefore, if the queue position
     * is being displayed to the end user it is usually a good idea to add 1 to
     * the value this method returns before display. If the user is not currently
     * waiting in the workgorup, or no queue position information is available, -1
     * will be returned.
     *
     * @return the user's current position in the workgorup queue, or -1 if the
     *      position isn't available or if the user isn't in the queue.
     */
    public int getQueuePosition() {
        return queuePosition;
    }

    /**
     * Returns the estimated time (in seconds) that the user has to left wait in
     * the workgroup queue before being routed. If the user is not currently waiting
     * int he workgroup, or no queue time information is available, -1 will be
     * returned.
     *
     * @return the estimated time remaining (in seconds) that the user has to
     *      wait in the workgropu queue, or -1 if time information isn't available
     *      or if the user isn't int the queue.
     */
    public int getQueueRemainingTime() {
        return queueRemainingTime;
    }

    /**
     * Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user will leave the queue: <ul>
     *     <li>The user is routed to an agent, which triggers a groupchat invitation.
     *     <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     *     <li>A server error occurs, or an administrator explicitly removes the user
     *         from the queue.
     * </ul>
     *
     * A user cannot request to join the queue again if already in the queue. Therefore, this
     * method will do nothing if the user is already in the queue.<p>
     *
     * Some servers may be configured to require certain meta-data in
     * order to join the queue. In that case, the {@link #joinQueue(Map)} method
     * should be used instead of this method so that meta-data may be passed in.
     *
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *      that a connection failure occured or that the server explicitly rejected the
     *      request to join the queue.
     */
    public void joinQueue() throws XMPPException {
        joinQueue(null);
    }

    /**
     * Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user will leave the queue: <ul>
     *     <li>The user is routed to an agent, which triggers a groupchat invitation.
     *     <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     *     <li>A server error occurs, or an administrator explicitly removes the user
     *         from the queue.
     * </ul>
     *
     * A user cannot request to join the queue again if already in the queue. Therefore, this
     * method will do nothing if the user is already in the queue.<p>
     *
     * Arbitrary meta-data can be passed in with the queue join request in order to assist
     * the server in routing the user to an agent and to provide information about the
     * user to the agent. Some servers may be configured to require certain meta-data in
     * order to join the queue.<p>
     *
     * The server may reject the join queue request, which will cause an XMPPException to
     * be thrown. The error codes for specific cases are as follows:<ul>
     *
     *      <li>503 -- the workgroup is closed or otherwise unavailable to take
     *          new chat requests.
     * </ul>
     *
     * @param metaData the metaData for the join request.
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *      that a connection failure occured or that the server explicitly rejected the
     *      request to join the queue (error code 503). The error code should be checked
     *      to determine the specific error.
     */
    public void joinQueue(Map metaData) throws XMPPException {
        // If already in the queue ignore the join request.
        if (inQueue) {
            return;
        }

        JoinQueuePacket joinPacket = new JoinQueuePacket(workgroupName, metaData);
        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(joinPacket.getPacketID()));

        this.connection.sendPacket(joinPacket);

        IQ response = (IQ)collector.nextResult(10000);
       
        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }

        // Notify listeners that we've joined the queue.
        fireQueueJoinedEvent();
    }

    /**
     * Departs the workgroup queue. If the user is not currently in the queue, this
     * method will do nothing.<p>
     *
     * Normally, the user would not manually leave the queue. However, they may wish to
     * under certain circumstances -- for example, if they no longer wish to be routed
     * to an agent because they've been waiting too long.
     *
     * @throws XMPPException if an error occured trying to send the depart queue
     *      request to the server.
     */
    public void departQueue() throws XMPPException {
        // If not in the queue ignore the depart request.
        if (!inQueue) {
            return;
        }

        DepartQueuePacket departPacket = new DepartQueuePacket(this.workgroupName);
        PacketCollector collector = this.connection.createPacketCollector(
                new PacketIDFilter(departPacket.getPacketID()));

        connection.sendPacket(departPacket);

        IQ response = (IQ)collector.nextResult(5000);
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }

        // Notify listeners that we're no longer in the queue.
        fireQueueDepartedEvent();
    }

    /**
     * Adds a queue listener that will be notified of queue events for the user
     * that created this Workgroup instance.
     *
     * @param queueListener the queue listener.
     */
    public void addQueueListener(QueueListener queueListener) {
        synchronized(queueListeners) {
            if (!queueListeners.contains(queueListener)) {
                queueListeners.add(queueListener);
            }
        }
    }

    /**
     * Removes a queue listener.
     *
     * @param queueListener the queue listener.
     */
    public void removeQueueListener(QueueListener queueListener) {
        synchronized(queueListeners) {
            queueListeners.remove(queueListener);
        }
    }

    /**
     * Adds an invitation listener that will be notified of groupchat invitations
     * from the workgroup for the the user that created this Workgroup instance.
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
    public void removeQueueListener(InvitationListener invitationListener) {
        synchronized(invitationListeners) {
            invitationListeners.remove(invitationListener);
        }
    }

    private void fireInvitationEvent(Invitation invitation) {
        synchronized (invitationListeners) {
            for (Iterator i=invitationListeners.iterator(); i.hasNext(); ) {
                InvitationListener listener = (InvitationListener)i.next();
                listener.invitationReceived(invitation);
            }
        }
    }

    private void fireQueueJoinedEvent() {
        synchronized (queueListeners) {
            for (Iterator i=queueListeners.iterator(); i.hasNext(); ) {
                QueueListener listener = (QueueListener)i.next();
                listener.joinedQueue();
            }
        }
    }

    private void fireQueueDepartedEvent() {
        synchronized (queueListeners) {
            for (Iterator i=queueListeners.iterator(); i.hasNext(); ) {
                QueueListener listener = (QueueListener)i.next();
                listener.departedQueue();
            }
        }
    }

    private void fireQueuePositionEvent(int currentPosition) {
        synchronized (queueListeners) {
            for (Iterator i=queueListeners.iterator(); i.hasNext(); ) {
                QueueListener listener = (QueueListener)i.next();
                listener.queuePositionUpdated(currentPosition);
            }
        }
    }

    private void fireQueueTimeEvent(int secondsRemaining) {
        synchronized (queueListeners) {
            for (Iterator i=queueListeners.iterator(); i.hasNext(); ) {
                QueueListener listener = (QueueListener)i.next();
                listener.queueWaitTimeUpdated(secondsRemaining);
            }
        }
    }

    // PacketListener Implementation.

    private void handlePacket(Packet packet) {
        if (packet instanceof Message) {
            Message msg = (Message)packet;
            // Check to see if the user left the queue.
            PacketExtension pe = msg.getExtension("depart-queue", "xmpp:workgroup");

            if (pe != null) {
                fireQueueDepartedEvent();
            }
            else {
                // Check to see if the user has been invited to a chat.
                GroupChatInvitation invitation = (GroupChatInvitation)msg.getExtension(
                        "x", "jabber:x:conference");

                if (invitation != null) {
                    String roomAddress = invitation.getRoomAddress();
                    String sessionID = null;
                    Map metaData = null;

                    pe = msg.getExtension(SessionID.ELEMENT_NAME,
                            SessionID.NAMESPACE);
                    if (pe != null) {
                        sessionID = ((SessionID)pe).getSessionID();
                    }

                    pe = msg.getExtension(MetaData.ELEMENT_NAME,
                            MetaData.NAMESPACE);
                    if (pe != null) {
                        metaData = ((MetaData)pe).getMetaData();
                    }

                    Invitation inv = new Invitation(connection.getUser(), roomAddress,
                            workgroupName, sessionID, msg.getBody(),
                            msg.getFrom(), metaData);

                    fireInvitationEvent(inv);
                }
            }
        }
        // Check to see if it's a queue update notification.
        else if (packet instanceof QueueUpdate) {
            QueueUpdate queueUpdate = (QueueUpdate)packet;
            if (queueUpdate.getPosition() != -1) {
                fireQueuePositionEvent(queueUpdate.getPosition());
            }
            if (queueUpdate.getRemaingTime() != -1) {
                fireQueueTimeEvent(queueUpdate.getRemaingTime());
            }
        }
    }

    /**
     * IQ packet to request joining the workgroup queue.
     */
    private class JoinQueuePacket extends IQ {

        private Map metaData;

        public JoinQueuePacket(String workgroup, Map metaData) {
            this.metaData = metaData;

            setTo(workgroup);
            setType(IQ.Type.SET);
        }

        public String getChildElementXML() {
            StringBuffer buf = new StringBuffer();

            buf.append("<join-queue xmlns=\"xmpp:workgroup\">");
            buf.append("<queue-notifications/>");

            // Add any meta-data.
            buf.append(MetaDataUtils.serializeMetaData(metaData));

            buf.append("</join-queue>");

            return buf.toString();
        }
    }
}