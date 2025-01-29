/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ReactionsElementProvider extends ExtensionElementProvider<ReactionsElement> {
    @Override
    public ReactionsElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String id = parser.getAttributeValue(null, "id");
        List<Reaction> reactions = new ArrayList<>();

        while (true) {
            XmlPullParser.Event tag = parser.next();

            if (tag == XmlPullParser.Event.END_ELEMENT && parser.getName().equals(ReactionsElement.ELEMENT)) {
                break;
            }
            if (tag == XmlPullParser.Event.START_ELEMENT && parser.getName().equals(Reaction.ELEMENT)) {
                String emoji = parser.nextText();
                reactions.add(new Reaction(emoji));
            }
        }

        return new ReactionsElement(reactions, id);
    }
}
