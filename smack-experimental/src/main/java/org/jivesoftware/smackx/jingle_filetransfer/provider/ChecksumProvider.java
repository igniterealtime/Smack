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

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.ChecksumElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

import org.xmlpull.v1.XmlPullParser;


/**
 * Provider for the Checksum element.
 */
public class ChecksumProvider extends ExtensionElementProvider<ChecksumElement> {

    private static HashElementProvider hashProvider = new HashElementProvider();

    @Override
    public ChecksumElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        JingleContentElement.Creator creator = null;
        String creatorString = parser.getAttributeValue(null, ChecksumElement.ATTR_CREATOR);
        if (creatorString != null) {
            creator = JingleContentElement.Creator.valueOf(creatorString);
        }
        String name = parser.getAttributeValue(null, ChecksumElement.ATTR_NAME);


        JingleFileTransferChildElement.Builder cb = JingleFileTransferChildElement.getBuilder();
        HashElement hashElement = null;
        Range range = null;

        boolean go = true;
        while (go) {
            int tag = parser.nextTag();
            String n = parser.getName();

            if (tag == START_TAG) {
                switch (n) {
                    case HashElement.ELEMENT:
                        hashElement = hashProvider.parse(parser);
                        break;

                    case Range.ELEMENT:
                        Long offset = ParserUtils.getLongAttribute(parser, Range.ATTR_OFFSET);
                        Long length = ParserUtils.getLongAttribute(parser, Range.ATTR_LENGTH);
                        range = new Range(offset, length);
                }
            } else if (tag == END_TAG) {
                switch (n) {
                    case Range.ELEMENT:
                        if (hashElement != null && range != null) {
                            range = new Range(range.getOffset(), range.getLength(), hashElement);
                            hashElement = null;
                        }
                        break;

                    case JingleFileTransferChildElement.ELEMENT:
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
        return new ChecksumElement(creator, name, cb.build());
    }
}
