/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.c2s;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

import org.jxmpp.jid.EntityFullJid;

public class SimpleXmppConnectionIntegrationTest extends AbstractSmackIntegrationTest {

    public SimpleXmppConnectionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void createConnectionTest() throws TimeoutException, Exception {
        EntityFullJid userTwo = conTwo.getUser();

        final String messageBody = testRunId + ": Hello from the other side!";
        Message message = conTwo.getStanzaFactory().buildMessageStanza()
                        .to(userTwo)
                        .setBody(messageBody)
                        .build();

        final SimpleResultSyncPoint messageReceived = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = (Stanza stanza) -> {
            if (((Message) stanza).getBody().equals(messageBody)) {
                messageReceived.signal();
            }
        };

        conTwo.addAsyncStanzaListener(stanzaListener, MessageWithBodiesFilter.INSTANCE);

        try {
            conOne.sendStanza(message);

            messageReceived.waitForResult(timeout);
        } finally {
            conTwo.removeAsyncStanzaListener(stanzaListener);
        }
    }
}
