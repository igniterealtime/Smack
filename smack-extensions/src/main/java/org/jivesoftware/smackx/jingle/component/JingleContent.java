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
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.callback.JingleSecurityCallback;
import org.jivesoftware.smackx.jingle.callback.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

/**
 * Internal class that holds the state of a content in a modifiable form.
 */
public class JingleContent implements JingleTransportCallback, JingleSecurityCallback {

    private static final Logger LOGGER = Logger.getLogger(JingleContent.class.getName());

    private final JingleContentElement.Creator creator;
    private final String name;
    private final String disposition;
    private JingleSession parent;
    private JingleContentElement.Senders senders;
    private JingleDescription<?> description;
    private JingleTransport<?> transport;
    private JingleSecurity<?> security;

    private JingleTransport<?> pendingReplacingTransport = null;

    private final Set<String> transportBlacklist = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Create an empty JingleContent with random name.
     * @param creator creator.
     * @param senders sender.
     */
    public JingleContent(JingleContentElement.Creator creator, JingleContentElement.Senders senders) {
        this(null, null, null, randomName(), null, creator, senders);
    }

    /**
     * Create a JingleContent.
     * @param description description component.
     * @param transport transport component.
     * @param security security component.
     * @param name content name.
     * @param disposition disposition.
     * @param creator creator.
     * @param senders senders.
     */
    public JingleContent(JingleDescription<?> description, JingleTransport<?> transport, JingleSecurity<?> security, String name, String disposition, JingleContentElement.Creator creator, JingleContentElement.Senders senders) {
        setDescription(description);
        setTransport(transport);
        setSecurity(security);
        this.name = name;
        this.disposition = disposition;
        this.creator = creator;
        this.senders = senders;
    }

    /**
     * Create a new JingleContent from a {@link JingleContentElement}.
     * @param content contentElement.
     * @return jingleContent.
     */
    public static JingleContent fromElement(JingleContentElement content) {
        JingleDescription<?> description = null;
        JingleTransport<?> transport = null;
        JingleSecurity<?> security = null;

        JingleContentDescriptionElement descriptionElement = content.getDescription();
        if (descriptionElement != null) {
            JingleDescriptionAdapter<?> descriptionAdapter = JingleManager.getJingleDescriptionAdapter(content.getDescription().getNamespace());
            if (descriptionAdapter != null) {
                description = descriptionAdapter.descriptionFromElement(content.getCreator(), content.getSenders(), content.getName(), content.getDisposition(), descriptionElement);
            } else {
                throw new AssertionError("Unsupported Description: " + descriptionElement.getNamespace());
            }
        }

        JingleContentTransportElement transportElement = content.getTransport();
        if (transportElement != null) {
            JingleTransportAdapter<?> transportAdapter = JingleManager.getJingleTransportAdapter(content.getTransport().getNamespace());
            if (transportAdapter != null) {
                transport = transportAdapter.transportFromElement(transportElement);
            } else {
                throw new AssertionError("Unsupported Transport: " + transportElement.getNamespace());
            }
        }

        JingleContentSecurityElement securityElement = content.getSecurity();
        if (securityElement != null) {
            JingleSecurityAdapter<?> securityAdapter = JingleManager.getJingleSecurityAdapter(content.getSecurity().getNamespace());
            if (securityAdapter != null) {
                security = securityAdapter.securityFromElement(securityElement);
            } else {
                throw new AssertionError("Unsupported Security: " + securityElement.getNamespace());
            }
        }

        return new JingleContent(description, transport, security, content.getName(), content.getDisposition(), content.getCreator(), content.getSenders());
    }

    /**
     * Set the senders attribute.
     * @param senders new value.
     */
    public void setSenders(JingleContentElement.Senders senders) {
        this.senders = senders;
    }

    /* HANDLE_XYZ */

    /**
     * Handle an incoming jingle request. This method basically routes incoming requests to different methods based on
     * the value of the request's action attribute.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    public IQ handleJingleRequest(JingleElement request, XMPPConnection connection) {
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

    /**
     * Handle incoming content-accept.
     * @param request request.
     * @param connection connection.
     */
    void handleContentAccept(JingleElement request, XMPPConnection connection) {
        start(connection);
    }

