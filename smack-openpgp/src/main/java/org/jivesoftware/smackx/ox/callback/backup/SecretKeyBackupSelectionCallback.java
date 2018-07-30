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
 * Callback to allow the user to decide, which locally available secret keys they want to include in a backup.
 */
public interface SecretKeyBackupSelectionCallback {

    /**
     * Let the user decide, which secret keys they want to backup.
     *
     * @param availableSecretKeys {@link Set} of {@link OpenPgpV4Fingerprint}s of locally available
     *                                       OpenPGP secret keys.
     * @return {@link Set} which contains the {@link OpenPgpV4Fingerprint}s the user decided to include
     *                                       in the backup.
     */
    Set<OpenPgpV4Fingerprint> selectKeysToBackup(Set<OpenPgpV4Fingerprint> availableSecretKeys);
}
