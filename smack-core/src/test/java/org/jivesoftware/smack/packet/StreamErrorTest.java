/**
 *
 * Copyright the original author or authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.jivesoftware.smack.packet.StreamError.Condition;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class StreamErrorTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParsingOfSimpleStreamError(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                // Usually the stream:stream element has more attributes (to, version, ...)
                // We omit those, since they are not relevant for testing
                "<stream:stream from='im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' xmlns:stream='http://etherx.jabber.org/streams'>" +
                "<stream:error>" +
                "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-streams' /> +" +
                "</stream:error>" +
                "</stream:stream>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, "error", parserKind);
        StreamError error = PacketParserUtils.parseStreamError(parser);

        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParsingOfStreamErrorWithText(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                // Usually the stream:stream element has more attributes (to, version, ...)
                // We omit those, since they are not relevant for testing
                "<stream:stream from='im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' xmlns:stream='http://etherx.jabber.org/streams'>" +
                "<stream:error>" +
                "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-streams' />" +
                "<text xml:lang='' xmlns='urn:ietf:params:xml:ns:xmpp-streams'>" +
                    "Replaced by new connection" +
                "</text>" +
                "</stream:error>" +
                "</stream:stream>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, "error", parserKind);
        StreamError error = PacketParserUtils.parseStreamError(parser);

        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
        assertEquals("Replaced by new connection", error.getDescriptiveText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParsingOfStreamErrorWithTextAndOptionalElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                // Usually the stream:stream element has more attributes (to, version, ...)
                // We omit those, since they are not relevant for testing
                "<stream:stream from='im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' xmlns:stream='http://etherx.jabber.org/streams'>" +
                "<stream:error>" +
                "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-streams' />" +
                "<text xml:lang='' xmlns='urn:ietf:params:xml:ns:xmpp-streams'>" +
                    "Replaced by new connection" +
                "</text>" +
                "<appSpecificElement xmlns='myns'>" +
                    "Text contents of application-specific condition element: Foo Bar" +
                "</appSpecificElement>" +
                "</stream:error>" +
                "</stream:stream>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, "error", parserKind);
        StreamError error = PacketParserUtils.parseStreamError(parser);

        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
        assertEquals("Replaced by new connection", error.getDescriptiveText());
        ExtensionElement appSpecificElement = error.getExtension("appSpecificElement", "myns");
        assertNotNull(appSpecificElement);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testStreamErrorXmlNotWellFormed(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                // Usually the stream:stream element has more attributes (to, version, ...)
                // We omit those, since they are not relevant for testing
                "<stream:stream from='im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' xmlns:stream='http://etherx.jabber.org/streams'>" +
                        "<stream:error><xml-not-well-formed xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error>" +
                        "</stream:stream>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, "error", parserKind);
        StreamError error = PacketParserUtils.parseStreamError(parser);

        assertNotNull(error);
        assertEquals(Condition.not_well_formed, error.getCondition());
    }
}
