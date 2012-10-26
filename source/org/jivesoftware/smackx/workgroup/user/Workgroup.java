/**
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

package org.jivesoftware.smackx.workgroup.user;

import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smackx.workgroup.WorkgroupInvitation;
import org.jivesoftware.smackx.workgroup.WorkgroupInvitationListener;
import org.jivesoftware.smackx.workgroup.ext.forms.WorkgroupForm;
import org.jivesoftware.smackx.workgroup.packet.DepartQueuePacket;
import org.jivesoftware.smackx.workgroup.packet.QueueUpdate;
import org.jivesoftware.smackx.workgroup.packet.SessionID;
import org.jivesoftware.smackx.workgroup.packet.UserID;
import org.jivesoftware.smackx.workgroup.settings.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.MUCUser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides workgroup services for users. Users can join the workgroup queue, depart the
 * queue, find status information about their placement in the queue, and register to
 * be notified when they are routed to an agent.<p>
 * <p/>
 * This class only provides a users perspective into a workgroup and is not intended
 * for use by agents.
 *
 * @author Matt Tucker
 * @author Derek DeMoro
 */
public class Workgroup {

    private String workgroupJID;
    private Connection connection;
    private boolean inQueue;
    private List<WorkgroupInvitationListener> invitationListeners;
    private List<QueueListener> queueListeners;

    private int queuePosition = -1;
    private int queueRemainingTime = -1;

    /**
     * Creates a new workgroup instance using the specified workgroup JID
     * (eg support@workgroup.example.com) and XMPP connection. The connection must have
     * undergone a successful login before being used to construct an instance of
     * this class.
     *
     * @param workgroupJID the JID of the workgroup.
     * @param connection   an XMPP connection which must have already undergone a
     *                     successful login.
     */
    public Workgroup(String workgroupJID, Connection connection) {
        // Login must have been done before passing in connection.
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must login to server before creating workgroup.");
        }

        this.workgroupJID = workgroupJID;
        this.connection = connection;
        inQueue = false;
        invitationListeners = new ArrayList<WorkgroupInvitationListener>();
        queueListeners = new ArrayList<QueueListener>();

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

        /**
         * Internal handling of an invitation.Recieving an invitation removes the user from the queue.
         */
        MultiUserChat.addInvitationListener(connection,
                new org.jivesoftware.smackx.muc.InvitationListener() {
                    public void invitationReceived(Connection conn, String room, String inviter,
                                                   String reason, String password, Message message) {
                        inQueue = false;
                        queuePosition = -1;
                        queueRemainingTime = -1;
                    }
                });

