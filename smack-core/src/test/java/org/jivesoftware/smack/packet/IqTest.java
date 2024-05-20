/**
 *
 * Copyright © 2023-2024 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class IqTest {

    @Test
    public void testIqErrorWithChildElement() {
        IQ request = new TestIQ();
        StanzaError error = StanzaError.getBuilder().setCondition(StanzaError.Condition.bad_request).build();
        ErrorIQ errorIq = IQ.createErrorResponse(request, error);

        String expected = "<iq xmlns='jabber:client' id='42' type='error'>"
                          + "<error type='modify'><bad-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>"
                          + "<test-iq xmlns='https://igniterealtime.org/projects/smack'/>"
                          + "</iq>";
        assertXmlSimilar(expected, errorIq.toXML());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testIqWithXmlns(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        final String iqXml = "<iq xmlns='jabber:client' type='result' to='username@tigase.mydomain.org/1423222896-tigase-59' id='3QLCH-1'>" +
                        "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>" +
                        "<jid>foo@tigase.mydomain.org/myresource</jid>" +
                        "</bind>" +
                        "</iq>";
        final String xml =
                        "<stream:stream xmlns='jabber:client' to='tigase.mydomain.org' xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='username@tigase.mydomain.org' xml:lang='en-US'>" +
                        iqXml +
                        "</stream:stream>";

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, "iq", parserKind);
        IQ iq = PacketParserUtils.parseIQ(parser);
        assertXmlSimilar(iqXml, iq.toXML());
    }
}
