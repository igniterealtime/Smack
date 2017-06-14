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
import static org.junit.Assert.fail;

import org.jivesoftware.smack.packet.StreamError.Condition;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class StreamErrorTest {

    @Test
    public void testParsingOfSimpleStreamError() {
        StreamError error = null;
        final String xml =
                // Usually the stream:stream element has more attributes (to, version, ...)
                // We omit those, since they are not relevant for testing
                "<stream:stream from='im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' xmlns:stream='http://etherx.jabber.org/streams'>" +
                "<stream:error>" +
                "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-streams' /> +" +
                "</stream:error>" +
                "</stream:stream>";
        try {
            XmlPullParser parser = PacketParserUtils.getParserFor(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
    }

    @Test
    public void testParsingOfStreamErrorWithText() {
        StreamError error = null;
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
        try {
            XmlPullParser parser = PacketParserUtils.getParserFor(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
        assertEquals("Replaced by new connection", error.getDescriptiveText());
    }

    @Test
    public void testParsingOfStreamErrorWithTextAndOptionalElement() {
        StreamError error = null;
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
        try {
            XmlPullParser parser = PacketParserUtils.getParserFor(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals(Condition.conflict, error.getCondition());
        assertEquals("Replaced by new connection", error.getDescriptiveText());
        ExtensionElement appSpecificElement = error.getExtension("appSpecificElement", "myns");
        assertNotNull(appSpecificElement);
    }

}
