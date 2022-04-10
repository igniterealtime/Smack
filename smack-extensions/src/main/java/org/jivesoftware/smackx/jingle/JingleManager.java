/**
 *
 * Copyright 2017 Florian Schmaus
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

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.JingleS5BTransportManager;
import org.jxmpp.jid.FullJid;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class JingleManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JingleManager.class.getName());

    private static final Map<XMPPConnection, JingleManager> INSTANCES = new WeakHashMap<>();

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    public static synchronized JingleManager getInstanceFor(XMPPConnection connection) {
        JingleManager jingleManager = INSTANCES.get(connection);
        if (jingleManager == null) {
            jingleManager = new JingleManager(connection);
            INSTANCES.put(connection, jingleManager);
        }
        return jingleManager;
    }

    private final Map<String, JingleHandler> descriptionHandlers = new ConcurrentHashMap<>();

    private final Map<FullJidAndSessionId, JingleSessionHandler> jingleSessionHandlers = new ConcurrentHashMap<>();

    private final JingleUtil jutil;

    private JingleManager(XMPPConnection connection) {
        super(connection);

        jutil = new JingleUtil(connection);

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(Jingle.ELEMENT, Jingle.NAMESPACE, Type.set, Mode.async) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        final Jingle jingle = (Jingle) iqRequest;

                        FullJid fullFrom = jingle.getFrom().asFullJidOrThrow();
                        String sid = jingle.getSid();
                        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(fullFrom, sid);

                        JingleSessionHandler sessionHandler = jingleSessionHandlers.get(fullJidAndSessionId);
                        if (sessionHandler != null) {
                            // Handle existing session
                            return sessionHandler.handleJingleSessionRequest(jingle);
                        }

                        if (jingle.getAction() == JingleAction.session_initiate) {

                            JingleContent content = jingle.getContents().get(0);
                            JingleContentDescription description = content.getDescription();
                            JingleHandler jingleDescriptionHandler
                                    = descriptionHandlers.get(description.getNamespace());

                            if (jingleDescriptionHandler == null) {
                                // Unsupported Application
                                LOGGER.warning("Unsupported Jingle application: (" + iqRequest.getStanzaId() + ") " + sid);
                                return jutil.createSessionTerminateUnsupportedApplications(fullFrom, sid);
                            }
                            return jingleDescriptionHandler.handleJingleRequest(jingle);
                        }

                        // Unknown session
                        LOGGER.warning("Unknown session: (" + iqRequest.getStanzaId() + ") " + sid);
                        return jutil.createErrorUnknownSession(jingle);
                    }
                });
        // Register transports.
        JingleTransportMethodManager transportMethodManager = JingleTransportMethodManager.getInstanceFor(connection);
        transportMethodManager.registerTransportManager(JingleIBBTransportManager.getInstanceFor(connection));
        transportMethodManager.registerTransportManager(JingleS5BTransportManager.getInstanceFor(connection));
    }

    public JingleHandler registerDescriptionHandler(String namespace, JingleHandler handler) {
        return descriptionHandlers.put(namespace, handler);
    }

    public JingleSessionHandler registerJingleSessionHandler(FullJid otherJid, String sessionId, JingleSessionHandler sessionHandler) {
        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(otherJid, sessionId);
        return jingleSessionHandlers.put(fullJidAndSessionId, sessionHandler);
    }

    public JingleSessionHandler unregisterJingleSessionHandler(FullJid otherJid, String sessionId, JingleSessionHandler sessionHandler) {
        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(otherJid, sessionId);
        return jingleSessionHandlers.remove(fullJidAndSessionId);
    }

    public static String randomId() {
        return StringUtils.randomString(24);
    }
}
