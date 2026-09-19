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
package org.jivesoftware.smackx.omemo.util;

/**
 * Some constants related to OMEMO for both Axolotl and omemo:2 namespaces.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public final class OmemoConstants {
    /**
     * Omemo related namespace.
     */
    public static final String OMEMO_NAMESPACE_V_AXOLOTL = "eu.siacs.conversations.axolotl";
    public static final String OMEMO_NAMESPACE_V_OMEMO = "urn:xmpp:omemo:2";
    public static final String OMEMO = "OMEMO";

    // PubSub Node names
    public static final String PEP_NODE_DEVICES_V_AXOLOTL = OMEMO_NAMESPACE_V_AXOLOTL + ".devicelist";
    public static final String PEP_NODE_BUNDLES_V_AXOLOTL = OMEMO_NAMESPACE_V_AXOLOTL + ".bundles";

    public static final String PEP_NODE_DEVICES_V_OMEMO = OMEMO_NAMESPACE_V_OMEMO + ":devices";
    public static final String PEP_NODE_BUNDLES_V_OMEMO = OMEMO_NAMESPACE_V_OMEMO + ":bundles";

    /**
     * How many preKeys do we want to publish?
     */
    public static final int PRE_KEY_COUNT_PER_BUNDLE = 100;

    /**
     * Return the node name of the PEP node containing the device bundle of the device with device id deviceId.
     *
     * @param deviceId id of the device
     * @param vOmemo2 omemo:2 option state.
     *
     * @return node name of the devices bundle node
     */
    public static String PEP_NODE_BUNDLE_FROM_DEVICE_ID(int deviceId, boolean vOmemo2) {
        return vOmemo2 ? PEP_NODE_BUNDLES_V_OMEMO : PEP_NODE_BUNDLES_V_AXOLOTL + ":" + deviceId;
    }

    public static String getOmemoNS(boolean vOmemo2) {
        return vOmemo2 ? OmemoConstants.PEP_NODE_DEVICES_V_OMEMO : OmemoConstants.PEP_NODE_DEVICES_V_AXOLOTL;
    }

    public static final String BODY_OMEMO_HINT = "I sent you an OMEMO '%s' encrypted message but your client doesn't seem to support that. Find more information on https://conversations.im/omemo";

    public static String getOmemoHint(boolean vOmemo2) {
        return String.format(BODY_OMEMO_HINT, vOmemo2 ? OmemoConstants.OMEMO_NAMESPACE_V_OMEMO : OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL);
    }

    /**
     * Information about the keys used for message encryption.
     */
    public static final class Crypto {
        public static final String KEYTYPE = "AES";
        public static final int KEYLENGTH = 128;
        public static final String CIPHERMODE = "AES/GCM/NoPadding";
    }
}
