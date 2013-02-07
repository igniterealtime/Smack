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
