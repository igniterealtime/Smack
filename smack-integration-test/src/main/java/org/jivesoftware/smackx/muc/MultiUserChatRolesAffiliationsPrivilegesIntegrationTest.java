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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;


public class MultiUserChatRolesAffiliationsPrivilegesIntegrationTest extends AbstractMultiUserChatIntegrationTest{

    public MultiUserChatRolesAffiliationsPrivilegesIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, TestNotPossibleException {
        super(environment);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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
     * <p>From XEP-0045 § 9.7:</p>
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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
     * Asserts that a user who undergoes an affiliation change receives that change as a presence update
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

        createMuc(mucAsSeenByOne, "one-" + randomString);

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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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

    /**
     * Asserts that an affiliation is persistent between visits to the room.
     *
     * <p>From XEP-0045 § 5.2:</p>
     * <blockquote>
     * These affiliations are long-lived in that they persist across a user's visits to the room and are not affected
     * by happenings in the room...Affiliations are granted, revoked, and maintained based on the user's bare JID, not
     * the nick as with roles.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
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
            assertEquals(MUCAffiliation.owner, MUCUser.from(p2).getItem().getAffiliation());
            assertEquals(MUCAffiliation.admin, MUCUser.from(p3).getItem().getAffiliation());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator cannot revoke voice from an owner
     *
     * <p>From XEP-0045 § 5.1.1:</p>
     * <blockquote>
     * A moderator MUST NOT be able to revoke voice privileges from an admin or owner
     * </blockquote>
     *
     * <p>From XEP-0045 § 8.4:</p>
     * <blockquote>
     * A moderator MUST NOT be able to revoke voice from a user whose affiliation is at or above the moderator's level.
     * In addition, a service MUST NOT allow the voice privileges of an admin or owner to be removed by anyone. If a
     * moderator attempts to revoke voice privileges from such a user, the service MUST deny the request and return a
     * &lt;not-allowed/&gt; error to the sender along with the offending item(s)
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
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
                            () -> mucAsSeenByTwo.revokeVoice(nicknameOne));
            assertEquals(xe.getStanzaError().getCondition().toString(), "not-allowed");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderator cannot revoke moderator privileges from a moderator with a higher affiliation
     * than themselves.
     *
     * <p>From XEP-0045 § 5.1.3 and §5.2.1:</p>
     * <blockquote>
     * A moderator SHOULD NOT be allowed to revoke moderation privileges from someone with a higher affiliation than
     * themselves (i.e., an unaffiliated moderator SHOULD NOT be allowed to revoke moderation privileges from an admin
     * or an owner, and an admin SHOULD NOT be allowed to revoke moderation privileges from an owner)
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestModeratorCannotBeRevokedFromHigherAffiliation() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createModeratedMuc(mucAsSeenByOne, nicknameOne);

        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
        mucAsSeenByOne.grantModerator(nicknameThree);

        try {
            // Admin cannot revoke from Owner
            XMPPException.XMPPErrorException xe1 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByTwo.revokeModerator(nicknameOne));
            // Moderator cannot revoke from Admin
            XMPPException.XMPPErrorException xe2 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByThree.revokeModerator(nicknameOne));
            // Moderator cannot revoke from Owner
            XMPPException.XMPPErrorException xe3 = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> mucAsSeenByThree.revokeModerator(nicknameTwo));
            assertEquals(xe1.getStanzaError().getCondition().toString(), "not-allowed");
            assertEquals(xe2.getStanzaError().getCondition().toString(), "not-allowed");
            assertEquals(xe3.getStanzaError().getCondition().toString(), "not-allowed");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that an unmoderated room assigns the correct default roles for a given affiliation
     *
     * <p>From XEP-0045 § 5.1.2:</p>
     * <blockquote>
     * ...the initial default roles that a service SHOULD set based on the user's affiliation...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestDefaultRoleForAffiliationInUnmoderatedRoom() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-unmoderatedroles");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        mucAsSeenByTwo.join(nicknameTwo);
        mucAsSeenByThree.join(nicknameThree);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                resultSyncPoint.signal("done");
            }
        });
        mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
        resultSyncPoint.waitForResult(timeout);

        try {
            assertEquals(mucAsSeenByOne.getOccupantsCount(), 3);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameOne)).getRole());
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameTwo)).getRole());
            assertEquals(MUCRole.participant, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameThree)).getRole());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a moderated room assigns the correct default roles for a given affiliation
     *
     * <p>From XEP-0045 § 5.1.2:</p>
     * <blockquote>
     * ...the initial default roles that a service SHOULD set based on the user's affiliation...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestDefaultRoleForAffiliationInModeratedRoom() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-moderatedroles");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByThree = mucManagerThree.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);
        final Resourcepart nicknameThree = Resourcepart.from("three-" + randomString);

        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                resultSyncPoint.signal("done");
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

            assertEquals(mucAsSeenByOne.getOccupantsCount(), 3);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameOne)).getRole());
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameTwo)).getRole());
            assertEquals(threeRole, mucAsSeenByOne.getOccupant(
                            JidCreate.entityFullFrom(mucAddress, nicknameThree)).getRole());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a members-only room assigns the correct default roles for a given affiliation
     *
     * <p>From XEP-0045 § 5.1.2:</p>
     * <blockquote>
     * ...the initial default roles that a service SHOULD set based on the user's affiliation...
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
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

        final ResultSyncPoint<String, Exception> adminResultSyncPoint = new ResultSyncPoint<>();
        mucAsSeenByOne.addParticipantStatusListener(new ParticipantStatusListener() {
            @Override
            public void adminGranted(EntityFullJid participant) {
                adminResultSyncPoint.signal("done");
            }
        });

        try {
            mucAsSeenByOne.grantMembership(conTwo.getUser().asBareJid());
            mucAsSeenByOne.grantMembership(conThree.getUser().asBareJid());

            mucAsSeenByTwo.join(nicknameTwo);
            mucAsSeenByThree.join(nicknameThree);
            mucAsSeenByOne.grantAdmin(conTwo.getUser().asBareJid());
            adminResultSyncPoint.waitForResult(timeout);
            assertEquals(mucAsSeenByOne.getOccupantsCount(), 3);
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(jidOne).getRole());
            assertEquals(MUCRole.moderator, mucAsSeenByOne.getOccupant(jidTwo).getRole());
            assertEquals(MUCRole.participant, mucAsSeenByOne.getOccupant(jidThree).getRole());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

}
