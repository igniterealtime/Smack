/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.IBBExtensions;
import org.jivesoftware.smackx.packet.IBBExtensions.Open;
import org.jivesoftware.smackx.packet.StreamInitiation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The in-band bytestream file transfer method, or IBB for short, transfers the
 * file over the same XML Stream used by XMPP. It is the fall-back mechanism in
 * case the SOCKS5 bytestream method of transfering files is not available.
 *
 * @author Alexander Wenckus
 * @see <a href="http://www.jabber.org/jeps/jep-0047.html">JEP-0047: In-Band
 *      Bytestreams (IBB)</a>
 */
public class IBBTransferNegotiator extends StreamNegotiator {

    protected static final String NAMESPACE = "http://jabber.org/protocol/ibb";

    public static final int DEFAULT_BLOCK_SIZE = 4096;

    private XMPPConnection connection;

    /**
     * The default constructor for the In-Band Bystream Negotiator.
     *
     * @param connection The connection which this negotiator works on.
     */
    protected IBBTransferNegotiator(XMPPConnection connection) {
        this.connection = connection;
    }

    /*
      * (non-Javadoc)
      *
      * @see org.jivesoftware.smackx.filetransfer.StreamNegotiator#initiateDownload(org.jivesoftware.smackx.packet.StreamInitiation,
      *      java.io.File)
      */
    public InputStream initiateIncomingStream(StreamInitiation initiation)
            throws XMPPException {
        StreamInitiation response = super.createInitiationAccept(initiation,
                NAMESPACE);

        // establish collector to await response
        PacketCollector collector = connection
                .createPacketCollector(new AndFilter(new FromContainsFilter(
                        initiation.getFrom()), new IBBSidFilter(initiation.getSessionID())));
        connection.sendPacket(response);

        IBBExtensions.Open openRequest = (IBBExtensions.Open) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (openRequest == null) {
            throw new XMPPException("No response from file transfer initiator");
        }
        else if (openRequest.getType().equals(IQ.Type.ERROR)) {
            throw new XMPPException(openRequest.getError());
        }
        collector.cancel();

        PacketFilter dataFilter = new AndFilter(new PacketExtensionFilter(
                IBBExtensions.Data.ELEMENT_NAME, IBBExtensions.NAMESPACE),
                new FromMatchesFilter(initiation.getFrom()));
        PacketFilter closeFilter = new AndFilter(new PacketTypeFilter(
                IBBExtensions.Close.class), new FromMatchesFilter(initiation
                .getFrom()));

        InputStream stream = new IBBInputStream(openRequest.getSessionID(),
                dataFilter, closeFilter);

        initInBandTransfer(openRequest);

        return stream;
    }

    /**
     * Creates and sends the response for the open request.
     *
     * @param openRequest The open request recieved from the peer.
     */
    private void initInBandTransfer(final Open openRequest) {
        connection.sendPacket(FileTransferNegotiator.createIQ(openRequest
                .getPacketID(), openRequest.getFrom(), openRequest.getTo(),
                IQ.Type.RESULT));
    }

    public OutputStream initiateOutgoingStream(String streamID, String initiator,
            String target) throws XMPPException {
        Open openIQ = new Open(streamID, DEFAULT_BLOCK_SIZE);
        openIQ.setTo(target);
        openIQ.setType(IQ.Type.SET);

        // wait for the result from the peer
        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(openIQ.getPacketID()));
        connection.sendPacket(openIQ);

        IQ openResponse = (IQ) collector.nextResult();
        collector.cancel();

        if (openResponse == null) {
            throw new XMPPException("No response from peer");
        }

        IQ.Type type = openResponse.getType();
        if (!type.equals(IQ.Type.RESULT)) {
            if (type.equals(IQ.Type.ERROR)) {
                throw new XMPPException("Target returned an error",
                        openResponse.getError());
            }
            else {
                throw new XMPPException("Target returned unknown response");
            }
        }

