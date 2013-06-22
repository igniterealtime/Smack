package org.jivesoftware.smack.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jivesoftware.smack.TestUtils;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ParsingExceptionTest {
    private final static ProviderManager PM = ProviderManager.getInstance();

    private final static String EXTENSION2 =
    "<extension2 xmlns='namespace'>" +
        "<bar node='testNode'>" +
            "<i id='testid1' >" +
            "</i>" +
        "</bar>" +
     "</extension2>";

    @Before
    public void init() {
        PM.addExtensionProvider(ThrowException.ELEMENT, ThrowException.NAMESPACE, new ThrowException());
    }

    @After
    public void tini() {
        PM.removeExtensionProvider(ThrowException.ELEMENT, ThrowException.NAMESPACE);
    }

    @Test
    public void consumeUnparsedInput() throws Exception {
        XmlPullParser parser = TestUtils.getMessageParser(
                "<message from='user@server.example' to='francisco@denmark.lit' id='foo'>" +
                    "<" + ThrowException.ELEMENT + " xmlns='" + ThrowException.NAMESPACE + "'>" +
                       "<nothingInHere>" +
                       "</nothingInHere>" +
                    "</" + ThrowException.ELEMENT + ">" +
                    EXTENSION2 +
                "</message>");
        int parserDepth = parser.getDepth();
        String content = null;
        try {
            PacketParserUtils.parseMessage(parser);
        } catch (Exception e) {
            content = PacketParserUtils.parseContentDepth(parser, parserDepth);
        }
        assertNotNull(content);
        assertEquals(content, "<nothingInHere></nothingInHere>" + "</" + ThrowException.ELEMENT + ">" + EXTENSION2);

    }

    static class ThrowException implements PacketExtensionProvider {
        public static final String ELEMENT = "exception";
        public static final String NAMESPACE = "http://smack.jivesoftware.org/exception";

        @Override
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            throw new XMPPException("Test Exception");
        }

    }
}
