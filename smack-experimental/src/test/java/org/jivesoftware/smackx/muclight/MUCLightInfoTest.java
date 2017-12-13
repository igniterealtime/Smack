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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.muclight.element.MUCLightGetInfoIQ;
import org.jivesoftware.smackx.muclight.element.MUCLightInfoIQ;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class MUCLightInfoTest {

    private static final String exampleWithVersion = "<iq to='coven@muclight.shakespeare.lit' id='getinfo1' type='get'>"
            + "<query xmlns='urn:xmpp:muclight:0#info'>" + "<version>abcdefg</version>" + "</query>" + "</iq>";

    private static final String exampleWithoutVersion = "<iq to='coven@muclight.shakespeare.lit' id='getinfo1' type='get'>"
            + "<query xmlns='urn:xmpp:muclight:0#info'>" + "</query>" + "</iq>";

    private static final String exampleInfoResult = "<iq from='coven@muclight.shakespeare.lit' to='cronel@shakespeare.lit/desktop' id='getinfo1' type='result'>"
            + "<query xmlns='urn:xmpp:muclight:0#info'>" + "<version>123456</version>" + "<configuration>"
            + "<roomname>test</roomname>" + "</configuration>" + "<occupants>"
            + "<user affiliation='owner'>john@test.com</user>" + "<user affiliation='member'>charlie@test.com</user>"
            + "<user affiliation='member'>pep@test.com</user>" + "</occupants>" + "</query>" + "</iq>";

    @Test
    public void checkMUCLightGetInfoIQStanzaWithVersion() throws Exception {
        MUCLightGetInfoIQ mucLightGetInfoIQWithVersion = new MUCLightGetInfoIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), "abcdefg");
        mucLightGetInfoIQWithVersion.setStanzaId("getinfo1");
        Assert.assertEquals(mucLightGetInfoIQWithVersion.toXML().toString(), exampleWithVersion);
    }

    @Test
    public void checkMUCLightGetInfoIQStanzaWithoutVersion() throws Exception {
        MUCLightGetInfoIQ mucLightGetInfoIQWithoutVersion = new MUCLightGetInfoIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), null);
        mucLightGetInfoIQWithoutVersion.setStanzaId("getinfo1");
        Assert.assertEquals(mucLightGetInfoIQWithoutVersion.toXML().toString(), exampleWithoutVersion);
    }

    @Test
    public void checkMUCLightInfoResult() throws Exception {
        IQ iqInfoResult = PacketParserUtils.parseStanza(exampleInfoResult);
        MUCLightInfoIQ mucLightInfoResponseIQ = (MUCLightInfoIQ) iqInfoResult;
        Assert.assertEquals(mucLightInfoResponseIQ.getVersion(), "123456");
        Assert.assertEquals(mucLightInfoResponseIQ.getConfiguration().getRoomName(), "test");
        Assert.assertEquals(mucLightInfoResponseIQ.getOccupants().size(), 3);
        Assert.assertEquals(mucLightInfoResponseIQ.getOccupants().get(JidCreate.from("john@test.com")),
                MUCLightAffiliation.owner);
        Assert.assertEquals(mucLightInfoResponseIQ.getOccupants().get(JidCreate.from("charlie@test.com")),
                MUCLightAffiliation.member);
        Assert.assertEquals(mucLightInfoResponseIQ.getOccupants().get(JidCreate.from("pep@test.com")),
                MUCLightAffiliation.member);
    }

}
