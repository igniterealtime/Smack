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
package org.jivesoftware.smack.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ParsingExceptionTest {

    private final static String EXTENSION2 =
    "<extension2 xmlns='namespace'>" +
        "<bar node='testNode'>" +
            "<i id='testid1' >" +
            "</i>" +
        "</bar>" +
     "</extension2>";

    @Before
    public void init() {
        ProviderManager.addExtensionProvider(ThrowException.ELEMENT, ThrowException.NAMESPACE, new ThrowException());
    }

    @After
    public void tini() {
        ProviderManager.removeExtensionProvider(ThrowException.ELEMENT, ThrowException.NAMESPACE);
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
            throw new SmackException("Test Exception");
        }

    }
}
