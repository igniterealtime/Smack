/**
 *
 * Copyright 2016 Fernando Ramirez, 2018-2020 Florian Schmaus
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

import org.jivesoftware.smackx.mam.MamManager.MamQueryArgs;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class ResultsLimitTest extends MamTest {

    private static final String resultsLimitStanza = "<iq id='sarasa' type='set'>" + "<query xmlns='urn:xmpp:mam:2' queryid='testid'>"
            + "<x xmlns='jabber:x:data' type='submit'>" + "<field var='FORM_TYPE'>" + "<value>"
            + MamElements.NAMESPACE + "</value>" + "</field>" + "</x>" + "<set xmlns='http://jabber.org/protocol/rsm'>"
            + "<max>10</max>" + "</set>" + "</query>" + "</iq>";

    @Test
    public void checkResultsLimit() throws Exception {
        DataForm dataForm = getNewMamForm();
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setStanzaId("sarasa");

        MamQueryArgs mamQueryArgs = MamQueryArgs.builder().setResultPageSize(10).build();
        mamQueryArgs.maybeAddRsmSet(mamQueryIQ);
        assertEquals(resultsLimitStanza, mamQueryIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

}
