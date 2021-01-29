/**
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.address.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.address.packet.MultipleAddresses;

import org.jxmpp.jid.Jid;

/**
 * The MultipleAddressesProvider parses {@link MultipleAddresses} packets.
 *
 * @author Gaston Dombiak
 */
public class MultipleAddressesProvider extends ExtensionElementProvider<MultipleAddresses> {

    @Override
    public MultipleAddresses parse(XmlPullParser parser,
                    int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException,
                    IOException {
        MultipleAddresses multipleAddresses = new MultipleAddresses();
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case MultipleAddresses.Address.ELEMENT:
                    String typeString = parser.getAttributeValue("", "type");
                    MultipleAddresses.Type type = MultipleAddresses.Type.valueOf(typeString);
                    Jid jid = ParserUtils.getJidAttribute(parser, "jid");
                    String node = parser.getAttributeValue("", "node");
                    String desc = parser.getAttributeValue("", "desc");
                    boolean delivered = "true".equals(parser.getAttributeValue("", "delivered"));
                    String uri = parser.getAttributeValue("", "uri");
                    // Add the parsed address
                    multipleAddresses.addAddress(type, jid, node, desc, delivered, uri);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        return multipleAddresses;
    }
}
