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
package org.jivesoftware.smackx.filetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * Negotiates a SOCKS5 Bytestream to be used for file transfers. The implementation is based on the
 * {@link Socks5BytestreamManager} and the {@link Socks5BytestreamRequest}.
 * 
 * @author Henning Staib
 * @see <a href="http://xmpp.org/extensions/xep-0065.html">XEP-0065: SOCKS5 Bytestreams</a>
 */
public class Socks5TransferNegotiator extends StreamNegotiator {

    private Connection connection;

    private Socks5BytestreamManager manager;

    Socks5TransferNegotiator(Connection connection) {
        this.connection = connection;
        this.manager = Socks5BytestreamManager.getBytestreamManager(this.connection);
    }

    @Override
    public OutputStream createOutgoingStream(String streamID, String initiator, String target)
                    throws XMPPException {
        try {
            return this.manager.establishSession(target, streamID).getOutputStream();
        }
        catch (IOException e) {
            throw new XMPPException("error establishing SOCKS5 Bytestream", e);
        }
        catch (InterruptedException e) {
            throw new XMPPException("error establishing SOCKS5 Bytestream", e);
        }
    }

    @Override
    public InputStream createIncomingStream(StreamInitiation initiation) throws XMPPException,
                    InterruptedException {
        /*
         * SOCKS5 initiation listener must ignore next SOCKS5 Bytestream request with given session
         * ID
         */
        this.manager.ignoreBytestreamRequestOnce(initiation.getSessionID());

        Packet streamInitiation = initiateIncomingStream(this.connection, initiation);
        return negotiateIncomingStream(streamInitiation);
    }

    @Override
    public PacketFilter getInitiationPacketFilter(final String from, String streamID) {
        /*
         * this method is always called prior to #negotiateIncomingStream() so the SOCKS5
         * InitiationListener must ignore the next SOCKS5 Bytestream request with the given session
         * ID
         */
        this.manager.ignoreBytestreamRequestOnce(streamID);

        return new AndFilter(new FromMatchesFilter(from), new BytestreamSIDFilter(streamID));
    }

    @Override
    public String[] getNamespaces() {
        return new String[] { Socks5BytestreamManager.NAMESPACE };
    }

    @Override
    InputStream negotiateIncomingStream(Packet streamInitiation) throws XMPPException,
                    InterruptedException {
        // build SOCKS5 Bytestream request
        Socks5BytestreamRequest request = new ByteStreamRequest(this.manager,
                        (Bytestream) streamInitiation);

        // always accept the request
        Socks5BytestreamSession session = request.accept();

        // test input stream
        try {
            PushbackInputStream stream = new PushbackInputStream(session.getInputStream());
            int firstByte = stream.read();
            stream.unread(firstByte);
            return stream;
        }
        catch (IOException e) {
            throw new XMPPException("Error establishing input stream", e);
        }
    }

    @Override
    public void cleanup() {
        /* do nothing */
    }

    /**
     * This PacketFilter accepts an incoming SOCKS5 Bytestream request with a specified session ID.
     */
    private static class BytestreamSIDFilter extends PacketTypeFilter {

        private String sessionID;

        public BytestreamSIDFilter(String sessionID) {
            super(Bytestream.class);
            if (sessionID == null) {
                throw new IllegalArgumentException("StreamID cannot be null");
            }
            this.sessionID = sessionID;
        }

        @Override
        public boolean accept(Packet packet) {
            if (super.accept(packet)) {
                Bytestream bytestream = (Bytestream) packet;

                // packet must by of type SET and contains the given session ID
                return this.sessionID.equals(bytestream.getSessionID())
                                && IQ.Type.SET.equals(bytestream.getType());
            }
            return false;
        }

    }

    /**
     * Derive from Socks5BytestreamRequest to access protected constructor.
     */
    private static class ByteStreamRequest extends Socks5BytestreamRequest {

        private ByteStreamRequest(Socks5BytestreamManager manager, Bytestream byteStreamRequest) {
            super(manager, byteStreamRequest);
        }

    }

}
