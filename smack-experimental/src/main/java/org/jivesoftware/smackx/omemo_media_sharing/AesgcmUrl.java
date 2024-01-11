/**
 *
 * Copyright © 2019 Paul Schaub
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
package org.jivesoftware.smackx.omemo_media_sharing;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.httpfileupload.element.Slot;

/**
 * This class represents a aesgcm URL as described in XEP-XXXX: OMEMO Media Sharing.
 * As the builtin {@link URL} class cannot handle the aesgcm protocol identifier, this class
 * is used as a utility class that bundles together a {@link URL}, key and IV.
 *
 * @see <a href="https://xmpp.org/extensions/inbox/omemo-media-sharing.html">XEP-XXXX: OMEMO Media Sharing</a>
 */
public class AesgcmUrl {

    public static final String PROTOCOL = "aesgcm";

    private final URL httpsUrl;
    private final byte[] keyBytes;
    private final byte[] ivBytes;

    /**
     * Private constructor that constructs the {@link AesgcmUrl} from a normal https {@link URL}, a key and iv.
     *
     * @param httpsUrl normal https url as given by the {@link Slot}.
     * @param key byte array of an encoded 256 bit aes key
     * @param iv 16 or 12 byte initialization vector
     */
    public AesgcmUrl(URL httpsUrl, byte[] key, byte[] iv) {
        this.httpsUrl = Objects.requireNonNull(httpsUrl);
        this.keyBytes = Objects.requireNonNull(key);
        this.ivBytes = Objects.requireNonNull(iv);
    }

    /**
     * Parse a {@link AesgcmUrl} from a {@link String}.
     * The parsed object will provide a normal {@link URL} under which the offered file can be downloaded,
     * as well as a {@link Cipher} that can be used to decrypt it.
     *
     * @param aesgcmUrlString aesgcm URL as a {@link String}
     */
    public AesgcmUrl(String aesgcmUrlString) {
        if (!aesgcmUrlString.startsWith(PROTOCOL)) {
            throw new IllegalArgumentException("Provided String does not resemble a aesgcm URL.");
        }

        // Convert aesgcm Url to https URL
        this.httpsUrl = extractHttpsUrl(aesgcmUrlString);

        // Extract IV and Key
        byte[][] ivAndKey = extractIVAndKey(aesgcmUrlString);
        this.ivBytes = ivAndKey[0];
        this.keyBytes = ivAndKey[1];
    }

    /**
     * Return a https {@link URL} under which the file can be downloaded.
     *
     * @return https URL
     */
    public URL getDownloadUrl() {
        return httpsUrl;
    }

    /**
     * Returns the {@link String} representation of this aesgcm URL.
     *
     * @return aesgcm URL with key and IV.
     */
    public String getAesgcmUrl() {
        String aesgcmUrl = httpsUrl.toString().replaceFirst(httpsUrl.getProtocol(), PROTOCOL);
        return aesgcmUrl + "#" + StringUtils.encodeHex(ivBytes) + StringUtils.encodeHex(keyBytes);
    }

    /**
     * Returns a {@link Cipher} in decryption mode, which can be used to decrypt the offered file.
     *
     * @return cipher
     *
     * @throws NoSuchPaddingException if the JVM cannot provide the specified cipher mode
     * @throws NoSuchAlgorithmException if the JVM cannot provide the specified cipher mode
     * @throws InvalidAlgorithmParameterException if the JVM cannot provide the specified cipher
     *                                            (eg. if no BC provider is added)
     * @throws InvalidKeyException if the provided key is invalid
     */
    public Cipher getDecryptionCipher() throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        return OmemoMediaSharingUtils.decryptionCipherFrom(keyBytes, ivBytes);
    }

    private static URL extractHttpsUrl(String aesgcmUrlString) {
        // aesgcm -> https
        String httpsUrlString = aesgcmUrlString.replaceFirst(PROTOCOL, "https");
        // remove #ref
        httpsUrlString = httpsUrlString.substring(0, httpsUrlString.indexOf("#"));

        try {
            return new URL(httpsUrlString);
        } catch (MalformedURLException e) {
            throw new AssertionError("Failed to convert aesgcm URL to https URL: '" + aesgcmUrlString + "'", e);
        }
    }

    private static byte[][] extractIVAndKey(String aesgcmUrlString) {
        int startOfRef = aesgcmUrlString.lastIndexOf("#");
        if (startOfRef == -1) {
            throw new IllegalArgumentException("The provided aesgcm Url does not have a ref part which is " +
                    "supposed to contain the encryption key for file encryption.");
        }

        String ref = aesgcmUrlString.substring(startOfRef + 1);
        byte[] refBytes = hexStringToByteArray(ref);

        byte[] key = new byte[32];
        byte[] iv;
        int ivLen;
        // determine the length of the initialization vector part
        switch (refBytes.length) {
            // 32 bytes key + 16 bytes IV
            case 48:
                ivLen = 16;
                break;

            // 32 bytes key + 12 bytes IV
            case 44:
                ivLen = 12;
                break;
            default:
                throw new IllegalArgumentException("Provided URL has an invalid ref tag (" + ref.length() + "): '" + ref + "'");
        }
        iv = new byte[ivLen];
        System.arraycopy(refBytes, 0, iv, 0, ivLen);
        System.arraycopy(refBytes, ivLen, key, 0, 32);

        return new byte[][] {iv, key};
    }

    /**
     * Convert a hexadecimal String to bytes.
     *
     * Source: https://stackoverflow.com/a/140861/11150851
     *
     * @param s hex string
     * @return byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
