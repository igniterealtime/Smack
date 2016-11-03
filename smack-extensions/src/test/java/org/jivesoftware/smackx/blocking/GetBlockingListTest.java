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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.blocking.element.BlockListIQ;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class GetBlockingListTest {

    String getBlockingListIQExample = "<iq id='blocklist1' type='get'>"
            + "<blocklist xmlns='urn:xmpp:blocking'/>" + "</iq>";

    String blockListIQExample = "<iq type='result' id='blocklist1'>" + "<blocklist xmlns='urn:xmpp:blocking'>"
            + "<item jid='romeo@montague.net'/>" + "<item jid='iago@shakespeare.lit'/>" + "</blocklist>" + "</iq>";

    String emptyBlockListIQExample = "<iq type='result' id='blocklist1'>" + "<blocklist xmlns='urn:xmpp:blocking'/>"
            + "</iq>";

    @Test
    public void checkGetBlockingListIQStanza() throws Exception {
        BlockListIQ getBlockListIQ = new BlockListIQ(null);
        getBlockListIQ.setType(Type.get);
        getBlockListIQ.setStanzaId("blocklist1");
        Assert.assertEquals(getBlockingListIQExample, getBlockListIQ.toXML().toString());
    }

    @Test
    public void checkBlockListIQ() throws Exception {
        IQ iq = (IQ) PacketParserUtils.parseStanza(blockListIQExample);
        BlockListIQ blockListIQ = (BlockListIQ) iq;
        Assert.assertEquals(2, blockListIQ.getJids().size());
        Assert.assertEquals(JidCreate.from("romeo@montague.net"), blockListIQ.getJids().get(0));
        Assert.assertEquals(JidCreate.from("iago@shakespeare.lit"), blockListIQ.getJids().get(1));

        IQ iq2 = (IQ) PacketParserUtils.parseStanza(emptyBlockListIQExample);
        BlockListIQ emptyBlockListIQ = (BlockListIQ) iq2;
        Assert.assertEquals(0, emptyBlockListIQ.getJids().size());
    }

}
