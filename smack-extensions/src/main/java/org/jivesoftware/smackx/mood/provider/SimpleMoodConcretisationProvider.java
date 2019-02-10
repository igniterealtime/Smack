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

import java.io.IOException;

import org.jivesoftware.smackx.mood.element.MoodConcretisation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Simple {@link MoodConcretisationProvider} implementation, suitable for really simple {@link MoodConcretisation}s,
 * that only consist of name and namespace. In such a case it is sufficient to just return an instance of the element
 * addressed by the element name and namespace, since no other values must be parsed.
 *
 * @param <C> type of the {@link MoodConcretisation}
 */
public abstract class SimpleMoodConcretisationProvider<C extends MoodConcretisation> extends MoodConcretisationProvider<C> {

    @Override
    public C parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException {
        // Since the elements name and namespace is known, we can just return an instance of the MoodConcretisation.
        return simpleExtension();
    }

    protected abstract C simpleExtension();
}
