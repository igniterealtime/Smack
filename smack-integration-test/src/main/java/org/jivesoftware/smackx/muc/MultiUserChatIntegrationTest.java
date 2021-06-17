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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;

@SpecificationReference(document = "XEP-0045")
public class MultiUserChatIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public MultiUserChatIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, TestNotPossibleException {
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
     * Asserts that a user is notified when a room is destroyed.
     *
     * @throws TimeoutException when roomDestroyed event doesn't get fired
     * @throws Exception when other errors occur
     */
    @SmackIntegrationTest(section = "10.9", quote =
        "A room owner MUST be able to destroy a room, especially if the room is persistent... The room removes all " +
        "users from the room... and destroys the room")
    public void mucDestroyTest() throws TimeoutException, Exception {

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-destroy");

        MultiUserChat muc = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(muc, Resourcepart.from("one-" + randomString));

        final SimpleResultSyncPoint mucDestroyed = new SimpleResultSyncPoint();

        @SuppressWarnings("deprecation")
        DefaultUserStatusListener userStatusListener = new DefaultUserStatusListener() {
            @Override
            public void roomDestroyed(MultiUserChat alternateMUC, String reason) {
                mucDestroyed.signal();
            }
        };

        muc.addUserStatusListener(userStatusListener);

        // These would be a test implementation bug, not assertion failure.
        if (mucManagerOne.getJoinedRooms().stream().noneMatch(room -> room.equals(mucAddress))) {
            throw new IllegalStateException("Expected user to have joined a room '" + mucAddress + "' (but does not appear to have done so).");
        }

        try {
            muc.destroy("Dummy reason", null);
            assertResult(mucDestroyed, "Expected " + conOne.getUser() + " to be notified of destruction of room " + mucAddress + " (but was not).");
        } finally {
            muc.removeUserStatusListener(userStatusListener);
        }

        assertEquals(0, mucManagerOne.getJoinedRooms().size(), "Expected " + conOne.getUser() + " to no longer be in any rooms after " + mucAddress + " was destroyed (but was).");
        assertEquals(0, muc.getOccupantsCount(), "Expected room " + mucAddress + " to no longer have any occupants after it was destroyed (but it has).");
        assertNull(muc.getNickname());
    }
}
