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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
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
class CloseListener implements PacketListener {

    /* manager containing the listeners and the XMPP connection */
    private final InBandBytestreamManager manager;

    /* packet filter for all In-Band Bytestream close requests */
    private final PacketFilter closeFilter = new AndFilter(new PacketTypeFilter(
                    Close.class), new IQTypeFilter(IQ.Type.SET));

    /**
     * Constructor.
     * 
     * @param manager the In-Band Bytestream manager
     */
    protected CloseListener(InBandBytestreamManager manager) {
        this.manager = manager;
    }

    public void processPacket(Packet packet) {
        Close closeRequest = (Close) packet;
        InBandBytestreamSession ibbSession = this.manager.getSessions().get(
                        closeRequest.getSessionID());
        if (ibbSession == null) {
            this.manager.replyItemNotFoundPacket(closeRequest);
        }
        else {
            ibbSession.closeByPeer(closeRequest);
            this.manager.getSessions().remove(closeRequest.getSessionID());
        }

    }

    /**
     * Returns the packet filter for In-Band Bytestream close requests.
     * 
     * @return the packet filter for In-Band Bytestream close requests
     */
    protected PacketFilter getFilter() {
        return this.closeFilter;
    }

}
