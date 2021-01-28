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
package org.jivesoftware.smackx.blocking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.blocking.element.BlockListIQ;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class GetBlockingListTest {

    private static final String getBlockingListIQExample = "<iq id='blocklist1' type='get'>"
            + "<blocklist xmlns='urn:xmpp:blocking'/>" + "</iq>";

    private static final String blockListIQExample = "<iq type='result' id='blocklist1'>" + "<blocklist xmlns='urn:xmpp:blocking'>"
            + "<item jid='romeo@montague.net'/>" + "<item jid='iago@shakespeare.lit'/>" + "</blocklist>" + "</iq>";

    private static final String emptyBlockListIQExample = "<iq type='result' id='blocklist1'>" + "<blocklist xmlns='urn:xmpp:blocking'/>"
            + "</iq>";

    @Test
    public void checkGetBlockingListIQStanza() throws Exception {
        BlockListIQ getBlockListIQ = new BlockListIQ(null);
        getBlockListIQ.setType(IQ.Type.get);
        getBlockListIQ.setStanzaId("blocklist1");
        assertEquals(getBlockingListIQExample, getBlockListIQ.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkBlockListIQ() throws Exception {
        IQ iq = PacketParserUtils.parseStanza(blockListIQExample);
        BlockListIQ blockListIQ = (BlockListIQ) iq;
        assertEquals(2, blockListIQ.getBlockedJids().size());
        assertEquals(JidCreate.from("romeo@montague.net"), blockListIQ.getBlockedJids().get(0));
        assertEquals(JidCreate.from("iago@shakespeare.lit"), blockListIQ.getBlockedJids().get(1));

        IQ iq2 = PacketParserUtils.parseStanza(emptyBlockListIQExample);
        BlockListIQ emptyBlockListIQ = (BlockListIQ) iq2;
        assertEquals(0, emptyBlockListIQ.getBlockedJids().size());
    }

}
