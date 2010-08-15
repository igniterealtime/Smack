/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses an In-Band Bytestream data packet which can be a packet extension of
 * either an IQ stanza or a message stanza.
 * 
 * @author Henning Staib
 */
public class DataPacketProvider implements PacketExtensionProvider, IQProvider {

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String sessionID = parser.getAttributeValue("", "sid");
        long seq = Long.parseLong(parser.getAttributeValue("", "seq"));
        String data = parser.nextText();
        return new DataPacketExtension(sessionID, seq, data);
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        DataPacketExtension data = (DataPacketExtension) parseExtension(parser);
        IQ iq = new Data(data);
        return iq;
    }

}
