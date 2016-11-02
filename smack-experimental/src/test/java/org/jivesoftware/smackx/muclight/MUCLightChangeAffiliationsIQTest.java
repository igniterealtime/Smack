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
package org.jivesoftware.smackx.muclight;

import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.muclight.element.MUCLightChangeAffiliationsIQ;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class MUCLightChangeAffiliationsIQTest {

    String stanza = "<iq " + "to='coven@muclight.shakespeare.lit' id='member1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
            + "<user affiliation='owner'>sarasa2@shakespeare.lit</user>"
            + "<user affiliation='member'>sarasa1@shakespeare.lit</user>"
            + "<user affiliation='none'>sarasa3@shakespeare.lit</user>" + "</query>" + "</iq>";

    @Test
    public void checkChangeAffiliationsMUCLightStanza() throws Exception {
        HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
        affiliations.put(JidCreate.from("sarasa2@shakespeare.lit"), MUCLightAffiliation.owner);
        affiliations.put(JidCreate.from("sarasa1@shakespeare.lit"), MUCLightAffiliation.member);
        affiliations.put(JidCreate.from("sarasa3@shakespeare.lit"), MUCLightAffiliation.none);

        MUCLightChangeAffiliationsIQ mucLightChangeAffiliationsIQ = new MUCLightChangeAffiliationsIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), affiliations);
        mucLightChangeAffiliationsIQ.setStanzaId("member1");

        Assert.assertEquals(mucLightChangeAffiliationsIQ.getTo(), "coven@muclight.shakespeare.lit");
        Assert.assertEquals(mucLightChangeAffiliationsIQ.getType(), IQ.Type.set);

        HashMap<Jid, MUCLightAffiliation> iqAffiliations = mucLightChangeAffiliationsIQ.getAffiliations();
        Assert.assertEquals(iqAffiliations.get(JidCreate.from("sarasa1@shakespeare.lit")), MUCLightAffiliation.member);
        Assert.assertEquals(iqAffiliations.get(JidCreate.from("sarasa2@shakespeare.lit")), MUCLightAffiliation.owner);
        Assert.assertEquals(iqAffiliations.get(JidCreate.from("sarasa3@shakespeare.lit")), MUCLightAffiliation.none);
    }

}
