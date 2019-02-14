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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.provider.JingleS5BTransportProvider;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Test Provider and serialization.
 */
public class JingleS5BTransportTest extends SmackTestSuite {

    @Test
    public void candidatesProviderTest() throws Exception {
        String xml =
                "<transport " +
                        "xmlns='urn:xmpp:jingle:transports:s5b:1' " +
                        "dstaddr='972b7bf47291ca609517f67f86b5081086052dad' " +
                        "mode='tcp' " +
                        "sid='vj3hs98y'>" +

                        "<candidate " +
                        "cid='hft54dqy' " +
                        "host='192.168.4.1' " +
                        "jid='romeo@montague.lit/orchard' " +
                        "port='5086' " +
                        "priority='8257636' " +
                        "type='direct'/>" +

                        "<candidate " +
                        "cid='hutr46fe' " +
                        "host='24.24.24.1' " +
                        "jid='romeo@montague.lit/orchard' " +
                        "port='5087' " +
                        "priority='8258636' " +
                        "type='direct'/>" +

                        "<candidate " +
                        "cid='xmdh4b7i' " +
                        "host='123.456.7.8' " +
                        "jid='streamer.shakespeare.lit' " +
                        "port='7625' " +
                        "priority='7878787' " +
                        "type='proxy'/>" +

                        "</transport>";
        JingleS5BTransport transport = new JingleS5BTransportProvider().parse(TestUtils.getParser(xml));
        assertEquals("972b7bf47291ca609517f67f86b5081086052dad", transport.getDestinationAddress());
        assertEquals("vj3hs98y", transport.getStreamId());
        assertEquals(Bytestream.Mode.tcp, transport.getMode());
        assertEquals(3, transport.getCandidates().size());

        assertTrue(transport.hasCandidate("hft54dqy"));
        assertFalse(transport.hasCandidate("invalidId"));
        JingleS5BTransportCandidate candidate1 =
                (JingleS5BTransportCandidate) transport.getCandidates().get(0);
        assertEquals(candidate1, transport.getCandidate("hft54dqy"));
        assertNotNull(candidate1.getStreamHost());
        assertEquals(JingleS5BTransportCandidate.Type.direct.getWeight(), candidate1.getType().getWeight());
        assertEquals("hft54dqy", candidate1.getCandidateId());
        assertEquals("192.168.4.1", candidate1.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate1.getJid());
        assertEquals(5086, candidate1.getPort());
        assertEquals(8257636, candidate1.getPriority());
        assertEquals(JingleS5BTransportCandidate.Type.direct, candidate1.getType());

        JingleS5BTransportCandidate candidate2 =
                (JingleS5BTransportCandidate) transport.getCandidates().get(1);
        assertEquals("hutr46fe", candidate2.getCandidateId());
        assertEquals("24.24.24.1", candidate2.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate2.getJid());
        assertEquals(5087, candidate2.getPort());
        assertEquals(8258636, candidate2.getPriority());
        assertEquals(JingleS5BTransportCandidate.Type.direct, candidate2.getType());

        JingleS5BTransportCandidate candidate3 =
                (JingleS5BTransportCandidate) transport.getCandidates().get(2);
        assertEquals("xmdh4b7i", candidate3.getCandidateId());
        assertEquals("123.456.7.8", candidate3.getHost());
        assertEquals(JidCreate.domainBareFrom("streamer.shakespeare.lit"), candidate3.getJid());
        assertEquals(7625, candidate3.getPort());
        assertEquals(7878787, candidate3.getPriority());
        assertEquals(JingleS5BTransportCandidate.Type.proxy, candidate3.getType());

        assertEquals(xml, transport.toXML().toString());
    }

    @Test
    public void infoProviderTest() throws Exception {
        String candidateError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-error/>" +
                        "</transport>";
        JingleS5BTransport candidateErrorTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateError));
        assertNull(candidateErrorTransport.getDestinationAddress());
        assertNotNull(candidateErrorTransport.getInfo());
        assertEquals("vj3hs98y", candidateErrorTransport.getStreamId());
        assertEquals(JingleS5BTransportInfo.CandidateError.INSTANCE,
                candidateErrorTransport.getInfo());
        assertEquals(candidateError, candidateErrorTransport.toXML().toString());

        String proxyError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<proxy-error/>" +
                        "</transport>";
        JingleS5BTransport proxyErrorTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(proxyError));
        assertNull(proxyErrorTransport.getDestinationAddress());
        assertNotNull(proxyErrorTransport.getInfo());
        assertNotNull(candidateErrorTransport.getInfo());
        assertEquals("vj3hs98y", proxyErrorTransport.getStreamId());
        assertEquals(JingleS5BTransportInfo.ProxyError.INSTANCE,
                proxyErrorTransport.getInfo());
        assertEquals(proxyError, proxyErrorTransport.toXML().toString());

        String candidateUsed =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-used cid='hr65dqyd'/>" +
                        "</transport>";
        JingleS5BTransport candidateUsedTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateUsed));
        assertNotNull(candidateUsedTransport.getInfo());
        assertEquals(new JingleS5BTransportInfo.CandidateUsed("hr65dqyd"),
                candidateUsedTransport.getInfo());
        assertEquals("hr65dqyd",
                ((JingleS5BTransportInfo.CandidateUsed)
                        candidateUsedTransport.getInfo()).getCandidateId());
        assertEquals(candidateUsed, candidateUsedTransport.toXML().toString());

        String candidateActivated =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-activated cid='hr65dqyd'/>" +
                        "</transport>";
        JingleS5BTransport candidateActivatedTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateActivated));
        assertNotNull(candidateActivatedTransport.getInfo());
        assertNotNull(candidateErrorTransport.getInfo());

        assertEquals(new JingleS5BTransportInfo.CandidateActivated("hr65dqyd"),
                candidateActivatedTransport.getInfo());
        assertEquals("hr65dqyd",
                ((JingleS5BTransportInfo.CandidateActivated)
                        candidateActivatedTransport.getInfo()).getCandidateId());
        assertEquals(candidateActivated, candidateActivatedTransport.toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void candidateBuilderInvalidPortTest() {
        JingleS5BTransportCandidate.getBuilder().setPort(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void candidateBuilderInvalidPriorityTest() {
        JingleS5BTransportCandidate.getBuilder().setPriority(-1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transportCandidateIllegalPriorityTest() throws XmppStringprepException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        @SuppressWarnings("unused")
        JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(
                "cid", "host", jid, 5555, -30, JingleS5BTransportCandidate.Type.proxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transportCandidateIllegalPortTest() throws XmppStringprepException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        @SuppressWarnings("unused")
        JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(
                "cid", "host", jid, -5555, 30, JingleS5BTransportCandidate.Type.proxy);
    }

    @Test
    public void candidateFromStreamHostTest() throws XmppStringprepException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        String host = "host.address";
        int port = 1234;
        Bytestream.StreamHost streamHost = new Bytestream.StreamHost(jid, host, port);

        JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(streamHost, 2000, JingleS5BTransportCandidate.Type.direct);

        assertEquals(2000, candidate.getPriority());
        assertEquals(jid, candidate.getJid());
        assertEquals(host, candidate.getHost());
        assertEquals(port, candidate.getPort());

        assertEquals(streamHost.toXML().toString(), candidate.getStreamHost().toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void typeFromIllegalStringTest() {
        JingleS5BTransportCandidate.Type.fromString("illegal-type");
    }
}
