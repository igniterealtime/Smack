/*
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.bytestreams.ibb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;
import org.mockito.ArgumentCaptor;

/**
 * Test for InBandBytestreamRequest.
 *
 * @author Henning Staib
 */
public class InBandBytestreamRequestTest extends SmackTestSuite {

    private static final Jid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    private static final Jid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;
    private String sessionID = "session_id";

    private XMPPConnection connection;
    private InBandBytestreamManager byteStreamManager;
    private Open initBytestream;

    /**
     * Initialize fields used in the tests.
     */
    @BeforeEach
    public void setup() {

        // mock connection
        connection = mock(XMPPConnection.class);

        // initialize InBandBytestreamManager to get the InitiationListener
        byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // create a In-Band Bytestream open packet
        initBytestream = new Open(sessionID, 4096);
        initBytestream.setFrom(initiatorJID);
        initBytestream.setTo(targetJID);

    }

    /**
     * Test reject() method.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @Test
    public void shouldReplyWithErrorIfRequestIsRejected() throws NotConnectedException, InterruptedException {
        InBandBytestreamRequest ibbRequest = new InBandBytestreamRequest(
                        byteStreamManager, initBytestream);

        // reject request
        ibbRequest.reject();

        // capture reply to the In-Band Bytestream open request
        ArgumentCaptor<IQ> argument = ArgumentCaptor.forClass(IQ.class);
        verify(connection).sendStanza(argument.capture());

        // assert that reply is the correct error packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.error, argument.getValue().getType());
        assertEquals(StanzaError.Condition.not_acceptable,
                        argument.getValue().getError().getCondition());

    }

    /**
     * Test accept() method.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldReturnSessionIfRequestIsAccepted() throws Exception {
        InBandBytestreamRequest ibbRequest = new InBandBytestreamRequest(
                        byteStreamManager, initBytestream);

        // accept request
        InBandBytestreamSession session = ibbRequest.accept();

        // capture reply to the In-Band Bytestream open request
        ArgumentCaptor<IQ> argument = ArgumentCaptor.forClass(IQ.class);
        verify(connection).sendStanza(argument.capture());

        // assert that reply is the correct acknowledgment packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.result, argument.getValue().getType());

        assertNotNull(session);
        assertNotNull(session.getInputStream());
        assertNotNull(session.getOutputStream());

    }

}
