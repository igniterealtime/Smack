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
package org.jivesoftware.smackx.omemo.listener;

import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;

import org.jxmpp.jid.BareJid;

/**
 * Listener interface that allows implementations to receive decrypted OMEMO MUC messages.
 * @author Paul Schaub
 */
public interface OmemoMucMessageListener {

    /**
     * Gets called whenever an OMEMO message has been received in a MultiUserChat and successfully decrypted.
     * @param muc MultiUserChat the message was sent in
     * @param from the bareJid of the sender
     * @param decryptedBody the decrypted Body of the message
     * @param message the original message with encrypted element
     * @param wrappingMessage in case of a carbon copy, this is the wrapping message
     * @param omemoInformation information about the encryption of the message
     */
    void onOmemoMucMessageReceived(MultiUserChat muc, BareJid from, String decryptedBody, Message message,
                                   Message wrappingMessage, OmemoMessageInformation omemoInformation);

    /**
     * Gets called, whenever an OmemoElement without a body (an OmemoKeyTransportElement) is received.
     *
     * @param muc               MultiUserChat the message was sent in
     * @param from              bareJid of the sender
     * @param cipherAndAuthTag  transportedKey along with an optional authTag
     * @param message           Message that contained the KeyTransport
     * @param wrappingMessage   Wrapping message (eg. carbon), or null
     * @param omemoInformation  Information about the messages encryption etc.
     */
    void onOmemoKeyTransportReceived(MultiUserChat muc, BareJid from, CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation);
}
