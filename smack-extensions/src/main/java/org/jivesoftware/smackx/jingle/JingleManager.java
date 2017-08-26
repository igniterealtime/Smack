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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.exception.UnsupportedDescriptionException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedSecurityException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedTransportException;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle.provider.JingleContentSecurityProvider;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle.util.FullJidAndSessionId;
import org.jivesoftware.smackx.jingle.util.Role;

import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * Manager for Jingle (XEP-0166).
 */
public final class JingleManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JingleManager.class.getName());
    private static final WeakHashMap<XMPPConnection, JingleManager> INSTANCES = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleContentDescriptionProvider}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleContentDescriptionProvider<?>> descriptionProviders = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleContentTransportProvider}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleContentTransportProvider<?>> transportProviders = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleContentSecurityProvider}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleContentSecurityProvider<?>> securityProviders = new WeakHashMap<>();


    /**
     * Map of registered {@link JingleDescriptionAdapter}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleDescriptionAdapter<?>> descriptionAdapters = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleTransportAdapter}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleTransportAdapter<?>> transportAdapters = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleSecurityAdapter}s and their namespaces.
     */
    private static final WeakHashMap<String, JingleSecurityAdapter<?>> securityAdapters = new WeakHashMap<>();


    /**
     * Map of registered {@link JingleDescriptionManager}s and their namespaces.
     */
    private final WeakHashMap<String, JingleDescriptionManager> descriptionManagers = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleTransportManager}s and their namespaces.
     */
    private final WeakHashMap<String, JingleTransportManager> transportManagers = new WeakHashMap<>();

    /**
     * Map of registered {@link JingleSecurityManager}s and their namespaces.
     */
    private final WeakHashMap<String, JingleSecurityManager> securityManagers = new WeakHashMap<>();

    /**
     * Map of active {@link JingleSession}s.
     */
    private final ConcurrentHashMap<FullJidAndSessionId, JingleSession> jingleSessions = new ConcurrentHashMap<>();

    private JingleManager(final XMPPConnection connection) {
        super(connection);

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(JingleElement.ELEMENT, JingleElement.NAMESPACE, IQ.Type.set, IQRequestHandler.Mode.async) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        final JingleElement jingle = (JingleElement) iqRequest;

                        FullJid fullFrom = jingle.getFrom().asFullJidOrThrow();
                        String sid = jingle.getSid();
                        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(fullFrom, sid);

                        JingleSession session = jingleSessions.get(fullJidAndSessionId);

                        // We have not seen this session before.
                        // Either it is fresh, or unknown.
                        if (session == null) {
                            if (jingle.getAction() == JingleAction.session_initiate) {
                                //fresh. phew!
                                try {
                                    LOGGER.log(Level.INFO, "Create new session with " + jingle.getFrom() + ": " + jingle.getSid());
                                    session = JingleSession.fromSessionInitiate(JingleManager.this, jingle);
                                    jingleSessions.put(fullJidAndSessionId, session);
                                } catch (UnsupportedDescriptionException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_applications);
                                } catch (UnsupportedTransportException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_transports);
                                } catch (UnsupportedSecurityException e) {
                                    LOGGER.log(Level.SEVERE, "Unsupported Security: " + e, e);
                                    return null;
                                }

                            } else {
                                // Unknown session. Error!
                                LOGGER.log(Level.INFO, connection().getUser().asFullJidOrThrow() + " received unknown session: " + jingle.getFrom().asFullJidOrThrow() + " " + jingle.getSid());
                                return JingleElement.createJingleErrorUnknownSession(jingle);
                            }
                        }

                        return session.handleJingleRequest(jingle);
                    }
                });
    }

    /**
     * Return connections Instance of the JingleManager.
     * @param connection connection
     * @return connections instance.
     */
    public static JingleManager getInstanceFor(XMPPConnection connection) {
        JingleManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    /**
     * Register a {@link JingleDescriptionAdapter}.
     * @param adapter adapter.
     */
    public static void addJingleDescriptionAdapter(JingleDescriptionAdapter<?> adapter) {
        descriptionAdapters.put(adapter.getNamespace(), adapter);
    }

    /**
     * Register a {@link JingleTransportAdapter}.
     * @param adapter adapter.
     */
    public static void addJingleTransportAdapter(JingleTransportAdapter<?> adapter) {
        transportAdapters.put(adapter.getNamespace(), adapter);
    }

    /**
     * Register a {@link JingleSecurityAdapter}.
     * @param adapter adapter.
     */
    public static void addJingleSecurityAdapter(JingleSecurityAdapter<?> adapter) {
        securityAdapters.put(adapter.getNamespace(), adapter);
    }

    /**
     * Return the registered {@link JingleDescriptionAdapter} with namespace namespace or null.
     * @param namespace namespace.
     * @return adapter or null.
     */
    public static JingleDescriptionAdapter<?> getJingleDescriptionAdapter(String namespace) {
        return descriptionAdapters.get(namespace);
    }

    /**
     * Return the registered {@link JingleTransportAdapter} with namespace namespace or null.
     * @param namespace namespace.
     * @return adapter or null.
     */
    public static JingleTransportAdapter<?> getJingleTransportAdapter(String namespace) {
        return transportAdapters.get(namespace);
    }

    /**
     * Return the registered {@link JingleSecurityAdapter} with namespace namespace or null.
     * @param namespace namespace.
     * @return adapter or null.
     */
    public static JingleSecurityAdapter<?> getJingleSecurityAdapter(String namespace) {
        return securityAdapters.get(namespace);
    }

    /**
     * Add a {@link JingleContentDescriptionProvider}.
     * @param provider provider.
     */
    public static void addJingleDescriptionProvider(JingleContentDescriptionProvider<?> provider) {
        descriptionProviders.put(provider.getNamespace(), provider);
    }

    /**
     * Remove the {@link JingleContentDescriptionProvider} with namespace namespace.
     * @param namespace namespace.
     */
    public static void removeJingleDescriptionProvider(String namespace) {
        descriptionProviders.remove(namespace);
    }

    /**
     * Return the {@link JingleContentDescriptionProvider} with namespace namespace or null.
     * @param namespace namespace.
     * @return provider or null.
     */
    public static JingleContentDescriptionProvider<?> getJingleDescriptionProvider(String namespace) {
        return descriptionProviders.get(namespace);
    }

    /**
     * Add a {@link JingleContentTransportProvider}.
     * @param provider provider.
     */
    public static void addJingleTransportProvider(JingleContentTransportProvider<?> provider) {
        transportProviders.put(provider.getNamespace(), provider);
    }

    /**
     * Remove the {@link JingleContentTransportProvider} with namespace namespace.
     * @param namespace namespace.
     */
    public static void removeJingleTransportProvider(String namespace) {
        transportProviders.remove(namespace);
    }

    /**
     * Return the {@link JingleContentTransportProvider} with namespace namespace or null.
     * @param namespace namespace.
     * @return provider or null.
     */
    public static JingleContentTransportProvider<?> getJingleTransportProvider(String namespace) {
        return transportProviders.get(namespace);
    }

    /**
     * Add a {@link JingleContentSecurityProvider}.
     * @param provider provider.
     */
    public static void addJingleSecurityProvider(JingleContentSecurityProvider<?> provider) {
        securityProviders.put(provider.getNamespace(), provider);
    }

    /**
     * Remove the {@link JingleContentSecurityProvider} with namespace namespace.
     * @param namespace namespace.
     */
    public static void removeJingleSecurityProvider(String namespace) {
        securityProviders.remove(namespace);
    }

    /**
     * Return the {@link JingleContentSecurityProvider} with namespace namespace or null.
     * @param namespace namespace.
     * @return provider or null.
     */
    public static JingleContentSecurityProvider<?> getJingleSecurityProvider(String namespace) {
        return securityProviders.get(namespace);
    }

    /**
     * Add a {@link JingleDescriptionManager}.
     * @param manager manager
     */
    public void addJingleDescriptionManager(JingleDescriptionManager manager) {
        descriptionManagers.put(manager.getNamespace(), manager);
    }

    /**
     * Get the {@link JingleDescriptionManager} with namespace namespace.
     * @param namespace namespace
     * @return manager or null.
     */
    public JingleDescriptionManager getDescriptionManager(String namespace) {
        return descriptionManagers.get(namespace);
    }

    /**
     * Add a {@link JingleTransportManager}.
     * @param manager manager
     */
    public void addJingleTransportManager(JingleTransportManager manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    /**
     * Get the {@link JingleTransportManager} with namespace namespace.
     * @param namespace namespace
     * @return manager or null.
     */
    public JingleTransportManager getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    /**
     * Add a {@link JingleSecurityManager}.
     * @param manager manager
     */
    public void addJingleSecurityManager(JingleSecurityManager manager) {
        securityManagers.put(manager.getNamespace(), manager);
    }

    /**
     * Get the {@link JingleSecurityManager} with namespace namespace.
     * @param namespace namespace
     * @return manager or null.
     */
    public JingleSecurityManager getSecurityManager(String namespace) {
        return securityManagers.get(namespace);
    }

    /**
     * Return a list of all available {@link JingleTransportManager}s that the recipient and we support.
     * @param to recipient
     * @return list of {@link JingleTransportManager}s.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public List<JingleTransportManager> getAvailableTransportManagers(Jid to) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        return getAvailableTransportManagers(to, Collections.<String>emptySet());
    }

    /**
     * Return a list of all available {@link JingleTransportManager}s that the recipient and we support,
     * but exclude all managers whos namespaces are in the except set.
     * @param to recipient
     * @param except blacklist.
     * @return list of {@link JingleTransportManager}s.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public List<JingleTransportManager> getAvailableTransportManagers(Jid to, Set<String> except) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Set<String> available = new HashSet<>(transportManagers.keySet());
        available.removeAll(except);
        List<JingleTransportManager> remaining = new ArrayList<>();

        for (String namespace : available) {
            if (ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(to, namespace)) {
                remaining.add(transportManagers.get(namespace));
            }
        }

        Collections.sort(remaining, new Comparator<JingleTransportManager>() {
            @Override
            public int compare(JingleTransportManager t0, JingleTransportManager t1) {
                return t1.compareTo(t0); //Invert otherwise ascending order to descending.
            }
        });

        return remaining;
    }

    /**
     * Return the best available {@link JingleTransportManager}, which both we and the recipient support.
     * @param to recipient.
     * @return best available {@link JingleTransportManager}.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public JingleTransportManager getBestAvailableTransportManager(Jid to) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        return getBestAvailableTransportManager(to, Collections.<String>emptySet());
    }

    /**
     * Return the best available {@link JingleTransportManager}, which both we and the recipient support,
     * and whichs namespace is not on the blacklist.
     * @param to recipient.
     * @param except blacklist.
     * @return best available {@link JingleTransportManager}.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public JingleTransportManager getBestAvailableTransportManager(Jid to, Set<String> except) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        List<JingleTransportManager> managers = getAvailableTransportManagers(to, except);

        if (managers.size() > 0) {
            return managers.get(0);
        }

        return null;
    }

    /**
     * Return the connection.
     * @return connection.
     */
    public XMPPConnection getConnection() {
        return connection();
    }

    /**
     * Create a new {@link JingleSession} and add it to the list of sessions.
     * Then return it.
     * @param role our role.
     * @param peer peer.
     * @return session.
     */
    public JingleSession createSession(Role role, FullJid peer) {
        JingleSession session;

        if (role == Role.initiator) {
            session = new JingleSession(this, connection().getUser().asFullJidOrThrow(), peer,
                    role, StringUtils.randomString(24));
        } else {
            session = new JingleSession(this, peer, connection().getUser().asFullJidOrThrow(),
                    role, StringUtils.randomString(24));
        }

        jingleSessions.put(new FullJidAndSessionId(peer, session.getSessionId()), session);
        return session;
    }

    /**
     * Add a {@link JingleSession} to the list of active sessions.
     * @param session session.
     */
    public void addSession(JingleSession session) {
        if (!jingleSessions.containsValue(session)) {
            jingleSessions.put(new FullJidAndSessionId(session.getPeer(), session.getSessionId()), session);
        }
    }

    /**
     * Remove a {@link JingleSession} from the list of active sessions.
     * @param session session.
     */
    public void removeSession(JingleSession session) {
        jingleSessions.remove(new FullJidAndSessionId(session.getPeer(), session.getSessionId()));
    }
}
