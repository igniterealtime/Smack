/*
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
package org.jivesoftware.smackx.chatstate;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

@SpecificationReference(document = "XEP-0085", version = "2.1")
public class ChatStateIntegrationTest extends AbstractSmackIntegrationTest {

    // Listener for composing chat state
    private final SimpleResultSyncPoint composingSyncPoint = new SimpleResultSyncPoint();
    private void  composingListener(Chat chat, ChatState state, Message message) {
        if (state.equals(ChatState.composing)) {
            composingSyncPoint.signal();
        }
    }

    // Listener for active chat state
    private final SimpleResultSyncPoint activeSyncPoint = new SimpleResultSyncPoint();
    private void activeListener(Chat chat, ChatState state, Message message) {
        if (state.equals(ChatState.active)) {
            activeSyncPoint.signal();
        }
    }


    public ChatStateIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void testChatStateListeners() throws Exception {
        ChatStateManager manOne = ChatStateManager.getInstance(conOne);
        ChatStateManager manTwo = ChatStateManager.getInstance(conTwo);

        // Add chatState listeners.
        manTwo.addChatStateListener(this::composingListener);
        manTwo.addChatStateListener(this::activeListener);

        Chat chatOne = ChatManager.getInstanceFor(conOne)
                .chatWith(conTwo.getUser().asEntityBareJid());

        // Test, if setCurrentState works and the chatState arrives
        manOne.setCurrentState(ChatState.composing, chatOne);
        composingSyncPoint.waitForResult(timeout);

        // Test, if the OutgoingMessageInterceptor successfully adds a chatStateExtension of "active" to
        // an outgoing chat message and if it arrives at the other side.
        Chat chat = ChatManager.getInstanceFor(conOne)
                .chatWith(conTwo.getUser().asEntityBareJid());
        chat.send("Hi!");
        assertResult(activeSyncPoint, "Expected " + conTwo.getUser() + " to receive an 'active' chat state from " + conOne + " (but they did not).");
    }

    @AfterClass
    public void cleanup() {
        ChatStateManager manTwo = ChatStateManager.getInstance(conTwo);
        manTwo.removeChatStateListener(this::composingListener);
        manTwo.removeChatStateListener(this::activeListener);
    }
}
