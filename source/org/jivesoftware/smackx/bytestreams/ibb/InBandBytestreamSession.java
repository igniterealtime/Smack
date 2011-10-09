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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Close;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

/**
 * InBandBytestreamSession class represents an In-Band Bytestream session.
 * <p>
 * In-band bytestreams are bidirectional and this session encapsulates the streams for both
 * directions.
 * <p>
 * Note that closing the In-Band Bytestream session will close both streams. If both streams are
 * closed individually the session will be closed automatically once the second stream is closed.
 * Use the {@link #setCloseBothStreamsEnabled(boolean)} method if both streams should be closed
 * automatically if one of them is closed.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamSession implements BytestreamSession {

    /* XMPP connection */
    private final Connection connection;

    /* the In-Band Bytestream open request for this session */
    private final Open byteStreamRequest;

    /*
     * the input stream for this session (either IQIBBInputStream or MessageIBBInputStream)
     */
    private IBBInputStream inputStream;

    /*
     * the output stream for this session (either IQIBBOutputStream or MessageIBBOutputStream)
     */
    private IBBOutputStream outputStream;

    /* JID of the remote peer */
    private String remoteJID;

    /* flag to close both streams if one of them is closed */
    private boolean closeBothStreamsEnabled = false;

    /* flag to indicate if session is closed */
    private boolean isClosed = false;

    /**
     * Constructor.
     * 
     * @param connection the XMPP connection
     * @param byteStreamRequest the In-Band Bytestream open request for this session
     * @param remoteJID JID of the remote peer
     */
    protected InBandBytestreamSession(Connection connection, Open byteStreamRequest,
                    String remoteJID) {
        this.connection = connection;
        this.byteStreamRequest = byteStreamRequest;
        this.remoteJID = remoteJID;

        // initialize streams dependent to the uses stanza type
        switch (byteStreamRequest.getStanza()) {
        case IQ:
            this.inputStream = new IQIBBInputStream();
            this.outputStream = new IQIBBOutputStream();
            break;
        case MESSAGE:
            this.inputStream = new MessageIBBInputStream();
            this.outputStream = new MessageIBBOutputStream();
            break;
        }

    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public int getReadTimeout() {
        return this.inputStream.readTimeout;
    }

    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout must be >= 0");
        }
        this.inputStream.readTimeout = timeout;
    }

    /**
     * Returns whether both streams should be closed automatically if one of the streams is closed.
     * Default is <code>false</code>.
     * 
     * @return <code>true</code> if both streams will be closed if one of the streams is closed,
     *         <code>false</code> if both streams can be closed independently.
     */
    public boolean isCloseBothStreamsEnabled() {
        return closeBothStreamsEnabled;
    }

    /**
     * Sets whether both streams should be closed automatically if one of the streams is closed.
     * Default is <code>false</code>.
     * 
     * @param closeBothStreamsEnabled <code>true</code> if both streams should be closed if one of
     *        the streams is closed, <code>false</code> if both streams should be closed
     *        independently
     */
    public void setCloseBothStreamsEnabled(boolean closeBothStreamsEnabled) {
        this.closeBothStreamsEnabled = closeBothStreamsEnabled;
    }

    public void close() throws IOException {
        closeByLocal(true); // close input stream
        closeByLocal(false); // close output stream
    }

    /**
     * This method is invoked if a request to close the In-Band Bytestream has been received.
     * 
     * @param closeRequest the close request from the remote peer
     */
    protected void closeByPeer(Close closeRequest) {

        /*
         * close streams without flushing them, because stream is already considered closed on the
         * remote peers side
         */
        this.inputStream.closeInternal();
        this.inputStream.cleanup();
        this.outputStream.closeInternal(false);

        // acknowledge close request
        IQ confirmClose = IQ.createResultIQ(closeRequest);
        this.connection.sendPacket(confirmClose);

    }

    /**
     * This method is invoked if one of the streams has been closed locally, if an error occurred
     * locally or if the whole session should be closed.
     * 
     * @throws IOException if an error occurs while sending the close request
     */
    protected synchronized void closeByLocal(boolean in) throws IOException {
        if (this.isClosed) {
            return;
        }

        if (this.closeBothStreamsEnabled) {
            this.inputStream.closeInternal();
            this.outputStream.closeInternal(true);
        }
        else {
            if (in) {
                this.inputStream.closeInternal();
            }
            else {
                // close stream but try to send any data left
                this.outputStream.closeInternal(true);
            }
        }

        if (this.inputStream.isClosed && this.outputStream.isClosed) {
            this.isClosed = true;

            // send close request
            Close close = new Close(this.byteStreamRequest.getSessionID());
            close.setTo(this.remoteJID);
            try {
                SyncPacketSend.getReply(this.connection, close);
            }
            catch (XMPPException e) {
                throw new IOException("Error while closing stream: " + e.getMessage());
            }

            this.inputStream.cleanup();

            // remove session from manager
            InBandBytestreamManager.getByteStreamManager(this.connection).getSessions().remove(this);
        }

    }

    /**
     * IBBInputStream class is the base implementation of an In-Band Bytestream input stream.
     * Subclasses of this input stream must provide a packet listener along with a packet filter to
     * collect the In-Band Bytestream data packets.
     */
    private abstract class IBBInputStream extends InputStream {

        /* the data packet listener to fill the data queue */
        private final PacketListener dataPacketListener;

        /* queue containing received In-Band Bytestream data packets */
        protected final BlockingQueue<DataPacketExtension> dataQueue = new LinkedBlockingQueue<DataPacketExtension>();

        /* buffer containing the data from one data packet */
        private byte[] buffer;

        /* pointer to the next byte to read from buffer */
        private int bufferPointer = -1;

        /* data packet sequence (range from 0 to 65535) */
        private long seq = -1;

        /* flag to indicate if input stream is closed */
        private boolean isClosed = false;

        /* flag to indicate if close method was invoked */
        private boolean closeInvoked = false;

        /* timeout for read operations */
        private int readTimeout = 0;

        /**
         * Constructor.
         */
        public IBBInputStream() {
            // add data packet listener to connection
            this.dataPacketListener = getDataPacketListener();
            connection.addPacketListener(this.dataPacketListener, getDataPacketFilter());
        }

        /**
         * Returns the packet listener that processes In-Band Bytestream data packets.
         * 
         * @return the data packet listener
         */
        protected abstract PacketListener getDataPacketListener();

        /**
         * Returns the packet filter that accepts In-Band Bytestream data packets.
         * 
         * @return the data packet filter
         */
        protected abstract PacketFilter getDataPacketFilter();

        public synchronized int read() throws IOException {
            checkClosed();

            // if nothing read yet or whole buffer has been read fill buffer
            if (bufferPointer == -1 || bufferPointer >= buffer.length) {
                // if no data available and stream was closed return -1
                if (!loadBuffer()) {
                    return -1;
                }
            }

            // return byte and increment buffer pointer
            return (int) buffer[bufferPointer++];
        }

        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
                            || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return 0;
            }

            checkClosed();

            // if nothing read yet or whole buffer has been read fill buffer
            if (bufferPointer == -1 || bufferPointer >= buffer.length) {
                // if no data available and stream was closed return -1
                if (!loadBuffer()) {
                    return -1;
                }
            }

            // if more bytes wanted than available return all available
            int bytesAvailable = buffer.length - bufferPointer;
            if (len > bytesAvailable) {
                len = bytesAvailable;
            }

            System.arraycopy(buffer, bufferPointer, b, off, len);
            bufferPointer += len;
            return len;
        }

        public synchronized int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /**
         * This method blocks until a data packet is received, the stream is closed or the current
         * thread is interrupted.
         * 
         * @return <code>true</code> if data was received, otherwise <code>false</code>
         * @throws IOException if data packets are out of sequence
         */
        private synchronized boolean loadBuffer() throws IOException {

            // wait until data is available or stream is closed
            DataPacketExtension data = null;
            try {
                if (this.readTimeout == 0) {
                    while (data == null) {
                        if (isClosed && this.dataQueue.isEmpty()) {
                            return false;
                        }
                        data = this.dataQueue.poll(1000, TimeUnit.MILLISECONDS);
                    }
                }
                else {
                    data = this.dataQueue.poll(this.readTimeout, TimeUnit.MILLISECONDS);
                    if (data == null) {
                        throw new SocketTimeoutException();
                    }
                }
            }
            catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                return false;
            }

            // handle sequence overflow
            if (this.seq == 65535) {
                this.seq = -1;
            }

            // check if data packets sequence is successor of last seen sequence
            long seq = data.getSeq();
            if (seq - 1 != this.seq) {
                // packets out of order; close stream/session
                InBandBytestreamSession.this.close();
                throw new IOException("Packets out of sequence");
            }
            else {
                this.seq = seq;
            }

            // set buffer to decoded data
            buffer = data.getDecodedData();
            bufferPointer = 0;
            return true;
        }

        /**
         * Checks if this stream is closed and throws an IOException if necessary
         * 
         * @throws IOException if stream is closed and no data should be read anymore
         */
        private void checkClosed() throws IOException {
            /* throw no exception if there is data available, but not if close method was invoked */
            if ((isClosed && this.dataQueue.isEmpty()) || closeInvoked) {
                // clear data queue in case additional data was received after stream was closed
                this.dataQueue.clear();
                throw new IOException("Stream is closed");
            }
        }

        public boolean markSupported() {
            return false;
        }

        public void close() throws IOException {
            if (isClosed) {
                return;
            }

            this.closeInvoked = true;

            InBandBytestreamSession.this.closeByLocal(true);
        }

        /**
         * This method sets the close flag and removes the data packet listener.
         */
        private void closeInternal() {
            if (isClosed) {
                return;
            }
            isClosed = true;
        }

        /**
         * Invoked if the session is closed.
         */
        private void cleanup() {
            connection.removePacketListener(this.dataPacketListener);
        }

    }

    /**
     * IQIBBInputStream class implements IBBInputStream to be used with IQ stanzas encapsulating the
     * data packets.
     */
    private class IQIBBInputStream extends IBBInputStream {

        protected PacketListener getDataPacketListener() {
            return new PacketListener() {

                private long lastSequence = -1;

                public void processPacket(Packet packet) {
                    // get data packet extension
                    DataPacketExtension data = (DataPacketExtension) packet.getExtension(
                                    DataPacketExtension.ELEMENT_NAME,
                                    InBandBytestreamManager.NAMESPACE);

                    /*
                     * check if sequence was not used already (see XEP-0047 Section 2.2)
                     */
                    if (data.getSeq() <= this.lastSequence) {
                        IQ unexpectedRequest = IQ.createErrorResponse((IQ) packet, new XMPPError(
                                        XMPPError.Condition.unexpected_request));
                        connection.sendPacket(unexpectedRequest);
                        return;

                    }

                    // check if encoded data is valid (see XEP-0047 Section 2.2)
                    if (data.getDecodedData() == null) {
                        // data is invalid; respond with bad-request error
                        IQ badRequest = IQ.createErrorResponse((IQ) packet, new XMPPError(
                                        XMPPError.Condition.bad_request));
                        connection.sendPacket(badRequest);
                        return;
                    }

                    // data is valid; add to data queue
                    dataQueue.offer(data);

                    // confirm IQ
                    IQ confirmData = IQ.createResultIQ((IQ) packet);
                    connection.sendPacket(confirmData);

                    // set last seen sequence
                    this.lastSequence = data.getSeq();
                    if (this.lastSequence == 65535) {
                        this.lastSequence = -1;
                    }

                }

            };
        }

        protected PacketFilter getDataPacketFilter() {
            /*
             * filter all IQ stanzas having type 'SET' (represented by Data class), containing a
             * data packet extension, matching session ID and recipient
             */
            return new AndFilter(new PacketTypeFilter(Data.class), new IBBDataPacketFilter());
        }

    }

    /**
     * MessageIBBInputStream class implements IBBInputStream to be used with message stanzas
     * encapsulating the data packets.
     */
    private class MessageIBBInputStream extends IBBInputStream {

        protected PacketListener getDataPacketListener() {
            return new PacketListener() {

                public void processPacket(Packet packet) {
                    // get data packet extension
                    DataPacketExtension data = (DataPacketExtension) packet.getExtension(
                                    DataPacketExtension.ELEMENT_NAME,
                                    InBandBytestreamManager.NAMESPACE);

                    // check if encoded data is valid
                    if (data.getDecodedData() == null) {
                        /*
                         * TODO once a majority of XMPP server implementation support XEP-0079
                         * Advanced Message Processing the invalid message could be answered with an
                         * appropriate error. For now we just ignore the packet. Subsequent packets
                         * with an increased sequence will cause the input stream to close the
                         * stream/session.
                         */
                        return;
                    }

                    // data is valid; add to data queue
                    dataQueue.offer(data);

                    // TODO confirm packet once XMPP servers support XEP-0079
                }

            };
        }

        @Override
        protected PacketFilter getDataPacketFilter() {
            /*
             * filter all message stanzas containing a data packet extension, matching session ID
             * and recipient
             */
            return new AndFilter(new PacketTypeFilter(Message.class), new IBBDataPacketFilter());
        }

    }

    /**
     * IBBDataPacketFilter class filters all packets from the remote peer of this session,
     * containing an In-Band Bytestream data packet extension whose session ID matches this sessions
     * ID.
     */
    private class IBBDataPacketFilter implements PacketFilter {

        public boolean accept(Packet packet) {
            // sender equals remote peer
            if (!packet.getFrom().equalsIgnoreCase(remoteJID)) {
                return false;
            }

            // stanza contains data packet extension
            PacketExtension packetExtension = packet.getExtension(DataPacketExtension.ELEMENT_NAME,
                            InBandBytestreamManager.NAMESPACE);
            if (packetExtension == null || !(packetExtension instanceof DataPacketExtension)) {
                return false;
            }

            // session ID equals this session ID
            DataPacketExtension data = (DataPacketExtension) packetExtension;
            if (!data.getSessionID().equals(byteStreamRequest.getSessionID())) {
                return false;
            }

            return true;
        }

    }

    /**
     * IBBOutputStream class is the base implementation of an In-Band Bytestream output stream.
     * Subclasses of this output stream must provide a method to send data over XMPP stream.
     */
    private abstract class IBBOutputStream extends OutputStream {

        /* buffer with the size of this sessions block size */
        protected final byte[] buffer;

        /* pointer to next byte to write to buffer */
        protected int bufferPointer = 0;

        /* data packet sequence (range from 0 to 65535) */
        protected long seq = 0;

        /* flag to indicate if output stream is closed */
        protected boolean isClosed = false;

        /**
         * Constructor.
         */
        public IBBOutputStream() {
            this.buffer = new byte[(byteStreamRequest.getBlockSize()/4)*3];
        }

        /**
         * Writes the given data packet to the XMPP stream.
         * 
         * @param data the data packet
         * @throws IOException if an I/O error occurred while sending or if the stream is closed
         */
        protected abstract void writeToXML(DataPacketExtension data) throws IOException;

        public synchronized void write(int b) throws IOException {
            if (this.isClosed) {
                throw new IOException("Stream is closed");
            }

            // if buffer is full flush buffer
            if (bufferPointer >= buffer.length) {
                flushBuffer();
            }

            buffer[bufferPointer++] = (byte) b;
        }

        public synchronized void write(byte b[], int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
                            || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return;
            }

            if (this.isClosed) {
                throw new IOException("Stream is closed");
            }

            // is data to send greater than buffer size
            if (len >= buffer.length) {

                // "byte" off the first chunk to write out
                writeOut(b, off, buffer.length);

                // recursively call this method with the lesser amount
                write(b, off + buffer.length, len - buffer.length);
            }
            else {
                writeOut(b, off, len);
            }
        }

        public synchronized void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        /**
         * Fills the buffer with the given data and sends it over the XMPP stream if the buffers
         * capacity has been reached. This method is only called from this class so it is assured
         * that the amount of data to send is <= buffer capacity
         * 
         * @param b the data
         * @param off the data
         * @param len the number of bytes to write
         * @throws IOException if an I/O error occurred while sending or if the stream is closed
         */
        private synchronized void writeOut(byte b[], int off, int len) throws IOException {
            if (this.isClosed) {
                throw new IOException("Stream is closed");
            }

            // set to 0 in case the next 'if' block is not executed
            int available = 0;

            // is data to send greater that buffer space left
            if (len > buffer.length - bufferPointer) {
                // fill buffer to capacity and send it
                available = buffer.length - bufferPointer;
                System.arraycopy(b, off, buffer, bufferPointer, available);
                bufferPointer += available;
                flushBuffer();
            }

            // copy the data left to buffer
            System.arraycopy(b, off + available, buffer, bufferPointer, len - available);
            bufferPointer += len - available;
        }

        public synchronized void flush() throws IOException {
            if (this.isClosed) {
                throw new IOException("Stream is closed");
            }
            flushBuffer();
        }

        private synchronized void flushBuffer() throws IOException {

            // do nothing if no data to send available
            if (bufferPointer == 0) {
                return;
            }

            // create data packet
            String enc = StringUtils.encodeBase64(buffer, 0, bufferPointer, false);
            DataPacketExtension data = new DataPacketExtension(byteStreamRequest.getSessionID(),
                            this.seq, enc);

            // write to XMPP stream
            writeToXML(data);

            // reset buffer pointer
            bufferPointer = 0;

            // increment sequence, considering sequence overflow
            this.seq = (this.seq + 1 == 65535 ? 0 : this.seq + 1);

        }

        public void close() throws IOException {
            if (isClosed) {
                return;
            }
            InBandBytestreamSession.this.closeByLocal(false);
        }

        /**
         * Sets the close flag and optionally flushes the stream.
         * 
         * @param flush if <code>true</code> flushes the stream
         */
        protected void closeInternal(boolean flush) {
            if (this.isClosed) {
                return;
            }
            this.isClosed = true;

            try {
                if (flush) {
                    flushBuffer();
                }
            }
            catch (IOException e) {
                /*
                 * ignore, because writeToXML() will not throw an exception if stream is already
                 * closed
                 */
            }
        }

    }

    /**
     * IQIBBOutputStream class implements IBBOutputStream to be used with IQ stanzas encapsulating
     * the data packets.
     */
    private class IQIBBOutputStream extends IBBOutputStream {

        @Override
        protected synchronized void writeToXML(DataPacketExtension data) throws IOException {
            // create IQ stanza containing data packet
            IQ iq = new Data(data);
            iq.setTo(remoteJID);

            try {
                SyncPacketSend.getReply(connection, iq);
            }
            catch (XMPPException e) {
                // close session unless it is already closed
                if (!this.isClosed) {
                    InBandBytestreamSession.this.close();
                    throw new IOException("Error while sending Data: " + e.getMessage());
                }
            }

        }

    }

    /**
     * MessageIBBOutputStream class implements IBBOutputStream to be used with message stanzas
     * encapsulating the data packets.
     */
    private class MessageIBBOutputStream extends IBBOutputStream {

        @Override
        protected synchronized void writeToXML(DataPacketExtension data) {
            // create message stanza containing data packet
            Message message = new Message(remoteJID);
            message.addExtension(data);

            connection.sendPacket(message);

        }

    }

}
