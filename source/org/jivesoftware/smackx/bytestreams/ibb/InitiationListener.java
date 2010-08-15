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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

/**
 * InitiationListener handles all incoming In-Band Bytestream open requests. If there are no
 * listeners for a In-Band Bytestream request InitiationListener will always refuse the request and
 * reply with a &lt;not-acceptable/&gt; error (<a
 * href="http://xmpp.org/extensions/xep-0047.html#example-5" >XEP-0047</a> Section 2.1).
 * <p>
 * All In-Band Bytestream request having a block size greater than the maximum allowed block size
 * for this connection are rejected with an &lt;resource-constraint/&gt; error. The maximum block
 * size can be set by invoking {@link InBandBytestreamManager#setMaximumBlockSize(int)}.
 * 
 * @author Henning Staib
 */
class InitiationListener implements PacketListener {

    /* manager containing the listeners and the XMPP connection */
    private final InBandBytestreamManager manager;

    /* packet filter for all In-Band Bytestream requests */
    private final PacketFilter initFilter = new AndFilter(new PacketTypeFilter(Open.class),
                    new IQTypeFilter(IQ.Type.SET));

    /* executor service to process incoming requests concurrently */
    private final ExecutorService initiationListenerExecutor;

    /**
     * Constructor.
     * 
     * @param manager the In-Band Bytestream manager
     */
    protected InitiationListener(InBandBytestreamManager manager) {
        this.manager = manager;
        initiationListenerExecutor = Executors.newCachedThreadPool();
    }

    public void processPacket(final Packet packet) {
        initiationListenerExecutor.execute(new Runnable() {

            public void run() {
                processRequest(packet);
            }
        });
    }

    private void processRequest(Packet packet) {
        Open ibbRequest = (Open) packet;

        // validate that block size is within allowed range
        if (ibbRequest.getBlockSize() > this.manager.getMaximumBlockSize()) {
            this.manager.replyResourceConstraintPacket(ibbRequest);
            return;
        }

        // ignore request if in ignore list
        if (this.manager.getIgnoredBytestreamRequests().remove(ibbRequest.getSessionID()))
            return;

        // build bytestream request from packet
        InBandBytestreamRequest request = new InBandBytestreamRequest(this.manager, ibbRequest);

        // notify listeners for bytestream initiation from a specific user
        BytestreamListener userListener = this.manager.getUserListener(ibbRequest.getFrom());
        if (userListener != null) {
            userListener.incomingBytestreamRequest(request);

        }
        else if (!this.manager.getAllRequestListeners().isEmpty()) {
            /*
             * if there is no user specific listener inform listeners for all initiation requests
             */
            for (BytestreamListener listener : this.manager.getAllRequestListeners()) {
                listener.incomingBytestreamRequest(request);
            }

        }
        else {
            /*
             * if there is no listener for this initiation request, reply with reject message
             */
            this.manager.replyRejectPacket(ibbRequest);
        }
    }

    /**
     * Returns the packet filter for In-Band Bytestream open requests.
     * 
     * @return the packet filter for In-Band Bytestream open requests
     */
    protected PacketFilter getFilter() {
        return this.initFilter;
    }

    /**
     * Shuts down the listeners executor service.
     */
    protected void shutdown() {
        this.initiationListenerExecutor.shutdownNow();
    }

}
