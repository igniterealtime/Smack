/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.omemo.exceptions;

import java.util.Date;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

public class StaleDeviceException extends Exception {
    private static final long serialVersionUID = 1L;

    private final OmemoDevice device;
    private final Date lastMessageDate;
    private final Date lastDeviceIdPublication;

    /**
     * This exception gets thrown if a message cannot be encrypted for a device due to the device being inactive for too long (stale).
     *
     * @param device OmemoDevice.
     * @param lastMessageDate
     * @param lastDeviceIdPublicationDate
     */
    public StaleDeviceException(OmemoDevice device, Date lastMessageDate, Date lastDeviceIdPublicationDate) {
        this.device = device;
        this.lastMessageDate = lastMessageDate;
        this.lastDeviceIdPublication = lastDeviceIdPublicationDate;
    }

    /**
     * Return the date on which the last OMEMO message sent from the device was received.
     * @return last messages date
     */
    public Date getLastMessageDate() {
        return lastMessageDate;
    }

    /**
     * Return the date of the last time the deviceId was republished after being inactive/non-existent before.
     * @return date of last deviceId (re)publication.
     */
    public Date getLastDeviceIdPublicationDate() {
        return lastDeviceIdPublication;
    }

    /**
     * Return the stale OMEMO device.
     * @return stale device
     */
    public OmemoDevice getDevice() {
        return device;
    }
}
