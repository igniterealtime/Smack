/**
 *
 * Copyright 2015 Florian Schmaus
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

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

public class MultiUserChatIntegrationTest extends AbstractSmackIntegrationTest {

    private final String randomString = StringUtils.randomString(6);

    private final MultiUserChatManager mucManagerOne;
    private final MultiUserChatManager mucManagerTwo;
    private final DomainBareJid mucService;

    public MultiUserChatIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException, TestNotPossibleException {
        super(environment);
        mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);

        List<DomainBareJid> services = mucManagerOne.getServiceNames();
        if (services.isEmpty()) {
            throw new TestNotPossibleException("No MUC (XEP-45) service found");
        }
        else {
            mucService = services.get(0);
        }
    }

    @SmackIntegrationTest
    public void mucTest() throws TimeoutException, Exception {
        BareJid mucAddress = JidCreate.bareFrom(Localpart.from("smack-inttest-" + randomString), mucService.getDomain());

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

        boolean newlyCreated = mucAsSeenByOne.createOrJoin(Resourcepart.from("one-" + randomString));
        if (newlyCreated) {
            mucAsSeenByOne.sendConfigurationForm(new Form(DataForm.Type.submit));
        }
        mucAsSeenByTwo.join(Resourcepart.from("two-" + randomString));

        mucAsSeenByOne.sendMessage(mucMessage);
        resultSyncPoint.waitForResult(defaultTimeout);

        mucAsSeenByOne.leave();
        mucAsSeenByTwo.leave();
    }
}
