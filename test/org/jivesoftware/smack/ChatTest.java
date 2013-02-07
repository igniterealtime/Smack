/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

import java.util.Date;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.filter.ThreadFilter;


/**
 * Tests the chat functionality.
 *
 * @author Gaston Dombiak
 */
public class ChatTest extends SmackTestCase {

    public ChatTest(String arg0) {
        super(arg0);
    }

    public void testProperties() {
        try {
            Chat newChat = getConnection(0).getChatManager().createChat(getFullJID(1), null);
            PacketCollector collector = getConnection(1)
                    .createPacketCollector(new ThreadFilter(newChat.getThreadID()));

            Message msg = new Message();

            msg.setSubject("Subject of the chat");
            msg.setBody("Body of the chat");
            msg.setProperty("favoriteColor", "red");
            msg.setProperty("age", 30);
            msg.setProperty("distance", 30f);
            msg.setProperty("weight", 30d);
            msg.setProperty("male", true);
            msg.setProperty("birthdate", new Date());
            newChat.sendMessage(msg);

            Message msg2 = (Message) collector.nextResult(2000);

            assertNotNull("No message was received", msg2);
            assertEquals("Subjects are different", msg.getSubject(), msg2.getSubject());
            assertEquals("Bodies are different", msg.getBody(), msg2.getBody());
            assertEquals(
                    "favoriteColors are different",
                    msg.getProperty("favoriteColor"),
                    msg2.getProperty("favoriteColor"));
            assertEquals(
                    "ages are different",
                    msg.getProperty("age"),
                    msg2.getProperty("age"));
            assertEquals(
                    "distances are different",
                    msg.getProperty("distance"),
                    msg2.getProperty("distance"));
            assertEquals(
                    "weights are different",
                    msg.getProperty("weight"),
                    msg2.getProperty("weight"));
            assertEquals(
                    "males are different",
                    msg.getProperty("male"),
                    msg2.getProperty("male"));
            assertEquals(
                    "birthdates are different",
                    msg.getProperty("birthdate"),
                    msg2.getProperty("birthdate"));
        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
