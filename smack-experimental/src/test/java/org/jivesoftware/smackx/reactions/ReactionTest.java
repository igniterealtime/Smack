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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;
import org.jivesoftware.smackx.reactions.provider.ReactionsElementProvider;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jxmpp.stringprep.XmppStringprepException;
import org.mockito.Mockito;

/**
 * Tests related to managing reactions in Smack XMPP.
 * These tests cover the creation and manipulation of reaction elements,
 * including adding reactions to messages, XML serialization, and verifying
 * invalid restrictions.
 *
 * @author Ismael Nunes Campos
 * @since 2025
 */
public class ReactionTest extends SmackTestSuite {

    private XMPPConnection mockConnection;
    private ReactionsManager reactionsManager;
    private ReactionsListener mockListener1;
    private ReactionsListener mockListener2;

    @BeforeEach
    public void setUp() {

        mockConnection = Mockito.mock(XMPPConnection.class);

        reactionsManager = new ReactionsManager(mockConnection);

        mockListener1 = Mockito.mock(ReactionsListener.class);
        mockListener2 = Mockito.mock(ReactionsListener.class);
    }


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
    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    void deserializationTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<reactions xmlns='urn:xmpp:reactions:0' id='msg-id-123'>" +
                "<reaction>👍</reaction>" +
                "<reaction>❤️</reaction>" +
                "</reactions>";

        XmlPullParser parser = TestUtils.getParser(xml);

        ReactionsElementProvider provider = new ReactionsElementProvider();
        ReactionsElement reactionsElement = provider.parse(parser);

        assertEquals("msg-id-123", reactionsElement.getId());
        assertEquals(2, reactionsElement.getReactions().size());
        Set<Reaction> reactions = reactionsElement.getReactions();
        assertTrue(reactions.stream().anyMatch(r -> "👍".equals(r.getEmoji())));
        assertTrue(reactions.stream().anyMatch(r -> "❤️".equals(r.getEmoji())));
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

        Set<String> emojis = new LinkedHashSet<>(Arrays.asList("❤️", "\uD83D\uDE02"));

        MessageBuilder messageBuilder = ReactionsManager.createMessageWithReactions(emojis, messageId, null);
        Message message = messageBuilder
                        .ofType(Message.Type.chat)
                        .build();

        ReactionsElement reactionsElement = ReactionsElement.fromMessage(message);

        assertEquals(messageId, reactionsElement.getId());
        assertEquals(2, reactionsElement.getReactions().size());
        Set<Reaction> reactions = reactionsElement.getReactions();
        assertTrue(reactions.stream().anyMatch(r -> "❤️".equals(r.getEmoji())));
        assertTrue(reactions.stream().anyMatch(r -> "\uD83D\uDE02".equals(r.getEmoji())));

    }

    @Test
    public void testAddReactionsListener() {

        assertTrue(reactionsManager.addReactionsListener(mockListener1));
        verify(mockConnection, times(1)).addAsyncStanzaListener(any(), any());

        assertTrue(reactionsManager.addReactionsListener(mockListener2));
        verify(mockConnection, times(1)).addAsyncStanzaListener(any(), any());

    }

    @Test
    public void testRemoveReactionsListener() {

        reactionsManager.addReactionsListener(mockListener1);
        reactionsManager.addReactionsListener(mockListener2);

        assertTrue(reactionsManager.removeReactionsListener(mockListener1));

        verify(mockConnection, never()).removeAsyncStanzaListener(any());

        assertTrue(reactionsManager.removeReactionsListener(mockListener2));

        verify(mockConnection, times(1)).removeAsyncStanzaListener(any());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    void serializationTest() {

        String emoji = "❤️";
        Reaction reaction = new Reaction(emoji);
        String expectedXml = "<reaction>❤️</reaction>";
        assertEquals(expectedXml, reaction.toXML().toString(), "The generated XML does not match what was expected.");

        String reactions = "<reactions xmlns='urn:xmpp:reactions:0' id='744f6e18-a57a-11e9-a656-4889e7820c76'>"
                        + "<reaction>\uD83D\uDC4B</reaction>" +
                          "<reaction>\uD83D\uDC22</reaction>"
                        + "</reactions>";
        Set<Reaction> emojis = new LinkedHashSet<>();
        emojis.add(new Reaction("\uD83D\uDC4B"));
        emojis.add(new Reaction("\uD83D\uDC22"));
        ReactionsElement reactionsElement = new ReactionsElement(emojis, "744f6e18-a57a-11e9-a656-4889e7820c76");

        assertXmlSimilar(reactions, reactionsElement.toXML());

    }

    @Test
    void testCreateReactionRestrictionsForm() {

        int maxReactions = 1;
        Set<String> allowedEmojis = new LinkedHashSet<>();
        allowedEmojis.add("👍");
        allowedEmojis.add("❤️");
        allowedEmojis.add("😎");

        DataForm form = ReactionsManager.createReactionRestrictionsForm(maxReactions, allowedEmojis);

        assertEquals("urn:xmpp:reactions:0:restrictions", form.getFormType());

        assertEquals(String.valueOf(maxReactions), form.getField("max_reactions_per_user").getFirstValue());

        Set<String> actualAllowlist = new LinkedHashSet<>();
        form.getField("allowlist").getValues().forEach(value -> actualAllowlist.add((String) value));

        assertEquals(allowedEmojis, actualAllowlist);
    }
}
