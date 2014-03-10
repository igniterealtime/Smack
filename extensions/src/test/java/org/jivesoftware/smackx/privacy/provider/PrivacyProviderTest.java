/**
 *
 * Copyright 2012 Florian Schmaus
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
package org.jivesoftware.smackx.privacy.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.privacy.packet.Privacy;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;
import org.junit.Test;

public class PrivacyProviderTest extends InitExtensions {

    @Test
    public void parsePrivacyList() throws Exception {
        DummyConnection connection = new DummyConnection();

        // @formatter:off
        final String xmlPrivacyList =
        "<iq type='result' id='getlist2' to='romeo@example.net/orchard'>"
          + "<query xmlns='jabber:iq:privacy'>"
          + "<list name='public'>"
          + "<item type='jid'"
          + "value='tybalt@example.com'"
          + "action='deny'"
          + "order='1'/>"
          + "<item action='allow' order='2'/>"
          + "</list>"
          + "</query>"
          + "</iq>";
        // @formatter:on
        IQ iqPrivacyList = PacketParserUtils.parseIQ(TestUtils.getIQParser(xmlPrivacyList), connection);
        assertTrue(iqPrivacyList instanceof Privacy);
        
        Privacy privacyList = (Privacy) iqPrivacyList;
        List<PrivacyItem> pl = privacyList.getPrivacyList("public");

        PrivacyItem first = pl.get(0);
        assertEquals(PrivacyItem.Type.jid, first.getType());
        assertEquals("tybalt@example.com", first.getValue());
        assertEquals(false, first.isAllow());
        assertEquals(1, first.getOrder());
        
        PrivacyItem second = pl.get(1);
        assertEquals(true, second.isAllow());
        assertEquals(2, second.getOrder());
    }
}
