/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.selection_strategy;

import java.util.Date;
import java.util.Map;

import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.selection.keyring.PublicKeyRingSelectionStrategy;
import org.pgpainless.key.selection.keyring.SecretKeyRingSelectionStrategy;

public class AnnouncedKeys {

    public static class PubKeyRingSelectionStrategy extends PublicKeyRingSelectionStrategy<Map<OpenPgpV4Fingerprint, Date>> {

        @Override
        public boolean accept(Map<OpenPgpV4Fingerprint, Date> announcedKeys, PGPPublicKeyRing publicKeys) {
            return announcedKeys.keySet().contains(new OpenPgpV4Fingerprint(publicKeys));
        }
    }

    public static class SecKeyRingSelectionStrategy extends SecretKeyRingSelectionStrategy<Map<OpenPgpV4Fingerprint, Date>> {

        @Override
        public boolean accept(Map<OpenPgpV4Fingerprint, Date> announcedKeys, PGPSecretKeyRing secretKeys) {
            return announcedKeys.keySet().contains(new OpenPgpV4Fingerprint(secretKeys));
        }
    }
}
