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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;

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

        createMuc(mucAsSeenByOne, "one-" + randomString);
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
     * Asserts that a user is notified when a room is destroyed
     *
     * <p>From XEP-0045 § 10.9:</p>
     * <blockquote>
     * A room owner MUST be able to destroy a room, especially if the room is persistent... The room removes all users from the room... and destroys the room
     * </blockquote>
     *
     * @throws TimeoutException when roomDestroyed event doesn't get fired
     * @throws Exception when other errors occur
     */
     @SmackIntegrationTest
    public void mucDestroyTest() throws TimeoutException, Exception {

        EntityBareJid mucAddress = getRandomRoom("smack-inttest-destroy");

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
}
