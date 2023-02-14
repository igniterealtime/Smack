/**
 *
 * Copyright 2016 Fernando Ramirez
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class MamQueryIQProviderTest {

    private static final String exampleMamQueryIQ1 = "<iq type='set' id='query4'>" + "<query xmlns='urn:xmpp:mam:2' queryid='test'>"
            + "<x xmlns='jabber:x:data' type='submit'>" + "<field type='hidden' var='FORM_TYPE'>"
            + "<value>urn:xmpp:mam:2</value>" + "</field>"
            + "<field type='text-single' var='urn:example:xmpp:free-text-search'>"
            + "<value>Where arth thou, my Juliet?</value>" + "</field>"
            + "<field type='text-single' var='urn:example:xmpp:stanza-content'>"
            + "<value>{http://jabber.org/protocol/mood}mood/lonely</value>" + "</field>" + "</x>" + "</query>"
            + "</iq>";

    private static final String exampleMamQueryIQ2 = "<iq type='result' id='form1'>" + "<query xmlns='urn:xmpp:mam:2'>"
            + "<x xmlns='jabber:x:data' type='form'>" + "<field type='hidden' var='FORM_TYPE'>"
            + "<value>urn:xmpp:mam:2</value>" + "</field>" + "<field type='jid-single' var='with'/>"
            + "<field type='text-single' var='start'/>" + "<field type='text-single' var='end'/>"
            + "<field type='text-single' var='urn:example:xmpp:free-text-search'/>"
            + "<field type='text-single' var='urn:example:xmpp:stanza-content'/>" + "</x>" + "</query>" + "</iq>";

    @Test
    public void checkMamQueryIQProvider() throws Exception {
        // example 1
        IQ iq1 = PacketParserUtils.parseStanza(exampleMamQueryIQ1);
        MamQueryIQ mamQueryIQ1 = (MamQueryIQ) iq1;

        assertEquals(mamQueryIQ1.getType(), IQ.Type.set);
        assertEquals(mamQueryIQ1.getQueryId(), "test");

        DataForm dataForm1 = (DataForm) mamQueryIQ1.getExtension(DataForm.NAMESPACE);
        assertEquals(dataForm1.getType(), DataForm.Type.submit);

        List<FormField> fields1 = dataForm1.getFields();
        assertEquals(fields1.get(0).getType(), FormField.Type.hidden);
        assertEquals(fields1.get(1).getType(), FormField.Type.text_single);
        assertEquals(fields1.get(1).getValues().get(0).toString(), "Where arth thou, my Juliet?");
        assertEquals(fields1.get(2).getValues().get(0).toString(), "{http://jabber.org/protocol/mood}mood/lonely");

        // example2
        IQ iq2 = PacketParserUtils.parseStanza(exampleMamQueryIQ2);
        MamQueryIQ mamQueryIQ2 = (MamQueryIQ) iq2;

        assertEquals(mamQueryIQ2.getType(), IQ.Type.result);
        assertNull(mamQueryIQ2.getQueryId());

        DataForm dataForm2 = (DataForm) mamQueryIQ2.getExtension(DataForm.NAMESPACE);
        assertEquals(dataForm2.getType(), DataForm.Type.form);

        List<FormField> fields2 = dataForm2.getFields();
        assertEquals(fields2.get(0).getValues().get(0).toString(), "urn:xmpp:mam:2");
        assertTrue(fields2.get(0).getValues().size() == 1);
        assertEquals(fields2.get(1).getType(), FormField.Type.jid_single);
        assertEquals(fields2.get(2).getType(), FormField.Type.text_single);
        assertEquals(fields2.get(2).getValues(), new ArrayList<>());
        assertEquals(fields2.get(4).getFieldName(), "urn:example:xmpp:free-text-search");
    }

}
