/**
 *
 * Copyright 2021 Florian Schmaus, Dan Caseley
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;


@SpecificationReference(document = "XEP-0045")
public class MultiUserChatRolesAffiliationsPrivilegesIntegrationTest extends AbstractMultiUserChatIntegrationTest{

    public MultiUserChatRolesAffiliationsPrivilegesIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, TestNotPossibleException {
        super(environment);
    }

    /**
     * Asserts that a user who undergoes a role change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.3", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 9.6) The service MUST then send updated presence from this " +
        "individual to all occupants, indicating the addition of moderator status...")
    public void mucRoleTestForReceivingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void moderatorGranted() {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            // This implicitly tests "The service MUST add the user to the moderator list and then inform the admin of
            // success" in §9.6, since it'll throw on either an error IQ or on no response.
            mucAsSeenByOne.grantModerator(nicknameTwo);

            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to get a presence update after it was granted the role 'moderator' role in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "9.6", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 9.6) The service MUST then send updated presence from this " +
        "individual to all occupants, indicating the addition of moderator status...")
    public void mucRoleTestForWitnessingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void moderatorGranted(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.grantModerator(nicknameTwo);

            assertResult(resultSyncPoint, "Expected " + conThree.getUser() + " to get a presence update after another user in the room was granted the 'moderator' role in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }

    }

    /**
     * Asserts that a user who undergoes a role change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.3", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 9.7) The service MUST then send updated presence from this " +
        "individual to all occupants, indicating the removal of moderator status...")
    public void mucRoleTestForRemovingModerator() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void moderatorRevoked() {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            mucAsSeenByOne.grantModerator(nicknameTwo);
            mucAsSeenByOne.revokeModerator(nicknameTwo);

            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to get a presence update after its 'moderator' role in " + mucAddress + " was revoked (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "9.7", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 9.7) The service MUST then send updated presence from this " +
        "individual to all occupants, indicating the removal of moderator status...")
    public void mucRoleTestForWitnessingModeratorRemoval() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void moderatorRevoked(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.grantModerator(nicknameTwo);
            mucAsSeenByOne.revokeModerator(nicknameTwo);
            assertResult(resultSyncPoint, "Expected " + conThree.getUser() + " to get a presence update after the 'moderator' role of another user in the room was revoked in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user in an unmoderated room who undergoes an afilliation change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.3", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 8.4) The service MUST then send updated presence from " +
        "this individual to all occupants, indicating the removal of voice privileges...")
    public void mucRoleTestForRevokingVoice() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void voiceRevoked() {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByOne.revokeVoice(nicknameTwo);
            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to get a presence update after its 'voice' privilege was revoked in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes a role change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "8.4", quote =
        "(§ 5.1.3)... a MUC service implementation MUST change the occupant's role to reflect the change and " +
        "communicate the change to all occupants [...] (§ 8.4) The service MUST then send updated presence from " +
        "this individual to all occupants, indicating the removal of voice privileges...")
    public void mucRoleTestForWitnessingRevokingVoice() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void voiceRevoked(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.revokeVoice(nicknameTwo);
            assertResult(resultSyncPoint, "Expected " + conThree.getUser() + " to get a presence update after another user's 'voice' privilege was revoked in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who undergoes an affiliation change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.2.2", quote =
        "(§ 5.2.2) ... a MUC service implementation MUST change the user's affiliation to reflect the change and " +
        "communicate that to all occupants [...] (§ 10.6) If the user is in the room, the service MUST then send " +
        "updated presence from this individual to all occupants, indicating the granting of admin status...")
    public void mucAffiliationTestForReceivingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();


        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void adminGranted() {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            // This implicitly tests "The service MUST add the user to the admin list and then inform the owner of success" in §10.6, since it'll throw on either an error IQ or on no response.
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to get a presence update after its was granted 'admin' affiliation in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes an affiliation change receives that change as a
     * presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "10.6", quote =
        "(§ 5.2.2) ... a MUC service implementation MUST change the user's affiliation to reflect the change and " +
        "communicate that to all occupants [...] (§ 10.6) If the user is in the room, the service MUST then send " +
        "updated presence from this individual to all occupants, indicating the granting of admin status...")
    public void mucAffiliationTestForWitnessingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            assertResult(resultSyncPoint, "Expected " + conThree.getUser() + " to get a presence update after another user was granted 'admin' affiliation in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who undergoes an affiliation change receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "10.7", quote =
        "(§ 5.2.2) ... a MUC service implementation MUST change the user's affiliation to reflect the change and " +
        "communicate that to all occupants [...] (§ 10.6) If the user is in the room, the service MUST then send " +
        "updated presence from this individual to all occupants, indicating the loss of admin status by sending a " +
        "presence element...")
    public void mucAffiliationTestForRemovingAdmin() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByTwo.addUserStatusListener(new UserStatusListener() {
            @Override
            public void adminRevoked() {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByOne.revokeAdmin(conTwo.getUser().asEntityBareJid());
            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to get a presence update after its 'admin' affiliation was revoked in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user undergoes an affiliation change receives that change as a presence update
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
    @SmackIntegrationTest(section = "10.7", quote =
        "(§ 5.2.2) ... a MUC service implementation MUST change the user's affiliation to reflect the change and " +
        "communicate that to all occupants [...] (§ 10.6) If the user is in the room, the service MUST then send " +
        "updated presence from this individual to all occupants, indicating the loss of admin status by sending a " +
        "presence element...")
    public void mucAffiliationTestForWitnessingAdminRemoval() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByThree.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminRevoked(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());

            mucAsSeenByOne.revokeAdmin(conTwo.getUser().asEntityBareJid());
            assertResult(resultSyncPoint, "Expected " + conThree.getUser() + " to get a presence update after another user's 'admin' affiliation was revoked in " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who gets kicked receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "The kick is performed based on the occupant's room nickname and is completed by setting the role of a " +
        "participant or visitor to a value of \"none\". The service MUST remove the kicked occupant by sending a " +
        "presence stanza of type \"unavailable\" to each kicked occupant, including status code 307 in the extended " +
        "presence information, optionally along with the reason (if provided) and the roomnick or bare JID of the " +
        "user who initiated the kick.")
    public void mucPresenceTestForGettingKicked() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);

            final ResultSyncPoint<Presence, Exception> resultSyncPoint = new ResultSyncPoint<>();
            mucAsSeenByTwo.addParticipantListener(kickPresence -> resultSyncPoint.signal(kickPresence));

            mucAsSeenByOne.kickParticipant(nicknameTwo, "Nothing personal. Just a test.");
            Presence kickPresence = resultSyncPoint.waitForResult(timeout);
            MUCUser mucUser = MUCUser.from(kickPresence);
            assertNotNull(mucUser, "Expected, but unable, to create a MUCUser instance from 'kick' presence: " + kickPresence);
            assertAll(
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Missing self-presence status code in kick presence received by " + conTwo.getUser() + " after being kicked from room " + mucAddress),
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.KICKED_307), "Missing kick status code in kick presence received by " + conTwo.getUser() + " after being kicked from room " + mucAddress),
                    () -> assertEquals(MUCRole.none, mucUser.getItem().getRole(), "Role other than 'none' in kick presence received by " + conTwo.getUser() + " after being kicked from room " + mucAddress)
            );
            Jid itemJid = mucUser.getItem().getJid();
            if (itemJid != null) {
                assertEquals(conTwo.getUser().asEntityFullJidIfPossible(), itemJid, "Incorrect kicked user in kick presence received by " + conTwo.getUser() + " after being kicked from room " + mucAddress);
            }
        } catch (TimeoutException e) {
            fail("Expected " + conTwo.getUser() + " to receive a presence update after it was kicked from room " + mucAddress + " (but it did not).", e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a user who is present when another user gets kicked receives that change as a presence update.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "8.2", quote =
        "...the service MUST then inform all of the remaining occupants that the kicked occupant is no longer in the " +
        "room by sending presence stanzas of type \"unavailable\" from the individual's roomnick " +
        "(<room@service/nick>) to all the remaining occupants (just as it does when occupants exit the room of their " +
        "own volition), including the status code and optionally the reason and actor.")
    public void mucPresenceTestForWitnessingKick() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        createMuc(mucAsSeenByOne, "one-" + randomString);
        try {
            final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
            final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            final ResultSyncPoint<Presence, Exception> resultSyncPoint = new ResultSyncPoint<>();
            mucAsSeenByThree.addParticipantListener(kickPresence -> resultSyncPoint.signal(kickPresence));

            mucAsSeenByOne.kickParticipant(nicknameTwo, "Nothing personal. Just a test.");
            Presence kickPresence = resultSyncPoint.waitForResult(timeout);
            MUCUser mucUser = MUCUser.from(kickPresence);
            assertNotNull(mucUser, "Expected, but unable, to create a MUCUser instance from 'kick' presence: " + kickPresence);
            assertAll(
                    () -> assertFalse(mucUser.getStatus().contains(MUCUser.Status.PRESENCE_TO_SELF_110), "Incorrect self-presence status code in kick presence received by " + conThree.getUser() + " after another user was kicked from room " + mucAddress),
                    () -> assertTrue(mucUser.getStatus().contains(MUCUser.Status.KICKED_307), "Missing kick status code in kick presence received by " + conThree.getUser() + " after another user was kicked from room " + mucAddress),
                    () -> assertEquals(MUCRole.none, mucUser.getItem().getRole(), "Role other than 'none' in kick presence received by " + conThree.getUser() + " after another user was kicked from room " + mucAddress)
            );
            Jid itemJid = mucUser.getItem().getJid();
            if (itemJid != null) {
                assertEquals(conTwo.getUser().asEntityFullJidIfPossible(), itemJid, "Incorrect kicked user in kick presence received by " + conThree.getUser() + " after another user was kicked from room " + mucAddress);
            }
        } catch (TimeoutException e) {
            fail("Expected " + conThree.getUser() + " to receive a presence update after another user was kicked from room " + mucAddress + " (but it did not).", e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }

    }

    /**
     * Asserts that an affiliation is persistent between visits to the room.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.2", quote =
        "These affiliations are long-lived in that they persist across a user's visits to the room and are not " +
        "affected by happenings in the room...Affiliations are granted, revoked, and maintained based on the user's " +
        "bare JID, not the nick as with roles.")
    public void mucTestPersistentAffiliation() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.grantOwnership(conTwo.getUser().asBareJid());
            mucAsSeenByOne.grantAdmin(conThree.getUser().asBareJid());

            mucAsSeenByTwo.leave();
            mucAsSeenByThree.leave();
            Presence p2 = mucAsSeenByTwo.join(nicknameTwo);
            Presence p3 = mucAsSeenByThree.join(nicknameThree);
            assertEquals(MUCAffiliation.owner, MUCUser.from(p2).getItem().getAffiliation(), "Unexpected affiliation of " + conTwo.getUser() + " after it re-joined room " + mucAddress);
            assertEquals(MUCAffiliation.admin, MUCUser.from(p3).getItem().getAffiliation(), "Unexpected affiliation of " + conThree.getUser() + " after it re-joined room " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator cannot revoke voice from an owner.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.1", quote =
        "A moderator MUST NOT be able to revoke voice privileges from an admin or owner [...] (§ 8.4) A moderator " +
        "MUST NOT be able to revoke voice from a user whose affiliation is at or above the moderator's level. In " +
        "addition, a service MUST NOT allow the voice privileges of an admin or owner to be removed by anyone. If a " +
        "moderator attempts to revoke voice privileges from such a user, the service MUST deny the request and return " +
        "a <not-allowed/> error to the sender along with the offending item(s)")
    public void mucTestModeratorCannotRevokeVoiceFromOwner() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createModeratedMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByOne.grantModerator(nicknameTwo);
            XMPPException.XMPPErrorException xe = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByTwo.revokeVoice(nicknameOne),
                    "Expected an XMPP error when " + conTwo.getUser() + " was trying to revoke the 'voice' privilege of " + conOne.getUser() + " in room " + mucAddress);
            assertEquals("not-allowed", xe.getStanzaError().getCondition().toString(), "Unexpected stanza error condition in error returned when " + conTwo.getUser() + " was trying to revoke the 'voice' privilege of " + conOne.getUser() + " in room " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator cannot revoke moderator privileges from a moderator with a higher affiliation
     * than themselves.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.3", quote =
        "A moderator SHOULD NOT be allowed to revoke moderation privileges from someone with a higher affiliation " +
        "than themselves (i.e., an unaffiliated moderator SHOULD NOT be allowed to revoke moderation privileges from " +
        "an admin or an owner, and an admin SHOULD NOT be allowed to revoke moderation privileges from an owner)")
    public void mucTestModeratorCannotBeRevokedFromHigherAffiliation() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createModeratedMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            mucAsSeenByOne.grantModerator(nicknameThree);

            // Admin cannot revoke from Owner
            XMPPException.XMPPErrorException xe1 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByTwo.revokeModerator(nicknameOne),
                    "Expected an XMPP error when " + conTwo.getUser() + " (an admin) was trying to revoke the 'moderator' role of " + conOne.getUser() + " (an owner) in room " + mucAddress);
            // Moderator cannot revoke from Owner
            XMPPException.XMPPErrorException xe2 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByThree.revokeModerator(nicknameOne),
                    "Expected an XMPP error when " + conThree.getUser() + " (a moderator) was trying to revoke the 'moderator' role of " + conOne.getUser() + " (an owner) in room " + mucAddress);
            // Moderator cannot revoke from Admin
            XMPPException.XMPPErrorException xe3 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByThree.revokeModerator(nicknameTwo),
                "Expected an XMPP error when " + conThree.getUser() + " (a moderator) was trying to revoke the 'moderator' role of " + conTwo.getUser() + " (an admin) in room " + mucAddress);
            assertEquals("not-allowed", xe1.getStanzaError().getCondition().toString(), "Unexpected condition in XMPP error when " + conTwo.getUser() + " (an admin) was trying to revoke the 'moderator' role of " + conOne.getUser() + " (an owner) in room " + mucAddress);
            assertEquals("not-allowed", xe2.getStanzaError().getCondition().toString(), "Unexpected condition in XMPP error when " + conThree.getUser() + " (a moderator) was trying to revoke the 'moderator' role of " + conOne.getUser() + " (an owner) in room " + mucAddress);
            assertEquals("not-allowed", xe3.getStanzaError().getCondition().toString(), "Unexpected condition in XMPP error when " + conThree.getUser() + " (a moderator) was trying to revoke the 'moderator' role of " + conTwo.getUser() + " (an admin) in room " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that an unmoderated room assigns the correct default roles for a given affiliation.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.2", quote =
        "...the initial default roles that a service SHOULD set based on the user's affiliation...")
    public void mucTestDefaultRoleForAffiliationInUnmoderatedRoom() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-unmoderatedroles");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);

            final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();
            mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void adminGranted(EntityFullJid participant) {
                    resultSyncPoint.signal();
                }
            });
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            resultSyncPoint.waitForResult(timeout);

            assertEquals(3, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameOne)).getRole(),
                "Unexpected role for occupant " + nicknameOne + " of " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameTwo)).getRole(),
                        "Unexpected role for occupant " + nicknameTwo + " of " + mucAddress);
            assertEquals(MUCRole.participant, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)).getRole(),
                "Unexpected role for occupant " + nicknameThree + " of " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderated room assigns the correct default roles for a given affiliation.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.2", quote =
        "...the initial default roles that a service SHOULD set based on the user's affiliation...")
    public void mucTestDefaultRoleForAffiliationInModeratedRoom() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderatedroles");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();
        mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                resultSyncPoint.signal();
            }
        });

        createModeratedMuc(mucAsSeenByOne, nicknameOne);

        final MUCRole threeRole;
        switch (sinttestConfiguration.compatibilityMode) {
        default:
            threeRole = MUCRole.visitor;
            break;
        case ejabberd:
            threeRole = MUCRole.participant;
            break;
        }

        try {
            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            resultSyncPoint.waitForResult(timeout);

            assertEquals(3, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameOne)).getRole(),
                "Unexpected role for occupant " + nicknameOne + " of " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameTwo)).getRole(),
                "Unexpected role for occupant " + nicknameTwo + " of " + mucAddress);
            assertEquals(threeRole, mucAsSeenByOne.getOccupant(JidCreate.entityFullFrom(mucAddress, nicknameThree)).getRole(),
                "Unexpected role for occupant " + nicknameThree + " of " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a members-only room assigns the correct default roles for a given affiliation.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "5.1.2", quote =
        "...the initial default roles that a service SHOULD set based on the user's affiliation...")
    public void mucTestDefaultRoleForAffiliationInMembersOnlyRoom() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-membersonlyroles");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        final EntityFullJid jidOne = JidCreate.entityFullFrom(mucAddress, nicknameOne);
        final EntityFullJid jidTwo = JidCreate.entityFullFrom(mucAddress, nicknameTwo);
        final EntityFullJid jidThree = JidCreate.entityFullFrom(mucAddress, nicknameThree);

        createMembersOnlyMuc(mucAsSeenByOne, nicknameOne);

        final SimpleResultSyncPoint adminResultSyncPoint = new SimpleResultSyncPoint();
        mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                adminResultSyncPoint.signal();
            }
        });

        try {
            mucAsSeenByOne.grantMembership(conTwo.getUser().asBareJid());
            mucAsSeenByOne.grantMembership(conThree.getUser().asBareJid());

            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            adminResultSyncPoint.waitForResult(timeout);
            assertEquals(3, mucAsSeenByOne.getOccupantsCount(), "Unexpected occupant count in room " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(jidOne).getRole(), "Unexpected role for occupant " + jidOne + " in room " + mucAddress);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(jidTwo).getRole(), "Unexpected role for occupant " + jidTwo + " in room " + mucAddress);
            assertEquals(MUCRole.participant, mucAsSeenByOne.getOccupant(jidThree).getRole(), "Unexpected role for occupant " + jidThree + " in room " + mucAddress);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

}
