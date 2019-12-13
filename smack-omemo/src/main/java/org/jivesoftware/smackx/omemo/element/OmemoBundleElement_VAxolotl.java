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

/**
 * OMEMO device bundle as described by the protocol.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0384.html#usecases-announcing">XEP-0384: OMEMO Encryption (Example 3)</a>.
 *
 * @author Paul Schaub
 */
public class OmemoBundleElement_VAxolotl extends OmemoBundleElement {

    public OmemoBundleElement_VAxolotl(int signedPreKeyId, String signedPreKeyB64, String signedPreKeySigB64, String identityKeyB64, HashMap<Integer, String> preKeysB64) {
        super(signedPreKeyId, signedPreKeyB64, signedPreKeySigB64, identityKeyB64, preKeysB64);
    }

    public OmemoBundleElement_VAxolotl(int signedPreKeyId, byte[] signedPreKey, byte[] signedPreKeySig, byte[] identityKey, HashMap<Integer, byte[]> preKeys) {
        super(signedPreKeyId, signedPreKey, signedPreKeySig, identityKey, preKeys);
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE_V_AXOLOTL;
    }
}
