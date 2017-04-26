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
package org.jivesoftware.smackx.omemo.util;

/**
 * @author Paul Schaub
 */
public class OmemoConstants {
    /**
     * Omemo related namespace. TODO: Change to "urn:xmpp:omemo:0" once OMEMO implementations changes
     * TODO: Also think about the resources/omemo.providers file
     */

    public static final String OMEMO = "OMEMO";
    public static final String OMEMO_NAMESPACE = "eu.siacs.conversations.axolotl";

    public static final String PEP_NODE_DEVICE_LIST = OMEMO_NAMESPACE + ".devicelist";
    public static final String PEP_NODE_DEVICE_LIST_NOTIFY = PEP_NODE_DEVICE_LIST + "+notify";
    public static final String PEP_NODE_BUNDLES = OMEMO_NAMESPACE + ".bundles";

    public static final String ID = "id";

    public static final boolean APPEND_AUTH_TAG_TO_MESSAGE_KEY = false;

    public static final int TYPE_OMEMO_PREKEY_MESSAGE = 1;
    public static final int TYPE_OMEMO_MESSAGE = 0;

    /**
     * Return the node name of the PEP node containing the device bundle of the device with device id deviceId
     *
     * @param deviceId id of the device
     * @return node name of the devices bundle node
     */
    public static String PEP_NODE_BUNDLE_FROM_DEVICE_ID(int deviceId) {
        return PEP_NODE_BUNDLES + ":" + deviceId;
    }

    public static final String BODY_OMEMO_HINT = "I sent you an OMEMO encrypted message but your client doesn’t seem to support that. Find more information on https://conversations.im/omemo";

    /**
     * Element names of a OMEMO deviceList update
     */
    public static class List {
        public static final String DEVICE = "device";
        public static final String LIST = "list";
    }

    /**
     * Element names of a OMEMO bundle
     */
    public static class Bundle {
        public static final String BUNDLE = "bundle";
        public static final String SIGNED_PRE_KEY_PUB = "signedPreKeyPublic";
        public static final String SIGNED_PRE_KEY_ID = "signedPreKeyId";
        public static final String SIGNED_PRE_KEY_SIG = "signedPreKeySignature";
        public static final String IDENTITY_KEY = "identityKey";
        public static final String PRE_KEYS = "prekeys";
        public static final String PRE_KEY_PUB = "preKeyPublic";
        public static final String PRE_KEY_ID = "preKeyId";
    }

    /**
     * Element names of a encrypted OMEMO message
     */
    public static class Encrypted {
        public static final String ENCRYPTED = "encrypted";
        public static final String HEADER = "header";
        public static final String SID = "sid";
        public static final String KEY = "key";
        public static final String RID = "rid";
        public static final String PREKEY = "prekey";
        public static final String IV = "iv";
        public static final String PAYLOAD = "payload";
    }

    /**
     * Information about the keys used for message encryption
     */
    public static class Crypto {
        public static final String KEYTYPE = "AES";
        public static final String CIPHERMODE = "AES/GCM/NoPadding";
        public static final String PROVIDER = "BC";
    }
}
