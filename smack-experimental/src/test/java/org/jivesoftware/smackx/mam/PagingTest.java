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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StreamOpen;

import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.mam.element.MamVersion;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class PagingTest extends MamTest {

    private static final String pagingStanza = "<iq id='sarasa' type='set'>" + "<query xmlns='urn:xmpp:mam:2' queryid='testid'>"
            + "<x xmlns='jabber:x:data' type='submit'>" + "<field var='FORM_TYPE'>"
            + "<value>urn:xmpp:mam:2</value>" + "</field>" + "</x>" + "<set xmlns='http://jabber.org/protocol/rsm'>"
            + "<max>10</max>" + "</set>" + "</query>" + "</iq>";

    @Test
    public void checkPageQueryStanza() throws Exception {
        DataForm dataForm = getNewMamForm();
        int max = 10;
        RSMSet rsmSet = new RSMSet(max);

        MamQueryIQ mamQueryIQ = new MamQueryIQ(MamVersion.MAM2, queryId, dataForm);
        mamQueryIQ.setStanzaId("sarasa");
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.addExtension(rsmSet);

        assertEquals(mamQueryIQ.getDataForm(), dataForm);
        assertEquals(mamQueryIQ.getDataForm().getFields().get(0).getValues().get(0).toString(), "urn:xmpp:mam:2");
        assertEquals(pagingStanza, mamQueryIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

}
