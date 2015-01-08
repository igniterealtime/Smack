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
import org.jivesoftware.smackx.bytestreams.ibb.packet.Close;

/**
 * CloseListener handles all In-Band Bytestream close requests.
 * <p>
 * If a close request is received it looks if a stored In-Band Bytestream
 * session exists and closes it. If no session with the given session ID exists
 * an &lt;item-not-found/&gt; error is returned to the sender.
 * 
 * @author Henning Staib
 */
class CloseListener extends AbstractIqRequestHandler {

    /* manager containing the listeners and the XMPP connection */
    private final InBandBytestreamManager manager;

    /**
     * Constructor.
     * 
     * @param manager the In-Band Bytestream manager
     */
    protected CloseListener(InBandBytestreamManager manager) {
        super(Close.ELEMENT, Close.NAMESPACE, IQ.Type.set, Mode.async);
        this.manager = manager;
    }

    @Override
    public IQ handleIQRequest(IQ iqRequest) {
        Close closeRequest = (Close) iqRequest;
        InBandBytestreamSession ibbSession = this.manager.getSessions().get(
                        closeRequest.getSessionID());
        if (ibbSession == null) {
            try {
                this.manager.replyItemNotFoundPacket(closeRequest);
            }
            catch (NotConnectedException e) {
                return null;
            }
        }
        else {
            try {
                ibbSession.closeByPeer(closeRequest);
            }
            catch (NotConnectedException e) {
                return null;
            }
            this.manager.getSessions().remove(closeRequest.getSessionID());
        }
        return null;
    }

}
