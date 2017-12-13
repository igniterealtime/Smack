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

import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.provider.MamPrefsIQProvider;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;

public class MamPrefIQProviderTest extends MamTest {

    private static final String exampleMamPrefsIQ1 = "<iq type='set' id='juliet3'>" + "<prefs xmlns='urn:xmpp:mam:1' default='roster'>"
            + "<always>" + "<jid>romeo@montague.lit</jid>" + "</always>" + "<never>"
            + "<jid>montague@montague.lit</jid>" + "</never>" + "</prefs>" + "</iq>";

    private static final String exampleMamPrefsIQ2 = "<iq type='set' id='juliet3'>" + "<prefs xmlns='urn:xmpp:mam:1' default='roster'>"
            + "<always>" + "<jid>romeo@montague.lit</jid>" + "<jid>montague@montague.lit</jid>" + "</always>"
            + "<never>" + "</never>" + "</prefs>" + "</iq>";

    private static final String exampleMamPrefsIQ3 = "<iq type='get' id='juliet3'>" + "<prefs xmlns='urn:xmpp:mam:1'>" + "</prefs>"
            + "</iq>";

    private static final String exampleMamPrefsResultIQ = "<iq type='result' id='juliet3'>"
            + "<prefs xmlns='urn:xmpp:mam:1' default='roster'>" + "<always>" + "<jid>romeo@montague.lit</jid>"
            + "</always>" + "<never>" + "<jid>sarasa@montague.lit</jid>" + "<jid>montague@montague.lit</jid>"
            + "</never>" + "</prefs>" + "</iq>";

    @Test
    public void checkMamPrefsIQProvider() throws Exception {
        XmlPullParser parser1 = PacketParserUtils.getParserFor(exampleMamPrefsIQ1);
        MamPrefsIQ mamPrefIQ1 = new MamPrefsIQProvider().parse(parser1);

        Assert.assertEquals(IQ.Type.set, mamPrefIQ1.getType());
        Assert.assertEquals(mamPrefIQ1.getAlwaysJids().get(0).toString(), "romeo@montague.lit");
        Assert.assertEquals(mamPrefIQ1.getNeverJids().get(0).toString(), "montague@montague.lit");

        XmlPullParser parser2 = PacketParserUtils.getParserFor(exampleMamPrefsIQ2);
        MamPrefsIQ mamPrefIQ2 = new MamPrefsIQProvider().parse(parser2);
        Assert.assertEquals(IQ.Type.set, mamPrefIQ2.getType());
        Assert.assertEquals(mamPrefIQ2.getAlwaysJids().get(0).toString(), "romeo@montague.lit");
        Assert.assertEquals(mamPrefIQ2.getAlwaysJids().get(1).toString(), "montague@montague.lit");
        Assert.assertTrue(mamPrefIQ2.getNeverJids().isEmpty());

        XmlPullParser parser3 = PacketParserUtils.getParserFor(exampleMamPrefsIQ3);
        MamPrefsIQ mamPrefIQ3 = new MamPrefsIQProvider().parse(parser3);
        Assert.assertEquals(IQ.Type.set, mamPrefIQ3.getType());
    }

    @Test
    public void checkMamPrefResult() throws Exception {
        IQ iq = PacketParserUtils.parseStanza(exampleMamPrefsResultIQ);

        MamPrefsIQ mamPrefsIQ = (MamPrefsIQ) iq;

        List<Jid> alwaysJids = mamPrefsIQ.getAlwaysJids();
        List<Jid> neverJids = mamPrefsIQ.getNeverJids();

        Assert.assertEquals(alwaysJids.size(), 1);
        Assert.assertEquals(neverJids.size(), 2);
        Assert.assertEquals(alwaysJids.get(0).toString(), "romeo@montague.lit");
        Assert.assertEquals(neverJids.get(1).toString(), "montague@montague.lit");
    }

}
