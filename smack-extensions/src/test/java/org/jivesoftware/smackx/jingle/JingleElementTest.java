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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.provider.JingleProvider;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;

import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xml.sax.SAXException;

/**
 * Test the JingleUtil class.
 */
public class JingleElementTest extends SmackTestSuite {

    private FullJid romeo;
    private FullJid juliet;

    @Before
    public void setup() throws XmppStringprepException {

        XMPPConnection connection = new DummyConnection(
                DummyConnection.getDummyConfigurationBuilder()
                        .setUsernameAndPassword("romeo@montague.lit",
                                "iluvJulibabe13").build());
        JingleManager jm = JingleManager.getInstanceFor(connection);
        romeo = connection.getUser().asFullJidOrThrow();
        juliet = JidCreate.fullFrom("juliet@capulet.lit/balcony");
    }

    @Test
    public void createSessionTerminateDeclineTest() throws Exception {
        JingleElement terminate = JingleElement.createSessionTerminate(juliet, "thisismadness", JingleReasonElement.Reason.decline);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisismadness'>" +
                        "<reason>" +
                        "<decline/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, terminate.getStanzaId(), jingleXML);
        assertXMLEqual(xml, terminate.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.decline);
    }

    @Test
    public void createSessionTerminateSuccessTest() throws Exception {
        JingleElement success = JingleElement.createSessionTerminate(juliet, "thisissparta", JingleReasonElement.Reason.success);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisissparta'>" +
                        "<reason>" +
                        "<success/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, success.getStanzaId(), jingleXML);
        assertXMLEqual(xml, success.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.success);
    }

    @Test
    public void createSessionTerminateBusyTest() throws Exception {
        JingleElement busy = JingleElement.createSessionTerminate(juliet, "thisispatrick", JingleReasonElement.Reason.busy);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisispatrick'>" +
                        "<reason>" +
                        "<busy/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, busy.getStanzaId(), jingleXML);
        assertXMLEqual(xml, busy.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.busy);
    }

    @Test
    public void createSessionTerminateAlternativeSessionTest() throws Exception {
        JingleElement busy = JingleElement.createSessionTerminate(juliet, "thisistherhythm", JingleReasonElement.AlternativeSession("ofthenight"));
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisistherhythm'>" +
                        "<reason>" +
                        "<alternative-session>" +
                        "<sid>ofthenight</sid>" +
                        "</alternative-session>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, busy.getStanzaId(), jingleXML);
        assertXMLEqual(xml, busy.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.alternative_session);
        JingleReasonElement.AlternativeSession alt = (JingleReasonElement.AlternativeSession) jingle.getReason();
        assertEquals("ofthenight", alt.getAlternativeSessionId());
    }

    @Test
    public void createSessionTerminateCancelTest() throws Exception {
        JingleElement cancel = JingleElement.createSessionTerminate(juliet, "thisistheend", JingleReasonElement.Reason.cancel);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisistheend'>" +
                        "<reason>" +
                        "<cancel/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, cancel.getStanzaId(), jingleXML);
        assertXMLEqual(xml, cancel.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.cancel);
    }

    @Test
    public void createSessionTerminateUnsupportedTransportsTest() throws Exception {
        JingleElement unsupportedTransports = JingleElement.createSessionTerminate(juliet, "thisisus", JingleReasonElement.Reason.unsupported_transports);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisisus'>" +
                        "<reason>" +
                        "<unsupported-transports/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, unsupportedTransports.getStanzaId(), jingleXML);
        assertXMLEqual(xml, unsupportedTransports.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.unsupported_transports);
    }

    @Test
    public void createSessionTerminateUnsupportedApplicationsTest() throws Exception {
        JingleElement unsupportedApplications = JingleElement.createSessionTerminate(juliet, "thisiswar", JingleReasonElement.Reason.unsupported_applications);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisiswar'>" +
                        "<reason>" +
                        "<unsupported-applications/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, unsupportedApplications.getStanzaId(), jingleXML);
        assertXMLEqual(xml, unsupportedApplications.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReasonElement.Reason.unsupported_applications);
    }

