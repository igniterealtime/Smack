/**
 *
 * Copyright (C) 2007 Jive Software, 2019 Florian Schmaus.
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
package org.jivesoftware.smack.provider;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.io.IOException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.packet.SaslNonza;
import org.jivesoftware.smack.sasl.packet.SaslNonza.SASLFailure;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SaslProviderTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void parseSASLFailureSimple(SmackTestUtil.XmlPullParserKind parserKind)
                    throws TransformerException, ParserConfigurationException, FactoryConfigurationError,
                    XmlPullParserException, IOException, SmackParsingException {
        // @formatter:off
        final String saslFailureString = XMLBuilder.create(SASLFailure.ELEMENT, SaslNonza.NAMESPACE)
                        .e(SASLError.account_disabled.toString())
                        .asString();
        // @formatter:on
        SASLFailure saslFailure = SmackTestUtil.parse(saslFailureString, SaslFailureProvider.class, parserKind);
        assertXmlSimilar(saslFailureString, saslFailure.toString());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void parseSASLFailureExtended(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException, TransformerException,
                    ParserConfigurationException, FactoryConfigurationError {
        // @formatter:off
        final String saslFailureString = XMLBuilder.create(SASLFailure.ELEMENT, SaslNonza.NAMESPACE)
                        .e(SASLError.account_disabled.toString())
                        .up()
                        .e("text").a("xml:lang", "en")
                            .t("Call 212-555-1212 for assistance.")
                        .up()
                        .e("text").a("xml:lang", "de")
                            .t("Bitte wenden sie sich an (04321) 123-4444")
                        .up()
                        .e("text")
                            .t("Wusel dusel")
                        .asString();
        // @formatter:on
        SASLFailure saslFailure = SmackTestUtil.parse(saslFailureString, SaslFailureProvider.class, parserKind);
        assertXmlSimilar(saslFailureString, saslFailure.toXML(StreamOpen.CLIENT_NAMESPACE));
    }

}
