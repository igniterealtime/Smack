/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.jivesoftware.smackx.ox.util.SecretKeyBackupHelper;

import org.junit.jupiter.api.Test;

public class OpenPgpSecretKeyBackupPassphraseTest {

    @Test
    public void secretKeyPassphraseConstructorTest() {
        OpenPgpSecretKeyBackupPassphrase valid =
                new OpenPgpSecretKeyBackupPassphrase("TWNK-KD5Y-MT3T-E1GS-DRDB-KVTW");

        assertNotNull(valid);
        for (int i = 0; i < 50; i++) {
            assertNotNull(SecretKeyBackupHelper.generateBackupPassword());
        }

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("TWNKKD5YMT3TE1GSDRDBKVTW"));

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("0123-4567-89AB-CDEF-GHIJ-KLMN"));

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("CONT-AINS-ILLE-GALL-ETTE-RSO0"));

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("TWNK-KD5Y-MT3T-E1GS-DRDB-"));

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("TWNK-KD5Y-MT3T-E1GS-DRDB-KVTW-ADDD"));

        assertThrows(IllegalArgumentException.class,
                () -> new OpenPgpSecretKeyBackupPassphrase("TWNK KD5Y MT3T E1GS DRDB KVTW"));

    }
}
