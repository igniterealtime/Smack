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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.BODY_OMEMO_HINT;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Async;

import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListVAxolotlElement;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoVAxolotlElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoOmemoSupportException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.ClearTextMessage;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.omemo.listener.OmemoMucMessageListener;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Manager that allows sending messages encrypted with OMEMO.
 * This class also provides some methods useful for a client that implements OMEMO.
 *
 * @author Paul Schaub
 */

public final class OmemoManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(OmemoManager.class.getName());

    private static final WeakHashMap<XMPPConnection, WeakHashMap<Integer,OmemoManager>> INSTANCES = new WeakHashMap<>();
    private final OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> service;

    private final HashSet<OmemoMessageListener> omemoMessageListeners = new HashSet<>();
    private final HashSet<OmemoMucMessageListener> omemoMucMessageListeners = new HashSet<>();

    private OmemoService<?,?,?,?,?,?,?,?,?>.OmemoStanzaListener omemoStanzaListener;
    private OmemoService<?,?,?,?,?,?,?,?,?>.OmemoCarbonCopyListener omemoCarbonCopyListener;

    private int deviceId;

    /**
     * Private constructor to prevent multiple instances on a single connection (which probably would be bad!).
     *
     * @param connection connection
     */
    private OmemoManager(XMPPConnection connection, int deviceId) {
        super(connection);

        this.deviceId = deviceId;

        connection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                if (resumed) {
                    return;
                }
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            initialize();
                        } catch (InterruptedException | CorruptedOmemoKeyException | PubSubException.NotALeafNodeException | SmackException.NotLoggedInException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                            LOGGER.log(Level.SEVERE, "connectionListener.authenticated() failed to initialize OmemoManager: "
                                    + e.getMessage());
                        }
                    }
                });
            }
        });

        service = OmemoService.getInstance();
    }

    /**
     * Get an instance of the OmemoManager for the given connection and deviceId.
     *
     * @param connection Connection
     * @param deviceId deviceId of the Manager. If the deviceId is null, a random id will be generated.
     * @return an OmemoManager
     */
    public synchronized static OmemoManager getInstanceFor(XMPPConnection connection, Integer deviceId) {
        WeakHashMap<Integer,OmemoManager> managersOfConnection = INSTANCES.get(connection);
        if (managersOfConnection == null) {
            managersOfConnection = new WeakHashMap<>();
            INSTANCES.put(connection, managersOfConnection);
        }

        if (deviceId == null || deviceId < 1) {
            deviceId = randomDeviceId();
        }

        OmemoManager manager = managersOfConnection.get(deviceId);
        if (manager == null) {
            manager = new OmemoManager(connection, deviceId);
            managersOfConnection.put(deviceId, manager);
        }
        return manager;
    }

    /**
     * Get an instance of the OmemoManager for the given connection.
     * This method creates the OmemoManager for the stored defaultDeviceId of the connections user.
     * If there is no such id is stored, it uses a fresh deviceId and sets that as defaultDeviceId instead.
     *
     * @param connection connection
     * @return OmemoManager
     */
    public synchronized static OmemoManager getInstanceFor(XMPPConnection connection) {
        BareJid user;
        if (connection.getUser() != null) {
            user = connection.getUser().asBareJid();
        } else {
            //This might be dangerous
            try {
                user = JidCreate.bareFrom(((AbstractXMPPConnection) connection).getConfiguration().getUsername());
            } catch (XmppStringprepException e) {
                throw new AssertionError("Username is not a valid Jid. " +
                        "Use OmemoManager.gerInstanceFor(Connection, deviceId) instead.");
            }
        }

        int defaultDeviceId = OmemoService.getInstance().getOmemoStoreBackend().getDefaultDeviceId(user);
        if (defaultDeviceId < 1) {
            defaultDeviceId = randomDeviceId();
            OmemoService.getInstance().getOmemoStoreBackend().setDefaultDeviceId(user, defaultDeviceId);
        }

        return getInstanceFor(connection, defaultDeviceId);
    }

    /**
     * Initializes the OmemoManager. This method is called automatically once the client logs into the server successfully.
     *
     * @throws CorruptedOmemoKeyException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotLoggedInException
     * @throws PubSubException.NotALeafNodeException
     */
    public void initialize() throws CorruptedOmemoKeyException, InterruptedException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException,
            PubSubException.NotALeafNodeException {
        getOmemoService().initialize(this);
    }

    /**
     * OMEMO encrypt a cleartext message for a single recipient.
     *
     * @param to recipients barejid
     * @param message text to encrypt
     * @return encrypted message
     * @throws CryptoFailedException                when something crypto related fails
     * @throws UndecidedOmemoIdentityException      When there are undecided devices
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws CannotEstablishOmemoSessionException when we could not create session withs all of the recipients devices.
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public Message encrypt(BareJid to, String message) throws CryptoFailedException, UndecidedOmemoIdentityException, NoSuchAlgorithmException, InterruptedException, CannotEstablishOmemoSessionException, SmackException.NotConnectedException, SmackException.NoResponseException {
        Message m = new Message();
        m.setBody(message);
        OmemoVAxolotlElement encrypted = getOmemoService().processSendingMessage(this, to, m);
        return finishMessage(encrypted);
    }

    /**
     * OMEMO encrypt a cleartext message for multiple recipients.
     *
     * @param recipients recipients barejids
     * @param message text to encrypt
     * @return encrypted message.
     * @throws CryptoFailedException    When something crypto related fails
     * @throws UndecidedOmemoIdentityException  When there are undecided devices.
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws CannotEstablishOmemoSessionException When there is one recipient, for whom we failed to create a session
     *                                              with every one of their devices.
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public Message encrypt(ArrayList<BareJid> recipients, String message) throws CryptoFailedException, UndecidedOmemoIdentityException, NoSuchAlgorithmException, InterruptedException, CannotEstablishOmemoSessionException, SmackException.NotConnectedException, SmackException.NoResponseException {
        Message m = new Message();
        m.setBody(message);
        OmemoVAxolotlElement encrypted = getOmemoService().processSendingMessage(this, recipients, m);
        return finishMessage(encrypted);
    }

    /**
     * Encrypt a message for all recipients in the MultiUserChat.
     *
     * @param muc multiUserChat
     * @param message message to send
     * @return encrypted message
     * @throws UndecidedOmemoIdentityException when there are undecided devices.
     * @throws NoSuchAlgorithmException
     * @throws CryptoFailedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws NoOmemoSupportException When the muc doesn't support OMEMO.
     * @throws CannotEstablishOmemoSessionException when there is a user for whom we could not create a session
     *                                              with any of their devices.
     */
    public Message encrypt(MultiUserChat muc, String message) throws UndecidedOmemoIdentityException, NoSuchAlgorithmException, CryptoFailedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, NoOmemoSupportException, CannotEstablishOmemoSessionException {
        if (!multiUserChatSupportsOmemo(muc.getRoom())) {
            throw new NoOmemoSupportException();
        }
        Message m = new Message();
        m.setBody(message);
        ArrayList<BareJid> recipients = new ArrayList<>();
        for (EntityFullJid e : muc.getOccupants()) {
            recipients.add(muc.getOccupant(e).getJid().asBareJid());
        }
        return encrypt(recipients, message);
    }

    /**
     * Encrypt a message for all users we could build a session with successfully in a previous attempt.
     * This method can come in handy as a fallback when encrypting a message fails due to devices we cannot
     * build a session with.
     *
     * @param exception CannotEstablishSessionException from a previous encrypt(user(s), message) call.
     * @param message message we want to send.
     * @return encrypted message
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException when there are undecided identities.
     */
    public Message encryptForExistingSessions(CannotEstablishOmemoSessionException exception, String message) throws CryptoFailedException, UndecidedOmemoIdentityException {
        Message m = new Message();
        m.setBody(message);
        OmemoVAxolotlElement encrypted = getOmemoService().encryptOmemoMessage(this, exception.getSuccesses(), m);
        return finishMessage(encrypted);
    }

    /**
     * Decrypt an OMEMO message. This method comes handy when dealing with messages that were not automatically
     * decrypted by smack-omemo, eg. MAM query messages.
     * @param sender sender of the message
     * @param omemoMessage message
     * @return decrypted message
     * @throws InterruptedException                 Exception
     * @throws SmackException.NoResponseException   Exception
     * @throws SmackException.NotConnectedException Exception
     * @throws CryptoFailedException                When decryption fails
     * @throws XMPPException.XMPPErrorException     Exception
     * @throws CorruptedOmemoKeyException           When the used keys are invalid
     * @throws NoRawSessionException                When there is no double ratchet session found for this message
     */
    public ClearTextMessage decrypt(BareJid sender, Message omemoMessage) throws InterruptedException, SmackException.NoResponseException, SmackException.NotConnectedException, CryptoFailedException, XMPPException.XMPPErrorException, CorruptedOmemoKeyException, NoRawSessionException {
        return getOmemoService().processLocalMessage(this, sender, omemoMessage);
    }

    /**
     * Return a list of all OMEMO messages that were found in the MAM query result, that could be successfully decrypted.
     * Normal cleartext messages are also added to this list.
     *
     * @param mamQueryResult mamQueryResult
     * @return list of decrypted OmemoMessages
     * @throws InterruptedException                 Exception
     * @throws XMPPException.XMPPErrorException     Exception
     * @throws SmackException.NotConnectedException Exception
     * @throws SmackException.NoResponseException   Exception
     */
    public List<ClearTextMessage> decryptMamQueryResult(MamManager.MamQueryResult mamQueryResult) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        List<ClearTextMessage> l = new ArrayList<>();
        l.addAll(getOmemoService().decryptMamQueryResult(this, mamQueryResult));
        return l;
    }

    /**
     * Trust that a fingerprint belongs to an OmemoDevice.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     * @param device device
     * @param fingerprint fingerprint
     */
    public void trustOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        getOmemoService().getOmemoStoreBackend().trustOmemoIdentity(this, device, fingerprint);
    }

    /**
     * Distrust the fingerprint/OmemoDevice tuple.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     * @param device device
     * @param fingerprint fingerprint
     */
    public void distrustOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        getOmemoService().getOmemoStoreBackend().distrustOmemoIdentity(this, device, fingerprint);
    }

    /**
     * Returns true, if the fingerprint/OmemoDevice tuple is trusted, otherwise false.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     * @param device device
     * @param fingerprint fingerprint
     * @return
     */
    public boolean isTrustedOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        return getOmemoService().getOmemoStoreBackend().isTrustedOmemoIdentity(this, device, fingerprint);
    }

    /**
     * Returns true, if the fingerprint/OmemoDevice tuple is decided by the user.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     * @param device device
     * @param fingerprint fingerprint
     * @return
     */
    public boolean isDecidedOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        return getOmemoService().getOmemoStoreBackend().isDecidedOmemoIdentity(this, device, fingerprint);
    }

    /**
     * Clear all other devices except this one from our device list and republish the list.
     *
     * @throws InterruptedException
     * @throws SmackException
     * @throws XMPPException.XMPPErrorException
     * @throws CorruptedOmemoKeyException
     */
    public void purgeDevices() throws SmackException, InterruptedException, XMPPException.XMPPErrorException, CorruptedOmemoKeyException {
        getOmemoService().publishDeviceIdIfNeeded(this,true);
        getOmemoService().publishBundle(this);
    }

    /**
     * Generate fresh identity keys and bundle and publish it to the server.
     * @throws SmackException
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws CorruptedOmemoKeyException
     */
    public void regenerate() throws SmackException, InterruptedException, XMPPException.XMPPErrorException, CorruptedOmemoKeyException {
        //create a new identity and publish new keys to the server
        getOmemoService().regenerate(this, null);
        getOmemoService().publishDeviceIdIfNeeded(this,false);
        getOmemoService().publishBundle(this);
    }

    /**
     * Send a ratchet update message. This can be used to advance the ratchet of a session in order to maintain forward
     * secrecy.
     *
     * @param recipient recipient
     * @throws UndecidedOmemoIdentityException      When the trust of session with the recipient is not decided yet
     * @throws CorruptedOmemoKeyException           When the used identityKeys are corrupted
     * @throws CryptoFailedException                When something fails with the crypto
     * @throws CannotEstablishOmemoSessionException When we can't establish a session with the recipient
     */
    public void sendRatchetUpdateMessage(OmemoDevice recipient)
            throws CorruptedOmemoKeyException, UndecidedOmemoIdentityException, CryptoFailedException,
            CannotEstablishOmemoSessionException {
        getOmemoService().sendOmemoRatchetUpdateMessage(this, recipient, false);
    }

    /**
     * Create a new KeyTransportElement. This message will contain the AES-Key and IV that can be used eg. for encrypted
     * Jingle file transfer.
     *
     * @param aesKey    AES key to transport
     * @param iv        Initialization vector
     * @param to        list of recipient devices
     * @return          KeyTransportMessage
     * @throws UndecidedOmemoIdentityException      When the trust of session with the recipient is not decided yet
     * @throws CorruptedOmemoKeyException           When the used identityKeys are corrupted
     * @throws CryptoFailedException                When something fails with the crypto
     * @throws CannotEstablishOmemoSessionException When we can't establish a session with the recipient
     */
    public OmemoVAxolotlElement createKeyTransportElement(byte[] aesKey, byte[] iv, OmemoDevice ... to)
            throws UndecidedOmemoIdentityException, CorruptedOmemoKeyException, CryptoFailedException,
            CannotEstablishOmemoSessionException {
        return getOmemoService().prepareOmemoKeyTransportElement(this, aesKey, iv, to);
    }

    /**
     * Create a new Message from a encrypted OmemoMessageElement.
     * Add ourselves as the sender and the encrypted element.
     * Also tell the server to store the message despite a possible missing body.
     * The body will be set to a hint message that we are using OMEMO.
     *
     * @param encrypted OmemoMessageElement
     * @return Message containing the OMEMO element and some additional information
     */
    Message finishMessage(OmemoVAxolotlElement encrypted) {
        if (encrypted == null) {
            return null;
        }

        Message chatMessage = new Message();
        chatMessage.setFrom(connection().getUser().asBareJid());
        chatMessage.addExtension(encrypted);

        if (OmemoConfiguration.getAddOmemoHintBody()) {
            chatMessage.setBody(BODY_OMEMO_HINT);
        }

        if (OmemoConfiguration.getAddMAMStorageProcessingHint()) {
            StoreHint.set(chatMessage);
        }

        if (OmemoConfiguration.getAddEmeEncryptionHint()) {
            chatMessage.addExtension(new ExplicitMessageEncryptionElement(OMEMO_NAMESPACE_V_AXOLOTL, OMEMO));
        }

        return chatMessage;
    }

    /**
     * Returns true, if the contact has any active devices published in a deviceList.
     *
     * @param contact contact
     * @return true if contact has at least one OMEMO capable device.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean contactSupportsOmemo(BareJid contact) throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        getOmemoService().refreshDeviceList(this, contact);
        return !getOmemoService().getOmemoStoreBackend().loadCachedDeviceList(this, contact)
                .getActiveDevices().isEmpty();
    }

    /**
     * Returns true, if the MUC with the EntityBareJid multiUserChat is non-anonymous and members only (prerequisite
     * for OMEMO encryption in MUC).
     *
     * @param multiUserChat EntityBareJid of the MUC
     * @return true if chat supports OMEMO
     * @throws XMPPException.XMPPErrorException     if
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    public boolean multiUserChatSupportsOmemo(EntityBareJid multiUserChat) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        RoomInfo roomInfo = MultiUserChatManager.getInstanceFor(connection()).getRoomInfo(multiUserChat);
        return roomInfo.isNonanonymous() && roomInfo.isMembersOnly();
    }

    /**
     * Returns true, if the Server supports PEP.
     *
     * @param connection XMPPConnection
     * @param server domainBareJid of the server to test
     * @return true if server supports pep
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public static boolean serverSupportsOmemo(XMPPConnection connection, DomainBareJid server) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(server).containsFeature(PubSub.NAMESPACE);
    }

    /**
     * Return the fingerprint of our identity key.
     *
     * @return fingerprint
     */
    public OmemoFingerprint getOurFingerprint() {
        return getOmemoService().getOmemoStoreBackend().getFingerprint(this);
    }

    public OmemoFingerprint getFingerprint(OmemoDevice device) throws CannotEstablishOmemoSessionException {
        if (device.equals(getOwnDevice())) {
            return getOurFingerprint();
        }

        return getOmemoService().getOmemoStoreBackend().getFingerprint(this, device);
    }

    /**
     * Return all fingerprints of active devices of a contact.
     * @param contact contact
     * @return HashMap of deviceIds and corresponding fingerprints.
     */
    public HashMap<OmemoDevice, OmemoFingerprint> getActiveFingerprints(BareJid contact) {
        HashMap<OmemoDevice, OmemoFingerprint> fingerprints = new HashMap<>();
        CachedDeviceList deviceList = getOmemoService().getOmemoStoreBackend().loadCachedDeviceList(this, contact);
        for (int id : deviceList.getActiveDevices()) {
            OmemoDevice device = new OmemoDevice(contact, id);
            OmemoFingerprint fingerprint = null;
            try {
                fingerprint = getFingerprint(device);
            } catch (CannotEstablishOmemoSessionException e) {
                LOGGER.log(Level.WARNING, "Could not build session with device " + id
                        + " of user " + contact + ": " + e.getMessage());
            }

            if (fingerprint != null) {
                fingerprints.put(device, fingerprint);
            }
        }
        return fingerprints;
    }

    public void addOmemoMessageListener(OmemoMessageListener listener) {
        omemoMessageListeners.add(listener);
    }

    public void removeOmemoMessageListener(OmemoMessageListener listener) {
        omemoMessageListeners.remove(listener);
    }

    public void addOmemoMucMessageListener(OmemoMucMessageListener listener) {
        omemoMucMessageListeners.add(listener);
    }

    public void removeOmemoMucMessageListener(OmemoMucMessageListener listener) {
        omemoMucMessageListeners.remove(listener);
    }

    /**
     * Build OMEMO sessions with devices of contact.
     *
     * @param contact contact we want to build session with.
     * @throws InterruptedException
     * @throws CannotEstablishOmemoSessionException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public void buildSessionsWith(BareJid contact) throws InterruptedException, CannotEstablishOmemoSessionException, SmackException.NotConnectedException, SmackException.NoResponseException {
        getOmemoService().buildOrCreateOmemoSessionsFromBundles(this, contact);
    }

    /**
     * Request a deviceList update from contact contact.
     *
     * @param contact contact we want to obtain the deviceList from.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public void requestDeviceListUpdateFor(BareJid contact) throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        getOmemoService().refreshDeviceList(this, contact);
    }

    /**
     * Rotate the signedPreKey published in our OmemoBundle. This should be done every now and then (7-14 days).
     * The old signedPreKey should be kept for some more time (a month or so) to enable decryption of messages
     * that have been sent since the key was changed.
     *
     * @throws CorruptedOmemoKeyException When the IdentityKeyPair is damaged.
     * @throws InterruptedException XMPP error
     * @throws XMPPException.XMPPErrorException XMPP error
     * @throws SmackException.NotConnectedException XMPP error
     * @throws SmackException.NoResponseException XMPP error
     * @throws PubSubException.NotALeafNodeException if the bundle node on the server is a CollectionNode
     */
    public void rotateSignedPreKey() throws CorruptedOmemoKeyException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, PubSubException.NotALeafNodeException {
        //generate key
        getOmemoService().getOmemoStoreBackend().changeSignedPreKey(this);
        //publish
        getOmemoService().publishDeviceIdIfNeeded(this, false);
        getOmemoService().publishBundle(this);
    }

    /**
     * Return true, if the given Stanza contains an OMEMO element 'encrypted'.
     * @param stanza stanza
     * @return true if stanza has extension 'encrypted'
     */
    public static boolean stanzaContainsOmemoElement(Stanza stanza) {
        return stanza.hasExtension(OmemoElement.ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
    }

    /**
     * Throw an IllegalStateException if no OmemoService is set.
     */
    private void throwIfNoServiceSet() {
        if (service == null) {
            throw new IllegalStateException("No OmemoService set in OmemoManager.");
        }
    }

    public static int randomDeviceId() {
        int i = new Random().nextInt(Integer.MAX_VALUE);

        if (i == 0) {
            return randomDeviceId();
        }

        return Math.abs(i);
    }

    /**
     * Return the BareJid of the user.
     *
     * @return bareJid
     */
    public BareJid getOwnJid() {
        EntityFullJid fullJid = connection().getUser();
        if (fullJid == null) return null;
        return fullJid.asBareJid();
    }

    /**
     * Return the deviceId of this OmemoManager.
     *
     * @return deviceId
     */
    public int getDeviceId() {
        return deviceId;
    }

    /**
     * Return the OmemoDevice of the user.
     *
     * @return omemoDevice
     */
    public OmemoDevice getOwnDevice() {
        return new OmemoDevice(getOwnJid(), getDeviceId());
    }

    void setDeviceId(int nDeviceId) {
        INSTANCES.get(connection()).remove(getDeviceId());
        INSTANCES.get(connection()).put(nDeviceId, this);
        this.deviceId = nDeviceId;
    }

    /**
     * Notify all registered OmemoMessageListeners about a received OmemoMessage.
     *
     * @param decryptedBody      decrypted Body element of the message
     * @param encryptedMessage   unmodified message as it was received
     * @param wrappingMessage    message that wrapped the incoming message
     * @param messageInformation information about the messages encryption (used identityKey, carbon...)
     */
    void notifyOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation messageInformation) {
        for (OmemoMessageListener l : omemoMessageListeners) {
            l.onOmemoMessageReceived(decryptedBody, encryptedMessage, wrappingMessage, messageInformation);
        }
    }

    void notifyOmemoKeyTransportMessageReceived(CipherAndAuthTag cipherAndAuthTag, Message transportingMessage,
                                                Message wrappingMessage, OmemoMessageInformation information) {
        for (OmemoMessageListener l : omemoMessageListeners) {
            l.onOmemoKeyTransportReceived(cipherAndAuthTag, transportingMessage, wrappingMessage, information);
        }
    }

    /**
     * Notify all registered OmemoMucMessageListeners of an incoming OmemoMessageElement in a MUC.
     *
     * @param muc              MultiUserChat the message was received in
     * @param from             BareJid of the user that sent the message
     * @param decryptedBody    decrypted body
     * @param message          original message with encrypted content
     * @param wrappingMessage  wrapping message (in case of carbon copy)
     * @param omemoInformation information about the encryption of the message
     */
    void notifyOmemoMucMessageReceived(MultiUserChat muc, BareJid from, String decryptedBody, Message message,
                                               Message wrappingMessage, OmemoMessageInformation omemoInformation) {
        for (OmemoMucMessageListener l : omemoMucMessageListeners) {
            l.onOmemoMucMessageReceived(muc, from, decryptedBody, message,
                    wrappingMessage, omemoInformation);
        }
    }

    void notifyOmemoMucKeyTransportMessageReceived(MultiUserChat muc, BareJid from, CipherAndAuthTag cipherAndAuthTag,
                                                   Message transportingMessage, Message wrappingMessage,
                                                   OmemoMessageInformation messageInformation) {
        for (OmemoMucMessageListener l : omemoMucMessageListeners) {
            l.onOmemoKeyTransportReceived(muc, from, cipherAndAuthTag,
                    transportingMessage, wrappingMessage, messageInformation);
        }
    }

    /**
     * Remove all active stanza listeners of this manager from the connection.
     * This is somewhat the counterpart of initialize().
     */
    public void shutdown() {
        PEPManager.getInstanceFor(connection()).removePEPListener(deviceListUpdateListener);
        connection().removeAsyncStanzaListener(omemoStanzaListener);
        CarbonManager.getInstanceFor(connection()).removeCarbonCopyReceivedListener(omemoCarbonCopyListener);
    }

    /**
     * Get our connection.
     *
     * @return the connection of this manager
     */
    XMPPConnection getConnection() {
        return connection();
    }

    /**
     * Return the OMEMO service object.
     *
     * @return omemoService
     */
    OmemoService<?,?,?,?,?,?,?,?,?> getOmemoService() {
        throwIfNoServiceSet();
        return service;
    }

    PEPListener deviceListUpdateListener = new PEPListener() {
        @Override
        public void eventReceived(EntityBareJid from, EventElement event, Message message) {
            for (ExtensionElement items : event.getExtensions()) {
                if (!(items instanceof ItemsExtension)) {
                    continue;
                }

                for (ExtensionElement item : ((ItemsExtension) items).getItems()) {
                    if (!(item instanceof PayloadItem<?>)) {
                        continue;
                    }

                    PayloadItem<?> payloadItem = (PayloadItem<?>) item;

                    if (!(payloadItem.getPayload() instanceof  OmemoDeviceListVAxolotlElement)) {
                        continue;
                    }

                    //Device List <list>
                    OmemoDeviceListVAxolotlElement omemoDeviceListElement = (OmemoDeviceListVAxolotlElement) payloadItem.getPayload();
                    int ourDeviceId = getDeviceId();
                    getOmemoService().getOmemoStoreBackend().mergeCachedDeviceList(OmemoManager.this, from, omemoDeviceListElement);

                    if (from == null) {
                        //Unknown sender, no more work to do.
                        //TODO: This DOES happen for some reason. Figure out when...
                        continue;
                    }

                    if (!from.equals(getOwnJid())) {
                        //Not our deviceList, so nothing more to do
                        continue;
                    }

                    if (omemoDeviceListElement.getDeviceIds().contains(ourDeviceId)) {
                        //We are on the list. Nothing more to do
                        continue;
                    }

                    //Our deviceList and we are not on it! We don't want to miss all the action!!!
                    LOGGER.log(Level.INFO, "Our deviceId was not on the list!");
                    Set<Integer> deviceListIds = omemoDeviceListElement.copyDeviceIds();
                    //enroll at the deviceList
                    deviceListIds.add(ourDeviceId);
                    omemoDeviceListElement = new OmemoDeviceListVAxolotlElement(deviceListIds);

                    try {
                        OmemoService.publishDeviceIds(OmemoManager.this, omemoDeviceListElement);
                    } catch (SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
                        //TODO: It might be dangerous NOT to retry publishing our deviceId
                        LOGGER.log(Level.SEVERE,
                                "Could not publish our device list after an update without our id was received: "
                                        + e.getMessage());
                    }
                }
            }
        }
    };



    OmemoService<?,?,?,?,?,?,?,?,?>.OmemoStanzaListener getOmemoStanzaListener() {
        if (omemoStanzaListener == null) {
            omemoStanzaListener = getOmemoService().createStanzaListener(this);
        }
        return omemoStanzaListener;
    }

    OmemoService<?,?,?,?,?,?,?,?,?>.OmemoCarbonCopyListener getOmemoCarbonCopyListener() {
        if (omemoCarbonCopyListener == null) {
            omemoCarbonCopyListener = getOmemoService().createOmemoCarbonCopyListener(this);
        }
        return omemoCarbonCopyListener;
    }
}
