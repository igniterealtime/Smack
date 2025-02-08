/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;
import org.jivesoftware.smackx.reactions.provider.ReactionsElementProvider;

import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Tests related to managing reactions in Smack XMPP.
 * These tests cover the creation and manipulation of reaction elements,
 * including adding reactions to messages, XML parsing, and verifying
 * invalid restrictions.
 *
 * @author Ismael Nunes Campos
 * @since 2025
 */
public class ReactionTest extends SmackTestSuite {

    /**
     * Tests parsing of a reactions XML and validates the extracted data.
     * <p>
     * This test validates the creation of a `ReactionsElement` from an XML
     * and checks if the message ID and emoji reactions are correctly extracted.
     *
     * @throws XmlPullParserException If an error occurs during XML parsing.
     * @throws IOException If an I/O error occurs during parsing.
     * @throws SmackParsingException If a failure occurs while parsing the XML.
     */
    @Test
    void testReaction() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<reactions xmlns='urn:xmpp:reactions:0' id='msg-id-123'>" +
                "<reaction>👍</reaction>" +
                "<reaction>❤️</reaction>" +
                "</reactions>";

        XmlPullParser parser = TestUtils.getParser(xml);

        ReactionsElementProvider provider = new ReactionsElementProvider();
        ReactionsElement reactionsElement = provider.parse(parser);

        assertEquals("msg-id-123", reactionsElement.getId());
        assertEquals(2, reactionsElement.getReactions().size());
        assertEquals("👍", reactionsElement.getReactions().get(0).getEmoji());
        assertEquals("❤️", reactionsElement.getReactions().get(1).getEmoji());
    }

    /**
     * Tests adding reactions to a message.
     * <p>
     * This test ensures that the `addReactionsToMessage` method correctly
     * adds reactions to a message and that the reactions are correctly
     * reflected in the message's extension elements.
     *
     */
    @Test
    public void testAddReactionsToMessage() throws XmppStringprepException {
        String messageId = "1234";

        List<String> emojis = Arrays.asList("❤️", "\uD83D\uDE02");

        XMPPConnection connection = mock(XMPPConnection.class);
        ReactionsManager reactionsManager = new ReactionsManager(connection);

        MessageBuilder messageBuilder = reactionsManager.createMessageWithReactions(emojis, messageId, null);
        Message message = messageBuilder
                        .ofType(Message.Type.chat)
                        .build();

        ReactionsElement reactionsElement = ReactionsElement.fromMessage(message);

        assertEquals(messageId, reactionsElement.getId());
        assertEquals(2, reactionsElement.getReactions().size());
        assertEquals("❤️", reactionsElement.getReactions().get(0).getEmoji());
        assertEquals("\uD83D\uDE02", reactionsElement.getReactions().get(1).getEmoji());

    }

    /**
     * Tests the reactions element listener.
     * <p>
     * This test simulates the receipt of a message with reactions and validates
     * that the `reactionsElementListener` properly processes the reactions
     * and adds the reactions extension to the message.
     *
     */
    @Test
    public void testReactionsElementListener() {

        // Define reactions
        List<Reaction> reactions = Arrays.asList(new Reaction("😊"), new Reaction("😂"));
        String messageId = "1234";

        // Simulate a message with reactions
        Message message = StanzaBuilder.buildMessage().build();
        ReactionsElement reactionsElement = new ReactionsElement(reactions, messageId);
        message.addExtension(reactionsElement);

        // Act: Call the listener
        XMPPConnection connection = mock(XMPPConnection.class); // Mock of XMPP connection
        ReactionsManager reactionsManager = new ReactionsManager(connection);
        reactionsManager.reactionsElementListener(message);

        ReactionsElement reactionsFromMessage = ReactionsElement.fromMessage(message);
        // Assertions: Ensure that the message contains the reactions element
        assertNotNull(reactionsFromMessage);

    }

    @Test
    void testXMLParsing() {

        String emoji = "❤️";
        Reaction reaction = new Reaction(emoji);
        String expectedXml = "<reaction xmlns='jabber:client'>❤️</reaction>";
        assertEquals(expectedXml, reaction.toXML().toString(), "O XML gerado não corresponde ao esperado.");

        String reactions = "<reactions xmlns='urn:xmpp:reactions:0' id='744f6e18-a57a-11e9-a656-4889e7820c76'>"
                        + "<reaction xmlns='jabber:client'>\uD83D\uDC4B</reaction>" +
                          "<reaction xmlns='jabber:client'>\uD83D\uDC22</reaction>"
                        + "</reactions>";
        List<Reaction> emojis = new ArrayList<>();
        emojis.add(new Reaction("\uD83D\uDC4B"));
        emojis.add(new Reaction("\uD83D\uDC22"));
        ReactionsElement reactionsElement = new ReactionsElement(emojis, "744f6e18-a57a-11e9-a656-4889e7820c76");

        assertXmlSimilar(reactions, reactionsElement.toXML());

    }


}
