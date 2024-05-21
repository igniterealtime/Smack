/**
 *
 * Copyright 2015-2024 Florian Schmaus, 2021 Dan Caseley
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.sm.predicates.ForEveryMessage;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucConfigurationNotSupportedException;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.igniterealtime.smack.inttest.Configuration;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.MultiResultSyncPoint;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

@SpecificationReference(document = "XEP-0045")
public class MultiUserChatOccupantIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public MultiUserChatOccupantIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
                    SmackException.NotConnectedException, InterruptedException, TestNotPossibleException {
        super(environment);
    }

    /**
     * Asserts that when a user joins a room, all events are received, and in the correct order.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.1 & 7.2.2", quote = "" +
        "§ 7.1 The order of events involved in joining a room needs to be consistent so that clients can know which events to expect when. After a client sends presence to join a room, the MUC service MUST send it events in the following order: 1. In-room presence from other occupants 2. In-room presence from the joining entity itself (so-called \"self-presence\") 3. Room history (if any) 4. The room subject [...]" +
        "§ 7.2.2 This self-presence MUST NOT be sent to the new occupant until the room has sent the presence of all other occupants to the new occupant ... The service MUST first send the complete list of the existing occupants to the new occupant and only then send the new occupant's own presence to the new occupant")
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
            String body = message.getBody();
            if (body == null) return;

            if (body.equals(mucMessage)) {
                messageReflectionSyncPoint.signal();
            }
        });

        mucAsSeenByOne.sendMessage(mucMessage);
        messageReflectionSyncPoint.waitForResult(timeout);

        final ResultSyncPoint<String, Exception> subjectResultSyncPoint = new ResultSyncPoint<>();
        List<Object> results = new ArrayList<>();

        mucAsSeenByTwo.addMessageListener(message -> {
            String body = message.getBody();
            if (mucMessage.equals(body)) {
                results.add(body);
            }
        });

        mucAsSeenByTwo.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override public void joined(EntityFullJid participant) {
                // Ignore self-presence, but record all other participants.
                final EntityFullJid participantTwo = JidCreate.entityFullFrom(mucAddress, nicknameTwo);
                if (!participantTwo.equals(participant)) {
                    results.add(participant);
                }
            }
        });

        mucAsSeenByTwo.addSubjectUpdatedListener((subject, from) -> {
            results.add(subject);
            subjectResultSyncPoint.signal(subject);
        });

        try {
            Presence reflectedJoinPresence = mucAsSeenByTwo.join(nicknameTwo);
            results.add(reflectedJoinPresence.getFrom()); // Self-presence should be second

            subjectResultSyncPoint.waitForResult(timeout); // Wait for subject, as it should be 4th (last)

            assertEquals(4, results.size(), "Unexpected amount of stanzas received by '" + conTwo.getUser() + "' after it joined room '" + mucAddress + "'.");
            assertEquals(JidCreate.fullFrom(mucAddress, nicknameOne), results.get(0), "Unexpected 'from' address of the first stanza that was received by '" + conTwo.getUser() + "' after it joined room '" + mucAddress + "'.");
            assertEquals(JidCreate.fullFrom(mucAddress, nicknameTwo), results.get(1), "Unexpected 'from' address of the seconds stanza that was received by '" + conTwo.getUser() + "' after it joined room '" + mucAddress + "'.");
            assertEquals(mucMessage, results.get(2), "The third stanza that was received by '" + conTwo.getUser() + "' after it joined room '" + mucAddress + "' was expected to be a different stanza.");
            assertEquals(mucSubject, results.get(3), "The fourth stanza that was received by '" + conTwo.getUser() + "' after it joined room '" + mucAddress + "' was expected to be a different stanza.");
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
    @SmackIntegrationTest(section = "7.2.1 & 7.4", quote =
        "§ 7.2.1: In order to participate in the discussions held in a multi-user chat room, a user MUST first become an occupant by entering the room [...] " +
        "§ 7.4: If the sender is not an occupant of the room, the service SHOULD return a <not-acceptable/> error to the sender and SHOULD NOT reflect the message to all occupants")
    public void mucSendBeforeJoiningTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-send-without-joining");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        createMuc(mucAsSeenByOne, Resourcepart.from("one-" + randomString));

        ResultSyncPoint<Message, Exception> errorMessageResultSyncPoint = new ResultSyncPoint<>();
        conTwo.addStanzaListener(packet -> errorMessageResultSyncPoint.signal((Message) packet), ForEveryMessage.INSTANCE);

        ResultSyncPoint<Message, Exception> distributedMessageResultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addMessageListener(distributedMessageResultSyncPoint::signal);

        try {
            mucAsSeenByTwo.sendMessage("Message without Joining");
            Message response = assertResult(errorMessageResultSyncPoint, "Expected an error to be returned to '" + conTwo.getUser() + "' after it sent a message to room '" + mucAddress + "' without joining it first (but no error was returned).");
            assertEquals(StanzaError.Condition.not_acceptable, response.getError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conTwo.getUser() + "' after it sent a message to room '" + mucAddress + "' without joining it first.");
            assertThrows(TimeoutException.class, () -> distributedMessageResultSyncPoint.waitForResult(1000), "Occupant '" + conOne.getUser() + "' should NOT have seen the message that was sent by '" + conTwo.getUser() + "' to room '" + mucAddress + "' without the sender have joined the room (but the message was observed by the occupant).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user joins a room, they are sent presence information about existing participants and
     * themselves that includes role and affiliation information and appropriate status codes.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.2", quote =
        "If the service is able to add the user to the room, it MUST send presence from all the existing participants' " +
        "occupant JIDs to the new occupant's full JID, including extended presence information about roles in a " +
        "single <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an " +
        "<item/> child with the 'role' attribute set to a value of \"moderator\", \"participant\", or \"visitor\", " +
        "and with the 'affiliation' attribute set to a value of \"owner\", \"admin\", \"member\", or \"none\" as " +
        "appropriate. [...] the \"self-presence\" sent by the room to the new user MUST include a status code of 110 " +
        "so that the user knows this presence refers to itself as an occupant [...] The service MUST first send the " +
        "complete list of the existing occupants to the new occupant and only then send the new occupant's own " +
        "presence to the new occupant.")
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

        List<Presence> results = new ArrayList<>();
        mucAsSeenByThree.addParticipantListener(results::add);

        try {
            // Will block until all self-presence is received, prior to which all others presences will have been received.
            mucAsSeenByThree.join(nicknameThree);

            assertEquals(3, results.size(), "Unexpected amount of occupants seen by '" + conThree + "' in room '" + mucAddress + "' after joining."); // The 3rd will be self-presence.
            assertNotNull(MUCUser.from(results.get(0)), "Expected to be able to parse a MUC occupant from '" + results.get(0) + "', but could not. Its syntax is likely incorrect."); // Smack implementation guarantees the "x" element and muc#user namespace

            // The order of all but the last presence (which should be the self-presence) is unpredictable.
            MUCItem mucItemSelf = MUCUser.from(results.get(2)).getItem();
            Set<MUCItem> others = new HashSet<>();
            others.add(MUCUser.from(results.get(0)).getItem());
            others.add(MUCUser.from(results.get(1)).getItem());

            assertEquals(MUCAffiliation.none, mucItemSelf.getAffiliation(), "Unexpected MUC affiliation in reflected self-presence of '" + conThree.getUser() + "' joining room '" + mucAddress + "'.");
            assertEquals(1, others.stream().filter(item -> MUCAffiliation.owner.equals(item.getAffiliation())).count(), "Unexpected amount of other occupants in room '" + mucAddress + "' (as observed by '" + conThree.getUser() + "') that have the 'owner' affiliation.");
            assertEquals(1, others.stream().filter(item -> MUCAffiliation.none.equals(item.getAffiliation())).count(), "Unexpected amount of other occupants in room '" + mucAddress + "' (as observed by '" + conThree.getUser() + "') that have no affiliation.");

            assertEquals(MUCRole.participant, mucItemSelf.getRole(), "Unexpected MUC role in reflected self-presence of '" + conThree.getUser() + "' joining room '" + mucAddress + "'.");
            assertEquals(2, others.stream().filter(item -> MUCRole.moderator.equals(item.getRole())).count(), "Unexpected amount of other occupants in room '" + mucAddress + "' (as observed by '" + conThree.getUser() + "') that have the 'moderator' role.");

            assertTrue(MUCUser.from(results.get(2)).getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Expected to find status '" + MUCUser.Status.PRESENCE_TO_SELF_110 + "' in reflected self-presence of '" + conThree.getUser() + "' joining room '" + mucAddress + "' (but did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user joins a room, all users are sent presence information about the new participant.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.2", quote =
        "the service MUST also send presence from the new participant's occupant JID to the full JIDs of all the " +
        "occupants (including the new occupant)")
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

            List<Presence> results = syncPoint.waitForResults(timeout);
            assertTrue(results.stream().allMatch(
                            result -> JidCreate.fullFrom(mucAddress, nicknameThree).equals(result.getFrom())),
                "Expected all occupants of room '" + mucAddress + "' to be notified of '" + conThree.getUser() + "' using nickname '" + nicknameThree + "' joining the room (but one or more got notified ).");
            assertTrue(results.stream().anyMatch(
                            result -> result.getTo().equals(conOne.getUser().asEntityFullJidIfPossible())),
                "Expected '" + conOne.getUser().asEntityFullJidIfPossible() + "' to be notified of '" + conThree.getUser() + "' joining room '" + mucAddress + "' (but did not)");
            assertTrue(results.stream().anyMatch(
                            result -> result.getTo().equals(conTwo.getUser().asEntityFullJidIfPossible())),
                "Expected '" + conTwo.getUser().asEntityFullJidIfPossible() + "' to be notified of '" + conThree.getUser() + "' joining room '" + mucAddress + "' (but did not)");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user enters a non-anonymous room, the presence notifications contain extended presence
     * information.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.3", quote =
        "If the room is non-anonymous, the service MUST send the new occupant's full JID to all occupants using " +
        "extended presence information in an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' " +
        "namespace and containing an <item/> child with a 'jid' attribute specifying the occupant's full JID. [...]" +
        "If the user is entering a room that is non-anonymous (i.e., which informs all occupants of each occupant's " +
        "full JID as shown above), the service MUST warn the user by including a status code of \"100\" in the " +
        "initial presence that the room sends to the new occupant.")
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
            Presence presenceReceivedByOne = assertResult(participantOneSyncPoint, "Expected '" + conOne.getUser() + "' to receive a presence stanza after '" + conTwo.getUser() + "' joined room '" + mucAddress + "' (but did not).");
            Presence presenceReceivedByTwo = assertResult(participantTwoSyncPoint, "Expected '" + conTwo.getUser() + "' to receive a presence stanza after they themselves joined room '" + mucAddress + "' (but did not).");

            // Check the presence received by participant one for inclusion of full jid of participant two
            MUCUser announcedParticipantTwoUser = MUCUser.from(presenceReceivedByOne);
            assertNotNull(announcedParticipantTwoUser, "Expected to be able to parse a MUC occupant from '" + presenceReceivedByOne + "', but could not. Its syntax is likely incorrect."); // Smack implementation guarantees the "x" element and muc#user namespace
            assertNotNull(announcedParticipantTwoUser.getItem(), "Expected to be able to parse a MUC occupant item from '" + presenceReceivedByOne + "', but could not. Its syntax is likely incorrect.");
            assertEquals(conTwo.getUser().asEntityFullJidOrThrow(), announcedParticipantTwoUser.getItem().getJid(), "Expected extended presence information received by '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' joined room '" + mucAddress + "' to include their full JID.");

            // Check the presence received by participant two for inclusion of status 100
            assertTrue(MUCUser.from(presenceReceivedByTwo).getStatus().stream().anyMatch(status -> 100 == status.getCode()),
                "Expected to find status '100' in reflected self-presence of '" + conTwo.getUser() + "' joining room '" + mucAddress + "' (but did not).");

        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user enters a semi-anonymous room, the presence notifications received by occupants that
     * are not a moderator does not contain extended presence information.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.4", quote =
        "If the room is semi-anonymous, the service MUST send presence from the new occupant to all occupants as " +
        "specified above (i.e., unless the room is configured to not broadcast presence from new occupants below a " +
        "certain affiliation level as controlled by the \"muc#roomconfig_presencebroadcast\" room configuration " +
        "option), but MUST include the new occupant's full JID only in the presence notifications it sends to " +
        "occupants with a role of \"moderator\" and not to non-moderator occupants.")
    public void mucJoinSemiAnonymousRoomReceivedByNonModeratorTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-seminanonymous-by-non-moderator");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createSemiAnonymousMuc(mucAsSeenByOne, nicknameOne);

        mucAsSeenByTwo.join(nicknameTwo);

        // First pass: participant two is not a moderator yet
        final ResultSyncPoint<Presence, ?> participantTwoSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByThree.join(nicknameThree);
            Presence presenceReceivedByTwo = assertResult(participantTwoSyncPoint, "Expected '" + conTwo.getUser() + "' to receive presence when '" + conThree.getUser() + "' joined room '" + mucAddress + "' (but did not).");

            // Check the presence received by participant two for exclusion of full jid of participant three
            assertNull(MUCUser.from(presenceReceivedByTwo).getItem().getJid(), "Did not expect '" + conTwo.getUser() + "' (who is not a moderator at this stage) to receive the full JID of '" + conThree.getUser() + "' when they joined room '" + mucAddress + "' (but they did).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user enters a semi-anonymous room, the presence notifications contain extended presence
     * information when sent to moderators.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.4", quote =
        "If the room is semi-anonymous, the service MUST send presence from the new occupant to all occupants as " +
        "specified above (i.e., unless the room is configured to not broadcast presence from new occupants below a " +
        "certain affiliation level as controlled by the \"muc#roomconfig_presencebroadcast\" room configuration " +
        "option), but MUST include the new occupant's full JID only in the presence notifications it sends to " +
        "occupants with a role of \"moderator\" and not to non-moderator occupants.")
    public void mucJoinSemiAnonymousRoomReceivedByModeratorTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-seminanonymous-by-moderator");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        // Second pass: participant two is now a moderator
        createSemiAnonymousMuc(mucAsSeenByOne, nicknameOne);
        mucAsSeenByTwo.join(nicknameTwo);

        mucAsSeenByOne.grantModerator(nicknameTwo);
        final ResultSyncPoint<Presence, ?> participantTwoSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(presence -> {
            if (nicknameThree.equals(presence.getFrom().getResourceOrEmpty())) {
                participantTwoSyncPoint.signal(presence);
            }
        });

        try {
            mucAsSeenByThree.join(nicknameThree);
            Presence presenceReceivedByTwo = assertResult(participantTwoSyncPoint, "Expected '" + conTwo.getUser() + "' to receive presence when '" + conThree.getUser() + "' joined room '" + mucAddress + "' (but did not).");

            // Check the presence received by participant two for inclusion of full jid of participant three
            MUCUser announcedParticipantThreeUser = MUCUser.from(presenceReceivedByTwo);
            assertEquals(conThree.getUser().asEntityFullJidOrThrow(), announcedParticipantThreeUser.getItem().getJid(), "Expected '" + conTwo.getUser() + "' (who is a moderator at this stage) to receive the full JID of '" + conThree.getUser() + "' when they joined room '" + mucAddress + "' (but they did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user can not enter a password-protected room without supplying a password.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.5", quote =
        "If the room requires a password and the user did not supply one (or the password provided is incorrect), the " +
        "service MUST deny access to the room and inform the user that they are unauthorized; this is done by returning " +
        "a presence stanza of type \"error\" specifying a <not-authorized/> error.")
    public void mucJoinPasswordProtectedWithoutPasswordRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-enterpasswordprotectedroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        final String correctPassword = StringUtils.insecureRandomString(8);

        createPasswordProtectedMuc(mucAsSeenByOne, nicknameOne, correctPassword);

        try {
            // First try: no password
            XMPPException.XMPPErrorException noPasswordErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo),
                "Expected an error to be returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' without a password.");
            assertNotNull(noPasswordErrorException.getStanzaError(),
                "Expected an error to be returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' without a password.");
            assertEquals(StanzaError.Condition.not_authorized, noPasswordErrorException.getStanzaError().getCondition(),
                        "Unexpected condition in the (expected) error that was returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' without a password.");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a password-protected room without supplying the correct password.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.5", quote =
        "If the room requires a password and the user did not supply one (or the password provided is incorrect), the " +
            "service MUST deny access to the room and inform the user that they are unauthorized; this is done by returning " +
            "a presence stanza of type \"error\" specifying a <not-authorized/> error.")
    public void mucJoinPasswordProtectedRoomWrongPasswordTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-enterpasswordprotectedroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        final String correctPassword = StringUtils.insecureRandomString(8);

        createPasswordProtectedMuc(mucAsSeenByOne, nicknameOne, correctPassword);

        try {
            // Second try: wrong password
            XMPPException.XMPPErrorException wrongPasswordErrorException = assertThrows(
                XMPPException.XMPPErrorException.class,
                () -> mucAsSeenByTwo.join(nicknameTwo, correctPassword + "_"),
                "Expected an error to be returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' using an incorrect password.");
            assertNotNull(wrongPasswordErrorException.getStanzaError(),
                "Expected an error to be returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' using an incorrect password.");
            assertEquals(StanzaError.Condition.not_authorized, wrongPasswordErrorException.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error that was returned when '" + conTwo.getUser() + "' attempted to join password-protected room '" + mucAddress + "' using an incorrect password.");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can enter a password-protected room when supplying the correct password.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.5", quote =
        "If the room requires a password and the user did not supply one (or the password provided is incorrect), the " +
            "service MUST deny access to the room and inform the user that they are unauthorized; this is done by returning " +
            "a presence stanza of type \"error\" specifying a <not-authorized/> error.")
    public void mucJoinPasswordProtectedRoomCorrectPasswordTest() throws Exception {
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
            // Third try: correct password
            mucAsSeenByTwo.join(nicknameTwo, correctPassword);
            Presence presenceCorrectPassword = assertResult(participantTwoSyncPoint, "Expected '" + conTwo.getUser() + "' to be able to join password-protected room '" + mucAddress + "' using the correct password (but no join-presence was received).");
            assertNull(presenceCorrectPassword.getError(), "Unexpected error in join-presence of '" + conTwo.getUser() + "' after joining password-protected room '" + mucAddress + "' using the correct password: " + presenceCorrectPassword.getError());
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a members-only room while not being a member.
     *
     * This test does not cover § 9.3, aka the happy path.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.6", quote =
        "If the room is members-only but the user is not on the member list, the service MUST deny access to the " +
        "room and inform the user that they are not allowed to enter the room; this is done by returning a presence " +
        "stanza of type \"error\" specifying a <registration-required/> error condition.")
    public void mucJoinMembersOnlyRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-entermembersonlyroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMembersOnlyMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException registrationRequiredErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo),
                "Expected an error to be returned when non-member '" + conTwo.getUser() + "' tries to join member-online room '" + mucAddress + "' (but an error was not returned).");
            assertNotNull(registrationRequiredErrorException, "Expected an error to be returned when non-member '" + conTwo.getUser() + "' tries to join member-online room '" + mucAddress + "' (but an error was not returned).");
            assertNotNull(registrationRequiredErrorException.getStanzaError(), "Expected an error to be returned when non-member '" + conTwo.getUser() + "' tries to join member-online room '" + mucAddress + "' (but an error was not returned).");
            assertEquals(StanzaError.Condition.registration_required, registrationRequiredErrorException.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error that was returned when non-member '" + conTwo.getUser() + "' joined member-online room '" + mucAddress + "' (.");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a room while being banned.
     *
     * <p>From XEP-0045 § 7.2.7:</p>
     * <blockquote>
     *
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.7", quote =
        "If the user has been banned from the room (i.e., has an affiliation of \"outcast\"), the service MUST deny " +
        "access to the room and inform the user of the fact that they are banned; this is done by returning a presence " +
        "stanza of type \"error\" specifying a <forbidden/> error condition.")
    public void mucBannedUserJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-banneduser");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        mucAsSeenByOne.banUser(conTwo.getUser().asBareJid(), "Insufficient witchcraft");

        try {
            XMPPException.XMPPErrorException forbiddenErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo),
                "Expected an error to be returned when outcast '" + conTwo.getUser() + "' tries to join room '" + mucAddress + "' (but an error was not returned).");
            assertNotNull(forbiddenErrorException, "Expected an error to be returned when outcast '" + conTwo.getUser() + "' tries to join room '" + mucAddress + "' (but an error was not returned).");
            assertNotNull(forbiddenErrorException.getStanzaError(), "Expected an error to be returned when outcast '" + conTwo.getUser() + "' tries to join room '" + mucAddress + "' (but an error was not returned).");
            assertEquals(StanzaError.Condition.forbidden, forbiddenErrorException.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error to was returned when outcast '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "'.");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user can not enter a room with the same nickname as another user who is already present.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.8", quote =
        "If the room already contains another user with the nickname desired by the user seeking to enter the room " +
        "(or if the nickname is reserved by another user on the member list), the service MUST deny access to the " +
        "room and inform the user of the conflict; this is done by returning a presence stanza of type \"error\" " +
        "specifying a <conflict/> error condition.")
    public void mucNicknameConflictJoinRoomTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-nicknameclash");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException conflictErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameOne),
                "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' using the nickname '" + nicknameOne + "' that was already in used by another occupant of the room (but an error was not returned).");
            assertNotNull(conflictErrorException, "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' using the nickname '" + nicknameOne + "' that was already in used by another occupant of the room (but an error was not returned).");
            assertNotNull(conflictErrorException.getStanzaError(), "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' using the nickname '" + nicknameOne + "' that was already in used by another occupant of the room (but an error was not returned).");
            assertEquals(StanzaError.Condition.conflict, conflictErrorException.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error that was returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' using the nickname '" + nicknameOne + "' that was already in used by another occupant of the room.");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a room can not go past the configured maximum number of users, if a non-admin non-owner user
     * attempts to join.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.9", quote =
        "If the room has reached its maximum number of occupants, the service SHOULD deny access to the room and " +
        "inform the user of the restriction; this is done by returning a presence stanza of type \"error\" " +
        "specifying a <service-unavailable/> error condition. Alternatively, the room could kick an \"idle user\" " +
        "in order to free up space (where the definition of \"idle user\" is up to the implementation).")
    public void mucMaxUsersLimitJoinRoomTest() throws Exception {
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

            assertEquals(2, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count as seen by '" + conOne.getUser() + "' in room '" + mucAddress + "' (prior to the join attempt of a third occupant).");

            // Now user 3 may or may not be able to join the room. The service can either deny access to user three, or
            // it can kick user 2. Both strategies would comply with the specification. So the only thing we can
            // reasonably test here is whether the room doesn't have more occupants than its max size.

            XMPPException.XMPPErrorException errorException = assertThrows(XMPPException.XMPPErrorException.class, () -> mucAsSeenByThree.join(nicknameThree));

            final StanzaError.Condition expectedCondition;
            switch (sinttestConfiguration.compatibilityMode) {
            default:
                expectedCondition = StanzaError.Condition.service_unavailable;
                break;
            case ejabberd:
                expectedCondition = StanzaError.Condition.resource_constraint;
                break;
            }

            StanzaError stanzaError = errorException.getStanzaError();
            assertNotNull(stanzaError);

            assertEquals(expectedCondition, stanzaError.getCondition());

            // Now we should wait until participant one is informed about the (probably failed) new participant three
            // room join. But if joining failed, there will be no such update. All we can reasonably do is wait until
            // participant three has received its own presence response. This is not watertight though.
            participantThreeSeesThreeSyncPoint.waitForResult(timeout);

            // Irrespective of the way the implementation handles max users, there should still be only 2 users.
            assertEquals(2, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count as seen by '" + conOne.getUser() + "' in room '" + mucAddress + "' (after the join attempt of a third occupant).");

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that an admin can still join a room that has reached the configured maximum number of users.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.9", quote =
        "If the room has reached its maximum number of occupants and a room admin or owner attempts to join, the " +
        "room MUST allow the admin or owner to join, up to some reasonable number of additional occupants; this " +
        "helps to prevent denial of service attacks caused by stuffing the room with non-admin users.")
    public void mucMaxUsersLimitAdminCanStillJoinRoomTest() throws Exception {
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

            assertEquals(2, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "') (prior to an admin attempting to join a room that was already having its maximum number of occupants).");

            mucAsSeenByOne.grantAdmin(conThree.getUser().asBareJid()); // blocking call
            mucAsSeenByThree.join(nicknameThree);
            participantOneSeesThreeSyncPoint.waitForResult(timeout);

            assertNotNull(mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)), "Expected admin '" + conThree.getUser() + "' to be in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "'), but was not.");
            assertEquals(3, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "') (after an admin joined a room that was already having its maximum number of occupants)");
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that an owner can still join a room that has reached the configured maximum number of users.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.9", quote =
        "If the room has reached its maximum number of occupants and a room admin or owner attempts to join, the " +
        "room MUST allow the admin or owner to join, up to some reasonable number of additional occupants; this " +
        "helps to prevent denial of service attacks caused by stuffing the room with non-admin users.")
    public void mucMaxUsersLimitOwnerCanStillJoinRoomTest() throws Exception {
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

            assertEquals(2, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "') (prior to an owner attempting to join a room that was already having its maximum number of occupants).");

            mucAsSeenByOne.grantOwnership(conThree.getUser().asBareJid()); // blocking call
            mucAsSeenByThree.join(nicknameThree);
            participantOneSeesThreeSyncPoint.waitForResult(timeout);

            assertNotNull(mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)), "Expected owner '" + conThree.getUser() + "' to be in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "'), but was not.");
            assertEquals(3, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room '" + mucAddress + "' (as observed by '" + conOne.getUser() + "') (after an owner joined a room that was already having its maximum number of occupants)");

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a room can not be entered while it still being created (locked).
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.10", quote =
        "If a user attempts to enter a room while it is \"locked\" (i.e., before the room creator provides an " +
        "initial configuration and therefore before the room officially exists), the service MUST refuse entry and " +
        "return an <item-not-found/> error to the user.")
    public void mucJoinLockedRoomTest() throws Exception {
        if (sinttestConfiguration.compatibilityMode == Configuration.CompatibilityMode.ejabberd) {
            throw new TestNotPossibleException("ejabberd does not implement MUC locked rooms as per XEP-0045 § 7.2.10");
        }

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-lockedroom");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        // Note the absence of handle.makeInstant() here. The room is still being created at this point, until a
        // configuration is set.
        mucAsSeenByOne.create(nicknameOne);

        try {
            XMPPException.XMPPErrorException conflictErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.join(nicknameTwo),
                "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' that is still being created/is locked (but no error was returned).");
            assertNotNull(conflictErrorException, "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' that is still being created/is locked (but no error was returned).");
            assertNotNull(conflictErrorException.getStanzaError(), "Expected an error to be returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' that is still being created/is locked (but no error was returned).");
            assertEquals(StanzaError.Condition.item_not_found, conflictErrorException.getStanzaError().getCondition(),
                "Unexpected condition in the (expected) error that was returned when '" + conTwo.getUser() + "' tried to join room '" + mucAddress + "' that is still being created/is locked.");

        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that a user is warned when entering a room that allows public logging.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.2.12", quote =
        "If the user is entering a room in which the discussions are logged to a public archive (often accessible " +
        "via HTTP), the service SHOULD allow the user to enter the room but MUST also warn the user that the " +
        "discussions are logged. This is done by including a status code of \"170\" in the initial presence that the " +
        "room sends to the new occupant.")
    public void mucJoinRoomWithPublicLoggingTest() throws Exception {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-publiclogging");

        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            mucAsSeenByOne.getConfigFormManager()
                .enablePublicLogging()
                .submitConfigurationForm();

            Presence twoPresence = mucAsSeenByTwo.join(nicknameTwo);
            assertTrue(MUCUser.from(twoPresence).getStatus().contains(MUCUser.Status.create(170)),
                "Expected initial presence reflected to '" + conTwo.getUser() + "' when joining room '" + mucAddress + "' to include the status code '170' (but it did not).");
        } catch (MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            mucAsSeenByOne.destroy();
        }
    }

    /**
     * Asserts that all users in a room are correctly informed about nickname change of a participant.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.6", quote =
        "A common feature of chat rooms is the ability for an occupant to change his or her nickname within the room. " +
        "In MUC this is done by sending updated presence information to the room, specifically by sending presence to " +
        "a new occupant JID in the same room (changing only the resource identifier in the occupant JID). The service " +
        "then sends two presence stanzas to the full JID of each occupant (including the occupant who is changing his " +
        "or her room nickname), one of type \"unavailable\" for the old nickname and one indicating availability for " +
        "the new nickname. The unavailable presence MUST contain the following as extended presence information in an " +
        "<x/>; element qualified by the 'http://jabber.org/protocol/muc#user' namespace: - The new nickname (in this " +
        "case, nick='oldhag') - A status code of 303 This enables the recipients to correlate the old roomnick with " +
        "the new roomnick.\n")
    public void mucChangeNicknameInformationTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-changenickname");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwoOriginal = Resourcepart.from("two-original-" + randomString);
        final Resourcepart nicknameTwoNew = Resourcepart.from("two-new-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            SimpleResultSyncPoint participantOneSeesTwoEnter = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantListener(presence -> {
                if (nicknameTwoOriginal.equals(presence.getFrom().getResourceOrEmpty())) {
                    participantOneSeesTwoEnter.signal();
                }
            });

            // Have participant two enter the room
            mucAsSeenByTwo.join(nicknameTwoOriginal);
            participantOneSeesTwoEnter.waitForResult(timeout);

            // Although logic dictates that the 'unavailable' presence stanzas for the old nick should precede the presence
            // stanza for the new nick - the specification does not dictate that. So we should allow for the order to be
            // reversed. Here we will expect an unavailable and an available presence stanza sent to both participant one
            // and participant two. So that adds up to a total of four.
            MultiResultSyncPoint<Presence, ?> participantTwoPresencesSyncPoint = new MultiResultSyncPoint<>(4);
            PresenceListener mucPresenceListener = presence -> {
                Resourcepart fromResource = presence.getFrom().getResourceOrEmpty();
                if (nicknameTwoOriginal.equals(fromResource) || nicknameTwoNew.equals(fromResource)) {
                    participantTwoPresencesSyncPoint.signal(presence);
                }
            };
            mucAsSeenByOne.addParticipantListener(mucPresenceListener);
            mucAsSeenByTwo.addParticipantListener(mucPresenceListener);

            // Participant two changes nickname
            mucAsSeenByTwo.changeNickname(nicknameTwoNew);
            final List<Presence> partTwoPresencesReceived = participantTwoPresencesSyncPoint.waitForResults(
                            timeout);

            final List<Presence> unavailablePresencesReceivedByOne = partTwoPresencesReceived.stream().filter(
                            presence -> !presence.isAvailable()).filter(
                            presence -> presence.getTo().equals(conOne.getUser().asEntityFullJidIfPossible())).collect(
                            Collectors.toList());
            final List<Presence> unavailablePresencesReceivedByTwo = partTwoPresencesReceived.stream().filter(
                            presence -> !presence.isAvailable()).filter(
                            presence -> presence.getTo().equals(conTwo.getUser().asEntityFullJidIfPossible())).collect(
                            Collectors.toList());
            final List<Presence> availablePresencesReceivedByOne = partTwoPresencesReceived.stream().filter(
                            Presence::isAvailable).filter(
                            presence -> presence.getTo().equals(conOne.getUser().asEntityFullJidIfPossible())).collect(
                            Collectors.toList());
            final List<Presence> availablePresencesReceivedByTwo = partTwoPresencesReceived.stream().filter(
                            Presence::isAvailable).filter(
                            presence -> presence.getTo().equals(conTwo.getUser().asEntityFullJidIfPossible())).collect(
                            Collectors.toList());

            // Validate that both users received both 'available' and 'unavailable' presence stanzas
            assertEquals(1, unavailablePresencesReceivedByOne.size(), "Expected '" + conOne.getUser() + "' to have received one 'unavailable' presence when '" + conTwo.getUser() + "' changed its nickname in room '" + mucAddress + "' (but did not)");
            assertEquals(1, unavailablePresencesReceivedByTwo.size(), "Expected '" + conTwo.getUser() + "' to have received one 'unavailable' presence when they changed their nickname in room '" + mucAddress + "' (but did not)");
            assertEquals(1, availablePresencesReceivedByOne.size(), "Expected '" + conOne.getUser() + "' to have received one 'available' presence when '" + conTwo.getUser() + "' changed its nickname in room '" + mucAddress + "' (but did not)");
            assertEquals(1, availablePresencesReceivedByTwo.size(), "Expected '" + conTwo.getUser() + "' to have received one 'available' presence when they changed their nickname in room '" + mucAddress + "' (but did not)");

            // Validate that the received 'unavailable' presence stanzas contain the status and items elements as specified
            assertTrue(MUCUser.from(unavailablePresencesReceivedByOne.get(0)).getStatus().stream().anyMatch(status -> 303 == status.getCode()),
                "Expected the 'unavailable' presence of user '" + conTwo.getUser() + "' reflecting their nickname change in room '" + mucAddress + "' as received by '" + conOne.getUser() + "' to include status code '303' (but it did not).");
            assertEquals(nicknameTwoNew, MUCUser.from(unavailablePresencesReceivedByOne.get(0)).getItem().getNick(),
                "Expected the 'unavailable' presence of user '" + conTwo.getUser() + "' reflecting their nickname change in room '" + mucAddress + "' as received by '" + conOne.getUser() + "' to include the new nickname (but it did not).");
            assertTrue(MUCUser.from(unavailablePresencesReceivedByTwo.get(0)).getStatus().stream().anyMatch(status -> 303 == status.getCode()),
                "Expected the 'unavailable' presence of user '" + conTwo.getUser() + "' reflecting their nickname change in room '" + mucAddress + "' as received by themselves to include status code '303' (but it did not).");
            assertEquals(nicknameTwoNew, MUCUser.from(unavailablePresencesReceivedByTwo.get(0)).getItem().getNick(),
                "Expected the 'unavailable' presence of user '" + conTwo.getUser() + "' reflecting their nickname change in room '" + mucAddress + "' as received by themselves to include the new nickname (but it did not).");

            // Validate that the received 'available' presence stanzas have the new nickname as from
            assertEquals(nicknameTwoNew, availablePresencesReceivedByOne.get(0).getFrom().getResourceOrEmpty(), "Expected the 'available' presence of '" + conTwo.getUser() + "' as received by '" + conOne.getUser() + "' in room '" + mucAddress + "' to be sent 'from' their new nickname (but it was not).");
            assertEquals(nicknameTwoNew, availablePresencesReceivedByTwo.get(0).getFrom().getResourceOrEmpty(), "Expected the 'available' presence of '" + conTwo.getUser() + "' as received by themselves in room '" + mucAddress + "' to be sent 'from' their new nickname (but it was not).");

        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that user can not change nickname to one that is already in use.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.6", quote =
        "If the user attempts to change his or her room nickname to a room nickname that is already in use by " +
        "another user (or that is reserved by another user affiliated with the room, e.g., a member or owner), the " +
        "service MUST deny the nickname change request and inform the user of the conflict; this is done by " +
        "returning a presence stanza of type \"error\" specifying a <conflict/> error condition:")
    public void mucBlockChangeNicknameInformationTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-blockchangenickname");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwoOriginal = Resourcepart.from("two-original-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);

        SimpleResultSyncPoint participantOneSeesTwoEnter = new SimpleResultSyncPoint();
        mucAsSeenByOne.addParticipantListener(presence -> {
            if (nicknameTwoOriginal.equals(presence.getFrom().getResourceOrEmpty())) {
                participantOneSeesTwoEnter.signal();
            }
        });

        // Have participant two enter the room
        mucAsSeenByTwo.join(nicknameTwoOriginal);
        participantOneSeesTwoEnter.waitForResult(timeout);

        try {
            // Participant two changes nickname
            XMPPException.XMPPErrorException conflictErrorException = assertThrows(
                            XMPPException.XMPPErrorException.class, () -> mucAsSeenByTwo.changeNickname(nicknameOne),
                "Expected an error to be returned when '" + conTwo.getUser() + "' tried to change their nickname in room '" + mucAddress + "' to a nickname that was already in use by another occupant (but no error was returned).");
            assertNotNull(conflictErrorException, "Expected an error to be returned when '" + conTwo.getUser() + "' tried to change their nickname in room '" + mucAddress + "' to a nickname that was already in use by another occupant (but no error was returned).");
            assertNotNull(conflictErrorException.getStanzaError(), "Expected an error to be returned when '" + conTwo.getUser() + "' tried to change their nickname in room '" + mucAddress + "' to a nickname that was already in use by another occupant (but no error was returned).");
            assertEquals(StanzaError.Condition.conflict, conflictErrorException.getStanzaError().getCondition(),
                "Unexpected conidtion in the (expected) error to was returned when '" + conTwo.getUser() + "' tried to change their nickname in room '" + mucAddress + "' to a nickname that was already in use by another occupant.");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that when a user leaves a room, they are themselves included on the list of users notified (self-presence).
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "7.14", quote =
        "The service MUST then send a presence stanzas of type \"unavailable\" from the departing user's occupant " +
        "JID to the departing occupant's full JIDs, including a status code of \"110\" to indicate that this " +
        "notification is \"self-presence\"")
    public void mucLeaveTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-leave");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        try {
            muc.join(Resourcepart.from("nick-one"));

            Presence reflectedLeavePresence = muc.leave();

            MUCUser mucUser = MUCUser.from(reflectedLeavePresence);
            assertNotNull(mucUser, "Expected the reflected 'leave' presence of '" + conOne.getUser() + "' that left room '" + mucAddress + "' to include a valid 'user' child element (but it did not).");

            assertTrue(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Expected the reflected 'leave' presence of '" + conOne.getUser() + "' that left room '" + mucAddress + "' to include status '" + MUCUser.Status.PRESENCE_TO_SELF_110 + "' (but it did not).");
            assertEquals(mucAddress + "/nick-one", reflectedLeavePresence.getFrom().toString(), "Unexpected 'from' attribute value in the reflected 'leave' presence of '" + conOne.getUser() + "' that left room '" + mucAddress + "'.");
            assertEquals(conOne.getUser().asEntityFullJidIfPossible().toString(), reflectedLeavePresence.getTo().toString(), "Unexpected 'to' attribute value in the reflected 'leave' presence of '" + conOne.getUser() + "' that left room '" + mucAddress + "'.");
        } finally {
            muc.join(Resourcepart.from("nick-one")); // We need to be in the room to destroy the room
            tryDestroy(muc);
        }
    }
}
