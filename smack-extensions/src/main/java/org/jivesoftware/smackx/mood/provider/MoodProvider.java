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
package org.jivesoftware.smackx.mood.provider;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.mood.Mood;
import org.jivesoftware.smackx.mood.element.MoodConcretisation;
import org.jivesoftware.smackx.mood.element.MoodElement;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MoodProvider extends ExtensionElementProvider<MoodElement> {

    private static final Logger LOGGER = Logger.getLogger(MoodProvider.class.getName());
    public static final MoodProvider INSTANCE = new MoodProvider();

    @Override
    public MoodElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String text = null;
        Mood mood = null;
        MoodConcretisation concretisation = null;

        outerloop: while (true) {
            int tag = parser.next();
            String name = parser.getName();
            String namespace = parser.getNamespace();

            switch (tag) {
                case START_TAG:
                    if (MoodElement.ELEM_TEXT.equals(name)) {
                        text = parser.nextText();
                        continue outerloop;
                    }

                    if (!MoodElement.NAMESPACE.equals(namespace)) {
                        LOGGER.log(Level.FINE, "Foreign namespace " + namespace + " detected. Try to find suitable MoodConcretisationProvider.");
                        MoodConcretisationProvider<?> provider = (MoodConcretisationProvider) ProviderManager.getExtensionProvider(name, namespace);
                        if (provider != null) {
                            concretisation = provider.parse(parser);
                        } else {
                            LOGGER.log(Level.FINE, "No provider for <" + name + " xmlns:'" + namespace + "'/> found. Ignore.");
                        }
                        continue outerloop;
                    }

                    try {
                        mood = Mood.valueOf(name);
                        continue outerloop;
                    } catch (IllegalArgumentException e) {
                        throw new XmlPullParserException("Unknown mood value: " + name + " encountered.");
                    }

                case END_TAG:
                    if (MoodElement.ELEMENT.equals(name)) {
                        MoodElement.MoodSubjectElement subjectElement = (mood == null && concretisation == null) ?
                                null : new MoodElement.MoodSubjectElement(mood, concretisation);
                        return new MoodElement(subjectElement, text);
                    }
            }
        }
    }
}
