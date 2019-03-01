/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smackx.message_markup.provider;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.message_markup.element.BlockQuoteElement;
import org.jivesoftware.smackx.message_markup.element.CodeBlockElement;
import org.jivesoftware.smackx.message_markup.element.ListElement;
import org.jivesoftware.smackx.message_markup.element.MarkupElement;
import org.jivesoftware.smackx.message_markup.element.SpanElement;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MarkupElementProvider extends ExtensionElementProvider<MarkupElement> {

    @Override
    public MarkupElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException {

        MarkupElement.Builder markup = MarkupElement.getBuilder();

        int spanStart = -1, spanEnd = -1;
        Set<SpanElement.SpanStyle> spanStyles = new HashSet<>();

        int listStart = -1, listEnd = -1;
        List<ListElement.ListEntryElement> lis = new ArrayList<>();

        while (true) {
            int tag = parser.next();
            String name = parser.getName();
            int start, end;
            switch (tag) {
                case START_TAG:
                    switch (name) {
                        case BlockQuoteElement.ELEMENT:
                            start = ParserUtils.getIntegerAttributeOrThrow(parser, BlockQuoteElement.ATTR_START,
                                    "Message Markup BlockQuoteElement MUST contain a 'start' attribute.");
                            end = ParserUtils.getIntegerAttributeOrThrow(parser, BlockQuoteElement.ATTR_END,
                                    "Message Markup BlockQuoteElement MUST contain a 'end' attribute.");
                            markup.setBlockQuote(start, end);
                            break;

                        case CodeBlockElement.ELEMENT:
                            start = ParserUtils.getIntegerAttributeOrThrow(parser, CodeBlockElement.ATTR_START,
                                    "Message Markup CodeBlockElement MUST contain a 'start' attribute.");
                            end = ParserUtils.getIntegerAttributeOrThrow(parser, CodeBlockElement.ATTR_END,
                                    "Message Markup CodeBlockElement MUST contain a 'end' attribute.");
                            markup.setCodeBlock(start, end);
                            break;

                        case SpanElement.ELEMENT:
                            spanStyles = new HashSet<>();
                            spanStart = ParserUtils.getIntegerAttributeOrThrow(parser, SpanElement.ATTR_START,
                                    "Message Markup SpanElement MUST contain a 'start' attribute.");
                            spanEnd = ParserUtils.getIntegerAttributeOrThrow(parser, SpanElement.ATTR_END,
                                    "Message Markup SpanElement MUST contain a 'end' attribute.");
                            break;

                        case SpanElement.code:
                            spanStyles.add(SpanElement.SpanStyle.code);
                            break;

                        case SpanElement.emphasis:
                            spanStyles.add(SpanElement.SpanStyle.emphasis);
                            break;

                        case SpanElement.deleted:
                            spanStyles.add(SpanElement.SpanStyle.deleted);
                            break;

                        case ListElement.ELEMENT:
                            lis = new ArrayList<>();
                            listStart = ParserUtils.getIntegerAttributeOrThrow(parser, ListElement.ATTR_START,
                                    "Message Markup ListElement MUST contain a 'start' attribute.");
                            listEnd = ParserUtils.getIntegerAttributeOrThrow(parser, ListElement.ATTR_END,
                                    "Message Markup ListElement MUST contain a 'end' attribute.");
                            break;

                        case ListElement.ELEM_LI:
                            start = ParserUtils.getIntegerAttributeOrThrow(parser, ListElement.ATTR_START,
                                    "Message Markup ListElement 'li' MUST contain a 'start' attribute.");
                            lis.add(new ListElement.ListEntryElement(start));
                            break;
                    }
                    break;

                case END_TAG:
                    switch (name) {
                        case SpanElement.ELEMENT:
                            markup.addSpan(spanStart, spanEnd, spanStyles);
                            spanStart = -1; spanEnd = -1;
                            break;

                        case ListElement.ELEMENT:
                            MarkupElement.Builder.ListBuilder listBuilder = markup.beginList();
                            if (lis.size() > 0 && lis.get(0).getStart() != listStart) {
                                // TODO: Should be SmackParseException.
                                throw new IOException("Error while parsing incoming MessageMarkup ListElement: " +
                                        "'start' attribute of first 'li' element must equal 'start' attribute of list.");
                            }
                            for (int i = 0; i < lis.size(); i++) {
                                int elemStart = lis.get(i).getStart();
                                int elemEnd = i < lis.size() - 1 ? lis.get(i + 1).getStart() : listEnd;
                                listBuilder.addEntry(elemStart, elemEnd);
                            }
                            listBuilder.endList();
                            break;

                        case MarkupElement.ELEMENT:
                            return markup.build();
                    }

            }
        }
    }

}
