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

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Presence;

import junit.framework.TestCase;

/**
 * Tests the new MUC functionalities.
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
    public void testJoinAndInvite() {
        MultiUserChat muc = new MultiUserChat(conn1, "dpspanish@muc.jabber.org");

        
        try {
            // Join the room
            muc.join("testbot", SmackConfiguration.getPacketReplyTimeout(), null, 0, -1, -1, null);
            
            // Invite another user to join to the room
            muc.invite("gdombiak@jabber.org","Meet me in this excellent room");
            
            muc.changeAvailabilityStatus("Gone to have lunch", Presence.Mode.AWAY);
            
            muc.changeNickname("testbot2");
            
        }
        catch (XMPPException e) {
            fail(e.getMessage());
        }

        // Leave the room
        muc.leave();
    }

    /**
     * Tests creating a new "Reserved Room".
     */
    public void testCreateReservedRoom() {
        MultiUserChat muc = new MultiUserChat(conn1, "fruta124@muc.jabber.org");

        try {
            // Create the room
            muc.create("testbot");

            // Get the the room's configuration form
            Form form = muc.getConfigurationForm();
            assertNotNull("No room configuration form", form);
            // Create a new form to submit based on the original form
            Form submitForm = form.createAnswerForm();
            // Add default answers to the form to submit
            for (Iterator fields=form.getFields();fields.hasNext();) {
                FormField field = (FormField) fields.next();
                if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
                    // Add the field values to a List
                    List values = new ArrayList();
                    for (Iterator it=field.getValues();it.hasNext();) {
                        values.add((String)it.next());
                    }
                    // Add a new answer to form to submit
                    submitForm.addAnswer(field.getVariable(), values);
                }
            }
            // Update the new room's configuration
            muc.sendConfigurationForm(submitForm);
         
            // Destroy the new room
            muc.destroy("The room has almost no activity...", null);   
        }
        catch (XMPPException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Tests creating a new "Instant Room".
     */
    public void testCreateInstantRoom() {
        MultiUserChat muc = new MultiUserChat(conn1, "fruta124@muc.jabber.org");

        try {
            // Create the room
            muc.create("testbot");

            // Send an empty room configuration form which indicates that we want
            // an instant room
            muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
         
            // Destroy the new room
            muc.destroy("The room has almost no activity...", null);   
        }
        catch (XMPPException e) {
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
