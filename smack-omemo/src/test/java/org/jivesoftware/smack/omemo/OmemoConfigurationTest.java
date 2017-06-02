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
package org.jivesoftware.smack.omemo;

import junit.framework.TestCase;
import org.jivesoftware.smackx.omemo.OmemoConfiguration;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the OmemoConfiguration class.
 */
public class OmemoConfigurationTest {

    @Test
    public void omemoConfigurationTest() {
        @SuppressWarnings("unused") OmemoConfiguration configuration = new OmemoConfiguration();
        // Default Store Path
        File storePath = new File("test");
        assertNull("getFileBasedOmemoStoreDefaultPath MUST return null at this point.",
                OmemoConfiguration.getFileBasedOmemoStoreDefaultPath());
        OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(storePath);
        assertEquals("FileBasedOmemoStoreDefaultPath must equal the one we set.", storePath.getAbsolutePath(),
                OmemoConfiguration.getFileBasedOmemoStoreDefaultPath().getAbsolutePath());

        // EME
        OmemoConfiguration.setAddEmeEncryptionHint(false);
        assertEquals(false, OmemoConfiguration.getAddEmeEncryptionHint());
        OmemoConfiguration.setAddEmeEncryptionHint(true);
        assertEquals(true, OmemoConfiguration.getAddEmeEncryptionHint());

        // MAM
        OmemoConfiguration.setAddMAMStorageProcessingHint(false);
        assertEquals(false, OmemoConfiguration.getAddMAMStorageProcessingHint());
        OmemoConfiguration.setAddMAMStorageProcessingHint(true);
        assertEquals(true, OmemoConfiguration.getAddMAMStorageProcessingHint());

        // Body hint
        OmemoConfiguration.setAddOmemoHintBody(false);
        assertEquals(false, OmemoConfiguration.getAddOmemoHintBody());
        OmemoConfiguration.setAddOmemoHintBody(true);
        assertEquals(true, OmemoConfiguration.getAddOmemoHintBody());

        // Delete stale devices
        OmemoConfiguration.setDeleteStaleDevices(false);
        assertEquals(false, OmemoConfiguration.getDeleteStaleDevices());
        OmemoConfiguration.setDeleteStaleDevices(true);
        assertEquals(true, OmemoConfiguration.getDeleteStaleDevices());
        OmemoConfiguration.setDeleteStaleDevicesAfterHours(25);
        assertEquals(25, OmemoConfiguration.getDeleteStaleDevicesAfterHours());
        try {
            OmemoConfiguration.setDeleteStaleDevicesAfterHours(-3);
            TestCase.fail("OmemoConfiguration.setDeleteStaleDevicesAfterHours should not accept values <= 0.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        // Ignore stale device
        OmemoConfiguration.setIgnoreStaleDevices(false);
        assertEquals(false, OmemoConfiguration.getIgnoreStaleDevices());
        OmemoConfiguration.setIgnoreStaleDevices(true);
        assertEquals(true, OmemoConfiguration.getIgnoreStaleDevices());
        OmemoConfiguration.setIgnoreStaleDevicesAfterHours(44);
        assertEquals(44, OmemoConfiguration.getIgnoreStaleDevicesAfterHours());
        try {
            OmemoConfiguration.setIgnoreStaleDevicesAfterHours(-5);
            TestCase.fail("OmemoConfiguration.setIgnoreStaleDevicesAfterHours should not accept values <= 0.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Renew signedPreKeys
        OmemoConfiguration.setRenewOldSignedPreKeys(false);
        assertEquals(false, OmemoConfiguration.getRenewOldSignedPreKeys());
        OmemoConfiguration.setRenewOldSignedPreKeys(true);
        assertEquals(true, OmemoConfiguration.getRenewOldSignedPreKeys());
        OmemoConfiguration.setRenewOldSignedPreKeysAfterHours(77);
        assertEquals(77, OmemoConfiguration.getRenewOldSignedPreKeysAfterHours());
        try {
            OmemoConfiguration.setRenewOldSignedPreKeysAfterHours(0);
            TestCase.fail("OmemoConfiguration.setRenewOldSignedPreKeysAfterHours should not accept values <= 0");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        OmemoConfiguration.setMaxNumberOfStoredSignedPreKeys(6);
        assertEquals(6, OmemoConfiguration.getMaxNumberOfStoredSignedPreKeys());
        try {
            OmemoConfiguration.setMaxNumberOfStoredSignedPreKeys(0);
            TestCase.fail("OmemoConfiguration.setMaxNumberOfStoredSignedPreKeys should not accept values <= 0");
        } catch (IllegalArgumentException e) {
            //Expected
        }
    }
}
