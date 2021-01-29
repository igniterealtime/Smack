/**
 *
 * Copyright 2021 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.xdata.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class DataFormProviderTest {

    @Test
    public void testRetrieveFieldTypeFromReported() throws XmlPullParserException, IOException, SmackParsingException {

        String firstForm =
                        "<x xmlns='jabber:x:data' type='form'>" +
                        "  <title>Advanced User Search</title>" +
                        "  <instructions>The following fields are available for searching. Wildcard (*) characters are allowed as part of the query.</instructions>" +
                        "  <field var='FORM_TYPE' type='hidden'>" +
                        "    <value>jabber:iq:search</value>" +
                        "  </field>" +
                        "  <field label='Search' var='search'>" +
                        "    <required/>" +
                        "  </field>" +
                        "  <field label='Username' var='Username' type='boolean'>" +
                        "    <value>true</value>" +
                        "  </field>" +
                        "  <field label='Name' var='Name' type='boolean'>" +
                        "    <value>true</value>" +
                        "  </field>" +
                        "  <field label='Email' var='Email' type='boolean'>" +
                        "    <value>true</value>" +
                        "  </field>" +
                        "</x>";
        XmlPullParser parser = PacketParserUtils.getParserFor(firstForm);
        DataForm firstDataForm = DataFormProvider.INSTANCE.parse(parser);
        FormField usernameFormField = firstDataForm.getField("Username");
        assertEquals(FormField.Type.bool, usernameFormField.getType());

        String secondForm =
                        "<x xmlns='jabber:x:data' type='result'>" +
                        "  <field var='FORM_TYPE' type='hidden'/>" +
                        "  <reported>" +
                        "    <field var='jid' type='jid-single' label='JID'/>" +
                        "    <field var='Username' type='text-single' label='Username'/>" +
                        "    <field var='Name' type='text-single' label='Name'/>" +
                        "    <field var='Email' type='text-single' label='Email'/>" +
                        "  </reported>" +
                        "  <item>" +
                        "    <field var='Email'>" +
                        "      <value>" +
                        "        0" +
                        "      </value>" +
                        "    </field>" +
                        "    <field var='jid'>" +
                        "      <value>frank@orphu</value>" +
                        "    </field>" +
                        "    <field var='Username'>" +
                        "      <value>" +
                        "        frank" +
                        "      </value>" +
                        "    </field>" +
                        "    <field var='Name'>" +
                        "      <value>" +
                        "        0" +
                        "      </value>" +
                        "    </field>" +
                        "  </item>" +
                        "  <item>" +
                        "    <field var='Email'>" +
                        "      <value>" +
                        "      </value>" +
                        "    </field>" +
                        "    <field var='jid'>" +
                        "      <value>frank2@orphu</value>" +
                        "    </field>" +
                        "    <field var='Username'>" +
                        "      <value>" +
                        "        frank2" +
                        "      </value>" +
                        "    </field>" +
                        "    <field var='Name'>" +
                        "      <value>" +
                        "      </value>" +
                        "    </field>" +
                        "  </item>" +
                        "</x>";
        parser = PacketParserUtils.getParserFor(secondForm);
        DataForm secondDataForm = DataFormProvider.INSTANCE.parse(parser);
        List<DataForm.Item> items = secondDataForm.getItems();
        assertEquals(2, items.size());
    }

}
