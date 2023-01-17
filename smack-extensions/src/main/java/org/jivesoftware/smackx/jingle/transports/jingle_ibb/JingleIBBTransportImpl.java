/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports.jingle_ibb;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;

/**
 * Jingle InBandBytestream Transport component.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleIBBTransportImpl extends JingleTransport<JingleIBBTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleIBBTransportImpl.class.getName());

    public static final Short DEFAULT_BLOCK_SIZE = 4096;
    public static final Short MAX_BLOCKSIZE = 8192;

    private final String streamId;
    private Short blockSize;

    public JingleIBBTransportImpl() {
        this(DEFAULT_BLOCK_SIZE, StringUtils.randomString(10));
    }

    public JingleIBBTransportImpl(Short blockSize, String streamId) {
        this.streamId = streamId;
        this.blockSize = blockSize;
    }

    public Short getBlockSize() {
        return blockSize;
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public JingleIBBTransport getElement() {
        return new JingleIBBTransport(blockSize, streamId);
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE_V1;
    }

    @Override
    public void handleSessionAccept(JingleContentTransport transportElement, XMPPConnection connection) {
        JingleIBBTransport element = (JingleIBBTransport) transportElement;
        blockSize = blockSize < element.getBlockSize() ? blockSize : element.getBlockSize();
    }

    @Override
    public void establishIncomingBytestreamSession(final XMPPConnection connection, final JingleTransportCallback callback, final JingleSessionImpl session) {
        final InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        LOGGER.log(Level.INFO, "Listen for incoming IBB transports from " + session.getRemote() + ":" + getStreamId());
        InBandBytestreamListener bytestreamListener = new InBandBytestreamListener() {
            @Override
            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                LOGGER.log(Level.INFO, "Incoming IBB stream: " + request.getFrom().asFullJidIfPossible() + ":" + request.getSessionID());
                if (request.getFrom().asFullJidIfPossible().equals(session.getRemote())
                        && request.getSessionID().equals(getStreamId())) {

                    inBandBytestreamManager.removeIncomingBytestreamListener(this);
                    InBandBytestreamSession ibbSession;
                    try {
                        ibbSession = request.accept();

                        // Must close both input and output streams to trigger sending of IBB <close/> element as defined in XEP-0047
                        ibbSession.setCloseBothStreamsEnabled(true);
                        mBytestreamSession = ibbSession;
                        callback.onTransportReady(mBytestreamSession);
                    } catch (InterruptedException | SmackException e) {
                        callback.onTransportFailed(e);
                    }
                }
            }
        };

        InBandBytestreamManager.getByteStreamManager(connection)
                .addIncomingBytestreamListener(bytestreamListener);
    }

    @Override
    public void establishOutgoingBytestreamSession(final XMPPConnection connection, final JingleTransportCallback callback, final JingleSessionImpl session) {
        InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        inBandBytestreamManager.setDefaultBlockSize(blockSize);
        try {
            InBandBytestreamSession ibbSession = inBandBytestreamManager.establishSession(session.getRemote(), getStreamId());

            // Must close both input and output streams to trigger sending of IBB <close/> element as defined in XEP-0047
            ibbSession.setCloseBothStreamsEnabled(true);
            mBytestreamSession = ibbSession;
            callback.onTransportReady(mBytestreamSession);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            callback.onTransportFailed(e);
        }
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
    public IQ handleTransportInfo(JingleContentTransportInfo info, Jingle wrapping) {
        return IQ.createResultIQ(wrapping);
    }
}
