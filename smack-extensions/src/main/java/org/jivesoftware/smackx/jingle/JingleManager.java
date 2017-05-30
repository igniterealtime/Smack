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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

public final class JingleManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JingleManager.class.getName());

    private static final Map<XMPPConnection, JingleManager> INSTANCES = new WeakHashMap<>();

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

    private JingleManager(XMPPConnection connection) {
        super(connection);

        connection.registerIQRequestHandler(
                        new AbstractIqRequestHandler(Jingle.ELEMENT, Jingle.NAMESPACE, Type.set, Mode.async) {
                            @Override
                            public IQ handleIQRequest(IQ iqRequest) {
                                final Jingle jingle = (Jingle) iqRequest;

                                if (jingle.getContents().isEmpty()) {
                                    Jid from = jingle.getFrom();
                                    assert (from != null);
                                    FullJid fullFrom = from.asFullJidOrThrow();
                                    String sid = jingle.getSid();
                                    FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(fullFrom, sid);
                                    JingleSessionHandler jingleSessionHandler = jingleSessionHandlers.get(fullJidAndSessionId);
                                    if (jingleSessionHandler == null) {
                                        // TODO handle non existing jingle session handler.
                                        return null;
                                    }
                                    return jingleSessionHandler.handleRequest(jingle, sid);
                                }

                                if (jingle.getContents().size() > 1) {
                                    LOGGER.severe("Jingle IQs with more then one content element are currently not supported by Smack");
                                    return null;
                                }

                                JingleContent content = jingle.getContents().get(0);
                                JingleContentDescription description = content.getDescription();
                                JingleHandler jingleDescriptionHandler = descriptionHandlers.get(
                                                description.getNamespace());
                                if (jingleDescriptionHandler == null) {
                                    // TODO handle non existing content description handler.
                                    return null;
                                }
                                return jingleDescriptionHandler.handleRequest(jingle);
                            }
                        });
    }

    public JingleHandler registerDescriptionHandler(String namespace, JingleHandler handler) {
        return descriptionHandlers.put(namespace, handler);
    }

    public JingleSessionHandler registerJingleSessionHandler(FullJid otherJid, String sessionId, JingleSessionHandler sessionHandler) {
        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(otherJid, sessionId);
        return jingleSessionHandlers.put(fullJidAndSessionId, sessionHandler);
    }

    public JingleSessionHandler unregisterJingleSessionhandler(FullJid otherJid, String sessionId, JingleSessionHandler sessionHandler) {
        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(otherJid, sessionId);
        return jingleSessionHandlers.remove(fullJidAndSessionId);
    }

    private static final class FullJidAndSessionId {
        final FullJid fullJid;
        final String sessionId;

        private FullJidAndSessionId(FullJid fullJid, String sessionId) {
            this.fullJid = fullJid;
            this.sessionId = sessionId;
        }

        @Override
        public int hashCode() {
            int hashCode = 31 * fullJid.hashCode();
            hashCode = 31 * hashCode + sessionId.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof FullJidAndSessionId)) {
                return false;
            }
            FullJidAndSessionId otherFullJidAndSessionId = (FullJidAndSessionId) other;
            return fullJid.equals(otherFullJidAndSessionId.fullJid)
                            && sessionId.equals(otherFullJidAndSessionId.sessionId);
        }
    }
}
