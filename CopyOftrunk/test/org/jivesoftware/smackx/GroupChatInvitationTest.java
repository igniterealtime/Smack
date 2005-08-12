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
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;

/**
 * 
 * 
 * @author Matt Tucker
 */
public class GroupChatInvitationTest extends SmackTestCase {

    private PacketCollector collector = null;

    /**
     * Constructor for GroupChatInvitationTest.
     * @param arg0
     */
    public GroupChatInvitationTest(String arg0) {
        super(arg0);
    }

    public void testInvitation() {
        try {
            GroupChatInvitation invitation = new GroupChatInvitation("test@" + getChatDomain());
            Message message = new Message(getBareJID(1));
            message.setBody("Group chat invitation!");
            message.addExtension(invitation);
            getConnection(0).sendPacket(message);

            Thread.sleep(250);

            Message result = (Message)collector.pollResult();
            assertNotNull("Message not delivered correctly.", result);

            GroupChatInvitation resultInvite = (GroupChatInvitation)result.getExtension("x",
                    "jabber:x:conference");

            assertEquals("Invitation not to correct room", "test@" + getChatDomain(),
                    resultInvite.getRoomAddress());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        // Register listener for groupchat invitations.
        PacketFilter filter = new PacketExtensionFilter("x", "jabber:x:conference");
        collector = getConnection(1).createPacketCollector(filter);
    }

    protected void tearDown() throws Exception {
        // Cancel the packet collector so that no more results are queued up
        collector.cancel();

        super.tearDown();
    }

    protected int getMaxConnections() {
        return 2;
    }
}