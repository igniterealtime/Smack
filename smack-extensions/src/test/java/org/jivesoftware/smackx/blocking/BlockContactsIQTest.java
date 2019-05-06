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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.blocking.element.BlockContactsIQ;

import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class BlockContactsIQTest {

    private static final String blockContactIQExample = "<iq xmlns='jabber:client' id='block1' type='set'>" + "<block xmlns='urn:xmpp:blocking'>"
            + "<item jid='romeo@montague.net'/>" + "<item jid='pepe@montague.net'/>" + "</block>" + "</iq>";

    private static final String blockContactPushIQExample = "<iq xmlns='jabber:client' to='juliet@capulet.com/chamber' type='set' id='push1'>"
            + "<block xmlns='urn:xmpp:blocking'>" + "<item jid='romeo@montague.net'/>"
            + "<item jid='pepe@montague.net'/>" + "</block>" + "</iq>";

    @Test
    public void checkBlockContactIQStanza() throws Exception {
        List<Jid> jids = new ArrayList<>();
        jids.add(JidCreate.from("romeo@montague.net"));
        jids.add(JidCreate.from("pepe@montague.net"));

        BlockContactsIQ blockContactIQ = new BlockContactsIQ(jids);
        blockContactIQ.setStanzaId("block1");

        assertEquals(blockContactIQExample, blockContactIQ.toXML().toString());
    }

    @Test
    public void checkBlockContactPushIQ() throws Exception {
        IQ iq = PacketParserUtils.parseStanza(blockContactPushIQExample);
        BlockContactsIQ blockContactIQ = (BlockContactsIQ) iq;
        assertEquals(JidCreate.from("romeo@montague.net"), blockContactIQ.getJids().get(0));
        assertEquals(JidCreate.from("pepe@montague.net"), blockContactIQ.getJids().get(1));
    }

}