    /**
     * Handle incoming session-accept. This means activating the transport and starting the transmission.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    IQ handleSessionAccept(JingleElement request, XMPPConnection connection) {
        LOGGER.log(Level.INFO, "RECEIVED SESSION ACCEPT!");
        JingleContentElement contentElement = null;
        for (JingleContentElement c : request.getContents()) {
            if (c.getName().equals(getName())) {
                contentElement = c;
                break;
            }
        }

        if (contentElement == null) {
            throw new AssertionError("Session Accept did not contain this content.");
        }

        getTransport().handleSessionAccept(contentElement.getTransport(), connection);
        start(connection);
        return IQ.createResultIQ(request);
    }

    /**
     * Handle a content-modify request.
     * TODO: Implement.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleContentModify(JingleElement request, XMPPConnection connection) {
        return IQ.createErrorResponse(request, XMPPError.Condition.feature_not_implemented);
    }

    /**
     * Handle a description-info request.
     * TODO: Implement.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleDescriptionInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createErrorResponse(request, XMPPError.Condition.feature_not_implemented);
        //return description.handleDescriptionInfo(request.getContents().get(0).getDescription().getDescriptionInfo());
    }

    /**
     * Handle a content-remove request.
     * TODO: Implement.
     * @param session session that has this content as child.
     * @param connection connection.
     */
    public void handleContentRemove(JingleSession session, XMPPConnection connection) {

    }

