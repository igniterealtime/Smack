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
package org.jivesoftware.smackx.omemo.listener;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jxmpp.jid.BareJid;

/**
 * Listener interface that allows implementations to receive decrypted OMEMO MUC messages
 * @author Paul Schaub
 */
public interface OmemoMucMessageListener<T_IdKey> {

    /**
     * Gets called whenever an OMEMO message has been received in a MultiUserChat and successfully decrypted.
     * @param muc MultiUserChat the message was sent in
     * @param from the bareJid of the sender
     * @param decryptedBody the decrypted Body of the message
     * @param message the original message with encrypted elements
     * @param wrappingMessage in case of a carbon copy, this is the wrapping message
     * @param omemoInformation information about the encryption of the message
     */
    void onOmemoMucMessageReceived(MultiUserChat muc, BareJid from, String decryptedBody, Message message,
                                   Message wrappingMessage, OmemoMessageInformation<T_IdKey> omemoInformation);
}
