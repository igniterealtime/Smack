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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.MessageBuilder;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * Simple OMEMO message encryption integration test.
 * During this test Alice sends an encrypted message to Bob. Bob decrypts it and sends a response to Alice.
 * It is checked whether the messages can be decrypted, and if used up pre-keys result in renewed bundles.
 */
public class MessageEncryptionIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public MessageEncryptionIntegrationTest(SmackIntegrationTestEnvironment<?> environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    /**
     * This test checks whether the following actions are performed.
     *
     * Alice publishes bundle A1
     * Bob publishes bundle B1
     *
     * Alice sends message to Bob (preKeyMessage)
     * Bob publishes bundle B2
     * Alice still has A1
     *
     * Bob responds to Alice (normal message)
     * Alice still has A1
     * Bob still has B2
     * @throws Exception if an exception occurs.
     */
    @SuppressWarnings("SynchronizeOnNonFinalField")
    @SmackIntegrationTest
    public void messageTest() throws Exception {
        OmemoBundleElement a1 = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement b1 = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());

        // Alice sends message(s) to bob
        // PreKeyMessage A -> B
        final String body1 = "One is greater than zero (for small values of zero).";
        AbstractOmemoMessageListener.PreKeyMessageListener listener1 =
                new AbstractOmemoMessageListener.PreKeyMessageListener(body1);
        bob.addOmemoMessageListener(listener1);
        OmemoMessage.Sent e1 = alice.encrypt(bob.getOwnJid(), body1);

        XMPPConnection alicesConnection = alice.getConnection();
        MessageBuilder messageBuilder = alicesConnection.getStanzaFactory().buildMessageStanza();
        alicesConnection.sendStanza(e1.buildMessage(messageBuilder, bob.getOwnJid()));
        listener1.getSyncPoint().waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(listener1);

        OmemoBundleElement a1_ = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement b2;

        synchronized (bob) { // Circumvent race condition where bundle gets replenished after getting stored in b2
            b2 = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());
        }

        assertEquals("Alice sent bob a preKeyMessage, so her bundle MUST still be the same.", a1, a1_);
        assertNotEquals("Bob just received a preKeyMessage from alice, so his bundle must have changed.", b1, b2);

        // Message B -> A
        final String body3 = "The german words for 'leek' and 'wimp' are the same.";
        AbstractOmemoMessageListener.MessageListener listener3 =
                new AbstractOmemoMessageListener.MessageListener(body3);
        alice.addOmemoMessageListener(listener3);
        OmemoMessage.Sent e3 = bob.encrypt(alice.getOwnJid(), body3);
        XMPPConnection bobsConnection = bob.getConnection();
        messageBuilder = bobsConnection.getStanzaFactory().buildMessageStanza();
        bobsConnection.sendStanza(e3.buildMessage(messageBuilder, alice.getOwnJid()));
        listener3.getSyncPoint().waitForResult(10 * 1000);
        alice.removeOmemoMessageListener(listener3);

        OmemoBundleElement a1__ = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement b2_ = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());

        assertEquals("Since alice initiated the session with bob, at no time he sent a preKeyMessage, " +
                "so her bundle MUST still be the same.", a1_, a1__);
        assertEquals("Bob changed his bundle earlier, but at this point his bundle must be equal to " +
                "after the first change.", b2, b2_);
    }
}
