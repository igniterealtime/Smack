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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.smackx.filetransfer.StreamNegotiator;


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
class InitiationListener extends AbstractIqRequestHandler {
    private static final Logger LOGGER = Logger.getLogger(InitiationListener.class.getName());

    /* manager containing the listeners and the XMPP connection */
    private final InBandBytestreamManager manager;

    /* executor service to process incoming requests concurrently */
    private final ExecutorService initiationListenerExecutor;

    /**
     * Constructor.
     * 
     * @param manager the In-Band Bytestream manager
     */
    protected InitiationListener(InBandBytestreamManager manager) {
        super(Open.ELEMENT, Open.NAMESPACE, IQ.Type.set, Mode.async);
        this.manager = manager;
        initiationListenerExecutor = Executors.newCachedThreadPool();
     }

    @Override
    public IQ handleIQRequest(final IQ packet) {
        initiationListenerExecutor.execute(new Runnable() {

            public void run() {
                try {
                    processRequest(packet);
                }
                catch (InterruptedException | NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "proccessRequest", e);
                }
            }
        });
        return null;
    }

    private void processRequest(Stanza packet) throws NotConnectedException, InterruptedException {
        Open ibbRequest = (Open) packet;

        // validate that block size is within allowed range
        if (ibbRequest.getBlockSize() > this.manager.getMaximumBlockSize()) {
            this.manager.replyResourceConstraintPacket(ibbRequest);
            return;
        }

        StreamNegotiator.signal(ibbRequest.getFrom().toString() + '\t' + ibbRequest.getSessionID(), ibbRequest);

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
     * Shuts down the listeners executor service.
     */
    protected void shutdown() {
        this.initiationListenerExecutor.shutdownNow();
    }

}
