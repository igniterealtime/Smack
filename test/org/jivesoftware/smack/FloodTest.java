/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.smack;

import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Simple test to measure server performance.
 *
 * @author Gaston Dombiak
 */
public class FloodTest  extends SmackTestCase {

    public FloodTest(String arg0) {
        super(arg0);
    }

    public void testMessageFlood() {
        try {
            Chat chat11 = getConnection(0).createChat(getBareJID(1));
            Chat chat12 = new Chat(getConnection(1), getBareJID(0), chat11.getThreadID());

            Chat chat21 = getConnection(0).createChat(getBareJID(2));
            Chat chat22 = new Chat(getConnection(2), getBareJID(0), chat21.getThreadID());

            Chat chat31 = getConnection(0).createChat(getBareJID(3));
            Chat chat32 = new Chat(getConnection(3), getBareJID(0), chat31.getThreadID());

            for (int i=0; i<500; i++) {
                chat11.sendMessage("Hello_1" + i);
                chat21.sendMessage("Hello_2" + i);
                chat31.sendMessage("Hello_3" + i);
            }
            for (int i=0; i<500; i++) {
                assertNotNull("Some message was lost (" + i + ")", chat12.nextMessage(1000));
                assertNotNull("Some message was lost (" + i + ")", chat22.nextMessage(1000));
                assertNotNull("Some message was lost (" + i + ")", chat32.nextMessage(1000));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 4;
    }
}
