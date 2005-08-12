/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smack;

import java.util.Date;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.SmackTestCase;


/**
 * Tests the chat functionality.
 * 
 * @author Gaston Dombiak
 */
public class ChatTest extends SmackTestCase {

    /**
     * Constructor for ChatTest.
     * @param arg0
     */
    public ChatTest(String arg0) {
        super(arg0);
    }

    public void testProperties() {
        try {
            Chat newChat = getConnection(0).createChat(getFullJID(1));
            Chat newChat2 = new Chat(getConnection(1), getFullJID(0), newChat.getThreadID());

            Message msg = newChat.createMessage();

            msg.setSubject("Subject of the chat");
            msg.setBody("Body of the chat");
            msg.setProperty("favoriteColor", "red");
            msg.setProperty("age", 30);
            msg.setProperty("distance", 30f);
            msg.setProperty("weight", 30d);
            msg.setProperty("male", true);
            msg.setProperty("birthdate", new Date());
            newChat.sendMessage(msg);

            Message msg2 = newChat2.nextMessage(2000);
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
