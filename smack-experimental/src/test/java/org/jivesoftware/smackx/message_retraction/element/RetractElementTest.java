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

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.message_retraction.provider.RetractElementProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class RetractElementTest {

    @Test
    public void serializationTest() {
        RetractElement retractElement = new RetractElement();
        String expectedXml = "<retract xmlns='urn:xmpp:message-retract:0'/>";

        assertXmlSimilar(expectedXml, retractElement.toXML());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void deserializationTest(SmackTestUtil.XmlPullParserKind parserKind)
            throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<retract xmlns='urn:xmpp:message-retract:0'/>";
        RetractElement element = SmackTestUtil.parse(xml, RetractElementProvider.class, parserKind);

        assertNotNull(element);
    }
}
