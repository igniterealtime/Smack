/*
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoOptOutElement;

import org.jxmpp.JxmppContext;

/**
 * Smack ExtensionProvider that parses OMEMO OptOut reason element.
 *
 * @author Eng Chong Meng
 */
public class OmemoOptOutProvider extends ExtensionElementProvider<OmemoOptOutElement> {
    @Override
    public OmemoOptOutElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        OmemoOptOutElement optOutElement = new OmemoOptOutElement();

        outerloop:
        while (true) {
            XmlPullParser.Event tag = parser.next();
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                if (name.equals(OmemoOptOutElement.ATTR_REASON)) {
                    parser.next();
                    optOutElement.setReason(parser.getText());
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                break;
            }
        }
        return optOutElement;
    }
}
