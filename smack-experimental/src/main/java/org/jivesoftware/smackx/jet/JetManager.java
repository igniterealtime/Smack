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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
 * @see <a href="https://geekplace.eu/xeps/xep-jet/xep-jet.html">Proto-XEP</a>
 */
public final class JetManager extends Manager implements JingleDescriptionManager {

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

    /**
     * Return an instance of the JetManager for the given connection.
     * @param connection connection.
     * @return instance.
     */
    public static JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    /**
     * Send a file to recipient, JET encrypted using the method described by envelopeManager.
     * @param file file
     * @param recipient recipient
     * @param envelopeManager {@link JingleEnvelopeManager} (eg. OmemoManager).
     * @return controller for the outgoing file transfer.
     */
    public OutgoingFileOfferController sendEncryptedFile(File file, FullJid recipient, JingleEnvelopeManager envelopeManager)
            throws IOException, NoSuchAlgorithmException, SmackException.FeatureNotSupportedException,
            InvalidKeyException, InterruptedException, XMPPException.XMPPErrorException, NoSuchPaddingException,
            JingleEnvelopeManager.JingleEncryptionException, SmackException.NotConnectedException,
            SmackException.NoResponseException, NoSuchProviderException, InvalidAlgorithmParameterException {
        return sendEncryptedFile(file, JingleFile.fromFile(file, null, null, null), recipient, envelopeManager);
    }

    /**
     * Send a file to recipient, JET encrypted using the method described by envelopeManager.
     * @param file file containing data.
     * @param metadata custom metadata about the file (like alternative filename...)
     * @param recipient recipient
     * @param envelopeManager {@link JingleEnvelopeManager} (eg. OmemoManager).
     * @return controller for the outgoing file transfer.
     */
    public OutgoingFileOfferController sendEncryptedFile(File file, JingleFile metadata, FullJid recipient, JingleEnvelopeManager envelopeManager)
            throws FileNotFoundException, SmackException.FeatureNotSupportedException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException, XMPPException.XMPPErrorException, NoSuchPaddingException,
            JingleEnvelopeManager.JingleEncryptionException, SmackException.NotConnectedException,
            SmackException.NoResponseException, NoSuchProviderException, InvalidAlgorithmParameterException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        return sendEncryptedStream(new FileInputStream(file), metadata, recipient, envelopeManager);
    }

    /**
     * Send the content of an InputStream to recipient, JET encrypted via the method described by envelopeManager.
     * @param inputStream InputStream with data.
     * @param metadata metadata about the inputstream (filename etc).
     * @param recipient recipient
     * @param envelopeManager {@link JingleEnvelopeManager} (eg. OmemoManager).
     * @return controller for the outgoing file transfer.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.FeatureNotSupportedException Recipient does not support JET or needed Jingle features.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws JingleEnvelopeManager.JingleEncryptionException JET encryption failed.
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
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

    /**
     * Register an {@link JingleEnvelopeManager}.
     * @param method manager.
     */
    public void registerEnvelopeManager(JingleEnvelopeManager method) {
        envelopeManagers.put(method.getJingleEnvelopeNamespace(), method);
    }

    /**
     * Unregister an {@link JingleEnvelopeManager}.
     * @param namespace namespace of the manager.
     */
    public void unregisterEnvelopeManager(String namespace) {
        envelopeManagers.remove(namespace);
    }

    /**
     * Return an {@link JingleEnvelopeManager} for the given namespace.
     * @param namespace namespace.
     * @return manager or null.
     */
    public JingleEnvelopeManager getEnvelopeManager(String namespace) {
        return envelopeManagers.get(namespace);
    }

    /**
     * Register an {@link ExtensionElementProvider} for an envelope element.
     * @param namespace namespace.
     * @param provider provider.
     */
    public static void registerEnvelopeProvider(String namespace, ExtensionElementProvider<?> provider) {
        envelopeProviders.put(namespace, provider);
    }

    /**
     * Unregister an {@link ExtensionElementProvider} for an envelope element.
     * @param namespace namespace.
     */
    public static void unregisterEnvelopeProvider(String namespace) {
        envelopeProviders.remove(namespace);
    }

    /**
     * Return an {@link ExtensionElementProvider} for an envelope element with the given namespace.
     * @param namespace namespace.
     * @return provider.
     */
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

    /**
     * Throw a {@link org.jivesoftware.smack.SmackException.FeatureNotSupportedException} when recipient doesn't support JET.
     * @param recipient recipient.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.FeatureNotSupportedException
     */
    private void throwIfRecipientLacksSupport(FullJid recipient) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
        if (!ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(recipient, getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), recipient);
        }
    }
}
