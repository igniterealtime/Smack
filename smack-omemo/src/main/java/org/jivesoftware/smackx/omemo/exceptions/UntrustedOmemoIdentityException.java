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
package org.jivesoftware.smackx.omemo.exceptions;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

/**
 * Exception that gets thrown when we try to en-/decrypt a message for an untrusted contact.
 * This might either be because the user actively untrusted a device, or we receive a message from a contact
 * which contains an identityKey that differs from the one the user trusted.
 */
public class UntrustedOmemoIdentityException extends Exception {

    private static final long serialVersionUID = 1L;
    private final OmemoDevice device;
    private final OmemoFingerprint trustedKey, untrustedKey;

    /**
     * Constructor for when we receive a message with an identityKey different from the one we trusted.
     *
     * @param device device which sent the message.
     * @param fpTrusted fingerprint of the identityKey we previously had and trusted.
     * @param fpUntrusted fingerprint of the new key which is untrusted.
     */
    public UntrustedOmemoIdentityException(OmemoDevice device, OmemoFingerprint fpTrusted, OmemoFingerprint fpUntrusted) {
        super();
        this.device = device;
        this.trustedKey = fpTrusted;
        this.untrustedKey = fpUntrusted;
    }

    /**
     * Constructor for when encryption fails because the user untrusted a recipients device.
     *
     * @param device device the user wants to encrypt for, but which has been marked as untrusted.
     * @param untrustedKey fingerprint of that device.
     */
    public UntrustedOmemoIdentityException(OmemoDevice device, OmemoFingerprint untrustedKey) {
        this(device, null, untrustedKey);
    }

    /**
     * Return the device which sent the message.
     *
     * @return omemoDevice.
     */
    public OmemoDevice getDevice() {
        return device;
    }

    /**
     * Return the fingerprint of the key we expected.
     * This might return null in case this exception got thrown during encryption process.
     *
     * @return the trusted fingerprint.
     */
    public OmemoFingerprint getTrustedFingerprint() {
        return trustedKey;
    }

    /**
     * Return the fingerprint of the unexpected untrusted key.
     *
     * @return the OMEMO fingerprint.
     */
    public OmemoFingerprint getUntrustedFingerprint() {
        return untrustedKey;
    }

    @Override
    public String getMessage() {
        if (trustedKey != null) {
            return "Untrusted OMEMO Identity encountered:\n" +
                    "Fingerprint of trusted key:\n" + trustedKey.blocksOf8Chars() + "\n" +
                    "Fingerprint of untrusted key:\n" + untrustedKey.blocksOf8Chars();
        } else {
            return "Untrusted OMEMO Identity encountered:\n" +
                    "Fingerprint of untrusted key:\n" + untrustedKey.blocksOf8Chars();
        }
    }
}
