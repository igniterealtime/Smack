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
package org.jivesoftware.smackx.jingle.transports.jingle_ibb;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;

public class JingleIBBTransportSession extends JingleTransportSession<JingleIBBTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleIBBTransportSession.class.getName());

    private final JingleIBBTransportManager transportManager;

    public JingleIBBTransportSession(JingleSession session) {
        super(session);
        transportManager = JingleIBBTransportManager.getInstanceFor(session.getConnection());
    }

    @Override
    public JingleIBBTransport createTransport() {

        if (theirProposal == null) {
            return new JingleIBBTransport();
        } else {
            return new JingleIBBTransport(theirProposal.getBlockSize(), theirProposal.getSessionId());
        }

    }

    @Override
    public void setTheirProposal(JingleContentTransport transport) {
        theirProposal = (JingleIBBTransport) transport;
    }

    @Override
    public void initiateOutgoingSession(JingleTransportInitiationCallback callback) {
        LOGGER.log(Level.INFO, "Initiate Jingle InBandBytestream session.");

        BytestreamSession session;
        try {
            session = InBandBytestreamManager.getByteStreamManager(jingleSession.getConnection())
                    .establishSession(jingleSession.getRemote(), theirProposal.getSessionId());
            callback.onSessionInitiated(session);
        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            callback.onException(e);
        }
    }

    @Override
    public void initiateIncomingSession(final JingleTransportInitiationCallback callback) {
        LOGGER.log(Level.INFO, "Await Jingle InBandBytestream session.");

        InBandBytestreamManager.getByteStreamManager(jingleSession.getConnection()).addIncomingBytestreamListener(new BytestreamListener() {
            @Override
            public void incomingBytestreamRequest(BytestreamRequest request) {
                if (request.getFrom().asFullJidIfPossible().equals(jingleSession.getRemote())
                        && request.getSessionID().equals(theirProposal.getSessionId())) {
                    BytestreamSession session;

                    try {
                        session = request.accept();
                    } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                        callback.onException(e);
                        return;
                    }
                    callback.onSessionInitiated(session);
                }
            }
        });
    }

    @Override
    public String getNamespace() {
        return transportManager.getNamespace();
    }

    @Override
    public IQ handleTransportInfo(Jingle transportInfo) {
        return IQ.createResultIQ(transportInfo);
        // TODO
    }

    @Override
    public JingleTransportManager<JingleIBBTransport> transportManager() {
        return JingleIBBTransportManager.getInstanceFor(jingleSession.getConnection());
    }
}
