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
package org.jivesoftware.smackx.jingle.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.callbacks.JingleSecurityCallback;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;

/**
 * Internal class that holds the state of a content in a modifiable form.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleContentImpl implements JingleTransportCallback, JingleSecurityCallback {
    private static final Logger LOGGER = Logger.getLogger(JingleContentImpl.class.getName());

    private final JingleContent.Creator creator;
    private final String name;
    private final String disposition;
    private JingleSessionImpl parent;
    private JingleContent.Senders senders;
    private JingleDescription<?> description;
    private JingleTransport<?> transport;
    private JingleSecurity<?> security;

    private JingleTransport<?> pendingReplacingTransport = null;

    private final Set<String> transportBlacklist = Collections.synchronizedSet(new HashSet<>());
    private final JingleUtil jutil;

    // Just for handling Unused Variable warning
    private XMPPConnection mConnection;

    public JingleContentImpl(XMPPConnection connection, JingleContent.Creator creator, JingleContent.Senders senders) {
        this(connection, null, null, null, randomName(), null, creator, senders);
    }

    public JingleContentImpl(XMPPConnection connection, JingleDescription<?> description, JingleTransport<?> transport, JingleSecurity<?> security, String name, String disposition, JingleContent.Creator creator, JingleContent.Senders senders) {
        setDescription(description);
        setTransport(transport);
        setSecurity(security);

        this.jutil = new JingleUtil(connection);
        this.name = name;
        this.disposition = disposition;
        this.creator = creator;
        this.senders = senders;
    }

    public static JingleContentImpl fromElement(XMPPConnection connection, JingleContent content) {
        JingleDescription<?> description = null;
        JingleTransport<?> transport = null;
        JingleSecurity<?> security = null;

        JingleContentDescription contentDescription = content.getDescription();
        if (contentDescription != null) {
            JingleDescriptionAdapter<?> descriptionAdapter
                    = JingleContentProviderManager.getJingleDescriptionAdapter(contentDescription.getNamespace());
            if (descriptionAdapter != null) {
                description = descriptionAdapter.descriptionFromElement(content.getCreator(), content.getSenders(), content.getName(), content.getDisposition(), contentDescription);
            }
            else {
                throw new AssertionError("Unsupported Description: " + contentDescription.getNamespace());
            }
        }

        JingleContentTransport contentTransport = content.getTransport();
        if (contentTransport != null) {
            JingleTransportAdapter<?> transportAdapter
                    = JingleContentProviderManager.getJingleTransportAdapter(contentTransport.getNamespace());
            if (transportAdapter != null) {
                transport = transportAdapter.transportFromElement(contentTransport);
            }
            else {
                throw new AssertionError("Unsupported Transport: " + contentTransport.getNamespace());
            }
        }

        JingleContentSecurity securityElement = content.getSecurity();
        if (securityElement != null) {
            JingleSecurityAdapter<?> securityAdapter
                    = JingleContentProviderManager.getJingleSecurityAdapter(content.getSecurity().getNamespace());
            if (securityAdapter != null) {
                security = securityAdapter.securityFromElement(securityElement);
            }
            else {
                throw new AssertionError("Unsupported Security: " + securityElement.getNamespace());
            }
        }

        return new JingleContentImpl(connection, description, transport, security, content.getName(), content.getDisposition(), content.getCreator(), content.getSenders());
    }

    public void setSenders(JingleContent.Senders senders) {
        this.senders = senders;
    }

    /* HANDLE_XYZ */
    public IQ handleJingleRequest(Jingle request, XMPPConnection connection) {
        switch (request.getAction()) {
            case content_modify:
                return handleContentModify(request, connection);
            case description_info:
                return handleDescriptionInfo(request, connection);
            case security_info:
                return handleSecurityInfo(request, connection);
            case session_info:
                return handleSessionInfo(request, connection);
            case transport_accept:
                return handleTransportAccept(request, connection);
            case transport_info:
                return handleTransportInfo(request, connection);
            case transport_reject:
                return handleTransportReject(request, connection);
            case transport_replace:
                return handleTransportReplace(request, connection);
            default:
                throw new AssertionError("Illegal jingle action: " + request.getAction() + " is not allowed here.");
        }
    }

    void handleContentAccept(Jingle request, XMPPConnection connection) {
        start(connection);
    }

    IQ handleSessionAccept(Jingle request, XMPPConnection connection) {
        LOGGER.info("RECEIVED SESSION ACCEPT!");
        JingleContent contentElement = null;
        for (JingleContent c : request.getContents()) {
            if (c.getName().equals(getName())) {
                contentElement = c;
                break;
            }
        }

        if (contentElement == null) {
            throw new AssertionError("Session Accept did not contain this content.");
        }

        // Notify session listener that remote has accepted the file transfer.
        getParent().notifySessionAccepted();

        mConnection = connection;
        getTransport().handleSessionAccept(contentElement.getTransport(), mConnection);
        start(mConnection);
        return IQ.createResultIQ(request);
    }

    private IQ handleContentModify(Jingle request, XMPPConnection connection) {
        mConnection = connection;
        return IQ.createErrorResponse(request, StanzaError.Condition.feature_not_implemented);
    }

    private IQ handleDescriptionInfo(Jingle request, XMPPConnection connection) {
        mConnection = connection;
        return IQ.createErrorResponse(request, StanzaError.Condition.feature_not_implemented);
    }

    public void handleContentRemove(JingleSessionImpl session, XMPPConnection connection) {
        mConnection = connection;
    }

    private IQ handleSecurityInfo(Jingle request, XMPPConnection connection) {
        mConnection = connection;
        return IQ.createErrorResponse(request, StanzaError.Condition.feature_not_implemented);
    }

    private IQ handleSessionInfo(Jingle request, XMPPConnection connection) {
        mConnection = connection;
        return IQ.createResultIQ(request);
    }

    private IQ handleTransportAccept(Jingle request, XMPPConnection connection) {
        if (pendingReplacingTransport == null) {
            LOGGER.warning("Received transport-accept, but apparently we did not try to replace the transport.");
            return jutil.createErrorOutOfOrder(request);
        }

        transport = pendingReplacingTransport;
        pendingReplacingTransport = null;

        start(connection);

        return IQ.createResultIQ(request);
    }

    private IQ handleTransportInfo(Jingle request, XMPPConnection connection) {
        assert request.getContents().size() == 1;
        JingleContent content = request.getContents().get(0);

        mConnection = connection;
        return transport.handleTransportInfo(content.getTransport().getInfo(), request);
    }

    private IQ handleTransportReject(Jingle request, final XMPPConnection connection) {
        if (pendingReplacingTransport == null) {
            throw new AssertionError("We didn't try to replace the transport.");
        }
        Async.go(() -> {
            transportBlacklist.add(pendingReplacingTransport.getNamespace());
            pendingReplacingTransport = null;
            try {
                replaceTransport(transportBlacklist, connection);
            } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Could not replace transport: " + e, e);
            }
        });
        return IQ.createResultIQ(request);
    }

    private IQ handleTransportReplace(final Jingle request, final XMPPConnection connection) {
        // Tie Break?
        if (pendingReplacingTransport != null) {
            Async.go(() -> {
                try {
                    jutil.sendErrorTieBreak(request);
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Could not send tie-break: " + e, e);
                }
            });
            return IQ.createResultIQ(request);
        }

        JingleContent contentElement = null;
        for (JingleContent c : request.getContents()) {
            if (c.getName().equals(getName())) {
                contentElement = c;
                break;
            }
        }

        if (contentElement == null) {
            throw new AssertionError("Unknown content");
        }

        final JingleSessionImpl session = getParent();
        final JingleContentTransport transportElement = contentElement.getTransport();

        JingleTransportMethodManager jtmManager = JingleTransportMethodManager.getInstanceFor(parent.getConnection());
        JingleTransportManager<?> jtManager = jtmManager.getTransportManager(transportElement.getNamespace());

        // Unsupported/Blacklisted transport -> reject.
        if (jtManager == null || getTransportBlacklist().contains(transportElement.getNamespace())) {
            Async.go(() -> {
                try {
                    jutil.sendTransportReject(session.getRemote(), session.getLocal(), session.getSessionId(), getCreator(), getName(), transportElement);
                } catch (SmackException.NotConnectedException | InterruptedException
                        | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Could not send transport-reject: " + e, e);
                }
            });

        }
        else {
            // Blacklist current transport
            this.getTransportBlacklist().add(this.transport.getNamespace());

            this.transport = jtManager.createTransportForResponder(this, transportElement);
            Async.go(() -> {
                try {
                    jutil.sendTransportAccept(session.getRemote(), session.getLocal(), session.getSessionId(), getCreator(), getName(), transport.getElement());
                } catch (SmackException.NotConnectedException | InterruptedException
                        | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Could not send transport-accept: " + e, e);
                }
            });
            start(connection);
        }

        return IQ.createResultIQ(request);
    }

    /* MISCELLANEOUS */

    public JingleContent getElement() {
        JingleContent.Builder builder = JingleContent.getBuilder()
                .setName(name)
                .setCreator(creator)
                .setSenders(senders)
                .setDisposition(disposition);

        if (description != null) {
            builder.setDescription(description.getElement());
        }

        if (transport != null) {
            builder.setTransport(transport.getElement());
        }

        if (security != null) {
            builder.setSecurity(security.getElement());
        }

        return builder.build();
    }

    public Set<String> getTransportBlacklist() {
        return transportBlacklist;
    }

    public JingleContent.Creator getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public JingleContent.Senders getSenders() {
        return senders;
    }

    public void setParent(JingleSessionImpl session) {
        if (!session.equals(parent)) {
            this.parent = session;
        }
    }

    public JingleSessionImpl getParent() {
        return parent;
    }

    public JingleDescription<?> getDescription() {
        return description;
    }

    public void setDescription(JingleDescription<?> description) {
        if (description != null && this.description != description) {
            this.description = description;
            description.setParent(this);
        }
    }

    public JingleTransport<?> getTransport() {
        return transport;
    }

    public void setTransport(JingleTransport<?> transport) {
        if (transport != null && this.transport != transport) {
            this.transport = transport;
            transport.setParent(this);
        }
    }

    public JingleSecurity<?> getSecurity() {
        return security;
    }

    public void setSecurity(JingleSecurity<?> security) {
        if (security != null && this.security != security) {
            this.security = security;
            security.setParent(this);
        }
    }

    public boolean isSending() {
        return (getSenders() == JingleContent.Senders.initiator && getParent().isInitiator()) ||
                (getSenders() == JingleContent.Senders.responder && getParent().isResponder()) ||
                getSenders() == JingleContent.Senders.both;
    }

    public boolean isReceiving() {
        return (getSenders() == JingleContent.Senders.initiator && getParent().isResponder()) ||
                (getSenders() == JingleContent.Senders.responder && getParent().isInitiator()) ||
                getSenders() == JingleContent.Senders.both;
    }

    public void start(final XMPPConnection connection) {
        transport.prepare(connection);

        if (security != null) {
            security.prepare(connection, getParent().getRemote());
        }

        // Establish transport
        Async.go(() -> {
            try {
                if (isReceiving()) {
                    LOGGER.info("Establish incoming bytestream.");
                    getTransport().establishIncomingBytestreamSession(connection, JingleContentImpl.this, getParent());
                }
                else if (isSending()) {
                    LOGGER.info("Establish outgoing bytestream.");
                    getTransport().establishOutgoingBytestreamSession(connection, JingleContentImpl.this, getParent());
                }
                else {
                    LOGGER.info("Neither receiving, nor sending. Assume receiving.");
                    getTransport().establishIncomingBytestreamSession(connection, JingleContentImpl.this, getParent());
                }
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Error establishing connection: " + e, e);
            }
        });
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        LOGGER.info("TransportReady: " + (isReceiving() ? "Receive" : "Send"));
        if (bytestreamSession == null) {
            throw new AssertionError("bytestreamSession MUST NOT be null at this point.");
        }

        /*
         * Must execute byteStream sending in Async; large file sending may take > 5 seconds,
         * and sending IQ.createResultIQ() will be timeout: SmackException$NoResponseException
         * JingleS5BTransportImpl.connectIfReady() Could not send candidate activated
         */
        Async.go(() -> {
            if (security != null) {
                if (isReceiving()) {
                    LOGGER.info("Decrypt incoming Bytestream.");
                    getSecurity().decryptIncomingBytestream(bytestreamSession, this);
                }
                else if (isSending()) {
                    LOGGER.info("Encrypt outgoing Bytestream.");
                    getSecurity().encryptOutgoingBytestream(bytestreamSession, this);
                }
            }
            else {
                description.onBytestreamReady(bytestreamSession);
            }
        });
    }

    @Override
    public void onTransportFailed(Exception e) {
        // Add current transport to blacklist.
        getTransportBlacklist().add(transport.getNamespace());

        // Replace transport.
        if (getParent().isInitiator()) {
            try {
                replaceTransport(getTransportBlacklist(), getParent().getConnection());
            } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.SEVERE, "Could not send transport-replace: " + e, e);
            }
        }
    }

    @Override
    public void onSecurityReady(BytestreamSession bytestreamSession) {
        description.onBytestreamReady(bytestreamSession);
    }

    @Override
    public void onSecurityFailed(Exception e) {
        LOGGER.log(Level.SEVERE, "Security failed: " + e, e);
    }

    public void onContentFinished() {
        JingleSessionImpl session = getParent();
        session.onContentFinished(this);
    }

    public void onContentFailed(Exception e) {
    }

    public void onContentCancel() {
        JingleSessionImpl session = getParent();
        session.onContentCancel(this);
    }

    private void replaceTransport(Set<String> blacklist, XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        if (pendingReplacingTransport != null) {
            throw new AssertionError("Transport replace already pending.");
        }

        JingleSessionImpl session = getParent();
        JingleTransportMethodManager jtmManager = JingleTransportMethodManager.getInstanceFor(connection);
        JingleTransportManager<?> jtManager = jtmManager.getBestAvailableTransportManager(blacklist);

        if (jtManager == null) {
            jutil.sendSessionTerminateFailedTransport(session.getRemote(), session.getSessionId());
            return;
        }

        pendingReplacingTransport = jtManager.createTransportForInitiator(this);
        jutil.sendTransportReplace(session.getRemote(), session.getLocal(),
                session.getSessionId(), getCreator(), getName(), pendingReplacingTransport.getElement());
    }

    private static String randomName() {
        return "cont-" + StringUtils.randomString(16);
    }
}
