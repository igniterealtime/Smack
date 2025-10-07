/*
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.jivesoftware.smack.packet.StreamOpen;

import org.jivesoftware.smackx.muclight.element.MUCLightSetConfigsIQ;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class MUCLightSetConfigsIQTest {

    private static final String setConfigsIQExample = "<iq to='coven@muclight.shakespeare.lit' id='conf1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#configuration'>" + "<roomname>A Darker Cave</roomname>"
            + "<color>blue</color>" + "</query>" + "</iq>";

    private static final String changeRoomNameIQExample = "<iq to='coven@muclight.shakespeare.lit' id='roomName1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#configuration'>" + "<roomname>A Darker Cave</roomname>" + "</query>"
            + "</iq>";

    private static final String changeSubjectIQExample = "<iq to='coven@muclight.shakespeare.lit' id='subject1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#configuration'>" + "<subject>To be or not to be?</subject>"
            + "</query>" + "</iq>";

    @Test
    public void checkSetConfigsStanza() throws Exception {
        HashMap<String, String> customConfigs = new HashMap<>();
        customConfigs.put("color", "blue");

        MUCLightSetConfigsIQ mucLightSetConfigsIQ = new MUCLightSetConfigsIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), "A Darker Cave", customConfigs);
        mucLightSetConfigsIQ.setStanzaId("conf1");

        assertEquals(setConfigsIQExample, mucLightSetConfigsIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkChangeRoomNameStanza() throws Exception {
        MUCLightSetConfigsIQ mucLightChangeRoomNameIQ = new MUCLightSetConfigsIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), "A Darker Cave", null);
        mucLightChangeRoomNameIQ.setStanzaId("roomName1");

        assertEquals(changeRoomNameIQExample, mucLightChangeRoomNameIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkChangeSubjectStanza() throws Exception {
        MUCLightSetConfigsIQ mucLightChangeSubjectIQ = new MUCLightSetConfigsIQ(
                JidCreate.from("coven@muclight.shakespeare.lit"), null, "To be or not to be?", null);
        mucLightChangeSubjectIQ.setStanzaId("subject1");

        assertEquals(changeSubjectIQExample, mucLightChangeSubjectIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

}
