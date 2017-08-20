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
package org.jivesoftware.smackx.jet;

import java.io.File;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ciphers.Aes256GcmNoPadding;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jet.provider.JetSecurityProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.util.Role;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle Encrypted Transfers (XEP-XXXX).
 */
public final class JetManager extends Manager implements JingleDescriptionManager {

    private static final Logger LOGGER = Logger.getLogger(JetManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JetManager> INSTANCES = new WeakHashMap<>();
    private static final HashMap<String, JingleEnvelopeManager> envelopeManagers = new HashMap<>();
    private static final HashMap<String, ExtensionElementProvider<?>> envelopeProviders = new HashMap<>();

    private final JingleManager jingleManager;

    static {
        JingleManager.addJingleSecurityAdapter(new JetSecurityAdapter());
        JingleManager.addJingleSecurityProvider(new JetSecurityProvider());
    }

    private JetManager(XMPPConnection connection) {
        super(connection);
        this.jingleManager = JingleManager.getInstanceFor(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        jingleManager.addJingleDescriptionManager(this);
    }

    public static JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public OutgoingFileOfferController sendEncryptedFile(File file, FullJid recipient, JingleEnvelopeManager envelopeManager) throws Exception {
        return sendEncryptedFile(file, JingleFile.fromFile(file, null, null, null), recipient, envelopeManager);
    }

    public OutgoingFileOfferController sendEncryptedFile(File file, JingleFile metadata, FullJid recipient, JingleEnvelopeManager envelopeManager) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        throwIfRecipientLacksSupport(recipient);

        JingleSession session = jingleManager.createSession(Role.initiator, recipient);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file, metadata);
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager(recipient);
        content.setTransport(transportManager.createTransportForInitiator(content));

        JetSecurity security = new JetSecurity(envelopeManager, recipient, content.getName(), Aes256GcmNoPadding.NAMESPACE);
        content.setSecurity(security);
        session.sendInitiate(connection());

        return offer;
    }

    public OutgoingFileOfferController sendEncryptedStream(InputStream inputStream, JingleFile metadata, FullJid recipient, JingleEnvelopeManager envelopeManager)
            throws XMPPException.XMPPErrorException, SmackException.FeatureNotSupportedException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            JingleEnvelopeManager.JingleEncryptionException, NoSuchProviderException, InvalidAlgorithmParameterException {

        throwIfRecipientLacksSupport(recipient);
        JingleSession session = jingleManager.createSession(Role.initiator, recipient);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(inputStream, metadata);
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager(recipient);
        content.setTransport(transportManager.createTransportForInitiator(content));

        JetSecurity security = new JetSecurity(envelopeManager, recipient, content.getName(), Aes256GcmNoPadding.NAMESPACE);
        content.setSecurity(security);
        session.sendInitiate(connection());

        return offer;
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
        return JetSecurity.NAMESPACE;
    }

    @Override
    public void notifySessionInitiate(JingleSession session) {
        JingleFileTransferManager.getInstanceFor(connection()).notifySessionInitiate(session);
    }

    @Override
    public void notifyContentAdd(JingleSession session, JingleContent content) {
        JingleFileTransferManager.getInstanceFor(connection()).notifyContentAdd(session, content);
    }

    private void throwIfRecipientLacksSupport(FullJid recipient) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
        if (!ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(recipient, getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), recipient);
        }
    }
}
