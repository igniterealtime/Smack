/*
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
package org.jivesoftware.smackx.omemo.element;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.Objects;

import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;

/**
 * A OMEMO devices containing the OmemoDevice elements of all active devices of a contact.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public abstract class OmemoDeviceListElement implements ExtensionElement {
    /**
     * Unmodifiable set of device IDs.
     */
    protected final Set<OmemoDeviceElement> deviceElements;

    public OmemoDeviceListElement(Set<OmemoDeviceElement> deviceIds) {
        deviceIds = Objects.requireNonNull(deviceIds);
        deviceElements = Collections.unmodifiableSet(deviceIds);
    }

    public OmemoDeviceListElement(OmemoCachedDeviceList cachedList) {
        deviceElements = new HashSet<>();
        deviceElements.addAll(cachedList.getActiveDevices());
    }

    public Set<OmemoDeviceElement> getDevices() {
        return deviceElements;
    }

    public Set<OmemoDeviceElement> copyDevices() {
        return new HashSet<>(deviceElements);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder("OmemoDeviceListElement[");
        Iterator<OmemoDeviceElement> iterator = deviceElements.iterator();
        for (OmemoDeviceElement i : deviceElements) {
            sb.append(i);
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.append(']').toString();
    }
}
