/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.omemo.elements.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.elements.OmemoMessageElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.InvalidOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.omemo.listener.OmemoMucMessageListener;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;
import org.jivesoftware.smackx.omemo.util.PubSubHelper;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Encrypted.ENCRYPTED;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.*;

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
public abstract class OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    protected static final Logger LOGGER = Logger.getLogger(OmemoService.class.getName());
    protected final PubSubHelper pubSubHelper;

    protected LeafNode ownDeviceListNode;
    protected final OmemoManager omemoManager;
    protected final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore;

    private final HashSet<OmemoMessageListener<T_IdKey>> omemoMessageListeners = new HashSet<>();
    private final HashSet<OmemoMucMessageListener<T_IdKey>> omemoMucMessageListeners = new HashSet<>();

    protected final BareJid ownJid;

    /**
     * Create a new OmemoService object. This should only happen once.
     *
     * @param manager The OmemoManager we want to provide this service to
     * @param store   The OmemoStore implementation that holds the key material
     */
    public OmemoService(OmemoManager manager,
                        OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store)
            throws SmackException, InterruptedException, XMPPException.XMPPErrorException, InvalidOmemoKeyException {
        this.omemoManager = manager;
        this.omemoStore = store;
        store.setOmemoService(this); //Tell the store about us
        this.ownJid = manager.getConnection().getUser().asBareJid();
        this.pubSubHelper = new PubSubHelper(manager);
        if (getOmemoStore().isFreshInstallation()) {
            LOGGER.log(Level.INFO, "No key material found. Looks like we have a fresh installation.");
            //Create new key material and publish it to the server
            publishInformationIfNeeded(true, false);
        }
        subscribeToDeviceLists();
        registerOmemoMessageStanzaListeners();   //Wait for new OMEMO messages
        omemoStore.initializeOmemoSessions();   //Preload existing OMEMO sessions
        omemoManager.setOmemoService(this);          //Let the manager know we are ready
    }

    /**
     * Get our latest deviceListNode from the server.
     * This method is used to prevent us from getting our node too often (it may take some time)
     */
    private void fetchLatestDeviceListNode() throws SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        LOGGER.log(Level.INFO, "Fetching latest device list node...");
        this.ownDeviceListNode = getPubSubHelper().getNode(null, PEP_NODE_DEVICE_LIST);
        LOGGER.log(Level.INFO, "Latest device list node fetched.");
    }

    /**
     * Publish our deviceId and a fresh bundle to the server
     *
     * @param regenerate         Do we want to generate a new Identity?
     * @param deleteOtherDevices Do we want to delete other devices from our deviceList?
     */
    void publishInformationIfNeeded(boolean regenerate, boolean deleteOtherDevices) throws InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            InvalidOmemoKeyException {
        if (regenerate) {
            //Generate unique ID that is not already taken
            int deviceIdCandidate;
            do {
                deviceIdCandidate = omemoStore.generateOmemoDeviceId();
            } while (!omemoStore.isAvailableDeviceId(deviceIdCandidate));
            omemoStore.storeOmemoDeviceId(deviceIdCandidate);
            omemoStore.regenerate();
        }
        publishDeviceIdIfNeeded(deleteOtherDevices);

        publishBundle();
    }

    /**
     * Publish a fresh bundle to the server.
     */
    private void publishBundle()
            throws SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, InvalidOmemoKeyException, XMPPException.XMPPErrorException {
        LOGGER.log(Level.INFO, "Publishing bundle...");
        LeafNode bundleNode = getPubSubHelper()
                .getNode(ownJid, PEP_NODE_BUNDLE_FROM_DEVICE_ID(omemoStore.loadOmemoDeviceId()));
        bundleNode.send(new PayloadItem<>(omemoStore.packOmemoBundle()));
        LOGGER.log(Level.INFO, "Bundle published!");

    }

    /**
     * Publish our deviceId in case it is not on the list already.
     *
     * @param deleteOtherDevices Do we want to remove other devices from the list?
     *                           If we do, publish the list with only our id, regardless if we were on the list
     *                           already.
     */
    private void publishDeviceIdIfNeeded(boolean deleteOtherDevices)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            XMPPException.XMPPErrorException {

        fetchLatestDeviceListNode();
        LOGGER.log(Level.INFO, "Publish Device Id if needed...");
        OmemoDeviceListElement lastKnown;

        LOGGER.log(Level.INFO, "Extract Device List from last node.");
        lastKnown = getPubSubHelper().extractDeviceListFrom(ownDeviceListNode);

        if (lastKnown == null) {
            LOGGER.log(Level.INFO, "Last deviceList was null.");
            lastKnown = new OmemoDeviceListElement();
        }

        if (deleteOtherDevices) {
            LOGGER.log(Level.INFO, "Deleting other device from the deviceList.");
            lastKnown.clear();
        }

        int ourDeviceId = omemoStore.loadOmemoDeviceId();
        if (!lastKnown.contains(ourDeviceId)) {
            lastKnown.add(ourDeviceId);
            LOGGER.log(Level.INFO, "Last deviceList did not contain our id " + ourDeviceId + ". Publish it.");
            if (ownDeviceListNode == null) {
                fetchLatestDeviceListNode();
            }

            if (ownDeviceListNode != null) {
                ownDeviceListNode.send(new PayloadItem<>(lastKnown));
            }
        }
        LOGGER.log(Level.INFO, "Success!");
    }

    /**
     * Subscribe to the device lists of our contacts using PEP
     */
    private void subscribeToDeviceLists() {
        LOGGER.log(Level.INFO, "Subscribe to device Lists.");
        registerDeviceListListener();
        ServiceDiscoveryManager.getInstanceFor(omemoManager.getConnection()).addFeature(PEP_NODE_DEVICE_LIST_NOTIFY);
    }

    /**
     * Build sessions for all devices of the contact that we do not have a session with yet
     *
     * @param jid the BareJid of the contact
     */
    private void buildSessionsFromOmemoBundles(BareJid jid) {
        CachedDeviceList devices = omemoStore.loadCachedDeviceList(jid);
        if (devices == null) {
            try {
                omemoStore.mergeCachedDeviceList(jid, pubSubHelper.fetchDeviceList(jid));
            } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
        devices = omemoStore.loadCachedDeviceList(jid);
        if (devices == null) {
            return;
        }

        for (int id : devices.getActiveDevices()) {
            OmemoDevice device = new OmemoDevice(jid, id);
            if (omemoStore.getOmemoSessionOf(device) == null) {
                try {
                    LOGGER.log(Level.INFO, "Build session for " + device);
                    buildSessionFromOmemoBundle(device);
                } catch (CannotEstablishOmemoSessionException | InvalidOmemoKeyException e) {
                    e.printStackTrace();
                    //Skip
                }
            }
        }
    }

    /**
     * Build an OmemoSession for the given OmemoDevice.
     *
     * @param device OmemoDevice
     * @throws CannotEstablishOmemoSessionException when no session could be established
     */
    public void buildSessionFromOmemoBundle(OmemoDevice device) throws CannotEstablishOmemoSessionException, InvalidOmemoKeyException {
        if (device.equals(new OmemoDevice(ownJid, omemoStore.loadOmemoDeviceId()))) {
            LOGGER.log(Level.INFO, "Do not build a session with yourself!");
            return;
        }
        OmemoBundleElement bundle = null;
        try {
            bundle = pubSubHelper.fetchBundle(device);
        } catch (SmackException | XMPPException.XMPPErrorException | InterruptedException e) {
            e.printStackTrace();
        }
        if (bundle == null) {
            LOGGER.log(Level.WARNING, "Couldn't build session for " + device);
            throw new CannotEstablishOmemoSessionException("Can't build Session for " + device);
        }
        HashMap<Integer, T_Bundle> bundles;
        try {
            bundles = getOmemoStore().keyUtil().BUNDLE.bundles(bundle, device);
        } catch (InvalidOmemoKeyException e) {
            LOGGER.log(Level.SEVERE, "Bundle contained invalid OmemoIdentityKey.");
            throw e;
        }
        int randomIndex = new Random().nextInt(bundles.size());
        T_Bundle randomPreKeyBundle = new ArrayList<>(bundles.values()).get(randomIndex);
        processBundle(randomPreKeyBundle, device);
    }

    /**
     * Process a received bundle. Typically that includes saving keys and building a session
     *
     * @param bundle T_Bundle (depends on used Signal/Olm library)
     * @param device OmemoDevice
     */
    protected abstract void processBundle(T_Bundle bundle, OmemoDevice device) throws InvalidOmemoKeyException;

    /**
     * Register a PEPListener that listens for deviceList updates.
     */
    private void registerDeviceListListener() {
        PEPManager.getInstanceFor(omemoManager.getConnection()).addPEPListener(new PEPListener() {
            @Override
            public void eventReceived(EntityBareJid from, EventElement event, Message message) {
                for (ExtensionElement items : event.getExtensions()) {
                    if (items instanceof ItemsExtension) {
                        for (ExtensionElement item : ((ItemsExtension) items).getItems()) {
                            if (item instanceof PayloadItem<?>) {
                                PayloadItem<?> payloadItem = (PayloadItem<?>) item;
                                //Device List <list>
                                if (payloadItem.getPayload() instanceof OmemoDeviceListElement) {
                                    OmemoDeviceListElement omemoDeviceListElement = (OmemoDeviceListElement) payloadItem.getPayload();
                                    if (omemoStore != null) {
                                        omemoStore.mergeCachedDeviceList(from, omemoDeviceListElement);
                                    }
                                    if (from != null && from.equals(ownJid) && !omemoDeviceListElement.contains(omemoStore.loadOmemoDeviceId())) {
                                        //Our deviceId was not in our list!
                                        try {
                                            publishDeviceIdIfNeeded(false);
                                        } catch (SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
                                            //TODO: It might be dangerous NOT to retry publishing our deviceId
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Process a received message. Try to decrypt it in case we are a recipient device. If we are not a recipient
     * device, or decryption fails, return null.
     *
     * @param sender  the BareJid of the sender of the message
     * @param message the encrypted message
     * @return decrypted message or null
     */
    private Message processReceivingMessage(BareJid sender, OmemoMessageElement message, final OmemoMessageInformation<T_IdKey> information) throws CryptoFailedException {
        ArrayList<OmemoMessageElement.OmemoHeader.Key> messageRecipientKeys = message.getHeader().getKeys();
        for (OmemoMessageElement.OmemoHeader.Key k : messageRecipientKeys) {
            if (k.getId() == omemoStore.loadOmemoDeviceId()) {
                LOGGER.log(Level.INFO, "Found a key with our deviceId! Try to decrypt the message!");
                return decryptOmemoMessage(new OmemoDevice(sender, message.getHeader().getSid()), message, information);
            }
        }
        LOGGER.log(Level.INFO, "There is no key with our deviceId. Silently discard the message.");
        return null;
    }

    /**
     * Encrypt a clear text message for the given recipient.
     * The body of the message will be encrypted.
     *
     * @param recipient BareJid of the recipient
     * @param message   message to encrypt.
     * @return OmemoMessageElement
     */
    OmemoMessageElement processSendingMessage(BareJid recipient, Message message) throws CryptoFailedException, UndecidedOmemoIdentityException {
        ArrayList<BareJid> recipients = new ArrayList<>();
        recipients.add(recipient);
        return processSendingMessage(recipients, message);
    }

    /**
     * Encrypt a clear text message for the given recipients.
     * The body of the message will be encrypted.
     *
     * @param recipients List of BareJids of all recipients
     * @param message    message to encrypt.
     * @return OmemoMessageElement
     */
    OmemoMessageElement processSendingMessage(List<BareJid> recipients, Message message) throws CryptoFailedException, UndecidedOmemoIdentityException {
        //Them - The contact wants to read the message on all their devices.
        //Fetch a fresh list in case we had none before.
        List<OmemoDevice> receivers = new ArrayList<>();
        for (BareJid recipient : recipients) {
            if (recipient.equals(ownJid)) {
                //Skip our jid
                continue;
            }
            buildSessionsFromOmemoBundles(recipient);
            CachedDeviceList theirDevices = omemoStore.loadCachedDeviceList(recipient);
            for (int id : theirDevices.getActiveDevices()) {
                receivers.add(new OmemoDevice(recipient, id));
            }
        }

        //TODO: What if the recipients list does not exist/not contain any of their keys (they do not support OMEMO)?

        //Us - We want to read the message on all of our devices
        CachedDeviceList ourDevices = omemoStore.loadCachedDeviceList(ownJid);
        if (ourDevices == null) {
            ourDevices = new CachedDeviceList();
        }
        for (int id : ourDevices.getActiveDevices()) {
            if (id != omemoStore.loadOmemoDeviceId()) {
                receivers.add(new OmemoDevice(ownJid, id));
            }
        }

        return encryptOmemoMessage(receivers, message);
    }

    /**
     * Decrypt a incoming OmemoMessageElement that was sent by the OmemoDevice 'from'
     *
     * @param from    OmemoDevice that sent the message
     * @param message Encrypted OmemoMessageElement
     * @return Decrypted message
     * @throws CryptoFailedException when decrypting message fails for some reason
     */
    protected Message decryptOmemoMessage(OmemoDevice from, OmemoMessageElement message, final OmemoMessageInformation<T_IdKey> information) throws CryptoFailedException {
        int preKeyCountBefore = getOmemoStore().loadOmemoPreKeys().size();
        Message decrypted;

        //Get the session that will decrypt the message. If we have no such session, create a new one.
        OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> session = omemoStore.getOmemoSessionOf(from);
        if (session != null) {
            decrypted = message.decrypt(session, omemoStore.loadOmemoDeviceId());
        } else {
            session = createSession(from);
            decrypted = message.decrypt(session, omemoStore.loadOmemoDeviceId());
        }

        information.setSenderDevice(from);
        information.setSenderIdentityKey(session.getIdentityKey());

        // Check, if we use up a preKey (the message was a PreKeyMessage)
        // If we did, republish a bundle with the used keys replaced with fresh keys
        // TODO: Do this AFTER returning the message?
        if (getOmemoStore().loadOmemoPreKeys().size() != preKeyCountBefore) {
            LOGGER.log(Level.INFO, "We used up a preKey. Publish new Bundle.");
            try {
                publishBundle();
            } catch (InvalidOmemoKeyException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            }
        }
        return decrypted;
    }

    /**
     * Encrypt the message and return it as an OmemoMessageElement
     *
     * @param receivers List of devices that will be able to decipher the message.
     * @param message   Clear text message
     * @return OmemoMessageElement
     */
    protected OmemoMessageElement encryptOmemoMessage(List<OmemoDevice> receivers, Message message) throws CryptoFailedException, UndecidedOmemoIdentityException {
        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                builder = new OmemoMessageBuilder<>(omemoStore, message.getBody());

        UndecidedOmemoIdentityException undecided = null;

        for (OmemoDevice c : receivers) {
            try {
                builder.addRecipient(c);
            } catch (CannotEstablishOmemoSessionException e) {
                //TODO: How to react?
                LOGGER.log(Level.WARNING, e.getMessage());
                e.printStackTrace();
            } catch (InvalidOmemoKeyException e) {
                //TODO: Same here
                e.printStackTrace();
            } catch (UndecidedOmemoIdentityException e) {
                //Collect all undecided devices
                if (undecided == null) {
                    undecided = e;
                } else {
                    undecided.join(e);
                }
            }
        }

        if (undecided != null) {
            throw undecided;
        }
        return builder.finish();
    }

    /**
     * Create a new crypto-specific Session object
     *
     * @param from the device we want to create the session with.
     * @return a new session
     */
    protected abstract OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createSession(OmemoDevice from);

    /**
     * Return our OmemoStore
     *
     * @return our store
     */
    OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoStore() {
        return this.omemoStore;
    }

    /**
     * Return our PubSubHelper
     *
     * @return PubSubHelper
     */
    public PubSubHelper getPubSubHelper() {
        return this.pubSubHelper;
    }

    /**
     * Listen for incoming messages and carbons, decrypt them and pass the cleartext messages to the registered
     * OmemoMessageListeners.
     */
    private void registerOmemoMessageStanzaListeners() {
        omemoManager.getConnection().addAsyncStanzaListener(omemoMessageListener, omemoMessageFilter);
        //Carbons
        CarbonManager.getInstanceFor(omemoManager.getConnection())
                .addCarbonCopyReceivedListener(omemoCarbonMessageListener);
    }

    /**
     * StanzaFilter that filters messages containing a OMEMO message element
     */
    private final StanzaFilter omemoMessageFilter = new StanzaFilter() {
        @Override
        public boolean accept(Stanza stanza) {
            return stanza instanceof Message && stanza.hasExtension(ENCRYPTED, OMEMO_NAMESPACE);
        }
    };

    /**
     * StanzaListener that listens for incoming OMEMO messages
     */
    private final StanzaListener omemoMessageListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
            Message decrypted;
            Jid sender = packet.getFrom();
            MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(omemoManager.getConnection());
            OmemoMessageInformation<T_IdKey> messageInfo = new OmemoMessageInformation<>();

            //Is it a MUC message...
            if (mucm.getJoinedRooms().contains(sender.asBareJid().asEntityBareJidIfPossible())) {
                MultiUserChat muc = mucm.getMultiUserChat(sender.asEntityBareJidIfPossible());
                BareJid senderContact = muc.getOccupant(sender.asEntityFullJidIfPossible()).getJid().asBareJid();
                LOGGER.log(Level.INFO, "Received a MUC message from " + senderContact + " in MUC " + muc.getRoom().asBareJid());
                try {
                    decrypted = processReceivingMessage(senderContact, (OmemoMessageElement) packet.getExtension(ENCRYPTED, OMEMO_NAMESPACE), messageInfo);
                    if (decrypted != null) {
                        notifyOmemoMucMessageReceived(muc, senderContact, decrypted.getBody(), (Message) packet, null, messageInfo);
                    }
                } catch (CryptoFailedException e) {
                    e.printStackTrace();
                }
            }

            //... or a normal chat message...
            else {
                try {
                    decrypted = processReceivingMessage(
                            packet.getFrom().asBareJid(),
                            (OmemoMessageElement) packet.getExtension(ENCRYPTED, OMEMO_NAMESPACE), messageInfo);
                    if (decrypted != null) {
                        notifyOmemoMessageReceived(decrypted.getBody(), (Message) packet, null, messageInfo);
                    }
                } catch (CryptoFailedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * CarbonCopyListener that listens for incoming OMEMO message carbon copies.
     */
    private final CarbonCopyReceivedListener omemoCarbonMessageListener = new CarbonCopyReceivedListener() {
        @Override
        public void onCarbonCopyReceived(CarbonExtension.Direction direction, Message carbonCopy, Message wrappingMessage) {
            if (omemoMessageFilter.accept(carbonCopy)) {
                Message decrypted;
                Jid sender = carbonCopy.getFrom();
                MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(omemoManager.getConnection());
                OmemoMessageInformation<T_IdKey> messageInfo = new OmemoMessageInformation<>();
                if (CarbonExtension.Direction.received.equals(direction)) {
                    messageInfo.setCarbon(OmemoMessageInformation.CARBON.RECV);
                } else {
                    messageInfo.setCarbon(OmemoMessageInformation.CARBON.SENT);
                }

                //Is it a MUC message...
                if (mucm.getJoinedRooms().contains(sender.asBareJid().asEntityBareJidIfPossible())) {
                    LOGGER.log(Level.INFO, "Received a MUC message");
                    MultiUserChat muc = mucm.getMultiUserChat(sender.asEntityBareJidIfPossible());
                    BareJid senderContact = muc.getOccupant(sender.asEntityFullJidIfPossible()).getJid().asBareJid();
                    LOGGER.log(Level.INFO, "Sender was probably " + senderContact);
                    try {
                        decrypted = processReceivingMessage(senderContact, (OmemoMessageElement) carbonCopy.getExtension(ENCRYPTED, OMEMO_NAMESPACE), messageInfo);
                        if (decrypted != null) {
                            notifyOmemoMucMessageReceived(muc, senderContact, decrypted.getBody(), carbonCopy, wrappingMessage, messageInfo);
                        }
                    } catch (CryptoFailedException e) {
                        e.printStackTrace();
                    }
                }

                //... or a normal chat message...
                else {
                    try {
                        decrypted = processReceivingMessage(carbonCopy.getFrom().asBareJid(),
                                (OmemoMessageElement) carbonCopy.getExtension(ENCRYPTED, OMEMO_NAMESPACE), messageInfo);
                        if (decrypted != null) {
                            notifyOmemoMessageReceived(decrypted.getBody(), carbonCopy, wrappingMessage, messageInfo);
                        }
                    } catch (CryptoFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    /**
     * Add an OmemoMessageListener, which the client can use to get updated when OmemoMessages are received in normal chat
     * sessions.
     *
     * @param listener OmemoMessageListener
     */
    @SuppressWarnings("unused")
    public void addOmemoMessageListener(OmemoMessageListener<T_IdKey> listener) {
        this.omemoMessageListeners.add(listener);
    }

    /**
     * Add an OmemoMucMessageListener, which the client can use to get updated when an OmemoMessageElement is received in a
     * MUC.
     *
     * @param listener OmemoMucMessageListener
     */
    @SuppressWarnings("unused")
    public void addOmemoMucMessageListener(OmemoMucMessageListener<T_IdKey> listener) {
        this.omemoMucMessageListeners.add(listener);
    }

    /**
     * Remove an OmemoMessageListener
     *
     * @param listener OmemoMessageListener
     */
    @SuppressWarnings("unused")
    public void removeOmemoMessageListener(OmemoMessageListener<T_IdKey> listener) {
        this.omemoMessageListeners.remove(listener);
    }

    /**
     * Remove an OmemoMucMessageListener
     *
     * @param listener OmemoMucMessageListener
     */
    @SuppressWarnings("unused")
    public void removeOmemoMucMessageListener(OmemoMucMessageListener<T_IdKey> listener) {
        this.omemoMucMessageListeners.remove(listener);
    }

    /**
     * Notify all registered OmemoMessageListeners about a received OmemoMessage
     *
     * @param decryptedBody      decrypted Body element of the message
     * @param encryptedMessage   unmodified message as it was received
     * @param messageInformation information about the messages encryption (used identityKey, carbon...)
     */
    private void notifyOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation<T_IdKey> messageInformation) {
        for (OmemoMessageListener<T_IdKey> l : omemoMessageListeners) {
            l.onOmemoMessageReceived(decryptedBody, encryptedMessage, wrappingMessage, messageInformation);
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
    private void notifyOmemoMucMessageReceived(MultiUserChat muc, BareJid from, String decryptedBody, Message message,
                                               Message wrappingMessage, OmemoMessageInformation<T_IdKey> omemoInformation) {
        for (OmemoMucMessageListener<T_IdKey> l : omemoMucMessageListeners) {
            l.onOmemoMucMessageReceived(muc, from, decryptedBody, message,
                    wrappingMessage, omemoInformation);
        }
    }
}