    /**
     * Handle security-info requests.
     * TODO: Implement.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleSecurityInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createErrorResponse(request, XMPPError.Condition.feature_not_implemented);
    }

    /**
     * Handle session-info requests.
     * TODO: Implement.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleSessionInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    /**
     * Handle transport-accept requests.
     * This includes starting the transport and afterwards starting transmission.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleTransportAccept(JingleElement request, XMPPConnection connection) {

        if (pendingReplacingTransport == null) {
            LOGGER.log(Level.WARNING, "Received transport-accept, but apparently we did not try to replace the transport.");
            return JingleElement.createJingleErrorOutOfOrder(request);
        }

        transport = pendingReplacingTransport;
        pendingReplacingTransport = null;

        start(connection);

        return IQ.createResultIQ(request);
    }

    /**
     * Handle transport-info requests.
     * Pass the request down to the transport.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleTransportInfo(JingleElement request, XMPPConnection connection) {
        assert request.getContents().size() == 1;
        JingleContentElement content = request.getContents().get(0);

        return transport.handleTransportInfo(content.getTransport().getInfo(), request);
    }

    /**
     * Handle a transport-reject request.
     * That includes replacing the transport with the next choice.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleTransportReject(JingleElement request, final XMPPConnection connection) {
        if (pendingReplacingTransport == null) {
            // TODO: Throw other exception?
            throw new AssertionError("We didn't try to replace the transport.");
        }
        Async.go(new Runnable() {
            @Override
            public void run() {
                transportBlacklist.add(pendingReplacingTransport.getNamespace());
                pendingReplacingTransport = null;
                try {
                    replaceTransport(transportBlacklist, connection);
                } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Could not replace transport: " + e, e);
                }
            }
        });
        return IQ.createResultIQ(request);
    }

    /**
     * Handle a transport-replace request.
     * That includes replacing the transport if suitable and starting the transmission.
     * Otherwise reject the transport.
     * @param request request.
     * @param connection connection.
     * @return result.
     */
    private IQ handleTransportReplace(final JingleElement request, final XMPPConnection connection) {
        //Tie Break?
        if (pendingReplacingTransport != null) {
            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.createStanzaCollectorAndSend(JingleElement.createJingleErrorTieBreak(request)).nextResultOrThrow();
                    } catch (SmackException.NoResponseException | SmackException.NotConnectedException | InterruptedException | XMPPException.XMPPErrorException e) {
                        LOGGER.log(Level.SEVERE, "Could not send tie-break: " + e, e);
                    }
                }
            });
            return IQ.createResultIQ(request);
        }

        JingleContentElement contentElement = null;
        for (JingleContentElement c : request.getContents()) {
            if (c.getName().equals(getName())) {
                contentElement = c;
                break;
            }
        }

        if (contentElement == null) {
            throw new AssertionError("Unknown content");
        }

        final JingleSession session = getParent();
        final JingleContentTransportElement transportElement = contentElement.getTransport();

        JingleTransportManager tm = session.getJingleManager().getTransportManager(transportElement.getNamespace());

        // Unsupported/Blacklisted transport -> reject.
        if (tm == null || getTransportBlacklist().contains(transportElement.getNamespace())) {
            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        getParent().getJingleManager().getConnection().createStanzaCollectorAndSend(JingleElement.createTransportReject(session.getOurJid(), session.getPeer(), session.getSessionId(), getCreator(), getName(), transportElement));
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Could not send transport-reject: " + e, e);
                    }
                }
            });

        } else {
            //Blacklist current transport
            this.getTransportBlacklist().add(this.transport.getNamespace());

            this.transport = tm.createTransportForResponder(this, transportElement);
            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        getParent().getJingleManager().getConnection().createStanzaCollectorAndSend(JingleElement.createTransportAccept(session.getOurJid(), session.getPeer(), session.getSessionId(), getCreator(), getName(), transport.getElement()));
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Could not send transport-accept: " + e, e);
                    }
                }
            });
            start(connection);
        }

        return IQ.createResultIQ(request);
    }

    /* MISCELLANEOUS */

    /**
     * Return a {@link JingleContentElement} representing this {@link JingleContent}.
     * @return contentElement.
     */
    public JingleContentElement getElement() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder()
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

    /**
     * Return the transportBlacklist of this content.
     * The blacklist contains the namespaces of {@link JingleTransport} classes, that this content will not offer/accept.
     * @return transport blacklist.
     */
    public Set<String> getTransportBlacklist() {
        return transportBlacklist;
    }

    /**
     * Get the creator of the content. The creator is the party that added the content to the session.
     * @return creator.
     */
    public JingleContentElement.Creator getCreator() {
        return creator;
    }

    /**
     * Get the name of the content.
     * @return content-name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the senders of the content.
     * @return senders.
     */
    public JingleContentElement.Senders getSenders() {
        return senders;
    }

    /**
     * Set the parent {@link JingleSession} of the content.
     * @param session session that has this content as child.
     */
    public void setParent(JingleSession session) {
        if (this.parent != session) {
            this.parent = session;
        }
    }

    /**
     * Return the parent {@link JingleSession} of this content.
     * @return parent session.
     */
    public JingleSession getParent() {
        return parent;
    }

    /**
     * Return the {@link JingleDescription} of this content.
     * @return jingle description component.
     */
    public JingleDescription<?> getDescription() {
        return description;
    }

    /**
     * Set the {@link JingleDescription} of this content.
     * If needed, set the parent of the description to this content.
     * @param description jingle description component.
     */
    public void setDescription(JingleDescription<?> description) {
        if (description != null && this.description != description) {
            this.description = description;
            description.setParent(this);
        }
    }

    /**
     * Return the {@link JingleTransport} of this content.
     * @return jingle transport component.
     */
    public JingleTransport<?> getTransport() {
        return transport;
    }

    /**
     * Set the {@link JingleTransport} of this content.
     * If needed, set the parent of the transport component to this content.
     * @param transport jingle transport component.
     */
    public void setTransport(JingleTransport<?> transport) {
        if (transport != null && this.transport != transport) {
            this.transport = transport;
            transport.setParent(this);
        }
    }

    /**
     * Return the {@link JingleSecurity} of this content.
     * @return jingle security component.
     */
    public JingleSecurity<?> getSecurity() {
        return security;
    }

    /**
     * Set the {@link JingleSecurity} of this content.
     * If needed, set the parent of the security component to this content.
     * @param security jingle security component.
     */
    public void setSecurity(JingleSecurity<?> security) {
        if (security != null && this.security != security) {
            this.security = security;
            security.setParent(this);
        }
    }

    /**
     * Return true, if we are the sending party. Otherwise return false.
     * @return true if we are sending.
     */
    public boolean isSending() {
        return (getSenders() == JingleContentElement.Senders.initiator && getParent().isInitiator()) ||
                (getSenders() == JingleContentElement.Senders.responder && getParent().isResponder()) ||
                getSenders() == JingleContentElement.Senders.both;
    }

    /**
     * Return true if we are the receiving party. Otherwise return false.
     * @return true if we are receiving.
     */
    public boolean isReceiving() {
        return (getSenders() == JingleContentElement.Senders.initiator && getParent().isResponder()) ||
                (getSenders() == JingleContentElement.Senders.responder && getParent().isInitiator()) ||
                getSenders() == JingleContentElement.Senders.both;
    }

    /**
     * Prepare the security component (if any) and establish a bytestream session.
     * @param connection connection
     */
    public void start(final XMPPConnection connection) {
        transport.prepare(connection);

        if (security != null) {
            security.prepare(connection, getParent().getPeer());
        }

        //Establish transport
        Async.go(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isReceiving()) {
                        LOGGER.log(Level.INFO, "Establish incoming bytestream.");
                        getTransport().establishIncomingBytestreamSession(connection, JingleContent.this, getParent());
                    } else if (isSending()) {
                        LOGGER.log(Level.INFO, "Establish outgoing bytestream.");
                        getTransport().establishOutgoingBytestreamSession(connection, JingleContent.this, getParent());
                    } else {
                        LOGGER.log(Level.INFO, "Neither receiving, nor sending. Assume receiving.");
                        getTransport().establishIncomingBytestreamSession(connection, JingleContent.this, getParent());
                    }
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Error establishing connection: " + e, e);
                }
            }
        });
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        LOGGER.log(Level.INFO, "TransportReady: " + (isReceiving() ? "Receive" : "Send"));
        if (bytestreamSession == null) {
            throw new AssertionError("bytestreamSession MUST NOT be null at this point.");
        }

        if (security != null) {
            if (isReceiving()) {
                LOGGER.log(Level.INFO, "Decrypt incoming Bytestream.");
                getSecurity().decryptIncomingBytestream(bytestreamSession, this);
            } else if (isSending()) {
                LOGGER.log(Level.INFO, "Encrypt outgoing Bytestream.");
                getSecurity().encryptOutgoingBytestream(bytestreamSession, this);
            }
        } else {
            description.onBytestreamReady(bytestreamSession);
        }
    }

    @Override
    public void onTransportFailed(Exception e) {
        //Add current transport to blacklist.
        getTransportBlacklist().add(transport.getNamespace());

        //Replace transport.
        if (getParent().isInitiator()) {
            try {
                replaceTransport(getTransportBlacklist(), getParent().getJingleManager().getConnection());
            } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.SEVERE, "Could not send transport-replace: " + e, e);
            }
        }
    }

    @Override
    public void onSecurityReady(BytestreamSession bytestreamSession) {
        getDescription().onBytestreamReady(bytestreamSession);
    }

    @Override
    public void onSecurityFailed(Exception e) {
        LOGGER.log(Level.SEVERE, "Security failed: " + e, e);
    }

    /**
     * Content finished locally.
     */
    public void onContentFinished() {
        JingleSession session = getParent();
        session.onContentFinished(this);
    }

    /**
     * Content failed locally.
     * @param e exception.
     */
    public void onContentFailed(Exception e) {

    }

    /**
     * Content cancelled locally.
     */
    public void onContentCancel() {
        JingleSession session = getParent();
        session.onContentCancel(this);
    }

    /**
     * Handle incoming content-terminate.
     * Pass it down to the description.
     * @param reason reason.
     */
    public void handleContentTerminate(JingleReasonElement.Reason reason) {
        description.handleContentTerminate(reason);
    }

    /**
     * Locally replace the transport method.
     * @param blacklist ignore all methods on the blacklist.
     * @param connection connection.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NoResponseException
     */
    private void replaceTransport(Set<String> blacklist, XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        if (pendingReplacingTransport != null) {
            throw new AssertionError("Transport replace already pending.");
        }

        JingleSession session = getParent();
        JingleManager jingleManager = session.getJingleManager();

        JingleTransportManager rManager = jingleManager.getBestAvailableTransportManager(getParent().getPeer(), blacklist);
        if (rManager == null) {
            JingleElement failedTransport = JingleElement.createSessionTerminate(session.getPeer(),
                    session.getSessionId(), JingleReasonElement.Reason.failed_transport);
            connection.createStanzaCollectorAndSend(failedTransport).nextResultOrThrow();
            return;
        }

        pendingReplacingTransport = rManager.createTransportForInitiator(this);

        JingleElement transportReplace = JingleElement.createTransportReplace(session.getInitiator(), session.getPeer(),
                session.getSessionId(), getCreator(), getName(), pendingReplacingTransport.getElement());

        connection.createStanzaCollectorAndSend(transportReplace).nextResultOrThrow();
    }

    /**
     * Return a random content name.
     * @return random name.
     */
    private static String randomName() {
        return "cont-" + StringUtils.randomString(16);
    }
}
