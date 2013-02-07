/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
 *
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

import java.io.InputStream;
import java.io.OutputStream;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * The In-Band Bytestream file transfer method, or IBB for short, transfers the
 * file over the same XML Stream used by XMPP. It is the fall-back mechanism in
 * case the SOCKS5 bytestream method of transferring files is not available.
 * 
 * @author Alexander Wenckus
 * @author Henning Staib
 * @see <a href="http://xmpp.org/extensions/xep-0047.html">XEP-0047: In-Band
 *      Bytestreams (IBB)</a>
 */
public class IBBTransferNegotiator extends StreamNegotiator {

    private Connection connection;

    private InBandBytestreamManager manager;

    /**
     * The default constructor for the In-Band Bytestream Negotiator.
     * 
     * @param connection The connection which this negotiator works on.
     */
    protected IBBTransferNegotiator(Connection connection) {
        this.connection = connection;
        this.manager = InBandBytestreamManager.getByteStreamManager(connection);
    }

    public OutputStream createOutgoingStream(String streamID, String initiator,
                    String target) throws XMPPException {
        InBandBytestreamSession session = this.manager.establishSession(target, streamID);
        session.setCloseBothStreamsEnabled(true);
        return session.getOutputStream();
    }

    public InputStream createIncomingStream(StreamInitiation initiation)
                    throws XMPPException {
        /*
         * In-Band Bytestream initiation listener must ignore next in-band
         * bytestream request with given session ID
         */
        this.manager.ignoreBytestreamRequestOnce(initiation.getSessionID());

        Packet streamInitiation = initiateIncomingStream(this.connection, initiation);
        return negotiateIncomingStream(streamInitiation);
    }

    public PacketFilter getInitiationPacketFilter(String from, String streamID) {
        /*
         * this method is always called prior to #negotiateIncomingStream() so
         * the In-Band Bytestream initiation listener must ignore the next
         * In-Band Bytestream request with the given session ID
         */
        this.manager.ignoreBytestreamRequestOnce(streamID);

        return new AndFilter(new FromContainsFilter(from), new IBBOpenSidFilter(streamID));
    }

    public String[] getNamespaces() {
        return new String[] { InBandBytestreamManager.NAMESPACE };
    }

    InputStream negotiateIncomingStream(Packet streamInitiation) throws XMPPException {
        // build In-Band Bytestream request
        InBandBytestreamRequest request = new ByteStreamRequest(this.manager,
                        (Open) streamInitiation);

        // always accept the request
        InBandBytestreamSession session = request.accept();
        session.setCloseBothStreamsEnabled(true);
        return session.getInputStream();
    }

    public void cleanup() {
    }

    /**
     * This PacketFilter accepts an incoming In-Band Bytestream open request
     * with a specified session ID.
     */
    private static class IBBOpenSidFilter extends PacketTypeFilter {

        private String sessionID;

        public IBBOpenSidFilter(String sessionID) {
            super(Open.class);
            if (sessionID == null) {
                throw new IllegalArgumentException("StreamID cannot be null");
            }
            this.sessionID = sessionID;
        }

        public boolean accept(Packet packet) {
            if (super.accept(packet)) {
                Open bytestream = (Open) packet;

                // packet must by of type SET and contains the given session ID
                return this.sessionID.equals(bytestream.getSessionID())
                                && IQ.Type.SET.equals(bytestream.getType());
            }
            return false;
        }
    }

    /**
     * Derive from InBandBytestreamRequest to access protected constructor.
     */
    private static class ByteStreamRequest extends InBandBytestreamRequest {

        private ByteStreamRequest(InBandBytestreamManager manager, Open byteStreamRequest) {
            super(manager, byteStreamRequest);
        }

    }

}
