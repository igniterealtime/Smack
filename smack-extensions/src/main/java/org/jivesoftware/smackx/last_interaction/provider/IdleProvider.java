/**
 *
 * Copyright © 2018 Paul Schaub, 2019 Florian Schmaus
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
package org.jivesoftware.smackx.last_interaction.provider;

import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.parsing.SmackParsingException.SmackTextParseException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.last_interaction.element.IdleElement;

import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;

public class IdleProvider extends ExtensionElementProvider<IdleElement> {

    public static final IdleProvider TEST_INSTANCE = new IdleProvider();

    @Override
    public IdleElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws SmackTextParseException {
        String dateString = parser.getAttributeValue(null, IdleElement.ATTR_SINCE);
        Date since;
        try {
            since = XmppDateTime.parseXEP0082Date(dateString);
        } catch (ParseException e) {
            throw new SmackParsingException.SmackTextParseException(e);
        }
        return new IdleElement(since);
    }
}
