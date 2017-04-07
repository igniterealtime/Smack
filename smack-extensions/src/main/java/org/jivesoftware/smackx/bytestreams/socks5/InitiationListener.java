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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.filetransfer.StreamNegotiator;

/**
 * InitiationListener handles all incoming SOCKS5 Bytestream initiation requests. If there are no
 * listeners for a SOCKS5 bytestream request InitiationListener will always refuse the request and
 * reply with a &lt;not-acceptable/&gt; error (<a
 * href="http://xmpp.org/extensions/xep-0065.html#usecase-alternate">XEP-0065</a> Section 5.2.A2).
 * 
 * @author Henning Staib
 */
final class InitiationListener extends AbstractIqRequestHandler {
    private static final Logger LOGGER = Logger.getLogger(InitiationListener.class.getName());

    /* manager containing the listeners and the XMPP connection */
    private final Socks5BytestreamManager manager;

    /* executor service to process incoming requests concurrently */
    private final ExecutorService initiationListenerExecutor;

    /**
     * Constructor
     * 
     * @param manager the SOCKS5 Bytestream manager
     */
    protected InitiationListener(Socks5BytestreamManager manager) {
        super(Bytestream.ELEMENT, Bytestream.NAMESPACE, IQ.Type.set, Mode.async);
        this.manager = manager;
        initiationListenerExecutor = Executors.newCachedThreadPool();
    }


    @Override
    public IQ handleIQRequest(final IQ packet) {
        initiationListenerExecutor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    processRequest(packet);
                }
                catch (InterruptedException | NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "process request", e);
                }
            }
        });

        return null;
    }

    private void processRequest(Stanza packet) throws NotConnectedException, InterruptedException {
        Bytestream byteStreamRequest = (Bytestream) packet;

        StreamNegotiator.signal(byteStreamRequest.getFrom().toString() + '\t' + byteStreamRequest.getSessionID(), byteStreamRequest);

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
     * Shuts down the listeners executor service.
     */
    protected void shutdown() {
        this.initiationListenerExecutor.shutdownNow();
    }

}
