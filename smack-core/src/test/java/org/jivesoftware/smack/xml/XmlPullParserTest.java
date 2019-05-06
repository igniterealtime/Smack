/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.jivesoftware.smack.test.util.SmackTestUtil;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class XmlPullParserTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testSimpleEmptyElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        Reader reader = new StringReader("<empty-element/>");
        XmlPullParser parser = parserKind.factory.newXmlPullParser(reader);

        assertEquals(XmlPullParser.Event.START_DOCUMENT, parser.getEventType());
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
        QName qname = parser.getQName();
        assertEquals(qname.getLocalPart(), "empty-element");
        assertEquals(qname.getPrefix(), XMLConstants.DEFAULT_NS_PREFIX);
        assertEquals(qname.getNamespaceURI(), XMLConstants.NULL_NS_URI);
        assertEquals(XmlPullParser.Event.END_ELEMENT, parser.next());
        assertEquals(XmlPullParser.Event.END_DOCUMENT, parser.next());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testQNameSimpleElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String simpleElement = "<outer-element xmlns='outer-namespace'><inner-element/></outer-element>";
        XmlPullParser parser = SmackTestUtil.getParserFor(simpleElement, parserKind);
        QName qname = parser.getQName();
        assertEquals("outer-element", qname.getLocalPart());
        assertEquals("outer-namespace", qname.getNamespaceURI());
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
        qname = parser.getQName();
        assertEquals("inner-element", qname.getLocalPart());
        assertEquals("outer-namespace", qname.getNamespaceURI());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testQNamePrefixElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String prefixElement = "<outer-element xmlns='outer-namespace' xmlns:inner-prefix='inner-namespace'><inner-prefix:inner-element></outer-element>";
        XmlPullParser parser = SmackTestUtil.getParserFor(prefixElement, parserKind);
        QName qname = parser.getQName();
        assertEquals("outer-element", qname.getLocalPart());
        assertEquals("outer-namespace", qname.getNamespaceURI());
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
        qname = parser.getQName();
        assertEquals("inner-element", qname.getLocalPart());
        assertEquals("inner-namespace", qname.getNamespaceURI());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAttributesElementWithOneAttribute(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String elementWithOneAttribute = "<element attribute-one='attribute-one-value'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(elementWithOneAttribute, parserKind);
        assertAttributeHolds(parser, 0, "attribute-one", "", "");
        assertThrows(NullPointerException.class, () ->
            assertAttributeHolds(parser, 1, "attribute-one", "", ""));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAttributesNamespacedElementWithOneAttribute(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String namespacedElementWithOneAttribute = "<element xmlns='element-namespace' attribute-one='attribute-one-value'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(namespacedElementWithOneAttribute, parserKind);
        assertAttributeHolds(parser, 0, "attribute-one", "", "");
        assertThrows(NullPointerException.class, () ->
            assertAttributeHolds(parser, 1, "attribute-one", "", ""));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAttributesNamespacedElementWithOneNamespacedAttribute(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String namespacedElementWithOneNamespacedAttribute = "<element xmlns='element-namespace' xmlns:attribute-namespace='attribute-namespace-value' attribute-namespace:attribute-one='attribute-one-value'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(namespacedElementWithOneNamespacedAttribute, parserKind);
        assertAttributeHolds(parser, 0, "attribute-one", "attribute-namespace", "attribute-namespace-value");
        assertThrows(NullPointerException.class, () ->
            assertAttributeHolds(parser, 1, "attribute-one", "", ""));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNamespacedAttributes(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String element = "<element xmlns:attr='attribute-namespace' attr:attributeOneName='attributeOneValue'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(element, parserKind);
        assertEquals(1, parser.getAttributeCount());

        assertEquals("attributeOneName", parser.getAttributeName(0));
        assertEquals("attr", parser.getAttributePrefix(0));
        assertEquals("attribute-namespace", parser.getAttributeNamespace(0));
        QName attributeZeroQname = parser.getAttributeQName(0);
        assertEquals("attributeOneName", attributeZeroQname.getLocalPart());
        assertEquals("attr", attributeZeroQname.getPrefix());
        assertEquals("attribute-namespace", attributeZeroQname.getNamespaceURI());

        // Test how parser handle non-existent attributes.
        assertNull(parser.getAttributeName(1));
        assertNull(parser.getAttributePrefix(1));
        assertNull(parser.getAttributeNamespace(1));
        QName attributeOneQname = parser.getAttributeQName(1);
        assertNull(attributeOneQname);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAttributeType(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String element = "<element xmlns:attr='attribute-namespace' attr:attributeOneName='attributeOneValue'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(element, parserKind);

        assertEquals("CDATA", parser.getAttributeType(0));

        assertNull(parser.getAttributeType(1));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNextText(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        XmlPullParser parser;

        String simpleElement = "<element>Element text</element>";
        parser = SmackTestUtil.getParserFor(simpleElement, parserKind);
        assertEquals("Element text", parser.nextText());

        String complexElement = "<outer-elment><element1>Element 1 &apos; text</element1><element2>Element 2 text</element2></outer-element>";
        parser = SmackTestUtil.getParserFor(complexElement, parserKind);
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
        assertEquals("element1", parser.getName());
        assertEquals(0, parser.getAttributeCount());
        assertEquals("Element 1 ' text", parser.nextText());

        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
        assertEquals("element2", parser.getName());
        assertEquals(0, parser.getAttributeCount());
        assertEquals("Element 2 text", parser.nextText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNextTextMixedContent(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String element = "<element>Mixed content element text<inner-element>Inner element text</inner-element></element>";
        XmlPullParser parser = SmackTestUtil.getParserFor(element, parserKind);
        assertThrows(XmlPullParserException.class, () -> parser.nextText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNextTextOnEndElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String element = "<element>Element text</element>";
        XmlPullParser parser = SmackTestUtil.getParserFor(element, parserKind);
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.getEventType());
        assertEquals(XmlPullParser.Event.TEXT_CHARACTERS, parser.next());
        assertEquals(XmlPullParser.Event.END_ELEMENT, parser.next());
        assertThrows(XmlPullParserException.class, () -> parser.nextText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNextTextOnEmptyElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        String[] emptyElementStream = Stream.of().toArray(String[]::new);
        for (String emptyElement : emptyElementStream) {
            XmlPullParser parser = SmackTestUtil.getParserFor(emptyElement, parserKind);
            assertEquals(XmlPullParser.Event.START_ELEMENT, parser.getEventType());
            assertEquals("", parser.nextText());
        }
    }

    private static void assertAttributeHolds(XmlPullParser parser, int attributeIndex, String expectedLocalpart,
                    String expectedPrefix, String expectedNamespace) {
        QName qname = parser.getAttributeQName(attributeIndex);
        String qnameNamespaceUri = qname.getNamespaceURI();

        assertEquals(expectedLocalpart, qname.getLocalPart());
        assertEquals(expectedPrefix, qname.getPrefix());
        assertEquals(expectedNamespace, qnameNamespaceUri);

        assertEquals(qname.getLocalPart(), parser.getAttributeName(attributeIndex));
        assertEquals(qname.getPrefix(), parser.getAttributePrefix(attributeIndex));

        final String expectedGetAttributeNamespace;
        if (qnameNamespaceUri.equals(XMLConstants.NULL_NS_URI)) {
            expectedGetAttributeNamespace = null;
        }
        else {
            expectedGetAttributeNamespace = qnameNamespaceUri;
        }
        assertEquals(expectedGetAttributeNamespace, parser.getAttributeNamespace(attributeIndex));
    }
}
