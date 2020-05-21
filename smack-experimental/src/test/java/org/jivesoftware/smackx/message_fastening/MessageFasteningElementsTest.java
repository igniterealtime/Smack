/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.message_fastening;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaFactory;
import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.message_fastening.element.ExternalElement;
import org.jivesoftware.smackx.message_fastening.element.FasteningElement;
import org.jivesoftware.smackx.message_fastening.provider.FasteningElementProvider;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class MessageFasteningElementsTest {

    private final StanzaFactory stanzaFactory = new StanzaFactory(new StandardStanzaIdSource());

    /**
     * Test XML serialization of the {@link FasteningElement} using the example provided by
     * the XEP.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#wrapped-payloads">XEP-0422 §3.1 Wrapped Payloads</a>
     */
    @Test
    public void fasteningElementSerializationTest() {
        String xml =
                "<apply-to xmlns='urn:xmpp:fasten:0' id='origin-id-1'>" +
                "    <i-like-this xmlns='urn:example:like'/>" +
                "</apply-to>";

        FasteningElement applyTo = FasteningElement.builder()
                .setOriginId("origin-id-1")
                .addWrappedPayload(new StandardExtensionElement("i-like-this", "urn:example:like"))
                .build();

        assertXmlSimilar(xml, applyTo.toXML().toString());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void fasteningDeserializationTest(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        String xml =
                "<apply-to xmlns='urn:xmpp:fasten:0' id='origin-id-1'>" +
                "    <i-like-this xmlns='urn:example:like'/>" +
                "    <external name='custom' element-namespace='urn:example:custom'/>" +
                "    <external name='body'/>" +
                "</apply-to>";

        FasteningElement parsed = SmackTestUtil.parse(xml, FasteningElementProvider.class, parserKind);

        assertNotNull(parsed);
        assertEquals(new OriginIdElement("origin-id-1"), parsed.getReferencedStanzasOriginId());
        assertFalse(parsed.isRemovingElement());
        assertFalse(parsed.isShellElement());

        assertEquals(1, parsed.getWrappedPayloads().size());
        assertEquals("i-like-this", parsed.getWrappedPayloads().get(0).getElementName());
        assertEquals("urn:example:like", parsed.getWrappedPayloads().get(0).getNamespace());

        assertEquals(2, parsed.getExternalPayloads().size());
        ExternalElement custom = parsed.getExternalPayloads().get(0);
        assertEquals("custom", custom.getName());
        assertEquals("urn:example:custom", custom.getElementNamespace());
        ExternalElement body  = parsed.getExternalPayloads().get(1);
        assertEquals("body", body.getName());
        assertNull(body.getElementNamespace());
    }

    @Test
    public void fasteningDeserializationClearTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml =
                "<apply-to xmlns='urn:xmpp:fasten:0' id='origin-id-1' clear='true'>" +
                "    <i-like-this xmlns='urn:example:like'/>" +
                "</apply-to>";

        FasteningElement parsed = FasteningElementProvider.TEST_INSTANCE.parse(TestUtils.getParser(xml));

        assertTrue(parsed.isRemovingElement());
    }

    @Test
    public void fasteningElementWithExternalElementsTest() {
        String xml =
                "<apply-to xmlns='urn:xmpp:fasten:0' id='origin-id-2'>" +
                "    <external name='body'/>" +
                "    <external name='custom' element-namespace='urn:example:custom'/>" +
                "    <edit xmlns='urn:example.edit'/>" +
                "</apply-to>";

        FasteningElement element = FasteningElement.builder()
                .setOriginId("origin-id-2")
                .addExternalPayloads(Arrays.asList(
                        new ExternalElement("body"),
                        new ExternalElement("custom", "urn:example:custom")
                ))
                .addWrappedPayload(
                        new StandardExtensionElement("edit", "urn:example.edit"))
                .build();

        assertXmlSimilar(xml, element.toXML().toString());
    }

    @Test
    public void createShellElementSharesOriginIdTest() {
        OriginIdElement originIdElement = new OriginIdElement("sensitive-stanza-1");
        FasteningElement sensitiveFastening = FasteningElement.builder()
                .setOriginId(originIdElement)
                .build();

        FasteningElement shellElement = FasteningElement.createShellElementForSensitiveElement(sensitiveFastening);

        assertEquals(originIdElement, shellElement.getReferencedStanzasOriginId());
    }

    @Test
    public void fasteningRemoveSerializationTest() {
        String xml =
                "<apply-to xmlns='urn:xmpp:fasten:0' id='origin-id-1' clear='true'>" +
                        "      <i-like-this xmlns='urn:example:like'>Very much</i-like-this>" +
                        "</apply-to>";

        FasteningElement element = FasteningElement.builder()
                .setOriginId("origin-id-1")
                .setClear()
                .addWrappedPayload(StandardExtensionElement.builder("i-like-this", "urn:example:like")
                        .setText("Very much")
                        .build())
                .build();

        assertXmlSimilar(xml, element.toXML().toString());
    }

    @Test
    public void hasFasteningElementTest() {
        MessageBuilder messageBuilderWithFasteningElement = StanzaBuilder.buildMessage()
                .setBody("Hi!")
                .addExtension(FasteningElement.builder().setOriginId("origin-id-1").build());
        MessageBuilder messageBuilderWithoutFasteningElement = StanzaBuilder.buildMessage()
                .setBody("Ho!");

        assertTrue(FasteningElement.hasFasteningElement(messageBuilderWithFasteningElement));
        assertFalse(FasteningElement.hasFasteningElement(messageBuilderWithoutFasteningElement));
    }

    @Test
    public void shellElementMustNotHaveClearAttributeTest() {
        assertThrows(IllegalArgumentException.class, () ->
                FasteningElement.builder()
                        .setShell()
                        .setClear()
                        .build());
    }

    @Test
    public void shellElementMustNotContainAnyPayloads() {
        assertThrows(IllegalArgumentException.class, () ->
                FasteningElement.builder()
                        .setShell()
                        .addWrappedPayload(new StandardExtensionElement("edit", "urn:example.edit"))
                        .build());

        assertThrows(IllegalArgumentException.class, () ->
                FasteningElement.builder()
                        .setShell()
                        .addExternalPayload(new ExternalElement("body"))
                        .build());
    }

    @Test
    public void ensureAddFasteningElementToStanzaWorks() {
        MessageBuilder message = stanzaFactory.buildMessageStanza();
        FasteningElement fasteningElement = FasteningElement.builder().setOriginId("another-apply-to").build();

        // Adding only one element is allowed
        fasteningElement.applyTo(message);
    }

    /**
     * Ensure, that {@link FasteningElement#applyTo(MessageBuilder)}
     * throws when trying to add an {@link FasteningElement} to a {@link MessageBuilder} that already contains one
     * such element.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#rules">XEP-0422: §4. Business Rules</a>
     */
    @Test
    public void ensureStanzaCanOnlyContainOneFasteningElement() {
        MessageBuilder messageWithFastening = stanzaFactory.buildMessageStanza();
        FasteningElement.builder().setOriginId("origin-id").build().applyTo(messageWithFastening);

        // Adding a second fastening MUST result in exception
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                FasteningElement.builder().setOriginId("another-apply-to").build()
                        .applyTo(messageWithFastening));
    }
}
