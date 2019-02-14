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
import java.text.ParseException;
import java.util.ArrayList;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Provider for JingleContentDescriptionFileTransfer elements.
 */
public class JingleFileTransferProvider
        extends JingleContentDescriptionProvider<JingleFileTransfer> {

    @Override
    public JingleFileTransfer parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackParsingException {
        ArrayList<JingleContentDescriptionChildElement> payloads = new ArrayList<>();
        boolean inRange = false;
        JingleFileTransferChild.Builder builder = JingleFileTransferChild.getBuilder();
        HashElement inRangeHash = null;

        int offset = 0;
        int length = -1;

        while (true) {

            int tag = parser.nextTag();
            String elem = parser.getName();

            if (tag == START_TAG) {
                switch (elem) {
                    case JingleFileTransferChild.ELEM_DATE:
                    try {
                        builder.setDate(XmppDateTime.parseXEP0082Date(parser.nextText()));
                    } catch (ParseException e) {
                        throw new SmackParsingException.SmackTextParseException(e);
                    }
                        break;

                    case JingleFileTransferChild.ELEM_DESC:
                        builder.setDescription(parser.nextText());
                        break;

                    case JingleFileTransferChild.ELEM_MEDIA_TYPE:
                        builder.setMediaType(parser.nextText());
                        break;

                    case JingleFileTransferChild.ELEM_NAME:
                        builder.setName(parser.nextText());
                        break;

                    case JingleFileTransferChild.ELEM_SIZE:
                        builder.setSize(Integer.parseInt(parser.nextText()));
                        break;

                    case Range.ELEMENT:
                        inRange = true;
                        String offsetString = parser.getAttributeValue(null, Range.ATTR_OFFSET);
                        String lengthString = parser.getAttributeValue(null, Range.ATTR_LENGTH);
                        offset = (offsetString != null ? Integer.parseInt(offsetString) : 0);
                        length = (lengthString != null ? Integer.parseInt(lengthString) : -1);

                        if (parser.isEmptyElementTag()) {
                            inRange = false;
                            builder.setRange(new Range(offset, length));
                        }
                        break;

                    case HashElement.ELEMENT:
                        if (inRange) {
                            inRangeHash = new HashElementProvider().parse(parser);
                        } else {
                            builder.setHash(new HashElementProvider().parse(parser));
                        }
                        break;
                }

            } else if (tag == END_TAG) {
                switch (elem) {

                    case Range.ELEMENT:
                        inRange = false;
                        builder.setRange(new Range(offset, length, inRangeHash));
                        inRangeHash = null;
                        break;

                    case JingleFileTransferChild.ELEMENT:
                        payloads.add(builder.build());
                        builder = JingleFileTransferChild.getBuilder();
                        break;

                    case JingleFileTransfer.ELEMENT:
                        return new JingleFileTransfer(payloads);
                }
            }
        }
    }
}
