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

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;

/**
 * DataListener handles all In-Band Bytestream IQ stanzas containing a data
 * stanza(/packet) extension that don't belong to an existing session.
 * <p>
 * If a data stanza(/packet) is received it looks if a stored In-Band Bytestream session
 * exists. If no session with the given session ID exists an
 * &lt;item-not-found/&gt; error is returned to the sender.
 * <p>
 * Data packets belonging to a running In-Band Bytestream session are processed
 * by more specific listeners registered when an {@link InBandBytestreamSession}
 * is created.
 * 
 * @author Henning Staib
 */
class DataListener extends AbstractIqRequestHandler {

    /* manager containing the listeners and the XMPP connection */
    private final InBandBytestreamManager manager;

    /**
     * Constructor.
     * 
     * @param manager the In-Band Bytestream manager
     */
    public DataListener(InBandBytestreamManager manager) {
      super(DataPacketExtension.ELEMENT, DataPacketExtension.NAMESPACE, IQ.Type.set, Mode.async);
        this.manager = manager;
    }

    @Override
    public IQ handleIQRequest(IQ iqRequest) {
        Data data = (Data) iqRequest;
        InBandBytestreamSession ibbSession = this.manager.getSessions().get(
                        data.getDataPacketExtension().getSessionID());
        try {
            if (ibbSession == null) {
                this.manager.replyItemNotFoundPacket(data);
            }
            else {
                ibbSession.processIQPacket(data);
            }
        }
        catch (NotConnectedException e) {
            return null;
        }
        return null;
    }

}
