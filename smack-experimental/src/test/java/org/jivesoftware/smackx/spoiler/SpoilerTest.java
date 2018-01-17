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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.spoiler.element.SpoilerElement;
import org.jivesoftware.smackx.spoiler.provider.SpoilerProvider;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class SpoilerTest extends SmackTestSuite {

    @Test
    public void emptySpoilerTest() throws Exception {
        final String xml = "<spoiler xmlns='urn:xmpp:spoiler:0'/>";

        SpoilerElement empty = SpoilerManager.createSpoiler();

        assertNull(empty.getHint());
        assertNull(empty.getLanguage());

        assertXMLEqual(xml, empty.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);
        assertXMLEqual(xml, parsed.toXML().toString());
    }

    @Test
    public void hintSpoilerTest() throws Exception {
        final String xml = "<spoiler xmlns='urn:xmpp:spoiler:0'>Love story end</spoiler>";

        SpoilerElement withHint = SpoilerManager.createSpoiler("Love story end");

        assertEquals("Love story end", withHint.getHint());
        assertNull(withHint.getLanguage());

        assertXMLEqual(xml, withHint.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);

        assertXMLEqual(xml, parsed.toXML().toString());
    }

    @Test
    public void i18nHintSpoilerTest() throws Exception {
        final String xml = "<spoiler xml:lang='de' xmlns='urn:xmpp:spoiler:0'>Der Kuchen ist eine Lüge!</spoiler>";

        SpoilerElement i18nHint = SpoilerManager.createSpoiler("de", "Der Kuchen ist eine Lüge!");

        assertEquals("Der Kuchen ist eine Lüge!", i18nHint.getHint());
        assertEquals("de", i18nHint.getLanguage());

        assertXMLEqual(xml, i18nHint.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        SpoilerElement parsed = SpoilerProvider.INSTANCE.parse(parser);
        assertEquals(i18nHint.getLanguage(), parsed.getLanguage());

        assertXMLEqual(xml, parsed.toXML().toString());
    }

    @Test
    public void getSpoilersTest() {
        Message m = new Message();
        assertNull(SpoilerManager.getSpoilers(m));

        m.addExtension(SpoilerManager.createSpoiler());
        assertTrue(SpoilerManager.containsSpoiler(m));

        Map<String, String> spoilers = SpoilerManager.getSpoilers(m);
        assertEquals(1, spoilers.size());
        assertEquals(null, spoilers.get(""));

        final String spoilerText = "Spoiler Text";

        m.addExtension(SpoilerManager.createSpoiler("de", spoilerText));
        spoilers = SpoilerManager.getSpoilers(m);
        assertEquals(2, spoilers.size());
        assertEquals(spoilerText, spoilers.get("de"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void spoilerCheckArgumentsNullTest() {
        SpoilerElement spoilerElement = new SpoilerElement("de", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void spoilerCheckArgumentsEmptyTest() {
        SpoilerElement spoilerElement = new SpoilerElement("de", "");
    }
}
