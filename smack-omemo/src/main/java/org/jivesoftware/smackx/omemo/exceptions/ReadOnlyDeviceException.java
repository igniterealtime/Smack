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

import org.jivesoftware.smackx.omemo.OmemoConfiguration;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

/**
 * Exception that signals, that a device is considered read-only.
 * Read-only devices are devices that receive OMEMO messages, but do not send any.
 * Those devices are weakening forward secrecy. For that reason, read-only devices are ignored after n messages have
 * been sent without getting a reply back.
 */
public class ReadOnlyDeviceException extends Exception {
    private static final long serialVersionUID = 1L;

    private final OmemoDevice device;

    /**
     * Constructor.
     * We do not need to hand over the current value of the message counter, as that value will always be equal to
     * {@link OmemoConfiguration#getMaxReadOnlyMessageCount()}. Therefore providing the {@link OmemoDevice} should be
     * enough.
     *
     * @param device device which is considered read-only.
     */
    public ReadOnlyDeviceException(OmemoDevice device) {
        this.device = device;
    }

    /**
     * Return the device in question.
     * @return device
     */
    public OmemoDevice getDevice() {
        return device;
    }
}
