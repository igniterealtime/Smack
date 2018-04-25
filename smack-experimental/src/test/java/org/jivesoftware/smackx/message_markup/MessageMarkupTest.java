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
package org.jivesoftware.smackx.message_markup;

import static junit.framework.TestCase.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.util.List;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.message_markup.element.BlockQuoteElement;
import org.jivesoftware.smackx.message_markup.element.CodeBlockElement;
import org.jivesoftware.smackx.message_markup.element.ListElement;
import org.jivesoftware.smackx.message_markup.element.MarkupElement;
import org.jivesoftware.smackx.message_markup.element.SpanElement;
import org.jivesoftware.smackx.message_markup.provider.MarkupElementProvider;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class MessageMarkupTest extends SmackTestSuite {

    @Test
    public void emphasisTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<span start='9' end='15'>" +
                        "<emphasis/>" +
                    "</span>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setEmphasis(9, 15);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        SpanElement spanElement = (SpanElement) children.get(0);
        assertEquals(9, spanElement.getStart());
        assertEquals(15, spanElement.getEnd());
        assertEquals(1, spanElement.getStyles().size());
        assertEquals(SpanElement.SpanStyle.emphasis, spanElement.getStyles().iterator().next());
    }

    @Test
    public void codeTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<span start='9' end='15'>" +
                        "<code/>" +
                    "</span>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setCode(9, 15);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        SpanElement spanElement = (SpanElement) children.get(0);
        assertEquals(9, spanElement.getStart());
        assertEquals(15, spanElement.getEnd());
        assertEquals(1, spanElement.getStyles().size());
        assertEquals(SpanElement.SpanStyle.code, spanElement.getStyles().iterator().next());
    }

    @Test
    public void deletedTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<span start='9' end='15'>" +
                        "<deleted/>" +
                    "</span>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setDeleted(9, 15);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        SpanElement spanElement = (SpanElement) children.get(0);
        assertEquals(9, spanElement.getStart());
        assertEquals(15, spanElement.getEnd());
        assertEquals(1, spanElement.getStyles().size());
        assertEquals(SpanElement.SpanStyle.deleted, spanElement.getStyles().iterator().next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongStartEndTest() {
        MarkupElement.getBuilder().setEmphasis(12, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void overlappingSpansTest() {
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setEmphasis(0, 10);
        m.setDeleted(5, 15);
    }

    @Test
    public void codeBlockTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<bcode start='23' end='48'/>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setCodeBlock(23, 48);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        CodeBlockElement codeBlock = (CodeBlockElement) children.get(0);
        assertEquals(23, codeBlock.getStart());
        assertEquals(48, codeBlock.getEnd());
    }

    @Test
    public void listTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<list start='31' end='89'>" +
                        "<li start='31'/>" +
                        "<li start='47'/>" +
                        "<li start='61'/>" +
                        "<li start='69'/>" +
                    "</list>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m = m.beginList()
                .addEntry(31, 47)
                .addEntry(47, 61)
                .addEntry(61, 69)
                .addEntry(69, 89)
                .endList();
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        ListElement list = (ListElement) children.get(0);
        assertEquals(31, list.getStart());
        assertEquals(89, list.getEnd());
        assertEquals(4, list.getEntries().size());
        assertEquals(list.getStart(), list.getEntries().get(0).getStart());
        assertEquals(47, list.getEntries().get(1).getStart());
    }

    @Test(expected = IllegalArgumentException.class)
    public void listWrongSecondEntryTest() {
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.beginList().addEntry(0,1).addEntry(3,4);
    }

    @Test
    public void blockQuoteTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<bquote start='9' end='32'/>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setBlockQuote(9 ,32);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(1, children.size());

        BlockQuoteElement quote = (BlockQuoteElement) children.get(0);
        assertEquals(9, quote.getStart());
        assertEquals(32, quote.getEnd());
    }

    @Test
    public void nestedBlockQuoteTest() throws Exception {
        String xml =
                "<markup xmlns='urn:xmpp:markup:0'>" +
                    "<bquote start='0' end='57'/>" +
                    "<bquote start='11' end='34'/>" +
                "</markup>";
        MarkupElement.Builder m = MarkupElement.getBuilder();
        m.setBlockQuote(0, 57);
        m.setBlockQuote(11, 34);
        assertXMLEqual(xml, m.build().toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(xml);
        MarkupElement parsed = new MarkupElementProvider().parse(parser);
        List<MarkupElement.MarkupChildElement> children = parsed.getChildElements();
        assertEquals(2, children.size());

        BlockQuoteElement q1 = (BlockQuoteElement) children.get(0);
        BlockQuoteElement q2 = (BlockQuoteElement) children.get(1);

        assertEquals(0, q1.getStart());
        assertEquals(57, q1.getEnd());

        assertEquals(11, q2.getStart());
        assertEquals(34, q2.getEnd());
    }
}
