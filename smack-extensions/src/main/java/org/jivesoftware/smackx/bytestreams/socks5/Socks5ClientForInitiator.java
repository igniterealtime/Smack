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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;
import org.jxmpp.jid.Jid;

/**
 * Implementation of a SOCKS5 client used on the initiators side. This is needed because connecting
 * to the local SOCKS5 proxy differs form the regular way to connect to a SOCKS5 proxy. Additionally
 * a remote SOCKS5 proxy has to be activated by the initiator before data can be transferred between
 * the peers.
 * 
 * @author Henning Staib
 */
class Socks5ClientForInitiator extends Socks5Client {

    /* the XMPP connection used to communicate with the SOCKS5 proxy */
    private WeakReference<XMPPConnection> connection;

    /* the session ID used to activate SOCKS5 stream */
    private String sessionID;

    /* the target JID used to activate SOCKS5 stream */
    // TODO fullJid?
    private final Jid target;

    /**
     * Creates a new SOCKS5 client for the initiators side.
     * 
     * @param streamHost containing network settings of the SOCKS5 proxy
     * @param digest identifying the SOCKS5 Bytestream
     * @param connection the XMPP connection
     * @param sessionID the session ID of the SOCKS5 Bytestream
     * @param target the target JID of the SOCKS5 Bytestream
     */
    public Socks5ClientForInitiator(StreamHost streamHost, String digest, XMPPConnection connection,
                    String sessionID, Jid target) {
        super(streamHost, digest);
        this.connection = new WeakReference<>(connection);
        this.sessionID = sessionID;
        this.target = target;
    }

    @Override
    public Socket getSocket(int timeout) throws IOException, InterruptedException,
                    TimeoutException, XMPPException, SmackException {
        Socket socket = null;

        // check if stream host is the local SOCKS5 proxy
        if (this.streamHost.getJID().equals(this.connection.get().getUser())) {
            Socks5Proxy socks5Server = Socks5Proxy.getSocks5Proxy();
            socket = socks5Server.getSocket(this.digest);
            if (socket == null) {
                throw new SmackException("target is not connected to SOCKS5 proxy");
            }
        }
        else {
            socket = super.getSocket(timeout);

            try {
                activate();
            }
            catch (XMPPException e1) {
                socket.close();
                throw e1;
            }
            catch (NoResponseException e2) {
                socket.close();
                throw e2;
            }

        }

        return socket;
    }

    /**
     * Activates the SOCKS5 Bytestream by sending an XMPP SOCKS5 Bytestream activation stanza(/packet) to the
     * SOCKS5 proxy.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @throws SmackException if there was no response from the server.
     */
    private void activate() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Bytestream activate = createStreamHostActivation();
        // if activation fails #nextResultOrThrow() throws an exception
        connection.get().createStanzaCollectorAndSend(activate).nextResultOrThrow();
    }

    /**
     * Returns a SOCKS5 Bytestream activation packet.
     * 
     * @return SOCKS5 Bytestream activation packet
     */
    private Bytestream createStreamHostActivation() {
        Bytestream activate = new Bytestream(this.sessionID);
        activate.setMode(null);
        activate.setType(IQ.Type.set);
        activate.setTo(this.streamHost.getJID());

        activate.setToActivate(this.target);

        return activate;
    }

}
