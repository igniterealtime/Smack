/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2003 Jive Software. All rights reserved.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import junit.framework.TestCase;

/**
 * 
 * 
 * @author Matt Tucker
 */
public class GroupChatInvitationTest extends TestCase {

    private XMPPConnection con1 = null;
    private XMPPConnection con2 = null;
    private PacketCollector collector = null;

    public void testInvitation() {
        try {
            GroupChatInvitation invitation = new GroupChatInvitation("test@chat.localhost");
            Message message = new Message("test2@" + con1.getHost());
            message.setBody("Group chat invitation!");
            message.addExtension(invitation);
            con1.sendPacket(message);

            Thread.sleep(250);

            Message result = (Message)collector.pollResult();
            assertNotNull("Message not delivered correctly.", result);

            GroupChatInvitation resultInvite = (GroupChatInvitation)result.getExtension("x",
                    "jabber:x:conference");

            assertEquals("Invitation not to correct room", "test@chat.localhost",
                    resultInvite.getRoomAddress());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            con1 = new XMPPConnection("localhost");
            con2 = new XMPPConnection("localhost");

            // Create the test accounts
            if (!con1.getAccountManager().supportsAccountCreation()) {
                fail("Server does not support account creation");
            }
            con1.getAccountManager().createAccount("test1", "test1");
            con2.getAccountManager().createAccount("test2", "test2");

            // Login with the test accounts
            con1.login("test1", "test1");
            con2.login("test2", "test2");

            // Register listener for groupchat invitations.
            PacketFilter filter = new PacketExtensionFilter("x", "jabber:x:conference");
            collector = con2.createPacketCollector(filter);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();

        // Delete the created accounts for the test
        con1.getAccountManager().deleteAccount();
        con2.getAccountManager().deleteAccount();

        // Close all the connections
        con1.close();
        con2.close();
    }
}