/**
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
import org.jivesoftware.smackx.bytestreams.ibb.CloseListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Close;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

/**
 * Test for the CloseListener class.
 * 
 * @author Henning Staib
 */
public class CloseListenerTest {

    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";

    /**
     * If a close request to an unknown session is received it should be replied
     * with an &lt;item-not-found/&gt; error.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReplyErrorIfSessionIsUnknown() throws Exception {

        // mock connection
        Connection connection = mock(Connection.class);

        // initialize InBandBytestreamManager to get the CloseListener
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // get the CloseListener from InBandByteStreamManager
        CloseListener closeListener = Whitebox.getInternalState(byteStreamManager,
                        CloseListener.class);

        Close close = new Close("unknownSessionId");
        close.setFrom(initiatorJID);
        close.setTo(targetJID);

        closeListener.processPacket(close);

        // wait because packet is processed in an extra thread
        Thread.sleep(200);

        // capture reply to the In-Band Bytestream close request
        ArgumentCaptor<IQ> argument = ArgumentCaptor.forClass(IQ.class);
        verify(connection).sendPacket(argument.capture());

        // assert that reply is the correct error packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.ERROR, argument.getValue().getType());
        assertEquals(XMPPError.Condition.item_not_found.toString(),
                        argument.getValue().getError().getCondition());

    }

}
