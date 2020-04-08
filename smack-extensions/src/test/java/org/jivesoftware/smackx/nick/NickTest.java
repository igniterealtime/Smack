/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.nick;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.nick.packet.Nick;
import org.jivesoftware.smackx.nick.provider.NickProvider;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class NickTest {
    /**
     * see <a href="https://xmpp.org/extensions/xep-0172.html#example-3">XEP-0172: User Nickname - Example 3</a>
     */
    private static final String XML = "<nick xmlns='http://jabber.org/protocol/nick'>Ishmael</nick>";

    @Test
    public void disallowEmptyNickTest() {
        assertThrows("Empty String as argument MUST cause IllegalArgumentException.",
                IllegalArgumentException.class, () -> new Nick(""));
    }

    @Test
    public void disallowNullNickTest() {
        assertThrows("Null argument MUST cause IllegalArgumentException.",
                IllegalArgumentException.class, () -> new Nick(null));
    }


    @Test
    public void serializationTest() {
        Nick nick = new Nick("Ishmael");

        assertXmlSimilar(XML, nick.toXML());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void deserializationTest(SmackTestUtil.XmlPullParserKind parserKind)
            throws XmlPullParserException, IOException, SmackParsingException {
        Nick nick = SmackTestUtil.parse(XML, NickProvider.class, parserKind);

        assertNotNull(nick);
        assertEquals("Ishmael", nick.getName());
    }

    @Test
    public void nicksAreEscapedTest() {
        String name = "</nick>\"'&";

        Nick nick = new Nick(name);

        assertXmlSimilar("<nick xmlns='http://jabber.org/protocol/nick'>" +
                "&lt;/nick&gt;&quot;&apos;&amp;" +
                "</nick>", nick.toXML());
        assertEquals(name, nick.getName());
    }
}
