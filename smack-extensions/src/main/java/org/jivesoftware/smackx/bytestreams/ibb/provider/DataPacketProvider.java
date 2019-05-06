/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.bytestreams.ibb.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;

/**
 * Parses an In-Band Bytestream data stanza which can be a stanza extension of
 * either an IQ stanza or a message stanza.
 *
 * @author Henning Staib
 */
public class DataPacketProvider {

    public static class IQProvider extends org.jivesoftware.smack.provider.IQProvider<Data> {

        private static final PacketExtensionProvider packetExtensionProvider = new PacketExtensionProvider();

        @Override
        public Data parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                        throws IOException, XmlPullParserException, SmackParsingException {
            DataPacketExtension data = packetExtensionProvider.parse(parser);
            return new Data(data);
        }
    }

    public static class PacketExtensionProvider extends org.jivesoftware.smack.provider.ExtensionElementProvider<DataPacketExtension> {

        @Override
        public DataPacketExtension parse(XmlPullParser parser,
                        int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException,
                        IOException {
            String sessionID = parser.getAttributeValue("", "sid");
            long seq = Long.parseLong(parser.getAttributeValue("", "seq"));
            String data = parser.nextText();
            return new DataPacketExtension(sessionID, seq, data);
        }

    }
}
