/**
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

/**
 * Test for the CloseListener class.
 * 
 * @author Henning Staib
 */
public class DataListenerTest extends InitExtensions {

    static final Jid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    static final Jid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;

    /**
     * If a data stanza(/packet) of an unknown session is received it should be replied
     * with an &lt;item-not-found/&gt; error.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReplyErrorIfSessionIsUnknown() throws Exception {

        // mock connection
        XMPPConnection connection = mock(XMPPConnection.class);

        // initialize InBandBytestreamManager to get the DataListener
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // get the DataListener from InBandByteStreamManager
        DataListener dataListener = Whitebox.getInternalState(byteStreamManager,
                        DataListener.class);

        DataPacketExtension dpe = new DataPacketExtension("unknownSessionID", 0, "Data");
        Data data = new Data(dpe);
        data.setFrom(initiatorJID);
        data.setTo(targetJID);

        dataListener.handleIQRequest(data);

        // wait because packet is processed in an extra thread
        Thread.sleep(200);

        // capture reply to the In-Band Bytestream close request
        ArgumentCaptor<IQ> argument = ArgumentCaptor.forClass(IQ.class);
        verify(connection).sendStanza(argument.capture());

        // assert that reply is the correct error packet
        assertEquals(initiatorJID, argument.getValue().getTo());
        assertEquals(IQ.Type.error, argument.getValue().getType());
        assertEquals(XMPPError.Condition.item_not_found,
                        argument.getValue().getError().getCondition());

    }

}
