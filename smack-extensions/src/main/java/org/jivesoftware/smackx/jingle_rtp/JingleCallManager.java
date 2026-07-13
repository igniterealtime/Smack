/*
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

import java.util.WeakHashMap;

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
import org.jivesoftware.smackx.jingle_rtp.element.Parameter;
import org.jivesoftware.smackx.jingle_rtp.element.PayloadType;
import org.jivesoftware.smackx.jingle_rtp.element.RawUdpTransport;
import org.jivesoftware.smackx.jingle_rtp.element.RtcpFb;
import org.jivesoftware.smackx.jingle_rtp.element.RtcpMux;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;
import org.jivesoftware.smackx.jingle_rtp.element.RtpExtmap;
import org.jivesoftware.smackx.jingle_rtp.element.RtpHeader;
import org.jivesoftware.smackx.jingle_rtp.element.SctpMap;
import org.jivesoftware.smackx.jingle_rtp.element.SdpBandwidth;
import org.jivesoftware.smackx.jingle_rtp.element.SdpCrypto;
import org.jivesoftware.smackx.jingle_rtp.element.SdpSource;
import org.jivesoftware.smackx.jingle_rtp.element.SdpSourceGroup;
import org.jivesoftware.smackx.jingle_rtp.element.SdpTransfer;
import org.jivesoftware.smackx.jingle_rtp.element.SdpTransferred;
import org.jivesoftware.smackx.jingle_rtp.element.SrtpEncryption;
import org.jivesoftware.smackx.jingle_rtp.element.SrtpFingerprint;
import org.jivesoftware.smackx.jingle_rtp.element.ZrtpHash;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPDescriptionProvider;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPRawTransportProvider;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPTransportProvider;

import org.jxmpp.jid.FullJid;

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

    public static synchronized JingleCallManager getInstanceFor(XMPPConnection connection, BasicTelephony basicTelephony) {
        JingleCallManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleCallManager(connection, basicTelephony);
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

        /*
         * Register all jingle related extension providers for the RTP media call support.
         * It is the responsibility of the application to register the <code>JingleProvider</code> itself.
         *
         * Note: All sub Elements without its own NAMESPACE use their parent NAMESPACE for provider support (Parser implementation)
         */
        JingleContentProviderManager.addJingleContentDescriptionProvider(
                RtpDescription.NAMESPACE, new JingleRTPDescriptionProvider());
        JingleContentProviderManager.addJingleContentTransportProvider(
                IceUdpTransport.NAMESPACE, new JingleRTPTransportProvider());
        JingleContentProviderManager.addJingleContentTransportProvider(
                RawUdpTransport.NAMESPACE, new JingleRTPRawTransportProvider());

        // Jingle Grouping provider: use default instead of new JingleGroupingProvider());
        ProviderManager.addExtensionProvider(
                Grouping.ELEMENT, Grouping.NAMESPACE,
                new DefaultXmlElementProvider<>(Grouping.class, Grouping.NAMESPACE));

        // <raw-udp/> provider - RawUdpTransport
        ProviderManager.addExtensionProvider(
                IceUdpTransport.ELEMENT, RawUdpTransport.NAMESPACE,
                new DefaultXmlElementProvider<>(RawUdpTransport.class, RawUdpTransport.NAMESPACE));

        // inputevent <inputevt/> provider
        ProviderManager.addExtensionProvider(
                InputEvent.ELEMENT, InputEvent.NAMESPACE,
                new DefaultXmlElementProvider<>(InputEvent.class, InputEvent.NAMESPACE));

        // <group/> sub-element <content/>
        ProviderManager.addExtensionProvider(
                JingleContent.ELEMENT, Grouping.NAMESPACE, new DefaultXmlElementProvider<>(JingleContent.class));

        // <extmap-allow-mixed/> provider
        ProviderManager.addExtensionProvider(
                RtpExtmap.ELEMENT, RtpExtmap.NAMESPACE,
                new DefaultXmlElementProvider<>(RtpExtmap.class, RtpExtmap.NAMESPACE));

        // rtcp-fb
        ProviderManager.addExtensionProvider(
                RtcpFb.ELEMENT, RtcpFb.NAMESPACE,
                new DefaultXmlElementProvider<>(RtcpFb.class, RtcpFb.NAMESPACE));

        // <rtp-hdrext/> provider
        ProviderManager.addExtensionProvider(
                RtpHeader.ELEMENT, RtpHeader.NAMESPACE,
                new DefaultXmlElementProvider<>(RtpHeader.class, RtpHeader.NAMESPACE));

        // SctpMap
        ProviderManager.addExtensionProvider(
                SctpMap.ELEMENT, SctpMap.NAMESPACE,
                new DefaultXmlElementProvider<>(SctpMap.class, SctpMap.NAMESPACE));
        ProviderManager.addExtensionProvider(
                SdpSource.ELEMENT, SdpSource.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpSource.class, SdpSource.NAMESPACE));

        ProviderManager.addExtensionProvider(
                SdpSourceGroup.ELEMENT, SdpSourceGroup.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpSourceGroup.class, SdpSourceGroup.NAMESPACE));

        // <parameter/> provider - SdpSource
        ProviderManager.addExtensionProvider(
                SdpSource.ELEMENT, SdpSource.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpSource.class, SdpSource.NAMESPACE));

        // <zrtp-hash/> provider
        ProviderManager.addExtensionProvider(
                ZrtpHash.ELEMENT, ZrtpHash.NAMESPACE,
                new DefaultXmlElementProvider<>(ZrtpHash.class, ZrtpHash.NAMESPACE));

        // XEP-0251: Jingle Session Transfer <transfer/> and <transferred> providers
        ProviderManager.addExtensionProvider(
                SdpTransfer.ELEMENT, SdpTransfer.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpTransfer.class, SdpTransfer.NAMESPACE));

        ProviderManager.addExtensionProvider(
                SdpTransferred.ELEMENT, SdpTransferred.NAMESPACE,
                new DefaultXmlElementProvider<>(SdpTransferred.class, SdpTransferred.NAMESPACE));

        // DTLS-SRTP
        ProviderManager.addExtensionProvider(
                SrtpFingerprint.ELEMENT, SrtpFingerprint.NAMESPACE,
                new DefaultXmlElementProvider<>(SrtpFingerprint.class, SrtpFingerprint.NAMESPACE));

        // ============ NamedElement Provider ===================== //
        // ice-udp <candidate/> provider - IceUdpTransportCandidate
        JingleContentProviderManager.addJingleContentELementProvider(IceUdpTransportCandidate.ELEMENT,
                new DefaultElementProvider<>(IceUdpTransportCandidate.class, IceUdpTransportCandidate.ELEMENT));

        // ice-udp <remote-candidate/> provider - IceUdpTransportRemoteCandidate
        JingleContentProviderManager.addJingleContentELementProvider(IceUdpTransportRemoteCandidate.ELEMENT,
                new DefaultElementProvider<>(IceUdpTransportRemoteCandidate.class, IceUdpTransportRemoteCandidate.ELEMENT));

        // <parameter/> provider
        JingleContentProviderManager.addJingleContentELementProvider(Parameter.ELEMENT,
                new DefaultElementProvider<>(Parameter.class, Parameter.ELEMENT));

        JingleContentProviderManager.addJingleContentELementProvider(PayloadType.ELEMENT,
                new DefaultElementProvider<>(PayloadType.class, PayloadType.ELEMENT));

        // raw-udp <candidate/> provider - RawUdpTransport
        // JingleContentProviderManager.addJingleContentELementProvider(
        //        RawUdpTransport.ELEMENT, new DefaultElementProvider<>(IceUdpTransportCandidate.class));

        // rtcp-mux => XEP-0167: Jingle RTP Sessions
        // Multiplexing RTP Data and Control Packets on a Single Port (April 2010)
        // https://tools.ietf.org/html/rfc5761#section-5.1.3 (5.1.3. Interactions with ICE)
        JingleContentProviderManager.addJingleContentELementProvider(RtcpMux.ELEMENT,
                new DefaultElementProvider<>(RtcpMux.class, RtcpMux.ELEMENT));

        // SdpBandwidth
        JingleContentProviderManager.addJingleContentELementProvider(SdpBandwidth.ELEMENT,
                new DefaultElementProvider<>(SdpBandwidth.class, SdpBandwidth.ELEMENT));

        // <crypto/> provider
        JingleContentProviderManager.addJingleContentELementProvider(SdpCrypto.ELEMENT,
                new DefaultElementProvider<>(SdpCrypto.class, SdpCrypto.ELEMENT));

        // <encryption/> provider
        JingleContentProviderManager.addJingleContentELementProvider(SrtpEncryption.ELEMENT,
                new DefaultElementProvider<>(SrtpEncryption.class, SrtpEncryption.ELEMENT));
    }

    /**
     * Register a new JingleSessionHandler with JingleManager when a new session-initiate is received.
     * Note: this will not get call if the media call setup is via JingleMessage protocol;
     * Media call <code>transfer</code> is handled via this callback
     *
     * @param jingle Jingle session-initiate
     *
     * @return IQ.Result for ack
     */
    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        // see <a href="https://xmpp.org/extensions/xep-0166.html#def">XEP-0166 Jingle#7. Formal Definition</a>
        // conversations excludes initiator attribute in session-initiate
        FullJid initiator = jingle.getInitiator();
        if (initiator == null) {
            initiator = jingle.getFrom().asEntityFullJidIfPossible();
        }
        final JingleCallSessionImpl session = new JingleCallSessionImpl(connection(), initiator, jingle.getSid(),
                jingle.getContents(), mBasicTelephony);
        Async.go(() -> mBasicTelephony.handleJingleCallSession(session, jingle));
        return IQ.createResultIQ(jingle);
    }

    public String getNamespace() {
        return RtpDescription.NAMESPACE;
    }
}
