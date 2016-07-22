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

import java.lang.reflect.Method;

import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.junit.Test;

import org.junit.Assert;

public class PagingTest extends MamTest {

    String pagingStanza = "<iq id='sarasa' type='set'>" + "<query xmlns='urn:xmpp:mam:1' queryid='testid'>"
            + "<x xmlns='jabber:x:data' type='submit'>" + "<field var='FORM_TYPE' type='hidden'>"
            + "<value>urn:xmpp:mam:1</value>" + "</field>" + "</x>" + "<set xmlns='http://jabber.org/protocol/rsm'>"
            + "<max>10</max>" + "</set>" + "</query>" + "</iq>";

    @Test
    public void checkPageQueryStanza() throws Exception {
        Method methodPreparePageQuery = MamManager.class.getDeclaredMethod("preparePageQuery", MamQueryIQ.class,
                RSMSet.class);
        methodPreparePageQuery.setAccessible(true);

        DataForm dataForm = getNewMamForm();
        int max = 10;
        RSMSet rsmSet = new RSMSet(max);

        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, dataForm);
        mamQueryIQ.setStanzaId("sarasa");

        methodPreparePageQuery.invoke(mamManager, mamQueryIQ, rsmSet);

        Assert.assertEquals(mamQueryIQ.getDataForm(), dataForm);
        Assert.assertEquals(mamQueryIQ.getDataForm().getFields().get(0).getValues().get(0), "urn:xmpp:mam:1");
        Assert.assertEquals(mamQueryIQ.toXML().toString(), pagingStanza);
    }

}