        // Register a packet listener for all the messages sent to this client.
        PacketFilter typeFilter = new PacketTypeFilter(Message.class);

        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                handlePacket(packet);
            }
        }, typeFilter);
    }

    /**
     * Returns the name of this workgroup (eg support@example.com).
     *
     * @return the name of the workgroup.
     */
    public String getWorkgroupJID() {
        return workgroupJID;
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
     * Returns true if the workgroup is available for receiving new requests. The workgroup will be
     * available only when agents are available for this workgroup.
     *
     * @return true if the workgroup is available for receiving new requests.
     */
    public boolean isAvailable() {
        Presence directedPresence = new Presence(Presence.Type.available);
        directedPresence.setTo(workgroupJID);
        PacketFilter typeFilter = new PacketTypeFilter(Presence.class);
        PacketFilter fromFilter = new FromContainsFilter(workgroupJID);
        PacketCollector collector = connection.createPacketCollector(new AndFilter(fromFilter,
                typeFilter));

        connection.sendPacket(directedPresence);

        Presence response = (Presence)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            return false;
        }
        else if (response.getError() != null) {
            return false;
        }
        else {
            return Presence.Type.available == response.getType();
        }
    }

    /**
     * Returns the users current position in the workgroup queue. A value of 0 means
     * the user is next in line to be routed; therefore, if the queue position
     * is being displayed to the end user it is usually a good idea to add 1 to
     * the value this method returns before display. If the user is not currently
     * waiting in the workgroup, or no queue position information is available, -1
     * will be returned.
     *
     * @return the user's current position in the workgroup queue, or -1 if the
     *         position isn't available or if the user isn't in the queue.
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
     *         wait inthe workgroupu queue, or -1 if time information isn't available
     *         or if the user isn't int the queue.
     */
    public int getQueueRemainingTime() {
        return queueRemainingTime;
    }

    /**
     * Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user can leave the queue: <ul>
     * <p/>
     * <li>The user is routed to an agent, which triggers a GroupChat invitation.
     * <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     * <li>A server error occurs, or an administrator explicitly removes the user
     * from the queue.
     * </ul>
     * <p/>
     * A user cannot request to join the queue again if already in the queue. Therefore,
     * this method will throw an IllegalStateException if the user is already in the queue.<p>
     * <p/>
     * Some servers may be configured to require certain meta-data in order to
     * join the queue. In that case, the {@link #joinQueue(Form)} method should be
     * used instead of this method so that meta-data may be passed in.<p>
     * <p/>
     * The server tracks the conversations that a user has with agents over time. By
     * default, that tracking is done using the user's JID. However, this is not always
     * possible. For example, when the user is logged in anonymously using a web client.
     * In that case the user ID might be a randomly generated value put into a persistent
     * cookie or a username obtained via the session. A userID can be explicitly
     * passed in by using the {@link #joinQueue(Form, String)} method. When specified,
     * that userID will be used instead of the user's JID to track conversations. The
     * server will ignore a manually specified userID if the user's connection to the server
     * is not anonymous.
     *
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *                       that a connection failure occured or that the server explicitly rejected the
     *                       request to join the queue.
     */
    public void joinQueue() throws XMPPException {
        joinQueue(null);
    }

    /**
     * Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user can leave the queue: <ul>
     * <p/>
     * <li>The user is routed to an agent, which triggers a GroupChat invitation.
     * <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     * <li>A server error occurs, or an administrator explicitly removes the user
     * from the queue.
     * </ul>
     * <p/>
     * A user cannot request to join the queue again if already in the queue. Therefore,
     * this method will throw an IllegalStateException if the user is already in the queue.<p>
     * <p/>
     * Some servers may be configured to require certain meta-data in order to
     * join the queue.<p>
     * <p/>
     * The server tracks the conversations that a user has with agents over time. By
     * default, that tracking is done using the user's JID. However, this is not always
     * possible. For example, when the user is logged in anonymously using a web client.
     * In that case the user ID might be a randomly generated value put into a persistent
     * cookie or a username obtained via the session. A userID can be explicitly
     * passed in by using the {@link #joinQueue(Form, String)} method. When specified,
     * that userID will be used instead of the user's JID to track conversations. The
     * server will ignore a manually specified userID if the user's connection to the server
     * is not anonymous.
     *
     * @param answerForm the completed form the send for the join request.
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *                       that a connection failure occured or that the server explicitly rejected the
     *                       request to join the queue.
     */
    public void joinQueue(Form answerForm) throws XMPPException {
        joinQueue(answerForm, null);
    }

    /**
     * <p>Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user can leave the queue: <ul>
     * <p/>
     * <li>The user is routed to an agent, which triggers a GroupChat invitation.
     * <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     * <li>A server error occurs, or an administrator explicitly removes the user
     * from the queue.
     * </ul>
     * <p/>
     * A user cannot request to join the queue again if already in the queue. Therefore,
     * this method will throw an IllegalStateException if the user is already in the queue.<p>
     * <p/>
     * Some servers may be configured to require certain meta-data in order to
     * join the queue.<p>
     * <p/>
     * The server tracks the conversations that a user has with agents over time. By
     * default, that tracking is done using the user's JID. However, this is not always
     * possible. For example, when the user is logged in anonymously using a web client.
     * In that case the user ID might be a randomly generated value put into a persistent
     * cookie or a username obtained via the session. When specified, that userID will
     * be used instead of the user's JID to track conversations. The server will ignore a
     * manually specified userID if the user's connection to the server is not anonymous.
     *
     * @param answerForm the completed form associated with the join reqest.
     * @param userID     String that represents the ID of the user when using anonymous sessions
     *                   or <tt>null</tt> if a userID should not be used.
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *                       that a connection failure occured or that the server explicitly rejected the
     *                       request to join the queue.
     */
    public void joinQueue(Form answerForm, String userID) throws XMPPException {
        // If already in the queue ignore the join request.
        if (inQueue) {
            throw new IllegalStateException("Already in queue " + workgroupJID);
        }

        JoinQueuePacket joinPacket = new JoinQueuePacket(workgroupJID, answerForm, userID);


        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(joinPacket.getPacketID()));

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
     * <p>Joins the workgroup queue to wait to be routed to an agent. After joining
     * the queue, queue status events will be sent to indicate the user's position and
     * estimated time left in the queue. Once joining the queue, there are three ways
     * the user can leave the queue: <ul>
     * <p/>
     * <li>The user is routed to an agent, which triggers a GroupChat invitation.
     * <li>The user asks to leave the queue by calling the {@link #departQueue} method.
     * <li>A server error occurs, or an administrator explicitly removes the user
     * from the queue.
     * </ul>
     * <p/>
     * A user cannot request to join the queue again if already in the queue. Therefore,
     * this method will throw an IllegalStateException if the user is already in the queue.<p>
     * <p/>
     * Some servers may be configured to require certain meta-data in order to
     * join the queue.<p>
     * <p/>
     * The server tracks the conversations that a user has with agents over time. By
     * default, that tracking is done using the user's JID. However, this is not always
     * possible. For example, when the user is logged in anonymously using a web client.
     * In that case the user ID might be a randomly generated value put into a persistent
     * cookie or a username obtained via the session. When specified, that userID will
     * be used instead of the user's JID to track conversations. The server will ignore a
     * manually specified userID if the user's connection to the server is not anonymous.
     *
     * @param metadata metadata to create a dataform from.
     * @param userID   String that represents the ID of the user when using anonymous sessions
     *                 or <tt>null</tt> if a userID should not be used.
     * @throws XMPPException if an error occured joining the queue. An error may indicate
     *                       that a connection failure occured or that the server explicitly rejected the
     *                       request to join the queue.
     */
    public void joinQueue(Map<String,Object> metadata, String userID) throws XMPPException {
        // If already in the queue ignore the join request.
        if (inQueue) {
            throw new IllegalStateException("Already in queue " + workgroupJID);
        }

        // Build dataform from metadata
        Form form = new Form(Form.TYPE_SUBMIT);
        Iterator<String> iter = metadata.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            String value = metadata.get(name).toString();

            String escapedName = StringUtils.escapeForXML(name);
            String escapedValue = StringUtils.escapeForXML(value);

            FormField field = new FormField(escapedName);
            field.setType(FormField.TYPE_TEXT_SINGLE);
            form.addField(field);
            form.setAnswer(escapedName, escapedValue);
        }
        joinQueue(form, userID);
    }

    /**
     * Departs the workgroup queue. If the user is not currently in the queue, this
     * method will do nothing.<p>
     * <p/>
     * Normally, the user would not manually leave the queue. However, they may wish to
     * under certain circumstances -- for example, if they no longer wish to be routed
     * to an agent because they've been waiting too long.
     *
     * @throws XMPPException if an error occured trying to send the depart queue
     *                       request to the server.
     */
    public void departQueue() throws XMPPException {
        // If not in the queue ignore the depart request.
        if (!inQueue) {
            return;
        }

        DepartQueuePacket departPacket = new DepartQueuePacket(this.workgroupJID);
        PacketCollector collector = this.connection.createPacketCollector(new PacketIDFilter(departPacket.getPacketID()));

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
        synchronized (queueListeners) {
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
        synchronized (queueListeners) {
            queueListeners.remove(queueListener);
        }
    }

    /**
     * Adds an invitation listener that will be notified of groupchat invitations
     * from the workgroup for the the user that created this Workgroup instance.
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
    public void removeQueueListener(WorkgroupInvitationListener invitationListener) {
        synchronized (invitationListeners) {
            invitationListeners.remove(invitationListener);
        }
    }

    private void fireInvitationEvent(WorkgroupInvitation invitation) {
        synchronized (invitationListeners) {
            for (Iterator<WorkgroupInvitationListener> i = invitationListeners.iterator(); i.hasNext();) {
                WorkgroupInvitationListener listener = i.next();
                listener.invitationReceived(invitation);
            }
        }
    }

    private void fireQueueJoinedEvent() {
        synchronized (queueListeners) {
            for (Iterator<QueueListener> i = queueListeners.iterator(); i.hasNext();) {
                QueueListener listener = i.next();
                listener.joinedQueue();
            }
        }
    }

    private void fireQueueDepartedEvent() {
        synchronized (queueListeners) {
            for (Iterator<QueueListener> i = queueListeners.iterator(); i.hasNext();) {
                QueueListener listener = i.next();
                listener.departedQueue();
            }
        }
    }

    private void fireQueuePositionEvent(int currentPosition) {
        synchronized (queueListeners) {
            for (Iterator<QueueListener> i = queueListeners.iterator(); i.hasNext();) {
                QueueListener listener = i.next();
                listener.queuePositionUpdated(currentPosition);
            }
        }
    }

    private void fireQueueTimeEvent(int secondsRemaining) {
        synchronized (queueListeners) {
            for (Iterator<QueueListener> i = queueListeners.iterator(); i.hasNext();) {
                QueueListener listener = i.next();
                listener.queueWaitTimeUpdated(secondsRemaining);
            }
        }
    }

    // PacketListener Implementation.

    private void handlePacket(Packet packet) {
        if (packet instanceof Message) {
            Message msg = (Message)packet;
            // Check to see if the user left the queue.
            PacketExtension pe = msg.getExtension("depart-queue", "http://jabber.org/protocol/workgroup");
            PacketExtension queueStatus = msg.getExtension("queue-status", "http://jabber.org/protocol/workgroup");

            if (pe != null) {
                fireQueueDepartedEvent();
            }
            else if (queueStatus != null) {
                QueueUpdate queueUpdate = (QueueUpdate)queueStatus;
                if (queueUpdate.getPosition() != -1) {
                    fireQueuePositionEvent(queueUpdate.getPosition());
                }
                if (queueUpdate.getRemaingTime() != -1) {
                    fireQueueTimeEvent(queueUpdate.getRemaingTime());
                }
            }

            else {
                // Check if a room invitation was sent and if the sender is the workgroup
                MUCUser mucUser = (MUCUser)msg.getExtension("x", "http://jabber.org/protocol/muc#user");
                MUCUser.Invite invite = mucUser != null ? mucUser.getInvite() : null;
                if (invite != null && workgroupJID.equals(invite.getFrom())) {
                    String sessionID = null;
                    Map<String, List<String>> metaData = null;

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

                    WorkgroupInvitation inv = new WorkgroupInvitation(connection.getUser(), msg.getFrom(),
                            workgroupJID, sessionID, msg.getBody(),
                            msg.getFrom(), metaData);

                    fireInvitationEvent(inv);
                }
            }
        }
    }

    /**
     * IQ packet to request joining the workgroup queue.
     */
    private class JoinQueuePacket extends IQ {

        private String userID = null;
        private DataForm form;

        public JoinQueuePacket(String workgroup, Form answerForm, String userID) {
            this.userID = userID;

            setTo(workgroup);
            setType(IQ.Type.SET);

            form = answerForm.getDataFormToSend();
            addExtension(form);
        }

        public String getChildElementXML() {
            StringBuilder buf = new StringBuilder();

            buf.append("<join-queue xmlns=\"http://jabber.org/protocol/workgroup\">");
            buf.append("<queue-notifications/>");
            // Add the user unique identification if the session is anonymous
            if (connection.isAnonymous()) {
                buf.append(new UserID(userID).toXML());
            }

            // Append data form text
            buf.append(form.toXML());

            buf.append("</join-queue>");

            return buf.toString();
        }
    }

    /**
     * Returns a single chat setting based on it's identified key.
     *
     * @param key the key to find.
     * @return the ChatSetting if found, otherwise false.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public ChatSetting getChatSetting(String key) throws XMPPException {
        ChatSettings chatSettings = getChatSettings(key, -1);
        return chatSettings.getFirstEntry();
    }

    /**
     * Returns ChatSettings based on type.
     *
     * @param type the type of ChatSettings to return.
     * @return the ChatSettings of given type, otherwise null.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public ChatSettings getChatSettings(int type) throws XMPPException {
        return getChatSettings(null, type);
    }

    /**
     * Returns all ChatSettings.
     *
     * @return all ChatSettings of a given workgroup.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public ChatSettings getChatSettings() throws XMPPException {
        return getChatSettings(null, -1);
    }


    /**
     * Asks the workgroup for it's Chat Settings.
     *
     * @return key specify a key to retrieve only that settings. Otherwise for all settings, key should be null.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    private ChatSettings getChatSettings(String key, int type) throws XMPPException {
        ChatSettings request = new ChatSettings();
        if (key != null) {
            request.setKey(key);
        }
        if (type != -1) {
            request.setType(type);
        }
        request.setType(IQ.Type.GET);
        request.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        connection.sendPacket(request);


        ChatSettings response = (ChatSettings)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }

    /**
     * The workgroup service may be configured to send email. This queries the Workgroup Service
     * to see if the email service has been configured and is available.
     *
     * @return true if the email service is available, otherwise return false.
     */
    public boolean isEmailAvailable() {
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);

        try {
            String workgroupService = StringUtils.parseServer(workgroupJID);
            DiscoverInfo infoResult = discoManager.discoverInfo(workgroupService);
            return infoResult.containsFeature("jive:email:provider");
        }
        catch (XMPPException e) {
            return false;
        }
    }

    /**
     * Asks the workgroup for it's Offline Settings.
     *
     * @return offlineSettings the offline settings for this workgroup.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public OfflineSettings getOfflineSettings() throws XMPPException {
        OfflineSettings request = new OfflineSettings();
        request.setType(IQ.Type.GET);
        request.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        connection.sendPacket(request);


        OfflineSettings response = (OfflineSettings)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }

    /**
     * Asks the workgroup for it's Sound Settings.
     *
     * @return soundSettings the sound settings for the specified workgroup.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public SoundSettings getSoundSettings() throws XMPPException {
        SoundSettings request = new SoundSettings();
        request.setType(IQ.Type.GET);
        request.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        connection.sendPacket(request);


        SoundSettings response = (SoundSettings)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }

    /**
     * Asks the workgroup for it's Properties
     *
     * @return the WorkgroupProperties for the specified workgroup.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public WorkgroupProperties getWorkgroupProperties() throws XMPPException {
        WorkgroupProperties request = new WorkgroupProperties();
        request.setType(IQ.Type.GET);
        request.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        connection.sendPacket(request);


        WorkgroupProperties response = (WorkgroupProperties)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }

    /**
     * Asks the workgroup for it's Properties
     *
     * @param jid the jid of the user who's information you would like the workgroup to retreive.
     * @return the WorkgroupProperties for the specified workgroup.
     * @throws XMPPException if an error occurs while getting information from the server.
     */
    public WorkgroupProperties getWorkgroupProperties(String jid) throws XMPPException {
        WorkgroupProperties request = new WorkgroupProperties();
        request.setJid(jid);
        request.setType(IQ.Type.GET);
        request.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        connection.sendPacket(request);


        WorkgroupProperties response = (WorkgroupProperties)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }


    /**
     * Returns the Form to use for all clients of a workgroup. It is unlikely that the server
     * will change the form (without a restart) so it is safe to keep the returned form
     * for future submissions.
     *
     * @return the Form to use for searching transcripts.
     * @throws XMPPException if an error occurs while sending the request to the server.
     */
    public Form getWorkgroupForm() throws XMPPException {
        WorkgroupForm workgroupForm = new WorkgroupForm();
        workgroupForm.setType(IQ.Type.GET);
        workgroupForm.setTo(workgroupJID);

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(workgroupForm.getPacketID()));
        connection.sendPacket(workgroupForm);

        WorkgroupForm response = (WorkgroupForm)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return Form.getFormFrom(response);
    }

    /*
    public static void main(String args[]) throws Exception {
        Connection con = new XMPPConnection("anteros");
        con.connect();
        con.loginAnonymously();

        Workgroup workgroup = new Workgroup("demo@workgroup.anteros", con);
        WorkgroupProperties props = workgroup.getWorkgroupProperties("derek@anteros.com");

        System.out.print(props);
        con.disconnect();
    }
    */


}