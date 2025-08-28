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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleContentImpl;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.provider.JingleS5BTransportProvider;

import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * Manager for Jingle SOCKS5 Bytestream transports (XEP-0260).
 */
public final class JingleS5BTransportManager extends JingleTransportManager<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportManager.class.getName());
    private final Socks5BytestreamManager s5Manager = Socks5BytestreamManager.getBytestreamManager(connection());

    private static final WeakHashMap<XMPPConnection, JingleS5BTransportManager> INSTANCES = new WeakHashMap<>();

    private List<Bytestream.StreamHost> localStreamHosts = null;
    private List<Bytestream.StreamHost> availableStreamHosts = null;

    private static boolean useLocalCandidates = true;
    private static boolean useExternalCandidates = true;

    private JingleS5BTransportManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentTransportProvider(getNamespace(), new JingleS5BTransportProvider());
        JingleContentProviderManager.addJingleTransportAdapter(new JingleS5BTransportAdapter());
    }

    public static synchronized JingleS5BTransportManager getInstanceFor(XMPPConnection connection) {
        JingleS5BTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleS5BTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
    }

    @Override
    public JingleTransportSession<JingleS5BTransport> transportSession(JingleSession jingleSession) {
        return new JingleS5BTransportSession(jingleSession);
    }

    @Override
    public int getPriority() {
        return 10000; // SOCKS5 has a high priority
    }

    @Override
    public JingleTransport<?> createTransportForInitiator(JingleContentImpl content) {
        JingleSessionImpl session = content.getParent();
        String sid = StringUtils.randomString(24);
        List<JingleTransportCandidate<?>> candidates = collectCandidates();
        return new JingleS5BTransportImpl(session.getInitiator(), session.getResponder(), sid, Bytestream.Mode.tcp, candidates);
    }

    @Override
    public JingleTransport<?> createTransportForResponder(JingleContentImpl content, JingleContentTransport peersTransport) {
        JingleS5BTransportImpl other = new JingleS5BTransportAdapter().transportFromElement(peersTransport);
        JingleSessionImpl session = content.getParent();
        return new JingleS5BTransportImpl(session.getInitiator(), session.getResponder(), collectCandidates(), other);
    }

    List<JingleTransportCandidate<?>> collectCandidates() {
        List<JingleTransportCandidate<?>> candidates = new ArrayList<>();
        // per XEP-0260: Jingle SOCKS5 Bytestreams Transport Method (2.2 Exchanging Candidates):
        // priority = (2^16)*(type preference) + (local preference); (2^16) = 65536

        // Local host
        int priority = 65536 * JingleS5BTransportCandidate.Type.direct.getWeight() + 100;
        if (JingleS5BTransportManager.isUseLocalCandidates()) {
            for (Bytestream.StreamHost host : getLocalStreamHosts()) {
                candidates.add(new JingleS5BTransportCandidateImpl(StringUtils.randomString(16), host, priority, JingleS5BTransportCandidate.Type.direct));
            }
        }

        List<Bytestream.StreamHost> remoteHosts = Collections.emptyList();
        if (JingleS5BTransportManager.isUseExternalCandidates()) {
            try {
                remoteHosts = getAvailableStreamHosts();
            } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                LOGGER.log(Level.WARNING, "Could not determine available StreamHosts.", e);
            }
        }

        // proxy server
        priority = 65536 * JingleS5BTransportCandidate.Type.proxy.getWeight();
        for (Bytestream.StreamHost host : remoteHosts) {
            candidates.add(new JingleS5BTransportCandidateImpl(StringUtils.randomString(16), host, priority, JingleS5BTransportCandidate.Type.proxy));
        }

        return candidates;
    }

    private static Jingle createTransportInfo(JingleS5BTransportImpl transport, JingleS5BTransportInfo info) {
        JingleContentImpl content = transport.getParent();
        JingleSessionImpl session = content.getParent();

        Jingle.Builder jb = Jingle.builder(session.getConnection())
                .setSessionId(session.getSessionId())
                .setAction(JingleAction.transport_info);

        if (session.isInitiator()) {
            jb.setInitiator(session.getInitiator());
        }
        else {
            jb.setResponder(session.getResponder());
        }

        JingleContent.Builder cb = JingleContent.getBuilder()
                .setCreator(content.getCreator())
                .setName(content.getName())
                .setSenders(content.getSenders());

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder()
                .setTransportInfo(info)
                .setStreamId(transport.getStreamId());

        Jingle jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setFrom(session.getLocal());
        jingle.setTo(session.getRemote());

        return jingle;
    }

    public Jingle createCandidateUsed(JingleS5BTransportImpl transport, JingleS5BTransportCandidateImpl candidate) {
        return createTransportInfo(transport, new JingleS5BTransportInfo.CandidateUsed(candidate.getCandidateId()));
    }

    public Jingle createCandidateError(JingleS5BTransportImpl transport) {
        return createTransportInfo(transport, JingleS5BTransportInfo.CandidateError.INSTANCE);
    }

    public Jingle createProxyError(JingleS5BTransportImpl transport) {
        return createTransportInfo(transport, JingleS5BTransportInfo.ProxyError.INSTANCE);
    }

    public Jingle createCandidateActivated(JingleS5BTransportImpl transport, JingleS5BTransportCandidateImpl candidate) {
        return createTransportInfo(transport, new JingleS5BTransportInfo.CandidateActivated(candidate.getCandidateId()));
    }

    // ================== Smack start ================= //

    private List<Bytestream.StreamHost> queryAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        List<Jid> proxies = s5Manager.determineProxies();
        return determineStreamHostInfo(proxies);
    }

    private List<Bytestream.StreamHost> queryLocalStreamHosts() {
        return s5Manager.getLocalStreamHost();
    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (availableStreamHosts == null) {
            availableStreamHosts = queryAvailableStreamHosts();
        }
        return availableStreamHosts;
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        if (localStreamHosts == null) {
            localStreamHosts = queryLocalStreamHosts();
        }
        return localStreamHosts;
    }

    public List<Bytestream.StreamHost> determineStreamHostInfo(List<Jid> proxies) {
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
            } catch (Exception e) {
                iterator.remove();
            }
        }

        return streamHosts;
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        try {
            Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
            if (!socks5Proxy.isRunning()) {
                socks5Proxy.start();
            }
            localStreamHosts = queryLocalStreamHosts();
            availableStreamHosts = queryAvailableStreamHosts();
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException |
                 XMPPException.XMPPErrorException e) {
            LOGGER.log(Level.WARNING, "Could not query available StreamHosts: " + e, e);
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
        connectionClosed();
    }

    public Jingle createCandidateUsed(FullJid recipient, FullJid initiator, String sessionId, JingleContent.Senders contentSenders,
            JingleContent.Creator contentCreator, String contentName, String streamId,
            String candidateId) {
        Jingle.Builder jb = Jingle.builder(connection());
        jb.setSessionId(sessionId).setInitiator(initiator).setAction(JingleAction.transport_info);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(contentName).setCreator(contentCreator).setSenders(contentSenders);

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setCandidateUsed(candidateId).setStreamId(streamId);

        Jingle jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setFrom(getConnection().getUser().asFullJidOrThrow());
        jingle.setTo(recipient);

        return jingle;
    }

    public Jingle createCandidateError(FullJid remote, FullJid initiator, String sessionId, JingleContent.Senders senders, JingleContent.Creator creator, String name, String streamId) {
        Jingle.Builder jb = Jingle.builder(connection());
        jb.setSessionId(sessionId).setInitiator(initiator).setAction(JingleAction.transport_info);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(name).setCreator(creator).setSenders(senders);

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setCandidateError().setStreamId(streamId);

        Jingle jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setFrom(getConnection().getUser().asFullJidOrThrow());
        jingle.setTo(remote);

        return jingle;
    }

    public Jingle createProxyError(FullJid remote, FullJid initiator, String sessionId,
            JingleContent.Senders senders, JingleContent.Creator creator,
            String name, String streamId) {
        Jingle.Builder jb = Jingle.builder(connection());
        jb.setSessionId(sessionId).setAction(JingleAction.transport_info).setInitiator(initiator);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setSenders(senders).setCreator(creator).setName(name);

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setStreamId(sessionId).setProxyError().setStreamId(streamId);

        Jingle jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setTo(remote);
        jingle.setFrom(getConnection().getUser().asFullJidOrThrow());
        return jingle;
    }

    public Jingle createCandidateActivated(FullJid remote, FullJid initiator, String sessionId,
            JingleContent.Senders senders, JingleContent.Creator creator,
            String name, String streamId, String candidateId) {
        Jingle.Builder jb = Jingle.builder(connection());
        jb.setInitiator(initiator).setSessionId(sessionId).setAction(JingleAction.transport_info);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(name).setCreator(creator).setSenders(senders);

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setStreamId(streamId).setCandidateActivated(candidateId);

        Jingle jingle = jb.addJingleContent(cb.setTransport(tb.build()).build()).build();
        jingle.setFrom(getConnection().getUser().asFullJidOrThrow());
        jingle.setTo(remote);
        return jingle;
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
}
