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

package org.jivesoftware.smackx.commands;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;

/**
 * AdHocCommand tests.
 *
 * @author Matt Tucker
 */
public class AdHocCommandDiscoTest extends SmackTestCase {

    /**
     * Constructor for test.
     * @param arg0 argument.
     */
    public AdHocCommandDiscoTest(String arg0) {
        super(arg0);
    }

    public void testAdHocCommands() {
        try {
            AdHocCommandManager manager1 = AdHocCommandManager.getAddHocCommandsManager(getConnection(0));
            manager1.registerCommand("test", "test node", LocalCommand.class);

            manager1.registerCommand("test2", "test node", new LocalCommandFactory() {
                public LocalCommand getInstance() throws InstantiationException, IllegalAccessException {
                    return new LocalCommand() {
                        public boolean isLastStage() {
                            return true;
                        }

                        public boolean hasPermission(String jid) {
                            return true;
                        }

                        public void execute() throws XMPPException {
                            Form result = new Form(Form.TYPE_RESULT);
                            FormField resultField = new FormField("test2");
                            resultField.setLabel("test node");
                            resultField.addValue("it worked");
                            result.addField(resultField);
                            setForm(result);
                        }

                        public void next(Form response) throws XMPPException {
                            //
                        }

                        public void complete(Form response) throws XMPPException {
                            //
                        }

                        public void prev() throws XMPPException {
                            //
                        }

                        public void cancel() throws XMPPException {
                            //
                        }
                    };
                }
            });
            
            AdHocCommandManager manager2 = AdHocCommandManager.getAddHocCommandsManager(getConnection(1));
            DiscoverItems items = manager2.discoverCommands(getFullJID(0));

            assertTrue("Disco for command test failed", items.getItems().next().getNode().equals("test"));

            RemoteCommand command = manager2.getRemoteCommand(getFullJID(0), "test2");
            command.execute();
            assertEquals("Disco for command test failed", command.getForm().getField("test2").getValues().next(), "it worked");
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected int getMaxConnections() {
        return 2;
    }
}