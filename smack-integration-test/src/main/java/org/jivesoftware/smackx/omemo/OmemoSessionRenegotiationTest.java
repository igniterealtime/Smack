/**
 *
 * Copyright 2017 Florian Schmaus, Paul Schaub
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.subscribe;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.unidirectionalTrust;

import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

/**
 * Test session renegotiation.
 */
public class OmemoSessionRenegotiationTest extends AbstractOmemoIntegrationTest {

    private OmemoManager alice, bob;
    private OmemoStore<?,?,?,?,?,?,?,?,?> store;

    public OmemoSessionRenegotiationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        super(environment);
    }

    @Override
    public void before() {
        alice = OmemoManager.getInstanceFor(conOne, 1337);
        bob = OmemoManager.getInstanceFor(conTwo, 1009);
        store = OmemoService.getInstance().getOmemoStoreBackend();
    }

    @SmackIntegrationTest
    public void sessionRenegotiationTest() throws Exception {

        final boolean[] phaseTwo = new boolean[1];
        final SimpleResultSyncPoint sp1 = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint sp2 = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint sp3 = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint sp4 = new SimpleResultSyncPoint();

        final String m1 = "1: Alice says hello to bob.";
        final String m2 = "2: Bob replies to Alice.";
        final String m3 = "3. This message will arrive but Bob cannot decrypt it.";
        final String m4 = "4. This message is readable by Bob again.";

        subscribe(alice, bob, "Bob");
        subscribe(bob, alice, "Alice");

        setUpOmemoManager(alice);
        setUpOmemoManager(bob);

        unidirectionalTrust(alice, bob);
        unidirectionalTrust(bob, alice);

        OmemoMessageListener first = new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Bob received OMEMO message: " + decryptedBody);
                assertEquals("Received message MUST match the one we sent.", decryptedBody, m1);
                sp1.signal();
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }
        };
        bob.addOmemoMessageListener(first);

        ChatManager.getInstanceFor(alice.getConnection()).chatWith(bob.getOwnJid().asEntityBareJidIfPossible())
                .send(alice.encrypt(bob.getOwnJid(), m1));

        sp1.waitForResult(10 * 1000);

        bob.removeOmemoMessageListener(first);

        OmemoMessageListener second = new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Alice received OMEMO message: " + decryptedBody);
                assertEquals("Reply must match the message we sent.", decryptedBody, m2);
                sp2.signal();
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }
        };
        alice.addOmemoMessageListener(second);

        ChatManager.getInstanceFor(bob.getConnection()).chatWith(alice.getOwnJid().asEntityBareJidIfPossible())
                .send(bob.encrypt(alice.getOwnJid(), m2));

        sp2.waitForResult(10 * 1000);

        alice.removeOmemoMessageListener(second);

        store.forgetOmemoSessions(bob);
        store.removeAllRawSessionsOf(bob, alice.getOwnJid());

        OmemoMessageListener third = new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                fail("Bob should not have received a decipherable message: " + decryptedBody);
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }
        };
        bob.addOmemoMessageListener(third);

        OmemoMessageListener fourth = new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Alice received preKeyMessage.");
                sp3.signal();
            }
        };
        alice.addOmemoMessageListener(fourth);

        ChatManager.getInstanceFor(alice.getConnection()).chatWith(bob.getOwnJid().asEntityBareJidIfPossible())
                .send(alice.encrypt(bob.getOwnJid(), m3));

        sp3.waitForResult(10 * 1000);

        bob.removeOmemoMessageListener(third);
        alice.removeOmemoMessageListener(fourth);

        OmemoMessageListener fifth = new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Bob received an OMEMO message: " + decryptedBody);
                assertEquals("The received message must match the one we sent.",
                        decryptedBody, m4);
                sp4.signal();
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }
        };
        bob.addOmemoMessageListener(fifth);

        ChatManager.getInstanceFor(alice.getConnection()).chatWith(bob.getOwnJid().asEntityBareJidIfPossible())
                .send(alice.encrypt(bob.getOwnJid(), m4));

        sp4.waitForResult(10 * 1000);
    }

    @Override
    public void after() {
        alice.shutdown();
        bob.shutdown();
        cleanServerSideTraces(alice);
        cleanServerSideTraces(bob);
    }
}
