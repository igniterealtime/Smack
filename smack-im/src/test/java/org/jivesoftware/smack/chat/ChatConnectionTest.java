/**
 *
 * Copyright 2010 Jive Software.
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
package org.jivesoftware.smack.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.test.util.WaitForPacketListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;

@SuppressWarnings({"deprecation", "ReferenceEquality"})
public class ChatConnectionTest {

    private DummyConnection dc;
    private ChatManager cm;
    private TestChatManagerListener listener;
    private WaitForPacketListener waitListener;

    @Before
    public void setUp() throws Exception {
        // Defaults
        ChatManager.setDefaultIsNormalIncluded(true);
        ChatManager.setDefaultMatchMode(ChatManager.MatchMode.BARE_JID);

        dc = DummyConnection.newConnectedDummyConnection();
        cm = ChatManager.getInstanceFor(dc);
        listener = new TestChatManagerListener();
        cm.addChatListener(listener);
        waitListener = new WaitForPacketListener();
        dc.addSyncStanzaListener(waitListener, null);
    }

    @After
    public void tearDown() throws Exception {
        if (dc != null) {
            dc.disconnect();
        }
    }

    @Test
    public void validateDefaultSetNormalIncludedFalse() {
        ChatManager.setDefaultIsNormalIncluded(false);
        assertFalse(ChatManager.getInstanceFor(new DummyConnection()).isNormalIncluded());
    }

    @Test
    public void validateDefaultSetNormalIncludedTrue() {
        ChatManager.setDefaultIsNormalIncluded(true);
        assertTrue(ChatManager.getInstanceFor(new DummyConnection()).isNormalIncluded());
    }

    @Test
    public void validateDefaultSetMatchModeNone() {
        ChatManager.setDefaultMatchMode(ChatManager.MatchMode.NONE);
        assertEquals(ChatManager.MatchMode.NONE, ChatManager.getInstanceFor(new DummyConnection()).getMatchMode());
    }

    @Test
    public void validateDefaultSetMatchModeEntityBareJid() {
        ChatManager.setDefaultMatchMode(ChatManager.MatchMode.BARE_JID);
        assertEquals(ChatManager.MatchMode.BARE_JID, ChatManager.getInstanceFor(new DummyConnection()).getMatchMode());
    }

    @Test
    public void validateMessageTypeWithDefaults1() {
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.chat);
        processServerMessage(incomingChat);
        assertNotNull(listener.getNewChat());
    }

    @Test
    public void validateMessageTypeWithDefaults2() {
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat);
        assertNotNull(listener.getNewChat());
    }
    @Test
    public void validateMessageTypeWithDefaults3() {
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.groupchat);
        processServerMessage(incomingChat);
        assertNull(listener.getNewChat());
    }

    @Test
    public void validateMessageTypeWithDefaults4() {
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.headline);
        assertNull(listener.getNewChat());
    }

    @Test
    public void validateMessageTypeWithNoNormal1() {
        cm.setNormalIncluded(false);
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.chat);
        processServerMessage(incomingChat);
        assertNotNull(listener.getNewChat());
    }

    @Test
    public void validateMessageTypeWithNoNormal2() {
        cm.setNormalIncluded(false);
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat);
        assertNull(listener.getNewChat());
    }

    // No thread behaviour
    @Test
    public void chatMatchedOnJIDWhenNoThreadBareMode() {
        // ChatManager.MatchMode.BARE_JID is the default, so setting required.
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        cm.addChatListener(listener);
        Stanza incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);

        // Should match on chat with full jid
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        assertEquals(2, msgListener.getNumMessages());

        // Should match on chat with bare jid
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat);
        assertEquals(3, msgListener.getNumMessages());
    }

    @Test
    public void chatMatchedOnJIDWhenNoThreadJidMode() {
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        cm.setMatchMode(ChatManager.MatchMode.SUPPLIED_JID);
        cm.addChatListener(listener);
        Stanza incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        cm.removeChatListener(listener);

        // Should match on chat with full jid
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        assertEquals(2, msgListener.getNumMessages());

        // Should not match on chat with bare jid
        TestChatManagerListener listener2 = new TestChatManagerListener();
        cm.addChatListener(listener2);
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat);
        assertEquals(2, msgListener.getNumMessages());
        assertNotNull(listener2.getNewChat());
    }

    @Test
    public void chatMatchedOnJIDWhenNoThreadNoneMode() {
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        cm.setMatchMode(ChatManager.MatchMode.NONE);
        cm.addChatListener(listener);
        Stanza incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertEquals(1, msgListener.getNumMessages());
        cm.removeChatListener(listener);

        // Should not match on chat with full jid
        TestChatManagerListener listener2 = new TestChatManagerListener();
        cm.addChatListener(listener2);
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);
        assertEquals(1, msgListener.getNumMessages());
        assertNotNull(newChat);
        cm.removeChatListener(listener2);

        // Should not match on chat with bare jid
        TestChatManagerListener listener3 = new TestChatManagerListener();
        cm.addChatListener(listener3);
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat);
        assertEquals(1, msgListener.getNumMessages());
        assertNotNull(listener3.getNewChat());
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an incoming chat message that has no thread
     * id and the user is a full jid.
     */
    @Test
    public void chatFoundWhenNoThreadEntityFullJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an incoming chat message that has no thread
     * id and the user is a base jid.
     */
    @Test
    public void chatFoundWhenNoThreadBaseJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an incoming chat message that has the same id
     * and the user is a full jid.
     */
    @Test
    public void chatFoundWithSameThreadEntityFullJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(outgoing.getThreadID(), true);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an incoming chat message that has the same id
     * and the user is a base jid.
     */
    @Test
    public void chatFoundWithSameThreadBaseJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(outgoing.getThreadID(), false);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is not matched to an incoming chat message that has a
     * different id and the same user as a base jid.
     */
    @Test
    public void chatNotFoundWithDiffThreadBaseJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(outgoing.getThreadID() + "ff", false);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertFalse(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is not matched to an incoming chat message that has a
     * different id and the same base jid.
     */
    @Test
    public void chatNotFoundWithDiffThreadEntityFullJid() {
        Chat outgoing = cm.createChat(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, null);

        Stanza incomingChat = createChatPacket(outgoing.getThreadID() + "ff", true);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertFalse(newChat == outgoing);
    }

    @Test
    public void chatNotMatchedWithTypeNormal() {
        cm.setNormalIncluded(false);

        Message incomingChat = createChatPacket(null, false);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat);

        assertNull(listener.getNewChat());
    }

    private static Message createChatPacket(final String threadId, final boolean isEntityFullJid) {
        Message chatMsg = new Message(JidTestUtil.BARE_JID_1, Message.Type.chat);
        chatMsg.setBody("the body message - " + System.currentTimeMillis());
        Jid jid;
        if (isEntityFullJid) {
            jid = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
        } else {
            jid = JidTestUtil.DUMMY_AT_EXAMPLE_ORG;
        }
        chatMsg.setFrom(jid);
        chatMsg.setThread(threadId);
        return chatMsg;
    }

    private void processServerMessage(Stanza incomingChat) {
        TestChatServer chatServer = new TestChatServer(incomingChat, dc);
        chatServer.start();
        try {
            chatServer.join();
        } catch (InterruptedException e) {
            fail();
        }
        waitListener.waitAndReset();
    }

    static class TestChatManagerListener extends WaitForPacketListener implements ChatManagerListener {
        private Chat newChat;
        private ChatMessageListener listener;

        public TestChatManagerListener(TestMessageListener msgListener) {
            listener = msgListener;
        }

        public TestChatManagerListener() {
        }

        @Override
        public void chatCreated(Chat chat, boolean createdLocally) {
            newChat = chat;

            if (listener != null)
                newChat.addMessageListener(listener);
            reportInvoked();
        }

        public Chat getNewChat() {
            return newChat;
        }
    }

    private static class TestChatServer extends Thread {
        private Stanza chatPacket;
        private DummyConnection con;

        TestChatServer(Stanza chatMsg, DummyConnection conect) {
            chatPacket = chatMsg;
            con = conect;
        }

        @Override
        public void run() {
            con.processStanza(chatPacket);
        }
    }

    private static class TestMessageListener implements ChatMessageListener {
        private Chat msgChat;
        private int counter = 0;

        @Override
        public void processMessage(Chat chat, Message message) {
            msgChat = chat;
            counter++;
        }

        @SuppressWarnings("unused")
        public Chat getChat() {
            return msgChat;
        }

        public int getNumMessages() {
            return counter;
        }
    }
}
