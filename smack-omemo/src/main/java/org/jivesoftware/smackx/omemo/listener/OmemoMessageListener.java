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
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.omemo.OmemoMessage;

/**
 * Listener interface that allows implementations to receive decrypted OMEMO messages.
 *
 * @author Paul Schaub
 */
public interface OmemoMessageListener {
    /**
     * Gets called, whenever an OmemoMessage has been received and was successfully decrypted.
     *
     * @param stanza Received (encrypted) stanza.
     * @param decryptedMessage decrypted OmemoMessage.
     */
    void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received decryptedMessage);

    void onOmemoCarbonCopyReceived(CarbonExtension.Direction direction,
                                   Message carbonCopy,
                                   Message wrappingMessage,
                                   OmemoMessage.Received decryptedCarbonCopy);
}
