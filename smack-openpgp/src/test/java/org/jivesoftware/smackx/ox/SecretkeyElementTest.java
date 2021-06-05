/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.Charset;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.provider.SecretkeyElementProvider;

import org.junit.jupiter.api.Test;

public class SecretkeyElementTest extends SmackTestSuite {

    @Test
    public void providerTest() throws Exception {
        String expected =
                "<secretkey xmlns='urn:xmpp:openpgp:0'>" +
                "BASE64_OPENPGP_ENCRYPTED_SECRET_KEY" +
                "</secretkey>";
        byte[] key = "BASE64_OPENPGP_ENCRYPTED_SECRET_KEY".getBytes(Charset.forName("UTF-8"));

        SecretkeyElement element = new SecretkeyElement(key);

        assertXmlSimilar(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        SecretkeyElement parsed = SecretkeyElementProvider.TEST_INSTANCE.parse(parser);

        assertArrayEquals(element.getB64Data(), parsed.getB64Data());
    }
}
