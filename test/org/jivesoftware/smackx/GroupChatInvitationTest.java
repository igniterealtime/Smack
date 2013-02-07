/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2003 Jive Software. All rights reserved.
 * ====================================================================
/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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