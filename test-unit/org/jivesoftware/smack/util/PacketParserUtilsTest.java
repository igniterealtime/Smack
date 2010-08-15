/**
 * $Revision:$
 * $Date:$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smack.util;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Properties;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
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
                        .parseMessage(getParser(control));

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

        message = (Message) PacketParserUtils.parseMessage(getParser(control));

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
        
        message = (Message) PacketParserUtils.parseMessage(getParser(control));

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

        message = (Message) PacketParserUtils.parseMessage(getParser(control));

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

        message = (Message) PacketParserUtils.parseMessage(getParser(control));

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

        message = (Message) PacketParserUtils.parseMessage(getParser(control));

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

        message = (Message) PacketParserUtils.parseMessage(getParser(control));

        assertNull(message.getBody());
        assertFalse(message.getBodyLanguages().isEmpty());
        assertTrue(message.getBodyLanguages().contains(defaultLanguage));
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertNull(message.getBody(otherLanguage));
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
                        .parseMessage(getParser(control));

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
                        .parseMessage(getParser(control));

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
                        .parseMessage(getParser(control));

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
                        .parseMessage(getParser(control));

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
                        .parseMessage(getParser(control));

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
                        .parseMessage(getParser(control));

        assertEquals(defaultLanguage, message.getBody());
        assertEquals(defaultLanguage, message.getBody(defaultLanguage));
        assertEquals(1, message.getBodies().size());
        assertEquals(0, message.getBodyLanguages().size());
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
            Message message = (Message) PacketParserUtils.parseMessage(getParser(control));
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
            PacketParserUtils.parseMessage(getParser(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </span>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </body> Body");
        
        try {
            PacketParserUtils.parseMessage(getParser(invalidControl));
            fail("Exception should be thrown");
        } catch(XmlPullParserException e) {
            assertTrue(e.getMessage().contains("end tag name </body>"));
        }

        invalidControl = validControl.replace("Good Message Body", "Bad </message> Body");
        
        try {
            PacketParserUtils.parseMessage(getParser(invalidControl));
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

        
        Packet message = PacketParserUtils.parseMessage(getParser(control));
        assertXMLEqual(control, message.toXML());
    }

    private XmlPullParser getParser(String control) throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();
        parser.setInput(new StringReader(control));
        while(true) {
            if(parser.next() == XmlPullParser.START_TAG
                    && parser.getName().equals("message")) { break; }
        }
        return parser;
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
