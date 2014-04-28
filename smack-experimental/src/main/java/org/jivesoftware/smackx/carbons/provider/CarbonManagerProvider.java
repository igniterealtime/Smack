/**
 *
 * Copyright 2013 Georg Lukas
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
package org.jivesoftware.smackx.carbons.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension.Direction;
import org.jivesoftware.smackx.forward.Forwarded;
import org.xmlpull.v1.XmlPullParser;

/**
 * This class implements the {@link PacketExtensionProvider} to parse
 * cabon copied messages from a packet.  It will return a {@link CarbonExtension} packet extension.
 * 
 * @author Georg Lukas
 *
 */
public class CarbonManagerProvider implements PacketExtensionProvider {

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        Direction dir = Direction.valueOf(parser.getName());
        Forwarded fwd = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("forwarded")) {
                fwd = (Forwarded) PacketParserUtils.parsePacketExtension(Forwarded.ELEMENT_NAME, Forwarded.NAMESPACE, parser);
            }
            else if (eventType == XmlPullParser.END_TAG && dir == Direction.valueOf(parser.getName()))
                done = true;
        }
        if (fwd == null)
            throw new Exception("sent/received must contain exactly one <forwarded> tag");
        return new CarbonExtension(dir, fwd);
    }
}
