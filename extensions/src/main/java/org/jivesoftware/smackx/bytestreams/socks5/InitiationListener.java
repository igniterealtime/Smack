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
package org.jivesoftware.smackx.bytestreams.socks5;

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
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;

/**
 * InitiationListener handles all incoming SOCKS5 Bytestream initiation requests. If there are no
 * listeners for a SOCKS5 bytestream request InitiationListener will always refuse the request and
 * reply with a &lt;not-acceptable/&gt; error (<a
 * href="http://xmpp.org/extensions/xep-0065.html#usecase-alternate">XEP-0065</a> Section 5.2.A2).
 * 
 * @author Henning Staib
 */
final class InitiationListener implements PacketListener {

    /* manager containing the listeners and the XMPP connection */
    private final Socks5BytestreamManager manager;

    /* packet filter for all SOCKS5 Bytestream requests */
    private final PacketFilter initFilter = new AndFilter(new PacketTypeFilter(Bytestream.class),
                    new IQTypeFilter(IQ.Type.SET));

    /* executor service to process incoming requests concurrently */
    private final ExecutorService initiationListenerExecutor;

    /**
     * Constructor
     * 
     * @param manager the SOCKS5 Bytestream manager
     */
    protected InitiationListener(Socks5BytestreamManager manager) {
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
        Bytestream byteStreamRequest = (Bytestream) packet;

        // ignore request if in ignore list
        if (this.manager.getIgnoredBytestreamRequests().remove(byteStreamRequest.getSessionID())) {
            return;
        }

        // build bytestream request from packet
        Socks5BytestreamRequest request = new Socks5BytestreamRequest(this.manager,
                        byteStreamRequest);

        // notify listeners for bytestream initiation from a specific user
        BytestreamListener userListener = this.manager.getUserListener(byteStreamRequest.getFrom());
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
            this.manager.replyRejectPacket(byteStreamRequest);
        }
    }

    /**
     * Returns the packet filter for SOCKS5 Bytestream initialization requests.
     * 
     * @return the packet filter for SOCKS5 Bytestream initialization requests
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
