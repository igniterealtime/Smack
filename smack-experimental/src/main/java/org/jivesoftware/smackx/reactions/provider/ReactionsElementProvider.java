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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;

/**
 * A provider class for parsing {@link ReactionsElement} from an XML stream.
 * <p>
 * This class is responsible for extracting the relevant information from an XML input stream and converting it into a {@link ReactionsElement}.
 * It processes the XML structure according to the expected format of the reactions element and its child elements.
 * </p>
 * @see <a href="http://xmpp.org/extensions/xep-0444.html">XEP-0444: Message
 *       Reactions</a>
 *
 *  @author Ismael Nunes Campos
 */
public class ReactionsElementProvider extends ExtensionElementProvider<ReactionsElement> {

    @Override
    public ReactionsElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String id = parser.getAttributeValue("id");
        Set<Reaction> reactions = new HashSet<>();

        outerloop: while (true) {
            XmlPullParser.Event tag = parser.next();

            switch (tag) {
            case START_ELEMENT:
                if (parser.getName().equals(Reaction.ELEMENT)) {
                    String emoji = parser.nextText();
                    reactions.add(new Reaction(emoji));
                }
                break;

            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }

        return new ReactionsElement(reactions, id);
    }
}
