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
package org.jivesoftware.smackx.chat;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;

public class OutgoingMessageListenerIntegrationTest extends AbstractSmackIntegrationTest {

    public OutgoingMessageListenerIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void outgoingMessageListenerTest() throws Exception {
        ChatManager chatManagerOne = ChatManager.getInstanceFor(conOne);

        final String body = StringUtils.randomString(16);
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
        final OutgoingChatMessageListener listener = new OutgoingChatMessageListener() {
            @Override
            public void newOutgoingMessage(EntityBareJid to, Message message, Chat chat) {
                if (message.getBody().equals(body)) {
                    syncPoint.signal();
                }
            }
        };

        EntityBareJid peer = conTwo.getUser().asEntityBareJid();

        try {
            chatManagerOne.addOutgoingListener(listener);
            Chat chat = chatManagerOne.chatWith(peer);
            chat.send(body);
            syncPoint.waitForResult(10 * 1000);
        }
        finally {
            chatManagerOne.removeOutgoingListener(listener);
        }
    }
}
