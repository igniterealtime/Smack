/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.mam;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager.MamQueryResult;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jxmpp.jid.EntityBareJid;

public class MamIntegrationTest extends AbstractSmackIntegrationTest {

    private final MamManager mamManagerConTwo;

    public MamIntegrationTest(SmackIntegrationTestEnvironment environment) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException {
        super(environment);

        mamManagerConTwo = MamManager.getInstanceFor(conTwo);

        if (!mamManagerConTwo.isSupportedByServer()) {
            throw new TestNotPossibleException("Message Archive Management (XEP-0313) is not supported by the server.");
        }

    }

    @SmackIntegrationTest
    public void mamTest() throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException,
                    NotLoggedInException {
        EntityBareJid userOne = conOne.getUser().asEntityBareJid();
        EntityBareJid userTwo = conTwo.getUser().asEntityBareJid();

        //Make sure MAM is archiving messages
        mamManagerConTwo.updateArchivingPreferences(null, null, MamPrefsIQ.DefaultBehavior.always);

        Message message = new Message(userTwo);
        String messageId = message.setStanzaId();
        String messageBody = "test message";
        message.setBody(messageBody);

        conOne.sendStanza(message);

        int pageSize = 20;
        MamQueryResult mamQueryResult = mamManagerConTwo.queryArchive(pageSize, null, null, userOne, null);

        while (!mamQueryResult.mamFin.isComplete()) {
            mamQueryResult = mamManagerConTwo.pageNext(mamQueryResult, pageSize);
        }

        List<Forwarded> forwardedMessages = mamQueryResult.forwardedMessages;
        Message mamMessage = (Message) forwardedMessages.get(forwardedMessages.size() - 1).getForwardedStanza();

        assertEquals(messageId, mamMessage.getStanzaId());
        assertEquals(messageBody, mamMessage.getBody());
        assertEquals(conOne.getUser(), mamMessage.getFrom());
        assertEquals(userTwo, mamMessage.getTo());
    }

}
