/**
 *
 * Copyright 2015-2020 Florian Schmaus
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;

import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

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

        MessageBuilder messageBuilder = StanzaBuilder.buildMessage();

        messageBuilder.setSubject("Subject of the chat");
        messageBuilder.setBody("Body of the chat");
        addProperty(messageBuilder, "favoriteColor", "red");
        addProperty(messageBuilder, "age", 30);
        addProperty(messageBuilder, "distance", 30f);
        addProperty(messageBuilder, "weight", 30d);
        addProperty(messageBuilder, "male", true);
        addProperty(messageBuilder, "birthdate", new Date());

        Message msg = messageBuilder.build();
        newChat.sendMessage(msg);

        Message msg2 = collector.nextResult(2000);
        assertNotNull(msg2, "No message was received");
        assertEquals(msg.getSubject(), msg2.getSubject(), "Subjects are different");
        assertEquals(msg.getBody(), msg2.getBody(), "Bodies are different");
        assertEquals(
               getProperty(msg, "favoriteColor"),
               getProperty(msg2, "favoriteColor"),
               "favoriteColors are different");
        assertEquals(
               getProperty(msg, "age"),
               getProperty(msg2, "age"),
               "ages are different");
        assertEquals(
               getProperty(msg, "distance"),
               getProperty(msg2, "distance"),
               "distances are different");
        assertEquals(
               getProperty(msg, "weight"),
               getProperty(msg2, "weight"),
               "weights are different");
        assertEquals(
               getProperty(msg, "male"),
               getProperty(msg2, "male"),
               "males are different");
        assertEquals(
               getProperty(msg, "birthdate"),
               getProperty(msg2, "birthdate"),
               "birthdates are different");
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

            assertTrue(invoked, "TestChatManagerListener wasn't invoked");
        }
        finally {
            chatManagerOne.removeChatListener(listener);
        }
    }
}
