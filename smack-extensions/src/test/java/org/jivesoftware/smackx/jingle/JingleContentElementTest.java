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
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.TestIQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.util.Role;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test the JingleContent class.
 */
public class JingleContentElementTest extends SmackTestSuite {

    @Test(expected = NullPointerException.class)
    public void emptyBuilderThrowsTest() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder();
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onlyCreatorBuilderThrowsTest() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder();
        builder.setCreator(JingleContentElement.Creator.initiator);
        builder.build();
    }

    @Test
    public void parserTest() throws Exception {

        JingleContentElement.Builder builder = JingleContentElement.getBuilder();

        builder.setCreator(JingleContentElement.Creator.initiator);
        builder.setName("A name");

        JingleContentElement content = builder.build();
        assertNotNull(content);
        assertNull(content.getDescription());
        assertEquals(JingleContentElement.Creator.initiator, content.getCreator());
        assertEquals("A name", content.getName());

        builder.setSenders(JingleContentElement.Senders.both);
        content = builder.build();
        assertEquals(JingleContentElement.Senders.both, content.getSenders());

        builder.setDisposition("session");
        JingleContentElement content1 = builder.build();
        assertEquals("session", content1.getDisposition());
        assertNotSame(content.toXML(null).toString(), content1.toXML(null).toString());
        assertEquals(content1.toXML(null).toString(), builder.build().toXML(null).toString());

        String xml =
                "<content creator='initiator' disposition='session' name='A name' senders='both'>" +
                "</content>";
        assertEquals(xml, content1.toXML(null).toString());

        JingleContent fromElement = JingleContent.fromElement(content1);
        assertEquals("A name", fromElement.getName());
        assertEquals(content1.getCreator(), fromElement.getCreator());
        assertEquals(content1.getSenders(), fromElement.getSenders());
        assertNull(fromElement.getTransport());
        assertNull(fromElement.getDescription());
        assertNull(fromElement.getSecurity());
        assertXMLEqual(xml, fromElement.getElement().toXML(null).toString());
    }

    @Test
    public void handleJingleRequestTest() {
        XMPPConnection connection = mock(XMPPConnection.class);
        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);

        IQ descriptionInfoResult = new TestIQ("description_info", "test");
        IQ securityInfoResult = new TestIQ("description_info", "test");
        IQ sessionInfoResult = new TestIQ("session_info", "test");
        IQ transportAcceptResult = new TestIQ("transport_accept", "test");
        IQ transportInfoResult = new TestIQ("transport_info", "test");
        IQ transportRejectResult = new TestIQ("transport_reject", "test");
        IQ transportReplaceResult = new TestIQ("transport_replace", "test");

        JingleElement contentModify = JingleElement.getBuilder().setAction(JingleAction.content_modify).setSessionId("session").build();
        assertTrue(content.handleJingleRequest(contentModify, connection).getError().getCondition() == StanzaError.Condition.feature_not_implemented);

        JingleElement descriptionInfo = JingleElement.getBuilder().setAction(JingleAction.description_info).setSessionId("session").build();
        assertTrue(content.handleJingleRequest(descriptionInfo, connection).getError().getCondition() == StanzaError.Condition.feature_not_implemented);
    }

    @Test
    public void startTest() throws XmppStringprepException, SmackException.NotConnectedException, InterruptedException {
        XMPPConnection connection = new DummyConnection();
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(connection), connection.getUser().asFullJidOrThrow(), JidCreate.fullFrom("bob@baumeister.de/buddl"), Role.initiator, "session");
        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleTransport<?> transport = mock(JingleTransport.class);
        content.setTransport(transport);
        session.addContent(content);

        final boolean[] sync = new boolean[1];

        content.start(connection);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                sync[0] = true;
                return null;
            }
        }).when(transport).establishOutgoingBytestreamSession(connection, content, session);

        Date start = new Date();
        while (!sync[0]) {
            Date now = new Date();
            if (now.getTime() - start.getTime() > 2000) {
                break;
            }
            // Unfortunately there are no ResultSyncPoints available, so we have to cheat a little bit.
        }

        verify(transport).establishOutgoingBytestreamSession(connection, content, session);
    }
}
