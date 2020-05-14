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
package org.jivesoftware.smackx.message_retraction.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.message_retraction.provider.RetractedElementProvider;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jxmpp.util.XmppDateTime;

public class RetractedElementTest {

    @Test
    public void serializationTest() throws ParseException {
        Date stamp = XmppDateTime.parseXEP0082Date("2019-09-20T23:08:25.000+00:00");
        OriginIdElement originId = new OriginIdElement("origin-id-1");
        RetractedElement retractedElement = new RetractedElement(stamp, originId);
        String expectedXml = "" +
                "<retracted stamp='2019-09-20T23:08:25.000+00:00' xmlns='urn:xmpp:message-retract:0'>\n" +
                "  <origin-id xmlns='urn:xmpp:sid:0' id='origin-id-1'/>\n" +
                "</retracted>";

        assertXmlSimilar(expectedXml, retractedElement.toXML());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void deserializationTest(SmackTestUtil.XmlPullParserKind parserKind)
            throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
                "<retracted stamp='2019-09-20T23:08:25.000+00:00' xmlns='urn:xmpp:message-retract:0'>\n" +
                "  <origin-id xmlns='urn:xmpp:sid:0' id='origin-id-1'/>\n" +
                "</retracted>";

        RetractedElement element = SmackTestUtil.parse(xml, RetractedElementProvider.class, parserKind);
        assertNotNull(element.getOriginId());
    }
}
