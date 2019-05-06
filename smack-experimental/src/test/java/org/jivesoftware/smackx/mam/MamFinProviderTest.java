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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.mam.element.MamFinIQ;
import org.jivesoftware.smackx.mam.provider.MamFinIQProvider;
import org.jivesoftware.smackx.rsm.packet.RSMSet;

import org.junit.jupiter.api.Test;

public class MamFinProviderTest extends MamTest {

    static final String exmapleMamFinXml = "<fin xmlns='urn:xmpp:mam:1' stable='true'>"
            + "<set xmlns='http://jabber.org/protocol/rsm'>" + "<max>10</max>" + "<after>09af3-cc343-b409f</after>"
            + "</set>" + "</fin>";

    @Test
    public void checkMamFinProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(exmapleMamFinXml);
        MamFinIQ mamFinIQ = new MamFinIQProvider().parse(parser);

        assertFalse(mamFinIQ.isComplete());
        assertTrue(mamFinIQ.isStable());
        assertNull(mamFinIQ.getQueryId());

        RSMSet rsmSet = mamFinIQ.getRSMSet();
        assertEquals(rsmSet.getAfter(), "09af3-cc343-b409f");
        assertEquals(rsmSet.getMax(), 10);
    }

    @Test
    public void checkQueryLimitedResults() throws Exception {
        // @formatter:off
        final String IQ_LIMITED_RESULTS_EXAMPLE = "<iq type='result' id='u29303'>"
                        + "<fin xmlns='urn:xmpp:mam:1' complete='true'>"
                        + "<set xmlns='http://jabber.org/protocol/rsm'>"
                        + "<first index='0'>23452-4534-1</first>"
                        + "<last>390-2342-22</last>" + "<count>16</count>"
                        + "</set>"
                        + "</fin>"
                        + "</iq>";
        // @formatter:on

        IQ iq = PacketParserUtils.parseStanza(IQ_LIMITED_RESULTS_EXAMPLE);

        MamFinIQ mamFinIQ = (MamFinIQ) iq;
        assertEquals(mamFinIQ.getType(), Type.result);

        assertTrue(mamFinIQ.isComplete());
        assertEquals(mamFinIQ.getRSMSet().getCount(), 16);
        assertEquals(mamFinIQ.getRSMSet().getFirst(), "23452-4534-1");
        assertEquals(mamFinIQ.getRSMSet().getFirstIndex(), 0);
        assertEquals(mamFinIQ.getRSMSet().getLast(), "390-2342-22");
    }

}
