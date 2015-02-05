/**
 *
 * Copyright 2013-2014 Georg Lukas
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

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.delay.provider.DelayInformationProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This class implements the {@link PacketExtensionProvider} to parse
 * forwarded messages from a packet.  It will return a {@link Forwarded} packet extension.
 *
 * @author Georg Lukas
 */
public class ForwardedProvider extends PacketExtensionProvider<Forwarded> {

    private static final Logger LOGGER = Logger.getLogger(ForwardedProvider.class.getName());

    @Override
    public Forwarded parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        DelayInformation di = null;
        Stanza packet = null;

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (name) {
                case DelayInformation.ELEMENT:
                    if (DelayInformation.NAMESPACE.equals(namespace)) {
                        di = DelayInformationProvider.INSTANCE.parse(parser, parser.getDepth());
                    } else {
                        LOGGER.warning("Namespace '" + namespace + "' does not match expected namespace '"
                                        + DelayInformation.NAMESPACE + "'");
                    }
                    break;
                case Message.ELEMENT:
                    packet = PacketParserUtils.parseMessage(parser);
                    break;
                default:
                    LOGGER.warning("Unsupported forwarded packet type: " + name);
                }
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }

        if (packet == null)
            throw new SmackException("forwarded extension must contain a packet");
        return new Forwarded(di, packet);
    }
}
