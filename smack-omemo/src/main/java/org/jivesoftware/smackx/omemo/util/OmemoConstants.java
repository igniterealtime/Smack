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
package org.jivesoftware.smackx.omemo.util;

/**
 * Some constants related to OMEMO.
 * @author Paul Schaub
 */
public final class OmemoConstants {

    //Constants
    /**
     * Omemo related namespace.
     */
    public static final String OMEMO_NAMESPACE_V_AXOLOTL = "eu.siacs.conversations.axolotl";
    public static final String OMEMO = "OMEMO";

    //PubSub Node names
    public static final String PEP_NODE_DEVICE_LIST = OMEMO_NAMESPACE_V_AXOLOTL + ".devicelist";
    public static final String PEP_NODE_DEVICE_LIST_NOTIFY = PEP_NODE_DEVICE_LIST + "+notify";
    public static final String PEP_NODE_BUNDLES = OMEMO_NAMESPACE_V_AXOLOTL + ".bundles";

    /**
     * How many preKeys do we want to publish?
     */
    public static final int TARGET_PRE_KEY_COUNT = 100;

    /**
     * Return the node name of the PEP node containing the device bundle of the device with device id deviceId.
     *
     * @param deviceId id of the device
     * @return node name of the devices bundle node
     */
    public static String PEP_NODE_BUNDLE_FROM_DEVICE_ID(int deviceId) {
        return PEP_NODE_BUNDLES + ":" + deviceId;
    }

    public static final String BODY_OMEMO_HINT = "I sent you an OMEMO encrypted message but your client doesn’t seem to support that. Find more information on https://conversations.im/omemo";

    /**
     * Information about the keys used for message encryption.
     */
    public static final class Crypto {
        public static final String KEYTYPE = "AES";
        public static final int KEYLENGTH = 128;
        public static final String CIPHERMODE = "AES/GCM/NoPadding";
        public static final String PROVIDER = "BC";
    }
}
