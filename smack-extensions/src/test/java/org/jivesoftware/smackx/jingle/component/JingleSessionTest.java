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
package org.jivesoftware.smackx.jingle.component;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;
import org.jivesoftware.smackx.jingle.util.Role;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class JingleSessionTest extends SmackTestSuite {

    @Test
    public void jingleSessionTest() throws XmppStringprepException {
        DummyConnection dummyConnection = new DummyConnection();
        FullJid alice = JidCreate.fullFrom("alice@wonderland.lit/test123");
        FullJid madHatter = JidCreate.fullFrom("mad@hat.net/cat");

        JingleManager jingleManager = JingleManager.getInstanceFor(dummyConnection);

        JingleSession session = new JingleSession(jingleManager, alice, madHatter, Role.initiator, "WeReAlLmAdHeRe");

        assertEquals(alice, session.getInitiator());
        assertEquals(madHatter, session.getResponder());
        assertEquals(alice, session.getOurJid());
        assertEquals(madHatter, session.getPeer());

        assertEquals(0, session.getContents().size());
        assertEquals("WeReAlLmAdHeRe", session.getSessionId());
        assertEquals(jingleManager, session.getJingleManager());
    }

    @Test(expected = IllegalStateException.class)
    public void getSoleContentThrowingTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()), null, null, Role.initiator, null);
        assertTrue(session.isInitiator());
        assertFalse(session.isResponder());
        JingleContent c1 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleContent c2 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(c1);
        assertEquals(c1, session.getContent(c1.getName()));
        session.addContent(c2);
        assertEquals(c2, session.getContent(c2.getName()));

        session.getSoleContentOrThrow();
    }

    @Test
    public void getSoleContentTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()), null, null, Role.responder, null);
        assertTrue(session.isResponder());
        assertFalse(session.isInitiator());
        assertNull(session.getSoleContentOrThrow());
        JingleContent c1 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        assertNull(c1.getParent());
        session.addContent(c1);
        assertEquals(session, c1.getParent());

        assertEquals(c1, session.getSoleContentOrThrow());
    }

    @Test
    public void createSessionAcceptTest() throws XmppStringprepException {
        FullJid initiator = JidCreate.fullFrom("initiator@server.tld/res");
        FullJid responder = JidCreate.fullFrom("responder@server.tld/res");
        JingleManager manager = JingleManager.getInstanceFor(new DummyConnection());
        JingleSession session = new JingleSession(manager, initiator, responder, Role.initiator, "sessionId");

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);

        JingleIBBTransport transport = new JingleIBBTransport("streamId", (short) 1024);
        content.setTransport(transport);

        JingleDescription<?> description = new JingleDescription<JingleContentDescriptionElement>() {
            public static final String NAMESPACE = "urn:xmpp:jingle:apps:stub:0";

            @Override
            public JingleContentDescriptionElement getElement() {
                return new JingleContentDescriptionElement(null) {
                    @Override
                    public String getNamespace() {
                        return NAMESPACE;
                    }
                };
            }

            @Override
            public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
                return null;
            }

            @Override
            public void onBytestreamReady(BytestreamSession bytestreamSession) {

            }

            @Override
            public String getNamespace() {
                return NAMESPACE;
            }

            @Override
            public void handleContentTerminate(JingleReasonElement.Reason reason) {

            }
        };
        content.setDescription(description);

        session.addContent(content);

        JingleElement sessionElement = session.createSessionInitiate();
        assertNotNull(sessionElement);
        assertEquals("sessionId", sessionElement.getSid());
        assertEquals(initiator, sessionElement.getInitiator());
        assertNull(sessionElement.getResponder());
        assertEquals(JingleAction.session_initiate, sessionElement.getAction());
        JingleContentElement contentElement = sessionElement.getSoleContentOrThrow();
        assertNotNull(contentElement);
        assertEquals(content.getName(), contentElement.getName());
        assertEquals(content.getCreator(), contentElement.getCreator());
        assertEquals(content.getSenders(), contentElement.getSenders());
        assertEquals(0, content.getTransportBlacklist().size());
        assertEquals(content.getElement().toXML(null).toString(), contentElement.toXML(null).toString());

        JingleIBBTransportElement transportElement = (JingleIBBTransportElement) contentElement.getTransport();
        assertNotNull(transportElement);
        assertEquals(transport.getBlockSize(), transportElement.getBlockSize());
        assertEquals(transport.getStreamId(), transportElement.getStreamId());
        assertEquals(transport.getNamespace(), transportElement.getNamespace());
        assertEquals(transport.getElement().toXML(null).toString(), transportElement.toXML(null).toString());

        JingleContentDescriptionElement descriptionElement = contentElement.getDescription();
        assertNotNull(descriptionElement);
        assertEquals(description.getNamespace(), descriptionElement.getNamespace());
        assertEquals(description.getElement().toXML(null).toString(), descriptionElement.toXML(null).toString());

        assertNull(contentElement.getSecurity());
        assertTrue(content.isSending());
        assertFalse(content.isReceiving());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateContentAddTest() throws XmppStringprepException {
        FullJid initiator = JidCreate.fullFrom("initiator@server.tld/res");
        FullJid responder = JidCreate.fullFrom("responder@server.tld/res");
        JingleManager manager = JingleManager.getInstanceFor(new DummyConnection());
        JingleSession session = new JingleSession(manager, initiator, responder, Role.initiator, "sessionId");

        JingleContent content1 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleContent content2 = new JingleContent(null, null, null, content1.getName(), null, JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);

        session.addContent(content1);
        session.addContent(content2);
    }

    @Test(expected = IllegalStateException.class)
    public void sessionInitiateThrowsAsResponderTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()),
                null, null, Role.responder, "session");
        session.createSessionInitiate();
    }

    @Test
    public void sessionAcceptTest() throws XmppStringprepException {
        FullJid initiator = JidCreate.fullFrom("initiator@server.tld/res");
        FullJid responder = JidCreate.fullFrom("responder@server.tld/res");
        JingleManager manager = JingleManager.getInstanceFor(new DummyConnection());
        JingleSession session = new JingleSession(manager, initiator, responder, Role.responder, "sessionId");

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);

        JingleIBBTransport transport = new JingleIBBTransport("streamId", (short) 1024);
        content.setTransport(transport);

        JingleDescription<?> description = new JingleDescription<JingleContentDescriptionElement>() {
            public static final String NAMESPACE = "urn:xmpp:jingle:apps:stub:0";

            @Override
            public JingleContentDescriptionElement getElement() {
                return new JingleContentDescriptionElement(null) {
                    @Override
                    public String getNamespace() {
                        return NAMESPACE;
                    }
                };
            }

            @Override
            public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
                return null;
            }

            @Override
            public void onBytestreamReady(BytestreamSession bytestreamSession) {

            }

            @Override
            public String getNamespace() {
                return NAMESPACE;
            }

            @Override
            public void handleContentTerminate(JingleReasonElement.Reason reason) {

            }
        };
        content.setDescription(description);

        session.addContent(content);

        JingleElement accept = session.createSessionAccept();
        assertNotNull(accept);
        assertEquals(JingleAction.session_accept, accept.getAction());
        assertNull(accept.getInitiator());
        assertEquals(session.getResponder(), accept.getResponder());
        assertEquals(1, accept.getContents().size());
        assertEquals(content.getName(), accept.getSoleContentOrThrow().getName());
        assertFalse(content.isSending());
        assertTrue(content.isReceiving());
    }

    @Test(expected = IllegalStateException.class)
    public void sessionAcceptThrowsAsInitiatorTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()),
                null, null, Role.initiator, "session");
        session.createSessionAccept();
    }
}
