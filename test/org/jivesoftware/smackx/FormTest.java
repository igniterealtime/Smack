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

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import junit.framework.TestCase;

/**
 * Tests the DataForms extensions.
 * 
 * @author Gaston Dombiak
 */
public class FormTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;

    private String user1 = null;
    private String user2 = null;

    /**
     * Constructor for FormTest.
     * @param arg0
     */
    public FormTest(String arg0) {
        super(arg0);
    }

    /**
     * 1. Create a form to fill out and send it to the other user
     * 2. Retrieve the form to fill out, complete it and return it to the requestor
     * 3. Retrieve the completed form and check that everything is OK
     */
    public void testFilloutForm() {
        Form formToSend = new Form(Form.TYPE_FORM);
        formToSend.setInstructions(
            "Fill out this form to report your case.\nThe case will be created automatically.");
        formToSend.setTitle("Case configurations");
        // Add a hidden variable
        FormField field = new FormField("hidden_var");
        field.setType(FormField.TYPE_HIDDEN);
        field.addValue("Some value for the hidden variable");
        formToSend.addField(field);
        // Add a fixed variable
        field = new FormField();
        field.addValue("Section 1: Case description");
        formToSend.addField(field);
        // Add a text-single variable
        field = new FormField("name");
        field.setLabel("Enter a name for the case");
        field.setType(FormField.TYPE_TEXT_SINGLE);
        formToSend.addField(field);
        // Add a text-multi variable
        field = new FormField("description");
        field.setLabel("Enter a description");
        field.setType(FormField.TYPE_TEXT_MULTI);
        formToSend.addField(field);

        // Create the chats between the two participants
        Chat chat = conn1.createChat(user2);
        Chat chat2 = new Chat(conn2, user1, chat.getThreadID());

        Message msg = chat.createMessage();
        msg.setBody("To enter a case please fill out this form and send it back to me");
        msg.addExtension(formToSend.getDataFormToSend());

        try {
            // Send the message with the form to fill out
            chat.sendMessage(msg);

            // Get the message with the form to fill out
            Message msg2 = chat2.nextMessage();
            // Retrieve the form to fill out
            Form formToRespond = Form.getFormFrom(msg2);
            assertNotNull(formToRespond);
            assertNotNull(formToRespond.getField("name"));
            assertNotNull(formToRespond.getField("description"));
            // Obtain the form to send with the replies
            Form completedForm = formToRespond.createAnswerForm();
            assertNotNull(completedForm.getField("hidden_var"));
            completedForm.setAnswer("name", "Credit card number invalid");
            completedForm.setAnswer(
                "description",
                "The ATM says that my credit card number is invalid. What's going on?");
            msg2 = chat2.createMessage();
            msg2.setBody("To enter a case please fill out this form and send it back to me");
            msg2.addExtension(completedForm.getDataFormToSend());
            // Send the message with the completed form
            chat2.sendMessage(msg2);

            // Get the message with the completed form
            Message msg3 = chat.nextMessage();
            // Retrieve the completed form
            completedForm = Form.getFormFrom(msg3);
            assertNotNull(completedForm);
            assertNotNull(completedForm.getField("name"));
            assertNotNull(completedForm.getField("description"));
            assertEquals(
                completedForm.getField("name").getValues().next(),
                "Credit card number invalid");

        }
        catch (XMPPException ex) {
            fail(ex.getMessage());
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("localhost");
            conn2 = new XMPPConnection("localhost");

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // Delete the created accounts for the test
        conn1.getAccountManager().deleteAccount();
        conn2.getAccountManager().deleteAccount();

        // Close all the connections
        conn1.close();
        conn2.close();
    }

}
