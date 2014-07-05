/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
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
package org.jivesoftware.smackx.xhtmlim.provider;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * The XHTMLExtensionProvider parses XHTML packets.
 *
 * @author Florian Schmaus
 */
public class XHTMLExtensionProvider implements PacketExtensionProvider {
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws IOException, XmlPullParserException {
        XHTMLExtension xhtmlExtension = new XHTMLExtension();

        int startDepth = parser.getDepth();
        while (true) {
            int eventType = parser.getEventType();
            String name = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(Message.BODY)) {
                    xhtmlExtension.addBody(PacketParserUtils.parseElement(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (name.equals(XHTMLExtension.ELEMENT) && parser.getDepth() <= startDepth) {
                    return xhtmlExtension;
                }
            }
            parser.next();
        }
    }
}
