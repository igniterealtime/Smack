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
import java.util.Locale;

import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses an In-Band Bytestream open packet.
 * 
 * @author Henning Staib
 */
public class OpenIQProvider extends IQProvider<Open> {

    @Override
    public Open parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        String sessionID = parser.getAttributeValue("", "sid");
        int blockSize = Integer.parseInt(parser.getAttributeValue("", "block-size"));

        String stanzaValue = parser.getAttributeValue("", "stanza");
        StanzaType stanza;
        if (stanzaValue == null) {
            stanza = StanzaType.IQ;
        }
        else {
            stanza = StanzaType.valueOf(stanzaValue.toUpperCase(Locale.US));
        }

        parser.next();

        return new Open(sessionID, blockSize, stanza);
    }

}
