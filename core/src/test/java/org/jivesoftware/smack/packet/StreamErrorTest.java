package org.jivesoftware.smack.packet;

import static org.junit.Assert.*;

import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.test.util.TestUtils;
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
            XmlPullParser parser = TestUtils.getParser(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals("conflict", error.getCode());
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
            XmlPullParser parser = TestUtils.getParser(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals("conflict", error.getCode());
        assertEquals("Replaced by new connection", error.getText());
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
                "<appSpecificElement>" +
                    "Text contents of application-specific condition element: Foo Bar" +
                "</appSpecificElement>" +
                "</stream:error>" +
                "</stream:stream>";
        try {
            XmlPullParser parser = TestUtils.getParser(xml, "error");
            error = PacketParserUtils.parseStreamError(parser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(error);
        assertEquals("conflict", error.getCode());
        assertEquals("Replaced by new connection", error.getText());
        // As of now, Smack ignores application-specific condition elements, so we don't
        // test them.
    }

}
