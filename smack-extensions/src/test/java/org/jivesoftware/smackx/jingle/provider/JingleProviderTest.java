/**
 *
 * Copyright 2017-2022 Florian Schmaus
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
package org.jivesoftware.smackx.jingle.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class JingleProviderTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseUnknownJingleContentDescrption(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String unknownJingleContentDescriptionNamespace = "urn:xmpp:jingle:unknown-description:5";
        final String unknownJingleContentDescription =
                        // @formatter:off
                        "<description xmlns='" + unknownJingleContentDescriptionNamespace + "'>" +
                           "<file>" +
                             "<date>1969-07-21T02:56:15Z</date>" +
                             "<desc>This is a test. If this were a real file...</desc>" +
                             "<media-type>text/plain</media-type>" +
                             "<name>test.txt</name>" +
                             "<range/>" +
                             "<size>6144</size>" +
                             "<hash xmlns='urn:xmpp:hashes:2'" +
                                  " algo='sha-1'>w0mcJylzCn+AfvuGdqkty2+KP48=</hash>" +
                          "</file>" +
                        "</description>";
                        // @formatter:on
        CharSequence xml = createTestJingle(unknownJingleContentDescription);
        Jingle jingle = SmackTestUtil.parse(xml, JingleProvider.class, parserKind);

        JingleContentDescription jingleContentDescription = jingle.getSoleContentOrThrow().getDescription();

        String parsedUnknownJingleContentDescriptionNamespace = jingleContentDescription.getNamespace();
        assertEquals(unknownJingleContentDescriptionNamespace, parsedUnknownJingleContentDescriptionNamespace);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseUnknownJingleContentTransport(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String unknownJingleContentTransportNamespace = "urn:xmpp:jingle:unknown-transport:foo:1";
        final String unknownJingleContentTransport =
                        // @formatter:off
                        "<transport xmlns='" + unknownJingleContentTransportNamespace + "'" +
                                  " mode='tcp'" +
                                  " sid='vj3hs98y'>" +
                          "<candidate cid='hft54dqy'" +
                                   " host='192.168.4.1'" +
                                   " jid='romeo@montague.example/dr4hcr0st3lup4c'" +
                                   " port='5086'" +
                                   " priority='8257636'" +
                                   " type='direct'/>" +
                          "<candidate cid='hutr46fe'" +
                                   " host='24.24.24.1'" +
                                   " jid='romeo@montague.example/dr4hcr0st3lup4c'" +
                                   " port='5087'" +
                                   " priority='8258636'" +
                                   " type='direct'/>" +
                        "</transport>";
                        // @formatter:on
        CharSequence xml = createTestJingle(unknownJingleContentTransport);
        Jingle jingle = SmackTestUtil.parse(xml, JingleProvider.class, parserKind);

        JingleContentTransport jingleContentTransport = jingle.getSoleContentOrThrow().getTransport();

        String parsedUnknownJingleContentTransportNamespace = jingleContentTransport.getNamespace();
        assertEquals(unknownJingleContentTransportNamespace, parsedUnknownJingleContentTransportNamespace);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testReasonElementWithExtraElement(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<iq from='juliet@capulet.lit/balcony'"
                        +      " id='le71fa63'"
                        +      " to='romeo@montague.lit/orchard'"
                        +      " type='set'>"
                        +    "<jingle xmlns='urn:xmpp:jingle:1'"
                        +            " action='session-terminate'"
                        +            " sid='a73sjjvkla37jfea'>"
                        +      "<reason>"
                        +        "<success/>"
                        +        "<my-element xmlns='https://example.org' foo='bar'/>"
                        +      "</reason>"
                        +    "</jingle>"
                        + "</iq>";
        Jingle jingle = SmackTestUtil.parse(xml, JingleProvider.class, parserKind);
        JingleReason jingleReason = jingle.getReason();

        assertEquals(JingleReason.Reason.success, jingleReason.asEnum());

        XmlElement element = jingleReason.getElement();
        // TODO: Use JUnit 5.8's assertInstanceOf when possible
        // assertInstanceOf(StandardExtesionElement.class, extraElement);
        assertTrue(element instanceof StandardExtensionElement);
        StandardExtensionElement extraElement = (StandardExtensionElement) element;
        assertEquals("https://example.org", extraElement.getNamespace());
        assertEquals("bar", extraElement.getAttributes().get("foo"));
    }

    private static CharSequence createTestJingle(String... childs) throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(// @formatter:off
                        "<iq from='romeo@montague.example/dr4hcr0st3lup4c'" +
                             " id='nzu25s8'" +
                             " to='juliet@capulet.example/yn0cl4bnw0yr3vym'" +
                             " type='set'>" +
                          "<jingle xmlns='urn:xmpp:jingle:1' " +
                                "action='session-initiate' " +
                                "initiator='romeo@montague.example/dr4hcr0st3lup4c' " +
                                "sid='851ba2'>" +
                        "<content creator='initiator' name='a-file-offer' senders='initiator'>"
                   // @formatter:on
                  );
        for (String child : childs) {
            sb.append(child);
        }
        sb.append(// @formatter:off
                            "</content>" +
                          "</jingle>" +
                        "</iq>"
                  // @formatter:on
                 );

        return sb;
    }
}
