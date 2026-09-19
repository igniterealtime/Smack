/*
 *
 *  Copyright 2019-2023 Eng Chong Meng
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
package org.jivesoftware.smackx.httpauthorizationrequest.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmElement;

import org.jxmpp.JxmppContext;

/**
 * The XmlElement Provider for ConfirmElement.
 * XEP-0070: Verifying HTTP Requests via XMPP (1.0.2 (2025-09-30))
 *
 * @author Eng Chong Meng
 */
public class ConfirmExtProvider extends ExtensionElementProvider<ConfirmElement> {
    @Override
    public ConfirmElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
            throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String id = parser.getAttributeValue(null, ConfirmElement.ATTR_ID);
        String method = parser.getAttributeValue(null, ConfirmElement.ATTR_METHOD);
        String url = parser.getAttributeValue(null, ConfirmElement.ATTR_URL);

        return new ConfirmElement(id, method, url);
    }
}
