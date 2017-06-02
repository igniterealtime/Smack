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

import junit.framework.TestCase;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.pubsub.PubSubException;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.subscribe;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.unidirectionalTrust;

/**
 * Test message sending.
 */
public class OmemoMessageSendingTest extends AbstractOmemoIntegrationTest {

    private OmemoManager alice, bob;
    private OmemoStore<?,?,?,?,?,?,?,?,?> store;

    public OmemoMessageSendingTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        super(environment);
    }

    @Override
    public void before() {
        alice = OmemoManager.getInstanceFor(conOne, 123);
        bob = OmemoManager.getInstanceFor(conTwo, 345);
        store = OmemoService.getInstance().getOmemoStoreBackend();
    }

    /**
     * This Test tests sending and receiving messages.
     * Alice and Bob create fresh devices, then they add another to their rosters.
     * Next they build sessions with one another and Alice sends a message to Bob.
     * After receiving and successfully decrypting the message, its tested, if Bob
     * publishes a new Bundle. After that Bob replies to the message and its tested,
     * whether Alice can decrypt the message and if she does NOT publish a new Bundle.
     *
     * @throws CorruptedOmemoKeyException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotLoggedInException
     * @throws PubSubException.NotALeafNodeException
     * @throws CannotEstablishOmemoSessionException
     * @throws UndecidedOmemoIdentityException
     * @throws NoSuchAlgorithmException
     * @throws CryptoFailedException
     */
    @SmackIntegrationTest
    public void messageSendingTest() throws CorruptedOmemoKeyException, InterruptedException, SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, PubSubException.NotALeafNodeException, CannotEstablishOmemoSessionException, UndecidedOmemoIdentityException, NoSuchAlgorithmException, CryptoFailedException {
        final String alicesSecret = "Hey Bob! I love you!";
        final String bobsSecret = "I love you too, Alice."; //aww <3

        final SimpleResultSyncPoint messageOneSyncPoint = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint messageTwoSyncPoint = new SimpleResultSyncPoint();

        //Subscribe to one another
        subscribe(alice, bob, "Bob");
        subscribe(bob, alice,"Alice");

        //initialize OmemoManagers
        setUpOmemoManager(alice);
        setUpOmemoManager(bob);

        //Save initial bundles
        OmemoBundleElement aliceBundle = store.packOmemoBundle(alice);
        OmemoBundleElement bobsBundle = store.packOmemoBundle(bob);

        //Trust
        unidirectionalTrust(alice, bob);
        unidirectionalTrust(bob, alice);

        //Register messageListeners
        bob.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO,"Bob received message: " + decryptedBody);
                if (decryptedBody.trim().equals(alicesSecret.trim())) {
                    messageOneSyncPoint.signal();
                } else {
                    messageOneSyncPoint.signal(new Exception("Received message must equal sent message."));
                }
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
            }
        });

        alice.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Alice received message: " + decryptedBody);
                if (decryptedBody.trim().equals(bobsSecret.trim())) {
                    messageTwoSyncPoint.signal();
                } else {
                    messageTwoSyncPoint.signal(new Exception("Received message must equal sent message."));
                }
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {

            }
        });

        //Prepare Alice message for Bob
        Message encryptedA = alice.encrypt(bob.getOwnJid(), alicesSecret);
        ChatManager.getInstanceFor(alice.getConnection()).chatWith(bob.getOwnJid().asEntityBareJidIfPossible())
                .send(encryptedA);

        try {
            messageOneSyncPoint.waitForResult(10 * 1000);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while waiting for message: " + e, e);
            TestCase.fail("Bob must have received Alice message.");
        }

        //Check if Bob published a new Bundle
        assertNotSame("Bob must have published another bundle at this point, since we used a PreKeyMessage.",
                bobsBundle, OmemoService.fetchBundle(alice, bob.getOwnDevice()));

        //Prepare Bobs response
        Message encryptedB = bob.encrypt(alice.getOwnJid(), bobsSecret);
        ChatManager.getInstanceFor(bob.getConnection()).chatWith(alice.getOwnJid().asEntityBareJidIfPossible())
                .send(encryptedB);

        try {
            messageTwoSyncPoint.waitForResult(10 * 1000);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while waiting for response: " + e, e);
            TestCase.fail("Alice must have received a response from Bob.");
        }

        assertEquals("Alice must not have published a new bundle, since we built the session using Bobs bundle.",
                aliceBundle, OmemoService.fetchBundle(bob, alice.getOwnDevice()));
    }

    @Override
    public void after() {
        alice.shutdown();
        bob.shutdown();
        cleanServerSideTraces(alice);
        cleanServerSideTraces(bob);
    }
}
