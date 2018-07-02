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

    /**
     * Delete stale devices from the device list after a period of time.
     */
    private static boolean DELETE_STALE_DEVICES = true;
    private static int DELETE_STALE_DEVICE_AFTER_HOURS = 24 * 7 * 4;     //4 weeks

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

    /**
     * Upload a new signed prekey in intervals. This improves forward secrecy. Old keys are kept for some more time and
     * then deleted.
     */
    private static boolean RENEW_OLD_SIGNED_PREKEYS = false;
    private static int RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS = 24 * 7;    //One week
    private static int MAX_NUMBER_OF_STORED_SIGNED_PREKEYS = 4;

    /**
     * Decide, whether signed preKeys are automatically rotated or not.
     * It is highly recommended to rotate signed preKeys to preserve forward secrecy.
     *
     * @param renew automatically rotate signed preKeys?
     */
    public static void setRenewOldSignedPreKeys(boolean renew) {
        RENEW_OLD_SIGNED_PREKEYS = renew;
    }

    /**
     * Determine, whether signed preKeys are automatically rotated or not.
     *
     * @return auto-rotate signed preKeys?
     */
    public static boolean getRenewOldSignedPreKeys() {
        return RENEW_OLD_SIGNED_PREKEYS;
    }

    /**
     * Set the interval in hours, after which the published signed preKey should be renewed.
     * This value should be between one or two weeks.
     *
     * @param hours hours after which signed preKeys should be rotated.
     */
    public static void setRenewOldSignedPreKeysAfterHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than 0.");
        }
        RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS = hours;
    }

    /**
     * Get the interval in hours, after which the published signed preKey should be renewed.
     * This value should be between one or two weeks.
     *
     * @return hours after which signed preKeys should be rotated.
     */
    public static int getRenewOldSignedPreKeysAfterHours() {
        return RENEW_OLD_SIGNED_PREKEYS_AFTER_HOURS;
    }

    /**
     * Set the maximum number of signed preKeys that are cached until the oldest one gets deleted.
     * This number should not be too small in order to prevent message loss, but also not too big
     * to preserve forward secrecy.
     *
     * @param number number of cached signed preKeys.
     */
    public static void setMaxNumberOfStoredSignedPreKeys(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be greater than 0.");
        }
        MAX_NUMBER_OF_STORED_SIGNED_PREKEYS = number;
    }

    /**
     * Return the maximum number of signed preKeys that are cached until the oldest one gets deleted.
     * @return max number of cached signed preKeys.
     */
    public static int getMaxNumberOfStoredSignedPreKeys() {
        return MAX_NUMBER_OF_STORED_SIGNED_PREKEYS;
    }

    /**
     * Add a plaintext body hint about omemo encryption to the message.
     */
    private static boolean ADD_OMEMO_HINT_BODY = true;

    /**
     * Decide, whether an OMEMO message should carry a plaintext hint about OMEMO encryption.
     * Eg. "I sent you an OMEMO encrypted message..."
     *
     * @param addHint shall we add a hint?
     */
    public static void setAddOmemoHintBody(boolean addHint) {
        ADD_OMEMO_HINT_BODY = addHint;
    }

    /**
     * Determine, whether an OMEMO message should carry a plaintext hint about OMEMO encryption.
     *
     * @return true, if a hint is added to the message.
     */
    public static boolean getAddOmemoHintBody() {
        return ADD_OMEMO_HINT_BODY;
    }

    private static boolean REPAIR_BROKEN_SESSIONS_WITH_PREKEY_MESSAGES = true;

    /**
     * Determine, whether incoming messages, which have broken sessions should automatically be answered by an empty
     * preKeyMessage in order to establish a new session.
     *
     * @return true if session should be repaired automatically.
     */
    public static boolean getRepairBrokenSessionsWithPreKeyMessages() {
        return REPAIR_BROKEN_SESSIONS_WITH_PREKEY_MESSAGES;
    }

    /**
     * Decide, whether incoming messages, which have broken sessions should automatically be answered by an empty
     * preKeyMessage in order to establish a new session.
     *
     * @param repair repair sessions?
     */
    public static void setRepairBrokenSessionsWithPrekeyMessages(boolean repair) {
        REPAIR_BROKEN_SESSIONS_WITH_PREKEY_MESSAGES = repair;
    }

    private static boolean COMPLETE_SESSION_WITH_EMPTY_MESSAGE = true;

    /**
     * Determine, whether incoming preKeyMessages should automatically be answered by an empty message in order to
     * complete the session.
     *
     * @return true if sessions should be completed.
     */
    public static boolean getCompleteSessionWithEmptyMessage() {
        return COMPLETE_SESSION_WITH_EMPTY_MESSAGE;
    }

    /**
     * Decide, whether incoming preKeyMessages should automatically be answered by an empty message in order to
     * complete the session.
     *
     * @param complete complete the session or not
     */
    public static void setCompleteSessionWithEmptyMessage(boolean complete) {
        COMPLETE_SESSION_WITH_EMPTY_MESSAGE = complete;
    }
}
