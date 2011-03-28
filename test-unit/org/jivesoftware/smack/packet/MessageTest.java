/**
 * $Revision:$
 * $Date:$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smack.packet;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

/**
 *
 */
public class MessageTest {

    @Test
    public void setMessageTypeTest() throws IOException, SAXException, ParserConfigurationException {
        Message.Type type = Message.Type.chat;
        Message.Type type2 = Message.Type.headline;

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message")
                .append(" type=\"")
                .append(type)
                .append("\">")
                .append("</message>");
        String control = controlBuilder.toString();

        Message messageTypeInConstructor = new Message(null, Message.Type.chat);
        messageTypeInConstructor.setPacketID(Packet.ID_NOT_AVAILABLE);
        assertEquals(type, messageTypeInConstructor.getType());
        assertXMLEqual(control, messageTypeInConstructor.toXML());

        controlBuilder = new StringBuilder();
        controlBuilder.append("<message")
                .append(" type=\"")
                .append(type2)
                .append("\">")
                .append("</message>");
        control = controlBuilder.toString();

        Message messageTypeSet = getNewMessage();
        messageTypeSet.setType(type2);
        assertEquals(type2, messageTypeSet.getType());
        assertXMLEqual(control, messageTypeSet.toXML());
    }

    @Test(expected=IllegalArgumentException.class)
    public void setMessageTypeNullTest() {
        Message message = getNewMessage();
        message.setType(null);
    }

    @Test(expected=NullPointerException.class)
    public void setNullMessageBodyTest() {
        Message message = getNewMessage();
        message.addBody(null, null);
    }

    @Test
    public void setMessageSubjectTest() throws IOException, SAXException, ParserConfigurationException {
        final String messageSubject = "This is a test of the emergency broadcast system.";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message>")
                .append("<subject>")
                .append(messageSubject)
                .append("</subject>")
                .append("</message>");
        String control = controlBuilder.toString();

        Message message = getNewMessage();
        message.setSubject(messageSubject);

        assertEquals(messageSubject, message.getSubject());
        assertXMLEqual(control, message.toXML());
    }

    @Test
    public void oneMessageBodyTest() throws IOException, SAXException, ParserConfigurationException {
        final String messageBody = "This is a test of the emergency broadcast system.";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message>")
                .append("<body>")
                .append(messageBody)
                .append("</body>")
                .append("</message>");
        String control = controlBuilder.toString();

        Message message = getNewMessage();
        message.setBody(messageBody);

        assertEquals(messageBody, message.getBody());
        assertXMLEqual(control, message.toXML());
    }

    @Ignore
    @Test
    public void multipleMessageBodiesTest() throws IOException, SAXException, ParserConfigurationException {
        final String messageBody1 = "This is a test of the emergency broadcast system, 1.";
        final String lang2 = "ru";
        final String messageBody2 = "This is a test of the emergency broadcast system, 2.";
        final String lang3 = "sp";
        final String messageBody3 = "This is a test of the emergency broadcast system, 3.";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message>")
                .append("<body>")
                .append(messageBody1)
                .append("</body>")
                .append("<body xml:lang=\"")
                .append(lang2)
                .append("\">")
                .append(messageBody2)
                .append("</body>")
                .append("<body xml:lang=\"")
                .append(lang3)
                .append("\">")
                .append(messageBody3)
                .append("</body>")
                .append("</message>");
        String control = controlBuilder.toString();

        Message message = getNewMessage();
        message.addBody(null, messageBody1);
        message.addBody(lang2, messageBody2);
        message.addBody(lang3, messageBody3);
        assertXMLEqual(control, message.toXML());

        Collection<String> languages = message.getBodyLanguages();
        List<String> controlLanguages = new ArrayList<String>();
        controlLanguages.add(lang2);
        controlLanguages.add(lang3);
        controlLanguages.removeAll(languages);
        assertTrue(controlLanguages.size() == 0);
    }

    @Test
    public void removeMessageBodyTest() {
        Message message = getNewMessage();
        message.setBody("test");
        assertTrue(message.getBodies().size() == 1);

        message.setBody(null);
        assertTrue(message.getBodies().size() == 0);

        assertFalse(message.removeBody("sp"));

        Message.Body body = message.addBody("es", "test");
        assertTrue(message.getBodies().size() == 1);
        
        message.removeBody(body);
        assertTrue(message.getBodies().size() == 0);
    }

    @Test
    public void setMessageThreadTest() throws IOException, SAXException, ParserConfigurationException {
        final String messageThread = "1234";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message>")
                .append("<thread>")
                .append(messageThread)
                .append("</thread>")
                .append("</message>");
        String control = controlBuilder.toString();

        Message message = getNewMessage();
        message.setThread(messageThread);

        assertEquals(messageThread, message.getThread());
        assertXMLEqual(control, message.toXML());
    }

    @Test
    public void messageXmlLangTest() throws IOException, SAXException, ParserConfigurationException {
        final String lang = "sp";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message")
                .append(" xml:lang=\"")
                .append(lang)
                .append("\">")
                .append("</message>");
        String control = controlBuilder.toString();

        Message message = getNewMessage();
        message.setLanguage(lang);

        assertXMLEqual(control, message.toXML());
    }

    @Test
    public void messageEqualityTest() {
        Message message = getNewMessage();
        assertTrue(message.equals(message));
        //noinspection ObjectEqualsNull
        assertFalse(message.equals(null));
        assertFalse(message.equals("test"));
        Message message2 = getNewMessage();

        assertTrue(message.equals(message2));

        message.setTo("joe@shmoe.com");
        assertFalse(message.equals(message2));
        message2.setTo("joe@shmoe.com");

        message.setSubject("subject");
        assertFalse(message.equals(message2));
        message2.setSubject("subject");

        message.setThread("thread");
        assertFalse(message.equals(message2));
        message2.setThread("thread");

        message.setBody("body1");
        assertFalse(message.equals(message2));
        message2.setBody("body1");

        message.setLanguage("language");
        assertFalse(message.equals(message2));
        message2.setLanguage("language");

        message.setType(Message.Type.chat);
        assertFalse(message.equals(message2));
        message2.setType(Message.Type.chat);

        assertTrue(message.equals(message2));
    }

    private static Message getNewMessage() {
        Message message = new Message();
        message.setPacketID(Packet.ID_NOT_AVAILABLE);
        return message;
    }
}
