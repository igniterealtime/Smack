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
package org.jivesoftware.smackx.omemo;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYLENGTH;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYTYPE;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoElement_VAxolotl;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoIdentityKeyException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.StaleDeviceException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.exceptions.UntrustedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.listener.OmemoCarbonCopyStanzaReceivedListener;
import org.jivesoftware.smackx.omemo.internal.listener.OmemoMessageStanzaReceivedListener;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.OmemoTrustCallback;
import org.jivesoftware.smackx.omemo.trust.TrustState;
import org.jivesoftware.smackx.omemo.util.MessageOrOmemoMessage;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

/**
 * This class contains OMEMO related logic and registers listeners etc.
 *
 * @param <T_IdKeyPair> IdentityKeyPair class
 * @param <T_IdKey>     IdentityKey class
 * @param <T_PreKey>    PreKey class
 * @param <T_SigPreKey> SignedPreKey class
 * @param <T_Sess>      Session class
 * @param <T_Addr>      Address class
 * @param <T_ECPub>     Elliptic Curve PublicKey class
 * @param <T_Bundle>    Bundle class
 * @param <T_Ciph>      Cipher class
 * @author Paul Schaub
 */
public abstract class OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        implements OmemoCarbonCopyStanzaReceivedListener, OmemoMessageStanzaReceivedListener {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected static final Logger LOGGER = Logger.getLogger(OmemoService.class.getName());

    private static final long MILLIS_PER_HOUR = 1000L * 60 * 60;

    /**
     * This is a singleton.
     */
    private static OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> INSTANCE;

    private OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore;
    private final HashMap<OmemoManager, OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>> omemoRatchets = new HashMap<>();

    /**
     * Create a new OmemoService object. This should only happen once.
     */
    protected OmemoService() {
    }

