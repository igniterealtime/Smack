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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.blocking.element.UnblockContactsIQ;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class UnblockContactsIQTest {

    String unblockContactIQExample = "<iq id='unblock1' type='set'>" + "<unblock xmlns='urn:xmpp:blocking'>"
            + "<item jid='romeo@montague.net'/>" + "<item jid='pepe@montague.net'/>" + "</unblock>" + "</iq>";

    String unblockContactPushIQExample = "<iq to='juliet@capulet.com/chamber' type='set' id='push3'>"
            + "<unblock xmlns='urn:xmpp:blocking'>" + "<item jid='romeo@montague.net'/>"
            + "<item jid='pepe@montague.net'/>" + "</unblock>" + "</iq>";

    String unblockAllIQExample = "<iq id='unblock2' type='set'>" + "<unblock xmlns='urn:xmpp:blocking'/>"
            + "</iq>";

    String unblockAllPushIQExample = "<iq to='juliet@capulet.com/chamber' type='set' id='push5'>"
            + "<unblock xmlns='urn:xmpp:blocking'/>" + "</iq>";

    @Test
    public void checkUnblockContactIQStanza() throws Exception {
        List<Jid> jids = new ArrayList<>();
        jids.add(JidCreate.from("romeo@montague.net"));
        jids.add(JidCreate.from("pepe@montague.net"));

        UnblockContactsIQ unblockContactIQ = new UnblockContactsIQ(jids);
        unblockContactIQ.setStanzaId("unblock1");

        Assert.assertEquals(unblockContactIQExample, unblockContactIQ.toXML().toString());
    }

    @Test
    public void checkUnblockContactPushIQ() throws Exception {
        IQ iq = (IQ) PacketParserUtils.parseStanza(unblockContactPushIQExample);
        UnblockContactsIQ unblockContactIQ = (UnblockContactsIQ) iq;
        Assert.assertEquals(JidCreate.from("romeo@montague.net"), unblockContactIQ.getJids().get(0));
        Assert.assertEquals(JidCreate.from("pepe@montague.net"), unblockContactIQ.getJids().get(1));
    }

    @Test
    public void checkUnblockAllIQStanza() throws Exception {
        UnblockContactsIQ unblockAllIQ = new UnblockContactsIQ(null);
        unblockAllIQ.setStanzaId("unblock2");
        Assert.assertEquals(unblockAllIQExample, unblockAllIQ.toXML().toString());
    }

    @Test
    public void checkUnblockAllPushIQ() throws Exception {
        IQ iq = (IQ) PacketParserUtils.parseStanza(unblockAllPushIQExample);
        UnblockContactsIQ unblockAllIQ = (UnblockContactsIQ) iq;
        Assert.assertNull(unblockAllIQ.getJids());
    }

}
