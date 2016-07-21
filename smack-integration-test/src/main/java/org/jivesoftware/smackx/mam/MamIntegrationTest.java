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

import java.util.List;
import java.util.UUID;

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
import org.junit.Assert;
import org.jxmpp.jid.Jid;

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

    private Message getConTwoLastMessageSentFrom(Jid userOne) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        int pageSize = 20;
        MamQueryResult mamQueryResult = mamManagerConTwo.queryArchive(pageSize, null, null, userOne, null);

        while (!mamQueryResult.mamFin.isComplete()) {
            mamQueryResult = mamManagerConTwo.pageNext(mamQueryResult, pageSize);
        }

        List<Forwarded> forwardedMessages = mamQueryResult.forwardedMessages;
        Message messageFromMAM = (Message) forwardedMessages.get(forwardedMessages.size() - 1).getForwardedStanza();
        return messageFromMAM;
    }

    private Message prepareMessage(Jid to, String messageId, String body) {
        Message message = new Message();
        message.setTo(to);
        message.setStanzaId(messageId);
        message.setBody(body);
        return message;
    }

    @SmackIntegrationTest
    public void mamTest() throws Exception {
        Jid userOne = conOne.getUser().asEntityBareJid();
        Jid userTwo = conTwo.getUser().asEntityBareJid();

        String messageId = UUID.randomUUID().toString();
        String messageBody = "test message";

        Message message = prepareMessage(userTwo, messageId, messageBody);
        conOne.sendStanza(message);

        Message mamMessage = getConTwoLastMessageSentFrom(userOne);

        Assert.assertEquals(messageId, mamMessage.getStanzaId());
        Assert.assertEquals(messageBody, mamMessage.getBody());
        Assert.assertEquals(conOne.getUser(), mamMessage.getFrom());
        Assert.assertEquals(userTwo, mamMessage.getTo());
    }

}
