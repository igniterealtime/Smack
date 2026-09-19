/*
 *
 * Copyright 2018-2026 Florian Schmaus.
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
package org.jivesoftware.smack;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.Limits;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.Test;

public class AbstractXMPPConnectionTest extends SmackTestSuite {
    private final DummyConnection connection = new DummyConnection();

    @Test
    public void parseFeatures() throws XmlPullParserException, IOException, SmackParsingException {
        String featureStr = "<features xmlns='http://etherx.jabber.org/streams'>" +
                "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
                "  <mechanism>OAUTHBEARER</mechanism>" +
                "  <mechanism>SCRAM-SHA-1</mechanism>" +
                "  <mechanism>PLAIN</mechanism>" +
                "  <mechanism>SCRAM-SHA-1-PLUS</mechanism>" +
                "</mechanisms>" +
                "<sasl-channel-binding xmlns='urn:xmpp:sasl-cb:0'>" +
                "  <channel-binding type='tls-exporter' />" +
                "</sasl-channel-binding>" +
                "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>" +
                "  <required />" +
                "</bind>" +
                "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'>" +
                "  <optional />" +
                "</session>" +
                "<limits xmlns='urn:xmpp:stream-limits:0'>" +
                "  <max-bytes>262144</max-bytes>" +
                "  <idle-seconds>840</idle-seconds>" +
                "</limits>" +
                "<register xmlns='urn:xmpp:invite' />" +
                "<register xmlns='urn:xmpp:ibr-token:0' />" +
                "<register xmlns='http://jabber.org/features/iq-register' />" +
                "<ver xmlns='urn:xmpp:features:rosterver' />" +
                "<sub xmlns='urn:xmpp:features:pre-approval' />" +
                "<csi xmlns='urn:xmpp:csi:0' />" +
                "<c hash='sha-1' node='http://prosody.im'" +
                "  ver='uWfOTni5YjTtMRfYAjNKsj2GUXM=' xmlns='http://jabber.org/protocol/caps' />" +
                "</features>";
        InputStream input = new ByteArrayInputStream(featureStr.getBytes(StandardCharsets.UTF_8));
        XmlPullParser parser = PacketParserUtils.getParserFor(input);
        connection.parseFeatures(parser);

        assertTrue(connection.hasFeature("mechanisms", "urn:ietf:params:xml:ns:xmpp-sasl"));
        Mechanisms featMechanisms = connection.getFeature(Mechanisms.class);
        assertEquals(asList("OAUTHBEARER", "SCRAM-SHA-1", "PLAIN", "SCRAM-SHA-1-PLUS"), featMechanisms.getMechanisms());

        assertTrue(connection.hasFeature("bind", "urn:ietf:params:xml:ns:xmpp-bind"));
        Bind.Feature featBind = connection.getFeature(Bind.Feature.class);
        assertEquals(Bind.Feature.INSTANCE, featBind);

        assertTrue(connection.hasFeature("session", "urn:ietf:params:xml:ns:xmpp-session"));
        Session.Feature featSession = connection.getFeature(Session.Feature.class);
        assertTrue(featSession.isOptional());

        assertTrue(connection.hasFeature("limits", "urn:xmpp:stream-limits:0"));
        Limits featLimits = connection.getFeature(Limits.class);
        assertEquals(262144, featLimits.getMaxBytes());
        assertEquals(840, featLimits.getIdleSeconds());
    }
}
