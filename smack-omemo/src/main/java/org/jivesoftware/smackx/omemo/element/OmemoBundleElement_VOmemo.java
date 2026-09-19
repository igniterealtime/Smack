/*
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

import java.util.Map;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.omemo.util.OmemoConstants;

/**
 * OMEMO device bundle for omemo:2 as described by the protocol.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0384.html#usecases-announcing">XEP-0384: OMEMO Encryption (Example 3)</a>
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoBundleElement_VOmemo extends OmemoBundleElement {

    public static final String SIGNED_PRE_KEY = "spk";
    public static final String SIGNED_PRE_KEY_SIG = "spks";
    public static final String IDENTITY_KEY = "ik";
    public static final String KEY_ID = "id";
    public static final String PRE_KEYS = "prekeys";
    public static final String PRE_KEY = "pk";

    /**
     * Constructor to create a Bundle Element from base64 Strings.
     *
     * @param signedPreKeyId id
     * @param signedPreKeyB64 base64 encoded signedPreKey
     * @param signedPreKeySigB64 base64 encoded signedPreKeySignature
     * @param identityKeyB64 base64 encoded identityKey
     * @param preKeysB64 Map of base64 encoded preKeys
     */
    public OmemoBundleElement_VOmemo(int signedPreKeyId, String signedPreKeyB64, String signedPreKeySigB64, String identityKeyB64, Map<Integer, String> preKeysB64) {
        super(signedPreKeyId, signedPreKeyB64, signedPreKeySigB64, identityKeyB64, preKeysB64);
    }

    public OmemoBundleElement_VOmemo(int signedPreKeyId, byte[] signedPreKey, byte[] signedPreKeySig, byte[] identityKey, Map<Integer, byte[]> preKeys) {
        super(signedPreKeyId, signedPreKey, signedPreKeySig, identityKey, preKeys);
    }

    @Override
    public String getNamespace() {
        return OmemoConstants.OMEMO_NAMESPACE_V_OMEMO;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder sb = new XmlStringBuilder(this, enclosingNamespace).rightAngleBracket();

        sb.halfOpenElement(SIGNED_PRE_KEY).attribute(KEY_ID, signedPreKeyId).rightAngleBracket()
                .append(signedPreKeyB64).closeElement(SIGNED_PRE_KEY);

        sb.openElement(SIGNED_PRE_KEY_SIG).append(signedPreKeySignatureB64).closeElement(SIGNED_PRE_KEY_SIG);

        sb.openElement(IDENTITY_KEY).append(identityKeyB64).closeElement(IDENTITY_KEY);

        sb.openElement(PRE_KEYS);
        for (Map.Entry<Integer, String> p : this.preKeysB64.entrySet()) {
            sb.halfOpenElement(PRE_KEY).attribute(KEY_ID, p.getKey()).rightAngleBracket()
                    .append(p.getValue()).closeElement(PRE_KEY);
        }
        sb.closeElement(PRE_KEYS);

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OmemoBundleElement[\n");
        sb.append(SIGNED_PRE_KEY).append(' ').append(KEY_ID).append('=').append(signedPreKeyId)
                .append(':').append(signedPreKeyB64).append('\n')
                .append(SIGNED_PRE_KEY_SIG).append(": ").append(signedPreKeySignatureB64).append('\n')
                .append(IDENTITY_KEY).append(": ").append(identityKeyB64).append('\n')
                .append(PRE_KEYS).append(" (").append(preKeysB64.size()).append(")\n");
        for (Map.Entry<Integer, String> e : preKeysB64.entrySet()) {
            sb.append(PRE_KEY).append(' ').append(KEY_ID).append('=').append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        sb.append(']');
        return sb.toString();
    }
}
