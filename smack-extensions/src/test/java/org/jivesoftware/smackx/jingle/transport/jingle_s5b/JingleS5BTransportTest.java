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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.provider.JingleS5BTransportProvider;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xml.sax.SAXException;

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
        JingleS5BTransportElement transportElement = new JingleS5BTransportProvider().parse(TestUtils.getParser(xml));
        assertEquals("972b7bf47291ca609517f67f86b5081086052dad", transportElement.getDestinationAddress());
        assertEquals("vj3hs98y", transportElement.getStreamId());
        assertEquals(Bytestream.Mode.tcp, transportElement.getMode());
        assertEquals(3, transportElement.getCandidates().size());

        assertTrue(transportElement.hasCandidate("hft54dqy"));
        assertFalse(transportElement.hasCandidate("invalidId"));
        JingleS5BTransportCandidateElement candidate1 =
                (JingleS5BTransportCandidateElement) transportElement.getCandidates().get(0);
        assertEquals(candidate1, transportElement.getCandidate("hft54dqy"));
        assertNotNull(candidate1.getStreamHost());
        assertEquals(JingleS5BTransportCandidateElement.Type.direct.getWeight(), candidate1.getType().getWeight());
        assertEquals("hft54dqy", candidate1.getCandidateId());
        assertEquals("192.168.4.1", candidate1.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate1.getJid());
        assertEquals(5086, candidate1.getPort());
        assertEquals(8257636, candidate1.getPriority());
        assertEquals(JingleS5BTransportCandidateElement.Type.direct, candidate1.getType());

        JingleS5BTransportCandidateElement candidate2 =
                (JingleS5BTransportCandidateElement) transportElement.getCandidates().get(1);
        assertEquals("hutr46fe", candidate2.getCandidateId());
        assertEquals("24.24.24.1", candidate2.getHost());
        assertEquals(JidCreate.from("romeo@montague.lit/orchard"), candidate2.getJid());
        assertEquals(5087, candidate2.getPort());
        assertEquals(8258636, candidate2.getPriority());
        assertEquals(JingleS5BTransportCandidateElement.Type.direct, candidate2.getType());

        JingleS5BTransportCandidateElement candidate3 =
                (JingleS5BTransportCandidateElement) transportElement.getCandidates().get(2);
        assertEquals("xmdh4b7i", candidate3.getCandidateId());
        assertEquals("123.456.7.8", candidate3.getHost());
        assertEquals(JidCreate.domainBareFrom("streamer.shakespeare.lit"), candidate3.getJid());
        assertEquals(7625, candidate3.getPort());
        assertEquals(7878787, candidate3.getPriority());
        assertEquals(JingleS5BTransportCandidateElement.Type.proxy, candidate3.getType());

        assertEquals(xml, transportElement.toXML(null).toString());

        JingleS5BTransport transport = new JingleS5BTransportAdapter().transportFromElement(transportElement);
        assertNotNull(transport);
        assertEquals(transportElement.getStreamId(), transport.getStreamId());
        assertEquals(transportElement.getMode(), transport.getMode());
        assertEquals(transportElement.getDestinationAddress(), transport.getTheirDstAddr());
        assertNull(transport.getOurDstAddr());
        assertNotNull(transport.getOurCandidates());
        assertEquals(0, transport.getOurCandidates().size());
        assertNotNull(transport.getTheirCandidates());
        assertEquals(3, transport.getTheirCandidates().size());

        for (int i = 0; i < transport.getTheirCandidates().size() - 1; i++) {
            assertTrue(transport.getTheirCandidates().get(i).getPriority() >= transport.getTheirCandidates().get(i + 1).getPriority());
        }

        JingleTransportCandidate<?> c = transport.getTheirCandidates().get(1);
        transport.addTheirCandidate(c);

        assertEquals(3, transport.getTheirCandidates().size());
        assertTrue(c.getParent() == transport);

        assertNull(transport.getParent());

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        assertNull(content.getTransport());

        content.setTransport(transport);
        assertEquals(transport, content.getTransport());
        assertEquals(content, transport.getParent());
    }

    @Test
    public void infoProviderTest() throws Exception {
        String candidateError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-error/>" +
                        "</transport>";
        JingleS5BTransportElement candidateErrorTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateError));
        assertNull(candidateErrorTransport.getDestinationAddress());
        assertNotNull(candidateErrorTransport.getInfo());
        assertEquals("vj3hs98y", candidateErrorTransport.getStreamId());
        assertEquals(JingleS5BTransportInfoElement.CandidateError.INSTANCE,
                candidateErrorTransport.getInfo());
        assertEquals(candidateError, candidateErrorTransport.toXML(null).toString());

        String proxyError =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<proxy-error/>" +
                        "</transport>";
        JingleS5BTransportElement proxyErrorTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(proxyError));
        assertNull(proxyErrorTransport.getDestinationAddress());
        assertNotNull(proxyErrorTransport.getInfo());
        assertNotNull(candidateErrorTransport.getInfo());
        assertEquals("vj3hs98y", proxyErrorTransport.getStreamId());
        assertEquals(JingleS5BTransportInfoElement.ProxyError.INSTANCE,
                proxyErrorTransport.getInfo());
        assertEquals(proxyError, proxyErrorTransport.toXML(null).toString());

        String candidateUsed =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<candidate-used cid='hr65dqyd'/>" +
                        "</transport>";
        JingleS5BTransportElement candidateUsedTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateUsed));
        assertNotNull(candidateUsedTransport.getInfo());
        assertEquals(new JingleS5BTransportInfoElement.CandidateUsed("hr65dqyd"),
                candidateUsedTransport.getInfo());
        assertEquals("hr65dqyd",
                ((JingleS5BTransportInfoElement.CandidateUsed)
                        candidateUsedTransport.getInfo()).getCandidateId());
        assertEquals(candidateUsed, candidateUsedTransport.toXML(null).toString());

        String candidateActivated =
                "<transport xmlns='urn:xmpp:jingle:transports:s5b:1' sid='vj3hs98y'>" +
                        "<activated cid='hr65dqyd'/>" +
                        "</transport>";
        JingleS5BTransportElement candidateActivatedTransport = new JingleS5BTransportProvider()
                .parse(TestUtils.getParser(candidateActivated));
        assertNotNull(candidateActivatedTransport.getInfo());
        assertNotNull(candidateErrorTransport.getInfo());

        assertEquals(new JingleS5BTransportInfoElement.CandidateActivated("hr65dqyd"),
                candidateActivatedTransport.getInfo());
        assertEquals("hr65dqyd",
                ((JingleS5BTransportInfoElement.CandidateActivated)
                        candidateActivatedTransport.getInfo()).getCandidateId());
        assertEquals(candidateActivated, candidateActivatedTransport.toXML(null).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void candidateBuilderInvalidPortTest() {
        JingleS5BTransportCandidateElement.getBuilder().setPort(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void candidateBuilderInvalidPriorityTest() {
        JingleS5BTransportCandidateElement.getBuilder().setPriority(-1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transportCandidateIllegalPriorityTest() throws XmppStringprepException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        JingleS5BTransportCandidateElement candidate = new JingleS5BTransportCandidateElement(
                "cid", "host", jid, 5555, -30, JingleS5BTransportCandidateElement.Type.proxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transportCandidateIllegalPortTest() throws XmppStringprepException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        JingleS5BTransportCandidateElement candidate = new JingleS5BTransportCandidateElement(
                "cid", "host", jid, -5555, 30, JingleS5BTransportCandidateElement.Type.proxy);
    }

    @Test
    public void candidateFromStreamHostTest() throws IOException, SAXException {
        FullJid jid = JidCreate.fullFrom("test@test.test/test");
        String host = "host.address";
        int port = 1234;
        Bytestream.StreamHost streamHost = new Bytestream.StreamHost(jid, host, port);

        JingleS5BTransportCandidateElement candidate = new JingleS5BTransportCandidateElement(streamHost, 2000, JingleS5BTransportCandidateElement.Type.direct);

        assertEquals(2000, candidate.getPriority());
        assertEquals(jid, candidate.getJid());
        assertEquals(host, candidate.getHost());
        assertEquals(port, candidate.getPort());

        assertXMLEqual(streamHost.toXML(null).toString(), candidate.getStreamHost().toXML(null).toString());
    }

    @Test
    public void constructorsTest() throws XmppStringprepException {
        FullJid initiator = JidCreate.fullFrom("in@it.ia/tor");
        FullJid responder = JidCreate.fullFrom("re@sp.on/der");

        List<JingleTransportCandidate<?>> c1 = new ArrayList<>();
        JingleS5BTransportCandidate c11 = new JingleS5BTransportCandidate("1234", new Bytestream.StreamHost(JidCreate.from("p.b.c"), "p.b.c", 9999), 100, JingleS5BTransportCandidateElement.Type.proxy);
        c1.add(c11);

        List<JingleTransportCandidate<?>> c2 = new ArrayList<>();
        JingleS5BTransportCandidate c21 = new JingleS5BTransportCandidate("1337", new Bytestream.StreamHost(JidCreate.from("p.a.b"), "p.a.b", 1000), 101, JingleS5BTransportCandidateElement.Type.proxy);
        JingleS5BTransportCandidate c22 = new JingleS5BTransportCandidate("1009", new Bytestream.StreamHost(JidCreate.from("p.a.b"), "p.a.b", 2000), 10, JingleS5BTransportCandidateElement.Type.proxy);
        c2.add(c21);
        c2.add(c22);


        JingleS5BTransport t1 = new JingleS5BTransport(initiator, responder, "tSes", Bytestream.Mode.tcp, c1);

        assertEquals("tSes", t1.getStreamId());
        assertEquals(Bytestream.Mode.tcp, t1.getMode());

        assertEquals(Socks5Utils.createDigest("tSes", initiator, responder), t1.getOurDstAddr());
        assertNull(t1.getTheirDstAddr());

        assertEquals(1, t1.getOurCandidates().size());
        assertEquals(c11, t1.getOurCandidates().get(0));
        assertEquals(0, t1.getTheirCandidates().size());


        JingleS5BTransport t1parsed = new JingleS5BTransportAdapter().transportFromElement(t1.getElement());

        assertEquals(t1.getOurDstAddr(), t1parsed.getTheirDstAddr());
        assertNull(t1parsed.getOurDstAddr());
        assertEquals(0, t1parsed.getOurCandidates().size());
        assertEquals(t1.getStreamId(), t1parsed.getStreamId());
        assertEquals(t1.getMode(), t1parsed.getMode());
        assertEquals(t1.getOurCandidates().size(), t1parsed.getTheirCandidates().size());

        JingleS5BTransport t2 = new JingleS5BTransport(initiator, responder, c2, t1parsed);

        assertEquals("tSes", t2.getStreamId());
        assertEquals(Bytestream.Mode.tcp, t2.getMode());

        assertEquals(Socks5Utils.createDigest("tSes", responder, initiator), t2.getOurDstAddr());
        assertEquals(t1.getOurDstAddr(), t2.getTheirDstAddr());

        assertEquals(2, t2.getOurCandidates().size());
        assertEquals(c2, t2.getOurCandidates());
        assertEquals(t1.getOurCandidates().size(), t2.getTheirCandidates().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void typeFromIllegalStringTest() {
        JingleS5BTransportCandidateElement.Type.fromString("illegal-type");
    }
}
