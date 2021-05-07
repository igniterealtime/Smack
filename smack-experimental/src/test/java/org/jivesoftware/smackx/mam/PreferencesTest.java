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

import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ.DefaultBehavior;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreferencesTest {

    private static final String retrievePrefsStanzaExample = "<iq id='sarasa' type='get'>" + "<prefs xmlns='" + MamElements.MAM2_NAMESPACE
            + "'/>" + "</iq>";

    private static final String updatePrefsStanzaExample = "<iq id='sarasa' type='set'>" + "<prefs xmlns='" + MamElements.MAM2_NAMESPACE
            + "' default='roster'>" + "<always>" + "<jid>romeo@montague.lit</jid>" + "<jid>other@montague.lit</jid>"
            + "</always>" + "<never>" + "<jid>montague@montague.lit</jid>" + "</never>" + "</prefs>" + "</iq>";

    @Test
    public void checkRetrievePrefsStanza() throws Exception {
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ(MamElements.MAM2_NAMESPACE);
        mamPrefIQ.setStanzaId("sarasa");
        assertEquals(mamPrefIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString(), retrievePrefsStanzaExample);
    }

    @Test
    public void checkUpdatePrefsStanza() throws Exception {
        List<Jid> alwaysJids = new ArrayList<>();
        alwaysJids.add(JidCreate.from("romeo@montague.lit"));
        alwaysJids.add(JidCreate.from("other@montague.lit"));

        List<Jid> neverJids = new ArrayList<>();
        neverJids.add(JidCreate.from("montague@montague.lit"));

        MamPrefsIQ mamPrefIQ =  new MamPrefsIQ(MamElements.MAM2_NAMESPACE, alwaysJids, neverJids, DefaultBehavior.roster);
        mamPrefIQ.setStanzaId("sarasa");
        assertEquals(mamPrefIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString(), updatePrefsStanzaExample);
    }

}