    @Test
    public void createSessionTerminateFailedTransportTest() throws IOException, SAXException {
        JingleElement failedTransport = JingleElement.createSessionTerminate(juliet, "derailed", JingleReasonElement.Reason.failed_transport);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='derailed'>" +
                        "<reason>" +
                        "<failed-transport/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, failedTransport.getStanzaId(), jingleXML);
        assertXMLEqual(xml, failedTransport.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        assertEquals(JingleAction.session_terminate, failedTransport.getAction());
        assertEquals(JingleReasonElement.Reason.failed_transport, failedTransport.getReason().asEnum());
    }

    @Test
    public void createSessionTerminateFailedApplicationTest() throws IOException, SAXException {
        JingleElement failedApplication = JingleElement.createSessionTerminate(juliet, "crashed", JingleReasonElement.Reason.failed_application);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='crashed'>" +
                        "<reason>" +
                        "<failed-application/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, failedApplication.getStanzaId(), jingleXML);
        assertXMLEqual(xml, failedApplication.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        assertEquals(JingleAction.session_terminate, failedApplication.getAction());
        assertEquals(JingleReasonElement.Reason.failed_application, failedApplication.getReason().asEnum());
    }

    @Test
    public void createSessionPingTest() throws Exception {
        JingleElement ping = JingleElement.createSessionPing(juliet, "thisisit");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-info' " +
                        "sid='thisisit'/>";
        String xml = getIQXML(romeo, juliet, ping.getStanzaId(), jingleXML);
        assertXMLEqual(xml, ping.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(JingleAction.session_info, jingle.getAction());
    }

    @Test
    public void createSessionTerminateContentCancelTest() throws Exception {
        JingleElement cancel = JingleElement.createSessionTerminateContentCancel(juliet, "thisismumbo#5", JingleContentElement.Creator.initiator, "content123");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisismumbo#5'>" +
                        "<content creator='initiator' name='content123'/>" +
                        "<reason>" +
                        "<cancel/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, cancel.getStanzaId(), jingleXML);
        assertXMLEqual(xml, cancel.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        JingleElement jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(JingleAction.session_terminate, jingle.getAction());
        assertEquals(JingleReasonElement.Reason.cancel, jingle.getReason().asEnum());
        assertEquals("thisismumbo#5", jingle.getSid());
        JingleContentElement content = jingle.getContents().get(0);
        assertNotNull(content);
        assertEquals("content123", content.getName());
        assertEquals(JingleContentElement.Creator.initiator, content.getCreator());
    }

    @Test
    public void createSessionTerminateIncompatibleParameters() throws IOException, SAXException {
        JingleElement terminate = JingleElement.createSessionTerminate(juliet, "incompatibleSID", JingleReasonElement.Reason.incompatible_parameters);
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='incompatibleSID'>" +
                        "<reason>" +
                        "<incompatible-parameters/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, terminate.getStanzaId(), jingleXML);
        assertXMLEqual(xml, terminate.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        assertEquals(JingleReasonElement.Reason.incompatible_parameters, terminate.getReason().asEnum());
        assertEquals("incompatibleSID", terminate.getSid());
    }

    @Test
    public void createTransportAcceptTest() throws IOException, SAXException {
        JingleElement transportAccept = JingleElement.createTransportAccept(juliet, romeo, "transAcc", JingleContentElement.Creator.initiator, "cname", new JingleIBBTransportElement("transid", JingleIBBTransport.DEFAULT_BLOCK_SIZE));
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='transport-accept' " +
                        "initiator='" + juliet + "' " +
                        "sid='transAcc'>" +
                        "<content creator='initiator' name='cname'>" +
                        "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' " +
                        "block-size='" + JingleIBBTransport.DEFAULT_BLOCK_SIZE + "' " +
                        "sid='transid'/>" +
                        "</content>" +
                        "</jingle>";
        String xml = getIQXML(juliet, romeo, transportAccept.getStanzaId(), jingleXML);
        assertXMLEqual(xml, transportAccept.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        assertEquals(JingleAction.transport_accept, transportAccept.getAction());
        assertEquals("transAcc", transportAccept.getSid());
    }

    @Test
    public void createTransportRejectTest() {
        // TODO: Find example
    }

    @Test
    public void createTransportReplaceTest() throws IOException, SAXException {
        JingleElement transportReplace = JingleElement.createTransportReplace(juliet, romeo, "transAcc", JingleContentElement.Creator.initiator, "cname", new JingleIBBTransportElement("transid", JingleIBBTransport.DEFAULT_BLOCK_SIZE));
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='transport-replace' " +
                        "initiator='" + juliet + "' " +
                        "sid='transAcc'>" +
                        "<content creator='initiator' name='cname'>" +
                        "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' " +
                        "block-size='" + JingleIBBTransport.DEFAULT_BLOCK_SIZE + "' " +
                        "sid='transid'/>" +
                        "</content>" +
                        "</jingle>";
        String xml = getIQXML(juliet, romeo, transportReplace.getStanzaId(), jingleXML);
        assertXMLEqual(xml, transportReplace.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
        assertEquals(JingleAction.transport_replace, transportReplace.getAction());
        assertEquals("transAcc", transportReplace.getSid());
    }

    @Test
    public void createErrorMalformedRequestTest() throws Exception {
        JingleElement j = defaultJingle(romeo, "error123");
        IQ error = JingleElement.createJingleErrorMalformedRequest(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<bad-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void createErrorTieBreakTest() throws IOException, SAXException {
        JingleElement j = defaultJingle(romeo, "thisistie");
        IQ error = JingleElement.createJingleErrorTieBreak(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<tie-break xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void createErrorUnknownSessionTest() throws IOException, SAXException {
        JingleElement j = defaultJingle(romeo, "youknownothingjohnsnow");
        IQ error = JingleElement.createJingleErrorUnknownSession(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<unknown-session xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void createErrorUnknownInitiatorTest() throws IOException, SAXException {
        JingleElement j = defaultJingle(romeo, "iamyourfather");
        IQ error = JingleElement.createJingleErrorUnknownInitiator(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void createErrorOutOfOrderTest() throws Exception {
        JingleElement j = defaultJingle(romeo, "yourfatheriam");
        IQ error = JingleElement.createJingleErrorOutOfOrder(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='wait'>" + // TODO: Why?
                        "<unexpected-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<out-of-order xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void createErrorUnsupportedInfoTest() throws IOException, SAXException {
        JingleElement j = defaultJingle(romeo, "thisstatementiswrong");
        IQ error = JingleElement.createJingleErrorUnsupportedInfo(j);
        String xml =
                "<iq " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='modify'>" +
                        "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<unsupported-info xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    public static String getIQXML(FullJid from, FullJid to, String stanzaId, String jingleXML) {
        return "<iq id='" + stanzaId + "' to='" + to + "' type='set'>" +
                jingleXML +
                "</iq>";
    }

    private JingleElement defaultJingle(FullJid recipient, String sessionId) {
        return JingleElement.createSessionPing(recipient, sessionId);
    }
}
