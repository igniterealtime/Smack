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
package org.jivesoftware.smackx.forward.provider;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.delay.packet.DelayInfo;
import org.jivesoftware.smackx.forward.Forwarded;
import org.xmlpull.v1.XmlPullParser;

/**
 * This class implements the {@link PacketExtensionProvider} to parse
 * forwarded messages from a packet.  It will return a {@link Forwarded} packet extension.
 *
 * @author Georg Lukas
 */
public class ForwardedProvider implements PacketExtensionProvider {
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        DelayInfo di = null;
        Packet packet = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("delay"))
                    di = (DelayInfo)PacketParserUtils.parsePacketExtension(parser.getName(), parser.getNamespace(), parser);
                else if (parser.getName().equals("message"))
                    packet = PacketParserUtils.parseMessage(parser);
                else throw new Exception("Unsupported forwarded packet type: " + parser.getName());
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(Forwarded.ELEMENT_NAME))
                done = true;
        }
        if (packet == null)
            throw new Exception("forwarded extension must contain a packet");
        return new Forwarded(di, packet);
    }
}
