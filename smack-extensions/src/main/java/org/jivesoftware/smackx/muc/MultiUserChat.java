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

package org.jivesoftware.smackx.muc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithSubjectFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.filter.ToMatchesFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucAlreadyJoinedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucNotJoinedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MissingMucCreationAcknowledgeException;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;
import org.jivesoftware.smackx.muc.filter.MUCUserStatusCodeFilter;
import org.jivesoftware.smackx.muc.packet.Destroy;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCOwner;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.muc.packet.MUCUser.Status;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.util.cache.ExpirationCache;

/**
 * A MultiUserChat room (XEP-45), created with {@link MultiUserChatManager#getMultiUserChat(EntityBareJid)}.
 * <p>
 * A MultiUserChat is a conversation that takes place among many users in a virtual
 * room. A room could have many occupants with different affiliation and roles.
 * Possible affiliations are "owner", "admin", "member", and "outcast". Possible roles
 * are "moderator", "participant", and "visitor". Each role and affiliation guarantees
 * different privileges (e.g. Send messages to all occupants, Kick participants and visitors,
 * Grant voice, Edit member list, etc.).
 * </p>
 * <p>
 * <b>Note:</b> Make sure to leave the MUC ({@link #leave()}) when you don't need it anymore or
 * otherwise you may leak the instance.
 * </p>
 *
 * @author Gaston Dombiak
 * @author Larry Kirschner
 * @author Florian Schmaus
 */
public class MultiUserChat {
    private static final Logger LOGGER = Logger.getLogger(MultiUserChat.class.getName());

    private static final ExpirationCache<DomainBareJid, Void> KNOWN_MUC_SERVICES = new ExpirationCache<>(
                    100, 1000 * 60 * 60 * 24);

    private final XMPPConnection connection;
    private final EntityBareJid room;
    private final MultiUserChatManager multiUserChatManager;
    private final Map<EntityFullJid, Presence> occupantsMap = new ConcurrentHashMap<>();

    private final Set<InvitationRejectionListener> invitationRejectionListeners = new CopyOnWriteArraySet<InvitationRejectionListener>();
    private final Set<SubjectUpdatedListener> subjectUpdatedListeners = new CopyOnWriteArraySet<SubjectUpdatedListener>();
    private final Set<UserStatusListener> userStatusListeners = new CopyOnWriteArraySet<UserStatusListener>();
    private final Set<ParticipantStatusListener> participantStatusListeners = new CopyOnWriteArraySet<ParticipantStatusListener>();
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<MessageListener>();
    private final Set<PresenceListener> presenceListeners = new CopyOnWriteArraySet<PresenceListener>();
    private final Set<PresenceListener> presenceInterceptors = new CopyOnWriteArraySet<PresenceListener>();

    /**
     * This filter will match all stanzas send from the groupchat or from one if
     * the groupchat participants, i.e. it filters only the bare JID of the from
     * attribute against the JID of the MUC.
     */
    private final StanzaFilter fromRoomFilter;

    /**
     * Same as {@link #fromRoomFilter} together with {@link MessageTypeFilter#GROUPCHAT}.
     */
    private final StanzaFilter fromRoomGroupchatFilter;

    private final StanzaListener presenceInterceptor;
    private final StanzaListener messageListener;
    private final StanzaListener presenceListener;
    private final StanzaListener subjectListener;

    private static final StanzaFilter DECLINE_FILTER = new AndFilter(MessageTypeFilter.NORMAL,
                    new StanzaExtensionFilter(MUCUser.ELEMENT, MUCUser.NAMESPACE));
    private final StanzaListener declinesListener;

    private String subject;
    private Resourcepart nickname;
    private boolean joined = false;
    private StanzaCollector messageCollector;

    MultiUserChat(XMPPConnection connection, EntityBareJid room, MultiUserChatManager multiUserChatManager) {
        this.connection = connection;
        this.room = room;
        this.multiUserChatManager = multiUserChatManager;

        fromRoomFilter = FromMatchesFilter.create(room);
        fromRoomGroupchatFilter = new AndFilter(fromRoomFilter, MessageTypeFilter.GROUPCHAT);

        messageListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException {
                Message message = (Message) packet;
                for (MessageListener listener : messageListeners) {
                    listener.processMessage(message);
                }
            }
        };

