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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class QueryArchiveTest extends MamTest {

    String mamSimpleQueryIQ = "<iq id='sarasa' type='set'>" + "<query xmlns='urn:xmpp:mam:1' queryid='testid'>"
            + "<x xmlns='jabber:x:data' type='submit'>" + "<field var='FORM_TYPE' type='hidden'>" + "<value>"
            + MamElements.NAMESPACE + "</value>" + "</field>" + "</x>" + "</query>" + "</iq>";

    String mamQueryResultExample = "<message to='hag66@shakespeare.lit/pda' from='coven@chat.shakespeare.lit' id='iasd207'>"
            + "<result xmlns='urn:xmpp:mam:1' queryid='g27' id='34482-21985-73620'>"
            + "<forwarded xmlns='urn:xmpp:forward:0'>"
            + "<delay xmlns='urn:xmpp:delay' stamp='2002-10-13T23:58:37.000+00:00'></delay>" + "<message "
            + "from='coven@chat.shakespeare.lit/firstwitch' " + "id='162BEBB1-F6DB-4D9A-9BD8-CFDCC801A0B2' "
            + "type='chat'>" + "<body>Thrice the brinded cat hath mew.</body>" + "</message>" + "</forwarded>"
            + "</result>" + "</message>";

    @Test
    public void checkMamQueryIQ() throws Exception {
        DataForm dataForm = getNewMamForm();
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setStanzaId("sarasa");
        Assert.assertEquals(mamQueryIQ.toXML().toString(), mamSimpleQueryIQ);
    }

    @Test
    public void checkMamQueryResults() throws Exception {
        Message message = new Message();
        message.setStanzaId("iasd207");
        message.setFrom(JidCreate.from("coven@chat.shakespeare.lit"));
        message.setTo(JidCreate.from("hag66@shakespeare.lit/pda"));

        GregorianCalendar calendar = new GregorianCalendar(2002, 10 - 1, 13, 23, 58, 37);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();

        DelayInformation delay = new DelayInformation(date);
        Message forwardedMessage = new Message();
        forwardedMessage.setFrom(JidCreate.from("coven@chat.shakespeare.lit/firstwitch"));
        forwardedMessage.setStanzaId("162BEBB1-F6DB-4D9A-9BD8-CFDCC801A0B2");
        forwardedMessage.setType(Type.chat);
        forwardedMessage.setBody("Thrice the brinded cat hath mew.");

        Forwarded forwarded = new Forwarded(delay, forwardedMessage);

        message.addExtension(new MamResultExtension("g27", "34482-21985-73620", forwarded));

        Assert.assertEquals(message.toXML().toString(), mamQueryResultExample);

        MamResultExtension mamResultExtension = MamResultExtension.from(message);

        Assert.assertEquals(mamResultExtension.getId(), "34482-21985-73620");
        Assert.assertEquals(mamResultExtension.getForwarded().getDelayInformation().getStamp(), date);

        Message resultMessage = (Message) mamResultExtension.getForwarded().getForwardedStanza();
        Assert.assertEquals(resultMessage.getFrom(), JidCreate.from("coven@chat.shakespeare.lit/firstwitch"));
        Assert.assertEquals(resultMessage.getStanzaId(), "162BEBB1-F6DB-4D9A-9BD8-CFDCC801A0B2");
        Assert.assertEquals(resultMessage.getType(), Type.chat);
        Assert.assertEquals(resultMessage.getBody(), "Thrice the brinded cat hath mew.");
    }

}
