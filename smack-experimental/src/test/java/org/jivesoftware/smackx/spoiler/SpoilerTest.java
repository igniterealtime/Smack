/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.spoiler;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.spoiler.element.SpoilerElement;
import org.jivesoftware.smackx.spoiler.provider.SpoilerProvider;

import org.junit.jupiter.api.Test;

public class SpoilerTest extends SmackTestSuite {

    @Test
    public void emptySpoilerTest() throws Exception {
        final String xml = "<spoiler xmlns='urn:xmpp:spoiler:0'/>";

        Message message = StanzaBuilder.buildMessage().build();
        SpoilerElement.addSpoiler(message);

        SpoilerElement empty = message.getExtension(SpoilerElement.ELEMENT, SpoilerManager.NAMESPACE_0);

        assertNull(empty.getHint());
        assertNull(empty.getLanguage());

        assertXmlSimilar(xml, empty.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);
        assertXmlSimilar(xml, parsed.toXML().toString());
    }

    @Test
    public void hintSpoilerTest() throws Exception {
        final String xml = "<spoiler xmlns='urn:xmpp:spoiler:0'>Love story end</spoiler>";

        Message message = StanzaBuilder.buildMessage().build();
        SpoilerElement.addSpoiler(message, "Love story end");

        SpoilerElement withHint = message.getExtension(SpoilerElement.ELEMENT, SpoilerManager.NAMESPACE_0);

        assertEquals("Love story end", withHint.getHint());
        assertNull(withHint.getLanguage());

        assertXmlSimilar(xml, withHint.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);

        assertXmlSimilar(xml, parsed.toXML().toString());
    }

    @Test
    public void i18nHintSpoilerTest() throws Exception {
        final String xml = "<spoiler xml:lang='de' xmlns='urn:xmpp:spoiler:0'>Der Kuchen ist eine Lüge!</spoiler>";

        Message message = StanzaBuilder.buildMessage().build();
        SpoilerElement.addSpoiler(message, "de", "Der Kuchen ist eine Lüge!");

        SpoilerElement i18nHint = message.getExtension(SpoilerElement.ELEMENT, SpoilerManager.NAMESPACE_0);

        assertEquals("Der Kuchen ist eine Lüge!", i18nHint.getHint());
        assertEquals("de", i18nHint.getLanguage());

        assertXmlSimilar(xml, i18nHint.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);
        assertEquals(i18nHint.getLanguage(), parsed.getLanguage());

        assertXmlSimilar(xml, parsed.toXML().toString());
    }

    @Test
    public void getSpoilersTest() {
        Message m = StanzaBuilder.buildMessage().build();
        assertTrue(SpoilerElement.getSpoilers(m).isEmpty());

        SpoilerElement.addSpoiler(m);
        assertTrue(SpoilerElement.containsSpoiler(m));

        Map<String, String> spoilers = SpoilerElement.getSpoilers(m);
        assertEquals(1, spoilers.size());
        assertEquals(null, spoilers.get(""));

        final String spoilerText = "Spoiler Text";

        SpoilerElement.addSpoiler(m, "de", spoilerText);
        spoilers = SpoilerElement.getSpoilers(m);
        assertEquals(2, spoilers.size());
        assertEquals(spoilerText, spoilers.get("de"));
    }

    @Test
    public void spoilerCheckArgumentsNullTest() {
        assertThrows(IllegalArgumentException.class, () ->
        new SpoilerElement("de", null));
    }

    @Test
    public void spoilerCheckArgumentsEmptyTest() {
        assertThrows(IllegalArgumentException.class, () ->
        new SpoilerElement("de", ""));
    }
}
