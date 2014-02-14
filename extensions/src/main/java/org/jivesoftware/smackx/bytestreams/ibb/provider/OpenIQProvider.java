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
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses an In-Band Bytestream open packet.
 * 
 * @author Henning Staib
 */
public class OpenIQProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        String sessionID = parser.getAttributeValue("", "sid");
        int blockSize = Integer.parseInt(parser.getAttributeValue("", "block-size"));

        String stanzaValue = parser.getAttributeValue("", "stanza");
        StanzaType stanza = null;
        if (stanzaValue == null) {
            stanza = StanzaType.IQ;
        }
        else {
            stanza = StanzaType.valueOf(stanzaValue.toUpperCase());
        }

        return new Open(sessionID, blockSize, stanza);
    }

}
