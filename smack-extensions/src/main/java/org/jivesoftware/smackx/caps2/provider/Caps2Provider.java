/**
 *
 * Copyright 2020 Aditya Borikar
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
 package org.jivesoftware.smackx.caps2.provider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParser.TagEvent;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.caps2.element.Caps2Element.Caps2HashElement;

public class Caps2Provider extends ExtensionElementProvider<Caps2Element> {

    public static final Caps2Provider INSTANCE = new Caps2Provider();

    @Override
    public Caps2Element parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
        Set<Caps2HashElement> caps2HashElements = new HashSet<>();

        String name = parser.getName();
        String namespace = parser.getNamespace();
        while (!name.equals(Caps2Element.ELEMENT) || !namespace.equals(Caps2Element.NAMESPACE)) {
            parser.nextTag();
        }
        TagEvent event = parser.nextTag();

        loop : while (true) {
            switch (event) {
            case START_ELEMENT:
                name = parser.getName();
                namespace = parser.getNamespace();
                if (!name.equals(Caps2HashElement.ELEMENT) || !namespace.equals(Caps2HashElement.NAMESPACE)) {
                    continue;
                }
                String algorithm = parser.getAttributeValue("algo");
                String hash = parser.nextText();
                Caps2HashElement hashElement = new Caps2HashElement(algorithm, hash);
                caps2HashElements.add(hashElement);
                break;
            case END_ELEMENT:
                int currentDepth = parser.getDepth();
                if (initialDepth == currentDepth) {
                    break loop;
                }
                break;
            }
            event = parser.nextTag();
        }
        Caps2Element caps2Element = new Caps2Element(caps2HashElements);
        return caps2Element;
    }
}
