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

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

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
    private Connection connection;

    /* the session ID used to activate SOCKS5 stream */
    private String sessionID;

    /* the target JID used to activate SOCKS5 stream */
    private String target;

    /**
     * Creates a new SOCKS5 client for the initiators side.
     * 
     * @param streamHost containing network settings of the SOCKS5 proxy
     * @param digest identifying the SOCKS5 Bytestream
     * @param connection the XMPP connection
     * @param sessionID the session ID of the SOCKS5 Bytestream
     * @param target the target JID of the SOCKS5 Bytestream
     */
    public Socks5ClientForInitiator(StreamHost streamHost, String digest, Connection connection,
                    String sessionID, String target) {
        super(streamHost, digest);
        this.connection = connection;
        this.sessionID = sessionID;
        this.target = target;
    }

    public Socket getSocket(int timeout) throws IOException, XMPPException, InterruptedException,
                    TimeoutException {
        Socket socket = null;

        // check if stream host is the local SOCKS5 proxy
        if (this.streamHost.getJID().equals(this.connection.getUser())) {
            Socks5Proxy socks5Server = Socks5Proxy.getSocks5Proxy();
            socket = socks5Server.getSocket(this.digest);
            if (socket == null) {
                throw new XMPPException("target is not connected to SOCKS5 proxy");
            }
        }
        else {
            socket = super.getSocket(timeout);

            try {
                activate();
            }
            catch (XMPPException e) {
                socket.close();
                throw new XMPPException("activating SOCKS5 Bytestream failed", e);
            }

        }

        return socket;
    }

    /**
     * Activates the SOCKS5 Bytestream by sending a XMPP SOCKS5 Bytestream activation packet to the
     * SOCKS5 proxy.
     */
    private void activate() throws XMPPException {
        Bytestream activate = createStreamHostActivation();
        // if activation fails #getReply throws an exception
        SyncPacketSend.getReply(this.connection, activate);
    }

    /**
     * Returns a SOCKS5 Bytestream activation packet.
     * 
     * @return SOCKS5 Bytestream activation packet
     */
    private Bytestream createStreamHostActivation() {
        Bytestream activate = new Bytestream(this.sessionID);
        activate.setMode(null);
        activate.setType(IQ.Type.SET);
        activate.setTo(this.streamHost.getJID());

        activate.setToActivate(this.target);

        return activate;
    }

}
