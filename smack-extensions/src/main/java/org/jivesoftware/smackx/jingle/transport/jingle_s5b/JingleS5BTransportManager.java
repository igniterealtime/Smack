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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.provider.JingleS5BTransportProvider;

import org.jxmpp.jid.Jid;

/**
 * Manager for Jingle SOCKS5 Bytestream transports (XEP-0261).
 */
public final class JingleS5BTransportManager extends Manager implements JingleTransportManager {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportManager.class.getName());
    private final Socks5BytestreamManager socks5BytestreamManager = Socks5BytestreamManager.getBytestreamManager(connection());

    private static final WeakHashMap<XMPPConnection, JingleS5BTransportManager> INSTANCES = new WeakHashMap<>();

    private List<Bytestream.StreamHost> localStreamHosts = null;
    private List<Bytestream.StreamHost> availableStreamHosts = null;

    public static boolean useLocalCandidates = true;
    public static boolean useExternalCandidates = true;

    static {
        JingleManager.addJingleTransportAdapter(new JingleS5BTransportAdapter());
        JingleManager.addJingleTransportProvider(new JingleS5BTransportProvider());
    }

    private JingleS5BTransportManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.addJingleTransportManager(this);

        connection.addConnectionListener(connectionListener);
    }

    public static JingleS5BTransportManager getInstanceFor(XMPPConnection connection) {
        JingleS5BTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleS5BTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }

    @Override
    public JingleTransport<?> createTransportForInitiator(JingleContent content) {
        JingleSession session = content.getParent();
        String sid = StringUtils.randomString(24);
        List<JingleTransportCandidate<?>> candidates = collectCandidates();
        return new JingleS5BTransport(session.getInitiator(), session.getResponder(), sid, Bytestream.Mode.tcp, candidates);
    }

    @Override
    public JingleTransport<?> createTransportForResponder(JingleContent content, JingleTransport<?> peersTransport) {
        JingleSession session = content.getParent();
        return new JingleS5BTransport(session.getInitiator(), session.getResponder(), collectCandidates(), (JingleS5BTransport) peersTransport);
    }

    @Override
    public JingleTransport<?> createTransportForResponder(JingleContent content, JingleContentTransportElement peersTransportElement) {
        JingleS5BTransport other = new JingleS5BTransportAdapter().transportFromElement(peersTransportElement);
        return createTransportForResponder(content, other);
    }

    @Override
    public int getPriority() {
        return 10000; // SOCKS5 has a high priority
    }

    List<JingleTransportCandidate<?>> collectCandidates() {
        List<JingleTransportCandidate<?>> candidates = new ArrayList<>();

        //Local host
        if (JingleS5BTransportManager.isUseLocalCandidates()) {
            for (Bytestream.StreamHost host : getLocalStreamHosts()) {
                candidates.add(new JingleS5BTransportCandidate(StringUtils.randomString(16), host, 100, JingleS5BTransportCandidateElement.Type.proxy));
            }
        }

        List<Bytestream.StreamHost> remoteHosts = Collections.emptyList();
        if (JingleS5BTransportManager.isUseExternalCandidates()) {
            try {
                remoteHosts = getServersStreamHosts();
            } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                LOGGER.log(Level.WARNING, "Could not determine available StreamHosts.", e);
            }
        }

        for (Bytestream.StreamHost host : remoteHosts) {
            candidates.add(new JingleS5BTransportCandidate(StringUtils.randomString(16), host, 0, JingleS5BTransportCandidateElement.Type.proxy));
        }

        return candidates;
    }

    private List<Bytestream.StreamHost> queryServersStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        List<Jid> proxies = socks5BytestreamManager.determineProxies();
        return determineStreamHostInfo(proxies);
    }

    private List<Bytestream.StreamHost> queryLocalStreamHosts() {
        return socks5BytestreamManager.getLocalStreamHost();
    }

    private List<Bytestream.StreamHost> getServersStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (availableStreamHosts == null) {
            availableStreamHosts = queryServersStreamHosts();
        }
        return availableStreamHosts;
    }

    private List<Bytestream.StreamHost> getLocalStreamHosts() {
        if (localStreamHosts == null) {
            localStreamHosts = queryLocalStreamHosts();
        }
        return localStreamHosts;
    }

    private List<Bytestream.StreamHost> determineStreamHostInfo(List<Jid> proxies) {
        List<Bytestream.StreamHost> streamHosts = new ArrayList<>();

        Iterator<Jid> iterator = proxies.iterator();
        while (iterator.hasNext()) {
            Jid proxy = iterator.next();
            Bytestream request = new Bytestream();
            request.setType(IQ.Type.get);
            request.setTo(proxy);

            try {
                Bytestream response = connection().createStanzaCollectorAndSend(request).nextResultOrThrow();
                streamHosts.addAll(response.getStreamHosts());
            }
            catch (Exception e) {
                iterator.remove();
            }
        }

        return streamHosts;
    }

    private static JingleElement createTransportInfo(JingleS5BTransport transport, JingleS5BTransportInfoElement info) {
        JingleContent content = transport.getParent();
        JingleSession session = content.getParent();

        JingleElement.Builder jb = JingleElement.getBuilder()
                .setSessionId(session.getSessionId())
                .setAction(JingleAction.transport_info);

        if (session.isInitiator()) {
            jb.setInitiator(session.getInitiator());
        } else {
            jb.setResponder(session.getResponder());
        }

        JingleContentElement.Builder cb = JingleContentElement.getBuilder()
                .setCreator(content.getCreator())
                .setName(content.getName())
                .setSenders(content.getSenders());

        JingleS5BTransportElement.Builder tb = JingleS5BTransportElement.getBuilder()
                .setTransportInfo(info)
                .setStreamId(transport.getStreamId());

        JingleElement jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setFrom(session.getOurJid());
        jingle.setTo(session.getPeer());

        return jingle;
    }

    static JingleElement createCandidateUsed(JingleS5BTransport transport, JingleS5BTransportCandidate candidate) {
        return createTransportInfo(transport, new JingleS5BTransportInfoElement.CandidateUsed(candidate.getCandidateId()));
    }

    static JingleElement createCandidateError(JingleS5BTransport transport) {
        return createTransportInfo(transport, JingleS5BTransportInfoElement.CandidateError.INSTANCE);
    }

    static JingleElement createProxyError(JingleS5BTransport transport) {
        return createTransportInfo(transport, JingleS5BTransportInfoElement.ProxyError.INSTANCE);
    }

    static JingleElement createCandidateActivated(JingleS5BTransport transport, JingleS5BTransportCandidate candidate) {
        return createTransportInfo(transport, new JingleS5BTransportInfoElement.CandidateActivated(candidate.getCandidateId()));
    }

    public static void setUseLocalCandidates(boolean localCandidates) {
        JingleS5BTransportManager.useLocalCandidates = localCandidates;
    }

    public static void setUseExternalCandidates(boolean externalCandidates) {
        JingleS5BTransportManager.useExternalCandidates = externalCandidates;
    }

    public static boolean isUseLocalCandidates() {
        return useLocalCandidates;
    }

    public static boolean isUseExternalCandidates() {
        return useExternalCandidates;
    }

    @Override
    public int compareTo(JingleTransportManager other) {
        return getPriority() > other.getPriority() ? 1 : -1;
    }

    private final ConnectionListener connectionListener = new ConnectionListener() {

        @Override
        public void connected(XMPPConnection connection) {
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            if (connection.equals(connection())) {
                try {
                    Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
                    if (!socks5Proxy.isRunning()) {
                        socks5Proxy.start();
                    }
                    localStreamHosts = queryLocalStreamHosts();
                    availableStreamHosts = queryServersStreamHosts();
                } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    LOGGER.log(Level.WARNING, "Could not query available StreamHosts: " + e, e);
                }
            }
        }

        @Override
        public void connectionClosed() {
            Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
            if (proxy.isRunning()) {
                Socks5Proxy.getSocks5Proxy().stop();
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
        }

        @Override
        public void reconnectionSuccessful() {
        }

        @Override
        public void reconnectingIn(int seconds) {
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }
    };
}
