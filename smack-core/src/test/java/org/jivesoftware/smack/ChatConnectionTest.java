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
package org.jivesoftware.smack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jivesoftware.smack.ChatManager.MatchMode;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChatConnectionTest {

    private DummyConnection connection;

    @Before
    public void setUp() throws Exception {
        connection = getConnection();
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null)
            connection.disconnect();
    }

    @Test
    public void validateDefaultSetNormalIncluded() {
        ChatManager.setDefaultIsNormalIncluded(false);
        assertFalse(ChatManager.getInstanceFor(getConnection()).isNormalIncluded());
        
        ChatManager.setDefaultIsNormalIncluded(true);
        assertTrue(ChatManager.getInstanceFor(getConnection()).isNormalIncluded());
    }
    
    @Test
    public void validateDefaultSetMatchMode() {
        ChatManager.setDefaultMatchMode(MatchMode.NONE);
        assertEquals(MatchMode.NONE, ChatManager.getInstanceFor(getConnection()).getMatchMode());
        
        ChatManager.setDefaultMatchMode(MatchMode.BARE_JID);
        assertEquals(MatchMode.BARE_JID, ChatManager.getInstanceFor(getConnection()).getMatchMode());
    }

    @Test
    public void validateMessageTypeWithDefaults() {
        DummyConnection dc = getConnection();
        ChatManager cm = ChatManager.getInstanceFor(dc);
        TestChatManagerListener listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.chat);
        processServerMessage(incomingChat, dc);
        assertNotNull(listener.getNewChat());

        dc = getConnection();
        cm = ChatManager.getInstanceFor(dc);
        listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat, dc);
        assertNotNull(listener.getNewChat());

        dc = getConnection();
        cm = ChatManager.getInstanceFor(dc);
        listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.groupchat);
        processServerMessage(incomingChat, dc);
        assertNull(listener.getNewChat());

        dc = getConnection();
        cm = ChatManager.getInstanceFor(dc);
        listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.headline);
        processServerMessage(incomingChat, dc);
        assertNull(listener.getNewChat());
    }
    
    @Test
    public void validateMessageTypeWithNoNormal() {
        DummyConnection dc = getConnection();
        ChatManager cm = ChatManager.getInstanceFor(dc);
        cm.setNormalIncluded(false);
        TestChatManagerListener listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        Message incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.chat);
        processServerMessage(incomingChat, dc);
        assertNotNull(listener.getNewChat());

        dc = getConnection();
        cm = ChatManager.getInstanceFor(dc);
        cm.setNormalIncluded(false);
        listener = new TestChatManagerListener(); 
        cm.addChatListener(listener);
        incomingChat = createChatPacket("134", true);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat, dc);
        assertNull(listener.getNewChat());
    }

    // No thread behaviour
    @Test
    public void chatMatchedOnJIDWhenNoThreadBareMode() {
        // MatchMode.BARE_JID is the default, so setting required.
        DummyConnection con = getConnection();
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        ChatManager cm = ChatManager.getInstanceFor(con);
        cm.addChatListener(listener);
        Packet incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);

        // Should match on chat with full jid
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        assertEquals(2, msgListener.getNumMessages());

        // Should match on chat with bare jid
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat, con);
        assertEquals(3, msgListener.getNumMessages());
    }

    @Test
    public void chatMatchedOnJIDWhenNoThreadJidMode() {
        DummyConnection con = getConnection();
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        ChatManager cm = ChatManager.getInstanceFor(con);
        cm.setMatchMode(MatchMode.SUPPLIED_JID);
        cm.addChatListener(listener);
        Packet incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        cm.removeChatListener(listener);

        // Should match on chat with full jid
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        assertEquals(2, msgListener.getNumMessages());

        // Should not match on chat with bare jid
        TestChatManagerListener listener2 = new TestChatManagerListener();
        cm.addChatListener(listener2);
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat, con);
        assertEquals(2, msgListener.getNumMessages());
        assertNotNull(listener2.getNewChat());
    }

    @Test
    public void chatMatchedOnJIDWhenNoThreadNoneMode() {
        DummyConnection con = getConnection();
        TestMessageListener msgListener = new TestMessageListener();
        TestChatManagerListener listener = new TestChatManagerListener(msgListener);
        ChatManager cm = ChatManager.getInstanceFor(con);
        cm.setMatchMode(MatchMode.NONE);
        cm.addChatListener(listener);
        Packet incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertEquals(1, msgListener.getNumMessages());
        cm.removeChatListener(listener);

        // Should not match on chat with full jid
        TestChatManagerListener listener2 = new TestChatManagerListener();
        cm.addChatListener(listener2);
        incomingChat = createChatPacket(null, true);
        processServerMessage(incomingChat, con);
        assertEquals(1, msgListener.getNumMessages());
        assertNotNull(newChat);
        cm.removeChatListener(listener2);

        // Should not match on chat with bare jid
        TestChatManagerListener listener3 = new TestChatManagerListener();
        cm.addChatListener(listener3);
        incomingChat = createChatPacket(null, false);
        processServerMessage(incomingChat, con);
        assertEquals(1, msgListener.getNumMessages());
        assertNotNull(listener3.getNewChat());
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an incoming chat message that has no thread
     * id and the user is a full jid.
     */
    @Test
    public void chatFoundWhenNoThreadFullJid() {
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(null, true);
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
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(null, false);
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
    public void chatFoundWithSameThreadFullJid() {
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(outgoing.getThreadID(), true);
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
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(outgoing.getThreadID(), false);
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
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(outgoing.getThreadID() + "ff", false);
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
    public void chatNotFoundWithDiffThreadFullJid() {
        TestChatManagerListener listener = new TestChatManagerListener();
        ChatManager cm = ChatManager.getInstanceFor(connection);
        cm.addChatListener(listener);
        Chat outgoing = cm.createChat("you@testserver", null);

        Packet incomingChat = createChatPacket(outgoing.getThreadID() + "ff", true);
        processServerMessage(incomingChat);

        Chat newChat = listener.getNewChat();
        assertNotNull(newChat);
        assertFalse(newChat == outgoing);
    }

    @Test
    public void chatNotMatchedWithTypeNormal() {
        TestChatManagerListener listener = new TestChatManagerListener();
        DummyConnection con = getConnection();
        ChatManager cm = ChatManager.getInstanceFor(con);
        cm.setNormalIncluded(false);
        cm.addChatListener(listener);

        Message incomingChat = createChatPacket(null, false);
        incomingChat.setType(Type.normal);
        processServerMessage(incomingChat);

        assertNull(listener.getNewChat());
    }

    @SuppressWarnings("unused")
    private ChatManager getChatManager(boolean includeNormal, MatchMode mode) {
        ChatManager cm = ChatManager.getInstanceFor(getConnection());
        cm.setMatchMode(mode);
        cm.setNormalIncluded(includeNormal);
        return cm;
    }
    
    private DummyConnection getConnection() {
        DummyConnection con = new DummyConnection();
        
        try {
            con.connect();
            con.login("me", "secret");
        } catch (Exception e) {
            // No need for handling in a dummy connection.
        }
        return con;
    }
    private Message createChatPacket(final String threadId, final boolean isFullJid) {
        Message chatMsg = new Message("me@testserver", Message.Type.chat);
        chatMsg.setBody("the body message - " + System.currentTimeMillis());
        chatMsg.setFrom("you@testserver" + (isFullJid ? "/resource" : ""));

        if (threadId != null)
            chatMsg.setThread(threadId);
        return chatMsg;
    }

    private void processServerMessage(Packet incomingChat) {
        processServerMessage(incomingChat, connection);
    }
    
    private void processServerMessage(Packet incomingChat, DummyConnection con) {
        TestChatServer chatServer = new TestChatServer(incomingChat, con);
        chatServer.start();
        try {
            chatServer.join();
        } catch (InterruptedException e) {
            fail();
        }
    }

    class TestChatManagerListener implements ChatManagerListener {
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
        }

        public Chat getNewChat() {
            return newChat;
        }
    }
    
    private class TestChatServer extends Thread {
        private Packet chatPacket;
        private DummyConnection con;

        TestChatServer(Packet chatMsg, DummyConnection conect) {
            chatPacket = chatMsg;
            con = conect;
        }

        @Override
        public void run() {
            con.processPacket(chatPacket);
        }
    }

    private class TestMessageListener implements ChatMessageListener {
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
    };
}
