/**
 *
 * Copyright 2017 Paul Schaub, 2020 Florian Schmaus
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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Async;

import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoOmemoSupportException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.omemo.listener.OmemoMucMessageListener;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.OmemoTrustCallback;
import org.jivesoftware.smackx.omemo.trust.TrustState;
import org.jivesoftware.smackx.omemo.util.MessageOrOmemoMessage;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;

/**
 * Manager that allows sending messages encrypted with OMEMO.
 * This class also provides some methods useful for a client that implements OMEMO.
 *
 * @author Paul Schaub
 */

public final class OmemoManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(OmemoManager.class.getName());

    private static final Integer UNKNOWN_DEVICE_ID = -1;

    private static final WeakHashMap<XMPPConnection, TreeMap<Integer, OmemoManager>> INSTANCES = new WeakHashMap<>();
    private final OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> service;

    private final HashSet<OmemoMessageListener> omemoMessageListeners = new HashSet<>();
    private final HashSet<OmemoMucMessageListener> omemoMucMessageListeners = new HashSet<>();

    private final PepManager pepManager;

    private OmemoTrustCallback trustCallback;

    private BareJid ownJid;
    private Integer deviceId;

    /**
     * Private constructor.
     *
     * @param connection connection
     * @param deviceId deviceId
     */
    private OmemoManager(XMPPConnection connection, Integer deviceId) {
        super(connection);

        service = OmemoService.getInstance();
        pepManager = PepManager.getInstanceFor(connection);

        this.deviceId = deviceId;

        if (connection.isAuthenticated()) {
            initBareJidAndDeviceId(this);
        } else {
            connection.addConnectionListener(new ConnectionListener() {
                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {
                    initBareJidAndDeviceId(OmemoManager.this);
                }
            });
        }

        service.registerRatchetForManager(this);

        // StanzaListeners
        resumeStanzaAndPEPListeners();
    }

    /**
     * Return an OmemoManager instance for the given connection and deviceId.
     * If there was an OmemoManager for the connection and id before, return it. Otherwise create a new OmemoManager
     * instance and return it.
     *
     * @param connection XmppConnection.
     * @param deviceId MUST NOT be null and MUST be greater than 0.
     *
     * @return OmemoManager instance for the given connection and deviceId.
     */
    public static synchronized OmemoManager getInstanceFor(XMPPConnection connection, Integer deviceId) {
        if (deviceId == null || deviceId < 1) {
            throw new IllegalArgumentException("DeviceId MUST NOT be null and MUST be greater than 0.");
        }

        TreeMap<Integer, OmemoManager> managersOfConnection = INSTANCES.get(connection);
        if (managersOfConnection == null) {
            managersOfConnection = new TreeMap<>();
            INSTANCES.put(connection, managersOfConnection);
        }

        OmemoManager manager = managersOfConnection.get(deviceId);
        if (manager == null) {
            manager = new OmemoManager(connection, deviceId);
            managersOfConnection.put(deviceId, manager);
        }

        return manager;
    }

    /**
     * Returns an OmemoManager instance for the given connection. If there was one manager for the connection before,
     * return it. If there were multiple managers before, return the one with the lowest deviceId.
     * If there was no manager before, return a new one. As soon as the connection gets authenticated, the manager
     * will look for local deviceIDs and select the lowest one as its id. If there are not local deviceIds, the manager
     * will assign itself a random id.
     *
     * @param connection XmppConnection.
     *
     * @return OmemoManager instance for the given connection and a determined deviceId.
     */
    public static synchronized OmemoManager getInstanceFor(XMPPConnection connection) {
        TreeMap<Integer, OmemoManager> managers = INSTANCES.get(connection);
        if (managers == null) {
            managers = new TreeMap<>();
            INSTANCES.put(connection, managers);
        }

        OmemoManager manager;
        if (managers.size() == 0) {

            manager = new OmemoManager(connection, UNKNOWN_DEVICE_ID);
            managers.put(UNKNOWN_DEVICE_ID, manager);

        } else {
            manager = managers.get(managers.firstKey());
        }

        return manager;
    }

    /**
     * Set a TrustCallback for this particular OmemoManager.
     * TrustCallbacks are used to query and modify trust decisions.
     *
     * @param callback trustCallback.
     */
    public void setTrustCallback(OmemoTrustCallback callback) {
        if (trustCallback != null) {
            throw new IllegalStateException("TrustCallback can only be set once.");
        }
        trustCallback = callback;
    }

    /**
     * Return the TrustCallback of this manager.
     *
     * @return callback that is used for trust decisions.
     */
    OmemoTrustCallback getTrustCallback() {
        return trustCallback;
    }

    /**
     * Initializes the OmemoManager. This method must be called before the manager can be used.
     *
     * @throws CorruptedOmemoKeyException if the OMEMO key is corrupted.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws PubSubException.NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized void initialize()
            throws SmackException.NotLoggedInException, CorruptedOmemoKeyException, InterruptedException,
            SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotALeafNodeException, IOException {
        if (!connection().isAuthenticated()) {
            throw new SmackException.NotLoggedInException();
        }

        if (getTrustCallback() == null) {
            throw new IllegalStateException("No TrustCallback set.");
        }

        getOmemoService().init(new LoggedInOmemoManager(this));
    }

    /**
     * Initialize the manager without blocking. Once the manager is successfully initialized, the finishedCallback will
     * be notified. It will also get notified, if an error occurs.
     *
     * @param finishedCallback callback that gets called once the manager is initialized.
     */
    public void initializeAsync(final InitializationFinishedCallback finishedCallback) {
        Async.go(new Runnable() {
            @Override
            public void run() {
                try {
                    initialize();
                    finishedCallback.initializationFinished(OmemoManager.this);
                } catch (Exception e) {
                    finishedCallback.initializationFailed(e);
                }
            }
        });
    }

    /**
     * Return a set of all OMEMO capable devices of a contact.
     * Note, that this method does not explicitly refresh the device list of the contact, so it might be outdated.
     *
     * @see #requestDeviceListUpdateFor(BareJid)
     *
     * @param contact contact we want to get a set of device of.
     * @return set of known devices of that contact.
     *
     * @throws IOException if an I/O error occurred.
     */
    public Set<OmemoDevice> getDevicesOf(BareJid contact) throws IOException {
        OmemoCachedDeviceList list = getOmemoService().getOmemoStoreBackend().loadCachedDeviceList(getOwnDevice(), contact);
        HashSet<OmemoDevice> devices = new HashSet<>();

        for (int deviceId : list.getActiveDevices()) {
            devices.add(new OmemoDevice(contact, deviceId));
        }

        return devices;
    }

    /**
     * OMEMO encrypt a cleartext message for a single recipient.
     * Note that this method does NOT set the 'to' attribute of the message.
     *
     * @param recipient recipients bareJid
     * @param message text to encrypt
     * @return encrypted message
     *
     * @throws CryptoFailedException                when something crypto related fails
     * @throws UndecidedOmemoIdentityException      When there are undecided devices
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws IOException if an I/O error occurred.
     */
    public OmemoMessage.Sent encrypt(BareJid recipient, String message)
            throws CryptoFailedException, UndecidedOmemoIdentityException,
            InterruptedException, SmackException.NotConnectedException,
            SmackException.NoResponseException, SmackException.NotLoggedInException, IOException {
        Set<BareJid> recipients = new HashSet<>();
        recipients.add(recipient);
        return encrypt(recipients, message);
    }

    /**
     * OMEMO encrypt a cleartext message for multiple recipients.
     *
     * @param recipients recipients barejids
     * @param message text to encrypt
     * @return encrypted message.
     *
     * @throws CryptoFailedException    When something crypto related fails
     * @throws UndecidedOmemoIdentityException  When there are undecided devices.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized OmemoMessage.Sent encrypt(Set<BareJid> recipients, String message)
            throws CryptoFailedException, UndecidedOmemoIdentityException,
            InterruptedException, SmackException.NotConnectedException,
            SmackException.NoResponseException, SmackException.NotLoggedInException, IOException {
        LoggedInOmemoManager guard = new LoggedInOmemoManager(this);
        Set<OmemoDevice> devices = getDevicesOf(getOwnJid());
        for (BareJid recipient : recipients) {
            devices.addAll(getDevicesOf(recipient));
        }
        return service.createOmemoMessage(guard, devices, message);
    }

    /**
     * Encrypt a message for all recipients in the MultiUserChat.
     *
     * @param muc multiUserChat
     * @param message message to send
     * @return encrypted message
     *
     * @throws UndecidedOmemoIdentityException when there are undecided devices.
     * @throws CryptoFailedException if the OMEMO cryptography failed.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws NoOmemoSupportException When the muc doesn't support OMEMO.
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized OmemoMessage.Sent encrypt(MultiUserChat muc, String message)
            throws UndecidedOmemoIdentityException, CryptoFailedException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, NoOmemoSupportException,
            SmackException.NotLoggedInException, IOException {
        if (!multiUserChatSupportsOmemo(muc)) {
            throw new NoOmemoSupportException();
        }

        Set<BareJid> recipients = new HashSet<>();

        for (EntityFullJid e : muc.getOccupants()) {
            recipients.add(muc.getOccupant(e).getJid().asBareJid());
        }
        return encrypt(recipients, message);
    }

    /**
     * Manually decrypt an OmemoElement.
     * This method should only be used for use-cases, where the internal listeners don't pick up on an incoming message.
     * (for example MAM query results).
     *
     * @param sender bareJid of the message sender (must be the jid of the contact who sent the message)
     * @param omemoElement omemoElement
     * @return decrypted OmemoMessage
     *
     * @throws SmackException.NotLoggedInException if the Manager is not authenticated
     * @throws CorruptedOmemoKeyException if our or their key is corrupted
     * @throws NoRawSessionException if the message was not a preKeyMessage, but we had no session with the contact
     * @throws CryptoFailedException if decryption fails
     * @throws IOException if an I/O error occurred.
     */
    public OmemoMessage.Received decrypt(BareJid sender, OmemoElement omemoElement)
            throws SmackException.NotLoggedInException, CorruptedOmemoKeyException, NoRawSessionException,
            CryptoFailedException, IOException {
        LoggedInOmemoManager managerGuard = new LoggedInOmemoManager(this);
        return getOmemoService().decryptMessage(managerGuard, sender, omemoElement);
    }

    /**
     * Decrypt messages from a MAM query.
     *
     * @param mamQuery The MAM query
     * @return list of decrypted OmemoMessages
     *
     * @throws SmackException.NotLoggedInException if the Manager is not authenticated.
     * @throws IOException if an I/O error occurred.
     */
    public List<MessageOrOmemoMessage> decryptMamQueryResult(MamManager.MamQuery mamQuery)
            throws SmackException.NotLoggedInException, IOException {
        return new ArrayList<>(getOmemoService().decryptMamQueryResult(new LoggedInOmemoManager(this), mamQuery));
    }

    /**
     * Trust that a fingerprint belongs to an OmemoDevice.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     *
     * @param device device
     * @param fingerprint fingerprint
     */
    public void trustOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        if (trustCallback == null) {
            throw new IllegalStateException("No TrustCallback set.");
        }

        trustCallback.setTrust(device, fingerprint, TrustState.trusted);
    }

    /**
     * Distrust the fingerprint/OmemoDevice tuple.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     *
     * @param device device
     * @param fingerprint fingerprint
     */
    public void distrustOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        if (trustCallback == null) {
            throw new IllegalStateException("No TrustCallback set.");
        }

        trustCallback.setTrust(device, fingerprint, TrustState.untrusted);
    }

    /**
     * Returns true, if the fingerprint/OmemoDevice tuple is trusted, otherwise false.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     *
     * @param device device
     * @param fingerprint fingerprint
     * @return <code>true</code> if this is a trusted OMEMO identity.
     */
    public boolean isTrustedOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        if (trustCallback == null) {
            throw new IllegalStateException("No TrustCallback set.");
        }

        return trustCallback.getTrust(device, fingerprint) == TrustState.trusted;
    }

    /**
     * Returns true, if the fingerprint/OmemoDevice tuple is decided by the user.
     * The fingerprint must be the lowercase, hexadecimal fingerprint of the identityKey of the device and must
     * be of length 64.
     *
     * @param device device
     * @param fingerprint fingerprint
     * @return <code>true</code> if the trust is decided for the identity.
     */
    public boolean isDecidedOmemoIdentity(OmemoDevice device, OmemoFingerprint fingerprint) {
        if (trustCallback == null) {
            throw new IllegalStateException("No TrustCallback set.");
        }

        return trustCallback.getTrust(device, fingerprint) != TrustState.undecided;
    }

    /**
     * Send a ratchet update message. This can be used to advance the ratchet of a session in order to maintain forward
     * secrecy.
     *
     * @param recipient recipient
     *
     * @throws CorruptedOmemoKeyException           When the used identityKeys are corrupted
     * @throws CryptoFailedException                When something fails with the crypto
     * @throws CannotEstablishOmemoSessionException When we can't establish a session with the recipient
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws NoSuchAlgorithmException if no such algorithm is available.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized void sendRatchetUpdateMessage(OmemoDevice recipient)
            throws SmackException.NotLoggedInException, CorruptedOmemoKeyException, InterruptedException,
            SmackException.NoResponseException, NoSuchAlgorithmException, SmackException.NotConnectedException,
            CryptoFailedException, CannotEstablishOmemoSessionException, IOException {
        XMPPConnection connection = connection();
        MessageBuilder message = connection.getStanzaFactory()
                .buildMessageStanza()
                .to(recipient.getJid());

        OmemoElement element = getOmemoService().createRatchetUpdateElement(new LoggedInOmemoManager(this), recipient);
        message.addExtension(element);

        // Set MAM Storage hint
        StoreHint.set(message);
        connection.sendStanza(message.build());
    }

    /**
     * Returns true, if the contact has any active devices published in a deviceList.
     *
     * @param contact contact
     * @return true if contact has at least one OMEMO capable device.
     *
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws PubSubException.NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized boolean contactSupportsOmemo(BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, IOException {
        OmemoCachedDeviceList deviceList = getOmemoService().refreshDeviceList(connection(), getOwnDevice(), contact);
        return !deviceList.getActiveDevices().isEmpty();
    }

    /**
     * Returns true, if the MUC with the EntityBareJid multiUserChat is non-anonymous and members only (prerequisite
     * for OMEMO encryption in MUC).
     *
     * @param multiUserChat MUC
     * @return true if chat supports OMEMO
     *
     * @throws XMPPException.XMPPErrorException     if there was an XMPP protocol level error
     * @throws SmackException.NotConnectedException if the connection is not connected
     * @throws InterruptedException                 if the thread is interrupted
     * @throws SmackException.NoResponseException   if the server does not respond
     */
    public boolean multiUserChatSupportsOmemo(MultiUserChat multiUserChat)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        EntityBareJid jid = multiUserChat.getRoom();
        RoomInfo roomInfo = MultiUserChatManager.getInstanceFor(connection()).getRoomInfo(jid);
        return roomInfo.isNonanonymous() && roomInfo.isMembersOnly();
    }

    /**
     * Returns true, if the Server supports PEP.
     *
     * @param connection XMPPConnection
     * @param server domainBareJid of the server to test
     * @return true if server supports pep
     *
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     */
    public static boolean serverSupportsOmemo(XMPPConnection connection, DomainBareJid server)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection)
                .discoverInfo(server).containsFeature(PubSub.NAMESPACE);
    }

    /**
     * Return the fingerprint of our identity key.
     *
     * @return our own OMEMO fingerprint
     *
     * @throws SmackException.NotLoggedInException if we don't know our bareJid yet.
     * @throws CorruptedOmemoKeyException if our identityKey is corrupted.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized OmemoFingerprint getOwnFingerprint()
            throws SmackException.NotLoggedInException, CorruptedOmemoKeyException, IOException {
        if (getOwnJid() == null) {
            throw new SmackException.NotLoggedInException();
        }

        return getOmemoService().getOmemoStoreBackend().getFingerprint(getOwnDevice());
    }

    /**
     * Get the fingerprint of a contacts device.
     *
     * @param device contacts OmemoDevice
     * @return fingerprint of the given OMEMO device.
     *
     * @throws CannotEstablishOmemoSessionException if we have no session yet, and are unable to create one.
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws CorruptedOmemoKeyException if the copy of the fingerprint we have is corrupted.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized OmemoFingerprint getFingerprint(OmemoDevice device)
            throws CannotEstablishOmemoSessionException, SmackException.NotLoggedInException,
            CorruptedOmemoKeyException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, IOException {
        if (getOwnJid() == null) {
            throw new SmackException.NotLoggedInException();
        }

        if (device.equals(getOwnDevice())) {
            return getOwnFingerprint();
        }

        return getOmemoService().getOmemoStoreBackend()
                .getFingerprintAndMaybeBuildSession(new LoggedInOmemoManager(this), device);
    }

    /**
     * Return all OmemoFingerprints of active devices of a contact.
     * TODO: Make more fail-safe
     *
     * @param contact contact
     * @return Map of all active devices of the contact and their fingerprints.
     *
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws CorruptedOmemoKeyException if the OMEMO key is corrupted.
     * @throws CannotEstablishOmemoSessionException if no OMEMO session could be established.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized HashMap<OmemoDevice, OmemoFingerprint> getActiveFingerprints(BareJid contact)
            throws SmackException.NotLoggedInException, CorruptedOmemoKeyException,
            CannotEstablishOmemoSessionException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, IOException {
        if (getOwnJid() == null) {
            throw new SmackException.NotLoggedInException();
        }

        HashMap<OmemoDevice, OmemoFingerprint> fingerprints = new HashMap<>();
        OmemoCachedDeviceList deviceList = getOmemoService().getOmemoStoreBackend().loadCachedDeviceList(getOwnDevice(),
                contact);

        for (int id : deviceList.getActiveDevices()) {
            OmemoDevice device = new OmemoDevice(contact, id);
            OmemoFingerprint fingerprint = getFingerprint(device);

            if (fingerprint != null) {
                fingerprints.put(device, fingerprint);
            }
        }

        return fingerprints;
    }

    /**
     * Add an OmemoMessageListener. This listener will be informed about incoming OMEMO messages
     * (as well as KeyTransportMessages) and OMEMO encrypted message carbons.
     *
     * @param listener OmemoMessageListener
     */
    public void addOmemoMessageListener(OmemoMessageListener listener) {
        omemoMessageListeners.add(listener);
    }

    /**
     * Remove an OmemoMessageListener.
     *
     * @param listener OmemoMessageListener
     */
    public void removeOmemoMessageListener(OmemoMessageListener listener) {
        omemoMessageListeners.remove(listener);
    }

    /**
     * Add an OmemoMucMessageListener. This listener will be informed about incoming OMEMO encrypted MUC messages.
     *
     * @param listener OmemoMessageListener.
     */
    public void addOmemoMucMessageListener(OmemoMucMessageListener listener) {
        omemoMucMessageListeners.add(listener);
    }

    /**
     * Remove an OmemoMucMessageListener.
     *
     * @param listener OmemoMucMessageListener
     */
    public void removeOmemoMucMessageListener(OmemoMucMessageListener listener) {
        omemoMucMessageListeners.remove(listener);
    }

    /**
     * Request a deviceList update from contact contact.
     *
     * @param contact contact we want to obtain the deviceList from.
     *
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws IOException if an I/O error occurred.
     */
    public synchronized void requestDeviceListUpdateFor(BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, IOException {
        getOmemoService().refreshDeviceList(connection(), getOwnDevice(), contact);
    }

    /**
     * Publish a new device list with just our own deviceId in it.
     *
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws IOException if an I/O error occurred.
     * @throws PubSubException.NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     */
    public void purgeDeviceList()
            throws SmackException.NotLoggedInException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, IOException, PubSubException.NotALeafNodeException {
        getOmemoService().purgeDeviceList(new LoggedInOmemoManager(this));
    }

    /**
     * Rotate the signedPreKey published in our OmemoBundle and republish it. This should be done every now and
     * then (7-14 days). The old signedPreKey should be kept for some more time (a month or so) to enable decryption
     * of messages that have been sent since the key was changed.
     *
     * @throws CorruptedOmemoKeyException When the IdentityKeyPair is damaged.
     * @throws InterruptedException XMPP error
     * @throws XMPPException.XMPPErrorException XMPP error
     * @throws SmackException.NotConnectedException XMPP error
     * @throws SmackException.NoResponseException XMPP error
     * @throws SmackException.NotLoggedInException if the XMPP connection is not authenticated.
     * @throws IOException if an I/O error occurred.
     * @throws PubSubException.NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     */
    public synchronized void rotateSignedPreKey()
            throws CorruptedOmemoKeyException, SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            IOException, PubSubException.NotALeafNodeException {
        if (!connection().isAuthenticated()) {
            throw new SmackException.NotLoggedInException();
        }

        // generate key
        getOmemoService().getOmemoStoreBackend().changeSignedPreKey(getOwnDevice());

        // publish
        OmemoBundleElement bundle = getOmemoService().getOmemoStoreBackend().packOmemoBundle(getOwnDevice());
        OmemoService.publishBundle(connection(), getOwnDevice(), bundle);
    }

    /**
     * Return true, if the given Stanza contains an OMEMO element 'encrypted'.
     *
     * @param stanza stanza
     * @return true if stanza has extension 'encrypted'
     */
    static boolean stanzaContainsOmemoElement(Stanza stanza) {
        return stanza.hasExtension(OmemoElement.NAME_ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
    }

    /**
     * Throw an IllegalStateException if no OmemoService is set.
     */
    private void throwIfNoServiceSet() {
        if (service == null) {
            throw new IllegalStateException("No OmemoService set in OmemoManager.");
        }
    }

    /**
     * Returns a pseudo random number from the interval [1, Integer.MAX_VALUE].
     *
     * @return a random deviceId.
     */
    public static int randomDeviceId() {
        return new Random().nextInt(Integer.MAX_VALUE - 1) + 1;
    }

    /**
     * Return the BareJid of the user.
     *
     * @return our own bare JID.
     */
    public BareJid getOwnJid() {
        if (ownJid == null && connection().isAuthenticated()) {
            ownJid = connection().getUser().asBareJid();
        }

        return ownJid;
    }

    /**
     * Return the deviceId of this OmemoManager.
     *
     * @return this OmemoManagers deviceId.
     */
    public synchronized Integer getDeviceId() {
        return deviceId;
    }

    /**
     * Return the OmemoDevice of the user.
     *
     * @return our own OmemoDevice
     */
    public synchronized OmemoDevice getOwnDevice() {
        BareJid jid = getOwnJid();
        if (jid == null) {
            return null;
        }
        return new OmemoDevice(jid, getDeviceId());
    }

    /**
     * Set the deviceId of the manager to nDeviceId.
     *
     * @param nDeviceId new deviceId
     */
    synchronized void setDeviceId(int nDeviceId) {
        // Move this instance inside the HashMaps
        INSTANCES.get(connection()).remove(getDeviceId());
        INSTANCES.get(connection()).put(nDeviceId, this);

        this.deviceId = nDeviceId;
    }

    /**
     * Notify all registered OmemoMessageListeners about a received OmemoMessage.
     *
     * @param stanza original stanza
     * @param decryptedMessage decrypted OmemoMessage.
     */
    void notifyOmemoMessageReceived(Stanza stanza, OmemoMessage.Received decryptedMessage) {
        for (OmemoMessageListener l : omemoMessageListeners) {
            l.onOmemoMessageReceived(stanza, decryptedMessage);
        }
    }

    /**
     * Notify all registered OmemoMucMessageListeners of an incoming OmemoMessageElement in a MUC.
     *
     * @param muc               MultiUserChat the message was received in.
     * @param stanza            Original Stanza.
     * @param decryptedMessage  Decrypted OmemoMessage.
     */
    void notifyOmemoMucMessageReceived(MultiUserChat muc,
                                       Stanza stanza,
                                       OmemoMessage.Received decryptedMessage) {
        for (OmemoMucMessageListener l : omemoMucMessageListeners) {
            l.onOmemoMucMessageReceived(muc, stanza, decryptedMessage);
        }
    }

    /**
     * Notify all registered OmemoMessageListeners of an incoming OMEMO encrypted Carbon Copy.
     * Remember: If you want to receive OMEMO encrypted carbon copies, you have to enable carbons using
     * {@link CarbonManager#enableCarbons()}.
     *
     * @param direction             direction of the carbon copy
     * @param carbonCopy            carbon copy itself
     * @param wrappingMessage       wrapping message
     * @param decryptedCarbonCopy   decrypted carbon copy OMEMO element
     */
    void notifyOmemoCarbonCopyReceived(CarbonExtension.Direction direction,
                                       Message carbonCopy,
                                       Message wrappingMessage,
                                       OmemoMessage.Received decryptedCarbonCopy) {
        for (OmemoMessageListener l : omemoMessageListeners) {
            l.onOmemoCarbonCopyReceived(direction, carbonCopy, wrappingMessage, decryptedCarbonCopy);
        }
    }

    /**
     * Register stanza listeners needed for OMEMO.
     * This method is called automatically in the constructor and should only be used to restore the previous state
     * after {@link #stopStanzaAndPEPListeners()} was called.
     */
    public void resumeStanzaAndPEPListeners() {
        CarbonManager carbonManager = CarbonManager.getInstanceFor(connection());

        // Remove listeners to avoid them getting added twice
        connection().removeAsyncStanzaListener(this::internalOmemoMessageStanzaListener);
        carbonManager.removeCarbonCopyReceivedListener(this::internalOmemoCarbonCopyListener);

        // Add listeners
        pepManager.addPepEventListener(OmemoConstants.PEP_NODE_DEVICE_LIST, OmemoDeviceListElement.class, pepOmemoDeviceListEventListener);
        connection().addAsyncStanzaListener(this::internalOmemoMessageStanzaListener, OmemoManager::isOmemoMessage);
        carbonManager.addCarbonCopyReceivedListener(this::internalOmemoCarbonCopyListener);
    }

    /**
     * Remove active stanza listeners needed for OMEMO.
     */
    public void stopStanzaAndPEPListeners() {
        pepManager.removePepEventListener(pepOmemoDeviceListEventListener);
        connection().removeAsyncStanzaListener(this::internalOmemoMessageStanzaListener);
        CarbonManager.getInstanceFor(connection()).removeCarbonCopyReceivedListener(this::internalOmemoCarbonCopyListener);
    }

    /**
     * Build a fresh session with a contacts device.
     * This might come in handy if a session is broken.
     *
     * @param contactsDevice OmemoDevice of a contact.
     *
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     * @throws CorruptedOmemoKeyException if our or their identityKey is corrupted.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws CannotEstablishOmemoSessionException if no new session can be established.
     * @throws SmackException.NotLoggedInException if the connection is not authenticated.
     */
    public void rebuildSessionWith(OmemoDevice contactsDevice)
            throws InterruptedException, SmackException.NoResponseException, CorruptedOmemoKeyException,
            SmackException.NotConnectedException, CannotEstablishOmemoSessionException,
            SmackException.NotLoggedInException {
        if (!connection().isAuthenticated()) {
            throw new SmackException.NotLoggedInException();
        }
        getOmemoService().buildFreshSessionWithDevice(connection(), getOwnDevice(), contactsDevice);
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
     * @return the OmemoService object related to this OmemoManager.
     */
    OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> getOmemoService() {
        throwIfNoServiceSet();
        return service;
    }

    /**
     * StanzaListener that listens for incoming Stanzas which contain OMEMO elements.
     */
    private void internalOmemoMessageStanzaListener(final Stanza packet) {
        Async.go(new Runnable() {
            @Override
            public void run() {
                try {
                    getOmemoService().onOmemoMessageStanzaReceived(packet,
                            new LoggedInOmemoManager(OmemoManager.this));
                } catch (SmackException.NotLoggedInException | IOException e) {
                    LOGGER.log(Level.SEVERE, "Exception while processing OMEMO stanza", e);
                }
            }
        });
    }

    /**
     * CarbonCopyListener that listens for incoming carbon copies which contain OMEMO elements.
     */
    private void internalOmemoCarbonCopyListener(final CarbonExtension.Direction direction,
                    final Message carbonCopy,
                    final Message wrappingMessage) {
        Async.go(new Runnable() {
            @Override
            public void run() {
                if (isOmemoMessage(carbonCopy)) {
                    try {
                        getOmemoService().onOmemoCarbonCopyReceived(direction, carbonCopy, wrappingMessage,
                                new LoggedInOmemoManager(OmemoManager.this));
                    } catch (SmackException.NotLoggedInException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Exception while processing OMEMO stanza", e);
                    }
                }
            }
        });
    }

    @SuppressWarnings("UnnecessaryLambda")
    private final PepEventListener<OmemoDeviceListElement> pepOmemoDeviceListEventListener =
                    (from, receivedDeviceList, id, message) -> {
        // Device List <list>
        OmemoCachedDeviceList deviceList;
        try {
            getOmemoService().getOmemoStoreBackend().mergeCachedDeviceList(getOwnDevice(), from,
                            receivedDeviceList);

            if (!from.asBareJid().equals(getOwnJid())) {
                return;
            }

            deviceList = getOmemoService().cleanUpDeviceList(getOwnDevice());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                            "IOException while processing OMEMO PEP device updates. Message: " + message,
                                e);
            return;
        }
        final OmemoDeviceListElement_VAxolotl newDeviceList = new OmemoDeviceListElement_VAxolotl(deviceList);

        if (!newDeviceList.copyDeviceIds().equals(receivedDeviceList.copyDeviceIds())) {
            LOGGER.log(Level.FINE, "Republish deviceList due to changes:" +
                            " Received: " + Arrays.toString(receivedDeviceList.copyDeviceIds().toArray()) +
                            " Published: " + Arrays.toString(newDeviceList.copyDeviceIds().toArray()));
            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        OmemoService.publishDeviceList(connection(), newDeviceList);
                    } catch (InterruptedException | XMPPException.XMPPErrorException |
                                    SmackException.NotConnectedException | SmackException.NoResponseException | PubSubException.NotALeafNodeException e) {
                        LOGGER.log(Level.WARNING, "Could not publish our deviceList upon an received update.", e);
                    }
                }
            });
        }
    };

    /**
     * StanzaFilter that filters messages containing a OMEMO element.
     */
    private static boolean isOmemoMessage(Stanza stanza) {
        return stanza instanceof Message && OmemoManager.stanzaContainsOmemoElement(stanza);
    }

    /**
     * Guard class which ensures that the wrapped OmemoManager knows its BareJid.
     */
    public static class LoggedInOmemoManager {

        private final OmemoManager manager;

        public LoggedInOmemoManager(OmemoManager manager)
                throws SmackException.NotLoggedInException {

            if (manager == null) {
                throw new IllegalArgumentException("OmemoManager cannot be null.");
            }

            if (manager.getOwnJid() == null) {
                if (manager.getConnection().isAuthenticated()) {
                    manager.ownJid = manager.getConnection().getUser().asBareJid();
                } else {
                    throw new SmackException.NotLoggedInException();
                }
            }

            this.manager = manager;
        }

        public OmemoManager get() {
            return manager;
        }
    }

    /**
     * Callback which can be used to get notified, when the OmemoManager finished initializing.
     */
    public interface InitializationFinishedCallback {

        void initializationFinished(OmemoManager manager);

        void initializationFailed(Exception cause);
    }

    /**
     * Get the bareJid of the user from the authenticated XMPP connection.
     * If our deviceId is unknown, use the bareJid to look up deviceIds available in the omemoStore.
     * If there are ids available, choose the smallest one. Otherwise generate a random deviceId.
     *
     * @param manager OmemoManager
     */
    private static void initBareJidAndDeviceId(OmemoManager manager) {
        if (!manager.getConnection().isAuthenticated()) {
            throw new IllegalStateException("Connection MUST be authenticated.");
        }

        if (manager.ownJid == null) {
            manager.ownJid = manager.getConnection().getUser().asBareJid();
        }

        if (UNKNOWN_DEVICE_ID.equals(manager.deviceId)) {
            SortedSet<Integer> storedDeviceIds = manager.getOmemoService().getOmemoStoreBackend().localDeviceIdsOf(manager.ownJid);
            if (storedDeviceIds.size() > 0) {
                manager.setDeviceId(storedDeviceIds.first());
            } else {
                manager.setDeviceId(randomDeviceId());
            }
        }
    }
}
