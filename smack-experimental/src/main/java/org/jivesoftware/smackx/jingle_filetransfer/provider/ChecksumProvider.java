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

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.ChecksumElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

/**
 * Provider for the Checksum element.
 */
public class ChecksumProvider extends ExtensionElementProvider<ChecksumElement> {

    private static final HashElementProvider hashProvider = new HashElementProvider();

    @Override
    public ChecksumElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
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
            XmlPullParser.TagEvent tag = parser.nextTag();
            String n = parser.getName();

            switch (tag) {
            case START_ELEMENT:
                switch (n) {
                    case HashElement.ELEMENT:
                        hashElement = hashProvider.parse(parser);
                        break;

                    case Range.ELEMENT:
                        Long offset = ParserUtils.getLongAttribute(parser, Range.ATTR_OFFSET);
                        Long length = ParserUtils.getLongAttribute(parser, Range.ATTR_LENGTH);
                        range = new Range(offset, length);
                }
                break;
            case END_ELEMENT:
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
                break;
            }
        }
        return new ChecksumElement(creator, name, cb.build());
    }
}
