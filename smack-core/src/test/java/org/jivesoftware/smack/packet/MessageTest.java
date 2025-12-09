/*
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
package org.jivesoftware.smack.packet;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xml.sax.SAXException;

public class MessageTest {

    @Test
    public void setMessageTypeTest() throws IOException, SAXException {
        Message.Type type = Message.Type.chat;
        Message.Type type2 = Message.Type.headline;

        // @formatter:off
        String control = "<message" +
                " type=\"" +
                type +
                "\">" +
                "</message>";
        // @formatter:on

        Message messageBuildWithBuilder = StanzaBuilder.buildMessage()
                        .ofType(Message.Type.chat)
                        .build();

        assertEquals(type, messageBuildWithBuilder.getType());
        assertXmlSimilar(control, messageBuildWithBuilder.toXML(StreamOpen.CLIENT_NAMESPACE).toString());

        // @formatter:off
        control = "<message" +
                " type=\"" +
                type2 +
                "\">" +
                "</message>";
        // @formatter:on

        Message messageTypeSet = StanzaBuilder.buildMessage()
                        .ofType(type2)
                        .build();
        assertEquals(type2, messageTypeSet.getType());
        assertXmlSimilar(control, messageTypeSet.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test(expected = NullPointerException.class)
    public void setNullMessageBodyTest() {
        StanzaBuilder.buildMessage()
                        .addBody(null, null)
                        .build();
    }

    @Test
    public void setMessageSubjectTest() throws IOException, SAXException {
        final String messageSubject = "This is a test of the emergency broadcast system.";

        // @formatter:off
        String control = "<message>" +
                "<subject>" +
                messageSubject +
                "</subject>" +
                "</message>";
        // @formatter:on

        Message message = StanzaBuilder.buildMessage()
                        .setSubject(messageSubject)
                        .build();
        assertEquals(messageSubject, message.getSubject());
        assertXmlSimilar(control, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void oneMessageBodyTest() throws IOException, SAXException {
        final String messageBody = "This is a test of the emergency broadcast system.";

        // @formatter:off
        String control = "<message>" +
                "<body>" +
                messageBody +
                "</body>" +
                "</message>";
        // @formatter:on

        Message message = StanzaBuilder.buildMessage()
                        .setBody(messageBody)
                        .build();
        assertEquals(messageBody, message.getBody());
        assertXmlSimilar(control, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void multipleMessageBodiesTest() throws IOException, SAXException {
        final String messageBody1 = "This is a test of the emergency broadcast system, 1.";
        final String lang2 = "ru";
        final String messageBody2 = "This is a test of the emergency broadcast system, 2.";
        final String lang3 = "sp";
        final String messageBody3 = "This is a test of the emergency broadcast system, 3.";

        // @formatter:off
        String control = "<message>" +
                "<body>" +
                messageBody1 +
                "</body>" +
                "<body xml:lang=\"" +
                lang2 +
                "\">" +
                messageBody2 +
                "</body>" +
                "<body xml:lang=\"" +
                lang3 +
                "\">" +
                messageBody3 +
                "</body>" +
                "</message>";
        // @formatter:on

        Message message = StanzaBuilder.buildMessage()
                        .addBody(null, messageBody1)
                        .addBody(lang2, messageBody2)
                        .addBody(lang3, messageBody3)
                        .build();
        assertXmlSimilar(control, message.toXML(StreamOpen.CLIENT_NAMESPACE));

        Collection<String> languages = message.getBodyLanguages();
        List<String> controlLanguages = new ArrayList<>();
        controlLanguages.add(lang2);
        controlLanguages.add(lang3);
        controlLanguages.removeAll(languages);
        assertTrue(controlLanguages.isEmpty());
    }

    @Test
    public void simpleMessageBodyTest() {
        Message message = StanzaBuilder.buildMessage()
                        .setBody("test")
                        .build();
        assertEquals(1, message.getBodies().size());

        message = StanzaBuilder.buildMessage().build();
        assertEquals(0, message.getBodies().size());


        message = StanzaBuilder.buildMessage()
                        .addBody("es", "test")
                        .build();
        assertEquals(1, message.getBodies().size());
    }

    @Test
    public void setMessageThreadTest() throws IOException, SAXException {
        final String messageThread = "1234";

        // @formatter:off
        String control = "<message>" +
                "<thread>" +
                messageThread +
                "</thread>" +
                "</message>";
        // @formatter:on

        Message message = StanzaBuilder.buildMessage()
                        .setThread(messageThread)
                        .build();

        assertEquals(messageThread, message.getThread());
        assertXmlSimilar(control, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void messageXmlLangTest() throws IOException, SAXException {
        final String lang = "sp";

        // @formatter:off
        String control = "<message" +
                " xml:lang=\"" +
                lang +
                "\">" +
                "</message>";
        // @formatter:on

        Message message = StanzaBuilder.buildMessage()
                        .setLanguage(lang)
                        .build();

        assertXmlSimilar(control, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    /**
     * Tests that only required characters are XML escaped in body.
     *
     * @see <a href="https://issues.igniterealtime.org/browse/SMACK-892">SMACK-892</a>
     */
    @Test
    public void escapeInBodyTest() {
        String theFive = "\"'<>&";
        Message.Body body = new Message.Body(null, theFive);

        assertEquals("<body xmlns='jabber:client'>\"'&lt;>&amp;</body>", body.toXML().toString());
    }

    @Test
    public void getBodyReturnsNoBodyTest() {
        var message = StanzaBuilder.buildMessage()
            .addBody("de", "Hallo, wie geht es dir?")
            .addBody("en", "Hello, how are you?")
            .build();
        assertNull(message.getBody());

        var deBody = message.getBody("de");
        assertEquals("Hallo, wie geht es dir?", deBody);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void getBodyReturnsBodyViaStreamLangTest(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        // @formatter:off
        String streamWithMessage =
                        "<stream:stream xmlns:stream='http://etherx.jabber.org/streams' xml:lang='en' from='foo.im' id='my-stream-id' version='1.0' xmlns='jabber:client'>"
                        + "<message to='recipient@foo.im' from='sender@foo.im/res'>"
                        + "<body xml:lang='de'>Hallo, wie geht es dir?</body>"
                        + "<body xml:lang='en'>Hello, how are you?</body>"
                        + "</message>"
                        + "</stream:stream>"
                        ;
        // @formatter:on
        var stanzas = SmackTestUtil.parseStanzas(streamWithMessage, parserKind);
        assertEquals(1, stanzas.size());

        var message = assertInstanceOf(Message.class, stanzas.get(0));
        var body = message.getBody();
        assertNotNull(body);
        assertEquals("Hello, how are you?", body);
    }

    /*
     * Same as getBodyReturnsBodyViaStreamLangTest() but without xml:lang on <stream/>.
     */
    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void getBodyReturnsBody(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        // @formatter:off
        String streamWithMessage =
                        "<stream:stream xmlns:stream='http://etherx.jabber.org/streams' from='foo.im' id='my-stream-id' version='1.0' xmlns='jabber:client'>"
                        + "<message to='recipient@foo.im' from='sender@foo.im/res'>"
                        + "<body xml:lang='de'>Hallo, wie geht es dir?</body>"
                        + "<body xml:lang='en'>Hello, how are you?</body>"
                        + "</message>"
                        + "</stream:stream>"
                        ;
        // @formatter:on
        var stanzas = SmackTestUtil.parseStanzas(streamWithMessage, parserKind);
        assertEquals(1, stanzas.size());

        var message = assertInstanceOf(Message.class, stanzas.get(0));
        // In this case, no xml:lang on the stream or the message and no explicit language argument provided to
        // getBody(), it is ambiguous which body we should return.
        var body = message.getBody();
        assertNull(body);
    }
}
