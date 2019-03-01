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
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.mood.element.MoodConcretisation;
import org.jivesoftware.smackx.mood.element.MoodElement;
import org.jivesoftware.smackx.mood.provider.MoodProvider;
import org.jivesoftware.smackx.mood.provider.SimpleMoodConcretisationProvider;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

/**
 * This test checks, if extending XEP-0107: User Mood using custom mood concretisations works.
 * For that purpose, the second example of XEP-0107 §2.1 is recreated by creating a custom mood concretisation (ecstatic),
 * along with a provider, which is dynamically registered with the {@link ProviderManager}.
 */
public class MoodConcretisationTest extends SmackTestSuite {

    @Test
    public void concretisationTest() throws Exception {
        ProviderManager.addExtensionProvider(
                EcstaticMoodConcretisation.ELEMENT,
                EcstaticMoodConcretisation.NAMESPACE,
                EcstaticMoodConcretisationProvider.INSTANCE);

        String xml =
                "<mood xmlns='http://jabber.org/protocol/mood'>" +
                    "<happy>" +
                        "<ecstatic xmlns='https://example.org/'/>" +
                    "</happy>" +
                    "<text>Yay, the mood spec has been approved!</text>" +
                "</mood>";

        MoodElement element = new MoodElement(
                new MoodElement.MoodSubjectElement(
                        Mood.happy,
                        new EcstaticMoodConcretisation()),
                "Yay, the mood spec has been approved!");

        assertXMLEqual(xml, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MoodElement parsed = MoodProvider.INSTANCE.parse(parser);
        assertXMLEqual(xml, parsed.toXML().toString());

        assertTrue(parsed.hasConcretisation());
        assertTrue(parsed.hasText());
        assertEquals(EcstaticMoodConcretisation.ELEMENT, parsed.getMoodConcretisation().getMood());
    }

    @Test
    public void unknownConcretisationTest() throws Exception {
        String xml =
                "<mood xmlns='http://jabber.org/protocol/mood'>" +
                    "<sad>" +
                        "<owl xmlns='https://reddit.com/r/superbowl/'/>" +
                    "</sad>" +
                    "<text>Hoot hoot!</text>" +
                "</mood>";

        XmlPullParser parser = TestUtils.getParser(xml);
        MoodElement element = MoodProvider.INSTANCE.parse(parser);

        // We should not have a provider for sad owls, so the concretisation should not be there.
        assertFalse(element.hasConcretisation());
    }

    public static class EcstaticMoodConcretisation extends MoodConcretisation {

        public static final String NAMESPACE = "https://example.org/";
        public static final String ELEMENT = "ecstatic";

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static class EcstaticMoodConcretisationProvider extends SimpleMoodConcretisationProvider<EcstaticMoodConcretisation> {

        @Override
        protected EcstaticMoodConcretisation simpleExtension() {
            return new EcstaticMoodConcretisation();
        }

        static EcstaticMoodConcretisationProvider INSTANCE = new EcstaticMoodConcretisationProvider();
    }
}
