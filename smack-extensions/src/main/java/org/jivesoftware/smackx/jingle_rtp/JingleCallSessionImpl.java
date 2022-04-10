/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jxmpp.jid.FullJid;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that represents a Jingle session.
 *
 * @author Eng Chong Meng
 */
public class JingleCallSessionImpl extends JingleSession {
    private static final Logger LOGGER = Logger.getLogger(JingleCallSessionImpl.class.getName());

    /**
     * The <code>BasicTelephony</code> instance which has been used to create calls
     */
    private final BasicTelephony mBasicTelephony;

    private final XMPPConnection mConnection;

    private final JingleManager mManager;

    private final JingleUtil jutil;

    /**
     * Create a JingleSessionHandler to start a session-initiate for making call.
     *
     * @param connection XMPPConnection
     * @param recipient The remote file-recipient
     * @param sid Jingle session Id
     * @param basicTelephony a reference of the Basic Telephony implementation
     */
    public JingleCallSessionImpl(XMPPConnection connection, FullJid recipient, String sid, BasicTelephony basicTelephony) {
        this(connection, connection.getUser(), recipient, Role.initiator, sid, null, basicTelephony);
    }

    /**
     * Create a JingleSessionHandler for the newly received jingleSI (session-initiate).
     * Attempt to use initiator (optional) as first priority, fallback to use IQ from otherwise.
     *
     * @param connection XMPPConnection
     * @param jingleSI The received JingleIQ for session-initiate
     * @param basicTelephony a reference of the Basic Telephony implementation
     */
    public JingleCallSessionImpl(XMPPConnection connection, FullJid initiator, Jingle jingleSI, BasicTelephony basicTelephony) {
        this(connection, initiator, connection.getUser(), Role.responder, jingleSI.getSid(), jingleSI.getContents(), basicTelephony);
    }

    /**
     * Construct for the JingleFileSessionImpl for both session-initiate or session-accept.
     *
     * @param connection XMPPConnection
     * @param initiator JingleSI initiator
     * @param responder JingleSI responder
     * @param role Acting role for the this session-initial
     * @param sessionId Jingle session Id
     * @param contents Jingle contents
     * @param basicTelephony a reference of the Basic Telephony implementation
     */
    public JingleCallSessionImpl(XMPPConnection connection, FullJid initiator, FullJid responder, Role role,
            String sessionId, List<JingleContent> contents, BasicTelephony basicTelephony) {
        super(initiator, responder, role, sessionId, contents);

        mBasicTelephony = basicTelephony;
        mConnection = connection;
        jutil = new JingleUtil(mConnection);

        LOGGER.info("Register JingleSession Handler: " + remote + " " + sid);
        mManager = JingleManager.getInstanceFor(connection);
        mManager.registerJingleSessionHandler(remote, sid, this);
    }

    public static void got() {

    }

    /**
     * Pass on to BasicTelephony implementation to handle all call session request
     *
     * @param request Jingle request
     * @return IQResult
     */
    @Override
    public IQ handleJingleSessionRequest(Jingle request) {
        Async.go(() -> mBasicTelephony.handleJingleSession(request, this));
        return IQ.createResultIQ(request);
    }

    /**
     * Send session-terminate and unregister the associated Jingle Session Handler.
     * Any stray remote jingle stanzas received after that will be responded with i.e.
     * createErrorUnknownSession(jingle)
     *
     * @param reason reason for session-terminate
     * @param reasonText reason text string for session-terminate
     */
    public void terminateSession(JingleReason.Reason reason, String reasonText) {
        try {
            JingleReason jingleReason = new JingleReason(reason, reasonText, null);
            mConnection.createStanzaCollectorAndSend(jutil.createSessionTerminate(remote, sid, jingleReason));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
        }
        unregisterJingleSessionHandler();
    }

    public void unregisterJingleSessionHandler() {
        mManager.unregisterJingleSessionHandler(remote, sid, this);
        LOGGER.info("Unregister JingleSession Handler: " + remote + " " + sid);
    }

    @Override
    public XMPPConnection getConnection() {
        return mConnection;
    }

    @Override
    public void onTransportMethodFailed(String namespace) {
    }
}
