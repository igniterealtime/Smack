/**
 *
 * Copyright 2017-2019 Florian Schmaus
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
package org.jivesoftware.smackx.eme.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ExplicitMessageEncryptionProviderTest {

    private static final String OX_EME_ELEMENT = "<encryption xmlns='urn:xmpp:eme:0' namespace='urn:xmpp:openpgp:0'/>";

    private static final String UNKNOWN_NAMESPACE = "urn:xmpp:foobar:0";
    private static final String UNKNOWN_NAME = "Foo Bar";
    private static final String UNKNOWN_EME_ELEMENT = "<encryption xmlns='urn:xmpp:eme:0' namespace='" + UNKNOWN_NAMESPACE
                    + "' name='" + UNKNOWN_NAME + "'/>";

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseOxEmeElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        ExplicitMessageEncryptionElement eme = SmackTestUtil.parse(OX_EME_ELEMENT, ExplicitMessageEncryptionProvider.class, parserKind);
        assertEquals(ExplicitMessageEncryptionProtocol.openpgpV0, eme.getProtocol());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseUnknownEmeElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        ExplicitMessageEncryptionElement eme = SmackTestUtil.parse(UNKNOWN_EME_ELEMENT, ExplicitMessageEncryptionProvider.class, parserKind);
        assertEquals(UNKNOWN_NAMESPACE, eme.getEncryptionNamespace());
        assertEquals(UNKNOWN_NAME, eme.getName());
    }
}
