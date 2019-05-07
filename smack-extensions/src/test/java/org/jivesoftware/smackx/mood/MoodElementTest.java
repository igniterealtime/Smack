/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.mood;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.mood.element.MoodElement;
import org.jivesoftware.smackx.mood.provider.MoodProvider;

import org.junit.Test;

public class MoodElementTest extends SmackTestSuite {

    @Test
    public void toXmlTest() throws Exception {
        String xml =
                "<mood xmlns='http://jabber.org/protocol/mood'>" +
                "<happy/>" +
                "<text>Yay, the mood spec has been approved!</text>" +
                "</mood>";
        MoodElement moodElement = new MoodElement(new MoodElement.MoodSubjectElement(Mood.happy, null), "Yay, the mood spec has been approved!");

        assertXmlSimilar(xml, moodElement.toXML().toString());
        assertFalse(moodElement.hasConcretisation());
        assertEquals(Mood.happy, moodElement.getMood());

        XmlPullParser parser = TestUtils.getParser(xml);
        MoodElement parsed = MoodProvider.INSTANCE.parse(parser);
        assertEquals(xml, parsed.toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentsTest() {
        MoodElement element = new MoodElement(null, "Text alone is not allowed.");
    }

    @Test
    public void emptyMoodTest() throws Exception {
        MoodElement empty = new MoodElement(null, null);
        assertNull(empty.getText());
        assertNull(empty.getMood());
        assertNull(empty.getMoodConcretisation());
        assertFalse(empty.hasText());
        assertFalse(empty.hasConcretisation());

        String xml = "<mood xmlns='http://jabber.org/protocol/mood'/>";
        XmlPullParser parser = TestUtils.getParser(xml);
        MoodElement emptyParsed = MoodProvider.INSTANCE.parse(parser);
        assertEquals(empty.toXML().toString(), emptyParsed.toXML().toString());
    }

    @Test(expected = XmlPullParserException.class)
    public void unknownMoodValueExceptionTest() throws Exception {
        String xml =
                "<mood xmlns='http://jabber.org/protocol/mood'>" +
                    "<unknown/>" +
                "</mood>";
        XmlPullParser parser = TestUtils.getParser(xml);
        MoodElement element = MoodProvider.INSTANCE.parse(parser);
    }
}
