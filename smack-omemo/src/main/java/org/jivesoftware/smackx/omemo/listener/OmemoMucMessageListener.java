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

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.omemo.OmemoMessage;

/**
 * Listener interface that allows implementations to receive decrypted OMEMO MUC messages.
 * @author Paul Schaub
 */
public interface OmemoMucMessageListener {

    /**
     * Gets called whenever an OMEMO message has been received in a MultiUserChat and successfully decrypted.
     * @param muc MultiUserChat the message was sent in
     * @param stanza Original Stanza
     * @param decryptedOmemoMessage decrypted Omemo message
     */
    void onOmemoMucMessageReceived(MultiUserChat muc, Stanza stanza, OmemoMessage.Received decryptedOmemoMessage);
}
