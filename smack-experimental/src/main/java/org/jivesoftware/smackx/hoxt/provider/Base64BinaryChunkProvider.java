/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hoxt.packet.Base64BinaryChunk;

/**
 * Stanza provider for base64 binary chunks.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class Base64BinaryChunkProvider extends ExtensionElementProvider<Base64BinaryChunk> {

    @Override
    public Base64BinaryChunk parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        String streamId = parser.getAttributeValue("", Base64BinaryChunk.ATTRIBUTE_STREAM_ID);
        String nrString = parser.getAttributeValue("", Base64BinaryChunk.ATTRIBUTE_NR);
        String lastString = parser.getAttributeValue("", Base64BinaryChunk.ATTRIBUTE_LAST);
        boolean last = false;
        int nr = Integer.parseInt(nrString);

        if (lastString != null) {
            last = Boolean.parseBoolean(lastString);
        }

        String text = null;
        boolean done = false;

        while (!done) {
            XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals(Base64BinaryChunk.ELEMENT_CHUNK)) {
                    done = true;
                } else {
                    throw new IllegalArgumentException("unexpected end tag of: " + parser.getName());
                }
            } else if (eventType == XmlPullParser.Event.TEXT_CHARACTERS) {
                text = parser.getText();
            } else {
                throw new IllegalArgumentException("unexpected eventType: " + eventType);
            }
        }

        return new Base64BinaryChunk(text, streamId, nr, last);
    }
}
