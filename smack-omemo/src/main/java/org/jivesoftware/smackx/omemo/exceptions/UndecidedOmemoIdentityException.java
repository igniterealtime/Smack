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
package org.jivesoftware.smackx.omemo.exceptions;

import java.util.HashSet;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

/**
 * Exception that is thrown when the user tries to encrypt a message for a undecided device.
 *
 * @author Paul Schaub
 */
public class UndecidedOmemoIdentityException extends Exception {
    private static final long serialVersionUID = -6591706422506879747L;
    private final HashSet<OmemoDevice> devices = new HashSet<>();

    public UndecidedOmemoIdentityException(OmemoDevice contact) {
        super();
        this.devices.add(contact);
    }

    /**
     * Return the HashSet of undecided devices.
     *
     * @return undecided devices
     */
    public HashSet<OmemoDevice> getUndecidedDevices() {
        return this.devices;
    }

    /**
     * Add all undecided devices of another Exception to this Exceptions HashSet of undecided devices.
     *
     * @param other other Exception
     */
    public void join(UndecidedOmemoIdentityException other) {
        this.devices.addAll(other.getUndecidedDevices());
    }
}
