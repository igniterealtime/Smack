/**
 *
 * Copyright 2015-2024 Florian Schmaus
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.muc.MultiUserChatException.MissingMucCreationAcknowledgeException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucAlreadyJoinedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucConfigurationNotSupportedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class MultiUserChatIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public MultiUserChatIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, TestNotPossibleException, MucAlreadyJoinedException, MissingMucCreationAcknowledgeException, NotAMucServiceException, XmppStringprepException {
        super(environment);
    }

    @SmackIntegrationTest
    public void mucTest() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-message");

        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final String mucMessage = "Smack Integration Test MUC Test Message " + randomString;
        final SimpleResultSyncPoint resultSyncPoint = new SimpleResultSyncPoint();

        mucAsSeenByTwo.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String body = message.getBody();
                if (mucMessage.equals(body)) {
                    resultSyncPoint.signal();
                }
            }
        });

        createMuc(mucAsSeenByOne, "one-" + randomString);
        mucAsSeenByTwo.join(Resourcepart.from("two-" + randomString));
        mucAsSeenByOne.sendMessage(mucMessage);

        try {
            assertResult(resultSyncPoint, "Expected " + conTwo.getUser() + " to receive message that was sent by " + conOne.getUser() + " in room " + mucAddress + " (but it did not).");
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }


    /**
     * Asserts that an owner is notified of room destruction when they destroy a room.
     *
     * @throws TimeoutException when roomDestroyed event doesn't get fired
     * @throws Exception when other errors occur
     */
    @SmackIntegrationTest(section = "10.9", quote =
        "A room owner MUST be able to destroy a room, especially if the room is persistent... The room removes all " +
        "users from the room... and destroys the room")
    public void mucDestroyOwnerTest() throws TimeoutException, Exception {

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-destroy-owner");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("one-" + randomString));

        // These would be a test implementation bug, not assertion failure.
        if (!mucManagerOne.getJoinedRooms().contains(mucAddress)) {
            tryDestroy(muc);
            throw new IllegalStateException("Expected user to have joined a room '" + mucAddress + "' (but does not appear to have done so).");
        }

        final SimpleResultSyncPoint mucDestroyed = new SimpleResultSyncPoint();

        UserStatusListener userStatusListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String password, String reason) {
                mucDestroyed.signal();
            }
        };

        muc.addUserStatusListener(userStatusListener);

        try {
            muc.destroy("Dummy reason", null);
            assertResult(mucDestroyed, "Expected " + conOne.getUser() + " to be notified of destruction of room " + mucAddress + " (but was not).");
        } finally {
            muc.removeUserStatusListener(userStatusListener);
        }

        Set<EntityBareJid> joinedRooms = mucManagerOne.getJoinedRooms();
        assertFalse(muc.isJoined(), "Expected " + conOne.getUser() + " to no longer be in room " + mucAddress + " after it was destroyed, but it is still in.");
        assertEquals(0, joinedRooms.size(), "Expected " + conOne.getUser() + " to no longer be in any rooms after " + mucAddress + " was destroyed. But it is still in " + joinedRooms);
        assertEquals(0, muc.getOccupantsCount(), "Expected room " + mucAddress + " to no longer have any occupants after it was destroyed (but it has).");
        assertNull(muc.getNickname());
    }

    /**
     * Asserts that an occupant of a room is notified when a room is destroyed.
     *
     * @throws TimeoutException when roomDestroyed event doesn't get fired
     * @throws Exception when other errors occur
     */
    @SmackIntegrationTest(section = "10.9", quote =
        "A room owner MUST be able to destroy a room, especially if the room is persistent... The room removes all " +
            "users from the room... and destroys the room")
    public void mucDestroyTestOccupant() throws TimeoutException, Exception {

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-destroy-occupant");

        MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        MultiUserChat mucAsSeenByParticipant = mucManagerTwo.getMultiUserChat(mucAddress);
        createMuc(mucAsSeenByOwner, Resourcepart.from("one-" + randomString));

        // These would be a test implementation bug, not assertion failure.
        mucAsSeenByParticipant.join(Resourcepart.from("two-" + randomString));
        if (!mucManagerTwo.getJoinedRooms().contains(mucAddress)) {
            tryDestroy(mucAsSeenByOwner);
            throw new IllegalStateException("Expected user to have joined a room '" + mucAddress + "' (but does not appear to have done so).");
        }


        final SimpleResultSyncPoint mucDestroyed = new SimpleResultSyncPoint();

        UserStatusListener userStatusListener = new UserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String password, String reason) {
                mucDestroyed.signal();
            }
        };

        mucAsSeenByParticipant.addUserStatusListener(userStatusListener);

        try {
            mucAsSeenByOwner.destroy("Dummy reason", null);
            assertResult(mucDestroyed, "Expected " + conTwo.getUser() + " to be notified of destruction of room " + mucAddress + " (but was not).");
        } finally {
            mucAsSeenByParticipant.removeUserStatusListener(userStatusListener);
        }

        Set<EntityBareJid> joinedRooms = mucManagerTwo.getJoinedRooms();
        assertFalse(mucAsSeenByParticipant.isJoined(), "Expected " + conTwo.getUser() + " to no longer be in room " + mucAddress + " after it was destroyed, but it is still in.");
        assertEquals(0, joinedRooms.size(), "Expected " + conTwo.getUser() + " to no longer be in any rooms after " + mucAddress + " was destroyed. But it is still in " + joinedRooms);
        assertEquals(0, mucAsSeenByParticipant.getOccupantsCount(), "Expected room " + mucAddress + " to no longer have any occupants after it was destroyed (but it has).");
        assertNull(mucAsSeenByParticipant.getNickname());
    }

    @SmackIntegrationTest
    public void mucNameChangeTest()
                    throws XmppStringprepException, MucAlreadyJoinedException, MissingMucCreationAcknowledgeException,
                    NotAMucServiceException, NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException, MucConfigurationNotSupportedException {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-muc-name-change");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("one-" + randomString));

        final String newRoomName = "New Room Name (" + randomString + ")";

        try {
            muc.getConfigFormManager()
                .setRoomName(newRoomName)
                .submitConfigurationForm();

            MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(conTwo);
            RoomInfo roomInfo = mucManager.getRoomInfo(muc.getRoom());
            assertEquals(newRoomName, roomInfo.getName());
        } finally {
            tryDestroy(muc);
        }
    }

    @SmackIntegrationTest(section = "8.1", quote = "modify the subject [...] MUST be denied if the <user@host> of the 'from' address of the request does not match "
                    + "the bare JID portion of one of the moderators; in this case, the service MUST return a <forbidden/> error.")
    public void mucTestVisitorNotAllowedToChangeSubject() throws XmppStringprepException, MucAlreadyJoinedException,
                    MissingMucCreationAcknowledgeException, NotAMucServiceException, NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-visitor-change-subject");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddress);

        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        final Resourcepart nicknameTwo = Resourcepart.from("two-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            MucConfigFormManager configFormManager = mucAsSeenByOne.getConfigFormManager();
            if (configFormManager.occupantsAreAllowedToChangeSubject()) {
                configFormManager.disallowOccupantsToChangeSubject().submitConfigurationForm();
            }

            mucAsSeenByTwo.join(nicknameTwo);

            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> {
                mucAsSeenByTwo.changeSubject("Test Subject Change");
            }, "Expected an error after '" + conTwo.getUser()
                            + "' (that is not a moderator) tried to change the subject of room '" + mucAddress
                            + "' (but none occurred).");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(),
                            "Unexpected error condition in the (expected) error that was returned to '"
                                            + conTwo.getUser() + "' after it tried to change to subject of room '"
                                            + mucAddress + "' while not being a moderator.");
        } catch (MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    @SmackIntegrationTest
    public void mucTestChangeRoomName() throws XmppStringprepException, MucAlreadyJoinedException,
                    MissingMucCreationAcknowledgeException, NotAMucServiceException, NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException {
        final EntityBareJid mucAddress = getRandomRoom("smack-inttest-change-room-name");
        final MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);

        createMuc(mucAsSeenByOne, nicknameOne);
        try {
            String initialRoomName = "Initial Room Name";
            mucAsSeenByOne.getConfigFormManager().setRoomName(initialRoomName).submitConfigurationForm();
            RoomInfo roomInfo = mucManagerOne.getRoomInfo(mucAddress);
            assertEquals(initialRoomName, roomInfo.getName());

            String newRoomName = "New Room Name";
            mucAsSeenByOne.getConfigFormManager().setRoomName(newRoomName).submitConfigurationForm();
            roomInfo = mucManagerOne.getRoomInfo(mucAddress);
            assertEquals(newRoomName, roomInfo.getName());
        } catch (MucConfigurationNotSupportedException e) {
            throw new TestNotPossibleException(e);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }
}
