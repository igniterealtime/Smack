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
package org.jivesoftware.smack.chat2;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;

public class IncomingMessageListenerIntegrationTest extends AbstractChatIntegrationTest {

    public IncomingMessageListenerIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void test() throws Exception {
        final String body = StringUtils.randomString(16);
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
        final IncomingChatMessageListener listener = new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                if (body.equals(message.getBody())) {
                    syncPoint.signal();
                }
            }
        };

        try {
            chatManagerTwo.addIncomingListener(listener);
            Chat chat = chatManagerOne.chatWith(conTwo.getUser().asEntityBareJid());
            chat.send(body);
            syncPoint.waitForResult(timeout);
        }
        finally {
            chatManagerTwo.removeIncomingListener(listener);
        }
    }
}
