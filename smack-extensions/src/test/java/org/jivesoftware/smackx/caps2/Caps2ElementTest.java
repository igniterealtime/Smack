/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.caps2;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.SmackTestUtil.XmlPullParserKind;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.caps2.element.Caps2Element.Caps2HashElement;
import org.jivesoftware.smackx.caps2.provider.Caps2Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Caps2ElementTest {

    private static final String CAPS2ELEMENT_EXPECTED = "<c xmlns=\"urn:xmpp:caps\">" +
            "<hash xmlns=\"urn:xmpp:hashes:2\" algo=\"sha-256\">kzBZbkqJ3ADrj7v08reD1qcWUwNGHaidNUgD7nHpiw8=</hash>" +
            "<hash xmlns=\"urn:xmpp:hashes:2\" algo=\"sha3-256\">79mdYAfU9rEdTOcWDO7UEAt6E56SUzk/g6TnqUeuD9Q=</hash>" +
            "</c>";

    @Test
    public void toXmlTest() {
        Caps2HashElement sha_256_hashElement = new Caps2HashElement("sha-256", "kzBZbkqJ3ADrj7v08reD1qcWUwNGHaidNUgD7nHpiw8=");

        Caps2HashElement sha3_256_hashElement = new Caps2HashElement("sha3-256", "79mdYAfU9rEdTOcWDO7UEAt6E56SUzk/g6TnqUeuD9Q=");

        Set<Caps2HashElement> set = new HashSet<Caps2HashElement>();
        set.add(sha_256_hashElement);
        set.add(sha3_256_hashElement);

        Caps2Element element = new Caps2Element(set);

        assertXmlSimilar(CAPS2ELEMENT_EXPECTED, element.toXML());
    }

    @ParameterizedTest
    @EnumSource(value = SmackTestUtil.XmlPullParserKind.class)
    public void caps2ProviderTest(XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        XmlPullParser parser = SmackTestUtil.getParserFor(CAPS2ELEMENT_EXPECTED, parserKind);
        Caps2Element element = Caps2Provider.INSTANCE.parse(parser);
        assertXmlSimilar(CAPS2ELEMENT_EXPECTED, element.toXML());
    }
}
