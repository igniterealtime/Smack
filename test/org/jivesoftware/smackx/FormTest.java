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
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Tests the DataForms extensions.
 * 
 * @author Gaston Dombiak
 */
public class FormTest extends SmackTestCase {

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
        // Add a boolean variable
        field = new FormField("time");
        field.setLabel("Is this your first case?");
        field.setType(FormField.TYPE_BOOLEAN);
        formToSend.addField(field);
        // Add a text variable where an int value is expected
        field = new FormField("age");
        field.setLabel("How old are you?");
        field.setType(FormField.TYPE_TEXT_SINGLE);
        formToSend.addField(field);

        // Create the chats between the two participants
        Chat chat = getConnection(0).getChatManager().createChat(getBareJID(1), null);
        PacketCollector collector = getConnection(0).createPacketCollector(
                new ThreadFilter(chat.getThreadID()));
        PacketCollector collector2 = getConnection(1).createPacketCollector(
                new ThreadFilter(chat.getThreadID()));

        Message msg = new Message();
        msg.setBody("To enter a case please fill out this form and send it back to me");
        msg.addExtension(formToSend.getDataFormToSend());

        try {
            // Send the message with the form to fill out
            chat.sendMessage(msg);

            // Get the message with the form to fill out
            Message msg2 = (Message)collector2.nextResult(2000);
            assertNotNull("Messge not found", msg2);
            // Retrieve the form to fill out
            Form formToRespond = Form.getFormFrom(msg2);
            assertNotNull(formToRespond);
            assertNotNull(formToRespond.getField("name"));
            assertNotNull(formToRespond.getField("description"));
            // Obtain the form to send with the replies
            Form completedForm = formToRespond.createAnswerForm();
            assertNotNull(completedForm.getField("hidden_var"));
            // Check that a field of type String does not accept booleans
            try {
                completedForm.setAnswer("name", true);
                fail("A boolean value was set to a field of type String");
            }
            catch (IllegalArgumentException e) {
            }
            completedForm.setAnswer("name", "Credit card number invalid");
            completedForm.setAnswer(
                "description",
                "The ATM says that my credit card number is invalid. What's going on?");
            completedForm.setAnswer("time", true);
            completedForm.setAnswer("age", 20);
            // Create a new message to send with the completed form
            msg2 = new Message();
            msg2.setTo(msg.getFrom());
            msg2.setThread(msg.getThread());
            msg2.setType(Message.Type.chat);
            msg2.setBody("To enter a case please fill out this form and send it back to me");
            // Add the completed form to the message
            msg2.addExtension(completedForm.getDataFormToSend());
            // Send the message with the completed form
            getConnection(1).sendPacket(msg2);

            // Get the message with the completed form
            Message msg3 = (Message) collector.nextResult(2000);
            assertNotNull("Messge not found", msg3);
            // Retrieve the completed form
            completedForm = Form.getFormFrom(msg3);
            assertNotNull(completedForm);
            assertNotNull(completedForm.getField("name"));
            assertNotNull(completedForm.getField("description"));
            assertEquals(
                completedForm.getField("name").getValues().next(),
                "Credit card number invalid");
            assertNotNull(completedForm.getField("time"));
            assertNotNull(completedForm.getField("age"));
            assertEquals("The age is bad", "20", completedForm.getField("age").getValues().next());

        }
        catch (XMPPException ex) {
            fail(ex.getMessage());
        }
        finally {
            collector.cancel();
            collector2.cancel();
        }
    }

    protected int getMaxConnections() {
        return 2;
    }

}
