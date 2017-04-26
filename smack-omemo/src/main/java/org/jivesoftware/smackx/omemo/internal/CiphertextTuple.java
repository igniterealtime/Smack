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
package org.jivesoftware.smackx.omemo.internal;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.TYPE_OMEMO_PREKEY_MESSAGE;

/**
 * Bundles a decrypted ciphertext together with information about the message type.
 *
 * @author Paul Schaub
 */
public class CiphertextTuple {
    private final byte[] ciphertext;
    private final int messageType;

    public CiphertextTuple(byte[] ciphertext, int type) {
        this.ciphertext = ciphertext;
        this.messageType = type;
    }

    /**
     * Return the ciphertext
     *
     * @return ciphertext
     */
    public byte[] getCiphertext() {
        return ciphertext;
    }

    /**
     * Return the messageType
     *
     * @return messageType
     */
    public int getMessageType() {
        return this.messageType;
    }

    /**
     * Returns true if this is a preKeyMessage
     *
     * @return preKeyMessage?
     */
    public boolean isPreKeyMessage() {
        return getMessageType() == TYPE_OMEMO_PREKEY_MESSAGE;
    }
}
