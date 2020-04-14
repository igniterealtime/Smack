/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.MessageBuilder;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class SessionRenegotiationIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public SessionRenegotiationIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @SmackIntegrationTest
    public void sessionRenegotiationTest() throws Exception {

        boolean prevRepairProperty = OmemoConfiguration.getRepairBrokenSessionsWithPreKeyMessages();
        OmemoConfiguration.setRepairBrokenSessionsWithPrekeyMessages(true);
        boolean prevCompleteSessionProperty = OmemoConfiguration.getCompleteSessionWithEmptyMessage();
        OmemoConfiguration.setCompleteSessionWithEmptyMessage(false);

        // send PreKeyMessage -> Success
        final String body1 = "P = NP is true for all N,P from the set of complex numbers, where P is equal to 0";
        AbstractOmemoMessageListener.PreKeyMessageListener listener1 =
                new AbstractOmemoMessageListener.PreKeyMessageListener(body1);
        OmemoMessage.Sent e1 = alice.encrypt(bob.getOwnJid(), body1);
        bob.addOmemoMessageListener(listener1);

        XMPPConnection alicesConnection = alice.getConnection();
        MessageBuilder messageBuilder = alicesConnection.getStanzaFactory().buildMessageStanza();
        alicesConnection.sendStanza(e1.buildMessage(messageBuilder, bob.getOwnJid()));
        listener1.getSyncPoint().waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(listener1);

        // Remove the session on Bobs side.
        synchronized (bob) {
            bob.getOmemoService().getOmemoStoreBackend().removeRawSession(bob.getOwnDevice(), alice.getOwnDevice());
        }

        // Send normal message -> fail, bob repairs session with preKeyMessage
        final String body2 = "P = NP is also true for all N,P from the set of complex numbers, where N is equal to 1.";
        AbstractOmemoMessageListener.PreKeyKeyTransportListener listener2 =
                new AbstractOmemoMessageListener.PreKeyKeyTransportListener();
        OmemoMessage.Sent e2 = alice.encrypt(bob.getOwnJid(), body2);
        alice.addOmemoMessageListener(listener2);

        messageBuilder = alicesConnection.getStanzaFactory().buildMessageStanza();
        alicesConnection.sendStanza(e2.buildMessage(messageBuilder, bob.getOwnJid()));
        listener2.getSyncPoint().waitForResult(10 * 1000);
        alice.removeOmemoMessageListener(listener2);

        // Send normal message -> success
        final String body3 = "P = NP would be a disaster for the world of cryptography.";
        AbstractOmemoMessageListener.MessageListener listener3 = new AbstractOmemoMessageListener.MessageListener(body3);
        OmemoMessage.Sent e3 = alice.encrypt(bob.getOwnJid(), body3);
        bob.addOmemoMessageListener(listener3);

        messageBuilder = alicesConnection.getStanzaFactory().buildMessageStanza();
        alicesConnection.sendStanza(e3.buildMessage(messageBuilder, bob.getOwnJid()));
        listener3.getSyncPoint().waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(listener3);

        OmemoConfiguration.setRepairBrokenSessionsWithPrekeyMessages(prevRepairProperty);
        OmemoConfiguration.setCompleteSessionWithEmptyMessage(prevCompleteSessionProperty);
    }
}
