/**
 *
 * Copyright 2012-2019 Florian Schmaus
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
package org.jivesoftware.smackx.ping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.jivesoftware.smack.test.util.CharSequenceEquals.equalsCharSequence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.jxmpp.jid.JidTestUtil.DUMMY_AT_EXAMPLE_ORG;

import java.io.IOException;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.ping.packet.Ping;

import org.junit.jupiter.api.Test;

public class PingTest extends SmackTestSuite {

    @Test
    public void checkProvider() throws Exception {
        // @formatter:off
        String control = "<iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='s2c1' type='get'>"
                + "<ping xmlns='urn:xmpp:ping'/>"
                + "</iq>";
        // @formatter:on
        DummyConnection con = new DummyConnection();
        con.connect();
        // Enable ping for this connection
        PingManager.getInstanceFor(con);
        IQ pingRequest = PacketParserUtils.parseStanza(control);

        assertTrue(pingRequest instanceof Ping);

        con.processStanza(pingRequest);

        Stanza pongPacket = con.getSentPacket();
        assertTrue(pongPacket instanceof IQ);

        IQ pong = (IQ) pongPacket;
        assertThat("capulet.lit", equalsCharSequence(pong.getTo()));
        assertEquals("s2c1", pong.getStanzaId());
        assertEquals(IQ.Type.result, pong.getType());
    }

    @Test
    public void checkSendingPing() throws InterruptedException, SmackException, IOException, XMPPException {
        DummyConnection dummyCon = getAuthenticatedDummyConnection();
        PingManager pinger = PingManager.getInstanceFor(dummyCon);
        try {
            pinger.ping(DUMMY_AT_EXAMPLE_ORG);
        }
        catch (SmackException e) {
            // Ignore the fact the server won't answer for this unit test.
        }

        Stanza sentPacket = dummyCon.getSentPacket();
        assertTrue(sentPacket instanceof Ping);
    }

    @Test
    public void checkSuccessfulPing() throws Exception {
        ThreadedDummyConnection threadedCon = getAuthenticatedDummyConnection();

        PingManager pinger = PingManager.getInstanceFor(threadedCon);

        boolean pingSuccess = pinger.ping(DUMMY_AT_EXAMPLE_ORG);

        assertTrue(pingSuccess);

    }

    /**
     * DummyConnection will not reply so it will timeout.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @Test
    public void checkFailedPingOnTimeout() throws SmackException, IOException, XMPPException, InterruptedException {
        DummyConnection dummyCon = getAuthenticatedDummyConnectionWithoutIqReplies();
        PingManager pinger = PingManager.getInstanceFor(dummyCon);

        try {
            pinger.ping(DUMMY_AT_EXAMPLE_ORG);
        }
        catch (NoResponseException e) {
            return;
        }
        fail();
    }

    /**
     * Server returns an exception for entity.
     * @throws Exception if an exception occurs.
     */
    @Test
    public void checkFailedPingToEntityError() throws Exception {
        ThreadedDummyConnection threadedCon = getAuthenticatedDummyConnection();
        // @formatter:off
        String reply =
                "<iq type='error' id='qrzSp-16' to='test@myserver.com'>" +
                        "<ping xmlns='urn:xmpp:ping'/>" +
                        "<error type='cancel'>" +
                            "<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                 "</iq>";
        // @formatter:on
        IQ serviceUnavailable = PacketParserUtils.parseStanza(reply);
        threadedCon.addIQReply(serviceUnavailable);

        PingManager pinger = PingManager.getInstanceFor(threadedCon);

        boolean pingSuccess = pinger.ping(DUMMY_AT_EXAMPLE_ORG);

        assertFalse(pingSuccess);
    }

    @Test
    public void checkPingToServerSuccess() throws Exception {
        ThreadedDummyConnection con = getAuthenticatedDummyConnection();
        PingManager pinger = PingManager.getInstanceFor(con);

        boolean pingSuccess = pinger.pingMyServer();

        assertTrue(pingSuccess);
    }

    /**
     * Server returns an exception.
     * @throws Exception if an exception occurs.
     */
    @Test
    public void checkPingToServerError() throws Exception {
        ThreadedDummyConnection con = getAuthenticatedDummyConnection();
        // @formatter:off
        String reply =
                "<iq type='error' id='qrzSp-16' to='test@myserver.com' from='" + con.getXMPPServiceDomain() + "'>" +
                        "<ping xmlns='urn:xmpp:ping'/>" +
                        "<error type='cancel'>" +
                            "<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                 "</iq>";
        // @formatter:on
        IQ serviceUnavailable = PacketParserUtils.parseStanza(reply);
        con.addIQReply(serviceUnavailable);

        PingManager pinger = PingManager.getInstanceFor(con);

        boolean pingSuccess = pinger.pingMyServer();

        assertTrue(pingSuccess);
    }

    @Test
    public void checkPingToServerTimeout() throws SmackException, IOException, XMPPException, InterruptedException {
        DummyConnection con = getAuthenticatedDummyConnectionWithoutIqReplies();
        PingManager pinger = PingManager.getInstanceFor(con);

        boolean res = pinger.pingMyServer();
        assertFalse(res);
    }

    @Test
    public void checkSuccessfulDiscoRequest() throws Exception {
        ThreadedDummyConnection con = getAuthenticatedDummyConnection();

        // @formatter:off
        String reply =
                "<iq type='result' id='qrzSp-16' to='test@myserver.com'>" +
                        "<query xmlns='http://jabber.org/protocol/disco#info'><identity category='client' type='pc' name='Pidgin'/>" +
                            "<feature var='urn:xmpp:ping'/>" +
                        "</query></iq>";
        // @formatter:on
        IQ discoReply = PacketParserUtils.parseStanza(reply);
        con.addIQReply(discoReply);

        PingManager pinger = PingManager.getInstanceFor(con);
        boolean pingSupported = pinger.isPingSupported(DUMMY_AT_EXAMPLE_ORG);

        assertTrue(pingSupported);
    }

    @Test
    public void checkUnsuccessfulDiscoRequest() throws Exception {
        ThreadedDummyConnection con = getAuthenticatedDummyConnection();

        // @formatter:off
        String reply =
                "<iq type='result' id='qrzSp-16' to='test@myserver.com'>" +
                        "<query xmlns='http://jabber.org/protocol/disco#info'><identity category='client' type='pc' name='Pidgin'/>" +
                            "<feature var='urn:xmpp:noping'/>" +
                        "</query></iq>";
        // @formatter:on
        IQ discoReply = PacketParserUtils.parseStanza(reply);
        con.addIQReply(discoReply);

        PingManager pinger = PingManager.getInstanceFor(con);
        boolean pingSupported = pinger.isPingSupported(DUMMY_AT_EXAMPLE_ORG);

        assertFalse(pingSupported);
    }

    private static ThreadedDummyConnection getAuthenticatedDummyConnection() throws SmackException, IOException, XMPPException, InterruptedException {
        ThreadedDummyConnection connection = new ThreadedDummyConnection();
        connection.connect();
        connection.login();
        return connection;
    }

    /**
     * The returned connection won't send replies to IQs
     *
     * @return a dummy connection.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws IOException if an I/O error occurred.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private static DummyConnection getAuthenticatedDummyConnectionWithoutIqReplies() throws SmackException, IOException, XMPPException, InterruptedException {
        DummyConnection con = new DummyConnection();
        con.setReplyTimeout(500);
        con.connect();
        con.login();
        return con;
    }
}
