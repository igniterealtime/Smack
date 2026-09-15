/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.bind2.element;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.bind2.provider.Bind2Provider;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Bind2ElementsTest {

    @BeforeAll
    public static void setup() throws Exception {
        Class.forName(Bind2ModuleDescriptor.class.getName());
    }

    @Test
    public void testBindSerialization() throws Exception {
        Bind2Elements.Bind bind = new Bind2Elements.Bind(
            Collections.singleton("urn:xmpp:carbons:2"),
            null,
            Collections.emptyList()
        );

        String xml = bind.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bind xmlns='urn:xmpp:bind:0'><inline><feature var='urn:xmpp:carbons:2'/></inline></bind>", xml);
    }

    @Test
    public void testBindMultipleInlineFeaturesSerialization() throws Exception {
        Set<String> features = new LinkedHashSet<>(Arrays.asList("urn:xmpp:carbons:2", "urn:xmpp:csi:0", "urn:xmpp:sm:3"));
        Bind2Elements.Bind bind = new Bind2Elements.Bind(features, null, null);

        String xml = bind.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bind xmlns='urn:xmpp:bind:0'><inline><feature var='urn:xmpp:carbons:2'/><feature var='urn:xmpp:csi:0'/><feature var='urn:xmpp:sm:3'/></inline></bind>", xml);
    }

    @Test
    public void testBindClientSerialization() throws Exception {
        Bind2Elements.Bind bind = new Bind2Elements.Bind(
            null,
            "AwesomeXMPP",
            Collections.emptyList()
        );

        String xml = bind.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bind xmlns='urn:xmpp:bind:0'><tag>AwesomeXMPP</tag></bind>", xml);
    }

    @Test
    public void testEmptyBindSerialization() throws Exception {
        Bind2Elements.Bind bind = new Bind2Elements.Bind(null, null, null);
        String xml = bind.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bind xmlns='urn:xmpp:bind:0'/>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testBindParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<bind xmlns='urn:xmpp:bind:0'><tag>AwesomeXMPP</tag></bind>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Bind2Elements.Bind bind = Bind2Provider.BindProvider.INSTANCE.parse(
            parser,
            parser.getDepth(),
            XmlEnvironment.EMPTY,
            null
        );

        assertEquals("AwesomeXMPP", bind.getTag());
        assertTrue(bind.getInlineFeatures().isEmpty());
        assertTrue(bind.getExtensionElements().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testBindInlineParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<bind xmlns='urn:xmpp:bind:0'><inline><feature var='urn:xmpp:carbons:2'/><feature var='urn:xmpp:csi:0'/></inline></bind>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Bind2Elements.Bind bind = Bind2Provider.BindProvider.INSTANCE.parse(
            parser,
            parser.getDepth(),
            XmlEnvironment.EMPTY,
            null
        );

        assertNull(bind.getTag());
        assertEquals(2, bind.getInlineFeatures().size());
        assertTrue(bind.getInlineFeatures().contains("urn:xmpp:carbons:2"));
        assertTrue(bind.getInlineFeatures().contains("urn:xmpp:csi:0"));
        assertTrue(bind.getExtensionElements().isEmpty());
    }

    @Test
    public void testBindWithBothSerialization() throws Exception {
        StandardExtensionElement dummyElement = StandardExtensionElement.builder("x", "urn:test").build();

        Bind2Elements.Bind bind = new Bind2Elements.Bind(
            "AwesomeXMPP",
            Collections.singletonList(dummyElement)
        );
        String xml = bind.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bind xmlns='urn:xmpp:bind:0'><tag>AwesomeXMPP</tag><x xmlns='urn:test'/></bind>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testBindParsingWithBoth(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<bind xmlns='urn:xmpp:bind:0'><inline><feature var='urn:xmpp:carbons:2'/></inline><tag>AwesomeXMPP</tag><x xmlns='urn:test'/></bind>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        assertThrows(IllegalArgumentException.class, () ->
            Bind2Provider.BindProvider.INSTANCE.parse(
                parser, parser.getDepth(), XmlEnvironment.EMPTY, null
            )
        );
    }

    @Test
    public void testBoundSerialization() throws Exception {
        StandardExtensionElement mamMetadata = StandardExtensionElement.builder("metadata", "urn:xmpp:mam:2").build();
        Bind2Elements.Bound bound = new Bind2Elements.Bound(mamMetadata);

        String xml = bound.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bound xmlns='urn:xmpp:bind:0'><metadata xmlns='urn:xmpp:mam:2'/></bound>", xml);
    }

    @Test
    public void testEmptyBoundSerialization() throws Exception {
        Bind2Elements.Bound bound = new Bind2Elements.Bound(null);
        String xml = bound.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<bound xmlns='urn:xmpp:bind:0'/>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testBoundParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<bound xmlns='urn:xmpp:bind:0'><metadata xmlns='urn:xmpp:mam:2'><start id='alpha' timestamp='2020-01-01T00:00:00Z'/></metadata></bound>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Bind2Elements.Bound bound = Bind2Provider.BoundProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );

        assertNotNull(bound.getMamMetadata());
        assertEquals("metadata", bound.getMamMetadata().getElementName());
        assertEquals("urn:xmpp:mam:2", bound.getMamMetadata().getNamespace());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testEmptyBoundParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<bound xmlns='urn:xmpp:bind:0'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Bind2Elements.Bound bound = Bind2Provider.BoundProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );

        assertNull(bound.getMamMetadata());
    }
}
