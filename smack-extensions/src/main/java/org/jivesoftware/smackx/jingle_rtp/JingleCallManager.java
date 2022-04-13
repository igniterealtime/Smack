/**
 *
 * Copyright 2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_rtp.element.Grouping;
import org.jivesoftware.smackx.jingle_rtp.element.IceUdpTransport;
import org.jivesoftware.smackx.jingle_rtp.element.IceUdpTransportCandidate;
import org.jivesoftware.smackx.jingle_rtp.element.IceUdpTransportRemoteCandidate;
import org.jivesoftware.smackx.jingle_rtp.element.InputEvent;
import org.jivesoftware.smackx.jingle_rtp.element.ParameterElement;
import org.jivesoftware.smackx.jingle_rtp.element.PayloadType;
import org.jivesoftware.smackx.jingle_rtp.element.RawUdpTransport;
import org.jivesoftware.smackx.jingle_rtp.element.RtcpFb;
import org.jivesoftware.smackx.jingle_rtp.element.RtcpMux;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;
import org.jivesoftware.smackx.jingle_rtp.element.RtpExtmap;
import org.jivesoftware.smackx.jingle_rtp.element.RtpHeader;
import org.jivesoftware.smackx.jingle_rtp.element.SdpCrypto;
import org.jivesoftware.smackx.jingle_rtp.element.SdpSource;
import org.jivesoftware.smackx.jingle_rtp.element.SdpTransfer;
import org.jivesoftware.smackx.jingle_rtp.element.SdpTransferred;
import org.jivesoftware.smackx.jingle_rtp.element.SrtpEncryption;
import org.jivesoftware.smackx.jingle_rtp.element.SrtpFingerprint;
import org.jivesoftware.smackx.jingle_rtp.element.ZrtpHash;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPDescriptionProvider;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPTransportProvider;
import org.jxmpp.jid.FullJid;

import java.util.WeakHashMap;

/**
 * Manager for Jingle RTP session i.e.
 * <a href="https://xmpp.org/extensions/xep-0167.html">XEP-0167: Jingle RTP Sessions 1.2.0 (2020-04-22)</a>
 *
 * @author Eng Chong Meng
 */
public final class JingleCallManager extends Manager implements JingleHandler {
    private static final WeakHashMap<XMPPConnection, JingleCallManager> INSTANCES = new WeakHashMap<>();

    /**
     * The <code>BasicTelephony</code> instance which has been used to create calls
     */
    private final BasicTelephony mBasicTelephony;

