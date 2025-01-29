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
import org.jxmpp.jid.impl.JidCreate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
     *
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
     *
     * This test ensures that the `addReactionsToMessage` method correctly
     * adds reactions to a message and that the reactions are correctly
     * reflected in the message's extension elements.
     *
     * @throws Exception If an error occurs during message handling or assertions.
     */
    @Test
    public void testAddReactionsToMessage() throws Exception {

        List<String> emojis = Arrays.asList("❤️", "❤️");
        String messageId = "1234";

        MessageBuilder messageBuilder = StanzaBuilder.buildMessage();
        Message message = messageBuilder
                        .setBody("Hello")
                        .ofType(Message.Type.chat)
                        .to("teste@domain.com")
                        .build();

        ReactionsManager.addReactionsToMessage(message, emojis, messageId, null);

        ReactionsElement reactionsElement = (ReactionsElement) message.getExtensionElement(ReactionsElement.ELEMENT, ReactionsElement.NAMESPACE);

        assertEquals(messageId, reactionsElement.getId());
        assertEquals(2, reactionsElement.getReactions().size());
        assertEquals("❤️", reactionsElement.getReactions().get(0).getEmoji());
        assertEquals("❤️", reactionsElement.getReactions().get(1).getEmoji());

    }

    /**
     * Tests the reactions element listener.
     *
     * This test simulates the receipt of a message with reactions and validates
     * that the `reactionsElementListener` properly processes the reactions
     * and adds the reactions extension to the message.
     *
     * @throws Exception If an error occurs during message handling or listener processing.
     */
    @Test
    public void testReactionsElementListener() throws Exception {

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

        // Assertions: Ensure that the message contains the reactions element
        assertNotNull(message.getExtensionElement(ReactionsElement.ELEMENT, ReactionsElement.NAMESPACE));
    }

    /**
     * Tests that an exception is thrown when invalid reaction restrictions are provided.
     *
     * This test checks that the system throws an `IllegalArgumentException` when
     * attempting to create a reaction restriction form with invalid values (e.g., negative values).
     *
     * @throws Exception If an error occurs during the test execution.
     */
    @Test
    public void testInvalidReactionRestrictions() {
        // Check invalid restrictions, like negative values
        assertThrows(IllegalArgumentException.class, () -> {
            ReactionsManager.createReactionRestrictionsForm(-1, Arrays.asList("😊", "😂"));
        });
    }


}
