/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle_rtp.provider;

import static org.jivesoftware.smack.xml.XmlPullParser.Event.END_ELEMENT;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_rtp.DefaultXmlElementProvider;
import org.jivesoftware.smackx.jingle_rtp.element.Grouping;

/**
 * A Grouping provider that parses incoming stanza extensions into instances of the
 * {@link Class} that it has een instantiated for.
 *
 * @author Eng Chong Meng
 */
public class JingleGroupingProvider extends ExtensionElementProvider<Grouping> {
    /**
     * Parse an extension sub-stanza and create a <code>EE</code> instance. At the beginning of the
     * method call, the xml parser will be positioned on the opening element of the stanza extension
     * and at the end of the method call it will be on the closing element of the stanza extension.
     *
     * @param parser an XML parser positioned at the stanza's starting element.
     * @return a new Grouping stanza extension instance.
     *
     * @throws XmlPullParserException if an error occurs pull parsing the XML.
     * @throws IOException if an error occurs in IO.
     * @throws SmackParsingException if an error occurs parsing the XML.
     */
    @Override
    public Grouping parse(XmlPullParser parser, int i, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        DefaultXmlElementProvider<JingleContent> contentProvider = new DefaultXmlElementProvider<>(JingleContent.class);

        Grouping.Builder builder = Grouping.getBuilder();
        String semantics = parser.getAttributeValue("", Grouping.ATTR_SEMANTICS);
        if (semantics != null)
            builder.setSemantics(semantics);

        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            String elementName = parser.getName();

            if (elementName.equals(JingleContent.ELEMENT)) {
                JingleContent content = contentProvider.parse(parser);
                builder.addChildElement(content);
            }

            if ((eventType == END_ELEMENT) && parser.getName().equals(Grouping.ELEMENT)) {
                done = true;
            }
        }
        return builder.build();
    }
}
