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
package org.jivesoftware.smackx.omemo;

import java.io.File;

/**
 * Contains OMEMO related configuration options.
 *
 * @author Paul Schaub
 */
public final class OmemoConfiguration {

    /**
     * Ignore own other stale devices that we did not receive a message from for a period of time.
     * Ignoring means do not encrypt messages for them. This helps to mitigate stale devices that threaten
     * forward secrecy by never advancing ratchets.
     */
    private static boolean IGNORE_STALE_DEVICES = true;
    private static int IGNORE_STALE_DEVICE_AFTER_HOURS = 24 * 7;         //One week

    /**
     * Delete stale devices from the device list after a period of time.
     */
    private static boolean DELETE_STALE_DEVICES = true;
    private static int DELETE_STALE_DEVICE_AFTER_HOURS = 24 * 7 * 4;     //4 weeks

    /**
     * Upload a new signed prekey in intervals. This improves forward secrecy. Old keys are kept for some more time and
     * then deleted.
     */
    private static boolean RENEW_OLD_SIGNED_PREKEYS = false;
    private static int RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS = 24 * 7;    //One week
    private static int MAX_NUMBER_OF_STORED_SIGNED_PREKEYS = 4;

    /**
     * Add a plaintext body hint about omemo encryption to the message.
     */
    private static boolean ADD_OMEMO_HINT_BODY = true;

    /**
     * Add Explicit Message Encryption hint (XEP-0380) to the message.
     */
    private static boolean ADD_EME_ENCRYPTION_HINT = true;

    /**
     * Add MAM storage hint to allow the server to store messages that do not contain a body.
     */
    private static boolean ADD_MAM_STORAGE_HINT = true;

    private static File FILE_BASED_OMEMO_STORE_DEFAULT_PATH = null;

    public static void setIgnoreStaleDevices(boolean ignore) {
        IGNORE_STALE_DEVICES = ignore;
    }

    public static boolean getIgnoreStaleDevices() {
        return IGNORE_STALE_DEVICES;
    }

    public static void setIgnoreStaleDevicesAfterHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than 0.");
        }
        IGNORE_STALE_DEVICE_AFTER_HOURS = hours;
    }

    public static int getIgnoreStaleDevicesAfterHours() {
        return IGNORE_STALE_DEVICE_AFTER_HOURS;
    }

    public static void setDeleteStaleDevices(boolean delete) {
        DELETE_STALE_DEVICES = delete;
    }

    public static boolean getDeleteStaleDevices() {
        return DELETE_STALE_DEVICES;
    }

    public static void setDeleteStaleDevicesAfterHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than 0.");
        }
        DELETE_STALE_DEVICE_AFTER_HOURS = hours;
    }

    public static int getDeleteStaleDevicesAfterHours() {
        return DELETE_STALE_DEVICE_AFTER_HOURS;
    }

    public static void setRenewOldSignedPreKeys(boolean renew) {
        RENEW_OLD_SIGNED_PREKEYS = renew;
    }

    public static boolean getRenewOldSignedPreKeys() {
        return RENEW_OLD_SIGNED_PREKEYS;
    }

    public static void setRenewOldSignedPreKeysAfterHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than 0.");
        }
        RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS = hours;
    }

    public static int getRenewOldSignedPreKeysAfterHours() {
        return RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS;
    }

    public static void setMaxNumberOfStoredSignedPreKeys(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be greater than 0.");
        }
        MAX_NUMBER_OF_STORED_SIGNED_PREKEYS = number;
    }

    public static int getMaxNumberOfStoredSignedPreKeys() {
        return MAX_NUMBER_OF_STORED_SIGNED_PREKEYS;
    }

    public static void setAddOmemoHintBody(boolean addHint) {
        ADD_OMEMO_HINT_BODY = addHint;
    }

    public static boolean getAddOmemoHintBody() {
        return ADD_OMEMO_HINT_BODY;
    }

    public static void setAddEmeEncryptionHint(boolean addHint) {
        ADD_EME_ENCRYPTION_HINT = addHint;
    }

    public static boolean getAddEmeEncryptionHint() {
        return ADD_EME_ENCRYPTION_HINT;
    }

    public static void setAddMAMStorageProcessingHint(boolean addStorageHint) {
        ADD_MAM_STORAGE_HINT = addStorageHint;
    }

    public static boolean getAddMAMStorageProcessingHint() {
        return ADD_MAM_STORAGE_HINT;
    }

    public static void setFileBasedOmemoStoreDefaultPath(File path) {
        FILE_BASED_OMEMO_STORE_DEFAULT_PATH = path;
    }

    public static File getFileBasedOmemoStoreDefaultPath() {
        return FILE_BASED_OMEMO_STORE_DEFAULT_PATH;
    }
}
