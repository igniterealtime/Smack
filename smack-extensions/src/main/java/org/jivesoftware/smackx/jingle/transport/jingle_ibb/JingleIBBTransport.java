/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;

/**
 * Jingle InBandBytestream Transport component.
 */
public class JingleIBBTransport extends JingleTransport<JingleIBBTransportElement> {
    private static final Logger LOGGER = Logger.getLogger(JingleIBBTransport.class.getName());

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    public static final Short DEFAULT_BLOCK_SIZE = 4096;
    public static final Short MAX_BLOCKSIZE = 8192;

    private final String streamId;
    private Short blockSize;

    public JingleIBBTransport(String streamId, Short blockSize) {
        this.streamId = streamId;
        this.blockSize = blockSize;
    }

    public JingleIBBTransport() {
        this(StringUtils.randomString(10), DEFAULT_BLOCK_SIZE);
    }

    public Short getBlockSize() {
        return blockSize;
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public JingleIBBTransportElement getElement() {
        return new JingleIBBTransportElement(streamId, blockSize);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void handleSessionAccept(JingleContentTransportElement transportElement, XMPPConnection connection) {
        JingleIBBTransportElement element = (JingleIBBTransportElement) transportElement;
        blockSize = (blockSize < element.getBlockSize() ? blockSize : element.getBlockSize());
    }

    @Override
    public void establishIncomingBytestreamSession(final XMPPConnection connection, final JingleTransportCallback callback, final JingleSession session) {
        final InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        LOGGER.log(Level.INFO, "Listen for incoming IBB transports from " + session.getPeer() + ":" + getStreamId());
        InBandBytestreamListener bytestreamListener = new InBandBytestreamListener() {
            @Override
            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                LOGGER.log(Level.INFO, "Incoming IBB stream: " + request.getFrom().asFullJidIfPossible() + ":" + request.getSessionID());
                if (request.getFrom().asFullJidIfPossible().equals(session.getPeer())
                        && request.getSessionID().equals(getStreamId())) {

                    inBandBytestreamManager.removeIncomingBytestreamListener(this);

                    BytestreamSession bytestreamSession;
                    try {
                        bytestreamSession = request.accept();
                    } catch (InterruptedException | SmackException e) {
                        callback.onTransportFailed(e);
                        return;
                    }

                    JingleIBBTransport.this.bytestreamSession = bytestreamSession;
                    callback.onTransportReady(JingleIBBTransport.this.bytestreamSession);
                }
            }
        };

        InBandBytestreamManager.getByteStreamManager(connection)
                .addIncomingBytestreamListener(bytestreamListener);
    }

    @Override
    public void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, final JingleSession session) {
        InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        inBandBytestreamManager.setDefaultBlockSize(blockSize);
        try {
            JingleIBBTransport.this.bytestreamSession = inBandBytestreamManager.establishSession(session.getPeer(), getStreamId());
            callback.onTransportReady(this.bytestreamSession);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            callback.onTransportFailed(e);
        }
    }

    @Override
    public void cleanup() {
        // Nothing to do.
    }

    @Override
    public void addOurCandidate(JingleTransportCandidate<?> candidate) {
        // Sorry, we don't want any candidates.
    }

    @Override
    public void prepare(XMPPConnection connection) {
        // Nothing to do.
    }

    @Override
    public IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping) {
        return IQ.createResultIQ(wrapping);
    }
}
