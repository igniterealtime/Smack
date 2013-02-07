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
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
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
package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests creating new MUC rooms.
 *
 * @author Gaston Dombiak
 */
public class MultiUserChatCreationTest extends SmackTestCase {

    private String room;

    /**
     * Constructor for MultiUserChatCreationTest.
     * @param arg0
     */
    public MultiUserChatCreationTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests creating a new "Reserved Room".
     */
    public void testCreateReservedRoom() {
        MultiUserChat muc = new MultiUserChat(getConnection(0), room);

        try {
            // Create the room
            muc.create("testbot1");

            // Get the the room's configuration form
            Form form = muc.getConfigurationForm();
            assertNotNull("No room configuration form", form);
            // Create a new form to submit based on the original form
            Form submitForm = form.createAnswerForm();
            // Add default answers to the form to submit
            for (Iterator<FormField> fields = form.getFields(); fields.hasNext();) {
                FormField field = fields.next();
                if (!FormField.TYPE_HIDDEN.equals(field.getType())
                    && field.getVariable() != null) {
                    // Sets the default value as the answer
                    submitForm.setDefaultAnswer(field.getVariable());
                }
            }
            List<String> owners = new ArrayList<String>();
            owners.add(getBareJID(0));
            submitForm.setAnswer("muc#roomconfig_roomowners", owners);

            // Update the new room's configuration
            muc.sendConfigurationForm(submitForm);

            // Destroy the new room
            muc.destroy("The room has almost no activity...", null);

        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Tests creating a new "Instant Room".
     */
    public void testCreateInstantRoom() {
        MultiUserChat muc = new MultiUserChat(getConnection(0), room);

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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 2;
    }

    protected void setUp() throws Exception {
        super.setUp();
        room = "fruta124@" + getMUCDomain();
    }
}
