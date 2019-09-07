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
import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.provider.PubkeyElementProvider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jxmpp.util.XmppDateTime;

public class PubkeyElementTest extends SmackTestSuite {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void providerTest(SmackTestUtil.XmlPullParserKind parserKind)
                    throws ParseException, XmlPullParserException, IOException, SmackParsingException {
        String base64EncodedOpenPgpPublicKey = "VGhpcyBpcyBqdXN0IGEgdGVzdA==";
        String pubkeyElement =
                "<pubkey xmlns='urn:xmpp:openpgp:0' date='2018-01-21T10:46:21.000+00:00'>" +
                "<data>" +
                base64EncodedOpenPgpPublicKey +
                "</data>" +
                "</pubkey>";

        Date date = XmppDateTime.parseXEP0082Date("2018-01-21T10:46:21.000+00:00");
        PubkeyElement element = new PubkeyElement(new PubkeyElement.PubkeyDataElement(base64EncodedOpenPgpPublicKey), date);

        assertXmlSimilar(pubkeyElement, element.toXML().toString());

        XmlPullParser parser = SmackTestUtil.getParserFor(pubkeyElement, parserKind);
        PubkeyElement parsed = PubkeyElementProvider.INSTANCE.parse(parser);

        assertEquals(element.getDate(), parsed.getDate());
        assertEquals(element.getDataElement().getB64Data(), parsed.getDataElement().getB64Data());
    }
}
