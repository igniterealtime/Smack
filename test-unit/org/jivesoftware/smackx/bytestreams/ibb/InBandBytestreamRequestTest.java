/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test for InBandBytestreamRequest.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamRequestTest {

    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";
    String sessionID = "session_id";

    Connection connection;
    InBandBytestreamManager byteStreamManager;
    Open initBytestream;

    /**
     * Initialize fields used in the tests.
     */
    @Before
    public void setup() {

        // mock connection
        connection = mock(Connection.class);

        // initialize InBandBytestreamManager to get the InitiationListener
        byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // create a In-Band Bytestream open packet
        initBytestream = new Open(sessionID, 4096);
        initBytestream.setFrom(initiatorJID);
        initBytestream.setTo(targetJID);

    }

    /**
     * Test reject() method.
     */
    @Test
    public void shouldReplyWithErrorIfRequestIsRejected() {
        InBandBytestreamRequest ibbRequest = new InBandBytestreamRequest(
                        byteStreamManager, initBytestream);

        // reject request
        ibbRequest.reject();

        // capture reply to the In-Band Bytestream open request
        ArgumentCaptor<IQ> argument = ArgumentCaptor.forClass(IQ.class);
        verify(connection).sendPacket(argument.capture());

        // assert that reply is the correct error packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.ERROR, argument.getValue().getType());
        assertEquals(XMPPError.Condition.no_acceptable.toString(),
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
        verify(connection).sendPacket(argument.capture());

        // assert that reply is the correct acknowledgment packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.RESULT, argument.getValue().getType());

        assertNotNull(session);
        assertNotNull(session.getInputStream());
        assertNotNull(session.getOutputStream());

    }

}
