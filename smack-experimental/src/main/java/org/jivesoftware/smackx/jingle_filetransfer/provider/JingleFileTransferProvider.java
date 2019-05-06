/**
 *
 * Copyright 2017 Paul Schaub, 2019 Florian Schmaus
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
import java.text.ParseException;
import java.util.ArrayList;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

import org.jxmpp.util.XmppDateTime;

/**
 * Provider for JingleContentDescriptionFileTransfer elements.
 */
public class JingleFileTransferProvider
        extends JingleContentDescriptionProvider<JingleFileTransfer> {

    @Override
    public JingleFileTransfer parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ArrayList<JingleContentDescriptionChildElement> payloads = new ArrayList<>();
        JingleFileTransferChild.Builder builder = JingleFileTransferChild.getBuilder();


        String elementName;
        while (true) {

            XmlPullParser.TagEvent tag = parser.nextTag();

            switch (tag) {
            case START_ELEMENT:
                elementName = parser.getName();
                switch (elementName) {
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
                        Range range = parseRangeElement(parser);
                        builder.setRange(range);

                        break;

                    case HashElement.ELEMENT:
                        HashElement hashElement = HashElementProvider.INSTANCE.parse(parser);
                        builder.setHash(hashElement);
                        break;
                }
                break;
            case END_ELEMENT:
                elementName = parser.getName();
                switch (elementName) {
                    case JingleFileTransferChild.ELEMENT:
                        payloads.add(builder.build());
                        builder = JingleFileTransferChild.getBuilder();
                        break;

                    case JingleFileTransfer.ELEMENT:
                        return new JingleFileTransfer(payloads);
                }
                break;
            }
        }
    }

    public static Range parseRangeElement(XmlPullParser parser) throws IOException, XmlPullParserException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        final Integer offset = ParserUtils.getIntegerAttribute(parser, Range.ATTR_OFFSET);
        final Integer length = ParserUtils.getIntegerAttribute(parser, Range.ATTR_LENGTH);

        HashElement hashElement = null;
        outerloop: while (true) {
            String element;
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                element = parser.getName();
                switch (element) {
                case HashElement.ELEMENT:
                    hashElement = HashElementProvider.INSTANCE.parse(parser);
                    break;
                }
                break;
            case END_ELEMENT:
                element = parser.getName();
                if (element.equals(Range.ELEMENT) && parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return new Range(offset, length, hashElement);
    }

}
