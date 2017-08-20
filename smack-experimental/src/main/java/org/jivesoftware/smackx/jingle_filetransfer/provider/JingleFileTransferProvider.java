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

import java.util.ArrayList;

import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for JingleContentDescriptionFileTransfer elements.
 */
public class JingleFileTransferProvider
        extends JingleContentDescriptionProvider<JingleFileTransferElement> {

    @Override
    public JingleFileTransferElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        ArrayList<JingleContentDescriptionChildElement> payloads = new ArrayList<>();
        boolean inRange = false;
        JingleFileTransferChildElement.Builder builder = JingleFileTransferChildElement.getBuilder();
        HashElement inRangeHash = null;
        Long length = null, offset = null;
        while (true) {

            int tag = parser.nextTag();
            String elem = parser.getName();

            if (tag == START_TAG) {
                switch (elem) {
                    case JingleFileTransferChildElement.ELEM_DATE:
                        //builder.setDate(XmppDateTime.parseXEP0082Date(parser.nextText()));
                        parser.nextText();
                        break;

                    case JingleFileTransferChildElement.ELEM_DESC:
                        builder.setDescription(parser.nextText());
                        break;

                    case JingleFileTransferChildElement.ELEM_MEDIA_TYPE:
                        builder.setMediaType(parser.nextText());
                        break;

                    case JingleFileTransferChildElement.ELEM_NAME:
                        builder.setName(parser.nextText());
                        break;

                    case JingleFileTransferChildElement.ELEM_SIZE:
                        builder.setSize(Integer.parseInt(parser.nextText()));
                        break;

                    case Range.ELEMENT:
                        inRange = true;
                        offset = ParserUtils.getLongAttribute(parser, Range.ATTR_OFFSET);
                        length = ParserUtils.getLongAttribute(parser, Range.ATTR_LENGTH);

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

                    case JingleFileTransferChildElement.ELEMENT:
                        payloads.add(builder.build());
                        builder = JingleFileTransferChildElement.getBuilder();
                        break;

                    case JingleFileTransferElement.ELEMENT:
                        return new JingleFileTransferElement(payloads);
                }
            }
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }
}
