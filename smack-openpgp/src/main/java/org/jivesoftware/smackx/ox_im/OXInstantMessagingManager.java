/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox_im;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.crypto.OpenPgpElementAndMetadata;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.listener.SigncryptElementReceivedListener;

import org.bouncycastle.openpgp.PGPException;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.pgpainless.decryption_verification.OpenPgpMetadata;
import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * Entry point of Smacks API for XEP-0374: OpenPGP for XMPP: Instant Messaging.
 *
 * <h2>Setup</h2>
 *
 * In order to set up OX Instant Messaging, please first follow the setup routines of the {@link OpenPgpManager}, then
 * do the following steps:
 *
 * <h3>Acquire an {@link OXInstantMessagingManager} instance.</h3>
 *
 * <pre>
 * {@code
 * OXInstantMessagingManager instantManager = OXInstantMessagingManager.getInstanceFor(connection);
 * }
 * </pre>
 *
 * <h3>Listen for OX messages</h3>
 * In order to listen for incoming OX:IM messages, you have to register a listener.
 *
 * <pre>
 * {@code
 * instantManager.addOxMessageListener(
 *          new OxMessageListener() {
 *              void newIncomingOxMessage(OpenPgpContact contact,
 *                                        Message originalMessage,
 *                                        SigncryptElement decryptedPayload) {
 *                  Message.Body body = decryptedPayload.<Message.Body>getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE);
 *                  ...
 *              }
 *          });
 * }
 * </pre>
 *
 * <h3>Finally, announce support for OX:IM</h3>
 * In order to let your contacts know, that you support message encrypting using the OpenPGP for XMPP: Instant Messaging
 * profile, you have to announce support for OX:IM.
 *
 * <pre>
 * {@code
 * instantManager.announceSupportForOxInstantMessaging();
 * }
 * </pre>
 *
 * <h2>Sending messages</h2>
 * In order to send an OX:IM message, just do
 *
 * <pre>
 * {@code
 * instantManager.sendOxMessage(openPgpManager.getOpenPgpContact(contactsJid), "Hello World");
 * }
 * </pre>
 *
 * Note, that you have to decide, whether to trust the contacts keys prior to sending a message, otherwise undecided
 * keys are not included in the encryption process. You can trust keys by calling
 * {@link OpenPgpContact#trust(OpenPgpV4Fingerprint)}. Same goes for your own keys! In order to determine, whether
 * there are undecided keys, call {@link OpenPgpContact#hasUndecidedKeys()}. The trust state of a single key can be
 * determined using {@link OpenPgpContact#getTrust(OpenPgpV4Fingerprint)}.
 *
 * Note: This implementation does not yet have support for sending/receiving messages to/from MUCs.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0374.html">
 *     XEP-0374: OpenPGP for XMPP: Instant Messaging</a>
 */
public final class OXInstantMessagingManager extends Manager {

    public static final String NAMESPACE_0 = "urn:xmpp:openpgp:im:0";

    private static final Map<XMPPConnection, OXInstantMessagingManager> INSTANCES = new WeakHashMap<>();

    private final Set<OxMessageListener> oxMessageListeners = new HashSet<>();
    private final OpenPgpManager openPgpManager;

    private OXInstantMessagingManager(final XMPPConnection connection) {
        super(connection);
        openPgpManager = OpenPgpManager.getInstanceFor(connection);
        openPgpManager.registerSigncryptReceivedListener(signcryptElementReceivedListener);
        announceSupportForOxInstantMessaging();
    }

    /**
     * Return an instance of the {@link OXInstantMessagingManager} that belongs to the given {@code connection}.
     *
     * @param connection XMPP connection
     * @return manager instance
     */
    public static synchronized OXInstantMessagingManager getInstanceFor(XMPPConnection connection) {
        OXInstantMessagingManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new OXInstantMessagingManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    /**
     * Add the OX:IM namespace as a feature to our disco features.
     */
    public void announceSupportForOxInstantMessaging() {
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(NAMESPACE_0);
    }

    /**
     * Determine, whether a contact announces support for XEP-0374: OpenPGP for XMPP: Instant Messaging.
     *
     * @param jid {@link BareJid} of the contact in question.
     * @return true if contact announces support, otherwise false.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws InterruptedException if the thread gets interrupted
     * @throws SmackException.NoResponseException if the server doesn't respond
     */
    public boolean contactSupportsOxInstantMessaging(BareJid jid)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, NAMESPACE_0);
    }

    /**
     * Determine, whether a contact announces support for XEP-0374: OpenPGP for XMPP: Instant Messaging.
     *
     * @param contact {@link OpenPgpContact} in question.
     * @return true if contact announces support, otherwise false.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws SmackException.NoResponseException if the server doesn't respond
     */
    public boolean contactSupportsOxInstantMessaging(OpenPgpContact contact)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return contactSupportsOxInstantMessaging(contact.getJid());
    }

    /**
     * Add an {@link OxMessageListener}. The listener gets notified about incoming {@link OpenPgpMessage}s which
     * contained an OX-IM message.
     *
     * @param listener listener
     * @return true if the listener gets added, otherwise false.
     */
    public boolean addOxMessageListener(OxMessageListener listener) {
        return oxMessageListeners.add(listener);
    }

    /**
     * Remove an {@link OxMessageListener}. The listener will no longer be notified about OX-IM messages.
     *
     * @param listener listener
     * @return true, if the listener gets removed, otherwise false
     */
    public boolean removeOxMessageListener(OxMessageListener listener) {
        return oxMessageListeners.remove(listener);
    }

    /**
     * Send an OX message to a {@link OpenPgpContact}. The message will be encrypted to all active keys of the contact,
     * as well as all of our active keys. The message is also signed with our key.
     *
     * @param contact contact capable of OpenPGP for XMPP: Instant Messaging.
     * @param body message body.
     *
     * @return {@link OpenPgpMetadata} about the messages encryption + signatures.
     *
     * @throws InterruptedException if the thread is interrupted
     * @throws IOException IO is dangerous
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws SmackException.NotLoggedInException if we are not logged in
     * @throws PGPException PGP is brittle
     */
    public OpenPgpMetadata sendOxMessage(OpenPgpContact contact, CharSequence body)
            throws InterruptedException, IOException,
            SmackException.NotConnectedException, SmackException.NotLoggedInException, PGPException {
        Message message = new Message(contact.getJid());
        Message.Body mBody = new Message.Body(null, body.toString());

        OpenPgpMetadata metadata = addOxMessage(message, contact, Collections.<ExtensionElement>singletonList(mBody));

        ChatManager.getInstanceFor(connection()).chatWith(contact.getJid().asEntityBareJidIfPossible()).send(message);

        return metadata;
    }

    /**
     * Add an OX-IM message element to a message.
     *
     * @param message message
     * @param contact recipient of the message
     * @param payload payload which will be encrypted and signed
     *
     * @return {@link OpenPgpMetadata} about the messages encryption + metadata.
     *
     * @throws SmackException.NotLoggedInException in case we are not logged in
     * @throws PGPException in case something goes wrong during encryption
     * @throws IOException IO is dangerous (we need to read keys)
     */
    public OpenPgpMetadata addOxMessage(Message message, OpenPgpContact contact, List<ExtensionElement> payload)
            throws SmackException.NotLoggedInException, PGPException, IOException {
        return addOxMessage(message, Collections.singleton(contact), payload);
    }

    /**
     * Add an OX-IM message element to a message.
     *
     * @param message message
     * @param contacts recipients of the message
     * @param payload payload which will be encrypted and signed
     *
     * @return metadata about the messages encryption + signatures.
     *
     * @throws SmackException.NotLoggedInException in case we are not logged in
     * @throws PGPException in case something goes wrong during encryption
     * @throws IOException IO is dangerous (we need to read keys)
     */
    public OpenPgpMetadata addOxMessage(Message message, Set<OpenPgpContact> contacts, List<ExtensionElement> payload)
            throws SmackException.NotLoggedInException, IOException, PGPException {

        HashSet<OpenPgpContact> recipients = new HashSet<>(contacts);
        OpenPgpContact self = openPgpManager.getOpenPgpSelf();
        recipients.add(self);

        OpenPgpElementAndMetadata openPgpElementAndMetadata = signAndEncrypt(recipients, payload);
        message.addExtension(openPgpElementAndMetadata.getElement());

        // Set hints on message
        ExplicitMessageEncryptionElement.set(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0);
        StoreHint.set(message);
        setOXBodyHint(message);

        return openPgpElementAndMetadata.getMetadata();
    }

    /**
     * Wrap some {@code payload} into a {@link SigncryptElement}, sign and encrypt it for {@code contacts} and ourselves.
     *
     * @param contacts recipients of the message
     * @param payload payload which will be encrypted and signed
     *
     * @return encrypted and signed {@link OpenPgpElement}, along with {@link OpenPgpMetadata} about the
     * encryption + signatures.
     *
     * @throws SmackException.NotLoggedInException in case we are not logged in
     * @throws IOException IO is dangerous (we need to read keys)
     * @throws PGPException in case encryption goes wrong
     */
    public OpenPgpElementAndMetadata signAndEncrypt(Set<OpenPgpContact> contacts, List<ExtensionElement> payload)
            throws SmackException.NotLoggedInException, IOException, PGPException {

        Set<Jid> jids = new HashSet<>();
        for (OpenPgpContact contact : contacts) {
            jids.add(contact.getJid());
        }
        jids.add(openPgpManager.getOpenPgpSelf().getJid());

        SigncryptElement signcryptElement = new SigncryptElement(jids, payload);
        OpenPgpElementAndMetadata encrypted = openPgpManager.getOpenPgpProvider().signAndEncrypt(signcryptElement,
                openPgpManager.getOpenPgpSelf(), contacts);

        return encrypted;
    }

    /**
     * Manually decrypt and verify an {@link OpenPgpElement}.
     *
     * @param element encrypted, signed {@link OpenPgpElement}.
     * @param sender sender of the message.
     *
     * @return decrypted, verified message
     *
     * @throws SmackException.NotLoggedInException In case we are not logged in (we need our jid to access our keys)
     * @throws PGPException in case of an PGP error
     * @throws IOException in case of an IO error (reading keys, streams etc)
     * @throws XmlPullParserException in case that the content of the {@link OpenPgpElement} is not a valid
     * {@link OpenPgpContentElement} or broken XML.
     * @throws IllegalArgumentException if the elements content is not a {@link SigncryptElement}. This happens, if the
     * element likely is not an OX message.
     */
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, OpenPgpContact sender)
            throws SmackException.NotLoggedInException, PGPException, IOException, XmlPullParserException {

        OpenPgpMessage decrypted = openPgpManager.decryptOpenPgpElement(element, sender);
        if (decrypted.getState() != OpenPgpMessage.State.signcrypt) {
            throw new IllegalArgumentException("Decrypted message does appear to not be an OX message. (State: " + decrypted.getState() + ")");
        }

        return decrypted;
    }

    /**
     * Set a hint about the message being OX-IM encrypted as body of the message.
     *
     * @param message message
     */
    private static void setOXBodyHint(Message message) {
        message.setBody("This message is encrypted using XEP-0374: OpenPGP for XMPP: Instant Messaging.");
    }

    private final SigncryptElementReceivedListener signcryptElementReceivedListener = new SigncryptElementReceivedListener() {
        @Override
        public void signcryptElementReceived(OpenPgpContact contact, Message originalMessage, SigncryptElement signcryptElement, OpenPgpMetadata metadata) {
            for (OxMessageListener listener : oxMessageListeners) {
                listener.newIncomingOxMessage(contact, originalMessage, signcryptElement, metadata);
            }
        }
    };
}
