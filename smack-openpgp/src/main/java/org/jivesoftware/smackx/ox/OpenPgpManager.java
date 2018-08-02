/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil.PEP_NODE_PUBLIC_KEYS;
import static org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil.PEP_NODE_PUBLIC_KEYS_NOTIFY;
import static org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil.publishPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.callback.backup.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyBackupSelectionCallback;
import org.jivesoftware.smackx.ox.crypto.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.NoBackupFoundException;
import org.jivesoftware.smackx.ox.listener.CryptElementReceivedListener;
import org.jivesoftware.smackx.ox.listener.SignElementReceivedListener;
import org.jivesoftware.smackx.ox.listener.SigncryptElementReceivedListener;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;
import org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil;
import org.jivesoftware.smackx.ox.util.SecretKeyBackupHelper;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubFeature;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.collection.PGPKeyRing;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.BCUtil;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Entry point for Smacks API for OpenPGP for XMPP.
 *
 * <h2>Setup</h2>
 *
 * In order to use OpenPGP for XMPP in Smack, just follow the following procedure.<br>
 * <br>
 * First, acquire an instance of the {@link OpenPgpManager} for your {@link XMPPConnection} using
 * {@link #getInstanceFor(XMPPConnection)}.
 *
 * <pre>
 * {@code
 * OpenPgpManager openPgpManager = OpenPgpManager.getInstanceFor(connection);
 * }
 * </pre>
 *
 * You also need an {@link OpenPgpProvider}, as well as an {@link OpenPgpStore}.
 * The provider must be registered using {@link #setOpenPgpProvider(OpenPgpProvider)}.
 *
 * <pre>
 * {@code
 * OpenPgpStore store = new FileBasedOpenPgpStore(storePath);
 * OpenPgpProvider provider = new PainlessOpenPgpProvider(connection, store);
 * openPgpManager.setOpenPgpProvider(provider);
 * }
 * </pre>
 *
 * It is also advised to register a custom {@link SecretKeyRingProtector} using
 * {@link OpenPgpStore#setKeyRingProtector(SecretKeyRingProtector)} in order to be able to handle password protected
 * secret keys.<br>
 * <br>
 * Speaking of keys, you can now check, if you have any keys available in your {@link OpenPgpStore} by doing
 * {@link #hasSecretKeysAvailable()}.<br>
 * <br>
 * If you do, you can now announce support for OX and publish those keys using {@link #announceSupportAndPublish()}.<br>
 * <br>
 * Otherwise, you can either generate fresh keys using {@link #generateAndImportKeyPair(BareJid)},
 * or try to restore a secret key backup from your private PubSub node by doing
 * {@link #restoreSecretKeyServerBackup(AskForBackupCodeCallback)}.<br>
 * <br>
 * In any case you should still do an {@link #announceSupportAndPublish()} afterwards.
 * <br>
 * <br>
 * Contacts are represented by {@link OpenPgpContact}s in the context of OpenPGP for XMPP. You can get those by using
 * {@link #getOpenPgpContact(EntityBareJid)}. The main function of {@link OpenPgpContact}s is to bundle information
 * about the OpenPGP capabilities of a contact in one spot. The pendant to the {@link OpenPgpContact} is the
 * {@link OpenPgpSelf}, which encapsulates your own OpenPGP identity. Both classes can be used to acquire information
 * about the OpenPGP keys of a user.
 *
 * <h2>Elements</h2>
 *
 * OpenPGP for XMPP defines multiple different element classes which contain the users messages.
 * The outermost element is the {@link OpenPgpElement}, which contains an OpenPGP encrypted content element.
 *
 * The content can be either a {@link SignElement}, {@link CryptElement} or {@link SigncryptElement}, depending on the use-case.
 * Those content elements contain the actual payload. If an {@link OpenPgpElement} is decrypted, it will be returned in
 * form of an {@link OpenPgpMessage}, which represents the decrypted message + metadata.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0373.html">
 *     XEP-0373: OpenPGP for XMPP</a>
 */
public final class OpenPgpManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpManager.class.getName());

    /**
     * Map of instances.
     */
    private static final Map<XMPPConnection, OpenPgpManager> INSTANCES = new WeakHashMap<>();

    /**
     * {@link OpenPgpProvider} responsible for processing keys, encrypting and decrypting messages and so on.
     */
    private OpenPgpProvider provider;

    private final Set<SigncryptElementReceivedListener> signcryptElementReceivedListeners = new HashSet<>();
    private final Set<SignElementReceivedListener> signElementReceivedListeners = new HashSet<>();
    private final Set<CryptElementReceivedListener> cryptElementReceivedListeners = new HashSet<>();

    /**
     * Private constructor to avoid instantiation without putting the object into {@code INSTANCES}.
     *
     * @param connection xmpp connection.
     */
    private OpenPgpManager(XMPPConnection connection) {
        super(connection);
        ChatManager.getInstanceFor(connection).addIncomingListener(incomingOpenPgpMessageListener);
    }

    /**
     * Get the instance of the {@link OpenPgpManager} which belongs to the {@code connection}.
     *
     * @param connection xmpp connection.
     * @return instance of the manager.
     */
    public static OpenPgpManager getInstanceFor(XMPPConnection connection) {
        OpenPgpManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OpenPgpManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Return our own {@link BareJid}.
     *
     * @return our bareJid
     *
     * @throws SmackException.NotLoggedInException in case our connection is not logged in, which means our BareJid is unknown.
     */
    public BareJid getJidOrThrow() throws SmackException.NotLoggedInException {
        throwIfNotAuthenticated();
        return connection().getUser().asEntityBareJidOrThrow();
    }

    /**
     * Set the {@link OpenPgpProvider} which will be used to process incoming OpenPGP elements,
     * as well as to execute cryptographic operations.
     *
     * @param provider OpenPgpProvider.
     */
    public void setOpenPgpProvider(OpenPgpProvider provider) {
        this.provider = provider;
    }

    public OpenPgpProvider getOpenPgpProvider() {
        return provider;
    }

    /**
     * Get our OpenPGP self.
     *
     * @return self
     * @throws SmackException.NotLoggedInException if we are not logged in
     */
    public OpenPgpSelf getOpenPgpSelf() throws SmackException.NotLoggedInException {
        throwIfNoProviderSet();
        return new OpenPgpSelf(getJidOrThrow(), provider.getStore());
    }

    /**
     * Generate a fresh OpenPGP key pair, given we don't have one already.
     * Publish the public key to the Public Key Node and update the Public Key Metadata Node with our keys fingerprint.
     * Lastly register a {@link PEPListener} which listens for updates to Public Key Metadata Nodes.
     *
     * @throws NoSuchAlgorithmException if we are missing an algorithm to generate a fresh key pair.
     * @throws NoSuchProviderException if we are missing a suitable {@link java.security.Provider}.
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if one of the PubSub nodes is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws IOException IO is dangerous.
     * @throws InvalidAlgorithmParameterException if illegal algorithm parameters are used for key generation.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     * @throws PGPException if something goes wrong during key loading/generating
     */
    public void announceSupportAndPublish()
            throws NoSuchAlgorithmException, NoSuchProviderException, InterruptedException,
            PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, IOException,
            InvalidAlgorithmParameterException, SmackException.NotLoggedInException, PGPException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();

        OpenPgpV4Fingerprint primaryFingerprint = getOurFingerprint();

        if (primaryFingerprint == null) {
            primaryFingerprint = generateAndImportKeyPair(getJidOrThrow());
        }

        // Create <pubkey/> element
        PubkeyElement pubkeyElement;
        try {
            pubkeyElement = createPubkeyElement(getJidOrThrow(), primaryFingerprint, new Date());
        } catch (MissingOpenPgpKeyException e) {
            throw new AssertionError("Cannot publish our public key, since it is missing (MUST NOT happen!)");
        }

        // publish it
        publishPublicKey(connection(), pubkeyElement, primaryFingerprint);

        // Subscribe to public key changes
        PEPManager.getInstanceFor(connection()).addPEPListener(metadataListener);
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(PEP_NODE_PUBLIC_KEYS_NOTIFY);
    }

    /**
     * Generate a fresh OpenPGP key pair and import it.
     *
     * @param ourJid our {@link BareJid}.
     * @return {@link OpenPgpV4Fingerprint} of the generated key.
     * @throws NoSuchAlgorithmException if the JVM doesn't support one of the used algorithms.
     * @throws InvalidAlgorithmParameterException if the used algorithm parameters are invalid.
     * @throws NoSuchProviderException if we are missing a cryptographic provider.
     * @throws PGPException PGP is brittle.
     * @throws IOException IO is dangerous.
     */
    public OpenPgpV4Fingerprint generateAndImportKeyPair(BareJid ourJid)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException,
            PGPException, IOException {

        throwIfNoProviderSet();
        OpenPgpStore store = provider.getStore();
        PGPKeyRing keys = store.generateKeyRing(ourJid);
        try {
            store.importSecretKey(ourJid, keys.getSecretKeys());
            store.importPublicKey(ourJid, keys.getPublicKeys());
        } catch (MissingUserIdOnKeyException e) {
            // This should never throw, since we set our jid literally one line above this comment.
            throw new AssertionError(e);
        }

        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(keys.getSecretKeys());

        store.setTrust(ourJid, fingerprint, OpenPgpTrustStore.Trust.trusted);

        return fingerprint;
    }

    /**
     * Return the upper-case hex encoded OpenPGP v4 fingerprint of our key pair.
     *
     * @return fingerprint.
     * @throws SmackException.NotLoggedInException in case we are not logged in.
     * @throws IOException IO is dangerous.
     * @throws PGPException PGP is brittle.
     */
    public OpenPgpV4Fingerprint getOurFingerprint()
            throws SmackException.NotLoggedInException, IOException, PGPException {
        return getOpenPgpSelf().getSigningKeyFingerprint();
    }

    /**
     * Return an OpenPGP capable contact.
     * This object can be used as an entry point to OpenPGP related API.
     *
     * @param jid {@link BareJid} of the contact.
     * @return {@link OpenPgpContact}.
     */
    public OpenPgpContact getOpenPgpContact(EntityBareJid jid) {
        throwIfNoProviderSet();
        return provider.getStore().getOpenPgpContact(jid);
    }

    /**
     * Return true, if we have a secret key available, otherwise false.
     *
     * @return true if secret key available
     *
     * @throws SmackException.NotLoggedInException If we are not logged in (we need to know our jid in order to look up
     * our keys in the key store.
     * @throws PGPException in case the keys in the store are damaged somehow.
     * @throws IOException IO is dangerous.
     */
    public boolean hasSecretKeysAvailable() throws SmackException.NotLoggedInException, PGPException, IOException {
        throwIfNoProviderSet();
        return getOpenPgpSelf().hasSecretKeyAvailable();
    }

    /**
     * Determine, if we can sync secret keys using private PEP nodes as described in the XEP.
     * Requirements on the server side are support for PEP and support for the whitelist access model of PubSub.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @param connection XMPP connection
     * @return true, if the server supports secret key backups, otherwise false.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread is interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     */
    public static boolean serverSupportsSecretKeyBackups(XMPPConnection connection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection)
                .serverSupportsFeature(PubSubFeature.access_whitelist.toString());
    }

    /**
     * Remove the metadata listener. This method is mainly used in tests.
     */
    public void stopMetadataListener() {
        PEPManager.getInstanceFor(connection()).removePEPListener(metadataListener);
    }

    /**
     * Upload the encrypted secret key to a private PEP node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @param displayCodeCallback callback, which will receive the backup password used to encrypt the secret key.
     * @param selectKeyCallback callback, which will receive the users choice of which keys will be backed up.
     * @throws InterruptedException if the thread is interrupted.
     * @throws PubSubException.NotALeafNodeException if the private node is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     * @throws IOException IO is dangerous.
     * @throws SmackException.FeatureNotSupportedException if the server doesn't support the PubSub whitelist access model.
     * @throws PGPException PGP is brittle
     * @throws MissingOpenPgpKeyException in case we have no OpenPGP key pair to back up.
     */
    public void backupSecretKeyToServer(DisplayBackupCodeCallback displayCodeCallback,
                                        SecretKeyBackupSelectionCallback selectKeyCallback)
            throws InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            SmackException.NotLoggedInException, IOException,
            SmackException.FeatureNotSupportedException, PGPException, MissingOpenPgpKeyException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();

        BareJid ownJid = connection().getUser().asBareJid();

        String backupCode = SecretKeyBackupHelper.generateBackupPassword();

        PGPSecretKeyRingCollection secretKeyRings = provider.getStore().getSecretKeysOf(ownJid);

        Set<OpenPgpV4Fingerprint> availableKeyPairs = new HashSet<>();
        for (PGPSecretKeyRing ring : secretKeyRings) {
            availableKeyPairs.add(new OpenPgpV4Fingerprint(ring));
        }

        Set<OpenPgpV4Fingerprint> selectedKeyPairs = selectKeyCallback.selectKeysToBackup(availableKeyPairs);

        SecretkeyElement secretKey = SecretKeyBackupHelper.createSecretkeyElement(provider, ownJid, selectedKeyPairs, backupCode);

        OpenPgpPubSubUtil.depositSecretKey(connection(), secretKey);
        displayCodeCallback.displayBackupCode(backupCode);
    }

    /**
     * Delete the private {@link LeafNode} containing our secret key backup.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread gets interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     */
    public void deleteSecretKeyServerBackup()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, SmackException.NotLoggedInException {
        throwIfNotAuthenticated();
        OpenPgpPubSubUtil.deleteSecretKeyNode(connection());
    }

    /**
     * Fetch a secret key backup from the server and try to restore a selected secret key from it.
     *
     * @param codeCallback callback for prompting the user to provide the secret backup code.
     * @return fingerprint of the restored secret key
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if the private node is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws InvalidBackupCodeException if the user-provided backup code is invalid.
     * @throws SmackException.NotLoggedInException if we are not logged in
     * @throws IOException IO is dangerous
     * @throws MissingUserIdOnKeyException if the key that is to be imported is missing a user-id with our jid
     * @throws NoBackupFoundException if no secret key backup has been found
     * @throws PGPException in case the restored secret key is damaged.
     */
    public OpenPgpV4Fingerprint restoreSecretKeyServerBackup(AskForBackupCodeCallback codeCallback)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException,
            InvalidBackupCodeException, SmackException.NotLoggedInException, IOException, MissingUserIdOnKeyException,
            NoBackupFoundException, PGPException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();
        SecretkeyElement backup = OpenPgpPubSubUtil.fetchSecretKey(connection());
        if (backup == null) {
            throw new NoBackupFoundException();
        }

        String backupCode = codeCallback.askForBackupCode();

        PGPSecretKeyRing secretKeys = SecretKeyBackupHelper.restoreSecretKeyBackup(backup, backupCode);
        provider.getStore().importSecretKey(getJidOrThrow(), secretKeys);
        provider.getStore().importPublicKey(getJidOrThrow(), BCUtil.publicKeyRingFromSecretKeyRing(secretKeys));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(2048);
        for (PGPSecretKey sk : secretKeys) {
            PGPPublicKey pk = sk.getPublicKey();
            if (pk != null) pk.encode(buffer);
        }
        PGPPublicKeyRing publicKeys = new PGPPublicKeyRing(buffer.toByteArray(), new BcKeyFingerprintCalculator());
        provider.getStore().importPublicKey(getJidOrThrow(), publicKeys);

        return new OpenPgpV4Fingerprint(secretKeys);
    }

    /*
    Private stuff.
     */

    /**
     * {@link PEPListener} that listens for changes to the OX public keys metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>
     */
    private final PEPListener metadataListener = new PEPListener() {
        @Override
        public void eventReceived(final EntityBareJid from, final EventElement event, final Message message) {
            if (PEP_NODE_PUBLIC_KEYS.equals(event.getEvent().getNode())) {
                final BareJid contact = from.asBareJid();
                LOGGER.log(Level.INFO, "Received OpenPGP metadata update from " + contact);
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        ItemsExtension items = (ItemsExtension) event.getExtensions().get(0);
                        PayloadItem<?> payload = (PayloadItem) items.getItems().get(0);
                        PublicKeysListElement listElement = (PublicKeysListElement) payload.getPayload();

                        processPublicKeysListElement(from, listElement);
                    }
                }, "ProcessOXMetadata");
            }
        }
    };

    private void processPublicKeysListElement(BareJid contact, PublicKeysListElement listElement) {
        OpenPgpContact openPgpContact = getOpenPgpContact(contact.asEntityBareJidIfPossible());
        try {
            openPgpContact.updateKeys(connection(), listElement);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not update contacts keys", e);
        }
    }

    /**
     * Decrypt and or verify an {@link OpenPgpElement} and return the decrypted {@link OpenPgpMessage}.
     *
     * @param element {@link OpenPgpElement} containing the message.
     * @param sender {@link OpenPgpContact} who sent the message.
     *
     * @return decrypted and/or verified message
     *
     * @throws SmackException.NotLoggedInException in case we aren't logged in (we need to know our jid)
     * @throws IOException IO error (reading keys, streams etc)
     * @throws PGPException in case of an PGP error
     */
    public OpenPgpMessage decryptOpenPgpElement(OpenPgpElement element, OpenPgpContact sender)
            throws SmackException.NotLoggedInException, IOException, PGPException {
        return provider.decryptAndOrVerify(element, getOpenPgpSelf(), sender);
    }

    private final IncomingChatMessageListener incomingOpenPgpMessageListener =
            new IncomingChatMessageListener() {
                @Override
                public void newIncomingMessage(final EntityBareJid from, final Message message, Chat chat) {
                    Async.go(new Runnable() {
                        @Override
                        public void run() {
                            OpenPgpElement element = message.getExtension(OpenPgpElement.ELEMENT, OpenPgpElement.NAMESPACE);
                            if (element == null) {
                                // Message does not contain an OpenPgpElement -> discard
                                return;
                            }

                            OpenPgpContact contact = getOpenPgpContact(from);

                            OpenPgpMessage decrypted = null;
                            OpenPgpContentElement contentElement = null;
                            try {
                                decrypted = decryptOpenPgpElement(element, contact);
                                contentElement = decrypted.getOpenPgpContentElement();
                            } catch (PGPException e) {
                                LOGGER.log(Level.WARNING, "Could not decrypt incoming OpenPGP encrypted message", e);
                            } catch (XmlPullParserException | IOException e) {
                                LOGGER.log(Level.WARNING, "Invalid XML content of incoming OpenPGP encrypted message", e);
                            } catch (SmackException.NotLoggedInException e) {
                                LOGGER.log(Level.WARNING, "Cannot determine our JID, since we are not logged in.", e);
                            }

                            if (contentElement instanceof SigncryptElement) {
                                for (SigncryptElementReceivedListener l : signcryptElementReceivedListeners) {
                                    l.signcryptElementReceived(contact, message, (SigncryptElement) contentElement, decrypted.getMetadata());
                                }
                                return;
                            }

                            if (contentElement instanceof SignElement) {
                                for (SignElementReceivedListener l : signElementReceivedListeners) {
                                    l.signElementReceived(contact, message, (SignElement) contentElement, decrypted.getMetadata());
                                }
                                return;
                            }

                            if (contentElement instanceof CryptElement) {
                                for (CryptElementReceivedListener l : cryptElementReceivedListeners) {
                                    l.cryptElementReceived(contact, message, (CryptElement) contentElement, decrypted.getMetadata());
                                }
                                return;
                            }

                            else {
                                throw new AssertionError("Invalid element received: " + contentElement.getClass().getName());
                            }
                        }
                    });
                }
            };

    /**
     * Create a {@link PubkeyElement} which contains the OpenPGP public key of {@code owner} which belongs to
     * the {@link OpenPgpV4Fingerprint} {@code fingerprint}.
     *
     * @param owner owner of the public key
     * @param fingerprint fingerprint of the key
     * @param date date of creation of the element
     * @return {@link PubkeyElement} containing the key
     *
     * @throws MissingOpenPgpKeyException if the public key notated by the fingerprint cannot be found
     */
    private PubkeyElement createPubkeyElement(BareJid owner,
                                              OpenPgpV4Fingerprint fingerprint,
                                              Date date)
            throws MissingOpenPgpKeyException, IOException, PGPException {
        PGPPublicKeyRing ring = provider.getStore().getPublicKeyRing(owner, fingerprint);
        if (ring != null) {
            byte[] keyBytes = ring.getEncoded(true);
            return createPubkeyElement(keyBytes, date);
        }
        throw new MissingOpenPgpKeyException(owner, fingerprint);
    }

    /**
     * Create a {@link PubkeyElement} which contains the given {@code data} base64 encoded.
     *
     * @param bytes byte representation of an OpenPGP public key
     * @param date date of creation of the element
     * @return {@link PubkeyElement} containing the key
     */
    private static PubkeyElement createPubkeyElement(byte[] bytes, Date date) {
        return new PubkeyElement(new PubkeyElement.PubkeyDataElement(Base64.encode(bytes)), date);
    }

    /**
     * Register a {@link SigncryptElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link SigncryptElement} has been received and successfully decrypted.
     *
     * Note: This method is not intended for clients to listen for incoming {@link SigncryptElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    public void registerSigncryptReceivedListener(SigncryptElementReceivedListener listener) {
        signcryptElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link SigncryptElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link SigncryptElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterSigncryptElementReceivedListener(SigncryptElementReceivedListener listener) {
        signcryptElementReceivedListeners.remove(listener);
    }

    /**
     * Register a {@link SignElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link SignElement} has been received and successfully verified.
     *
     * Note: This method is not intended for clients to listen for incoming {@link SignElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    void registerSignElementReceivedListener(SignElementReceivedListener listener) {
        signElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link SignElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link SignElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterSignElementReceivedListener(SignElementReceivedListener listener) {
        signElementReceivedListeners.remove(listener);
    }

    /**
     * Register a {@link CryptElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link CryptElement} has been received and successfully decrypted.
     *
     * Note: This method is not intended for clients to listen for incoming {@link CryptElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    void registerCryptElementReceivedListener(CryptElementReceivedListener listener) {
        cryptElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link CryptElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link CryptElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterCryptElementReceivedListener(CryptElementReceivedListener listener) {
        cryptElementReceivedListeners.remove(listener);
    }

    /**
     * Throw an {@link IllegalStateException} if no {@link OpenPgpProvider} is set.
     * The OpenPgpProvider is used to process information related to RFC-4880.
     */
    private void throwIfNoProviderSet() {
        if (provider == null) {
            throw new IllegalStateException("No OpenPgpProvider set!");
        }
    }

    /**
     * Throw a {@link org.jivesoftware.smack.SmackException.NotLoggedInException} if the {@link XMPPConnection} of this
     * manager is not authenticated at this point.
     *
     * @throws SmackException.NotLoggedInException if we are not authenticated
     */
    private void throwIfNotAuthenticated() throws SmackException.NotLoggedInException {
        if (!connection().isAuthenticated()) {
            throw new SmackException.NotLoggedInException();
        }
    }
}
