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
package org.jivesoftware.smackx.ox.callback.backup;

import java.util.Set;

import org.pgpainless.key.OpenPgpV4Fingerprint;


/**
 * Callback to let the user decide which key from a backup they want to restore.
 */
public interface SecretKeyRestoreSelectionCallback {

    /**
     * Let the user choose, which SecretKey they want to restore as the new primary OpenPGP signing key.
     * @param availableSecretKeys {@link Set} of {@link OpenPgpV4Fingerprint}s of the keys which are contained
     *                                       in the backup.
     * @return {@link OpenPgpV4Fingerprint} of the key the user wants to restore as the new primary
     *                                     signing key.
     */
    OpenPgpV4Fingerprint selectSecretKeyToRestore(Set<OpenPgpV4Fingerprint> availableSecretKeys);
}