    /**
     * Return the singleton instance of this class. When no instance is set, throw an IllegalStateException instead.
     * @return instance.
     */
    public static OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("No OmemoService registered");
        }
        return INSTANCE;
    }

    /**
     * Set singleton instance. Throws an IllegalStateException, if there is already a service set as instance.
     *
     * @param omemoService instance
     */
    protected static void setInstance(OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> omemoService) {
        if (INSTANCE != null) {
            throw new IllegalStateException("An OmemoService is already registered");
        }
        INSTANCE = omemoService;
    }

    /**
     * Returns true, if an instance of the service singleton is set. Otherwise return false.
     *
     * @return true, if instance is not null.
     */
    public static boolean isServiceRegistered() {
        return INSTANCE != null;
    }

    /**
     * Return the used omemoStore backend.
     * If there is no store backend set yet, set the default one (typically a file-based one).
     *
     * @return omemoStore backend
     */
    public OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoStoreBackend() {
        if (omemoStore == null) {
            omemoStore = createDefaultOmemoStoreBackend();
        }
        return omemoStore;
    }

    /**
     * Set an omemoStore as backend. Throws an IllegalStateException, if there is already a backend set.
     *
     * @param omemoStore store.
     */
    @SuppressWarnings("unused")
    public void setOmemoStoreBackend(
            OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore) {
        if (this.omemoStore != null) {
            throw new IllegalStateException("An OmemoStore backend has already been set.");
        }
        this.omemoStore = omemoStore;
    }

    /**
     * Create a default OmemoStore object.
     *
     * @return default omemoStore.
     */
    protected abstract OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createDefaultOmemoStoreBackend();

    /**
     * Return a new instance of the OMEMO ratchet.
     * The ratchet is internally used to encrypt/decrypt message keys.
     *
     * @param manager OmemoManager
     * @param store OmemoStore
     * @return instance of the OmemoRatchet
     */
    protected abstract OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    instantiateOmemoRatchet(OmemoManager manager,
                            OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store);

    /**
     * Return the deposited instance of the OmemoRatchet for the given manager.
     * If there is none yet, create a new one, deposit it and return it.
     *
     * @param manager OmemoManager we want to have the ratchet for.
     * @return OmemoRatchet instance
     */
    protected OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoRatchet(OmemoManager manager) {
        OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                omemoRatchet = omemoRatchets.get(manager);
        if (omemoRatchet == null) {
            omemoRatchet = instantiateOmemoRatchet(manager, omemoStore);
            omemoRatchets.put(manager, omemoRatchet);
        }
        return omemoRatchet;
    }

    /**
     * Instantiate and deposit a Ratchet for the given OmemoManager.
     *
     * @param manager manager.
     */
    void registerRatchetForManager(OmemoManager manager) {
        omemoRatchets.put(manager, instantiateOmemoRatchet(manager, getOmemoStoreBackend()));
    }

    /**
     * Initialize OMEMO functionality for OmemoManager omemoManager.
     *
     * @param managerGuard OmemoManager we'd like to initialize.
     * @throws InterruptedException
     * @throws CorruptedOmemoKeyException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     * @throws PubSubException.NotALeafNodeException
     */
    void init(OmemoManager.LoggedInOmemoManager managerGuard)
            throws InterruptedException, CorruptedOmemoKeyException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException,
            PubSubException.NotALeafNodeException {

        OmemoManager manager = managerGuard.get();
        OmemoDevice userDevice = manager.getOwnDevice();

        // Create new keys if necessary and publish to the server.
        getOmemoStoreBackend().replenishKeys(userDevice);

        // Rotate signed preKey if necessary.
        if (shouldRotateSignedPreKey(userDevice)) {
            getOmemoStoreBackend().changeSignedPreKey(userDevice);
        }

        // Pack and publish bundle
        OmemoBundleElement bundle = getOmemoStoreBackend().packOmemoBundle(userDevice);
        publishBundle(manager.getConnection(), userDevice, bundle);

        // Fetch device list and republish deviceId if necessary
        refreshAndRepublishDeviceList(manager.getConnection(), userDevice);
    }

    /**
     * Create an empty OMEMO message, which is used to forward the ratchet of the recipient.
     * This message type is typically used to create stable sessions.
     * Note that trust decisions are ignored for the creation of this message.
     *
     * @param managerGuard Logged in OmemoManager
     * @param contactsDevice OmemoDevice of the contact
     * @return ratchet update message
     * @throws NoSuchAlgorithmException if AES algorithms are not supported on this system.
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws CorruptedOmemoKeyException if our IdentityKeyPair is corrupted.
     * @throws SmackException.NotConnectedException
     * @throws CannotEstablishOmemoSessionException if session negotiation fails.
     */
    OmemoElement createRatchetUpdateElement(OmemoManager.LoggedInOmemoManager managerGuard,
                                            OmemoDevice contactsDevice)
            throws InterruptedException, SmackException.NoResponseException, CorruptedOmemoKeyException,
            SmackException.NotConnectedException, CannotEstablishOmemoSessionException, NoSuchAlgorithmException,
            CryptoFailedException {

        OmemoManager manager = managerGuard.get();
        OmemoDevice userDevice = manager.getOwnDevice();

        if (contactsDevice.equals(userDevice)) {
            throw new IllegalArgumentException("\"Thou shall not update thy own ratchet!\" - William Shakespeare");
        }

        // Establish session if necessary
        if (!hasSession(userDevice, contactsDevice)) {
            buildFreshSessionWithDevice(manager.getConnection(), userDevice, contactsDevice);
        }

        // Generate fresh AES key and IV
        byte[] messageKey = OmemoMessageBuilder.generateKey(KEYTYPE, KEYLENGTH);
        byte[] iv = OmemoMessageBuilder.generateIv();

        // Create message builder
        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> builder;
        try {
            builder = new OmemoMessageBuilder<>(userDevice, gullibleTrustCallback, getOmemoRatchet(manager),
                    messageKey, iv, null);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | UnsupportedEncodingException | NoSuchProviderException | IllegalBlockSizeException e) {
            throw new CryptoFailedException(e);
        }

        // Add recipient
        try {
            builder.addRecipient(contactsDevice);
        } catch (UndecidedOmemoIdentityException | UntrustedOmemoIdentityException e) {
            throw new AssertionError("Gullible Trust Callback reported undecided or untrusted device, " +
                    "even though it MUST NOT do that.");
        } catch (NoIdentityKeyException e) {
            throw new AssertionError("We MUST have an identityKey for " + contactsDevice + " since we built a session." + e);
        }

        return builder.finish();
    }

    /**
     * Encrypt a message with a messageKey and an IV and create an OmemoMessage from it.
     *
     * @param managerGuard authenticated OmemoManager
     * @param contactsDevices set of recipient OmemoDevices
     * @param messageKey AES key to encrypt the message
     * @param iv iv to be used with the messageKey
     * @return OmemoMessage object which contains the OmemoElement and some information.
     *
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws UndecidedOmemoIdentityException if the list of recipient devices contains undecided devices
     * @throws CryptoFailedException if we are lacking some crypto primitives
     */
    private OmemoMessage.Sent encrypt(OmemoManager.LoggedInOmemoManager managerGuard,
                                      Set<OmemoDevice> contactsDevices,
                                      byte[] messageKey,
                                      byte[] iv,
                                      String message)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            UndecidedOmemoIdentityException, CryptoFailedException {

        OmemoManager manager = managerGuard.get();
        OmemoDevice userDevice = manager.getOwnDevice();

        // Do not encrypt for our own device.
        removeOurDevice(userDevice, contactsDevices);

        buildMissingSessionsWithDevices(manager.getConnection(), userDevice, contactsDevices);

        Set<OmemoDevice> undecidedDevices = getUndecidedDevices(userDevice, manager.getTrustCallback(), contactsDevices);
        if (!undecidedDevices.isEmpty()) {
            throw new UndecidedOmemoIdentityException(undecidedDevices);
        }

        // Keep track of skipped devices
        HashMap<OmemoDevice, Throwable> skippedRecipients = new HashMap<>();

        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> builder;
        try {
            builder = new OmemoMessageBuilder<>(
                    userDevice, manager.getTrustCallback(), getOmemoRatchet(managerGuard.get()), messageKey, iv, message);
        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException |
                NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException(e);
        }

        for (OmemoDevice contactsDevice : contactsDevices) {
            // Build missing sessions
            if (!hasSession(userDevice, contactsDevice)) {
                try {
                    buildFreshSessionWithDevice(manager.getConnection(), userDevice, contactsDevice);
                } catch (CorruptedOmemoKeyException | CannotEstablishOmemoSessionException e) {
                    LOGGER.log(Level.WARNING, "Could not build session with " + contactsDevice + ".", e);
                    skippedRecipients.put(contactsDevice, e);
                    continue;
                }
            }

            // Ignore own stale devices
            if (contactsDevice.getJid().equals(userDevice.getJid()) && OmemoConfiguration.getIgnoreStaleDevices()) {

                Date lastMessageDate = getOmemoStoreBackend().getDateOfLastReceivedMessage(userDevice, contactsDevice);
                if (lastMessageDate == null) {
                    lastMessageDate = new Date();
                    getOmemoStoreBackend().setDateOfLastReceivedMessage(userDevice, contactsDevice, lastMessageDate);
                }

                Date lastPublicationDate = getOmemoStoreBackend().getDateOfLastDeviceIdPublication(userDevice, contactsDevice);
                if (lastPublicationDate == null) {
                    lastPublicationDate = new Date();
                    getOmemoStoreBackend().setDateOfLastDeviceIdPublication(userDevice, contactsDevice, lastPublicationDate);
                }

                boolean stale = isStale(userDevice, contactsDevice, lastPublicationDate, OmemoConfiguration.getIgnoreStaleDevicesAfterHours());
                stale &= isStale(userDevice, contactsDevice, lastMessageDate, OmemoConfiguration.getIgnoreStaleDevicesAfterHours());

                if (stale) {
                    LOGGER.log(Level.FINE, "Device " + contactsDevice + " seems to be stale (last message received "
                            + lastMessageDate + ", last publication of deviceId: " + lastPublicationDate + "). Ignore it.");
                    skippedRecipients.put(contactsDevice, new StaleDeviceException(contactsDevice, lastMessageDate, lastPublicationDate));
                    continue;
                }
            }

            // Add recipients
            try {
                builder.addRecipient(contactsDevice);
            }
            catch (NoIdentityKeyException | CorruptedOmemoKeyException e) {
                LOGGER.log(Level.WARNING, "Encryption failed for device " + contactsDevice + ".", e);
                skippedRecipients.put(contactsDevice, e);
            }
            catch (UndecidedOmemoIdentityException e) {
                throw new AssertionError("Recipients device seems to be undecided, even though we should have thrown" +
                        " an exception earlier in that case. " + e);
            }
            catch (UntrustedOmemoIdentityException e) {
                LOGGER.log(Level.WARNING, "Device " + contactsDevice + " is untrusted. Message is not encrypted for it.");
                skippedRecipients.put(contactsDevice, e);
            }
        }

        OmemoElement element = builder.finish();

        return new OmemoMessage.Sent(element, messageKey, iv, contactsDevices, skippedRecipients);
    }

    /**
     * Decrypt an OMEMO message.
     * @param managerGuard authenticated OmemoManager.
     * @param senderJid BareJid of the sender.
     * @param omemoElement omemoElement.
     * @return decrypted OmemoMessage object.
     *
     * @throws CorruptedOmemoKeyException if the identityKey of the sender is damaged.
     * @throws CryptoFailedException if decryption fails.
     * @throws NoRawSessionException if we have no session with the device and it sent a normal (non-preKey) message.
     */
    OmemoMessage.Received decryptMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                         BareJid senderJid,
                                         OmemoElement omemoElement)
            throws CorruptedOmemoKeyException, CryptoFailedException, NoRawSessionException {

        OmemoManager manager = managerGuard.get();
        int senderId = omemoElement.getHeader().getSid();
        OmemoDevice senderDevice = new OmemoDevice(senderJid, senderId);

        CipherAndAuthTag cipherAndAuthTag = getOmemoRatchet(manager)
                .retrieveMessageKeyAndAuthTag(senderDevice, omemoElement);

        // Retrieve senders fingerprint. TODO: Find a way to do this without the store.
        OmemoFingerprint senderFingerprint;
        try {
            senderFingerprint = getOmemoStoreBackend().getFingerprint(manager.getOwnDevice(), senderDevice);
        } catch (NoIdentityKeyException e) {
            throw new AssertionError("Cannot retrieve OmemoFingerprint of sender although decryption was successful: " + e);
        }

        if (omemoElement.isMessageElement()) {
            // Use symmetric message key to decrypt message payload.
            String plaintext = OmemoRatchet.decryptMessageElement(omemoElement, cipherAndAuthTag);

            return new OmemoMessage.Received(omemoElement, cipherAndAuthTag.getKey(), cipherAndAuthTag.getIv(),
                    plaintext, senderFingerprint, senderDevice, cipherAndAuthTag.wasPreKeyEncrypted());

        } else {
            // KeyTransportMessages don't require decryption of the payload.
            return new OmemoMessage.Received(omemoElement, cipherAndAuthTag.getKey(), cipherAndAuthTag.getIv(),
                    null, senderFingerprint, senderDevice, cipherAndAuthTag.wasPreKeyEncrypted());
        }
    }

    /**
     * Create an OMEMO KeyTransportElement.
     * @see <a href="https://xmpp.org/extensions/xep-0384.html#usecases-keysend">XEP-0384: Sending a key</a>.
     *
     * @param managerGuard Initialized OmemoManager.
     * @param contactsDevices set of recipient devices.
     * @param key AES-Key to be transported.
     * @param iv initialization vector to be used with the key.
     * @return KeyTransportElement
     *
     * @throws InterruptedException
     * @throws UndecidedOmemoIdentityException if the list of recipients contains an undecided device
     * @throws CryptoFailedException if we are lacking some cryptographic algorithms
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    OmemoMessage.Sent createKeyTransportElement(OmemoManager.LoggedInOmemoManager managerGuard,
                                                Set<OmemoDevice> contactsDevices,
                                                byte[] key,
                                                byte[] iv)
            throws InterruptedException, UndecidedOmemoIdentityException, CryptoFailedException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return encrypt(managerGuard, contactsDevices, key, iv, null);
    }

    /**
     * Create an OmemoMessage.
     *
     * @param managerGuard initialized OmemoManager
     * @param contactsDevices set of recipient devices
     * @param message message we want to send
     * @return encrypted OmemoMessage
     *
     * @throws InterruptedException
     * @throws UndecidedOmemoIdentityException if the list of recipient devices contains an undecided device.
     * @throws CryptoFailedException if we are lacking some cryptographic algorithms
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    OmemoMessage.Sent createOmemoMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                         Set<OmemoDevice> contactsDevices,
                                         String message)
            throws InterruptedException, UndecidedOmemoIdentityException, CryptoFailedException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        byte[] key, iv;
        iv = OmemoMessageBuilder.generateIv();

        try {
            key = OmemoMessageBuilder.generateKey(KEYTYPE, KEYLENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoFailedException(e);
        }

        return encrypt(managerGuard, contactsDevices, key, iv, message);
    }

    /**
     * Retrieve a users OMEMO bundle.
     *
     * @param connection authenticated XMPP connection.
     * @param contactsDevice device of which we want to retrieve the bundle.
     * @return OmemoBundle of the device or null, if it doesn't exist.
     *
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotALeafNodeException
     * @throws PubSubException.NotAPubSubNodeException
     */
    private static OmemoBundleElement fetchBundle(XMPPConnection connection,
                                                  OmemoDevice contactsDevice)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException,
            PubSubException.NotAPubSubNodeException {

        PubSubManager pm = PubSubManager.getInstance(connection, contactsDevice.getJid());
        LeafNode node = pm.getLeafNode(contactsDevice.getBundleNodeName());

        if (node == null) {
            return null;
        }

        List<PayloadItem<OmemoBundleElement>> bundleItems = node.getItems();
        if (bundleItems.isEmpty()) {
            return null;
        }

        return bundleItems.get(bundleItems.size() - 1).getPayload();
    }

    /**
     * Publish the given OMEMO bundle to the server using PubSub.
     * @param connection our connection.
     * @param userDevice our device
     * @param bundle the bundle we want to publish
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    static void publishBundle(XMPPConnection connection, OmemoDevice userDevice, OmemoBundleElement bundle)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstance(connection, connection.getUser().asBareJid());
        pm.tryToPublishAndPossibleAutoCreate(userDevice.getBundleNodeName(), new PayloadItem<>(bundle));
    }

    /**
     * Retrieve the OMEMO device list of a contact.
     *
     * @param connection authenticated XMPP connection.
     * @param contact BareJid of the contact of which we want to retrieve the device list from.
     * @return
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotAPubSubNodeException
     */
    private static OmemoDeviceListElement fetchDeviceList(XMPPConnection connection, BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {

        PubSubManager pm = PubSubManager.getInstance(connection, contact);
        String nodeName = OmemoConstants.PEP_NODE_DEVICE_LIST;
        LeafNode node = pm.getLeafNode(nodeName);

        if (node == null) {
            return null;
        }

        List<PayloadItem<OmemoDeviceListElement>> items = node.getItems();
        if (items.isEmpty()) {
            return null;
        }

        return items.get(items.size() - 1).getPayload();
    }

    /**
     * Publish the given device list to the server.
     * @param connection authenticated XMPP connection.
     * @param deviceList users deviceList.
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     * @throws PubSubException.NotALeafNodeException
     */
    static void publishDeviceList(XMPPConnection connection, OmemoDeviceListElement deviceList)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {

        PubSubManager.getInstance(connection, connection.getUser().asBareJid())
                .tryToPublishAndPossibleAutoCreate(OmemoConstants.PEP_NODE_DEVICE_LIST, new PayloadItem<>(deviceList));
    }

    /**
     *
     * @param connection
     * @param userDevice
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    private void refreshAndRepublishDeviceList(XMPPConnection connection, OmemoDevice userDevice)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        // refreshOmemoDeviceList;
        OmemoDeviceListElement publishedList;
        try {
            publishedList = fetchDeviceList(connection, userDevice.getJid());
        } catch (PubSubException.NotAPubSubNodeException e) {
            publishedList = null;
        }
        if (publishedList == null) {
            publishedList = new OmemoDeviceListElement_VAxolotl(Collections.<Integer>emptySet());
        }

        getOmemoStoreBackend().mergeCachedDeviceList(userDevice, userDevice.getJid(), publishedList);

        OmemoCachedDeviceList cachedList = cleanUpDeviceList(userDevice);

        // Republish our deviceId if it is missing from the published list.
        if (!publishedList.getDeviceIds().equals(cachedList.getActiveDevices())) {
            publishDeviceList(connection, new OmemoDeviceListElement_VAxolotl(cachedList));
        }
    }

    /**
     * Add our load the deviceList of the user from cache, delete stale devices if needed, add the users device
     * back if necessary, store the refurbished list in cache and return it.
     *
     * @param userDevice
     * @return
     */
    OmemoCachedDeviceList cleanUpDeviceList(OmemoDevice userDevice) {
        OmemoCachedDeviceList cachedDeviceList;

        // Delete stale devices if allowed and necessary
        if (OmemoConfiguration.getDeleteStaleDevices()) {
            cachedDeviceList = deleteStaleDevices(userDevice);
        } else {
            cachedDeviceList = getOmemoStoreBackend().loadCachedDeviceList(userDevice);
        }


        // Add back our device if necessary
        if (!cachedDeviceList.getActiveDevices().contains(userDevice.getDeviceId())) {
            cachedDeviceList.addDevice(userDevice.getDeviceId());
        }

        getOmemoStoreBackend().storeCachedDeviceList(userDevice, userDevice.getJid(), cachedDeviceList);
        return cachedDeviceList;
    }

    /**
     * Refresh and merge device list of contact.
     *
     * @param connection authenticated XMPP connection
     * @param userDevice our OmemoDevice
     * @param contact contact we want to fetch the deviceList from
     * @return cached device list after refresh.
     *
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    OmemoCachedDeviceList refreshDeviceList(XMPPConnection connection, OmemoDevice userDevice, BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        // refreshOmemoDeviceList;
        OmemoDeviceListElement publishedList;
        try {
            publishedList = fetchDeviceList(connection, contact);
        } catch (PubSubException.NotAPubSubNodeException e) {
            LOGGER.log(Level.WARNING, "Error refreshing deviceList: ", e);
            publishedList = null;
        }
        if (publishedList == null) {
            publishedList = new OmemoDeviceListElement_VAxolotl(Collections.<Integer>emptySet());
        }

        return getOmemoStoreBackend().mergeCachedDeviceList(
                userDevice, contact, publishedList);
    }

    /**
     * Fetch the bundle of a contact and build a fresh OMEMO session with the contacts device.
     * Note that this builds a fresh session, regardless if we have had a session before or not.
     *
     * @param connection authenticated XMPP connection
     * @param userDevice our OmemoDevice
     * @param contactsDevice OmemoDevice of a contact.
     * @throws CannotEstablishOmemoSessionException if we cannot establish a session (because of missing bundle etc.)
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws CorruptedOmemoKeyException if our IdentityKeyPair is corrupted.
     */
    void buildFreshSessionWithDevice(XMPPConnection connection, OmemoDevice userDevice, OmemoDevice contactsDevice)
            throws CannotEstablishOmemoSessionException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, CorruptedOmemoKeyException {

        if (contactsDevice.equals(userDevice)) {
            // Do not build a session with yourself.
            return;
        }

        OmemoBundleElement bundleElement;
        try {
            bundleElement = fetchBundle(connection, contactsDevice);
        } catch (XMPPException.XMPPErrorException | PubSubException.NotALeafNodeException |
                PubSubException.NotAPubSubNodeException e) {
            throw new CannotEstablishOmemoSessionException(contactsDevice, e);
        }

        // Select random Bundle
        HashMap<Integer, T_Bundle> bundlesList = getOmemoStoreBackend().keyUtil().BUNDLE.bundles(bundleElement, contactsDevice);
        int randomIndex = new Random().nextInt(bundlesList.size());
        T_Bundle randomPreKeyBundle = new ArrayList<>(bundlesList.values()).get(randomIndex);

        // build the session
        OmemoManager omemoManager = OmemoManager.getInstanceFor(connection, userDevice.getDeviceId());
        processBundle(omemoManager, randomPreKeyBundle, contactsDevice);
    }

    /**
     * Build OMEMO sessions with all devices of the contact, we haven't had sessions with before.
     * This method returns a set of OmemoDevices. This set contains all devices, with which we either had sessions
     * before, plus those devices with which we just built sessions.
     *
     * @param connection authenticated XMPP connection.
     * @param userDevice our OmemoDevice
     * @param contact the BareJid of the contact with whom we want to build sessions with.
     * @return set of devices with a session.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    private Set<OmemoDevice> buildMissingSessionsWithContact(XMPPConnection connection,
                                                             OmemoDevice userDevice,
                                                             BareJid contact)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {

        OmemoCachedDeviceList contactsDeviceIds = getOmemoStoreBackend().loadCachedDeviceList(userDevice, contact);
        Set<OmemoDevice> contactsDevices = new HashSet<>();
        for (int deviceId : contactsDeviceIds.getActiveDevices()) {
            contactsDevices.add(new OmemoDevice(contact, deviceId));
        }

        return buildMissingSessionsWithDevices(connection, userDevice, contactsDevices);
    }

    /**
     * Build sessions with all devices from the set, we don't have a session with yet.
     * Return the set of all devices we have a session with afterwards.
     * @param connection authenticated XMPP connection
     * @param userDevice our OmemoDevice
     * @param devices set of devices we may want to build a session with if necessary
     * @return set of all devices with sessions
     *
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    private Set<OmemoDevice> buildMissingSessionsWithDevices(XMPPConnection connection,
                                                             OmemoDevice userDevice,
                                                             Set<OmemoDevice> devices)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {

        Set<OmemoDevice> devicesWithSession = new HashSet<>();
        for (OmemoDevice device : devices) {

            if (hasSession(userDevice, device)) {
                devicesWithSession.add(device);
                continue;
            }

            try {
                buildFreshSessionWithDevice(connection, userDevice, device);
                devicesWithSession.add(device);
            } catch (CannotEstablishOmemoSessionException e) {
                LOGGER.log(Level.WARNING, userDevice + " cannot establish session with " + device +
                        " because their bundle could not be fetched.", e);
            } catch (CorruptedOmemoKeyException e) {
                LOGGER.log(Level.WARNING, userDevice + " could not establish session with " + device +
                        "because their bundle seems to be corrupt.", e);
            }

        }

        return devicesWithSession;
    }

    /**
     * Build OMEMO sessions with all devices of the contacts, we haven't had sessions with before.
     * This method returns a set of OmemoDevices. This set contains all devices, with which we either had sessions
     * before, plus those devices with which we just built sessions.
     *
     * @param connection authenticated XMPP connection.
     * @param userDevice our OmemoDevice
     * @param contacts set of BareJids of contacts, we want to build sessions with.
     * @return set of devices, we have sessions with.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    private Set<OmemoDevice> buildMissingSessionsWithContacts(XMPPConnection connection,
                                                              OmemoDevice userDevice,
                                                              Set<BareJid> contacts)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {

        Set<OmemoDevice> devicesWithSessions = new HashSet<>();

        for (BareJid contact : contacts) {
            Set<OmemoDevice> devices = buildMissingSessionsWithContact(connection, userDevice, contact);
            devicesWithSessions.addAll(devices);
        }

        return devicesWithSessions;
    }

    /**
     * Return a set of all devices from the provided set, which trust level is undecided.
     * A device is also considered undecided, if its fingerprint cannot be loaded.
     *
     * @param userDevice our OmemoDevice
     * @param callback OmemoTrustCallback to query the trust decisions from
     * @param devices set of OmemoDevices
     * @return set of OmemoDevices which contains all devices from the set devices, which are undecided
     */
    private Set<OmemoDevice> getUndecidedDevices(OmemoDevice userDevice, OmemoTrustCallback callback, Set<OmemoDevice> devices) {
        Set<OmemoDevice> undecidedDevices = new HashSet<>();

        for (OmemoDevice device : devices) {

            OmemoFingerprint fingerprint;
            try {
                fingerprint = getOmemoStoreBackend().getFingerprint(userDevice, device);
            } catch (CorruptedOmemoKeyException | NoIdentityKeyException e) {
                // TODO: Best solution?
                LOGGER.log(Level.WARNING, "Could not load fingerprint of " + device, e);
                undecidedDevices.add(device);
                continue;
            }

            if (callback.getTrust(device, fingerprint) == TrustState.undecided) {
                undecidedDevices.add(device);
            }
        }

        return undecidedDevices;
    }

    /**
     * Return a set of all devices from the provided set, which are untrusted.
     * A device is also considered untrusted, if its fingerprint cannot be loaded.
     *
     * @param userDevice our own OmemoDevice
     * @param trustCallback OmemoTrustCallback to query trust decisions from
     * @param devices set of OmemoDevices
     * @return set of OmemoDevices from devices, which contains all devices which are untrusted
     */
    private Set<OmemoDevice> getUntrustedDeviced(OmemoDevice userDevice, OmemoTrustCallback trustCallback, Set<OmemoDevice> devices) {
        Set<OmemoDevice> untrustedDevices = new HashSet<>();

        for (OmemoDevice device : devices) {

            OmemoFingerprint fingerprint;
            try {
                fingerprint = getOmemoStoreBackend().getFingerprint(userDevice, device);
            } catch (CorruptedOmemoKeyException | NoIdentityKeyException e) {
                // TODO: Best solution?
                untrustedDevices.add(device);
                continue;
            }

            if (trustCallback.getTrust(device, fingerprint) == TrustState.untrusted) {
                untrustedDevices.add(device);
            }
        }

        return untrustedDevices;
    }

    /**
     * Return true, if the OmemoManager of userDevice has a session with the contactsDevice.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice OmemoDevice of the contact.
     * @return true if userDevice has session with contactsDevice.
     */
    private boolean hasSession(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        return getOmemoStoreBackend().loadRawSession(userDevice, contactsDevice) != null;
    }

    /**
     * Process a received bundle. Typically that includes saving keys and building a session.
     *
     * @param omemoManager our OmemoManager
     * @param contactsBundle bundle of the contact
     * @param contactsDevice OmemoDevice of the contact
     * @throws CorruptedOmemoKeyException
     */
    protected abstract void processBundle(OmemoManager omemoManager,
                                          T_Bundle contactsBundle,
                                          OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException;

    /**
     * Returns true, if a rotation of the signed preKey is necessary.
     *
     * @param userDevice our OmemoDevice
     * @return true if rotation is necessary
     */
    private boolean shouldRotateSignedPreKey(OmemoDevice userDevice) {
        if (!OmemoConfiguration.getRenewOldSignedPreKeys()) {
            return false;
        }

        Date now = new Date();
        Date lastRenewal = getOmemoStoreBackend().getDateOfLastSignedPreKeyRenewal(userDevice);

        if (lastRenewal == null) {
            lastRenewal = new Date();
            getOmemoStoreBackend().setDateOfLastSignedPreKeyRenewal(userDevice, lastRenewal);
        }

        long allowedAgeMillis = MILLIS_PER_HOUR * OmemoConfiguration.getRenewOldSignedPreKeysAfterHours();
        return now.getTime() - lastRenewal.getTime() > allowedAgeMillis;
    }

    /**
     * Return a copy of our deviceList, but with stale devices marked as inactive.
     * Never mark our own device as stale.
     * This method ignores {@link OmemoConfiguration#getDeleteStaleDevices()}!
     *
     * In this case, a stale device is one of our devices, from which we haven't received an OMEMO message from
     * for more than {@link OmemoConfiguration#DELETE_STALE_DEVICE_AFTER_HOURS} hours.
     *
     * @param userDevice our OmemoDevice
     * @return our altered deviceList with stale devices marked as inactive.
     */
    private OmemoCachedDeviceList deleteStaleDevices(OmemoDevice userDevice) {
        OmemoCachedDeviceList deviceList = getOmemoStoreBackend().loadCachedDeviceList(userDevice);
        int maxAgeHours = OmemoConfiguration.getDeleteStaleDevicesAfterHours();
        return removeStaleDevicesFromDeviceList(userDevice, userDevice.getJid(), deviceList, maxAgeHours);
    }

    /**
     * Return a copy of the contactsDeviceList, but with stale devices marked as inactive.
     * Never mark our own device as stale.
     * This method ignores {@link OmemoConfiguration#getIgnoreStaleDevices()}!
     *
     * In this case, a stale device is one of our devices, from which we haven't received an OMEMO message from
     * for more than {@link OmemoConfiguration#IGNORE_STALE_DEVICE_AFTER_HOURS} hours.
     *
     * @param userDevice our OmemoDevice
     * @param contact subjects BareJid
     * @param contactsDeviceList subjects deviceList
     * @return
     */
    private OmemoCachedDeviceList ignoreStaleDevices(OmemoDevice userDevice, BareJid contact, OmemoCachedDeviceList contactsDeviceList) {
        int maxAgeHours = OmemoConfiguration.getIgnoreStaleDevicesAfterHours();
        return removeStaleDevicesFromDeviceList(userDevice, contact, contactsDeviceList, maxAgeHours);
    }

    /**
     * Return a copy of the given deviceList of user contact, but with stale devices marked as inactive.
     * Never mark our own device as stale. If we haven't yet received a message from a device, store the current date
     * as last date of message receipt to allow future decisions.
     *
     * A stale device is a device, from which we haven't received an OMEMO message from for more than
     * "maxAgeMillis" milliseconds.
     *
     * @param userDevice our OmemoDevice.
     * @param contact subjects BareJid.
     * @param contactsDeviceList subjects deviceList.
     * @return copy of subjects deviceList with stale devices marked as inactive.
     */
    private OmemoCachedDeviceList removeStaleDevicesFromDeviceList(OmemoDevice userDevice,
                                                                   BareJid contact,
                                                                   OmemoCachedDeviceList contactsDeviceList,
                                                                   int maxAgeHours) {
        OmemoCachedDeviceList deviceList = new OmemoCachedDeviceList(contactsDeviceList); // Don't work on original list.

        // Iterate through original list, but modify copy instead
        for (int deviceId : contactsDeviceList.getActiveDevices()) {
            OmemoDevice device = new OmemoDevice(contact, deviceId);

            Date lastDeviceIdPublication = getOmemoStoreBackend().getDateOfLastDeviceIdPublication(userDevice, device);
            if (lastDeviceIdPublication == null) {
                lastDeviceIdPublication = new Date();
                getOmemoStoreBackend().setDateOfLastDeviceIdPublication(userDevice, device, lastDeviceIdPublication);
            }

            Date lastMessageReceived = getOmemoStoreBackend().getDateOfLastReceivedMessage(userDevice, device);
            if (lastMessageReceived == null) {
                lastMessageReceived = new Date();
                getOmemoStoreBackend().setDateOfLastReceivedMessage(userDevice, device, lastMessageReceived);
            }

            boolean stale = isStale(userDevice, device, lastDeviceIdPublication, maxAgeHours);
            stale &= isStale(userDevice, device, lastMessageReceived, maxAgeHours);

            if (stale) {
                deviceList.addInactiveDevice(deviceId);
            }
        }
        return deviceList;
    }


    /**
     * Remove our device from the collection of devices.
     *
     * @param userDevice our OmemoDevice
     * @param devices collection of OmemoDevices
     */
    static void removeOurDevice(OmemoDevice userDevice, Collection<OmemoDevice> devices) {
        if (devices.contains(userDevice)) {
            devices.remove(userDevice);
        }
    }

    /**
     *
     * @param userDevice
     * @param subject
     * @param lastReceipt
     * @param maxAgeHours
     * @return
     */
    static boolean isStale(OmemoDevice userDevice, OmemoDevice subject, Date lastReceipt, int maxAgeHours) {
        if (userDevice.equals(subject)) {
            return false;
        }

        if (lastReceipt == null) {
            return false;
        }

        long maxAgeMillis = MILLIS_PER_HOUR * maxAgeHours;
        Date now = new Date();

        return now.getTime() - lastReceipt.getTime() > maxAgeMillis;
    }

    /**
     * Gullible TrustCallback, which returns all queried identities as trusted.
     * This is only used for insensitive OMEMO messages like RatchetUpdateMessages.
     * DO NOT USE THIS FOR ANYTHING ELSE!
     */
    private static final OmemoTrustCallback gullibleTrustCallback = new OmemoTrustCallback() {
        @Override
        public TrustState getTrust(OmemoDevice device, OmemoFingerprint fingerprint) {
            return TrustState.trusted;
        }

        @Override
        public void setTrust(OmemoDevice device, OmemoFingerprint fingerprint, TrustState state) {
            // Not needed
        }
    };

    /**
     * Decrypt a possible OMEMO encrypted messages in a {@link MamManager.MamQuery}.
     * The returned list contains wrappers that either hold an {@link OmemoMessage} in case the message was decrypted
     * properly, otherwise it contains the message itself.
     *
     * @param managerGuard authenticated OmemoManager.
     * @param mamQuery Mam archive query
     * @return list of {@link MessageOrOmemoMessage}s.
     */
    List<MessageOrOmemoMessage> decryptMamQueryResult(OmemoManager.LoggedInOmemoManager managerGuard,
                                                      MamManager.MamQuery mamQuery) {
        List<MessageOrOmemoMessage> result = new ArrayList<>();
        for (Message message : mamQuery.getMessages()) {
            if (OmemoManager.stanzaContainsOmemoElement(message)) {
                OmemoElement element =
                        message.getExtension(OmemoElement.NAME_ENCRYPTED, OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL);
                // Decrypt OMEMO messages
                try {
                    OmemoMessage.Received omemoMessage = decryptMessage(managerGuard, message.getFrom().asBareJid(), element);
                    result.add(new MessageOrOmemoMessage(omemoMessage));
                } catch (NoRawSessionException | CorruptedOmemoKeyException | CryptoFailedException e) {
                    LOGGER.log(Level.WARNING, "decryptMamQueryResult failed to decrypt message from "
                            + message.getFrom() + " due to corrupted session/key: " + e.getMessage());
                    result.add(new MessageOrOmemoMessage(message));
                }
            } else {
                // Wrap cleartext messages
                result.add(new MessageOrOmemoMessage(message));
            }
        }

        return result;
    }


    @Override
    public void onOmemoCarbonCopyReceived(CarbonExtension.Direction direction,
                                          Message carbonCopy,
                                          Message wrappingMessage,
                                          OmemoManager.LoggedInOmemoManager managerGuard) {
        OmemoManager manager = managerGuard.get();
        // Avoid the ratchet being manipulated and the bundle being published multiple times simultaneously
        synchronized (manager.LOCK) {
            OmemoDevice userDevice = manager.getOwnDevice();
            OmemoElement element = carbonCopy.getExtension(OmemoElement.NAME_ENCRYPTED, OmemoElement_VAxolotl.NAMESPACE);
            if (element == null) {
                return;
            }

            OmemoMessage.Received decrypted;
            BareJid sender = carbonCopy.getFrom().asBareJid();

            try {
                decrypted = decryptMessage(managerGuard, sender, element);
                manager.notifyOmemoCarbonCopyReceived(direction, carbonCopy, wrappingMessage, decrypted);

                if (decrypted.isPreKeyMessage() && OmemoConfiguration.getCompleteSessionWithEmptyMessage()) {
                    LOGGER.log(Level.FINE, "Received a preKeyMessage in a carbon copy from " + decrypted.getSenderDevice() + ".\n" +
                            "Complete the session by sending an empty response message.");
                    try {
                        sendRatchetUpdate(managerGuard, decrypted.getSenderDevice());
                    } catch (CannotEstablishOmemoSessionException e) {
                        throw new AssertionError("Since we successfully received a message, we MUST be able to " +
                                "establish a session. " + e);
                    } catch (NoSuchAlgorithmException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                        LOGGER.log(Level.WARNING, "Cannot send a ratchet update message.", e);
                    }
                }
            } catch (NoRawSessionException e) {
                OmemoDevice device = e.getDeviceWithoutSession();
                LOGGER.log(Level.WARNING, "No raw session found for contact " + device + ". ", e);

                if (OmemoConfiguration.getRepairBrokenSessionsWithPreKeyMessages()) {
                    repairBrokenSessionWithPreKeyMessage(managerGuard, device);
                }
            } catch (CorruptedOmemoKeyException | CryptoFailedException e) {
                LOGGER.log(Level.WARNING, "Could not decrypt incoming carbon copy: ", e);
            }

            // Upload fresh bundle.
            if (getOmemoStoreBackend().loadOmemoPreKeys(userDevice).size() < OmemoConstants.PRE_KEY_COUNT_PER_BUNDLE) {
                LOGGER.log(Level.FINE, "We used up a preKey. Upload a fresh bundle.");
                try {
                    getOmemoStoreBackend().replenishKeys(userDevice);
                    OmemoBundleElement bundleElement = getOmemoStoreBackend().packOmemoBundle(userDevice);
                    publishBundle(manager.getConnection(), userDevice, bundleElement);
                } catch (CorruptedOmemoKeyException | InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    LOGGER.log(Level.WARNING, "Could not republish replenished bundle.", e);
                }
            }
        }
    }

    @Override
    public void onOmemoMessageStanzaReceived(Stanza stanza, OmemoManager.LoggedInOmemoManager managerGuard) {
        OmemoManager manager = managerGuard.get();
        // Avoid the ratchet being manipulated and the bundle being published multiple times simultaneously
        synchronized (manager.LOCK) {
            OmemoDevice userDevice = manager.getOwnDevice();
            OmemoElement element = stanza.getExtension(OmemoElement.NAME_ENCRYPTED, OmemoElement_VAxolotl.NAMESPACE);
            if (element == null) {
                return;
            }

            OmemoMessage.Received decrypted;
            BareJid sender;

            try {
                MultiUserChat muc = getMuc(manager.getConnection(), stanza.getFrom());
                if (muc != null) {
                    Occupant occupant = muc.getOccupant(stanza.getFrom().asEntityFullJidIfPossible());
                    if (occupant == null) {
                        LOGGER.log(Level.WARNING, "Cannot decrypt OMEMO MUC message; MUC Occupant is null.");
                        return;
                    }
                    Jid occupantJid = occupant.getJid();

                    if (occupantJid == null) {
                        LOGGER.log(Level.WARNING, "Cannot decrypt OMEMO MUC message; Senders Jid is null. " +
                                stanza.getFrom());
                        return;
                    }

                    sender = occupantJid.asBareJid();

                    // try is for this
                    decrypted = decryptMessage(managerGuard, sender, element);
                    manager.notifyOmemoMucMessageReceived(muc, stanza, decrypted);

                } else {
                    sender = stanza.getFrom().asBareJid();

                    // and this
                    decrypted = decryptMessage(managerGuard, sender, element);
                    manager.notifyOmemoMessageReceived(stanza, decrypted);
                }

                if (decrypted.isPreKeyMessage() && OmemoConfiguration.getCompleteSessionWithEmptyMessage()) {
                    LOGGER.log(Level.FINE, "Received a preKeyMessage from " + decrypted.getSenderDevice() + ".\n" +
                            "Complete the session by sending an empty response message.");
                    try {
                        sendRatchetUpdate(managerGuard, decrypted.getSenderDevice());
                    } catch (CannotEstablishOmemoSessionException e) {
                        throw new AssertionError("Since we successfully received a message, we MUST be able to " +
                                "establish a session. " + e);
                    } catch (NoSuchAlgorithmException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                        LOGGER.log(Level.WARNING, "Cannot send a ratchet update message.", e);
                    }
                }
            } catch (NoRawSessionException e) {
                OmemoDevice device = e.getDeviceWithoutSession();
                LOGGER.log(Level.WARNING, "No raw session found for contact " + device + ". ", e);

                if (OmemoConfiguration.getRepairBrokenSessionsWithPreKeyMessages()) {
                    repairBrokenSessionWithPreKeyMessage(managerGuard, device);
                }
            } catch (CorruptedOmemoKeyException | CryptoFailedException e) {
                LOGGER.log(Level.WARNING, "Could not decrypt incoming message: ", e);
            }

            // Upload fresh bundle.
            if (getOmemoStoreBackend().loadOmemoPreKeys(userDevice).size() < OmemoConstants.PRE_KEY_COUNT_PER_BUNDLE) {
                LOGGER.log(Level.FINE, "We used up a preKey. Upload a fresh bundle.");
                try {
                    getOmemoStoreBackend().replenishKeys(userDevice);
                    OmemoBundleElement bundleElement = getOmemoStoreBackend().packOmemoBundle(userDevice);
                    publishBundle(manager.getConnection(), userDevice, bundleElement);
                } catch (CorruptedOmemoKeyException | InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    LOGGER.log(Level.WARNING, "Could not republish replenished bundle.", e);
                }
            }
        }
    }

    /**
     * Decrypt the OmemoElement inside the given Stanza and return it.
     * Return null if something goes wrong.
     *
     * @param stanza stanza
     * @param managerGuard authenticated OmemoManager
     * @return decrypted OmemoMessage or null
     */
    OmemoMessage.Received decryptStanza(Stanza stanza, OmemoManager.LoggedInOmemoManager managerGuard) {
        OmemoManager manager = managerGuard.get();
        // Avoid the ratchet being manipulated and the bundle being published multiple times simultaneously
        synchronized (manager.LOCK) {
            OmemoDevice userDevice = manager.getOwnDevice();
            OmemoElement element = stanza.getExtension(OmemoElement.NAME_ENCRYPTED, OmemoElement_VAxolotl.NAMESPACE);
            if (element == null) {
                return null;
            }

            OmemoMessage.Received decrypted = null;
            BareJid sender;

            try {
                MultiUserChat muc = getMuc(manager.getConnection(), stanza.getFrom());
                if (muc != null) {
                    Occupant occupant = muc.getOccupant(stanza.getFrom().asEntityFullJidIfPossible());
                    Jid occupantJid = occupant.getJid();

                    if (occupantJid == null) {
                        LOGGER.log(Level.WARNING, "MUC message received, but there is no way to retrieve the senders Jid. " +
                                stanza.getFrom());
                        return null;
                    }

                    sender = occupantJid.asBareJid();

                    // try is for this
                    decrypted = decryptMessage(managerGuard, sender, element);

                } else {
                    sender = stanza.getFrom().asBareJid();

                    // and this
                    decrypted = decryptMessage(managerGuard, sender, element);
                }

                if (decrypted.isPreKeyMessage() && OmemoConfiguration.getCompleteSessionWithEmptyMessage()) {
                    LOGGER.log(Level.FINE, "Received a preKeyMessage from " + decrypted.getSenderDevice() + ".\n" +
                            "Complete the session by sending an empty response message.");
                    try {
                        sendRatchetUpdate(managerGuard, decrypted.getSenderDevice());
                    } catch (CannotEstablishOmemoSessionException e) {
                        throw new AssertionError("Since we successfully received a message, we MUST be able to " +
                                "establish a session. " + e);
                    } catch (NoSuchAlgorithmException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                        LOGGER.log(Level.WARNING, "Cannot send a ratchet update message.", e);
                    }
                }
            } catch (NoRawSessionException e) {
                OmemoDevice device = e.getDeviceWithoutSession();
                LOGGER.log(Level.WARNING, "No raw session found for contact " + device + ". ", e);

            } catch (CorruptedOmemoKeyException | CryptoFailedException e) {
                LOGGER.log(Level.WARNING, "Could not decrypt incoming message: ", e);
            }

            // Upload fresh bundle.
            if (getOmemoStoreBackend().loadOmemoPreKeys(userDevice).size() < OmemoConstants.PRE_KEY_COUNT_PER_BUNDLE) {
                LOGGER.log(Level.FINE, "We used up a preKey. Upload a fresh bundle.");
                try {
                    getOmemoStoreBackend().replenishKeys(userDevice);
                    OmemoBundleElement bundleElement = getOmemoStoreBackend().packOmemoBundle(userDevice);
                    publishBundle(manager.getConnection(), userDevice, bundleElement);
                } catch (CorruptedOmemoKeyException | InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    LOGGER.log(Level.WARNING, "Could not republish replenished bundle.", e);
                }
            }
            return decrypted;
        }
    }

    /**
     * Fetch and process a fresh bundle and send an empty preKeyMessage in order to establish a fresh session.
     *
     * @param managerGuard authenticated OmemoManager.
     * @param brokenDevice device which session broke.
     */
    private void repairBrokenSessionWithPreKeyMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                                      OmemoDevice brokenDevice) {

        LOGGER.log(Level.WARNING, "Attempt to repair the session by sending a fresh preKey message to "
                + brokenDevice);
        OmemoManager manager = managerGuard.get();
        try {
            // Create fresh session and send new preKeyMessage.
            buildFreshSessionWithDevice(manager.getConnection(), manager.getOwnDevice(), brokenDevice);
            sendRatchetUpdate(managerGuard, brokenDevice);

        } catch (CannotEstablishOmemoSessionException | CorruptedOmemoKeyException e) {
            LOGGER.log(Level.WARNING, "Unable to repair session with " + brokenDevice, e);
        } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {
            LOGGER.log(Level.WARNING, "Could not fetch fresh bundle for " + brokenDevice, e);
        } catch (CryptoFailedException | NoSuchAlgorithmException e) {
            LOGGER.log(Level.WARNING, "Could not create PreKeyMessage", e);
        }
    }

    /**
     * Send an empty OMEMO message to contactsDevice in order to forward the ratchet.
     * @param managerGuard
     * @param contactsDevice
     * @throws CorruptedOmemoKeyException if our or their OMEMO key is corrupted.
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws NoSuchAlgorithmException if AES encryption fails
     * @throws SmackException.NotConnectedException
     * @throws CryptoFailedException if encryption fails (should not happen though, but who knows...)
     * @throws CannotEstablishOmemoSessionException if we cannot establish a session with contactsDevice.
     */
    private void sendRatchetUpdate(OmemoManager.LoggedInOmemoManager managerGuard, OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException, InterruptedException, SmackException.NoResponseException,
            NoSuchAlgorithmException, SmackException.NotConnectedException, CryptoFailedException,
            CannotEstablishOmemoSessionException {

        OmemoManager manager = managerGuard.get();
        OmemoElement ratchetUpdate = createRatchetUpdateElement(managerGuard, contactsDevice);
        Message m = new Message();
        m.setTo(contactsDevice.getJid());
        m.addExtension(ratchetUpdate);
        manager.getConnection().sendStanza(m);
    }

    /**
     * Return the joined MUC with EntityBareJid jid, or null if its not a room and/or not joined.
     * @param connection xmpp connection
     * @param jid jid (presumably) of the MUC
     * @return MultiUserChat or null if not a MUC.
     */
    private static MultiUserChat getMuc(XMPPConnection connection, Jid jid) {
        EntityBareJid ebj = jid.asEntityBareJidIfPossible();
        if (ebj == null) {
            return null;
        }

        MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(connection);
        Set<EntityBareJid> joinedRooms = mucm.getJoinedRooms();
        if (joinedRooms.contains(ebj)) {
            return mucm.getMultiUserChat(ebj);
        }

        return null;
    }

    /**
     * Publish a new DeviceList with just our device in it.
     *
     * @param managerGuard authenticated OmemoManager.
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public void purgeDeviceList(OmemoManager.LoggedInOmemoManager managerGuard)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {

        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        OmemoDeviceListElement_VAxolotl newList =
                new OmemoDeviceListElement_VAxolotl(Collections.singleton(userDevice.getDeviceId()));

        // Merge list
        getOmemoStoreBackend().mergeCachedDeviceList(userDevice, userDevice.getJid(), newList);

        OmemoService.publishDeviceList(omemoManager.getConnection(), newList);
    }
}
