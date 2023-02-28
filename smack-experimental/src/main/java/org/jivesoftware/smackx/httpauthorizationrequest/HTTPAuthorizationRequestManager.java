/**
 *
 * Copyright 2019-2023 Eng Chong Meng
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
package org.jivesoftware.smackx.httpauthorizationrequest;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromTypeFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmExtension;
import org.jivesoftware.smackx.httpauthorizationrequest.packet.ConfirmIQ;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

/**
 * The HTTP Request manager for HTTP Request. The manager handles both the HTTP Request via:
 * IQ : when the request is via EntityFullJid
 * Message: When request is via EntityBareJid
 *
 * XEP-0070: Verifying HTTP Requests via XMPP (1.0.1 (2016-12-09))
 *
 * @see HTTPAuthorizationRequestListener on callback
 */
public final class HTTPAuthorizationRequestManager extends Manager {
    private static final StanzaFilter MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            new StanzaExtensionFilter(ConfirmExtension.ELEMENT, ConfirmExtension.NAMESPACE)
    );

    private static final StanzaFilter INCOMING_MESSAGE_FILTER = new AndFilter(
            MESSAGE_FILTER,
            FromTypeFilter.DOMAIN_BARE_JID
    );

    private static final Logger LOGGER = Logger.getLogger(HTTPAuthorizationRequestManager.class.getName());

    private static final Map<XMPPConnection, HTTPAuthorizationRequestManager> INSTANCES = new WeakHashMap<>();

    private final Set<HTTPAuthorizationRequestListener> incomingListeners = new CopyOnWriteArraySet<>();

    private ConfirmExtension mConfirmExtension;
    private Message msgRequest = null;
    private ConfirmIQ mIqRequest = null;
    private String instruction = null;

    public static synchronized HTTPAuthorizationRequestManager getInstanceFor(XMPPConnection connection) {
        HTTPAuthorizationRequestManager httpResponseManager = INSTANCES.get(connection);
        if (httpResponseManager == null) {
            httpResponseManager = new HTTPAuthorizationRequestManager(connection);
            INSTANCES.put(connection, httpResponseManager);
        }
        return httpResponseManager;
    }

    private HTTPAuthorizationRequestManager(final XMPPConnection connection) {
        super(connection);

        // Listen for message HTTP request
        connection.addSyncStanzaListener(stanza -> {
            final Message message = (Message) stanza;
            mConfirmExtension = ConfirmExtension.from(message);

            if (mConfirmExtension == null) {
                return;
            }

            msgRequest = message;
            mIqRequest = null;
            final Jid from = message.getFrom();
            DomainBareJid bareFrom = from.asDomainBareJid();

            Body bodyExt = message.getExtension(Body.class);
            if (bodyExt != null)
                instruction = bodyExt.getMessage();

            for (HTTPAuthorizationRequestListener listener : incomingListeners) {
                listener.onHTTPAuthorizationRequest(bareFrom, mConfirmExtension);
            }
        }, INCOMING_MESSAGE_FILTER);

        // Handler for IQ HTTP request
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(ConfirmIQ.ELEMENT, ConfirmIQ.NAMESPACE,
                IQ.Type.get, IQRequestHandler.Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                ConfirmIQ iqHttpRequest = (ConfirmIQ) iqRequest;
                mConfirmExtension = iqHttpRequest.getConfirmExtension();

                if (mConfirmExtension == null) {
                    return IQ.createErrorResponse(iqRequest, StanzaError.Condition.bad_request);
                }
                msgRequest = null;
                mIqRequest = iqHttpRequest;
                instruction = null;
                final Jid from = iqHttpRequest.getFrom();
                DomainBareJid bareFrom = from.asDomainBareJid();

                for (HTTPAuthorizationRequestListener listener : incomingListeners) {
                    listener.onHTTPAuthorizationRequest(bareFrom, mConfirmExtension);
                }
                // let us handle the reply
                return null;
            }
        });
    }

    /**
     * Add a new listener for incoming HTTP request via IQ/Message.
     *
     * @param listener the listener to add.
     *
     * @return <code>true</code> if the listener was not already added.
     */
    public boolean addIncomingListener(HTTPAuthorizationRequestListener listener) {
        return incomingListeners.add(listener);
    }

    /**
     * Remove an incoming HTTP Request listener.
     *
     * @param listener the listener to remove.
     *
     * @return <code>true</code> if the listener was active and got removed.
     */
    public boolean removeIncomingListener(HTTPAuthorizationRequestListener listener) {
        return incomingListeners.remove(listener);
    }

    public String getInstruction() {
        return instruction;
    }

    /**
     * Accept the HTTP Authorization Request.
     *
     * The actual reply can be in IQ or Message pending on the Request Stanza
     */
    public void accept() {
        try {
            if (msgRequest != null) {
                MessageBuilder messageAccept = StanzaBuilder.buildMessage()
                        .to(msgRequest.getFrom())
                        .ofType(msgRequest.getType())
                        .setThread(msgRequest.getThread())
                        .addExtension(mConfirmExtension);
                connection().sendStanza(messageAccept.build());
            }
            else if (mIqRequest != null) {
                connection().sendStanza(ConfirmIQ.createAuthRequestAccept(mIqRequest));
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to send http authorization accept: ", e.getMessage());
        }
    }

    /**
     * Reject the HTTP Authorization Request.
     *
     * The actual reply can be in IQ or Message pending on the Request Stanza
     */
    public void reject() {
        StanzaError stanzaError = StanzaError.getBuilder()
                .setType(StanzaError.Type.AUTH)
                .setCondition(StanzaError.Condition.not_authorized).build();

        try {
            if (msgRequest != null) {
                MessageBuilder messageDeny = StanzaBuilder.buildMessage()
                        .to(msgRequest.getFrom())
                        .ofType(Message.Type.error)
                        .setThread(msgRequest.getThread())
                        .addExtension(mConfirmExtension)
                        .addExtension(stanzaError);
                connection().sendStanza(messageDeny.build());
            }
            else if (mIqRequest != null) {
                IQ iqDeny =  ConfirmIQ.createAuthRequestDeny(mIqRequest, stanzaError);
                connection().sendStanza(iqDeny);
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to send http authorization reject: ", e.getMessage());
        }
    }
}
