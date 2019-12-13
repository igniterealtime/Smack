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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;

import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

import org.jxmpp.jid.Jid;

public class OmemoMessage {

    private final OmemoElement element;
    private final byte[] messageKey, iv;

    OmemoMessage(OmemoElement element, byte[] key, byte[] iv) {
        this.element = element;
        this.messageKey = key;
        this.iv = iv;
    }

    /**
     * Return the original OmemoElement (&lt;encrypted/&gt;).
     *
     * @return omemoElement of the message
     */
    public OmemoElement getElement() {
        return element;
    }

    /**
     * Return the messageKey (or transported key in case of a KeyTransportMessage).
     *
     * @return encryption key that protects the message payload
     */
    public byte[] getKey() {
        return messageKey.clone();
    }

    /**
     * Return the initialization vector belonging to the key.
     *
     * @return initialization vector
     */
    public byte[] getIv() {
        return iv.clone();
    }

    /**
     * Outgoing OMEMO message.
     */
    public static class Sent extends OmemoMessage {
        private final Set<OmemoDevice> intendedDevices = new HashSet<>();
        private final HashMap<OmemoDevice, Throwable> skippedDevices = new HashMap<>();

        /**
         * Create a new outgoing OMEMO message.
         * @param element OmemoElement
         * @param key messageKey (or transported key)
         * @param iv initialization vector belonging to key
         * @param intendedDevices devices the client intended to encrypt the message for
         * @param skippedDevices devices which were skipped during encryption process because encryption
         *                       failed for some reason
         */
        Sent(OmemoElement element, byte[] key, byte[] iv, Set<OmemoDevice> intendedDevices, HashMap<OmemoDevice, Throwable> skippedDevices) {
            super(element, key, iv);
            this.intendedDevices.addAll(intendedDevices);
            this.skippedDevices.putAll(skippedDevices);
        }

        /**
         * Return a list of all devices the sender originally intended to encrypt the message for.
         * @return list of intended recipients.
         */
        public Set<OmemoDevice> getIntendedDevices() {
            return intendedDevices;
        }

        /**
         * Return a map of all skipped recipients and the reasons for skipping.
         * @return map of skipped recipients and reasons for that.
         */
        public HashMap<OmemoDevice, Throwable> getSkippedDevices() {
            return skippedDevices;
        }

        /**
         * Determine, if some recipients were skipped during encryption.
         * @return true if recipients were skipped.
         */
        public boolean isMissingRecipients() {
            return !getSkippedDevices().isEmpty();
        }

        /**
         * Return the OmemoElement wrapped in a Message ready to be sent.
         * The message is addressed to recipient, contains the OmemoElement
         * as well as an optional clear text hint as body, a MAM storage hint
         * and an EME hint about OMEMO encryption.
         *
         * @param messageBuilder a message builder which will be used to build the message.
         * @param recipient recipient for the to-field of the message.
         * @return the build message.
         */
        public Message buildMessage(MessageBuilder messageBuilder, Jid recipient) {
            messageBuilder.ofType(Message.Type.chat).to(recipient);

            messageBuilder.addExtension(getElement());

            if (OmemoConfiguration.getAddOmemoHintBody()) {
                messageBuilder.setBody(BODY_OMEMO_HINT);
            }

            StoreHint.set(messageBuilder);
            messageBuilder.addExtension(new ExplicitMessageEncryptionElement(OMEMO_NAMESPACE_V_AXOLOTL, OMEMO));

            return messageBuilder.build();
        }
    }

    /**
     * Incoming OMEMO message.
     */
    public static class Received extends OmemoMessage {
        private final String message;
        private final OmemoFingerprint sendersFingerprint;
        private final OmemoDevice senderDevice;
        private final boolean preKeyMessage;

        /**
         * Create a new incoming OMEMO message.
         * @param element original OmemoElement
         * @param key message key (or transported key)
         * @param iv respective initialization vector
         * @param body decrypted body
         * @param sendersFingerprint OmemoFingerprint of the senders identityKey
         * @param senderDevice OmemoDevice of the sender
         * @param preKeyMessage if this was a preKeyMessage or not
         */
        Received(OmemoElement element, byte[] key, byte[] iv, String body, OmemoFingerprint sendersFingerprint, OmemoDevice senderDevice, boolean preKeyMessage) {
            super(element, key, iv);
            this.message = body;
            this.sendersFingerprint = sendersFingerprint;
            this.senderDevice = senderDevice;
            this.preKeyMessage = preKeyMessage;
        }

        /**
         * Return the decrypted body of the message.
         * @return decrypted body
         */
        public String getBody() {
            return message;
        }

        /**
         * Return the fingerprint of the messages sender device.
         * @return fingerprint of sender
         */
        public OmemoFingerprint getSendersFingerprint() {
            return sendersFingerprint;
        }

        /**
         * Return the OmemoDevice which sent the message.
         *
         * @return OMEMO device that sent the message.
         */
        public OmemoDevice getSenderDevice() {
            return senderDevice;
        }

        /**
         * Return true, if this message was sent as a preKeyMessage.
         * @return preKeyMessage or not
         */
        boolean isPreKeyMessage() {
            return preKeyMessage;
        }

        /**
         * Return true, if the message was a KeyTransportMessage.
         * A KeyTransportMessage is a OmemoMessage without a payload.
         * @return keyTransportMessage?
         */
        public boolean isKeyTransportMessage() {
            return message == null;
        }
    }
}
