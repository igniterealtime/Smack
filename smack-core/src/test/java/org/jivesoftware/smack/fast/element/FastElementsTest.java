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
package org.jivesoftware.smack.fast.element;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;

import org.jivesoftware.smack.fast.provider.FastProvider;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FastElementsTest {

    @Test
    public void testFastFeatureSerialization() throws Exception {
        FastElements.Fast fast = new FastElements.Fast(
            Arrays.asList("HT-SHA-256-ENDP", "HT-SHA-512-ENDP", "HT-SHA-256-NONE"),
            true
        );

        String xml = fast.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar(
            "<fast xmlns='urn:xmpp:fast:0' tls-0rtt='true'>" +
            "<mechanism>HT-SHA-256-ENDP</mechanism>" +
            "<mechanism>HT-SHA-512-ENDP</mechanism>" +
            "<mechanism>HT-SHA-256-NONE</mechanism>" +
            "</fast>",
            xml
        );
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testFastFeatureParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<fast xmlns='urn:xmpp:fast:0' tls-0rtt='true'>" +
                     "<mechanism>HT-SHA-256-ENDP</mechanism>" +
                     "<mechanism>HT-SHA-256-NONE</mechanism>" +
                     "</fast>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        FastElements.Fast fast = FastProvider.FastElementProvider.INSTANCE.parse(parser, parser.getDepth(), null, null);

        assertNotNull(fast);
        assertTrue(fast.isTls0rtt());
        assertEquals(Arrays.asList("HT-SHA-256-ENDP", "HT-SHA-256-NONE"), fast.getMechanisms());
        assertNull(fast.getCount());
        assertNull(fast.isInvalidate());
    }

    @Test
    public void testFastAuthenticateElementSerialization() throws Exception {
        FastElements.Fast fast = new FastElements.Fast(123L, true);

        String xml = fast.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<fast xmlns='urn:xmpp:fast:0' count='123' invalidate='true'/>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testFastAuthenticateElementParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<fast xmlns='urn:xmpp:fast:0' count='42' invalidate='false'/>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        FastElements.Fast fast = FastProvider.FastElementProvider.INSTANCE.parse(parser, parser.getDepth(), null, null);

        assertNotNull(fast);
        assertEquals(Long.valueOf(42), fast.getCount());
        assertFalse(Boolean.TRUE.equals(fast.isInvalidate()));
        assertTrue(fast.getMechanisms().isEmpty());
    }

    @Test
    public void testRequestTokenSerializationAndParsing() throws Exception {
        FastElements.RequestToken requestToken = new FastElements.RequestToken("HT-SHA-256-ENDP");

        String xml = requestToken.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<request-token xmlns='urn:xmpp:fast:0' mechanism='HT-SHA-256-ENDP'/>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testRequestTokenParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<request-token xmlns='urn:xmpp:fast:0' mechanism='HT-SHA-256-ENDP'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        FastElements.RequestToken parsed = FastProvider.RequestTokenProvider.INSTANCE.parse(parser, parser.getDepth(), null, null);

        assertNotNull(parsed);
        assertEquals("HT-SHA-256-ENDP", parsed.getMechanism());
    }

    @Test
    public void testTokenSerialization() throws Exception {
        Instant expiry = Instant.parse("2026-12-31T23:59:59Z");
        FastElements.Token token = new FastElements.Token("secret-token-12345", expiry);

        String xml = token.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<token xmlns='urn:xmpp:fast:0' expiry='2026-12-31T23:59:59Z' token='secret-token-12345'/>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testTokenParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String xml = "<token xmlns='urn:xmpp:fast:0' expiry='2026-12-31T23:59:59Z' token='secret-token-12345'/>";
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        FastElements.Token parsed = FastProvider.TokenProvider.INSTANCE.parse(parser, parser.getDepth(), null, null);

        assertNotNull(parsed);
        assertEquals("secret-token-12345", parsed.getToken());
        assertEquals(Instant.parse("2026-12-31T23:59:59Z"), parsed.getExpiry());
    }
}
