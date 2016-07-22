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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.junit.Assert;

public class PreferencesTest extends MamTest {

    String retrievePrefsStanzaExample = "<iq id='sarasa' type='get'>" + "<prefs xmlns='" + MamElements.NAMESPACE
            + "'></prefs>" + "</iq>";

    String updatePrefsStanzaExample = "<iq id='sarasa' type='set'>" + "<prefs xmlns='" + MamElements.NAMESPACE
            + "' default='roster'>" + "<always>" + "<jid>romeo@montague.lit</jid>" + "<jid>other@montague.lit</jid>"
            + "</always>" + "<never>" + "<jid>montague@montague.lit</jid>" + "</never>" + "</prefs>" + "</iq>";

    @Test
    public void checkRetrievePrefsStanza() throws Exception {
        Method prepareRetrievePreferencesStanza = MamManager.class
                .getDeclaredMethod("prepareRetrievePreferencesStanza");
        prepareRetrievePreferencesStanza.setAccessible(true);

        MamPrefsIQ mamPrefIQ = (MamPrefsIQ) prepareRetrievePreferencesStanza.invoke(mamManager);
        mamPrefIQ.setStanzaId("sarasa");
        Assert.assertEquals(mamPrefIQ.toXML().toString(), retrievePrefsStanzaExample);
    }

    @Test
    public void checkUpdatePrefsStanza() throws Exception {
        Method prepareUpdatePreferencesStanza = MamManager.class.getDeclaredMethod("prepareUpdatePreferencesStanza",
                List.class, List.class, String.class);
        prepareUpdatePreferencesStanza.setAccessible(true);

        List<Jid> alwaysJids = new ArrayList<>();
        alwaysJids.add(JidCreate.from("romeo@montague.lit"));
        alwaysJids.add(JidCreate.from("other@montague.lit"));

        List<Jid> neverJids = new ArrayList<>();
        neverJids.add(JidCreate.from("montague@montague.lit"));

        MamPrefsIQ mamPrefIQ = (MamPrefsIQ) prepareUpdatePreferencesStanza.invoke(mamManager, alwaysJids, neverJids, "roster");
        mamPrefIQ.setStanzaId("sarasa");
        Assert.assertEquals(mamPrefIQ.toXML().toString(), updatePrefsStanzaExample);
    }

}
