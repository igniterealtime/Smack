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

package org.jivesoftware.smack;

import static org.jivesoftware.smackx.jiveproperties.JivePropertiesManager.addProperty;
import static org.jivesoftware.smackx.jiveproperties.JivePropertiesManager.getProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Tests for Chat Manager and for Chat Manager Listener.
 *
 * @author Stawicki Adam
 */
public class ChatTest extends AbstractSmackIntegrationTest {

    @SuppressWarnings("deprecation")
    private final org.jivesoftware.smack.chat.ChatManager chatManagerOne;
    private boolean invoked;

    @SuppressWarnings("deprecation")
    public ChatTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        chatManagerOne = org.jivesoftware.smack.chat.ChatManager.getInstanceFor(conOne);
    }

    @BeforeClass
    public void setUp() {
        JivePropertiesManager.setJavaObjectEnabled(true);
    }

    @AfterClass
    public void tearDown() {
        JivePropertiesManager.setJavaObjectEnabled(false);
    }

    @SuppressWarnings("deprecation")
    @SmackIntegrationTest
    public void testProperties() throws Exception {
        org.jivesoftware.smack.chat.Chat newChat = chatManagerOne.createChat(conTwo.getUser());
        StanzaCollector collector = conTwo.createStanzaCollector(new ThreadFilter(newChat.getThreadID()));

        Message msg = new Message();

        msg.setSubject("Subject of the chat");
        msg.setBody("Body of the chat");
        addProperty(msg, "favoriteColor", "red");
        addProperty(msg, "age", 30);
        addProperty(msg, "distance", 30f);
        addProperty(msg, "weight", 30d);
        addProperty(msg, "male", true);
        addProperty(msg, "birthdate", new Date());

        newChat.sendMessage(msg);

        Message msg2 = collector.nextResult(2000);
        assertNotNull("No message was received", msg2);
        assertEquals("Subjects are different", msg.getSubject(), msg2.getSubject());
        assertEquals("Bodies are different", msg.getBody(), msg2.getBody());
        assertEquals(
               "favoriteColors are different",
               getProperty(msg, "favoriteColor"),
               getProperty(msg2, "favoriteColor"));
        assertEquals(
               "ages are different",
               getProperty(msg, "age"),
               getProperty(msg2, "age"));
        assertEquals(
               "distances are different",
               getProperty(msg, "distance"),
               getProperty(msg2, "distance"));
        assertEquals(
               "weights are different",
               getProperty(msg, "weight"),
               getProperty(msg2, "weight"));
        assertEquals(
               "males are different",
               getProperty(msg, "male"),
               getProperty(msg2, "male"));
        assertEquals(
               "birthdates are different",
               getProperty(msg, "birthdate"),
               getProperty(msg2, "birthdate"));
    }

    @SuppressWarnings("deprecation")
    @SmackIntegrationTest
    public void chatManagerTest() {
        ChatManagerListener listener = new ChatManagerListener() {

            @Override
            public void chatCreated(org.jivesoftware.smack.chat.Chat chat, boolean createdLocally) {
                invoked = true;
            }

        };
        try {
            chatManagerOne.addChatListener(listener);
            chatManagerOne.createChat(conTwo.getUser());

            assertTrue("TestChatManagerListener wasn't invoked", invoked);
        }
        finally {
            chatManagerOne.removeChatListener(listener);
        }
    }
}
