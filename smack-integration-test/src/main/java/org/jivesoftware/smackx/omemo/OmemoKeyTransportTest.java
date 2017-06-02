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
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.subscribe;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.unidirectionalTrust;
import static org.junit.Assert.assertTrue;

/**
 * Test keyTransportMessages.
 */
public class OmemoKeyTransportTest extends AbstractOmemoIntegrationTest {

    private OmemoManager alice, bob;

    public OmemoKeyTransportTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @Override
    public void before() {
        alice = OmemoManager.getInstanceFor(conOne, 11111);
        bob = OmemoManager.getInstanceFor(conTwo, 222222);
    }

    @SmackIntegrationTest
    public void keyTransportTest() throws Exception {
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        subscribe(alice, bob, "Bob");
        subscribe(bob, alice, "Alice");

        setUpOmemoManager(alice);
        setUpOmemoManager(bob);

        unidirectionalTrust(alice, bob);
        unidirectionalTrust(bob, alice);

        final byte[] key = OmemoMessageBuilder.generateKey();
        final byte[] iv = OmemoMessageBuilder.generateIv();

        bob.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                //Don't care
            }

            @Override
            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                LOGGER.log(Level.INFO, "Received a keyTransportMessage.");
                assertTrue("Key must match the one we sent.", Arrays.equals(key, cipherAndAuthTag.getKey()));
                assertTrue("IV must match the one we sent.", Arrays.equals(iv, cipherAndAuthTag.getIv()));
                syncPoint.signal();
            }
        });

        OmemoElement keyTransportElement = alice.createKeyTransportElement(key, iv, bob.getOwnDevice());
        Message message = new Message(bob.getOwnJid());
        message.addExtension(keyTransportElement);
        ChatManager.getInstanceFor(alice.getConnection()).chatWith(bob.getOwnJid().asEntityBareJidIfPossible())
                .send(message);

        try {
            syncPoint.waitForResult(10 * 1000);
        } catch (TimeoutException e) {
            TestCase.fail("We MUST have received the keyTransportMessage within 10 seconds.");
        }
    }

    @Override
    public void after() {
        alice.shutdown();
        bob.shutdown();
        cleanServerSideTraces(alice);
        cleanServerSideTraces(bob);
    }
}
