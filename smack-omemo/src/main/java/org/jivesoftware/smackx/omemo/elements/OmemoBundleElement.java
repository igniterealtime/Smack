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
package org.jivesoftware.smackx.omemo.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

import java.util.HashMap;
import java.util.Map;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Bundle.*;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE;

/**
 * OMEMO device bundle as described here:
 * https://xmpp.org/extensions/xep-0384.html#usecases-announcing (Example 3)
 *
 * @author Paul Schaub
 */
public class OmemoBundleElement implements ExtensionElement {

    private final int signedPreKeyId;
    private final byte[] signedPreKey;
    private final byte[] signedPreKeySignature;
    private final byte[] identityKey;
    private final HashMap<Integer, byte[]> preKeys;

    public OmemoBundleElement(int signedPreKeyId, byte[] signedPreKey, byte[] signedPreKeySig, byte[] identityKey, HashMap<Integer, byte[]> preKeys) {
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKey = signedPreKey;
        this.signedPreKeySignature = signedPreKeySig;
        this.identityKey = identityKey;
        this.preKeys = preKeys;
    }

    /**
     * Return the signedPreKey of the OmemoBundleElement
     *
     * @return signedPreKey as byte array
     */
    public byte[] getSignedPreKey() {
        return this.signedPreKey;
    }

    /**
     * Return the id of the signedPreKey in the bundle
     *
     * @return id of signedPreKey
     */
    public int getSignedPreKeyId() {
        return this.signedPreKeyId;
    }

    /**
     * Get the signature of the signedPreKey
     *
     * @return signature as byte array
     */
    public byte[] getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    /**
     * Return the public identityKey of the bundles owner.
     * This can be used to check the signedPreKeys signature.
     * The fingerprint of this key is, what the user has to verify.
     *
     * @return public identityKey as byte array
     */
    public byte[] getIdentityKey() {
        return this.identityKey;
    }

    /**
     * Return the HashMap of preKeys in the bundle.
     * The map uses the preKeys ids as key and the preKeys as value.
     *
     * @return preKeys
     */
    public HashMap<Integer, byte[]> getPreKeys() {
        return this.preKeys;
    }

    /**
     * Return a single preKey from the map.
     *
     * @param id id of the preKey
     * @return the preKey
     */
    public byte[] getPreKey(int id) {
        return preKeys.get(id);
    }

    @Override
    public String getElementName() {
        return BUNDLE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        sb.halfOpenElement(SIGNED_PRE_KEY_PUB).attribute(SIGNED_PRE_KEY_ID, signedPreKeyId).rightAngleBracket()
                .append(Base64.encodeToString(signedPreKey)).closeElement(SIGNED_PRE_KEY_PUB);

        sb.openElement(SIGNED_PRE_KEY_SIG).append(Base64.encodeToString(signedPreKeySignature)).closeElement(SIGNED_PRE_KEY_SIG);

        sb.openElement(IDENTITY_KEY).append(Base64.encodeToString(identityKey)).closeElement(IDENTITY_KEY);

        sb.openElement(PRE_KEYS);
        for (Map.Entry<Integer, byte[]> p : this.preKeys.entrySet()) {
            sb.halfOpenElement(PRE_KEY_PUB).attribute(PRE_KEY_ID, p.getKey()).rightAngleBracket()
                    .append(Base64.encodeToString(p.getValue())).closeElement(PRE_KEY_PUB);
        }
        sb.closeElement(PRE_KEYS);

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE;
    }

    @Override
    public String toString() {
        String out = "OmemoBundleElement[\n";
        out += SIGNED_PRE_KEY_PUB + " " + SIGNED_PRE_KEY_ID + "=" + signedPreKeyId + ": " + Base64.encodeToString(signedPreKey) + "\n";
        out += SIGNED_PRE_KEY_SIG + ": " + Base64.encodeToString(signedPreKeySignature) + "\n";
        out += IDENTITY_KEY + ": " + Base64.encodeToString(identityKey) + "\n";
        out += PRE_KEYS + " (" + preKeys.size() + ")\n";
        for (Map.Entry<Integer, byte[]> e : preKeys.entrySet()) {
            out += PRE_KEY_PUB + " " + PRE_KEY_ID + "=" + e.getKey() + ": " + Base64.encodeToString(e.getValue()) + "\n";
        }
        return out;
    }
}
