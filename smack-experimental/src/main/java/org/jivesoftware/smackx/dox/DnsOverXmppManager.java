/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.dox;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smack.packet.StanzaError.Type;
import org.jivesoftware.smack.util.RandomUtil;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.dox.element.DnsIq;

import org.jxmpp.jid.Jid;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;

public final class DnsOverXmppManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(DnsOverXmppManager.class.getName());

    private static final Map<XMPPConnection, DnsOverXmppManager> INSTANCES = new WeakHashMap<>();

    public static synchronized DnsOverXmppManager getInstanceFor(XMPPConnection connection) {
        DnsOverXmppManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new DnsOverXmppManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private static final String NAMESPACE = DnsIq.NAMESPACE;

    private static DnsOverXmppResolver defaultResolver;

    public void setDefaultDnsOverXmppResolver(DnsOverXmppResolver resolver) {
        defaultResolver = resolver;
    }

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private DnsOverXmppResolver resolver = defaultResolver;

    private boolean enabled;

    private final AbstractIqRequestHandler dnsIqRequestHandler = new AbstractIqRequestHandler(
            DnsIq.ELEMENT, DnsIq.NAMESPACE, IQ.Type.get, Mode.async) {

        @Override
        public IQ handleIQRequest(IQ iqRequest) {
            DnsOverXmppResolver resolver = DnsOverXmppManager.this.resolver;
            if (resolver == null) {
                LOGGER.info("Resolver was null while attempting to handle " + iqRequest);
                return null;
            }

            DnsIq dnsIqRequest = (DnsIq) iqRequest;
            DnsMessage query = dnsIqRequest.getDnsMessage();

            DnsMessage response;
            try {
                response = resolver.resolve(query);
            } catch (IOException exception) {
                StanzaError.Builder errorBuilder = StanzaError.getBuilder()
                        .setType(Type.CANCEL)
                        .setCondition(Condition.internal_server_error)
                        .setDescriptiveEnText("Exception while resolving your DNS query", exception)
                        ;

                IQ errorResponse = IQ.createErrorResponse(iqRequest, errorBuilder);
                return errorResponse;
            }

            if (query.id != response.id) {
                // The ID may not match because the resolver returned a cached result.
                response = response.asBuilder().setId(query.id).build();
            }

            DnsIq dnsIqResult = new DnsIq(response);
            dnsIqResult.setType(IQ.Type.result);
            return dnsIqResult;
        }
    };

    private DnsOverXmppManager(XMPPConnection connection) {
        super(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    public synchronized void setDnsOverXmppResolver(DnsOverXmppResolver resolver) {
        this.resolver = resolver;
        if (resolver == null) {
            disable();
        }
    }

    public synchronized void enable() {
        if (enabled) return;

        if (resolver == null) {
            throw new IllegalStateException("No DnsOverXmppResolver configured");
        }

        XMPPConnection connection = connection();
        if (connection == null) return;

        connection.registerIQRequestHandler(dnsIqRequestHandler);
        serviceDiscoveryManager.addFeature(NAMESPACE);
    }

    public synchronized void disable() {
        if (!enabled) return;

        XMPPConnection connection = connection();
        if (connection == null) return;

        serviceDiscoveryManager.removeFeature(NAMESPACE);
        connection.unregisterIQRequestHandler(dnsIqRequestHandler);
    }

    public boolean isSupported(Jid jid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return serviceDiscoveryManager.supportsFeature(jid, NAMESPACE);
    }

    public DnsMessage query(Jid jid, Question question) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DnsMessage queryMessage = DnsMessage.builder()
                .addQuestion(question)
                .setId(RandomUtil.nextSecureRandomInt())
                .setRecursionDesired(true)
                .build();
        return query(jid, queryMessage);
    }

    public DnsMessage query(Jid jid, DnsMessage query)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DnsIq queryIq = new DnsIq(query, jid);

        DnsIq responseIq = connection().sendIqRequestAndWaitForResponse(queryIq);

        return responseIq.getDnsMessage();
    }
}
