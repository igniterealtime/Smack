/**
 * $Revision:$
 * $Date:$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smack.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLNotEqual;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.jivesoftware.smack.TestUtils;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;

/**
 *
 */
public class PacketParserUtilsTest {
    
    private static Properties outputProperties = new Properties();
    {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
        
    @Test
    public void singleMessageBodyTest() throws Exception {
        String defaultLanguage = Packet.getDefaultLanguage();
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
        
        Message message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(otherLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML());
        
        // message has no language, body has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("body")
                .t(defaultLanguage)
            .asString(outputProperties);
        
        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertTrue(message.getBodyLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));

        // body attribute xml:lang is unnecessary
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertNull(message.getBody(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(defaultLanguage));
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
        assertXMLEqual(control, message.toXML());

    }

    @Test
    public void singleMessageSubjectTest() throws Exception {
        String defaultLanguage = Packet.getDefaultLanguage();
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
        
        Message message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(otherLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML());
        
        // message has no language, subject has no language
        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "zid615d9")
            .a("type", "chat")
            .e("subject")
                .t(defaultLanguage)
            .asString(outputProperties);
        
        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertTrue(message.getSubjectLanguages().isEmpty());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));

        // subject attribute xml:lang is unnecessary
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertNull(message.getSubject(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));

        assertNull(message.getSubject());
        assertFalse(message.getSubjectLanguages().isEmpty());
        assertTrue(message.getSubjectLanguages().contains(defaultLanguage));
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertNull(message.getSubject(otherLanguage));
        assertXMLEqual(control, message.toXML());

    }

    @Test
    public void multipleMessageBodiesTest() throws Exception {
        String defaultLanguage = Packet.getDefaultLanguage();
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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertTrue(message.getBodyLanguages().contains(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(otherLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertTrue(message.getBodyLanguages().contains(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(otherLanguage, message.getBody(otherLanguage));
        assertEquals(2, message.getBodies().size());
        assertEquals(1, message.getBodyLanguages().size());
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
        assertXMLNotEqual(control, message.toXML());

    }

    @Test
    public void multipleMessageSubjectsTest() throws Exception {
        String defaultLanguage = Packet.getDefaultLanguage();
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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertTrue(message.getSubjectLanguages().contains(otherLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(otherLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertTrue(message.getSubjectLanguages().contains(defaultLanguage));
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(otherLanguage, message.getSubject(otherLanguage));
        assertEquals(2, message.getSubjects().size());
        assertEquals(1, message.getSubjectLanguages().size());
        assertXMLEqual(control, message.toXML());

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

        message = (Message) PacketParserUtils
                        .parseMessage(TestUtils.getMessageParser(control));

        assertEquals(defaultLanguage, message.getSubject());
        assertEquals(defaultLanguage, message.getSubject(defaultLanguage));
        assertEquals(1, message.getSubjects().size());
        assertEquals(0, message.getSubjectLanguages().size());
        assertXMLNotEqual(control, message.toXML());

    }

    @Test
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
        
        try {
            Message message = (Message) PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));
            String body = "<span style=\"font-weight: bold;\">"
                            + "Bad Message Body</span>";
            assertEquals(body, message.getBody());
            
            assertXMLNotEqual(control, message.toXML());
            
            DetailedDiff diffs = new DetailedDiff(new Diff(control, message.toXML()));
            
            // body has no namespace URI, span is escaped 
            assertEquals(4, diffs.getAllDifferences().size());
        } catch(XmlPullParserException e) {
            fail("No parser exception should be thrown" + e.getMessage());
        }
        
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
            PacketParserUtils.parseMessage(TestUtils.getMessageParser(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </span>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </body> Body");
        
        try {
            PacketParserUtils.parseMessage(TestUtils.getMessageParser(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </body>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </message> Body");
        
        try {
            PacketParserUtils.parseMessage(TestUtils.getMessageParser(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </message>"));
        }

    }

    @Ignore
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
        
        Packet message = PacketParserUtils.parseMessage(TestUtils.getMessageParser(control));
        assertXMLEqual(control, message.toXML());
    }
    
    @Test
    public void validateSimplePresence() throws Exception {
    	String stanza = "<presence from='juliet@example.com/balcony' to='romeo@example.net'/>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	
    	assertXMLEqual(stanza, presence.toXML());
    }
    
    @Test
    public void validatePresenceProbe() throws Exception {
    	String stanza = "<presence from='mercutio@example.com' id='xv291f38' to='juliet@example.com' type='unsubscribed'/>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	
    	assertXMLEqual(stanza, presence.toXML());
    	assertEquals(Presence.Type.unsubscribed, presence.getType());
    }
    
    @Test
    public void validatePresenceOptionalElements() throws Exception {
    	String stanza = "<presence xml:lang='en' type='unsubscribed'>"
    			+ "<show>dnd</show>"
    			+ "<status>Wooing Juliet</status>"
    			+ "<priority>1</priority>"
    			+ "</presence>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	assertXMLEqual(stanza, presence.toXML());
    	assertEquals(Presence.Type.unsubscribed, presence.getType());
    	assertEquals("dnd", presence.getMode().name());
    	assertEquals("en", presence.getLanguage());
    	assertEquals("Wooing Juliet", presence.getStatus());
    	assertEquals(1, presence.getPriority());
    }

    @Test
    public void validatePresenceWithDelayedDelivery() throws Exception {
    	String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
    			+ "<delay xmlns='urn:xmpp:delay' stamp='2002-09-10T23:41:07Z'/></presence>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	
    	DelayInformation delay = (DelayInformation) presence.getExtension("urn:xmpp:delay");
    	assertNotNull(delay);
    	Date date = StringUtils.parseDate("2002-09-10T23:41:07Z");
    	assertEquals(date, delay.getStamp());
    }

    @Test
    public void validatePresenceWithLegacyDelayed() throws Exception {
    	String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
    			+ "<x xmlns='jabber:x:delay' stamp='20020910T23:41:07'/></presence>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	
    	DelayInformation delay = (DelayInformation) presence.getExtension("jabber:x:delay");
    	assertNotNull(delay);
    	Date date = StringUtils.parseDate("20020910T23:41:07");
    	Calendar cal = Calendar.getInstance();
    	cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    	cal.setTime(date);
    	assertEquals(cal.getTime(), delay.getStamp());
    }
    
    @Test
    public void parsePresenceWithInvalidDelayedDelivery() throws Exception {
    	String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
    			+ "<delay xmlns='urn:xmpp:delay'/></presence>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	assertNull(presence.getExtension("urn:xmpp:delay"));
    }

    @Test
    public void parsePresenceWithInvalidLegacyDelayed() throws Exception {
    	String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
    			+ "<x xmlns='jabber:x:delay'/></presence>";
    	
    	Presence presence = PacketParserUtils.parsePresence(TestUtils.getPresenceParser(stanza));
    	DelayInformation delay = (DelayInformation) presence.getExtension("urn:xmpp:delay");
    	assertNull(delay);
    }

    private String determineNonDefaultLanguage() {
        String otherLanguage = "jp";
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (int i = 0; i < availableLocales.length; i++) {
            if (availableLocales[i] != Locale.getDefault()) {
                otherLanguage = availableLocales[i].getLanguage().toLowerCase();
                break;
            }
        }
        return otherLanguage;
    }

}
