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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.SmackTestUtil.XmlPullParserKind;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.provider.MamPrefsIQProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jxmpp.jid.Jid;

public class MamPrefIQProviderTest extends MamTest {

    private static final String exampleMamPrefsIQ1 = "<prefs xmlns='urn:xmpp:mam:2' default='roster'>"
            + "<always>" + "<jid>romeo@montague.lit</jid>" + "</always>" + "<never>"
            + "<jid>montague@montague.lit</jid>" + "</never>" + "</prefs>";

    private static final String exampleMamPrefsIQ2 = "<prefs xmlns='urn:xmpp:mam:2' default='roster'>"
            + "<always>" + "<jid>romeo@montague.lit</jid>" + "<jid>montague@montague.lit</jid>" + "</always>"
            + "<never>" + "</never>" + "</prefs>";

    private static final String exampleMamPrefsIQ3 =  "<prefs xmlns='urn:xmpp:mam:2'>" + "</prefs>";

    private static final String exampleMamPrefsResultIQ = "<iq type='result' id='juliet3'>"
            + "<prefs xmlns='urn:xmpp:mam:2' default='roster'>" + "<always>" + "<jid>romeo@montague.lit</jid>"
            + "</always>" + "<never>" + "<jid>sarasa@montague.lit</jid>" + "<jid>montague@montague.lit</jid>"
            + "</never>" + "</prefs>" + "</iq>";

    @ParameterizedTest
    @EnumSource(value = SmackTestUtil.XmlPullParserKind.class)
    public void checkMamPrefsIQProvider(XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        XmlPullParser parser1 = SmackTestUtil.getParserFor(exampleMamPrefsIQ1, parserKind);
        MamPrefsIQ mamPrefIQ1 = MamPrefsIQProvider.INSTANCE.parse(parser1, null);

        assertEquals(IQ.Type.set, mamPrefIQ1.getType());
        assertEquals(mamPrefIQ1.getAlwaysJids().get(0).toString(), "romeo@montague.lit");
        assertEquals(mamPrefIQ1.getNeverJids().get(0).toString(), "montague@montague.lit");

        XmlPullParser parser2 = SmackTestUtil.getParserFor(exampleMamPrefsIQ2, parserKind);
        MamPrefsIQ mamPrefIQ2 = MamPrefsIQProvider.INSTANCE.parse(parser2, null);
        assertEquals(IQ.Type.set, mamPrefIQ2.getType());
        assertEquals(mamPrefIQ2.getAlwaysJids().get(0).toString(), "romeo@montague.lit");
        assertEquals(mamPrefIQ2.getAlwaysJids().get(1).toString(), "montague@montague.lit");
        assertTrue(mamPrefIQ2.getNeverJids().isEmpty());

        XmlPullParser parser3 = SmackTestUtil.getParserFor(exampleMamPrefsIQ3, parserKind);
        MamPrefsIQ mamPrefIQ3 = MamPrefsIQProvider.INSTANCE.parse(parser3, null);
        assertEquals(IQ.Type.set, mamPrefIQ3.getType());
    }

    @Test
    public void checkMamPrefResult() throws Exception {
        IQ iq = PacketParserUtils.parseStanza(exampleMamPrefsResultIQ);

        MamPrefsIQ mamPrefsIQ = (MamPrefsIQ) iq;

        List<Jid> alwaysJids = mamPrefsIQ.getAlwaysJids();
        List<Jid> neverJids = mamPrefsIQ.getNeverJids();

        assertEquals(alwaysJids.size(), 1);
        assertEquals(neverJids.size(), 2);
        assertEquals(alwaysJids.get(0).toString(), "romeo@montague.lit");
        assertEquals(neverJids.get(1).toString(), "montague@montague.lit");
    }

}
