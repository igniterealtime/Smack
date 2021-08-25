/**
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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;

/**
 * Parse JingleByteStreamTransport elements.
 */
public class JingleIBBTransportProvider extends JingleContentTransportProvider<JingleIBBTransportElement> {

    @Override
    public JingleIBBTransportElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        Short blockSize = ParserUtils.getShortAttribute(parser, JingleIBBTransportElement.ATTR_BLOCK_SIZE);
        String sid = parser.getAttributeValue(null, JingleIBBTransportElement.ATTR_SID);
        return new JingleIBBTransportElement(sid, blockSize);
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }
}
