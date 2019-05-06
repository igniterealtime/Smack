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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension;
import org.jivesoftware.smackx.mam.provider.MamResultProvider;

import org.junit.jupiter.api.Test;

public class MamResultProviderTest {

    private static final String exampleMamResultXml = "<result xmlns='urn:xmpp:mam:1' queryid='f27' id='28482-98726-73623'>"
            + "<forwarded xmlns='urn:xmpp:forward:0'>" + "<delay xmlns='urn:xmpp:delay' stamp='2010-07-10T23:08:25Z'/>"
            + "<message xmlns='jabber:client' to='juliet@capulet.lit/balcony' from='romeo@montague.lit/orchard' "
            + "type='chat'>"
            + "<body>Call me but love, and I'll be new baptized; Henceforth I never will be Romeo.</body>"
            + "</message>" + "</forwarded>" + "</result>";

    private static final String exampleResultMessage = "<message id='aeb213' to='juliet@capulet.lit/chamber'>"
            + "<result xmlns='urn:xmpp:mam:1' queryid='f27' id='28482-98726-73623'>"
            + "<forwarded xmlns='urn:xmpp:forward:0'>" + "<delay xmlns='urn:xmpp:delay' stamp='2010-07-10T23:08:25Z'/>"
            + "<message xmlns='jabber:client' from='witch@shakespeare.lit' to='macbeth@shakespeare.lit'>"
            + "<body>Hail to thee</body>" + "</message>" + "</forwarded>" + "</result>" + "</message>";

    @Test
    public void checkMamResultProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(exampleMamResultXml);
        MamResultExtension mamResultExtension = new MamResultProvider().parse(parser);

        assertEquals(mamResultExtension.getQueryId(), "f27");
        assertEquals(mamResultExtension.getId(), "28482-98726-73623");

        GregorianCalendar calendar = new GregorianCalendar(2010, 7 - 1, 10, 23, 8, 25);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();

        Forwarded forwarded = mamResultExtension.getForwarded();
        assertEquals(forwarded.getDelayInformation().getStamp(), date);

        Message message = (Message) forwarded.getForwardedStanza();
        assertEquals(message.getFrom().toString(), "romeo@montague.lit/orchard");
        assertEquals(message.getTo().toString(), "juliet@capulet.lit/balcony");
        assertEquals(message.getBody(),
                "Call me but love, and I'll be new baptized; Henceforth I never will be Romeo.");
    }

    @Test
    public void checkResultsParse() throws Exception {
        Message message = PacketParserUtils.parseStanza(exampleResultMessage);
        MamResultExtension mamResultExtension = MamResultExtension.from(message);

        assertEquals(mamResultExtension.getQueryId(), "f27");
        assertEquals(mamResultExtension.getId(), "28482-98726-73623");

        GregorianCalendar calendar = new GregorianCalendar(2010, 7 - 1, 10, 23, 8, 25);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();

        Forwarded forwarded = mamResultExtension.getForwarded();
        assertEquals(forwarded.getDelayInformation().getStamp(), date);

        Message forwardedMessage = (Message) forwarded.getForwardedStanza();
        assertEquals(forwardedMessage.getFrom().toString(), "witch@shakespeare.lit");
        assertEquals(forwardedMessage.getTo().toString(), "macbeth@shakespeare.lit");
        assertEquals(forwardedMessage.getBody(), "Hail to thee");
    }

}
