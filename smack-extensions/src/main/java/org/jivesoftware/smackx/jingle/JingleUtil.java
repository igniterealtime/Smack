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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleErrorElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

import org.jxmpp.jid.FullJid;

/**
 * Util to quickly create and send jingle stanzas.
 */
public class JingleUtil {

    private final XMPPConnection connection;

    public JingleUtil(XMPPConnection connection) {
        this.connection = connection;
    }

    public JingleElement createSessionInitiate(FullJid recipient,
                                               String sessionId,
                                               JingleContentElement.Creator contentCreator,
                                               String contentName,
                                               JingleContentElement.Senders contentSenders,
                                               JingleContentDescriptionElement description,
                                               JingleContentTransportElement transport) {

        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(connection.getUser());

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public JingleElement createSessionInitiateFileOffer(FullJid recipient,
                                                        String sessionId,
                                                        JingleContentElement.Creator contentCreator,
                                                        String contentName,
                                                        JingleContentDescriptionElement description,
                                                        JingleContentTransportElement transport) {
        return createSessionInitiate(recipient, sessionId, contentCreator, contentName,
                JingleContentElement.Senders.initiator, description, transport);
    }

    public IQ sendSessionInitiateFileOffer(FullJid recipient,
                                           String sessionId,
                                           JingleContentElement.Creator contentCreator,
                                           String contentName,
                                           JingleContentDescriptionElement description,
                                           JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        JingleElement jingle = createSessionInitiateFileOffer(recipient, sessionId, contentCreator, contentName, description, transport);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public IQ sendSessionInitiate(FullJid recipient,
                                  String sessionId,
                                  JingleContentElement.Creator contentCreator,
                                  String contentName,
                                  JingleContentElement.Senders contentSenders,
                                  JingleContentDescriptionElement description,
                                  JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {

        JingleElement jingle = createSessionInitiate(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionAccept(FullJid recipient,
                                      String sessionId,
                                      JingleContentElement.Creator contentCreator,
                                      String contentName,
                                      JingleContentElement.Senders contentSenders,
                                      JingleContentDescriptionElement description,
                                      JingleContentTransportElement transport) {

        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setResponder(connection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    public IQ sendSessionAccept(FullJid recipient,
                                String sessionId,
                                JingleContentElement.Creator contentCreator,
                                String contentName,
                                JingleContentElement.Senders contentSenders,
                                JingleContentDescriptionElement description,
                                JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {

        JingleElement jingle = createSessionAccept(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminate(FullJid recipient, String sessionId, JingleReasonElement reason) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId)
                .setReason(reason);

        JingleElement jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public JingleElement createSessionTerminate(FullJid recipient, String sessionId, JingleReasonElement.Reason reason) {
        return createSessionTerminate(recipient, sessionId, new JingleReasonElement(reason));
    }

    public JingleElement createSessionTerminateDecline(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.decline);
    }

    public IQ sendSessionTerminateDecline(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        JingleElement jingle = createSessionTerminateDecline(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateSuccess(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.success);
    }

    public IQ sendSessionTerminateSuccess(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        JingleElement jingle = createSessionTerminateSuccess(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateBusy(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.busy);
    }

    public IQ sendSessionTerminateBusy(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        JingleElement jingle = createSessionTerminateBusy(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.AlternativeSession(altSessionId));
    }

    public IQ sendSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        JingleElement jingle = createSessionTerminateAlternativeSession(recipient, sessionId, altSessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateCancel(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.cancel);
    }

    public IQ sendSessionTerminateCancel(FullJid recipient,
                                  String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        JingleElement jingle = createSessionTerminateCancel(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                                      JingleContentElement.Creator contentCreator, String contentName) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                  JingleContentElement.Creator contentCreator, String contentName)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateContentCancel(recipient, sessionId, contentCreator, contentName);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.unsupported_transports);
    }

    public IQ sendSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateUnsupportedTransports(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateFailedTransport(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.failed_transport);
    }

    public IQ sendSessionTerminateFailedTransport(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateFailedTransport(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.unsupported_applications);
    }

    public IQ sendSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateUnsupportedApplications(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateFailedApplication(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.failed_application);
    }

    public IQ sendSessionTerminateFailedApplication(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateFailedApplication(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReasonElement.Reason.incompatible_parameters);
    }

    public IQ sendSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleElement jingle = createSessionTerminateIncompatibleParameters(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public IQ sendContentRejectFileNotAvailable(FullJid recipient, String sessionId, JingleContentDescriptionElement description) {
        return null; //TODO Later
    }

    public JingleElement createSessionPing(FullJid recipient, String sessionId) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        JingleElement jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionPing(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleElement jingle = createSessionPing(recipient, sessionId);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public IQ createAck(JingleElement jingle) {
        return IQ.createResultIQ(jingle);
    }

    public void sendAck(JingleElement jingle) throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createAck(jingle));
    }

    public JingleElement createTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
                                         JingleContentElement.Creator contentCreator, String contentName,
                                         JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setInitiator(initiator)
                .setSessionId(sessionId)
                .setAction(JingleAction.transport_replace);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setName(contentName).setCreator(contentCreator).setTransport(transport);
        JingleElement jingle = jb.addJingleContent(cb.build()).build();

        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    public IQ sendTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
                                   JingleContentElement.Creator contentCreator, String contentName,
                                   JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleElement jingle = createTransportReplace(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
                                        JingleContentElement.Creator contentCreator, String contentName,
                                        JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setAction(JingleAction.transport_accept)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    public IQ sendTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
                                  JingleContentElement.Creator contentCreator, String contentName,
                                  JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleElement jingle = createTransportAccept(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    public JingleElement createTransportReject(FullJid recipient, FullJid initiator, String sessionId,
                                        JingleContentElement.Creator contentCreator, String contentName,
                                        JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.builder(connection);
        jb.setAction(JingleAction.transport_reject)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    public IQ sendTransportReject(FullJid recipient, FullJid initiator, String sessionId,
                                  JingleContentElement.Creator contentCreator, String contentName,
                                  JingleContentTransportElement transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleElement jingle = createTransportReject(recipient, initiator, sessionId, contentCreator, contentName, transport);
        return connection.sendIqRequestAndWaitForResponse(jingle);
    }

    /*
     * ####################################################################################################
     */

    public IQ createErrorUnknownSession(JingleElement request) {
        StanzaError error = StanzaError.getBuilder()
                        .setCondition(StanzaError.Condition.item_not_found)
                        .addExtension(JingleErrorElement.UNKNOWN_SESSION)
                        .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnknownSession(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnknownSession(request));
    }

    public IQ createErrorUnknownInitiator(JingleElement request) {
        return IQ.createErrorResponse(request, StanzaError.Condition.service_unavailable);
    }

    public void sendErrorUnknownInitiator(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnknownInitiator(request));
    }

    public IQ createErrorUnsupportedInfo(JingleElement request) {
        StanzaError error = StanzaError.getBuilder()
                        .setCondition(StanzaError.Condition.feature_not_implemented)
                        .addExtension(JingleErrorElement.UNSUPPORTED_INFO)
                        .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnsupportedInfo(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnsupportedInfo(request));
    }

    public IQ createErrorTieBreak(JingleElement request) {
        StanzaError error = StanzaError.getBuilder()
                        .setCondition(StanzaError.Condition.conflict)
                        .addExtension(JingleErrorElement.TIE_BREAK)
                        .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorTieBreak(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorTieBreak(request));
    }

    public IQ createErrorOutOfOrder(JingleElement request) {
        StanzaError error = StanzaError.getBuilder()
                        .setCondition(StanzaError.Condition.unexpected_request)
                        .addExtension(JingleErrorElement.OUT_OF_ORDER)
                        .build();
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorOutOfOrder(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorOutOfOrder(request));
    }

    public IQ createErrorMalformedRequest(JingleElement request) {
        return IQ.createErrorResponse(request, StanzaError.Condition.bad_request);
    }

    public void sendErrorMalformedRequest(JingleElement request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorMalformedRequest(request));
    }
}
