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
package org.jivesoftware.smackx.omemo.element;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * OMEMO device bundle as described here:
 * https://xmpp.org/extensions/xep-0384.html#usecases-announcing (Example 3).
 *
 * @author Paul Schaub
 */
public class OmemoBundleVAxolotlElement extends OmemoBundleElement {

    private final int signedPreKeyId;
    private final String signedPreKeyB64;
    private byte[] signedPreKey;
    private final String signedPreKeySignatureB64;
    private byte[] signedPreKeySignature;
    private final String identityKeyB64;
    private byte[] identityKey;
    private final HashMap<Integer, String> preKeysB64;
    private HashMap<Integer, byte[]> preKeys;

    /**
     * Constructor to create a Bundle Element from base64 Strings.
     *
     * @param signedPreKeyId id
     * @param signedPreKeyB64 base64 encoded signedPreKey
     * @param signedPreKeySigB64 base64 encoded signedPreKeySignature
     * @param identityKeyB64 base64 encoded identityKey
     * @param preKeysB64 HashMap of base64 encoded preKeys
     */
    public OmemoBundleVAxolotlElement(int signedPreKeyId, String signedPreKeyB64, String signedPreKeySigB64, String identityKeyB64, HashMap<Integer, String> preKeysB64) {
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyB64 = signedPreKeyB64;
        this.signedPreKeySignatureB64 = signedPreKeySigB64;
        this.identityKeyB64 = identityKeyB64;
        this.preKeysB64 = preKeysB64;
    }

    /**
     * Constructor to create a Bundle Element from decoded byte arrays.
     *
     * @param signedPreKeyId id
     * @param signedPreKey signedPreKey
     * @param signedPreKeySig signedPreKeySignature
     * @param identityKey identityKey
     * @param preKeys HashMap of preKeys
     */
    public OmemoBundleVAxolotlElement(int signedPreKeyId, byte[] signedPreKey, byte[] signedPreKeySig, byte[] identityKey, HashMap<Integer, byte[]> preKeys) {
        this.signedPreKeyId = signedPreKeyId;

        this.signedPreKey = signedPreKey;
        this.signedPreKeyB64 = Base64.encodeToString(signedPreKey);

        this.signedPreKeySignature = signedPreKeySig;
        this.signedPreKeySignatureB64 = Base64.encodeToString(signedPreKeySignature);

        this.identityKey = identityKey;
        this.identityKeyB64 = Base64.encodeToString(identityKey);

        this.preKeys = preKeys;
        this.preKeysB64 = new HashMap<>();
        for (int id : preKeys.keySet()) {
            preKeysB64.put(id, Base64.encodeToString(preKeys.get(id)));
        }
    }

    /**
     * Return the signedPreKey of the OmemoBundleElement.
     *
     * @return signedPreKey as byte array
     */
    public byte[] getSignedPreKey() {
        if (signedPreKey == null) {
            signedPreKey = Base64.decode(signedPreKeyB64);
        }
        return this.signedPreKey.clone();
    }

    /**
     * Return the id of the signedPreKey in the bundle.
     *
     * @return id of signedPreKey
     */
    public int getSignedPreKeyId() {
        return this.signedPreKeyId;
    }

    /**
     * Get the signature of the signedPreKey.
     *
     * @return signature as byte array
     */
    public byte[] getSignedPreKeySignature() {
        if (signedPreKeySignature == null) {
            signedPreKeySignature = Base64.decode(signedPreKeySignatureB64);
        }
        return signedPreKeySignature.clone();
    }

    /**
     * Return the public identityKey of the bundles owner.
     * This can be used to check the signedPreKeys signature.
     * The fingerprint of this key is, what the user has to verify.
     *
     * @return public identityKey as byte array
     */
    public byte[] getIdentityKey() {
        if (identityKey == null) {
            identityKey = Base64.decode(identityKeyB64);
        }
        return this.identityKey.clone();
    }

    /**
     * Return the HashMap of preKeys in the bundle.
     * The map uses the preKeys ids as key and the preKeys as value.
     *
     * @return preKeys
     */
    public HashMap<Integer, byte[]> getPreKeys() {
        if (preKeys == null) {
            preKeys = new HashMap<>();
            for (int id : preKeysB64.keySet()) {
                preKeys.put(id, Base64.decode(preKeysB64.get(id)));
            }
        }
        return this.preKeys;
    }

    /**
     * Return a single preKey from the map.
     *
     * @param id id of the preKey
     * @return the preKey
     */
    public byte[] getPreKey(int id) {
        return getPreKeys().get(id);
    }

    @Override
    public String getElementName() {
        return BUNDLE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        sb.halfOpenElement(SIGNED_PRE_KEY_PUB).attribute(SIGNED_PRE_KEY_ID, signedPreKeyId).rightAngleBracket()
                .append(signedPreKeyB64).closeElement(SIGNED_PRE_KEY_PUB);

        sb.openElement(SIGNED_PRE_KEY_SIG).append(signedPreKeySignatureB64).closeElement(SIGNED_PRE_KEY_SIG);

        sb.openElement(IDENTITY_KEY).append(identityKeyB64).closeElement(IDENTITY_KEY);

        sb.openElement(PRE_KEYS);
        for (Map.Entry<Integer, String> p : this.preKeysB64.entrySet()) {
            sb.halfOpenElement(PRE_KEY_PUB).attribute(PRE_KEY_ID, p.getKey()).rightAngleBracket()
                    .append(p.getValue()).closeElement(PRE_KEY_PUB);
        }
        sb.closeElement(PRE_KEYS);

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE_V_AXOLOTL;
    }

    @Override
    public String toString() {
        String out = "OmemoBundleElement[\n";
        out += SIGNED_PRE_KEY_PUB + " " + SIGNED_PRE_KEY_ID + "=" + signedPreKeyId + ": " + signedPreKeyB64 + "\n";
        out += SIGNED_PRE_KEY_SIG + ": " + signedPreKeySignatureB64 + "\n";
        out += IDENTITY_KEY + ": " + identityKeyB64 + "\n";
        out += PRE_KEYS + " (" + preKeysB64.size() + ")\n";
        for (Map.Entry<Integer, String> e : preKeysB64.entrySet()) {
            out += PRE_KEY_PUB + " " + PRE_KEY_ID + "=" + e.getKey() + ": " + e.getValue() + "\n";
        }
        return out;
    }
}
