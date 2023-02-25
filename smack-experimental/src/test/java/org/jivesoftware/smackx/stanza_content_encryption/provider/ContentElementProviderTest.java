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
package org.jivesoftware.smackx.stanza_content_encryption.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.stanza_content_encryption.element.ContentElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.FromAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.RandomPaddingAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.TimestampAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.ToAffixElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class ContentElementProviderTest {

    @Test
    public void testParsing() throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String xml = "" +
                "<content xmlns='urn:xmpp:sce:0'>\n" +
                "  <payload>\n" +
                "    <body xmlns='jabber:client'>Have you seen that new movie?</body>\n" +
                "    <x xmlns='jabber:x:oob'>\n" +
                "      <url>https://en.wikipedia.org/wiki/Fight_Club#Plot</url>\n" +
                "    </x>\n" +
                "  </payload>\n" +
                "  <from jid='ladymacbeth@shakespear.lit/castle'/>\n" +
                "  <to jid='doctor@shakespeare.lit/pda'/>\n" +
                "  <time stamp='1993-10-12T03:13:10.000+00:00'/>\n" +
                "  <rpad>A98D7KJF1ASDVG232sdff341</rpad>\n" +
                "</content>";

        ContentElementProvider provider = new ContentElementProvider();
        ContentElement contentElement = provider.parse(TestUtils.getParser(xml));

        assertNotNull(contentElement);

        assertEquals(4, contentElement.getAffixElements().size());
        assertTrue(contentElement.getAffixElements().contains(
                new FromAffixElement(JidCreate.from("ladymacbeth@shakespear.lit/castle"))));
        assertTrue(contentElement.getAffixElements().contains(
                new ToAffixElement(JidCreate.from("doctor@shakespeare.lit/pda"))));
        assertTrue(contentElement.getAffixElements().contains(
                new TimestampAffixElement(ParserUtils.getDateFromXep82String("1993-10-12T03:13:10.000+00:00"))));
        assertTrue(contentElement.getAffixElements().contains(
                new RandomPaddingAffixElement("A98D7KJF1ASDVG232sdff341")));

        assertEquals(2, contentElement.getPayload().getItems().size());

        assertTrue(contentElement.getPayload().getItems().get(0) instanceof Message.Body);
        Message.Body body = (Message.Body) contentElement.getPayload().getItems().get(0);
        assertEquals("Have you seen that new movie?", body.getMessage());

        StandardExtensionElement oob = (StandardExtensionElement) contentElement.getPayload().getItems().get(1);
        assertEquals("x", oob.getElementName());
        assertEquals("jabber:x:oob", oob.getNamespace());
        assertEquals("https://en.wikipedia.org/wiki/Fight_Club#Plot", oob.getFirstElement("url").getText());
    }
}