    public static synchronized JingleCallManager getInstanceFor(XMPPConnection connection, BasicTelephony operation) {
        JingleCallManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleCallManager(connection, operation);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private JingleCallManager(XMPPConnection connection, BasicTelephony basicTelephony) {
        super(connection);
        mBasicTelephony = basicTelephony;

        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());

        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(getNamespace(), this);

        JingleContentProviderManager.addJingleContentDescriptionProvider(RtpDescription.NAMESPACE, new JingleRTPDescriptionProvider());
        JingleContentProviderManager.addJingleContentTransportProvider(IceUdpTransport.NAMESPACE, new JingleRTPTransportProvider());

        /*
         * Register all jingle related extension providers for the RTP media call support.
         * It is the responsibility of the application to register the <code>JingleProvider</code> itself.
         *
         * Note: All sub Elements without its own NAMESPACE use their parent NAMESPACE for provider support (Parser implementation)
         */
        // <payload-type/> provider
        ProviderManager.addExtensionProvider(
                PayloadType.ELEMENT, RtpDescription.NAMESPACE,
                new DefaultXmlElementProvider<>(PayloadType.class, RtpDescription.NAMESPACE));

        // <parameter/> provider - RtpDescription
        ProviderManager.addExtensionProvider(
                ParameterElement.ELEMENT, RtpDescription.NAMESPACE,
                new DefaultXmlElementProvider<>(ParameterElement.class, RtpDescription.NAMESPACE));

        // <parameter/> provider - RtpHeader
        ProviderManager.addExtensionProvider(
                ParameterElement.ELEMENT, RtpHeader.NAMESPACE,
                new DefaultXmlElementProvider<>(ParameterElement.class, RtpHeader.NAMESPACE));

        // <parameter/> provider - SdpSource
        ProviderManager.addExtensionProvider(
                ParameterElement.ELEMENT, SdpSource.NAMESPACE,
                new DefaultXmlElementProvider<>(ParameterElement.class, SdpSource.NAMESPACE));

        // <rtp-hdrext/> provider
        ProviderManager.addExtensionProvider(
                RtpHeader.ELEMENT, RtpHeader.NAMESPACE,
                new DefaultXmlElementProvider<>(RtpHeader.class));

        // <extmap-allow-mixed/> provider
        ProviderManager.addExtensionProvider(
                RtpExtmap.ELEMENT, RtpExtmap.NAMESPACE,
                new DefaultXmlElementProvider<>(RtpExtmap.class));

        // <raw-udp/> provider - RawUdpTransport
        ProviderManager.addExtensionProvider(
                IceUdpTransport.ELEMENT, RawUdpTransport.NAMESPACE,
                new DefaultXmlElementProvider<>(RawUdpTransport.class));

        // ice-udp <candidate/> provider - IceUdpTransportCandidate
        ProviderManager.addExtensionProvider(
                IceUdpTransportCandidate.ELEMENT, IceUdpTransportCandidate.NAMESPACE,
                new DefaultXmlElementProvider<>(IceUdpTransportCandidate.class));

        // raw-udp <candidate/> provider - RawUdpTransport
        ProviderManager.addExtensionProvider(
                IceUdpTransportCandidate.ELEMENT, RawUdpTransport.NAMESPACE,
                new DefaultXmlElementProvider<>(IceUdpTransportCandidate.class));

        // ice-udp <remote-candidate/> provider - IceUdpTransportRemoteCandidate
        ProviderManager.addExtensionProvider(
                IceUdpTransportRemoteCandidate.ELEMENT, IceUdpTransportRemoteCandidate.NAMESPACE,
                new DefaultXmlElementProvider<>(IceUdpTransportRemoteCandidate.class));

        // rtcp-mux => XEP-0167: Jingle RTP Sessions
        ProviderManager.addExtensionProvider(
                RtcpMux.ELEMENT, RtpDescription.NAMESPACE,
                new DefaultXmlElementProvider<>(RtcpMux.class, RtpDescription.NAMESPACE));

        // rtcp-mux =>  Multiplexing RTP Data and Control Packets on a Single Port (April 2010)
        // https://tools.ietf.org/html/rfc5761#section-5.1.3 (5.1.3. Interactions with ICE)
        ProviderManager.addExtensionProvider(
                RtcpMux.ELEMENT, IceUdpTransport.NAMESPACE,
                new DefaultXmlElementProvider<>(RtcpMux.class, IceUdpTransport.NAMESPACE));

        // <encryption/> provider
        ProviderManager.addExtensionProvider(
                SrtpEncryption.ELEMENT, SrtpEncryption.NAMESPACE,
                new DefaultXmlElementProvider<>(SrtpEncryption.class));

        // <zrtp-hash/> provider
        ProviderManager.addExtensionProvider(
                ZrtpHash.ELEMENT, ZrtpHash.NAMESPACE,
                new DefaultXmlElementProvider<>(ZrtpHash.class));

        // <crypto/> provider
        ProviderManager.addExtensionProvider(
                SdpCrypto.ELEMENT, RtpDescription.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpCrypto.class));

        // <group/> provider
        ProviderManager.addExtensionProvider(
                Grouping.ELEMENT, Grouping.NAMESPACE,
                new DefaultXmlElementProvider<>(Grouping.class));

        // <group/> sub-element <content/>
        ProviderManager.addExtensionProvider(
                JingleContent.ELEMENT, Grouping.NAMESPACE,
                new DefaultXmlElementProvider<>(JingleContent.class));

        // Jitsi inputevent <inputevt/> provider
        ProviderManager.addExtensionProvider(
                InputEvent.ELEMENT, InputEvent.NAMESPACE,
                new DefaultXmlElementProvider<>(InputEvent.class));

        // DTLS-SRTP
        ProviderManager.addExtensionProvider(
                SrtpFingerprint.ELEMENT, SrtpFingerprint.NAMESPACE,
                new DefaultXmlElementProvider<>(SrtpFingerprint.class));

        // XEP-0251: Jingle Session Transfer <transfer/> and <transferred> providers
        ProviderManager.addExtensionProvider(
                SdpTransfer.ELEMENT, SdpTransfer.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpTransfer.class));

        ProviderManager.addExtensionProvider(
                SdpTransferred.ELEMENT, SdpTransferred.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpTransferred.class));

        // rtcp-fb
        ProviderManager.addExtensionProvider(
                RtcpFb.ELEMENT, RtcpFb.NAMESPACE,
                new DefaultXmlElementProvider<>(RtcpFb.class));
    }

    /**
     * Register a new JingleSessionHandler with JingleManager when a new session-initiate is received.
     * Note: this will not get call if the media call setup is via JingleMessage protocol
     *
     * @param jingle Jingle session-initiate
     * @return IQ.Result for ack
     */
    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        // see <a href="https://xmpp.org/extensions/xep-0166.html#def">XEP-0166 Jingle#7. Formal Definition</a>
        FullJid initiator = jingle.getInitiator();
        if (initiator == null) {
            // conversations excludes initiator attribute in session-initiate
            initiator = jingle.getFrom().asEntityFullJidIfPossible();
        }
        final JingleCallSessionImpl session = new JingleCallSessionImpl(connection(), initiator, jingle.getSid(),
                jingle.getContents(), mBasicTelephony);
        Async.go(() -> mBasicTelephony.handleJingleSession(jingle, session));
        return IQ.createResultIQ(jingle);
    }

    public String getNamespace() {
        return RtpDescription.NAMESPACE;
    }
}