        // Create a listener for subject updates.
        subjectListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Message msg = (Message) packet;
                EntityFullJid from = msg.getFrom().asEntityFullJidIfPossible();
                if (from == null) {
                    LOGGER.warning("Message subject not changed by a full JID: " + msg.getFrom());
                    return;
                }
                // Update the room subject
                subject = msg.getSubject();
                // Fire event for subject updated listeners
                for (SubjectUpdatedListener listener : subjectUpdatedListeners) {
                    listener.subjectUpdated(subject, from);
                }
            }
        };

        // Create a listener for all presence updates.
        presenceListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Presence presence = (Presence) packet;
                final EntityFullJid from = presence.getFrom().asEntityFullJidIfPossible();
                if (from == null) {
                    LOGGER.warning("Presence not from a full JID: " + presence.getFrom());
                    return;
                }
                String myRoomJID = MultiUserChat.this.room + "/" + nickname;
                boolean isUserStatusModification = presence.getFrom().equals(myRoomJID);
                switch (presence.getType()) {
                case available:
                    Presence oldPresence = occupantsMap.put(from, presence);
                    if (oldPresence != null) {
                        // Get the previous occupant's affiliation & role
                        MUCUser mucExtension = MUCUser.from(oldPresence);
                        MUCAffiliation oldAffiliation = mucExtension.getItem().getAffiliation();
                        MUCRole oldRole = mucExtension.getItem().getRole();
                        // Get the new occupant's affiliation & role
                        mucExtension = MUCUser.from(packet);
                        MUCAffiliation newAffiliation = mucExtension.getItem().getAffiliation();
                        MUCRole newRole = mucExtension.getItem().getRole();
                        // Fire role modification events
                        checkRoleModifications(oldRole, newRole, isUserStatusModification, from);
                        // Fire affiliation modification events
                        checkAffiliationModifications(
                            oldAffiliation,
                            newAffiliation,
                            isUserStatusModification,
                            from);
                    }
                    else {
                        // A new occupant has joined the room
                        if (!isUserStatusModification) {
                            for (ParticipantStatusListener listener : participantStatusListeners) {
                                listener.joined(from);
                            }
                        }
                    }
                    break;
                case unavailable:
                    occupantsMap.remove(from);
                    MUCUser mucUser = MUCUser.from(packet);
                    if (mucUser != null && mucUser.hasStatus()) {
                        // Fire events according to the received presence code
                        checkPresenceCode(
                            mucUser.getStatus(),
                            presence.getFrom().equals(myRoomJID),
                            mucUser,
                            from);
                    } else {
                        // An occupant has left the room
                        if (!isUserStatusModification) {
                            for (ParticipantStatusListener listener : participantStatusListeners) {
                                listener.left(from);
                            }
                        }
                    }
                    break;
                default:
                    break;
                }
                for (PresenceListener listener : presenceListeners) {
                    listener.processPresence(presence);
                }
            }
        };

        // Listens for all messages that include a MUCUser extension and fire the invitation
        // rejection listeners if the message includes an invitation rejection.
        declinesListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Message message = (Message) packet;
                // Get the MUC User extension
                MUCUser mucUser = MUCUser.from(packet);
                MUCUser.Decline rejection = mucUser.getDecline();
                // Check if the MUCUser informs that the invitee has declined the invitation
                if (rejection == null) {
                    return;
                }
                // Fire event for invitation rejection listeners
                fireInvitationRejectionListeners(message, rejection);
            }
        };

        presenceInterceptor = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Presence presence = (Presence) packet;
                for (PresenceListener interceptor : presenceInterceptors) {
                    interceptor.processPresence(presence);
                }
            }
        };
    }


    /**
     * Returns the name of the room this MultiUserChat object represents.
     *
     * @return the multi user chat room name.
     */
    public EntityBareJid getRoom() {
        return room;
    }

    /**
     * Enter a room, as described in XEP-45 7.2.
     *
     * @param conf the configuration used to enter the room.
     * @return the returned presence by the service after the client send the initial presence in order to enter the room.
     * @throws NotConnectedException
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws InterruptedException
     * @throws NotAMucServiceException 
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#enter">XEP-45 7.2 Entering a Room</a>
     */
    private Presence enter(MucEnterConfiguration conf) throws NotConnectedException, NoResponseException,
                    XMPPErrorException, InterruptedException, NotAMucServiceException {
        final DomainBareJid mucService = room.asDomainBareJid();
        if (!KNOWN_MUC_SERVICES.containsKey(mucService)) {
            if (multiUserChatManager.providesMucService(mucService)) {
                KNOWN_MUC_SERVICES.put(mucService, null);
            } else {
                throw new NotAMucServiceException(this);
            }
        }
        // We enter a room by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        Presence joinPresence = conf.getJoinPresence(this);

        // Setup the messageListeners and presenceListeners *before* the join presence is send.
        connection.addSyncStanzaListener(messageListener, fromRoomGroupchatFilter);
        connection.addSyncStanzaListener(presenceListener, new AndFilter(fromRoomFilter,
                        StanzaTypeFilter.PRESENCE));
        connection.addSyncStanzaListener(subjectListener, new AndFilter(fromRoomFilter,
                        MessageWithSubjectFilter.INSTANCE, new NotFilter(MessageTypeFilter.ERROR)));
        connection.addSyncStanzaListener(declinesListener, DECLINE_FILTER);
        connection.addPacketInterceptor(presenceInterceptor, new AndFilter(ToMatchesFilter.create(room),
                        StanzaTypeFilter.PRESENCE));
        messageCollector = connection.createStanzaCollector(fromRoomGroupchatFilter);

        // Wait for a presence packet back from the server.
        // @formatter:off
        StanzaFilter responseFilter = new AndFilter(StanzaTypeFilter.PRESENCE,
                        new OrFilter(
                            // We use a bare JID filter for positive responses, since the MUC service/room may rewrite the nickname.
                            new AndFilter(FromMatchesFilter.createBare(getRoom()), MUCUserStatusCodeFilter.STATUS_110_PRESENCE_TO_SELF),
                            // In case there is an error reply, we match on an error presence with the same stanza id and from the full
                            // JID we send the join presence to.
                            new AndFilter(FromMatchesFilter.createFull(joinPresence.getTo()), new StanzaIdFilter(joinPresence), PresenceTypeFilter.ERROR)
                        )
                    );
        // @formatter:on
        Presence presence;
        try {
            presence = connection.createStanzaCollectorAndSend(responseFilter, joinPresence).nextResultOrThrow(conf.getTimeout());
        }
        catch (NotConnectedException | InterruptedException | NoResponseException | XMPPErrorException e) {
            // Ensure that all callbacks are removed if there is an exception
            removeConnectionCallbacks();
            throw e;
        }

        // This presence must be send from a full JID. We use the resourcepart of this JID as nick, since the room may
        // performed roomnick rewriting
        this.nickname = presence.getFrom().asEntityFullJidIfPossible().getResourcepart();
        joined = true;

        // Update the list of joined rooms
        multiUserChatManager.addJoinedRoom(room);
        return presence;
    }

    /**
     * Get a new MUC enter configuration builder.
     * 
     * @param nickname the nickname used when entering the MUC room.
     * @return a new MUC enter configuration builder.
     * @since 4.2
     */
    public MucEnterConfiguration.Builder getEnterConfigurationBuilder(Resourcepart nickname) {
        return new MucEnterConfiguration.Builder(nickname, connection.getReplyTimeout());
    }

    /**
     * Creates the room according to some default configuration, assign the requesting user as the
     * room owner, and add the owner to the room but not allow anyone else to enter the room
     * (effectively "locking" the room). The requesting user will join the room under the specified
     * nickname as soon as the room has been created.
     * <p>
     * To create an "Instant Room", that means a room with some default configuration that is
     * available for immediate access, the room's owner should send an empty form after creating the
     * room. Simply call {@link MucCreateConfigFormHandle#makeInstant()} on the returned {@link MucCreateConfigFormHandle}.
     * </p>
     * <p>
     * To create a "Reserved Room", that means a room manually configured by the room creator before
     * anyone is allowed to enter, the room's owner should complete and send a form after creating
     * the room. Once the completed configuration form is sent to the server, the server will unlock
     * the room. You can use the returned {@link MucCreateConfigFormHandle} to configure the room.
     * </p>
     * 
     * @param nickname the nickname to use.
     * @return a handle to the MUC create configuration form API.
     * @throws XMPPErrorException if the room couldn't be created for some reason (e.g. 405 error if
     *         the user is not allowed to create the room)
     * @throws NoResponseException if there was no response from the server.
     * @throws InterruptedException 
     * @throws NotConnectedException 
     * @throws MucAlreadyJoinedException 
     * @throws MissingMucCreationAcknowledgeException 
     * @throws NotAMucServiceException 
     */
    public synchronized MucCreateConfigFormHandle create(Resourcepart nickname) throws NoResponseException,
                    XMPPErrorException, InterruptedException, MucAlreadyJoinedException,
                    NotConnectedException, MissingMucCreationAcknowledgeException, NotAMucServiceException {
        if (joined) {
            throw new MucAlreadyJoinedException();
        }

        MucCreateConfigFormHandle mucCreateConfigFormHandle = createOrJoin(nickname);
        if (mucCreateConfigFormHandle != null) {
            // We successfully created a new room
            return mucCreateConfigFormHandle;
        }
        // We need to leave the room since it seems that the room already existed
        leave();
        throw new MissingMucCreationAcknowledgeException();
    }

    /**
     * Create or join the MUC room with the given nickname.
     * 
     * @param nickname the nickname to use in the MUC room.
     * @return A {@link MucCreateConfigFormHandle} if the room was created while joining, or {@code null} if the room was just joined.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws InterruptedException 
     * @throws NotConnectedException 
     * @throws MucAlreadyJoinedException 
     * @throws NotAMucServiceException 
     */
    public synchronized MucCreateConfigFormHandle createOrJoin(Resourcepart nickname) throws NoResponseException, XMPPErrorException,
                    InterruptedException, MucAlreadyJoinedException, NotConnectedException, NotAMucServiceException {
        MucEnterConfiguration mucEnterConfiguration = getEnterConfigurationBuilder(nickname).build();
        return createOrJoin(mucEnterConfiguration);
    }

    /**
     * Like {@link #create(Resourcepart)}, but will return true if the room creation was acknowledged by
     * the service (with an 201 status code). It's up to the caller to decide, based on the return
     * value, if he needs to continue sending the room configuration. If false is returned, the room
     * already existed and the user is able to join right away, without sending a form.
     *
     * @param nickname the nickname to use.
     * @param password the password to use.
     * @param history the amount of discussion history to receive while joining a room.
     * @param timeout the amount of time to wait for a reply from the MUC service(in milliseconds).
     * @return A {@link MucCreateConfigFormHandle} if the room was created while joining, or {@code null} if the room was just joined.
     * @throws XMPPErrorException if the room couldn't be created for some reason (e.g. 405 error if
     *         the user is not allowed to create the room)
     * @throws NoResponseException if there was no response from the server.
     * @throws InterruptedException 
     * @throws MucAlreadyJoinedException if the MUC is already joined
     * @throws NotConnectedException 
     * @throws NotAMucServiceException 
     * @deprecated use {@link #createOrJoin(MucEnterConfiguration)} instead.
     */
    @Deprecated
    public MucCreateConfigFormHandle createOrJoin(Resourcepart nickname, String password, DiscussionHistory history, long timeout)
                    throws NoResponseException, XMPPErrorException, InterruptedException, MucAlreadyJoinedException, NotConnectedException, NotAMucServiceException {
        MucEnterConfiguration.Builder builder = getEnterConfigurationBuilder(nickname).withPassword(
                        password).timeoutAfter(timeout);

        return createOrJoin(builder.build());
    }

    /**
     * Like {@link #create(Resourcepart)}, but will return a {@link MucCreateConfigFormHandle} if the room creation was acknowledged by
     * the service (with an 201 status code). It's up to the caller to decide, based on the return
     * value, if he needs to continue sending the room configuration. If {@code null} is returned, the room
     * already existed and the user is able to join right away, without sending a form.
     *
     * @param mucEnterConfiguration the configuration used to enter the MUC.
     * @return A {@link MucCreateConfigFormHandle} if the room was created while joining, or {@code null} if the room was just joined.
     * @throws XMPPErrorException if the room couldn't be created for some reason (e.g. 405 error if
     *         the user is not allowed to create the room)
     * @throws NoResponseException if there was no response from the server.
     * @throws InterruptedException 
     * @throws MucAlreadyJoinedException if the MUC is already joined
     * @throws NotConnectedException 
     * @throws NotAMucServiceException 
     */
    public synchronized MucCreateConfigFormHandle createOrJoin(MucEnterConfiguration mucEnterConfiguration)
                    throws NoResponseException, XMPPErrorException, InterruptedException, MucAlreadyJoinedException, NotConnectedException, NotAMucServiceException {
        if (joined) {
            throw new MucAlreadyJoinedException();
        }

        Presence presence = enter(mucEnterConfiguration);

        // Look for confirmation of room creation from the server
        MUCUser mucUser = MUCUser.from(presence);
        if (mucUser != null && mucUser.getStatus().contains(Status.ROOM_CREATED_201)) {
            // Room was created and the user has joined the room
            return new MucCreateConfigFormHandle();
        }
        return null;
    }

    /**
     * A handle used to configure a newly created room. As long as the room is not configured it will be locked, which
     * means that no one is able to join. The room will become unlocked as soon it got configured. In order to create an
     * instant room, use {@link #makeInstant()}.
     * <p>
     * For advanced configuration options, use {@link MultiUserChat#getConfigurationForm()}, get the answer form with
     * {@link Form#createAnswerForm()}, fill it out and send it back to the room with
     * {@link MultiUserChat#sendConfigurationForm(Form)}.
     * </p>
     */
    public class MucCreateConfigFormHandle {

        /**
         * Create an instant room. The default configuration will be accepted and the room will become unlocked, i.e.
         * other users are able to join.
         *
         * @throws NoResponseException
         * @throws XMPPErrorException
         * @throws NotConnectedException
         * @throws InterruptedException
         * @see <a href="http://www.xmpp.org/extensions/xep-0045.html#createroom-instant">XEP-45 § 10.1.2 Creating an
         *      Instant Room</a>
         */
        public void makeInstant() throws NoResponseException, XMPPErrorException, NotConnectedException,
                        InterruptedException {
            sendConfigurationForm(new Form(DataForm.Type.submit));
        }

        /**
         * Alias for {@link MultiUserChat#getConfigFormManager()}.
         *
         * @return a MUC configuration form manager for this room.
         * @throws NoResponseException
         * @throws XMPPErrorException
         * @throws NotConnectedException
         * @throws InterruptedException
         * @see MultiUserChat#getConfigFormManager()
         */
        public MucConfigFormManager getConfigFormManager() throws NoResponseException,
                        XMPPErrorException, NotConnectedException, InterruptedException {
            return MultiUserChat.this.getConfigFormManager();
        }
    }

    /**
     * Create or join a MUC if it is necessary, i.e. if not the MUC is not already joined.
     *
     * @param nickname the required nickname to use.
     * @param password the optional password required to join
     * @return A {@link MucCreateConfigFormHandle} if the room was created while joining, or {@code null} if the room was just joined.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotAMucServiceException 
     */
    public MucCreateConfigFormHandle createOrJoinIfNecessary(Resourcepart nickname, String password) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, NotAMucServiceException {
        if (isJoined()) {
            return null;
        }
        MucEnterConfiguration mucEnterConfiguration = getEnterConfigurationBuilder(nickname).withPassword(
                        password).build();
        try {
            return createOrJoin(mucEnterConfiguration);
        }
        catch (MucAlreadyJoinedException e) {
            return null;
        }
    }

    /**
     * Joins the chat room using the specified nickname. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname. The default connection timeout for a reply
     * from the group chat server that the join succeeded will be used. After
     * joining the room, the room will decide the amount of history to send.
     *
     * @param nickname the nickname to use.
     * @throws NoResponseException 
     * @throws XMPPErrorException if an error occurs joining the room. In particular, a
     *      401 error can occur if no password was provided and one is required; or a
     *      403 error can occur if the user is banned; or a
     *      404 error can occur if the room does not exist or is locked; or a
     *      407 error can occur if user is not on the member list; or a
     *      409 error can occur if someone is already in the group chat with the same nickname.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws NotAMucServiceException 
     */
    public void join(Resourcepart nickname) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotAMucServiceException {
        MucEnterConfiguration.Builder builder = getEnterConfigurationBuilder(nickname);
        join(builder.build());
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname. The default connection timeout for a reply
     * from the group chat server that the join succeeded will be used. After
     * joining the room, the room will decide the amount of history to send.<p>
     *
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.
     *
     * @param nickname the nickname to use.
     * @param password the password to use.
     * @throws XMPPErrorException if an error occurs joining the room. In particular, a
     *      401 error can occur if no password was provided and one is required; or a
     *      403 error can occur if the user is banned; or a
     *      404 error can occur if the room does not exist or is locked; or a
     *      407 error can occur if user is not on the member list; or a
     *      409 error can occur if someone is already in the group chat with the same nickname.
     * @throws InterruptedException 
     * @throws NotConnectedException 
     * @throws NoResponseException if there was no response from the server. 
     * @throws NotAMucServiceException 
     */
    public void join(Resourcepart nickname, String password) throws XMPPErrorException, InterruptedException, NoResponseException, NotConnectedException, NotAMucServiceException {
        MucEnterConfiguration.Builder builder = getEnterConfigurationBuilder(nickname).withPassword(
                        password);
        join(builder.build());
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname.<p>
     *
     * To control the amount of history to receive while joining a room you will need to provide
     * a configured DiscussionHistory object.<p>
     *
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.<p>
     *
     * If the room does not already exist when the user seeks to enter it, the server will
     * decide to create a new room or not.
     *
     * @param nickname the nickname to use.
     * @param password the password to use.
     * @param history the amount of discussion history to receive while joining a room.
     * @param timeout the amount of time to wait for a reply from the MUC service(in milleseconds).
     * @throws XMPPErrorException if an error occurs joining the room. In particular, a
     *      401 error can occur if no password was provided and one is required; or a
     *      403 error can occur if the user is banned; or a
     *      404 error can occur if the room does not exist or is locked; or a
     *      407 error can occur if user is not on the member list; or a
     *      409 error can occur if someone is already in the group chat with the same nickname.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws NotAMucServiceException 
     * @deprecated use {@link #join(MucEnterConfiguration)} instead.
     */
    @Deprecated
    public void join(
        Resourcepart nickname,
        String password,
        DiscussionHistory history,
        long timeout)
        throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException, NotAMucServiceException {
        MucEnterConfiguration.Builder builder = getEnterConfigurationBuilder(nickname).withPassword(
                        password).timeoutAfter(timeout);

        join(builder.build());
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname.<p>
     *
     * To control the amount of history to receive while joining a room you will need to provide
     * a configured DiscussionHistory object.<p>
     *
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.<p>
     *
     * If the room does not already exist when the user seeks to enter it, the server will
     * decide to create a new room or not.
     *
     * @param mucEnterConfiguration the configuration used to enter the MUC.
     * @throws XMPPErrorException if an error occurs joining the room. In particular, a
     *      401 error can occur if no password was provided and one is required; or a
     *      403 error can occur if the user is banned; or a
     *      404 error can occur if the room does not exist or is locked; or a
     *      407 error can occur if user is not on the member list; or a
     *      409 error can occur if someone is already in the group chat with the same nickname.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws NotAMucServiceException 
     */
    public synchronized void join(MucEnterConfiguration mucEnterConfiguration)
        throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException, NotAMucServiceException {
        // If we've already joined the room, leave it before joining under a new
        // nickname.
        if (joined) {
            leave();
        }
        enter(mucEnterConfiguration);
    }

    /**
     * Returns true if currently in the multi user chat (after calling the {@link
     * #join(Resourcepart)} method).
     *
     * @return true if currently in the multi user chat room.
     */
    public boolean isJoined() {
        return joined;
    }

    /**
     * Leave the chat room.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public synchronized void leave() throws NotConnectedException, InterruptedException {
        // If not joined already, do nothing.
        if (!joined) {
            return;
        }
        // We leave a room by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        Presence leavePresence = new Presence(Presence.Type.unavailable);
        leavePresence.setTo(JidCreate.fullFrom(room, nickname));
        connection.sendStanza(leavePresence);
        // Reset occupant information.
        occupantsMap.clear();
        nickname = null;
        joined = false;
        userHasLeft();
    }

    /**
     * Get a {@link MucConfigFormManager} to configure this room.
     * <p>
     * Only room owners are able to configure a room.
     * </p>
     *
     * @return a MUC configuration form manager for this room.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#roomconfig">XEP-45 § 10.2 Subsequent Room Configuration</a>
     * @since 4.2
     */
    public MucConfigFormManager getConfigFormManager() throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return new MucConfigFormManager(this);
    }

    /**
     * Returns the room's configuration form that the room's owner can use or <tt>null</tt> if
     * no configuration is possible. The configuration form allows to set the room's language,
     * enable logging, specify room's type, etc..
     *
     * @return the Form that contains the fields to complete together with the instrucions or
     * <tt>null</tt> if no configuration is possible.
     * @throws XMPPErrorException if an error occurs asking the configuration form for the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Form getConfigurationForm() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.get);

        IQ answer = connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
        return Form.getFormFrom(answer);
    }

    /**
     * Sends the completed configuration form to the server. The room will be configured
     * with the new settings defined in the form.
     *
     * @param form the form with the new settings.
     * @throws XMPPErrorException if an error occurs setting the new rooms' configuration.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException
     */
    public void sendConfigurationForm(Form form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.set);
        iq.addExtension(form.getDataFormToSend());

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
    }

    /**
     * Returns the room's registration form that an unaffiliated user, can use to become a member
     * of the room or <tt>null</tt> if no registration is possible. Some rooms may restrict the
     * privilege to register members and allow only room admins to add new members.<p>
     *
     * If the user requesting registration requirements is not allowed to register with the room
     * (e.g. because that privilege has been restricted), the room will return a "Not Allowed"
     * error to the user (error code 405).
     *
     * @return the registration Form that contains the fields to complete together with the
     * instrucions or <tt>null</tt> if no registration is possible.
     * @throws XMPPErrorException if an error occurs asking the registration form for the room or a
     * 405 error if the user is not allowed to register with the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Form getRegistrationForm() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Registration reg = new Registration();
        reg.setType(IQ.Type.get);
        reg.setTo(room);

        IQ result = connection.createStanzaCollectorAndSend(reg).nextResultOrThrow();
        return Form.getFormFrom(result);
    }

    /**
     * Sends the completed registration form to the server. After the user successfully submits
     * the form, the room may queue the request for review by the room admins or may immediately
     * add the user to the member list by changing the user's affiliation from "none" to "member.<p>
     *
     * If the desired room nickname is already reserved for that room, the room will return a
     * "Conflict" error to the user (error code 409). If the room does not support registration,
     * it will return a "Service Unavailable" error to the user (error code 503).
     *
     * @param form the completed registration form.
     * @throws XMPPErrorException if an error occurs submitting the registration form. In particular, a
     *      409 error can occur if the desired room nickname is already reserved for that room;
     *      or a 503 error can occur if the room does not support registration.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendRegistrationForm(Form form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Registration reg = new Registration();
        reg.setType(IQ.Type.set);
        reg.setTo(room);
        reg.addExtension(form.getDataFormToSend());

        connection.createStanzaCollectorAndSend(reg).nextResultOrThrow();
    }

    /**
     * Sends a request to the server to destroy the room. The sender of the request
     * should be the room's owner. If the sender of the destroy request is not the room's owner
     * then the server will answer a "Forbidden" error (403).
     *
     * @param reason the reason for the room destruction.
     * @param alternateJID the JID of an alternate location.
     * @throws XMPPErrorException if an error occurs while trying to destroy the room.
     *      An error can occur which will be wrapped by an XMPPException --
     *      XMPP error code 403. The error code can be used to present more
     *      appropiate error messages to end-users.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void destroy(String reason, EntityBareJid alternateJID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.set);

        // Create the reason for the room destruction
        Destroy destroy = new Destroy(alternateJID, reason);
        iq.setDestroy(destroy);

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();

        // Reset occupant information.
        occupantsMap.clear();
        nickname = null;
        joined = false;
        userHasLeft();
    }

    /**
     * Invites another user to the room in which one is an occupant. The invitation
     * will be sent to the room which in turn will forward the invitation to the invitee.<p>
     *
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the the invitee may be added to the member list.
     *
     * @param user the user to invite to the room.(e.g. hecate@shakespeare.lit)
     * @param reason the reason why the user is being invited.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void invite(EntityBareJid user, String reason) throws NotConnectedException, InterruptedException {
        invite(new Message(), user, reason);
    }

    /**
     * Invites another user to the room in which one is an occupant using a given Message. The invitation
     * will be sent to the room which in turn will forward the invitation to the invitee.<p>
     *
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the the invitee may be added to the member list.
     *
     * @param message the message to use for sending the invitation.
     * @param user the user to invite to the room.(e.g. hecate@shakespeare.lit)
     * @param reason the reason why the user is being invited.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void invite(Message message, EntityBareJid user, String reason) throws NotConnectedException, InterruptedException {
        // TODO listen for 404 error code when inviter supplies a non-existent JID
        message.setTo(room);

        // Create the MUCUser packet that will include the invitation
        MUCUser mucUser = new MUCUser();
        MUCUser.Invite invite = new MUCUser.Invite(reason, user);
        mucUser.setInvite(invite);
        // Add the MUCUser packet that includes the invitation to the message
        message.addExtension(mucUser);

        connection.sendStanza(message);
    }

    /**
     * Adds a listener to invitation rejections notifications. The listener will be fired anytime
     * an invitation is declined.
     *
     * @param listener an invitation rejection listener.
     * @return true if the listener was not already added.
     */
    public boolean addInvitationRejectionListener(InvitationRejectionListener listener) {
         return invitationRejectionListeners.add(listener);
    }

    /**
     * Removes a listener from invitation rejections notifications. The listener will be fired
     * anytime an invitation is declined.
     *
     * @param listener an invitation rejection listener.
     * @return true if the listener was registered and is now removed.
     */
    public boolean removeInvitationRejectionListener(InvitationRejectionListener listener) {
        return invitationRejectionListeners.remove(listener);
    }

    /**
     * Fires invitation rejection listeners.
     *
     * @param invitee the user being invited.
     * @param reason the reason for the rejection
     */
    private void fireInvitationRejectionListeners(Message message, MUCUser.Decline rejection) {
        EntityBareJid invitee = rejection.getFrom();
        String reason = rejection.getReason();
        InvitationRejectionListener[] listeners;
        synchronized (invitationRejectionListeners) {
            listeners = new InvitationRejectionListener[invitationRejectionListeners.size()];
            invitationRejectionListeners.toArray(listeners);
        }
        for (InvitationRejectionListener listener : listeners) {
            listener.invitationDeclined(invitee, reason, message, rejection);
        }
    }

    /**
     * Adds a listener to subject change notifications. The listener will be fired anytime
     * the room's subject changes.
     *
     * @param listener a subject updated listener.
     * @return true if the listener was not already added.
     */
    public boolean addSubjectUpdatedListener(SubjectUpdatedListener listener) {
        return subjectUpdatedListeners.add(listener);
    }

    /**
     * Removes a listener from subject change notifications. The listener will be fired
     * anytime the room's subject changes.
     *
     * @param listener a subject updated listener.
     * @return true if the listener was registered and is now removed.
     */
    public boolean removeSubjectUpdatedListener(SubjectUpdatedListener listener) {
        return subjectUpdatedListeners.remove(listener);
    }

    /**
     * Adds a new {@link StanzaListener} that will be invoked every time a new presence
     * is going to be sent by this MultiUserChat to the server. Stanza(/Packet) interceptors may
     * add new extensions to the presence that is going to be sent to the MUC service.
     *
     * @param presenceInterceptor the new stanza(/packet) interceptor that will intercept presence packets.
     */
    public void addPresenceInterceptor(PresenceListener presenceInterceptor) {
        presenceInterceptors.add(presenceInterceptor);
    }

    /**
     * Removes a {@link StanzaListener} that was being invoked every time a new presence
     * was being sent by this MultiUserChat to the server. Stanza(/Packet) interceptors may
     * add new extensions to the presence that is going to be sent to the MUC service.
     *
     * @param presenceInterceptor the stanza(/packet) interceptor to remove.
     */
    public void removePresenceInterceptor(StanzaListener presenceInterceptor) {
        presenceInterceptors.remove(presenceInterceptor);
    }

    /**
     * Returns the last known room's subject or <tt>null</tt> if the user hasn't joined the room
     * or the room does not have a subject yet. In case the room has a subject, as soon as the
     * user joins the room a message with the current room's subject will be received.<p>
     *
     * To be notified every time the room's subject change you should add a listener
     * to this room. {@link #addSubjectUpdatedListener(SubjectUpdatedListener)}<p>
     *
     * To change the room's subject use {@link #changeSubject(String)}.
     *
     * @return the room's subject or <tt>null</tt> if the user hasn't joined the room or the
     * room does not have a subject yet.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the reserved room nickname for the user in the room. A user may have a reserved
     * nickname, for example through explicit room registration or database integration. In such
     * cases it may be desirable for the user to discover the reserved nickname before attempting
     * to enter the room.
     *
     * @return the reserved room nickname or <tt>null</tt> if none.
     * @throws SmackException if there was no response from the server.
     * @throws InterruptedException 
     */
    public String getReservedNickname() throws SmackException, InterruptedException {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(
                    room,
                    "x-roomuser-item");
            // Look for an Identity that holds the reserved nickname and return its name
            for (DiscoverInfo.Identity identity : result.getIdentities()) {
                return identity.getName();
            }
        }
        catch (XMPPException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving room nickname", e);
        }
        // If no Identity was found then the user does not have a reserved room nickname
        return null;
    }

    /**
     * Returns the nickname that was used to join the room, or <tt>null</tt> if not
     * currently joined.
     *
     * @return the nickname currently being used.
     */
    public Resourcepart getNickname() {
        return nickname;
    }

    /**
     * Changes the occupant's nickname to a new nickname within the room. Each room occupant
     * will receive two presence packets. One of type "unavailable" for the old nickname and one
     * indicating availability for the new nickname. The unavailable presence will contain the new
     * nickname and an appropriate status code (namely 303) as extended presence information. The
     * status code 303 indicates that the occupant is changing his/her nickname.
     *
     * @param nickname the new nickname within the room.
     * @throws XMPPErrorException if the new nickname is already in use by another occupant.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws MucNotJoinedException 
     */
    public synchronized void changeNickname(Resourcepart nickname) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, MucNotJoinedException  {
        StringUtils.requireNotNullOrEmpty(nickname, "Nickname must not be null or blank.");
        // Check that we already have joined the room before attempting to change the
        // nickname.
        if (!joined) {
            throw new MucNotJoinedException(this);
        }
        final EntityFullJid jid = JidCreate.fullFrom(room, nickname);
        // We change the nickname by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        // We don't have to signal the MUC support again
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(jid);

        // Wait for a presence packet back from the server.
        StanzaFilter responseFilter =
            new AndFilter(
                FromMatchesFilter.createFull(jid),
                new StanzaTypeFilter(Presence.class));
        StanzaCollector response = connection.createStanzaCollectorAndSend(responseFilter, joinPresence);
        // Wait up to a certain number of seconds for a reply. If there is a negative reply, an
        // exception will be thrown
        response.nextResultOrThrow();

        this.nickname = nickname;
    }

    /**
     * Changes the occupant's availability status within the room. The presence type
     * will remain available but with a new status that describes the presence update and
     * a new presence mode (e.g. Extended away).
     *
     * @param status a text message describing the presence update.
     * @param mode the mode type for the presence update.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws MucNotJoinedException 
     */
    public void changeAvailabilityStatus(String status, Presence.Mode mode) throws NotConnectedException, InterruptedException, MucNotJoinedException {
        StringUtils.requireNotNullOrEmpty(nickname, "Nickname must not be null or blank.");
        // Check that we already have joined the room before attempting to change the
        // availability status.
        if (!joined) {
            throw new MucNotJoinedException(this);
        }
        // We change the availability status by sending a presence packet to the room with the
        // new presence status and mode
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setStatus(status);
        joinPresence.setMode(mode);
        joinPresence.setTo(JidCreate.fullFrom(room, nickname));

        // Send join packet.
        connection.sendStanza(joinPresence);
    }

    /**
     * Kicks a visitor or participant from the room. The kicked occupant will receive a presence
     * of type "unavailable" including a status code 307 and optionally along with the reason
     * (if provided) and the bare JID of the user who initiated the kick. After the occupant
     * was kicked from the room, the rest of the occupants will receive a presence of type
     * "unavailable". The presence will include a status code 307 which means that the occupant
     * was kicked from the room.
     *
     * @param nickname the nickname of the participant or visitor to kick from the room
     * (e.g. "john").
     * @param reason the reason why the participant or visitor is being kicked from the room.
     * @throws XMPPErrorException if an error occurs kicking the occupant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was intended to be kicked (i.e. Not Allowed error); or a
     *      403 error can occur if the occupant that intended to kick another occupant does
     *      not have kicking privileges (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void kickParticipant(Resourcepart nickname, String reason) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nickname, MUCRole.none, reason);
    }

    /**
     * Sends a voice request to the MUC. The room moderators usually need to approve this request.
     *
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#requestvoice">XEP-45 § 7.13 Requesting
     *      Voice</a>
     * @since 4.1
     */
    public void requestVoice() throws NotConnectedException, InterruptedException {
        DataForm form = new DataForm(DataForm.Type.submit);
        FormField formTypeField = new FormField(FormField.FORM_TYPE);
        formTypeField.addValue(MUCInitialPresence.NAMESPACE + "#request");
        form.addField(formTypeField);
        FormField requestVoiceField = new FormField("muc#role");
        requestVoiceField.setType(FormField.Type.text_single);
        requestVoiceField.setLabel("Requested role");
        requestVoiceField.addValue("participant");
        form.addField(requestVoiceField);
        Message message = new Message(room);
        message.addExtension(form);
        connection.sendStanza(message);
    }

    /**
     * Grants voice to visitors in the room. In a moderated room, a moderator may want to manage
     * who does and does not have "voice" in the room. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nicknames the nicknames of the visitors to grant voice in the room (e.g. "john").
     * @throws XMPPErrorException if an error occurs granting voice to a visitor. In particular, a
     *      403 error can occur if the occupant that intended to grant voice is not
     *      a moderator in this room (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantVoice(Collection<Resourcepart> nicknames) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nicknames, MUCRole.participant);
    }

    /**
     * Grants voice to a visitor in the room. In a moderated room, a moderator may want to manage
     * who does and does not have "voice" in the room. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nickname the nickname of the visitor to grant voice in the room (e.g. "john").
     * @throws XMPPErrorException if an error occurs granting voice to a visitor. In particular, a
     *      403 error can occur if the occupant that intended to grant voice is not
     *      a moderator in this room (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantVoice(Resourcepart nickname) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nickname, MUCRole.participant, null);
    }

    /**
     * Revokes voice from participants in the room. In a moderated room, a moderator may want to
     * revoke an occupant's privileges to speak. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nicknames the nicknames of the participants to revoke voice (e.g. "john").
     * @throws XMPPErrorException if an error occurs revoking voice from a participant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to revoke his voice (i.e. Not Allowed error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeVoice(Collection<Resourcepart> nicknames) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nicknames, MUCRole.visitor);
    }

    /**
     * Revokes voice from a participant in the room. In a moderated room, a moderator may want to
     * revoke an occupant's privileges to speak. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nickname the nickname of the participant to revoke voice (e.g. "john").
     * @throws XMPPErrorException if an error occurs revoking voice from a participant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to revoke his voice (i.e. Not Allowed error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeVoice(Resourcepart nickname) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nickname, MUCRole.visitor, null);
    }

    /**
     * Bans users from the room. An admin or owner of the room can ban users from a room. This
     * means that the banned user will no longer be able to join the room unless the ban has been
     * removed. If the banned user was present in the room then he/she will be removed from the
     * room and notified that he/she was banned along with the reason (if provided) and the bare
     * XMPP user ID of the user who initiated the ban.
     *
     * @param jids the bare XMPP user IDs of the users to ban.
     * @throws XMPPErrorException if an error occurs banning a user. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to be banned (i.e. Not Allowed error).
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void banUsers(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.outcast);
    }

    /**
     * Bans a user from the room. An admin or owner of the room can ban users from a room. This
     * means that the banned user will no longer be able to join the room unless the ban has been
     * removed. If the banned user was present in the room then he/she will be removed from the
     * room and notified that he/she was banned along with the reason (if provided) and the bare
     * XMPP user ID of the user who initiated the ban.
     *
     * @param jid the bare XMPP user ID of the user to ban (e.g. "user@host.org").
     * @param reason the optional reason why the user was banned.
     * @throws XMPPErrorException if an error occurs banning a user. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to be banned (i.e. Not Allowed error).
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void banUser(Jid jid, String reason) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.outcast, reason);
    }

    /**
     * Grants membership to other users. Only administrators are able to grant membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list).
     *
     * @param jids the XMPP user IDs of the users to grant membership.
     * @throws XMPPErrorException if an error occurs granting membership to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantMembership(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.member);
    }

    /**
     * Grants membership to a user. Only administrators are able to grant membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list).
     *
     * @param jid the XMPP user ID of the user to grant membership (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs granting membership to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantMembership(Jid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.member, null);
    }

    /**
     * Revokes users' membership. Only administrators are able to revoke membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list). If the user is in the room and
     * the room is of type members-only then the user will be removed from the room.
     *
     * @param jids the bare XMPP user IDs of the users to revoke membership.
     * @throws XMPPErrorException if an error occurs revoking membership to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeMembership(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.none);
    }

    /**
     * Revokes a user's membership. Only administrators are able to revoke membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list). If the user is in the room and
     * the room is of type members-only then the user will be removed from the room.
     *
     * @param jid the bare XMPP user ID of the user to revoke membership (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs revoking membership to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeMembership(Jid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.none, null);
    }

    /**
     * Grants moderator privileges to participants or visitors. Room administrators may grant
     * moderator privileges. A moderator is allowed to kick users, grant and revoke voice, invite
     * other users, modify room's subject plus all the partcipants privileges.
     *
     * @param nicknames the nicknames of the occupants to grant moderator privileges.
     * @throws XMPPErrorException if an error occurs granting moderator privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantModerator(Collection<Resourcepart> nicknames) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nicknames, MUCRole.moderator);
    }

    /**
     * Grants moderator privileges to a participant or visitor. Room administrators may grant
     * moderator privileges. A moderator is allowed to kick users, grant and revoke voice, invite
     * other users, modify room's subject plus all the partcipants privileges.
     *
     * @param nickname the nickname of the occupant to grant moderator privileges.
     * @throws XMPPErrorException if an error occurs granting moderator privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantModerator(Resourcepart nickname) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nickname, MUCRole.moderator, null);
    }

    /**
     * Revokes moderator privileges from other users. The occupant that loses moderator
     * privileges will become a participant. Room administrators may revoke moderator privileges
     * only to occupants whose affiliation is member or none. This means that an administrator is
     * not allowed to revoke moderator privileges from other room administrators or owners.
     *
     * @param nicknames the nicknames of the occupants to revoke moderator privileges.
     * @throws XMPPErrorException if an error occurs revoking moderator privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeModerator(Collection<Resourcepart> nicknames) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nicknames, MUCRole.participant);
    }

    /**
     * Revokes moderator privileges from another user. The occupant that loses moderator
     * privileges will become a participant. Room administrators may revoke moderator privileges
     * only to occupants whose affiliation is member or none. This means that an administrator is
     * not allowed to revoke moderator privileges from other room administrators or owners.
     *
     * @param nickname the nickname of the occupant to revoke moderator privileges.
     * @throws XMPPErrorException if an error occurs revoking moderator privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeModerator(Resourcepart nickname) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeRole(nickname, MUCRole.participant, null);
    }

    /**
     * Grants ownership privileges to other users. Room owners may grant ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     * An owner is allowed to change defining room features as well as perform all administrative
     * functions.
     *
     * @param jids the collection of bare XMPP user IDs of the users to grant ownership.
     * @throws XMPPErrorException if an error occurs granting ownership privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantOwnership(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.owner);
    }

    /**
     * Grants ownership privileges to another user. Room owners may grant ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     * An owner is allowed to change defining room features as well as perform all administrative
     * functions.
     *
     * @param jid the bare XMPP user ID of the user to grant ownership (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs granting ownership privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantOwnership(Jid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.owner, null);
    }

    /**
     * Revokes ownership privileges from other users. The occupant that loses ownership
     * privileges will become an administrator. Room owners may revoke ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     *
     * @param jids the bare XMPP user IDs of the users to revoke ownership.
     * @throws XMPPErrorException if an error occurs revoking ownership privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeOwnership(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.admin);
    }

    /**
     * Revokes ownership privileges from another user. The occupant that loses ownership
     * privileges will become an administrator. Room owners may revoke ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     *
     * @param jid the bare XMPP user ID of the user to revoke ownership (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs revoking ownership privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeOwnership(Jid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.admin, null);
    }

    /**
     * Grants administrator privileges to other users. Room owners may grant administrator
     * privileges to a member or unaffiliated user. An administrator is allowed to perform
     * administrative functions such as banning users and edit moderator list.
     *
     * @param jids the bare XMPP user IDs of the users to grant administrator privileges.
     * @throws XMPPErrorException if an error occurs granting administrator privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantAdmin(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.admin);
    }

    /**
     * Grants administrator privileges to another user. Room owners may grant administrator
     * privileges to a member or unaffiliated user. An administrator is allowed to perform
     * administrative functions such as banning users and edit moderator list.
     *
     * @param jid the bare XMPP user ID of the user to grant administrator privileges
     * (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs granting administrator privileges to a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void grantAdmin(Jid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.admin);
    }

    /**
     * Revokes administrator privileges from users. The occupant that loses administrator
     * privileges will become a member. Room owners may revoke administrator privileges from
     * a member or unaffiliated user.
     *
     * @param jids the bare XMPP user IDs of the user to revoke administrator privileges.
     * @throws XMPPErrorException if an error occurs revoking administrator privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeAdmin(Collection<? extends Jid> jids) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jids, MUCAffiliation.admin);
    }

    /**
     * Revokes administrator privileges from a user. The occupant that loses administrator
     * privileges will become a member. Room owners may revoke administrator privileges from
     * a member or unaffiliated user.
     *
     * @param jid the bare XMPP user ID of the user to revoke administrator privileges
     * (e.g. "user@host.org").
     * @throws XMPPErrorException if an error occurs revoking administrator privileges from a user.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void revokeAdmin(EntityJid jid) throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, MUCAffiliation.member);
    }

    /**
     * Tries to change the affiliation with an 'muc#admin' namespace
     *
     * @param jid
     * @param affiliation
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    private void changeAffiliationByAdmin(Jid jid, MUCAffiliation affiliation)
                    throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        changeAffiliationByAdmin(jid, affiliation, null);
    }

    /**
     * Tries to change the affiliation with an 'muc#admin' namespace
     *
     * @param jid
     * @param affiliation
     * @param reason the reason for the affiliation change (optional)
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    private void changeAffiliationByAdmin(Jid jid, MUCAffiliation affiliation, String reason) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
            {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.set);
        // Set the new affiliation.
        MUCItem item = new MUCItem(affiliation, jid, reason);
        iq.addItem(item);

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
    }

    private void changeAffiliationByAdmin(Collection<? extends Jid> jids, MUCAffiliation affiliation)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.set);
        for (Jid jid : jids) {
            // Set the new affiliation.
            MUCItem item = new MUCItem(affiliation, jid);
            iq.addItem(item);
        }

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
    }

    private void changeRole(Resourcepart nickname, MUCRole role, String reason) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.set);
        // Set the new role.
        MUCItem item = new MUCItem(role, nickname, reason);
        iq.addItem(item);

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
    }

    private void changeRole(Collection<Resourcepart> nicknames, MUCRole role) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.set);
        for (Resourcepart nickname : nicknames) {
            // Set the new role.
            MUCItem item = new MUCItem(role, nickname);
            iq.addItem(item);
        }

        connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
    }

    /**
     * Returns the number of occupants in the group chat.<p>
     *
     * Note: this value will only be accurate after joining the group chat, and
     * may fluctuate over time. If you query this value directly after joining the
     * group chat it may not be accurate, as it takes a certain amount of time for
     * the server to send all presence packets to this client.
     *
     * @return the number of occupants in the group chat.
     */
    public int getOccupantsCount() {
        return occupantsMap.size();
    }

    /**
     * Returns an List  for the list of fully qualified occupants
     * in the group chat. For example, "conference@chat.jivesoftware.com/SomeUser".
     * Typically, a client would only display the nickname of the occupant. To
     * get the nickname from the fully qualified name, use the
     * {@link org.jxmpp.util.XmppStringUtils#parseResource(String)} method.
     * Note: this value will only be accurate after joining the group chat, and may
     * fluctuate over time.
     *
     * @return a List of the occupants in the group chat.
     */
    public List<EntityFullJid> getOccupants() {
        return new ArrayList<>(occupantsMap.keySet());
    }

    /**
     * Returns the presence info for a particular user, or <tt>null</tt> if the user
     * is not in the room.<p>
     *
     * @param user the room occupant to search for his presence. The format of user must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the occupant's current presence, or <tt>null</tt> if the user is unavailable
     *      or if no presence information is available.
     */
    public Presence getOccupantPresence(EntityFullJid user) {
        return occupantsMap.get(user);
    }

    /**
     * Returns the Occupant information for a particular occupant, or <tt>null</tt> if the
     * user is not in the room. The Occupant object may include information such as full
     * JID of the user as well as the role and affiliation of the user in the room.<p>
     *
     * @param user the room occupant to search for his presence. The format of user must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the Occupant or <tt>null</tt> if the user is unavailable (i.e. not in the room).
     */
    public Occupant getOccupant(EntityFullJid user) {
        Presence presence = getOccupantPresence(user);
        if (presence != null) {
            return new Occupant(presence);
        }
        return null;
    }

    /**
     * Adds a stanza(/packet) listener that will be notified of any new Presence packets
     * sent to the group chat. Using a listener is a suitable way to know when the list
     * of occupants should be re-loaded due to any changes.
     *
     * @param listener a stanza(/packet) listener that will be notified of any presence packets
     *      sent to the group chat.
     * @return true if the listener was not already added.
     */
    public boolean addParticipantListener(PresenceListener listener) {
        return presenceListeners.add(listener);
    }

    /**
     * Removes a stanza(/packet) listener that was being notified of any new Presence packets
     * sent to the group chat.
     *
     * @param listener a stanza(/packet) listener that was being notified of any presence packets
     *      sent to the group chat.
     * @return true if the listener was removed, otherwise the listener was not added previously.
     */
    public boolean removeParticipantListener(PresenceListener listener) {
        return presenceListeners.remove(listener);
    }

    /**
     * Returns a list of <code>Affiliate</code> with the room owners.
     *
     * @return a list of <code>Affiliate</code> with the room owners.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Affiliate> getOwners() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getAffiliatesByAdmin(MUCAffiliation.owner);
    }

    /**
     * Returns a list of <code>Affiliate</code> with the room administrators.
     *
     * @return a list of <code>Affiliate</code> with the room administrators.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Affiliate> getAdmins() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getAffiliatesByAdmin(MUCAffiliation.admin);
    }

    /**
     * Returns a list of <code>Affiliate</code> with the room members.
     *
     * @return a list of <code>Affiliate</code> with the room members.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Affiliate> getMembers() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        return getAffiliatesByAdmin(MUCAffiliation.member);
    }

    /**
     * Returns a list of <code>Affiliate</code> with the room outcasts.
     *
     * @return a list of <code>Affiliate</code> with the room outcasts.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Affiliate> getOutcasts() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getAffiliatesByAdmin(MUCAffiliation.outcast);
    }

    /**
     * Returns a collection of <code>Affiliate</code> that have the specified room affiliation
     * sending a request in the admin namespace.
     *
     * @param affiliation the affiliation of the users in the room.
     * @return a collection of <code>Affiliate</code> that have the specified room affiliation.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    private List<Affiliate> getAffiliatesByAdmin(MUCAffiliation affiliation) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.get);
        // Set the specified affiliation. This may request the list of owners/admins/members/outcasts.
        MUCItem item = new MUCItem(affiliation);
        iq.addItem(item);

        MUCAdmin answer = (MUCAdmin) connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();

        // Get the list of affiliates from the server's answer
        List<Affiliate> affiliates = new ArrayList<Affiliate>();
        for (MUCItem mucadminItem : answer.getItems()) {
            affiliates.add(new Affiliate(mucadminItem));
        }
        return affiliates;
    }

    /**
     * Returns a list of <code>Occupant</code> with the room moderators.
     *
     * @return a list of <code>Occupant</code> with the room moderators.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Occupant> getModerators() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getOccupants(MUCRole.moderator);
    }

    /**
     * Returns a list of <code>Occupant</code> with the room participants.
     * 
     * @return a list of <code>Occupant</code> with the room participants.
     * @throws XMPPErrorException if you don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Occupant> getParticipants() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getOccupants(MUCRole.participant);
    }

    /**
     * Returns a list of <code>Occupant</code> that have the specified room role.
     *
     * @param role the role of the occupant in the room.
     * @return a list of <code>Occupant</code> that have the specified room role.
     * @throws XMPPErrorException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    private List<Occupant> getOccupants(MUCRole role) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.get);
        // Set the specified role. This may request the list of moderators/participants.
        MUCItem item = new MUCItem(role);
        iq.addItem(item);

        MUCAdmin answer = (MUCAdmin) connection.createStanzaCollectorAndSend(iq).nextResultOrThrow();
        // Get the list of participants from the server's answer
        List<Occupant> participants = new ArrayList<Occupant>();
        for (MUCItem mucadminItem : answer.getItems()) {
            participants.add(new Occupant(mucadminItem));
        }
        return participants;
    }

    /**
     * Sends a message to the chat room.
     *
     * @param text the text of the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendMessage(String text) throws NotConnectedException, InterruptedException {
        Message message = createMessage();
        message.setBody(text);
        connection.sendStanza(message);
    }

    /**
     * Returns a new Chat for sending private messages to a given room occupant.
     * The Chat's occupant address is the room's JID (i.e. roomName@service/nick). The server
     * service will change the 'from' address to the sender's room JID and delivering the message
     * to the intended recipient's full JID.
     *
     * @param occupant occupant unique room JID (e.g. 'darkcave@macbeth.shakespeare.lit/Paul').
     * @param listener the listener is a message listener that will handle messages for the newly
     * created chat.
     * @return new Chat for sending private messages to a given room occupant.
     */
    // TODO This should be made new not using chat.Chat. Private MUC chats are different from XMPP-IM 1:1 chats in to many ways.
    // API sketch: PrivateMucChat createPrivateChat(Resourcepart nick)
    @SuppressWarnings("deprecation")
    public org.jivesoftware.smack.chat.Chat createPrivateChat(EntityFullJid occupant, ChatMessageListener listener) {
        return org.jivesoftware.smack.chat.ChatManager.getInstanceFor(connection).createChat(occupant, listener);
    }

    /**
     * Creates a new Message to send to the chat room.
     *
     * @return a new Message addressed to the chat room.
     */
    public Message createMessage() {
        return new Message(room, Message.Type.groupchat);
    }

    /**
     * Sends a Message to the chat room.
     *
     * @param message the message.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendMessage(Message message) throws NotConnectedException, InterruptedException {
        message.setTo(room);
        message.setType(Message.Type.groupchat);
        connection.sendStanza(message);
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
     * @throws MucNotJoinedException 
    */
    public Message pollMessage() throws MucNotJoinedException {
        if (messageCollector == null) {
            throw new MucNotJoinedException(this);
        }
        return messageCollector.pollResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a message is available.
     *
     * @return the next message.
     * @throws MucNotJoinedException 
     * @throws InterruptedException 
     */
    public Message nextMessage() throws MucNotJoinedException, InterruptedException {
        if (messageCollector == null) {
            throw new MucNotJoinedException(this);
        }
        return  messageCollector.nextResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a stanza(/packet) is available or the <tt>timeout</tt> has elapased.
     * If the timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the maximum amount of time to wait for the next message.
     * @return the next message, or <tt>null</tt> if the timeout elapses without a
     *      message becoming available.
     * @throws MucNotJoinedException 
     * @throws InterruptedException 
     */
    public Message nextMessage(long timeout) throws MucNotJoinedException, InterruptedException {
        if (messageCollector == null) {
            throw new MucNotJoinedException(this);
        }
        return messageCollector.nextResult(timeout);
    }

    /**
     * Adds a stanza(/packet) listener that will be notified of any new messages in the
     * group chat. Only "group chat" messages addressed to this group chat will
     * be delivered to the listener. If you wish to listen for other packets
     * that may be associated with this group chat, you should register a
     * PacketListener directly with the XMPPConnection with the appropriate
     * PacketListener.
     *
     * @param listener a stanza(/packet) listener.
     * @return true if the listener was not already added.
     */
    public boolean addMessageListener(MessageListener listener) {
        return messageListeners.add(listener);
    }

    /**
     * Removes a stanza(/packet) listener that was being notified of any new messages in the
     * multi user chat. Only "group chat" messages addressed to this multi user chat were
     * being delivered to the listener.
     *
     * @param listener a stanza(/packet) listener.
     * @return true if the listener was removed, otherwise the listener was not added previously.
     */
    public boolean removeMessageListener(MessageListener listener) {
        return messageListeners.remove(listener);
    }

    /**
     * Changes the subject within the room. As a default, only users with a role of "moderator"
     * are allowed to change the subject in a room. Although some rooms may be configured to
     * allow a mere participant or even a visitor to change the subject.
     *
     * @param subject the new room's subject to set.
     * @throws XMPPErrorException if someone without appropriate privileges attempts to change the
     *          room subject will throw an error with code 403 (i.e. Forbidden)
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void changeSubject(final String subject) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Message message = createMessage();
        message.setSubject(subject);
        // Wait for an error or confirmation message back from the server.
        StanzaFilter responseFilter = new AndFilter(fromRoomGroupchatFilter, new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
                Message msg = (Message) packet;
                return subject.equals(msg.getSubject());
            }
        });
        StanzaCollector response = connection.createStanzaCollectorAndSend(responseFilter, message);
        // Wait up to a certain number of seconds for a reply.
        response.nextResultOrThrow();
    }

    /**
     * Remove the connection callbacks (PacketListener, PacketInterceptor, StanzaCollector) used by this MUC from the
     * connection.
     */
    private void removeConnectionCallbacks() {
        connection.removeSyncStanzaListener(messageListener);
        connection.removeSyncStanzaListener(presenceListener);
        connection.removeSyncStanzaListener(declinesListener);
        connection.removePacketInterceptor(presenceInterceptor);
        if (messageCollector != null) {
            messageCollector.cancel();
            messageCollector = null;
        }
    }

    /**
     * Remove all callbacks and resources necessary when the user has left the room for some reason.
     */
    private synchronized void userHasLeft() {
        // Update the list of joined rooms
        multiUserChatManager.removeJoinedRoom(room);
        removeConnectionCallbacks();
    }

    /**
     * Adds a listener that will be notified of changes in your status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a user status listener.
     * @return true if the user status listener was not already added.
     */
    public boolean addUserStatusListener(UserStatusListener listener) {
        return userStatusListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in your status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a user status listener.
     * @return true if the listener was registered and is now removed.
     */
    public boolean removeUserStatusListener(UserStatusListener listener) {
        return userStatusListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes in occupants status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     * @return true if the listener was not already added.
     */
    public boolean addParticipantStatusListener(ParticipantStatusListener listener) {
        return participantStatusListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in occupants status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     * @return true if the listener was registered and is now removed.
     */
    public boolean removeParticipantStatusListener(ParticipantStatusListener listener) {
        return participantStatusListeners.remove(listener);
    }

    /**
     * Fires notification events if the role of a room occupant has changed. If the occupant that
     * changed his role is your occupant then the <code>UserStatusListeners</code> added to this
     * <code>MultiUserChat</code> will be fired. On the other hand, if the occupant that changed
     * his role is not yours then the <code>ParticipantStatusListeners</code> added to this
     * <code>MultiUserChat</code> will be fired. The following table shows the events that will
     * be fired depending on the previous and new role of the occupant.
     *
     * <pre>
     * <table border="1">
     * <tr><td><b>Old</b></td><td><b>New</b></td><td><b>Events</b></td></tr>
     *
     * <tr><td>None</td><td>Visitor</td><td>--</td></tr>
     * <tr><td>Visitor</td><td>Participant</td><td>voiceGranted</td></tr>
     * <tr><td>Participant</td><td>Moderator</td><td>moderatorGranted</td></tr>
     *
     * <tr><td>None</td><td>Participant</td><td>voiceGranted</td></tr>
     * <tr><td>None</td><td>Moderator</td><td>voiceGranted + moderatorGranted</td></tr>
     * <tr><td>Visitor</td><td>Moderator</td><td>voiceGranted + moderatorGranted</td></tr>
     *
     * <tr><td>Moderator</td><td>Participant</td><td>moderatorRevoked</td></tr>
     * <tr><td>Participant</td><td>Visitor</td><td>voiceRevoked</td></tr>
     * <tr><td>Visitor</td><td>None</td><td>kicked</td></tr>
     *
     * <tr><td>Moderator</td><td>Visitor</td><td>voiceRevoked + moderatorRevoked</td></tr>
     * <tr><td>Moderator</td><td>None</td><td>kicked</td></tr>
     * <tr><td>Participant</td><td>None</td><td>kicked</td></tr>
     * </table>
     * </pre>
     *
     * @param oldRole the previous role of the user in the room before receiving the new presence
     * @param newRole the new role of the user in the room after receiving the new presence
     * @param isUserModification whether the received presence is about your user in the room or not
     * @param from the occupant whose role in the room has changed
     * (e.g. room@conference.jabber.org/nick).
     */
    private void checkRoleModifications(
        MUCRole oldRole,
        MUCRole newRole,
        boolean isUserModification,
        EntityFullJid from) {
        // Voice was granted to a visitor
        if ((MUCRole.visitor.equals(oldRole) || MUCRole.none.equals(oldRole))
            && MUCRole.participant.equals(newRole)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.voiceGranted();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.voiceGranted(from);
                }
            }
        }
        // The participant's voice was revoked from the room
        else if (
            MUCRole.participant.equals(oldRole)
                && (MUCRole.visitor.equals(newRole) || MUCRole.none.equals(newRole))) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.voiceRevoked();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.voiceRevoked(from);
                }
            }
        }
        // Moderator privileges were granted to a participant
        if (!MUCRole.moderator.equals(oldRole) && MUCRole.moderator.equals(newRole)) {
            if (MUCRole.visitor.equals(oldRole) || MUCRole.none.equals(oldRole)) {
                if (isUserModification) {
                    for (UserStatusListener listener : userStatusListeners) {
                        listener.voiceGranted();
                    }
                }
                else {
                    for (ParticipantStatusListener listener : participantStatusListeners) {
                        listener.voiceGranted(from);
                    }
                }
            }
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.moderatorGranted();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.moderatorGranted(from);
                }
            }
        }
        // Moderator privileges were revoked from a participant
        else if (MUCRole.moderator.equals(oldRole) && !MUCRole.moderator.equals(newRole)) {
            if (MUCRole.visitor.equals(newRole) || MUCRole.none.equals(newRole)) {
                if (isUserModification) {
                    for (UserStatusListener listener : userStatusListeners) {
                        listener.voiceRevoked();
                    }
                }
                else {
                    for (ParticipantStatusListener listener : participantStatusListeners) {
                        listener.voiceRevoked(from);
                    }
                }
            }
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.moderatorRevoked();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.moderatorRevoked(from);
                }
            }
        }
    }

    /**
     * Fires notification events if the affiliation of a room occupant has changed. If the
     * occupant that changed his affiliation is your occupant then the
     * <code>UserStatusListeners</code> added to this <code>MultiUserChat</code> will be fired.
     * On the other hand, if the occupant that changed his affiliation is not yours then the
     * <code>ParticipantStatusListeners</code> added to this <code>MultiUserChat</code> will be
     * fired. The following table shows the events that will be fired depending on the previous
     * and new affiliation of the occupant.
     *
     * <pre>
     * <table border="1">
     * <tr><td><b>Old</b></td><td><b>New</b></td><td><b>Events</b></td></tr>
     *
     * <tr><td>None</td><td>Member</td><td>membershipGranted</td></tr>
     * <tr><td>Member</td><td>Admin</td><td>membershipRevoked + adminGranted</td></tr>
     * <tr><td>Admin</td><td>Owner</td><td>adminRevoked + ownershipGranted</td></tr>
     *
     * <tr><td>None</td><td>Admin</td><td>adminGranted</td></tr>
     * <tr><td>None</td><td>Owner</td><td>ownershipGranted</td></tr>
     * <tr><td>Member</td><td>Owner</td><td>membershipRevoked + ownershipGranted</td></tr>
     *
     * <tr><td>Owner</td><td>Admin</td><td>ownershipRevoked + adminGranted</td></tr>
     * <tr><td>Admin</td><td>Member</td><td>adminRevoked + membershipGranted</td></tr>
     * <tr><td>Member</td><td>None</td><td>membershipRevoked</td></tr>
     *
     * <tr><td>Owner</td><td>Member</td><td>ownershipRevoked + membershipGranted</td></tr>
     * <tr><td>Owner</td><td>None</td><td>ownershipRevoked</td></tr>
     * <tr><td>Admin</td><td>None</td><td>adminRevoked</td></tr>
     * <tr><td><i>Anyone</i></td><td>Outcast</td><td>banned</td></tr>
     * </table>
     * </pre>
     *
     * @param oldAffiliation the previous affiliation of the user in the room before receiving the
     * new presence
     * @param newAffiliation the new affiliation of the user in the room after receiving the new
     * presence
     * @param isUserModification whether the received presence is about your user in the room or not
     * @param from the occupant whose role in the room has changed
     * (e.g. room@conference.jabber.org/nick).
     */
    private void checkAffiliationModifications(
        MUCAffiliation oldAffiliation,
        MUCAffiliation newAffiliation,
        boolean isUserModification,
        EntityFullJid from) {
        // First check for revoked affiliation and then for granted affiliations. The idea is to
        // first fire the "revoke" events and then fire the "grant" events.

        // The user's ownership to the room was revoked
        if (MUCAffiliation.owner.equals(oldAffiliation) && !MUCAffiliation.owner.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.ownershipRevoked();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.ownershipRevoked(from);
                }
            }
        }
        // The user's administrative privileges to the room were revoked
        else if (MUCAffiliation.admin.equals(oldAffiliation) && !MUCAffiliation.admin.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.adminRevoked();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.adminRevoked(from);
                }
            }
        }
        // The user's membership to the room was revoked
        else if (MUCAffiliation.member.equals(oldAffiliation) && !MUCAffiliation.member.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.membershipRevoked();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.membershipRevoked(from);
                }
            }
        }

        // The user was granted ownership to the room
        if (!MUCAffiliation.owner.equals(oldAffiliation) && MUCAffiliation.owner.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.ownershipGranted();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.ownershipGranted(from);
                }
            }
        }
        // The user was granted administrative privileges to the room
        else if (!MUCAffiliation.admin.equals(oldAffiliation) && MUCAffiliation.admin.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.adminGranted();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.adminGranted(from);
                }
            }
        }
        // The user was granted membership to the room
        else if (!MUCAffiliation.member.equals(oldAffiliation) && MUCAffiliation.member.equals(newAffiliation)) {
            if (isUserModification) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.membershipGranted();
                }
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.membershipGranted(from);
                }
            }
        }
    }

    /**
     * Fires events according to the received presence code.
     *
     * @param statusCodes
     * @param isUserModification
     * @param mucUser
     * @param from
     */
    private void checkPresenceCode(
        Set<Status> statusCodes,
        boolean isUserModification,
        MUCUser mucUser,
        EntityFullJid from) {
        // Check if an occupant was kicked from the room
        if (statusCodes.contains(Status.KICKED_307)) {
            // Check if this occupant was kicked
            if (isUserModification) {
                joined = false;
                for (UserStatusListener listener : userStatusListeners) {
                    listener.kicked(mucUser.getItem().getActor(), mucUser.getItem().getReason());
                }

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.kicked(from, mucUser.getItem().getActor(), mucUser.getItem().getReason());
                }
            }
        }
        // A user was banned from the room
        if (statusCodes.contains(Status.BANNED_301)) {
            // Check if this occupant was banned
            if (isUserModification) {
                joined = false;
                for (UserStatusListener listener : userStatusListeners) {
                    listener.banned(mucUser.getItem().getActor(), mucUser.getItem().getReason());
                }

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
            else {
                for (ParticipantStatusListener listener : participantStatusListeners) {
                    listener.banned(from, mucUser.getItem().getActor(), mucUser.getItem().getReason());
                }
            }
        }
        // A user's membership was revoked from the room
        if (statusCodes.contains(Status.REMOVED_AFFIL_CHANGE_321)) {
            // Check if this occupant's membership was revoked
            if (isUserModification) {
                joined = false;
                for (UserStatusListener listener : userStatusListeners) {
                    listener.membershipRevoked();
                }

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
        }
        // A occupant has changed his nickname in the room
        if (statusCodes.contains(Status.NEW_NICKNAME_303)) {
            for (ParticipantStatusListener listener : participantStatusListeners) {
                listener.nicknameChanged(from, mucUser.getItem().getNick());
            }
        }
        //The room has been destroyed
        if (mucUser.getDestroy() != null) {
            MultiUserChat alternateMUC = multiUserChatManager.getMultiUserChat(mucUser.getDestroy().getJid());
            for (UserStatusListener listener : userStatusListeners) {
                listener.roomDestroyed(alternateMUC, mucUser.getDestroy().getReason());
            }

            // Reset occupant information.
            occupantsMap.clear();
            nickname = null;
            userHasLeft();
        }
    }

    @Override
    public String toString() {
        return "MUC: " + room + "(" + connection.getUser() + ")";
    }
}
