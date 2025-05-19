/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jet;

import java.io.File;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.WeakHashMap;

import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ciphers.Aes256GcmNoPadding;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jet.component.JetSecurityImpl;
import org.jivesoftware.smackx.jet.provider.JetSecurityProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.component.JingleContentImpl;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle Encrypted Transfers (XEP-0391).
 * @see <a href="https://xmpp.org/extensions/xep-0391.html">XEP-0391: Jingle Encrypted Transports 0.1.2 (2018-07-31))</a>
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public final class JetManager extends Manager implements JingleDescriptionManager, JingleHandler {
    private static final WeakHashMap<XMPPConnection, JetManager> INSTANCES = new WeakHashMap<>();

    private static final HashMap<String, JingleEnvelopeManager> envelopeManagers = new HashMap<>();

    private static final HashMap<String, ExtensionElementProvider<?>> envelopeProviders = new HashMap<>();

    private final XMPPConnection mConnection;

    private JetManager(XMPPConnection connection) {
        super(connection);
        mConnection = connection;
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());

        JingleContentProviderManager.addJingleContentSecurityProvider(getNamespace(), new JetSecurityProvider());
        JingleContentProviderManager.addJingleSecurityAdapter(new JetSecurityAdapter());

        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(getNamespace(), this);
        JingleContentProviderManager.addJingleDescriptionManager(this);
    }

    public static synchronized JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Register a new JingleSessionHandler with JingleManager when a new session-initiate is received.
     *
     * @param jingle Jingle session-initiate
     * @return IQ.Result
     */
    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        // see <a href="https://xmpp.org/extensions/xep-0166.html#def">XEP-0166 Jingle#7. Formal Definition</a>
        // conversations excludes initiator attribute in session-initiate
        FullJid initiator = jingle.getInitiator();
        if (initiator == null) {
            initiator = jingle.getFrom().asEntityFullJidIfPossible();
        }

        JingleSessionImpl session = new JingleSessionImpl(mConnection, initiator, jingle);
        return session.handleJingleSessionRequest(jingle);
    }

    public OutgoingFileOfferController sendEncryptedFile(File file, FullJid recipient, JingleEnvelopeManager envelopeManager) throws Exception {
        return sendEncryptedFile(file, JingleFile.fromFile(file, null, null, null), recipient, envelopeManager);
    }

    public OutgoingFileOfferController sendEncryptedFile(File file, JingleFile metadata, FullJid recipient, JingleEnvelopeManager envelopeManager) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        throwIfRecipientLacksSupport(recipient);

        JingleSessionImpl session = new JingleSessionImpl(mConnection, recipient);

        JingleContentImpl content
                = new JingleContentImpl(mConnection, JingleContent.Creator.initiator, JingleContent.Senders.initiator);
        session.addContentImpl(content);

        JingleOutgoingFileOffer outgoingFileOffer = new JingleOutgoingFileOffer(session, file, metadata);
        content.setDescription(outgoingFileOffer);

        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getBestAvailableTransportManager(mConnection);
        JingleTransport<?> transport = transportManager.createTransportForInitiator(content);
        content.setTransport(transport);

        JetSecurityImpl security = new JetSecurityImpl(envelopeManager, recipient, content.getName(), Aes256GcmNoPadding.NAMESPACE);
        content.setSecurity(security);

        session.sendInitiate(connection());
        return outgoingFileOffer;
    }

    public OutgoingFileOfferController sendEncryptedStream(final InputStream inputStream, JingleFile metadata, FullJid recipient, JingleEnvelopeManager envelopeManager)
            throws XMPPException.XMPPErrorException, SmackException.FeatureNotSupportedException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            JingleEnvelopeManager.JingleEncryptionException, NoSuchProviderException, InvalidAlgorithmParameterException {

        throwIfRecipientLacksSupport(recipient);

        JingleSessionImpl session = new JingleSessionImpl(mConnection, recipient);
        JingleContentImpl content
                = new JingleContentImpl(mConnection, JingleContent.Creator.initiator, JingleContent.Senders.initiator);
        session.addContentImpl(content);

        JingleOutgoingFileOffer outgoingFileOffer = new JingleOutgoingFileOffer(session, inputStream, metadata);
        content.setDescription(outgoingFileOffer);

        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getBestAvailableTransportManager(mConnection);
        JingleTransport<?> transport = transportManager.createTransportForInitiator(content);
        content.setTransport(transport);

        JetSecurityImpl security = new JetSecurityImpl(envelopeManager, recipient, content.getName(), Aes256GcmNoPadding.NAMESPACE);
        content.setSecurity(security);

        session.sendInitiate(connection());
        return outgoingFileOffer;
    }

    public void registerEnvelopeManager(JingleEnvelopeManager method) {
        envelopeManagers.put(method.getJingleEnvelopeNamespace(), method);
    }

    public void unregisterEnvelopeManager(String namespace) {
        envelopeManagers.remove(namespace);
    }

    public JingleEnvelopeManager getEnvelopeManager(String namespace) {
        return envelopeManagers.get(namespace);
    }

    public static void registerEnvelopeProvider(String namespace, ExtensionElementProvider<?> provider) {
        envelopeProviders.put(namespace, provider);
    }

    public static void unregisterEnvelopeProvider(String namespace) {
        envelopeProviders.remove(namespace);
    }

    public static ExtensionElementProvider<?> getEnvelopeProvider(String namespace) {
        return envelopeProviders.get(namespace);
    }

    @Override
    public String getNamespace() {
        return JetSecurityImpl.NAMESPACE;
    }

    @Override
    public void notifySessionInitiate(JingleSessionImpl session) {
        JingleFileTransferManager.getInstanceFor(connection()).notifySessionInitiate(session);
    }

    @Override
    public void notifyContentAdd(JingleSessionImpl session, JingleContentImpl content) {
        JingleFileTransferManager.getInstanceFor(connection()).notifyContentAdd(session, content);
    }

    private void throwIfRecipientLacksSupport(FullJid recipient) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
        if (!ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(recipient, getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), recipient);
        }
    }
}
