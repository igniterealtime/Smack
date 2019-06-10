/**
 *
 * Copyright 2016 Fernando Ramirez, 2018-2019 Florian Schmaus
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
package org.jivesoftware.smackx.mam;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.packet.StreamOpen;

import org.jivesoftware.smackx.mam.MamManager.MamQueryArgs;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class RetrieveFormFieldsTest extends MamTest {

    private static final String retrieveFormFieldStanza = "<iq id='sarasa' type='get'>" + "<query xmlns='" + MamElements.NAMESPACE
            + "' queryid='testid'></query>" + "</iq>";

    private static final String additionalFieldsStanza = "<x xmlns='jabber:x:data' type='submit'>" + "<field var='FORM_TYPE' type='hidden'>"
            + "<value>" + MamElements.NAMESPACE + "</value>" + "</field>"
            + "<field var='urn:example:xmpp:free-text-search'>" + "<value>Hi</value>" + "</field>"
            + "<field var='urn:example:xmpp:stanza-content' type='jid-single'>" + "<value>Hi2</value>" + "</field>"
            + "</x>";

    @Test
    public void checkRetrieveFormFieldsStanza() throws Exception {
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId);
        mamQueryIQ.setStanzaId("sarasa");

        assertEquals(retrieveFormFieldStanza, mamQueryIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkAddAdditionalFieldsStanza() throws Exception {
        FormField field1 = FormField.builder("urn:example:xmpp:free-text-search")
                        .setType(FormField.Type.text_single)
                        .addValue("Hi")
                        .build();

        FormField field2 = FormField.builder("urn:example:xmpp:stanza-content")
                        .setType(FormField.Type.jid_single)
                        .addValue("Hi2")
                        .build();

        MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
                        .withAdditionalFormField(field1)
                        .withAdditionalFormField(field2)
                        .build();
        DataForm dataForm = mamQueryArgs.getDataForm();

        String dataFormResult = dataForm.toXML().toString();

        assertXmlSimilar(additionalFieldsStanza, dataFormResult);
    }

}
