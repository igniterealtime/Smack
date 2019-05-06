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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.provider.PubkeyElementProvider;

import org.junit.jupiter.api.Test;
import org.jxmpp.util.XmppDateTime;

public class PubkeyElementTest extends SmackTestSuite {

    @Test
    public void providerTest() throws Exception {
        String expected =
                "<pubkey xmlns='urn:xmpp:openpgp:0' date='2018-01-21T10:46:21.000+00:00'>" +
                "<data>" +
                "BASE64_OPENPGP_PUBLIC_KEY" +
                "</data>" +
                "</pubkey>";

        Date date = XmppDateTime.parseXEP0082Date("2018-01-21T10:46:21.000+00:00");
        byte[] key = "BASE64_OPENPGP_PUBLIC_KEY".getBytes(Charset.forName("UTF-8"));
        PubkeyElement element = new PubkeyElement(new PubkeyElement.PubkeyDataElement(key), date);

        assertXMLEqual(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        PubkeyElement parsed = PubkeyElementProvider.TEST_INSTANCE.parse(parser);

        assertEquals(element.getDate(), parsed.getDate());
        assertTrue(Arrays.equals(element.getDataElement().getB64Data(), parsed.getDataElement().getB64Data()));
    }
}
