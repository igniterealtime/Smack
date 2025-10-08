/*
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smackx.iqlast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.test.util.ElementParserUtils;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.iqlast.packet.LastActivity;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.jupiter.api.Test;

public class LastActivityTest extends SmackTestSuite {

    @Test
    public void checkProvider() throws Exception {
        XMLBuilder xml = XMLBuilder.create("iq");
        xml.a("from", "romeo@montague.net/orchard")
            .a("id", "last2")
            .a("to", "juliet@capulet.com/balcony")
            .a("type", "get")
            .e("query")
                .namespace(LastActivity.NAMESPACE);

        DummyConnection c = new DummyConnection();
        c.connect();
        IQ lastRequest = ElementParserUtils.parseStanza(xml.asString());
        assertTrue(lastRequest instanceof LastActivity);

        c.processStanza(lastRequest);
        Stanza reply = c.getSentPacket();
        assertTrue(reply instanceof LastActivity);
        LastActivity l = (LastActivity) reply;
        assertEquals("last2", l.getStanzaId());
        assertEquals(IQ.Type.result, l.getType());
    }
}
