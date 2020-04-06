/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Test the JingleUtil class.
 */
public class JingleUtilTest extends SmackTestSuite {

    private XMPPConnection connection;
    private JingleUtil jutil;

    @BeforeEach
    public void setup() {
        connection = new DummyConnection(
                DummyConnection.getDummyConfigurationBuilder()
                .setUsernameAndPassword("romeo@montague.lit",
                "iluvJulibabe13").build());
        jutil = new JingleUtil(connection);
    }

    @SuppressWarnings("UnusedVariable")
    @Test
    public void sessionInitiateTest() throws XmppStringprepException {
        FullJid romeo = connection.getUser().asFullJidOrThrow();
        FullJid juliet = JidCreate.fullFrom("juliet@capulet.example/yn0cl4bnw0yr3vym");

        String sid = "851ba2";
        String contentName = "a-file-offer";
        Jingle jingle = jutil.createSessionInitiate(juliet, sid,
                JingleContent.Creator.initiator, contentName, JingleContent.Senders.initiator, null, null);



        String expected =
                "<iq from='" + romeo.toString() + "' " +
                    "id='nzu25s8' " +
                    "to='juliet@capulet.example/yn0cl4bnw0yr3vym' " +
                    "type='set'>" +
                        "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-initiate' " +
                        "initiator='romeo@montague.example/dr4hcr0st3lup4c' " +
                        "sid='851ba2'>" +
                            "<content creator='initiator' name='a-file-offer' senders='initiator'>" +
                                "<description xmlns='urn:xmpp:jingle:apps:file-transfer:5'>" +
                                    "<file>" +
                                        "<date>1969-07-21T02:56:15Z</date>" +
                                        "<desc>This is a test. If this were a real file...</desc>" +
                                        "<media-type>text/plain</media-type>" +
                                        "<name>test.txt</name>" +
                                        "<range/>" +
                                        "<size>6144</size>" +
                                        "<hash xmlns='urn:xmpp:hashes:2' " +
                                            "algo='sha-1'>w0mcJylzCn+AfvuGdqkty2+KP48=</hash>" +
                                    "</file>" +
                                "</description>" +
                            "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' " +
                            "mode='tcp' " +
                            "sid='vj3hs98y'> " +
                            "<candidate cid='hft54dqy' " +
                            "host='192.168.4.1' " +
                            "jid='romeo@montague.example/dr4hcr0st3lup4c' " +
                            "port='5086' " +
                            "priority='8257636' " +
                            "type='direct'/>" +
                                "<candidate cid='hutr46fe' " +
                                "host='24.24.24.1' " +
                                "jid='romeo@montague.example/dr4hcr0st3lup4c' " +
                                "port='5087' " +
                                "priority='8258636' " +
                                "type='direct'/>" +
                            "</transport>" +
                        "</content>" +
                    "</jingle>" +
                "</iq>";
        // TODO: Finish test
    }
}
