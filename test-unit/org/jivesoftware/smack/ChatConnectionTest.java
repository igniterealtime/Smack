/**
 * $RCSfile$
 * $Revision: 11640 $
 * $Date: 2010-02-18 08:38:57 -0500 (Thu, 18 Feb 2010) $
 *
 * Copyright 2010 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that verifies the correct behavior of the {@see Roster} implementation.
 * 
 * @see Roster
 * @see <a href="http://xmpp.org/rfcs/rfc3921.html#roster">Roster Management</a>
 * @author Guenther Niess
 */
public class ChatConnectionTest {

    private DummyConnection connection;

    @Before
    public void setUp() throws Exception {
	// Uncomment this to enable debug output
	//Connection.DEBUG_ENABLED = true;

	connection = new DummyConnection();
	connection.connect();
	connection.login("me", "secret");
    }

    @After
    public void tearDown() throws Exception {
	if (connection != null)
	    connection.disconnect();
    }

    /**
     * Confirm that a new chat is created when a chat message is received but
     * there is no thread id for a user with only a base jid.
     */
    @Test
    public void chatCreatedWithIncomingChatNoThreadBaseJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);

	Packet incomingChat = createChatPacket(null, false);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
    }

    /**
     * Confirm that a new chat is created when a chat message is received but
     * there is no thread id for a user with a full jid.
     */
    @Test
    public void chatCreatedWhenIncomingChatNoThreadFullJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);

	Packet incomingChat = createChatPacket(null, true);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an
     * incoming chat message that has no thread id and the user is a full jid.
     */
    @Test
    public void chatFoundWhenNoThreadFullJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(null, true);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an
     * incoming chat message that has no thread id and the user is a base jid.
     */
    @Test
    public void chatFoundWhenNoThreadBaseJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(null, false);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an
     * incoming chat message that has the same id and the user is a full jid.
     */
    @Test
    public void chatFoundWithSameThreadFullJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(outgoing.getThreadID(), true);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is matched to an
     * incoming chat message that has the same id and the user is a base jid.
     */
    @Test
    public void chatFoundWithSameThreadBaseJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(outgoing.getThreadID(), false);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertTrue(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is not matched to
     * an incoming chat message that has a different id and the same user as a
     * base jid.
     */
    @Ignore
    @Test
    public void chatNotFoundWithDiffThreadBaseJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(outgoing.getThreadID() + "ff", false);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertFalse(newChat == outgoing);
    }

    /**
     * Confirm that an existing chat created with a base jid is not matched to
     * an incoming chat message that has a different id and the same base jid.
     */
    @Ignore
    @Test
    public void chatNotFoundWithDiffThreadFullJid()
    {
	TestChatManagerListener listener = new TestChatManagerListener();
	connection.getChatManager().addChatListener(listener);
	Chat outgoing = connection.getChatManager().createChat("you@testserver", null);

	Packet incomingChat = createChatPacket(outgoing.getThreadID() + "ff", true);
	processServerMessage(incomingChat);

	Chat newChat = listener.getNewChat();
	assertNotNull(newChat);
	assertFalse(newChat == outgoing);
    }

    private Packet createChatPacket(final String threadId, final boolean isFullJid)
    {
	Message chatMsg = new Message("me@testserver", Message.Type.chat);
	chatMsg.setBody("the body message");
	chatMsg.setFrom("you@testserver" + (isFullJid ? "/resource" : ""));

	if (threadId != null)
	    chatMsg.addExtension(new PacketExtension()
	    {
		@Override
		public String toXML()
		{
		    return "<thread>" + threadId + "</thread>";
		}

		@Override
		public String getNamespace()
		{
		    return null;
		}

		@Override
		public String getElementName()
		{
		    return "thread";
		}
	    });
	return chatMsg;
    }

    private void processServerMessage(Packet incomingChat)
    {
	TestChatServer chatServer = new TestChatServer(incomingChat);
	chatServer.start();
	try
	{
	    chatServer.join();
	} catch (InterruptedException e)
	{
	    fail();
	}
    }

    class TestChatManagerListener implements ChatManagerListener
    {
	private Chat newChat;

	@Override
	public void chatCreated(Chat chat, boolean createdLocally)
	{
	    newChat = chat;
	}

	public Chat getNewChat()
	{
	    return newChat;
	}
    }

    private class TestChatServer extends Thread
    {
	private Packet chatPacket;

	TestChatServer(Packet chatMsg)
	{
	    chatPacket = chatMsg;
	}

	@Override
	public void run()
	{
	    connection.processPacket(chatPacket);
	}
    }
}