        return new IBBOutputStream(target, streamID, DEFAULT_BLOCK_SIZE);
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    private class IBBOutputStream extends OutputStream {

        protected byte[] buffer;

        protected int count = 0;

        protected int seq = 0;

        private final Message template;

        private final int options = Base64.DONT_BREAK_LINES;

        private IQ closePacket;

        private String messageID;

        IBBOutputStream(String userID, String sid, int blockSize) {
            if (blockSize <= 0) {
                throw new IllegalArgumentException("Buffer size <= 0");
            }
            buffer = new byte[blockSize];
            template = new Message(userID);
            messageID = template.getPacketID();
            template.addExtension(new IBBExtensions.Data(sid));
            closePacket = createClosePacket(userID, sid);
        }

        private IQ createClosePacket(String userID, String sid) {
            IQ packet = new IBBExtensions.Close(sid);
            packet.setTo(userID);
            packet.setType(IQ.Type.SET);
            return packet;
        }

        public void write(int b) throws IOException {
            if (count >= buffer.length) {
                flushBuffer();
            }

            buffer[count++] = (byte) b;
        }

        public synchronized void write(byte b[], int off, int len)
                throws IOException {
            if (len >= buffer.length) {
                throw new IllegalArgumentException(
                        "byte size exceeds blocksize");
            }
            if (len > buffer.length - count) {
                flushBuffer();
            }
            System.arraycopy(b, off, buffer, count, len);
            count += len;
        }

        private void flushBuffer() {
            writeToXML(buffer, 0, count);

            count = 0;
        }

        private void writeToXML(byte[] buffer, int offset, int len) {
            template.setPacketID(messageID + "_" + seq);
            IBBExtensions.Data ext = (IBBExtensions.Data) template
                    .getExtension(IBBExtensions.Data.ELEMENT_NAME,
                            IBBExtensions.NAMESPACE);

            String enc = Base64.encodeBytes(buffer, offset, count, options);

            ext.setData(enc);
            ext.setSeq(seq);

            connection.sendPacket(template);

            seq = (seq + 1 == 65535 ? 0 : seq + 1);
        }

        public void close() throws IOException {
            connection.sendPacket(closePacket);
        }

        public void flush() throws IOException {
            flushBuffer();
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

    }

    private class IBBInputStream extends InputStream implements PacketListener {

        private String streamID;

        private PacketCollector dataCollector;

        private byte[] buffer;

        private int bufferPointer;

        private int seq = -1;

        private boolean isDone;

        private boolean isEOF;

        private boolean isClosed;

        private IQ closeConfirmation;

        private IBBInputStream(String streamID, PacketFilter dataFilter,
                PacketFilter closeFilter) {
            this.streamID = streamID;
            this.dataCollector = connection.createPacketCollector(dataFilter);
            connection.addPacketListener(this, closeFilter);
            this.bufferPointer = -1;
        }

        public synchronized int read() throws IOException {
            if (isEOF || isClosed) {
                return -1;
            }
            if (bufferPointer == -1 || bufferPointer >= buffer.length) {
                loadBufferWait();
            }

            return (int) buffer[bufferPointer++];
        }

        public synchronized int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public synchronized int read(byte[] b, int off, int len)
                throws IOException {
            if (isEOF || isClosed) {
                return -1;
            }
            if (bufferPointer == -1 || bufferPointer >= buffer.length) {
                if (!loadBufferWait()) {
                    isEOF = true;
                    return -1;
                }
            }

            if (len - off > buffer.length - bufferPointer) {
                len = buffer.length - bufferPointer;
            }

            System.arraycopy(buffer, bufferPointer, b, off, len);
            bufferPointer += len;
            return len;
        }

        private boolean loadBufferWait() throws IOException {
            IBBExtensions.Data data;
            do {
                Message mess = null;
                while (mess == null) {
                    if (isDone) {
                        mess = (Message) dataCollector.pollResult();
                        if (mess == null) {
                            return false;
                        }
                    }
                    else {
                        mess = (Message) dataCollector.nextResult(1000);
                    }
                }
                data = (IBBExtensions.Data) mess.getExtension(
                        IBBExtensions.Data.ELEMENT_NAME,
                        IBBExtensions.NAMESPACE);
            }
            while (!data.getSessionID().equals(streamID));
            checkSequence((int) data.getSeq());
            buffer = Base64.decode(data.getData());
            bufferPointer = 0;
            return true;
        }

        private void checkSequence(int seq) throws IOException {
            if (this.seq == 65535) {
                this.seq = -1;
            }
            if (seq - 1 != this.seq) {
                cancelTransfer();
                throw new IOException("Packets out of sequence");
            }
            else {
                this.seq = seq;
            }
        }

        private void cancelTransfer() {
            cleanup();

            sendCancelMessage();
        }

        private void cleanup() {
            dataCollector.cancel();
            connection.removePacketListener(this);
        }

        private void sendCancelMessage() {
        }

        public boolean markSupported() {
            return false;
        }

        public void processPacket(Packet packet) {
            IBBExtensions.Close close = (IBBExtensions.Close) packet;
            if (close.getSessionID().equals(streamID)) {
                isDone = true;
                closeConfirmation = FileTransferNegotiator.createIQ(packet
                        .getPacketID(), packet.getFrom(), packet.getTo(),
                        IQ.Type.RESULT);
            }
        }

        public synchronized void close() throws IOException {
            if (isClosed) {
                return;
            }
            cleanup();

            if (isEOF) {
                sendCloseConfirmation();
            }
            else {
                sendCancelMessage();
            }
            isClosed = true;
        }

        private void sendCloseConfirmation() {
            connection.sendPacket(closeConfirmation);
        }
    }

    private static class IBBSidFilter implements PacketFilter {

        private String sessionID;

        public IBBSidFilter(String sessionID) {
            if (sessionID == null) {
                throw new IllegalArgumentException("StreamID cannot be null");
            }
            this.sessionID = sessionID;
        }

        public boolean accept(Packet packet) {
            if (!IBBExtensions.Open.class.isInstance(packet)) {
                return false;
            }
            IBBExtensions.Open open = (IBBExtensions.Open) packet;
            String sessionID = open.getSessionID();

            return (sessionID != null && sessionID.equals(this.sessionID));
        }
    }

}
