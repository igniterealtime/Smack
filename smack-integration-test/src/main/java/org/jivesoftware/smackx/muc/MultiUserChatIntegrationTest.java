/**
 *
 * Copyright 2015-2020 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.muc.MultiUserChat.MucCreateConfigFormHandle;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

public class MultiUserChatIntegrationTest extends AbstractSmackIntegrationTest {

    private final String randomString = StringUtils.insecureRandomString(6);

    private final MultiUserChatManager mucManagerOne;
    private final MultiUserChatManager mucManagerTwo;
    private final MultiUserChatManager mucManagerThree;
    private final DomainBareJid mucService;

    public MultiUserChatIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException, TestNotPossibleException {
        super(environment);
        mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        mucManagerThree = MultiUserChatManager.getInstanceFor(conThree);

        List<DomainBareJid> services = mucManagerOne.getMucServiceDomains();
        if (services.isEmpty()) {
            throw new TestNotPossibleException("No MUC (XEP-45) service found");
        }
        else {
            mucService = services.get(0);
        }
    }

    /**
     * Asserts that when a user joins a room, they are themselves included on the list of users notified (self-presence).
     *
     * <p>From XEP-0045 § 7.2.2:</p>
     * <blockquote>
     * ...the service MUST also send presence from the new participant's occupant JID to the full JIDs of all the
     * occupants (including the new occupant)
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucJoinTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-join-leave");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        try {
            Presence reflectedJoinPresence = muc.join(Resourcepart.from("nick-one"));

            MUCUser mucUser = MUCUser.from(reflectedJoinPresence);

            assertNotNull(mucUser);
            assertTrue(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110));
            assertEquals(mucAddress + "/nick-one", reflectedJoinPresence.getFrom().toString());
            assertEquals(conOne.getUser().asEntityFullJidIfPossible().toString(), reflectedJoinPresence.getTo().toString());
        } finally {
            tryDestroy(muc);
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
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-join-leave");

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
            tryDestroy(muc);
        }
    }

    @SmackIntegrationTest
    public void mucTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final String mucMessage = "Smack Integration Test MUC Test Message " + randomString;
        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByTwo.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String body = message.getBody();
                if (mucMessage.equals(body)) {
                    resultSyncPoint.signal(body);
                }
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        mucAsSeenByTwo.join(Resourcepart.from("two-" + randomString));
        mucAsSeenByOne.sendMessage(mucMessage);
        try {
            resultSyncPoint.waitForResult(timeout);
        } catch (TimeoutException e) {
            throw new AssertionError("Failed to receive presence", e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 9.6:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the addition of
     * moderator status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForReceivingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void moderatorGranted() {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);

        // This implicitly tests "The service MUST add the user to the moderator list and then inform the admin of
        // success" in §9.6, since it'll throw on either an error IQ or on no response.
        mucAsSeenByOne.grantModerator(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 9.6:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the addition of
     * moderator status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForWitnessingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void moderatorGranted(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        mucAsSeenByOne.grantModerator(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }

    }

    /**
     * Asserts that a user who undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 9.7:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the removal of
     * moderator status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForRemovingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void moderatorRevoked() {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);

        mucAsSeenByOne.grantModerator(nicknameTwo);
        mucAsSeenByOne.revokeModerator(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 9.6:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the removal of
     * moderator status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForWitnessingModeratorRemoval() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void moderatorRevoked(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        mucAsSeenByOne.grantModerator(nicknameTwo);
        mucAsSeenByOne.revokeModerator(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user in an unmoderated room who undergoes an afilliation change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 8.4:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the removal of
     * voice privileges...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForRevokingVoice() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void voiceRevoked() {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByOne.revokeVoice(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.1.3:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the occupant's role to reflect the change and communicate the change
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 8.4:</p>
     * <blockquote>
     * The service MUST then send updated presence from this individual to all occupants, indicating the removal of
     * voice privileges...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucRoleTestForWitnessingRevokingVoice() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void voiceRevoked(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        mucAsSeenByOne.revokeVoice(nicknameTwo);
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who undergoes an affiliation change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.2.2:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the user's affiliation to reflect the change and communicate that
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 10.6:</p>
     * <blockquote>
     * If the user is in the room, the service MUST then send updated presence from this individual to all occupants,
     * indicating the granting of admin status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucAffiliationTestForReceivingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();


        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void adminGranted() {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);

        // This implicitly tests "The service MUST add the user to the admin list and then inform the owner of success" in §10.6, since it'll throw on either an error IQ or on no response.
        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes an affiliation change receives that change as a
     * presence update
     *
     * <p>From XEP-0045 § 5.2.2:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the user's affiliation to reflect the change and communicate that
     * to all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 10.6:</p>
     * <blockquote>
     * If the user is in the room, the service MUST then send updated presence from this individual to all occupants,
     * indicating the granting of admin status...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucAffiliationTestForWitnessingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.2.2:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the user's affiliation to reflect the change and communicate that to
     * all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 10.7:</p>
     * <blockquote>
     * If the user is in the room, the service MUST then send updated presence from this individual to all occupants,
     * indicating the loss of admin status by sending a presence element...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucAffiliationTestForRemovingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void adminRevoked() {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);

        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);

        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
        mucAsSeenByOne.revokeAdmin(conTwo.getUser().asEntityBareJid());
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update
     *
     * <p>From XEP-0045 § 5.2.2:</p>
     * <blockquote>
     * ...a MUC service implementation MUST change the user's affiliation to reflect the change and communicate that to
     * all occupants...
     * </blockquote>
     *
     * <p>From XEP-0045 § 10.7:</p>
     * <blockquote>
     * If the user is in the room, the service MUST then send updated presence from this individual to all occupants,
     * indicating the loss of admin status by sending a presence element...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucAffiliationTestForWitnessingAdminRemoval() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminRevoked(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);
        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());

        mucAsSeenByOne.revokeAdmin(conTwo.getUser().asEntityBareJid());
        try {
            resultSyncPoint.waitForResult(timeout);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who gets kicked receives that change as a presence update
     *
     * <p>From XEP-0045 § 8.2:</p>
     * <blockquote>
     * The kick is performed based on the occupant's room nickname and is completed by setting the role of a
     * participant or visitor to a value of "none".
     *
     * The service MUST remove the kicked occupant by sending a presence stanza of type "unavailable" to each kicked
     * occupant, including status code 307 in the extended presence information, optionally along with the reason (if
     * provided) and the roomnick or bare JID of the user who initiated the kick.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucPresenceTestForGettingKicked() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);

        final ResultSyncPoint<Presence, Exception> resultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByTwo.addParticipantListener(kickPresence -> resultSyncPoint.signal(kickPresence));

        mucAsSeenByOne.kickParticipant(nicknameTwo, "Nothing personal. Just a test.");
        try {
            Presence kickPresence = resultSyncPoint.waitForResult(timeout);
            MUCUser mucUser = MUCUser.from(kickPresence);
            assertNotNull(mucUser);
            assertAll(
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Missing self-presence status code in kick presence"),
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.KICKED_307), "Missing kick status code in kick presence"),
                    () -> assertEquals(MUCRole.none, mucUser.getItem().getRole(), "Role other than 'none' in kick presence")
            );
            Jid itemJid = mucUser.getItem().getJid();
            if (itemJid != null) {
                assertEquals(conTwo.getUser().asEntityFullJidIfPossible(), itemJid, "Incorrect kicked user in kick presence");
            }
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user gets kicked receives that change as a presence update
     *
     * <p>From XEP-0045 § 8.2:</p>
     * <blockquote>
     * ...the service MUST then inform all of the remaining occupants that the kicked occupant is no longer in the room
     * by sending presence stanzas of type "unavailable" from the individual's roomnick (&lt;room@service/nick&gt;) to all
     * the remaining occupants (just as it does when occupants exit the room of their own volition), including the
     * status code and optionally the reason and actor.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucPresenceTestForWitnessingKick() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        createMUC(mucAsSeenByOne, "one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        final ResultSyncPoint<Presence, Exception> resultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByThree.addParticipantListener(kickPresence -> resultSyncPoint.signal(kickPresence));

        mucAsSeenByOne.kickParticipant(nicknameTwo, "Nothing personal. Just a test.");
        try {
            Presence kickPresence = resultSyncPoint.waitForResult(timeout);
            MUCUser mucUser = MUCUser.from(kickPresence);
            assertNotNull(mucUser);
            assertAll(
                    () -> assertFalse(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Incorrect self-presence status code in kick presence"),
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.KICKED_307), "Missing kick status code in kick presence"),
                    () -> assertEquals(MUCRole.none, mucUser.getItem().getRole(), "Role other than 'none' in kick presence")
            );
            Jid itemJid = mucUser.getItem().getJid();
            if (itemJid != null) {
                assertEquals(conTwo.getUser().asEntityFullJidIfPossible(), itemJid, "Incorrect kicked user in kick presence");
            }
        } finally {
            tryDestroy(mucAsSeenByOne);
        }

    }

    @SmackIntegrationTest
    public void mucDestroyTest() throws TimeoutException, Exception {

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-join-leave");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        muc.join(Resourcepart.from("nick-one"));

        final SimpleResultSyncPoint mucDestroyed = new SimpleResultSyncPoint();

        @SuppressWarnings("deprecation")
        DefaultUserStatusListener userStatusListener = new DefaultUserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                mucDestroyed.signal();
            }
        };

        muc.addUserStatusListener(userStatusListener);

        assertEquals(1, mucManagerOne.getJoinedRooms().size());
        assertEquals(1, muc.getOccupantsCount());
        assertNotNull(muc.getNickname());

        try {
            muc.destroy("Dummy reason", null);
            mucDestroyed.waitForResult(timeout);
        } finally {
            muc.removeUserStatusListener(userStatusListener);
        }

        assertEquals(0, mucManagerOne.getJoinedRooms().size());
        assertEquals(0, muc.getOccupantsCount());
        assertNull(muc.getNickname());
    }

    /**
     * Gets a random room name
     *
     * @param prefix A prefix to add to the room name for descriptive purposes
     */
    private EntityBareJid getRandomRoom(String prefix) throws XmppStringprepException {
        final String roomNameLocal = String.join("-", prefix, testRunId, StringUtils.insecureRandomString(6));
        return JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucService.getDomain());
    }

    /**
     * Destroys a MUC room, ignoring any exceptions.
     *
     * @param muc The room to destroy (can be null).
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     */
    static void tryDestroy(final MultiUserChat muc) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (muc == null) {
            return;
        }
        muc.destroy("test fixture teardown", null);
    }

    private static void createMUC(MultiUserChat muc, String resourceName) throws NoResponseException, XMPPErrorException, InterruptedException, MultiUserChatException.MucAlreadyJoinedException, NotConnectedException, MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException {
        MucCreateConfigFormHandle handle = muc.create(Resourcepart.from(resourceName));
        if (handle != null) {
            handle.makeInstant();
        }
    }

}
