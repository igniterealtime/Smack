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
package org.jivesoftware.smackx.jingle;

import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleError;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.element.JingleReason.Reason;
import org.jivesoftware.smackx.jingle_rtp.element.Grouping;
import org.jivesoftware.smackx.jingle_rtp.element.SessionInfo;
import org.jivesoftware.smackx.jingle_rtp.element.SessionInfoType;

import org.jxmpp.jid.FullJid;

/**
 * Util to quickly create and send jingle stanzas.
 */
public class JingleUtil {

    private final XMPPConnection mConnection;

    public JingleUtil(XMPPConnection connection) {
        mConnection = connection;
    }

    /**
     * Creates a new {@link Jingle} with the <code>session-initiate</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>session-initiate</code> packet.
     */
    public Jingle createSessionInitiate(FullJid recipient, String sessionId, List<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle jingleSI = jb.build();
        jingleSI.setFrom(mConnection.getUser());
        jingleSI.setTo(recipient);

        return jingleSI;
    }

    public Jingle createSessionInitiate(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName, JingleContent.Senders contentSenders,
            JingleContentDescription description, JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(mConnection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public Jingle createSessionInitiateFileOffer(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentDescription description, JingleContentTransport transport) {
        return createSessionInitiate(recipient, sessionId, contentCreator, contentName, JingleContent.Senders.initiator,
                description, transport);
    }

    public IQ sendSessionInitiateFileOffer(FullJid recipient,
            String sessionId,
            JingleContent.Creator contentCreator,
            String contentName,
            JingleContentDescription description,
            JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionInitiateFileOffer(recipient, sessionId, contentCreator, contentName, description, transport);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public IQ sendSessionInitiate(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName, JingleContent.Senders contentSenders,
            JingleContentDescription description, JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException {
        Jingle jingle = createSessionInitiate(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return mConnection.createStanzaCollectorAndSend(jingle).nextResult();
    }

    /**
     * Creates a {@link Jingle} <code>session-accept</code> packet with the specified <code>from</code>,
     * <code>to</code>, <code>sessionId</code>, and <code>content</code>. Given our role in a conversation, we would
     * assume that the <code>from</code> value should also be used for the value of the Jingle <code>responder</code>.
     *
     * @param sessionInitIQ the received session-initiate Jingle
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>session-accept</code> packet.
     */
    public Jingle createSessionAccept(Jingle sessionInitIQ, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setResponder(mConnection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionInitIQ.getSid());

        for (JingleContent content : contentList)
            jb.addJingleContent(content);

        // Just copy to sessionInitIQ Grouping element to session-accept
        ExtensionElement groupExtension = sessionInitIQ.getExtension(Grouping.QNAME);
        if (groupExtension != null) {
            jb.addExtension(groupExtension);
        }

        Jingle jingleSA = jb.build();
        jingleSA.setTo(sessionInitIQ.getInitiator());
        jingleSA.setFrom(mConnection.getUser());

        return jingleSA;
    }

    public Jingle createSessionAccept(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName, JingleContent.Senders contentSenders,
            JingleContentDescription description, JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setResponder(mConnection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(mConnection.getUser());

        return jingle;
    }

    public IQ sendSessionAccept(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName, JingleContent.Senders contentSenders,
            JingleContentDescription description, JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException {
        Jingle jingle = createSessionAccept(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return mConnection.createStanzaCollectorAndSend(jingle).nextResult();
    }

    /**
     * Creates a {@link Jingle} <code>session-info</code> packet carrying a the specified payload type.
     *
     * @param recipient their full jid
     * @param sessionId the ID of the Jingle session this IQ will belong to.
     * @return a {@link Jingle} <code>session-info</code> packet carrying a the specified payload type.
     */
    public Jingle createSessionInfo(FullJid recipient, String sessionId) {
        return createSessionInfo(recipient, sessionId, null);
    }

    /**
     * Creates a {@link Jingle} <code>session-info</code> packet carrying the specified payload type.
     *
     * @param recipient their full jid
     * @param sessionId the ID of the Jingle session this IQ will belong to.
     * @param type the exact type (e.g. ringing, hold, mute) of the session info IQ.
     * @return a {@link Jingle} <code>session-info</code> packet carrying a the specified payload type.
     */
    public Jingle createSessionInfo(FullJid recipient, String sessionId, SessionInfoType type) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_info)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());
        if (type != null)
            jb.setSessionInfo(SessionInfo.builder(type).build());

        Jingle sessionInfo = jb.build();
        sessionInfo.setFrom(mConnection.getUser());
        sessionInfo.setTo(recipient);

        return sessionInfo;
    }

    /**
     * Creates a {@link Jingle} <code>session-terminate</code> packet with the specified recipient, sessionId, and reason.
     *
     * @param recipient the remote Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param reason the reason for the termination
     * @return the newly constructed {@link Jingle} <code>session-terminate</code> packet. .
     */
    public Jingle createSessionTerminate(FullJid recipient, String sessionId, JingleReason reason) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId)
                .setReason(reason);

        Jingle jingle = jb.build();
        jingle.setFrom(mConnection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public Jingle createSessionTerminate(FullJid recipient, String sessionId, Reason reason) {
        return createSessionTerminate(recipient, sessionId, new JingleReason(reason));
    }

    public Jingle createSessionTerminateDecline(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.decline);
    }

    public IQ sendSessionTerminateDecline(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateDecline(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateSuccess(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Success);
    }

    public IQ sendSessionTerminateSuccess(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateSuccess(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateBusy(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.busy);
    }

    public IQ sendSessionTerminateBusy(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateBusy(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.AlternativeSession(altSessionId));
    }

    public IQ sendSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateAlternativeSession(recipient, sessionId, altSessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateCancel(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.cancel);
    }

    public IQ sendSessionTerminateCancel(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateCancel(recipient, sessionId);
        return mConnection.sendIqRequestAndWaitForResponse(jingle);
    }

    public Jingle createSessionTerminateContentCancel(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(mConnection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionTerminateContentCancel(FullJid recipient, String sessionId,
            JingleContent.Creator contentCreator, String contentName)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateContentCancel(recipient, sessionId, contentCreator, contentName);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.unsupported_transports);
    }

    public IQ sendSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateUnsupportedTransports(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateFailedTransport(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.failed_transport);
    }

    public IQ sendSessionTerminateFailedTransport(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateFailedTransport(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.unsupported_applications);
    }

    public IQ sendSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateUnsupportedApplications(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateFailedApplication(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.failed_application);
    }

    public IQ sendSessionTerminateFailedApplication(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateFailedApplication(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, Reason.incompatible_parameters);
    }

    public IQ sendSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateIncompatibleParameters(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    /**
     * Creates a {@link Jingle} <code>description-info</code> packet with the specified <code>from</code>,
     * <code>to</code>, <code>sessionId</code>, and <code>content</code>. Given our role in a conversation, we would
     * assume that the <code>from</code> value should also be used for the value of the Jingle <code>responder</code>.
     *
     * @param sessionInitIQ the received session-initiate Jingle
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>description-info</code> packet.
     */
    public Jingle createDescriptionInfo(Jingle sessionInitIQ, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.description_info)
                .setSessionId(sessionInitIQ.getSid())
                .setResponder(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle descriptionInfo = jb.build();
        descriptionInfo.setFrom(mConnection.getUser());
        descriptionInfo.setTo(sessionInitIQ.getInitiator());

        return descriptionInfo;
    }

    /**
     * Creates a {@link Jingle} <code>transport-info</code> packet with the specified <code>from</code>,
     * <code>to</code>, <code>sessionId</code>, and <code>contentList</code>. Given our role in a conversation, we
     * would assume that the <code>from</code> value should also be used for the value of the Jingle <code>responder</code>.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media transport descriptions.
     * @return the newly constructed {@link Jingle} <code>transport-info</code> packet.
     */
    public Jingle createTransportInfo(FullJid recipient, String sessionId, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.transport_info)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle transportInfo = jb.build();
        transportInfo.setFrom(mConnection.getUser());
        transportInfo.setTo(recipient);

        return transportInfo;
    }

    /**
     * Creates a new {@link Jingle} with the <code>content-add</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>content-add</code> packet.
     */
    public Jingle createContentAdd(FullJid recipient, String sessionId, List<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.content_add)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle contentAdd = jb.build();
        contentAdd.setFrom(mConnection.getUser());
        contentAdd.setTo(recipient);

        return contentAdd;
    }

    /**
     * Creates a new {@link Jingle} with the <code>content-accept</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>content-accept</code> packet.
     */
    public Jingle createContentAccept(FullJid recipient, String sessionId, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.content_accept)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle contentAccept = jb.build();
        contentAccept.setFrom(mConnection.getUser());
        contentAccept.setTo(recipient);

        return contentAccept;
    }

    /**
     * Creates a new {@link Jingle} with the <code>content-reject</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>content-reject</code> packet.
     */
    public Jingle createContentReject(FullJid recipient, String sessionId, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.content_reject)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        if (contentList != null) {
            for (JingleContent content : contentList) {
                jb.addJingleContent(content);
            }
        }

        Jingle contentReject = jb.build();
        contentReject.setFrom(mConnection.getUser());
        contentReject.setTo(recipient);

        return contentReject;
    }

    /**
     * Creates a new {@link Jingle} with the <code>content-modify</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param content the content element containing media and transport description.
     * @return the newly constructed {@link Jingle} <code>content-modify</code> packet.
     */
    public Jingle createContentModify(FullJid recipient, String sessionId, JingleContent content) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.content_modify)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        jb.addJingleContent(content);

        Jingle contentModify = jb.build();
        contentModify.setFrom(mConnection.getUser());
        contentModify.setTo(recipient);

        return contentModify;
    }

    /**
     * Creates a new {@link Jingle} with the <code>content-remove</code> action.
     *
     * @param recipient the destination Jid
     * @param sessionId the ID of the Jingle session that this message will be terminating.
     * @param contentList the content elements containing media and transport descriptions.
     * @return the newly constructed {@link Jingle} <code>content-remove</code> packet.
     */
    public Jingle createContentRemove(FullJid recipient, String sessionId, Iterable<JingleContent> contentList) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.content_remove)
                .setSessionId(sessionId)
                .setInitiator(mConnection.getUser());

