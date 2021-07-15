/**
 *
 * Copyright 2015-2020 Florian Schmaus, 2021 Dan Caseley
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.MultiResultSyncPoint;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sm.predicates.ForEveryMessage;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

public class MultiUserChatOccupantIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public MultiUserChatOccupantIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
                    SmackException.NotConnectedException, InterruptedException, TestNotPossibleException {
        super(environment);
    }

    /**
     * Asserts that when a user joins a room, all events are received, and in the correct order.
     *
     * <p>From XEP-0045 § 7.1:</p>
     * <blockquote>
     * The order of events involved in joining a room needs to be consistent so that clients can know which events to
     * expect when. After a client sends presence to join a room, the MUC service MUST send it events in the following
     * order:
     *   1. In-room presence from other occupants
     *   2. In-room presence from the joining entity itself (so-called "self-presence")
     *   3. Room history (if any)
     *   4. The room subject
     *   ...
     * </blockquote>
     *
     * <p>From XEP-0045 § 7.2.2</p>
     * <blockquote>
     * This self-presence MUST NOT be sent to the new occupant until the room has sent the presence of all other
     * occupants to the new occupant ... The service MUST first send the complete list of the existing occupants
     * to the new occupant and only then send the new occupant's own presence to the new occupant
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinEventOrderingTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-eventordering");
        final String mucSubject = "Subject smack-inttest-eventordering " + randomString;
        final String mucMessage = "Message smack-inttest-eventordering " + randomString;


        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        mucAsSeenByOne.changeSubject(mucSubject); // Blocks until confirmed.

        // Send and wait for the message to have been reflected, so that we can be sure it's part of the MUC history.
        final SimpleResultSyncPoint messageReflectionSyncPoint = new SimpleResultSyncPoint();
        mucAsSeenByOne.addMessageListener(message -> {
            if (message.getBody().equals(mucMessage)) {
                messageReflectionSyncPoint.signal();
            }
        });

        mucAsSeenByOne.sendMessage(mucMessage);
        messageReflectionSyncPoint.waitForResult(timeout);

        final ResultSyncPoint<String, Exception> subjectResultSyncPoint = new ResultSyncPoint<>();
        List<Object> results = new ArrayList<Object>();

        mucAsSeenByTwo.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String body = message.getBody();
                if (mucMessage.equals(body)) {
                    results.add(body);
                }
            }
        });

        mucAsSeenByTwo.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void joined(EntityFullJid participant) {
                results.add(participant);
            }
        });

        mucAsSeenByTwo.addSubjectUpdatedListener(new SubjectUpdatedListener() {
            @Override
            public void subjectUpdated(String subject, EntityFullJid from) {
                results.add(subject);
                subjectResultSyncPoint.signal(subject);
            }
        });

        try {
            Presence reflectedJoinPresence = mucAsSeenByTwo.join(nicknameTwo);
            results.add(reflectedJoinPresence.getFrom()); // Self-presence should be second

            subjectResultSyncPoint.waitForResult(timeout); // Wait for subject, as it should be 4th (last)

            assertEquals(4, results.size());
            assertEquals(JidCreate.fullFrom(mucAddress, nicknameOne), results.get(0));
            assertEquals(JidCreate.fullFrom(mucAddress, nicknameTwo), results.get(1));
            assertEquals(mucMessage, results.get(2));
            assertEquals(mucSubject, results.get(3));
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user sends a message to a room without joining, they receive an error and the message is not
     * sent to the occupants.
     *
     * <p>From XEP-0045 § 7.2.1:</p>
     * <blockquote>
     * In order to participate in the discussions held in a multi-user chat room, a user MUST first become an occupant
     * by entering the room
     * </blockquote>
     *
     * <p>From XEP-0045 § 7.4:</p>
     * <blockquote>
     * If the sender is not an occupant of the room, the service SHOULD return a &lt;not-acceptable/&gt; error to the
     * sender and SHOULD NOT reflect the message to all occupants
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucSendBeforeJoiningTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-send-without-joining");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        createMuc(mucAsSeenByOne, Resourcepart.from("one-" + randomString));

        ResultSyncPoint<Message, Exception> errorMessageResultSyncPoint = new ResultSyncPoint<>();
        conTwo.addStanzaListener(new StanzaListener() {
            @Override public void processStanza(Stanza packet)
                            throws SmackException.NotConnectedException, InterruptedException,
                            SmackException.NotLoggedInException {
                errorMessageResultSyncPoint.signal((Message) packet);
            }
        }, ForEveryMessage.INSTANCE);

        ResultSyncPoint<Message, Exception> distributedMessageResultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addMessageListener(new MessageListener() {
            @Override public void processMessage(Message message) {
                distributedMessageResultSyncPoint.signal(message);
            }
        });

        try {
            mucAsSeenByTwo.sendMessage("Message without Joining");
            Message response = errorMessageResultSyncPoint.waitForResult(timeout);
            assertEquals("not-acceptable", response.getError().getCondition().toString());
            assertThrows(TimeoutException.class, () -> distributedMessageResultSyncPoint.waitForResult(1000));
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user joins a room, they are sent presence information about existing participants and
     * themselves that includes role and affiliation information and appropriate status codes
     *
     * <p>From XEP-0045 § 7.2.2:</p>
     * <blockquote>
     * If the service is able to add the user to the room, it MUST send presence from all the existing
     * participants' occupant JIDs to the new occupant's full JID, including extended presence information about roles
     * in a single &lt;x/&gt; element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an
     * &lt;item/&gt; child with the 'role' attribute set to a value of "moderator", "participant", or "visitor", and with
     * the 'affiliation' attribute set to a value of "owner", "admin", "member", or "none" as appropriate.
     * </blockquote>
     *
     * <p>From XEP-0045 § 7.2.2:</p>
     * <blockquote>
     * the "self-presence" sent by the room to the new user MUST include a status code of 110 so that the user knows
     * this presence refers to itself as an occupant
     * </blockquote>
     *
     * <p>From XEP-0045 § 7.2.2:</p>
     * <blockquote>
     * The service MUST first send the complete list of the existing occupants to the new occupant and only then send
     * the new occupant's own presence to the new occupant.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinPresenceInformationTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-presenceinfo");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        mucAsSeenByTwo.join(nicknameTwo);

        SimpleResultSyncPoint oneSeesTwo = new SimpleResultSyncPoint();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                oneSeesTwo.signal();
            }
        });
        mucAsSeenByOne.grantModerator(nicknameTwo);
        oneSeesTwo.waitForResult(timeout);

        List<Presence> results = new ArrayList<Presence>();
        mucAsSeenByThree.addParticipantListener(new PresenceListener() {
            @Override public void processPresence(Presence presence) {
                results.add(presence);
            }
        });

        try {
            // Will block until all self-presence is received, prior to which all others presences will have been received.
            mucAsSeenByThree.join(nicknameThree);

            assertEquals(3, results.size()); // The 3rd will be self-presence.
            assertNotNull(MUCUser.from(results.get(0))); // Smack implementation guarantees the "x" element and muc#user namespace

            // The order of all but the last presence (which should be the self-presence) is unpredictable.
            MUCItem mucItemSelf = MUCUser.from(results.get(2)).getItem();
            Set<MUCItem> others = new HashSet<>();
            others.add(MUCUser.from(results.get(0)).getItem());
            others.add(MUCUser.from(results.get(1)).getItem());

            assertEquals(MUCAffiliation.none, mucItemSelf.getAffiliation());
            assertEquals(1, others.stream().filter(item -> MUCAffiliation.owner.equals(item.getAffiliation())).count());
            assertEquals(1, others.stream().filter(item -> MUCAffiliation.none.equals(item.getAffiliation())).count());

            assertEquals(MUCRole.participant, mucItemSelf.getRole());
            assertEquals(2, others.stream().filter(item -> MUCRole.moderator.equals(item.getRole())).count());

            assertTrue(MUCUser.from(results.get(2)).getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110));
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user joins a room, all users are sent presence information about the new participant.
     *
     *
     * <p>From XEP-0045 § 7.2.2:</p>
     * <blockquote>
     * the service MUST also send presence from the new participant's occupant JID to the full JIDs of all the
     * occupants (including the new occupant)
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinPresenceBroadcastTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-presenceinfo");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        mucAsSeenByTwo.join(nicknameTwo);

        final MultiResultSyncPoint<Presence, Exception> syncPoint = new MultiResultSyncPoint<>(2);

        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                syncPoint.signal(presence);
            }
        });

        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                syncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByThree.join(nicknameThree);

            Collection<Presence> results = syncPoint.waitForResults(timeout);
            assertTrue(results.stream().allMatch(result -> JidCreate.fullFrom(mucAddress, nicknameThree).equals(result.getFrom())));
            assertTrue(results.stream().anyMatch(result -> result.getTo().equals(conOne.getUser().asEntityFullJidIfPossible())));
            assertTrue(results.stream().anyMatch(result -> result.getTo().equals(conTwo.getUser().asEntityFullJidIfPossible())));
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user enters a non-anonymous room, the presence notifications contain extended presence
     * information.
     *
     * <p>From XEP-0045 § 7.2.3:</p>
     * <blockquote>
     * If the room is non-anonymous, the service MUST send the new occupant's full JID to all occupants using extended
     * presence information in an &lt;x/&gt; element qualified by the 'http://jabber.org/protocol/muc#user' namespace
     * and containing an &lt;item/&gt; child with a 'jid' attribute specifying the occupant's full JID.
     * </blockquote>
     *
     * <p>From XEP-0045 § 7.2.3:</p>
     * <blockquote>
     * If the user is entering a room that is non-anonymous (i.e., which informs all occupants of each occupant's full
     * JID as shown above), the service MUST warn the user by including a status code of "100" in the initial presence
     * that the room sends to the new occupant.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinNonAnonymousRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-joinnonanonymousroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createNonAnonymousMuc(mucAsSeenByOne, nicknameOne);

        final ResultSyncPoint<Presence, ?> participantOneSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSyncPoint.signal(presence);
            }
        });

        final ResultSyncPoint<Presence, ?> participantTwoSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByTwo.join(nicknameTwo);
            Presence presenceReceivedByOne = participantOneSyncPoint.waitForResult(timeout);
            Presence presenceReceivedByTwo = participantTwoSyncPoint.waitForResult(timeout);

            // Check the presence received by participant one for inclusion of full jid of participant two
            assertNotNull(presenceReceivedByOne);
            MUCUser announcedParticipantTwoUser = MUCUser.from(presenceReceivedByOne);
            assertNotNull(announcedParticipantTwoUser); // Smack implementation guarantees the "x" element and muc#user namespace
            assertNotNull(announcedParticipantTwoUser.getItem());
            assertEquals(conTwo.getUser().asEntityFullJidOrThrow(), announcedParticipantTwoUser.getItem().getJid());

            // Check the presence received by participant two for inclusion of status 100
            assertNotNull(presenceReceivedByTwo);
            assertTrue(MUCUser.from(presenceReceivedByTwo).getStatus().stream().anyMatch(status -> 100 == status.getCode()));

        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user enters a semi-anonymous room, the presence notifications contain extended presence
     * information only when sent to moderators.
     *
     * <p>From XEP-0045 § 7.2.4:</p>
     * <blockquote>
     * If the room is semi-anonymous, the service MUST send presence from the new occupant to all occupants as specified
     * above (i.e., unless the room is configured to not broadcast presence from new occupants below a certain
     * affiliation level as controlled by the "muc#roomconfig_presencebroadcast" room configuration option), but MUST
     * include the new occupant's full JID only in the presence notifications it sends to occupants with a role of
     * "moderator" and not to non-moderator occupants.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinSemiAnonymousRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-joinsemianonymousroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createSemiAnonymousMuc(mucAsSeenByOne, nicknameOne);

        mucAsSeenByTwo.join(nicknameTwo);

        // First pass: participant two is not a moderator yet
        final ResultSyncPoint<Presence, ?> participantTwoSyncPointFirstPass = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPointFirstPass.signal(presence);
            }
        });

        try {
            mucAsSeenByThree.join(nicknameThree);
            Presence presenceReceivedByTwo = participantTwoSyncPointFirstPass.waitForResult(timeout);

            // Check the presence received by participant two for exclusion of full jid of participant three
            assertNotNull(presenceReceivedByTwo);
            assertNull(MUCUser.from(presenceReceivedByTwo).getItem().getJid());
        } finally {
            mucAsSeenByThree.leave(); // Reset to have a second go
        }

        // Second pass: participant two is now a moderator
        mucAsSeenByOne.grantModerator(nicknameTwo);
        final ResultSyncPoint<Presence, ?> participantTwoSyncPointSecondPass = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPointSecondPass.signal(presence);
            }
        });

        try {
            mucAsSeenByThree.join(nicknameThree);
            Presence presenceReceivedByTwo = participantTwoSyncPointSecondPass.waitForResult(timeout);

            // Check the presence received by participant two for inclusion of full jid of participant three
            assertNotNull(presenceReceivedByTwo);
            MUCUser announcedParticipantThreeUser = MUCUser.from(presenceReceivedByTwo);
            assertNotNull(announcedParticipantThreeUser); // Smack implementation guarantees the "x" element and muc#user namespace
            assertNotNull(announcedParticipantThreeUser.getItem());
            assertEquals(conThree.getUser().asEntityFullJidOrThrow(), announcedParticipantThreeUser.getItem().getJid());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user can not enter a password-protected room without supplying a correct password.
     *
     * <p>From XEP-0045 § 7.2.5:</p>
     * <blockquote>
     * If the room requires a password and the user did not supply one (or the password provided is incorrect), the
     * service MUST deny access to the room and inform the user that they are unauthorized; this is done by returning a
     * presence stanza of type "error" specifying a &lt;not-authorized/&gt; error.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinPasswordProtectedRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-enterpasswordprotectedroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        final String correctPassword = StringUtils.insecureRandomString(8);

        createPasswordProtectedMuc(mucAsSeenByOne, nicknameOne, correctPassword);

        // Set up to receive presence responses on successful join
        final ResultSyncPoint<Presence, ?> participantTwoSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPoint.signal(presence);
            }
        });

        try {
            // First try: no password
            XMPPException.XMPPErrorException noPasswordErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo));
            assertNotNull(noPasswordErrorException);
            assertNotNull(noPasswordErrorException.getStanzaError());
            assertEquals("not-authorized", noPasswordErrorException.getStanzaError().getCondition().toString());

            // Second try: wrong password
            XMPPException.XMPPErrorException wrongPasswordErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo, correctPassword + "_"));
            assertNotNull(wrongPasswordErrorException);
            assertNotNull(wrongPasswordErrorException.getStanzaError());
            assertEquals("not-authorized", wrongPasswordErrorException.getStanzaError().getCondition().toString());

            // Third try: correct password
            mucAsSeenByTwo.join(nicknameTwo, correctPassword);
            Presence presenceCorrectPassword = participantTwoSyncPoint.waitForResult(timeout);
            assertNotNull(presenceCorrectPassword);
            assertNull(presenceCorrectPassword.getError());
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a members-only room while not being a member.
     *
     * This test does not cover § 9.3, aka the happy path.
     *
     * <p>From XEP-0045 § 7.2.6:</p>
     * <blockquote>
     * If the room is members-only but the user is not on the member list, the service MUST deny access to the room and
     * inform the user that they are not allowed to enter the room; this is done by returning a presence stanza of type
     * "error" specifying a &lt;registration-required/&gt; error condition.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinMembersOnlyRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-entermembersonlyroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMembersOnlyMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException registrationRequiredErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo));
            assertNotNull(registrationRequiredErrorException);
            assertNotNull(registrationRequiredErrorException.getStanzaError());
            assertEquals("registration-required", registrationRequiredErrorException.getStanzaError().getCondition().toString());
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a room while being banned.
     *
     * <p>From XEP-0045 § 7.2.7:</p>
     * <blockquote>
     * If the user has been banned from the room (i.e., has an affiliation of "outcast"), the service MUST deny access
     * to the room and inform the user of the fact that they are banned; this is done by returning a presence stanza of
     * type "error" specifying a &lt;forbidden/&gt; error condition.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucBannedUserJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-banneduser");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        mucAsSeenByOne.banUser(conTwo.getUser().asBareJid(), "Insufficient witchcraft");

        try {
            XMPPException.XMPPErrorException forbiddenErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo));
            assertNotNull(forbiddenErrorException);
            assertNotNull(forbiddenErrorException.getStanzaError());
            assertEquals("forbidden", forbiddenErrorException.getStanzaError().getCondition().toString());
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a room with the same nickname as an other user who is already present.
     *
     * <p>From XEP-0045 § 7.2.8:</p>
     * <blockquote>
     * If the room already contains another user with the nickname desired by the user seeking to enter the room (or if
     * the nickname is reserved by another user on the member list), the service MUST deny access to the room and inform
     * the user of the conflict; this is done by returning a presence stanza of type "error" specifying a
     * &lt;conflict/&gt; error condition.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucNicknameConflictJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-nicknameclash");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException conflictErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameOne));
            assertNotNull(conflictErrorException);
            assertNotNull(conflictErrorException.getStanzaError());
            assertEquals("conflict", conflictErrorException.getStanzaError().getCondition().toString());
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a room can not go past the configured maximum number of users, if a non-admin non-owner user
     * attempts to join.
     *
     * <p>From XEP-0045 § 7.2.9:</p>
     * <blockquote>
     * If the room has reached its maximum number of occupants, the service SHOULD deny access to the room and inform
     * the user of the restriction; this is done by returning a presence stanza of type "error" specifying a
     * &lt;service-unavailable/&gt; error condition.
     *
     * Alternatively, the room could kick an "idle user" in order to free up space (where the definition of "idle user"
     * is up to the implementation).
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest public void mucMaxUsersLimitJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-maxusersreached");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        setMaxUsers(mucAsSeenByOne, 2);

        // Set up for participant 1 to receive presence responses on join of participant 2
        final ResultSyncPoint<Presence, ?> participantOneSeesTwoSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesTwoSyncPoint.signal(presence);
            }
        });

        // Set up for participant 3 to receive presence responses on join of participant 3
        final ResultSyncPoint<Presence, ?> participantThreeSeesThreeSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByThree.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantThreeSeesThreeSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByTwo.join(nicknameTwo);
            participantOneSeesTwoSyncPoint.waitForResult(timeout);

            assertEquals(2, mucAsSeenByOne.getOccupantsCount());

            // Now user 3 may or may not be able to join the room. The service can either deny access to user three, or
            // it can kick user 2. Both strategies would comply with the specification. So the only thing we can
            // reasonably test here is whether the room doesn't have more occupants than its max size.

            try {
                mucAsSeenByThree.join(nicknameThree);
            } catch (XMPPException.XMPPErrorException unavailableErrorException) {
                // This exception is expected if the muc implementation employs the strategy to deny access beyond the
                // maxusers value
                if (unavailableErrorException.getStanzaError() == null || !"service-unavailable".equals(
                                unavailableErrorException.getStanzaError().getCondition().toString())) {
                    throw unavailableErrorException;
                }
            }

            // Now we should wait until participant one is informed about the (probably failed) new participant three
            // room join. But if joining failed, there will be no such update. All we can reasonably do is wait until
            // participant three has received its own presence response. This is not watertight though.
            participantThreeSeesThreeSyncPoint.waitForResult(timeout);

            // Irrespective of the way the implementation handles max users, there should still be only 2 users.
            assertEquals(2, mucAsSeenByOne.getOccupantsCount());

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that an admin can still join a room that has reached the configured maximum number of users.
     *
     * <p>From XEP-0045 § 7.2.9:</p>
     * <blockquote>
     * If the room has reached its maximum number of occupants and a room admin or owner attempts to join, the room MUST
     * allow the admin or owner to join, up to some reasonable number of additional occupants; this helps to prevent
     * denial of service attacks caused by stuffing the room with non-admin users.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest public void mucMaxUsersLimitAdminCanStillJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-maxusersreached-adminjoin");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        setMaxUsers(mucAsSeenByOne, 2);

        // Set up for participant 1 to receive presence responses on join of participant 2
        // Set up for participant 1 to receive presence responses on join of participant 3
        final ResultSyncPoint<Presence, ?> participantOneSeesTwoSyncPoint = new ResultSyncPoint<>();
        final ResultSyncPoint<Presence, ?> participantOneSeesThreeSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesTwoSyncPoint.signal(presence);
            } else if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesThreeSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByTwo.join(nicknameTwo);
            participantOneSeesTwoSyncPoint.waitForResult(timeout);

            assertEquals(2, mucAsSeenByOne.getOccupantsCount());

            mucAsSeenByOne.grantAdmin(conThree.getUser().asBareJid()); // blocking call
            mucAsSeenByThree.join(nicknameThree);
            participantOneSeesThreeSyncPoint.waitForResult(timeout);

            assertNotNull(mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)));
            assertEquals(3, mucAsSeenByOne.getOccupantsCount());

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that an owner can still join a room that has reached the configured maximum number of users.
     *
     * <p>From XEP-0045 § 7.2.9:</p>
     * <blockquote>
     * If the room has reached its maximum number of occupants and a room admin or owner attempts to join, the room MUST
     * allow the admin or owner to join, up to some reasonable number of additional occupants; this helps to prevent
     * denial of service attacks caused by stuffing the room with non-admin users.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest public void mucMaxUsersLimitOwnerCanStillJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-maxusersreached-ownerjoin");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        setMaxUsers(mucAsSeenByOne, 2);

        // Set up for participant 1 to receive presence responses on join of participant 2
        // Set up for participant 1 to receive presence responses on join of participant 3
        final ResultSyncPoint<Presence, ?> participantOneSeesTwoSyncPoint = new ResultSyncPoint<>();
        final ResultSyncPoint<Presence, ?> participantOneSeesThreeSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwo.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesTwoSyncPoint.signal(presence);
            } else if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesThreeSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByTwo.join(nicknameTwo);
            participantOneSeesTwoSyncPoint.waitForResult(timeout);

            assertEquals(2, mucAsSeenByOne.getOccupantsCount());

            mucAsSeenByOne.grantOwnership(conThree.getUser().asBareJid()); // blocking call
            mucAsSeenByThree.join(nicknameThree);
            participantOneSeesThreeSyncPoint.waitForResult(timeout);

            assertNotNull(mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)));
            assertEquals(3, mucAsSeenByOne.getOccupantsCount());

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a room can not be entered while it still being created (locked).
     *
     * <p>From XEP-0045 § 7.2.10:</p>
     * <blockquote>
     * If a user attempts to enter a room while it is "locked" (i.e., before the room creator provides an initial
     * configuration and therefore before the room officially exists), the service MUST refuse entry and return an
     * &lt;item-not-found/&gt; error to the user.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest public void mucJoinLockedRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-lockedroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createLockedMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException conflictErrorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo));
            assertNotNull(conflictErrorException);
            assertNotNull(conflictErrorException.getStanzaError());
            assertEquals("item-not-found", conflictErrorException.getStanzaError().getCondition().toString());

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user is warned when entering a room that allows public logging.
     *
     * <p>From XEP-0045 § 7.2.12:</p>
     * <blockquote>
     * If the user is entering a room in which the discussions are logged to a public archive (often accessible via
     * HTTP), the service SHOULD allow the user to enter the room but MUST also warn the user that the discussions are
     * logged. This is done by including a status code of "170" in the initial presence that the room sends to the new
     * occupant.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest public void mucJoinRoomWithPublicLoggingTest() throws Exception {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-publiclogging");

        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        setPublicLogging(mucAsSeenByOne, true);

        try {
            Presence twoPresence = mucAsSeenByTwo.join(nicknameTwo);
            assertTrue(MUCUser.from(twoPresence).getStatus().stream().anyMatch(status -> 170 == status.getCode()));
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that when a user leaves a room, they are themselves included on the list of users notified (self-presence).
     *
     * <p>From XEP-0045 § 7.14:</p>
     * <blockquote>
     * The service MUST then send a presence stanzas of type "unavailable" from the departing user's occupant JID to
     * the departing occupant's full JIDs, including a status code of "110" to indicate that this notification is
     * "self-presence"
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucLeaveTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-leave");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        try {
            muc.join(Resourcepart.from("nick-one"));

            Presence reflectedLeavePresence = muc.leave();

            MUCUser mucUser = MUCUser.from(reflectedLeavePresence);
            assertNotNull(mucUser);

            assertTrue(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110));
            assertEquals(mucAddress + "/nick-one", reflectedLeavePresence.getFrom().toString());
            assertEquals(conOne.getUser().asEntityFullJidIfPossible().toString(), reflectedLeavePresence.getTo().toString());
        } finally {
            muc.join(Resourcepart.from("nick-one")); // We need to be in the room to destroy the room
            tryDestroy(muc);
        }
    }
}
