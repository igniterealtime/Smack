/**
 *
 * Copyright 2021 Florian Schmaus
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

}
