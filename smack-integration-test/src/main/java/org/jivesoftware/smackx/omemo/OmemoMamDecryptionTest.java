/**
 *
 * Copyright 2018 Paul Schaub
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

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.util.MessageOrOmemoMessage;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * This test sends a message from Alice to Bob, while Bob has automatic decryption disabled.
 * Then Bob fetches his Mam archive and decrypts the result.
 */
public class OmemoMamDecryptionTest extends AbstractTwoUsersOmemoIntegrationTest {
    public OmemoMamDecryptionTest(SmackIntegrationTestEnvironment<?> environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
        MamManager bobsMamManager = MamManager.getInstanceFor(conTwo);
        if (!bobsMamManager.isSupported()) {
            throw new TestNotPossibleException("Test is not possible, because MAM is not supported on the server.");
        }
    }

    @SmackIntegrationTest
    public void mamDecryptionTest() throws XMPPException.XMPPErrorException, SmackException.NotLoggedInException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            CryptoFailedException, UndecidedOmemoIdentityException, IOException {
        // Make sure, Bobs server stores messages in the archive
        MamManager bobsMamManager = MamManager.getInstanceFor(bob.getConnection());
        bobsMamManager.enableMamForAllMessages();
        bobsMamManager.setDefaultBehavior(MamPrefsIQ.DefaultBehavior.always);

        // Prevent bob from automatically decrypting MAM messages.
        bob.stopStanzaAndPEPListeners();

        String body = "This message will be stored in MAM!";
        OmemoMessage.Sent encrypted = alice.encrypt(bob.getOwnJid(), body);
        alice.getConnection().sendStanza(encrypted.asMessage(bob.getOwnJid()));

        MamManager.MamQuery query = bobsMamManager.queryArchive(MamManager.MamQueryArgs.builder().limitResultsToJid(alice.getOwnJid()).build());
        assertEquals(1, query.getMessageCount());

        List<MessageOrOmemoMessage> decryptedMamQuery = bob.decryptMamQueryResult(query);

        assertEquals(1, decryptedMamQuery.size());
        assertEquals(body, decryptedMamQuery.get(decryptedMamQuery.size() - 1).getOmemoMessage().getBody());
    }
}
