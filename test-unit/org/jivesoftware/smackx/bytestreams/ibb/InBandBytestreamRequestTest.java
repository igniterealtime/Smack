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
