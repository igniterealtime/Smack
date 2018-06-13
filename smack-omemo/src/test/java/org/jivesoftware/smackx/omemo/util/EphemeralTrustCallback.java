/**
 *
 * Copyright the original author or authors
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

import java.util.HashMap;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.OmemoTrustCallback;
import org.jivesoftware.smackx.omemo.trust.TrustState;

/**
 * Ephemera Trust Callback used to make trust decisions in tests.
 */
public class EphemeralTrustCallback implements OmemoTrustCallback {

    private final HashMap<OmemoDevice, HashMap<OmemoFingerprint, TrustState>> trustStates = new HashMap<>();

    @Override
    public TrustState getTrust(OmemoDevice device, OmemoFingerprint fingerprint) {
        HashMap<OmemoFingerprint, TrustState> states = trustStates.get(device);

        if (states != null) {
            TrustState state = states.get(fingerprint);

            if (state != null) {
                return state;
            }
        }

        return TrustState.undecided;
    }

    @Override
    public void setTrust(OmemoDevice device, OmemoFingerprint fingerprint, TrustState state) {
        HashMap<OmemoFingerprint, TrustState> states = trustStates.get(device);

        if (states == null) {
            states = new HashMap<>();
            trustStates.put(device, states);
        }

        states.put(fingerprint, state);
    }
}