        for (JingleContent content : contentList) {
            jb.addJingleContent(content);
        }

        Jingle contentRemove = jb.build();
        contentRemove.setFrom(mConnection.getUser());
        contentRemove.setTo(recipient);

        return contentRemove;
    }

    public IQ sendContentRejectFileNotAvailable(FullJid recipient, String sessionId, JingleContentDescription description) {
        return null; //TODO Later
    }

    public Jingle createSessionPing(FullJid recipient, String sessionId) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        Jingle jingle = jb.build();
        jingle.setFrom(mConnection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionPing(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionPing(recipient, sessionId);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public IQ createAck(Jingle jingle) {
        return IQ.createResultIQ(jingle);
    }

    public void sendAck(Jingle jingle) throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createAck(jingle));
    }

    public Jingle createTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setInitiator(initiator)
                .setSessionId(sessionId)
                .setAction(JingleAction.transport_replace);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(contentName)
                .setCreator(contentCreator)
                .setTransport(transport);
        Jingle jingle = jb.addJingleContent(cb.build()).build();

        jingle.setTo(recipient);
        jingle.setFrom(mConnection.getUser());

        return jingle;
    }

    public IQ sendTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createTransportReplace(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.transport_accept)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(mConnection.getUser());

        return jingle;
    }

    public IQ sendTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createTransportAccept(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createTransportReject(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.transport_reject)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(mConnection.getUser());

        return jingle;
    }

    public IQ sendTransportReject(FullJid recipient, FullJid initiator, String sessionId,
            JingleContent.Creator contentCreator, String contentName,
            JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createTransportReject(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return mConnection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    /*
     * ####################################################################################################
     */

    public IQ createErrorUnknownSession(Jingle request) {
        StanzaError error = StanzaError.getBuilder()
                .setCondition(StanzaError.Condition.item_not_found)
                .addExtension(JingleError.UNKNOWN_SESSION)
                .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnknownSession(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorUnknownSession(request));
    }

    public IQ createErrorUnknownInitiator(Jingle request) {
        return IQ.createErrorResponse(request, StanzaError.Condition.service_unavailable);
    }

    public void sendErrorUnknownInitiator(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorUnknownInitiator(request));
    }

    public IQ createErrorUnsupportedInfo(Jingle request) {
        StanzaError error = StanzaError.getBuilder()
                .setCondition(StanzaError.Condition.feature_not_implemented)
                .addExtension(JingleError.UNSUPPORTED_INFO)
                .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnsupportedInfo(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorUnsupportedInfo(request));
    }

    public IQ createErrorTieBreak(Jingle request) {
        StanzaError error = StanzaError.getBuilder()
                .setCondition(StanzaError.Condition.conflict)
                .addExtension(JingleError.TIE_BREAK)
                .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorTieBreak(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorTieBreak(request));
    }

    public IQ createErrorOutOfOrder(Jingle request) {
        StanzaError error = StanzaError.getBuilder()
                .setCondition(StanzaError.Condition.unexpected_request)
                .addExtension(JingleError.OUT_OF_ORDER)
                .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorOutOfOrder(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorOutOfOrder(request));
    }

    public IQ createErrorMalformedRequest(Jingle request) {
        return IQ.createErrorResponse(request, StanzaError.Condition.bad_request);
    }

    public void sendErrorMalformedRequest(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        mConnection.sendStanza(createErrorMalformedRequest(request));
    }
}
