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
package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;

import junit.framework.TestCase;

/**
 * Represents...
 *
 * @author Gaston Dombiak
 */
public class MultiUserChatTest extends TestCase {

    private XMPPConnection conn1 = null;

    /**
     * Constructor for MultiUserChatTest.
     * @param arg0
     */
    public MultiUserChatTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests joining and leaving a room.
     *
     */
    public void testJoin() {
        MultiUserChat muc = new MultiUserChat(conn1, "dpspanish@muc.jabber.org");

        // Join the room
        try {
            muc.join("testbot", SmackConfiguration.getPacketReplyTimeout(), null, 0, -1, -1, null);
        }
        catch (XMPPException e) {
            fail(e.getMessage());
        }

        // Leave the room
        muc.leave();
    }

    /**
     * Tests joining a non-existant room.
     *
     */
    public void testCreateRoom() {
        MultiUserChat muc = new MultiUserChat(conn1, "fruta124@muc.jabber.org");

        // Join the room
        try {
            muc.join("testbot", SmackConfiguration.getPacketReplyTimeout(), null, 0, -1, -1, null);
            // TODO Check that the returned presence contains the status code 201
            
            Form form = muc.getConfigurationForm();
            assertNotNull("No room configuration form", form);
            Form submitForm = form.createAnswerForm();
            muc.submitConfigurationForm(submitForm);
            
        }
        catch (XMPPException e) {
            // TODO Check for the error code 404
            System.out.println(e.getMessage());
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("jabber.org");

            // Login with the test accounts
            conn1.login("jatul", "jatul");

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // Close the connection
        conn1.close();
    }
}
