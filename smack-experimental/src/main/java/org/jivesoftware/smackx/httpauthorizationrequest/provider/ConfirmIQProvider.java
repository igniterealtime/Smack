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

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmElement;
import org.jivesoftware.smackx.httpauthorizationrequest.packet.ConfirmIQ;

import org.jxmpp.JxmppContext;

/**
 * The IQ Provider for ConfirmIQ <code>ConfirmElement</code>.
 * XEP-0070: Verifying HTTP Requests via XMPP (1.0.2 (2025-09-30))
 *
 * @author Eng Chong Meng
 */
public class ConfirmIQProvider extends IqProvider<ConfirmIQ> {

    @Override
    public ConfirmIQ parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
            throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        ConfirmElement confirmElement = (ConfirmElement) PacketParserUtils
                .parseExtensionElement(ConfirmElement.ELEMENT, ConfirmElement.NAMESPACE, parser, xmlEnvironment, jxmppContext);
        return new ConfirmIQ(confirmElement);
    }
}
