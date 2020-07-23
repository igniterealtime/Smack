/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.Collections;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.sid.element.StanzaIdElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.util.XmppDateTime;

public class ContentElementTest {

    @Test
    public void testContentElement() throws ParseException {
        Message.Body body = new Message.Body("en", "My battery is low and it’s getting dark"); // :'(

        ContentElement contentElement = ContentElement.builder()
                .addPayloadItem(body)
                .setFrom(AffixElementsTest.JID_OPPORTUNITY)
                .addTo(AffixElementsTest.JID_HOUSTON)
                .setTimestamp(XmppDateTime.parseXEP0082Date("2018-06-10T00:00:00.000+00:00"))
                .setRandomPadding("RANDOMPADDING")
                .build();

        String expectedXml = "" +
                "<content xmlns='urn:xmpp:sce:0'>" +
                "  <to jid='missioncontrol@houston.nasa.gov'/>" +
                "  <from jid='opportunity@mars.planet'/>" +
                "  <time stamp='2018-06-10T00:00:00.000+00:00'/>" +
                "  <rpad>RANDOMPADDING</rpad>" +
                "  <payload>" +
                "    <body xmlns='jabber:client' xml:lang='en'>My battery is low and it’s getting dark</body>" +
                "  </payload>" +
                "</content>";

        assertXmlSimilar(expectedXml, contentElement.toXML());
        assertEquals(Collections.singletonList(body), contentElement.getPayload().getItems());

        assertEquals(4, contentElement.getAffixElements().size());
        assertTrue(contentElement.getAffixElements().contains(new ToAffixElement(AffixElementsTest.JID_HOUSTON)));
        assertTrue(contentElement.getAffixElements().contains(new FromAffixElement(AffixElementsTest.JID_OPPORTUNITY)));
        assertTrue(contentElement.getAffixElements().contains(
                new TimestampAffixElement(XmppDateTime.parseXEP0082Date("2018-06-10T00:00:00.000+00:00"))));
        assertTrue(contentElement.getAffixElements().contains(new RandomPaddingAffixElement("RANDOMPADDING")));
    }

    @Test
    public void stanzaIdForbiddenInContentElementPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> ContentElement.builder().addPayloadItem(new StanzaIdElement("alice@wonderland.lit")));
    }

    @Test
    public void processingHintsForbiddenInContentElementPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> ContentElement.builder().addPayloadItem(StoreHint.INSTANCE));
    }
}
