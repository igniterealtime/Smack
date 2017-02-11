/**
 *
 * Copyright (C) 2007 Jive Software.
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
package org.jivesoftware.smack.util;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLNotEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.test.util.XmlUnitUtils;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;

public class PacketParserUtilsTest {

    private static Properties outputProperties = new Properties();
    {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void singleMessageBodyTest() throws Exception {
        String defaultLanguage = Stanza.getDefaultLanguage();
        String otherLanguage = determineNonDefaultLanguage();

        String control;

        // message has default language, body has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("body")
                .t(defaultLanguage)
            .asString(outputProperties);

        Message message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has non-default language, body has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("body")
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(otherLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, body has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, body has default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));

        // body attribute xml:lang is unnecessary
        assertXMLNotEqual(control, message.toXML().toString());

        // message has no language, body has non-default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message =  PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has default language, body has non-default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("body")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has non-default language, body has default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("body")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(defaultLanguage));
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

    }

    @Test
    public void singleMessageSubjectTest() throws Exception {
        String defaultLanguage = Stanza.getDefaultLanguage();
        String otherLanguage = determineNonDefaultLanguage();

        String control;

        // message has default language, subject has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("subject")
                .t(defaultLanguage)
            .asString(outputProperties);

        Message message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has non-default language, subject has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("subject")
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(otherLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, subject has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, subject has default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));

        // subject attribute xml:lang is unnecessary
        assertXMLNotEqual(control, message.toXML().toString());

        // message has no language, subject has non-default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has default language, subject has non-default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("subject")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has non-default language, subject has default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("subject")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(defaultLanguage));
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

    }

    @Test
    public void multipleMessageBodiesTest() throws Exception {
        String defaultLanguage = Stanza.getDefaultLanguage();
        String otherLanguage = determineNonDefaultLanguage();

        String control;
        Message message;

        // message has default language, first body no language, second body other language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("body")
                .t(defaultLanguage)
            .up()
            .e("body")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has default language, first body no language, second body default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("body")
                .t(defaultLanguage)
            .up()
            .e("body")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage + "2")
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

        // message has non-default language, first body no language, second body default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("body")
                .t(otherLanguage)
            .up()
            .e("body")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(otherLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertTrue(message.getBodyLanguages().contains(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, first body no language, second body default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .t(defaultLanguage)
            .up()
            .e("body")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage + "2")
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

        // message has no language, first body no language, second body other language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .t(defaultLanguage)
            .up()
            .e("body")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, first body no language, second body no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .t(defaultLanguage)
            .up()
            .e("body")
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

    }

    @Test
    public void multipleMessageSubjectsTest() throws Exception {
        String defaultLanguage = Stanza.getDefaultLanguage();
        String otherLanguage = determineNonDefaultLanguage();

        String control;
        Message message;

        // message has default language, first subject no language, second subject other language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("subject")
                .t(defaultLanguage)
            .up()
            .e("subject")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has default language, first subject no language, second subject default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", defaultLanguage)
            .e("subject")
                .t(defaultLanguage)
            .up()
            .e("subject")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage + "2")
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

        // message has non-default language, first subject no language, second subject default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", otherLanguage)
            .e("subject")
                .t(otherLanguage)
            .up()
            .e("subject")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(otherLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertTrue(message.getSubjectLanguages().contains(defaultLanguage));
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, first subject no language, second subject default language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .t(defaultLanguage)
            .up()
            .e("subject")
                .a("xml:lang", defaultLanguage)
                .t(defaultLanguage + "2")
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

        // message has no language, first subject no language, second subject other language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .t(defaultLanguage)
            .up()
            .e("subject")
                .a("xml:lang", otherLanguage)
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertXMLEqual(control, message.toXML().toString());

        // message has no language, first subject no language, second subject no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .t(defaultLanguage)
            .up()
            .e("subject")
                .t(otherLanguage)
            .asString(outputProperties);

        message = PacketParserUtils
                        .parseMessage(PacketParserUtils.getParserFor(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML().toString());

    }

    /**
     * RFC6121 5.2.3 explicitly disallows mixed content in <body/> elements. Make sure that we throw
     * an exception if we encounter such an element.
     * 
     * @throws Exception
     */
    @Test(expected=XmlPullParserException.class)
    public void invalidMessageBodyContainingTagTest() throws Exception {
        String control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", "en")
            .e("body")
                .a("xmlns", "http://www.w3.org/1999/xhtml")
                .e("span")
                    .a("style", "font-weight: bold;")
                    .t("Bad Message Body")
            .asString(outputProperties);

        Message message = PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        fail("Should throw exception. Instead got message: " + message.toXML().toString());
    }

    @Test
    public void invalidXMLInMessageBody() throws Exception {
        String validControl = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", "en")
            .e("body")
                .t("Good Message Body")
            .asString(outputProperties);

        String invalidControl = validControl.replace("Good Message Body", "Bad </span> Body");

        try {
            PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </span>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </body> Body");

        try {
            PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </body>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </message> Body");

        try {
            PacketParserUtils.parseMessage(PacketParserUtils.getParserFor(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </message>"));
        }

    }

    @Test
    public void multipleMessageBodiesParsingTest() throws Exception {
        String control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .a("xml:lang", "en")
            .e("body")
                .t("This is a test of the emergency broadcast system, 1.")
            .up()
            .e("body")
                .a("xml:lang", "ru")
                .t("This is a test of the emergency broadcast system, 2.")
            .up()
            .e("body")
                .a("xml:lang", "sp")
                .t("This is a test of the emergency broadcast system, 3.")
            .asString(outputProperties);

        Stanza message = PacketParserUtils.parseStanza(control);
        XmlUnitUtils.assertSimilar(control, message.toXML());
    }

    @Test
    public void validateSimplePresence() throws Exception {
        // CHECKSTYLE:OFF
    	String stanza = "<presence from='juliet@example.com/balcony' to='romeo@example.net'/>";

    	Presence presence = PacketParserUtils.parsePresence(PacketParserUtils.getParserFor(stanza));

    	assertXMLEqual(stanza, presence.toXML().toString());
        // CHECKSTYLE:ON
    }

    @Test
    public void validatePresenceProbe() throws Exception {
        // CHECKSTYLE:OFF
    	String stanza = "<presence from='mercutio@example.com' id='xv291f38' to='juliet@example.com' type='unsubscribed'/>";

    	Presence presence = PacketParserUtils.parsePresence(PacketParserUtils.getParserFor(stanza));

    	assertXMLEqual(stanza, presence.toXML().toString());
    	assertEquals(Presence.Type.unsubscribed, presence.getType());
        // CHECKSTYLE:ON
    }

    @Test
    public void validatePresenceOptionalElements() throws Exception {
        // CHECKSTYLE:OFF
    	String stanza = "<presence xml:lang='en' type='unsubscribed'>"
    			+ "<show>dnd</show>"
    			+ "<status>Wooing Juliet</status>"
    			+ "<priority>1</priority>"
    			+ "</presence>";

    	Presence presence = PacketParserUtils.parsePresence(PacketParserUtils.getParserFor(stanza));
    	assertXMLEqual(stanza, presence.toXML().toString());
    	assertEquals(Presence.Type.unsubscribed, presence.getType());
    	assertEquals("dnd", presence.getMode().name());
    	assertEquals("en", presence.getLanguage());
    	assertEquals("Wooing Juliet", presence.getStatus());
    	assertEquals(1, presence.getPriority());
        // CHECKSTYLE:ON
    }

    @Test
    public void parseContentDepthTest() throws Exception {
        final String stanza = "<iq type='result' to='foo@bar.com' from='baz.com' id='42'/>";
        XmlPullParser parser = TestUtils.getParser(stanza, "iq");
        CharSequence content = PacketParserUtils.parseContent(parser);
        assertEquals("", content);
    }

    @Test
    public void parseElementMultipleNamespace()
                    throws ParserConfigurationException,
                    FactoryConfigurationError, XmlPullParserException,
                    IOException, TransformerException, SAXException {
        // @formatter:off
        final String stanza = XMLBuilder.create("outer", "outerNamespace").a("outerAttribute", "outerValue")
                        .element("inner", "innerNamespace").a("innverAttribute", "innerValue")
                            .element("innermost")
                                .t("some text")
                        .asString();
        // @formatter:on
        XmlPullParser parser = TestUtils.getParser(stanza, "outer");
        CharSequence result = PacketParserUtils.parseElement(parser, true);
        assertXMLEqual(stanza, result.toString());
    }

    @Test
    public void parseSASLFailureSimple() throws FactoryConfigurationError, SAXException, IOException,
                    TransformerException, ParserConfigurationException, XmlPullParserException {
        // @formatter:off
        final String saslFailureString = XMLBuilder.create(SASLFailure.ELEMENT, SaslStreamElements.NAMESPACE)
                        .e(SASLError.account_disabled.toString())
                        .asString();
        // @formatter:on
        XmlPullParser parser = TestUtils.getParser(saslFailureString, SASLFailure.ELEMENT);
        SASLFailure saslFailure = PacketParserUtils.parseSASLFailure(parser);
        assertXMLEqual(saslFailureString, saslFailure.toString());
    }

    @Test
    public void parseSASLFailureExtended() throws FactoryConfigurationError, TransformerException,
                    ParserConfigurationException, XmlPullParserException, IOException, SAXException {
        // @formatter:off
        final String saslFailureString = XMLBuilder.create(SASLFailure.ELEMENT, SaslStreamElements.NAMESPACE)
                        .e(SASLError.account_disabled.toString())
                        .up()
                        .e("text").a("xml:lang", "en")
                            .t("Call 212-555-1212 for assistance.")
                        .up()
                        .e("text").a("xml:lang", "de")
                            .t("Bitte wenden sie sich an (04321) 123-4444")
                        .up()
                        .e("text")
                            .t("Wusel dusel")
                        .asString();
        // @formatter:on
        XmlPullParser parser = TestUtils.getParser(saslFailureString, SASLFailure.ELEMENT);
        SASLFailure saslFailure = PacketParserUtils.parseSASLFailure(parser);
        XmlUnitUtils.assertSimilar(saslFailureString, saslFailure.toXML());
    }

    @SuppressWarnings("ReferenceEquality")
    private static String determineNonDefaultLanguage() {
        String otherLanguage = "jp";
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (int i = 0; i < availableLocales.length; i++) {
            if (availableLocales[i] != Locale.getDefault()) {
                otherLanguage = availableLocales[i].getLanguage().toLowerCase(Locale.US);
                // Check for empty strings as Java8 returns those here for certain Locales
                if (otherLanguage.length() > 0) {
                    break;
                }
            }
        }
        return otherLanguage;
    }

}
