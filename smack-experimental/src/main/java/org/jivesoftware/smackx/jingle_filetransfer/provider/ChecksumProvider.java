/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.provider;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_filetransfer.element.Checksum;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Provider for the Checksum element.
 */
public class ChecksumProvider extends ExtensionElementProvider<Checksum> {
    @Override
    public Checksum parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        JingleContent.Creator creator = null;
        String creatorString = parser.getAttributeValue(null, Checksum.ATTR_CREATOR);
        if (creatorString != null) {
            creator = JingleContent.Creator.valueOf(creatorString);
        }
        String name = parser.getAttributeValue(null, Checksum.ATTR_NAME);


        JingleFileTransferChild.Builder cb = JingleFileTransferChild.getBuilder();
        HashElement hashElement = null;
        Range range = null;

        boolean go = true;
        while (go) {
            int tag = parser.nextTag();
            String n = parser.getText();

            if (tag == START_TAG) {
                switch (n) {
                    case HashElement.ELEMENT:
                        hashElement = new HashElementProvider().parse(parser);
                        break;

                    case Range.ELEMENT:
                        String offset = parser.getAttributeValue(null, Range.ATTR_OFFSET);
                        String length = parser.getAttributeValue(null, Range.ATTR_LENGTH);
                        int o = offset == null ? 0 : Integer.parseInt(offset);
                        int l = length == null ? -1 : Integer.parseInt(length);
                        range = new Range(o, l);
                }
            } else if (tag == END_TAG) {
                switch (n) {
                    case Range.ELEMENT:
                        if (hashElement != null && range != null) {
                            range = new Range(range.getOffset(), range.getLength(), hashElement);
                            hashElement = null;
                        }
                        break;

                    case JingleFileTransferChild.ELEMENT:
                        if (hashElement != null) {
                            cb.setHash(hashElement);
                        }
                        if (range != null) {
                            cb.setRange(range);
                        }
                        go = false;
                }
            }
        }
        return new Checksum(creator, name, cb.build());
    }
}
