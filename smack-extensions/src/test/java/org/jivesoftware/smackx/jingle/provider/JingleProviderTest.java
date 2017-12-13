/**
 *
 * Copyright 2017 Florian Schmaus
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class JingleProviderTest {

    @Test
    public void testParseUnknownJingleContentDescrption() throws Exception {
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
        XmlPullParser parser = createTestJingle(unknownJingleContentDescription);
        Jingle jingle = (Jingle) PacketParserUtils.parseIQ(parser);

        JingleContentDescription jingleContentDescription = jingle.getSoleContentOrThrow().getDescription();

        String parsedUnknownJingleContentDescriptionNamespace = jingleContentDescription.getNamespace();
        assertEquals(unknownJingleContentDescriptionNamespace, parsedUnknownJingleContentDescriptionNamespace);
    }

    @Test
    public void testParseUnknownJingleContentTransport() throws Exception {
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
        XmlPullParser parser = createTestJingle(unknownJingleContentTransport);
        Jingle jingle = (Jingle) PacketParserUtils.parseIQ(parser);

        JingleContentTransport jingleContentTransport = jingle.getSoleContentOrThrow().getTransport();

        String parsedUnknownJingleContentTransportNamespace = jingleContentTransport.getNamespace();
        assertEquals(unknownJingleContentTransportNamespace, parsedUnknownJingleContentTransportNamespace);
    }

    private static XmlPullParser createTestJingle(String... childs) throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(// @formatter:off
                        "<iq from='romeo@montague.example/dr4hcr0st3lup4c'" +
                             " id='nzu25s8'" +
                             " to='juliet@capulet.example/yn0cl4bnw0yr3vym'" +
                             " type='set'>" +
                          "<jingle xmlns='urn:xmpp:jingle:1' " +
                                " action='session-initiate' " +
                             " initiator='romeo@montague.example/dr4hcr0st3lup4c' " +
                                   " sid='851ba2'>" +
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

        String jingleStanza = sb.toString();

        XmlPullParser parser = PacketParserUtils.getParserFor(jingleStanza);
        return parser;
    }
}
