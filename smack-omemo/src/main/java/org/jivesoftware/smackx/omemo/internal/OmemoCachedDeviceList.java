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
package org.jivesoftware.smackx.omemo.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceElement;

/**
 * This class is used to represent devices of contacts.
 * There are active devices (a set of OmemoDevice elements, which was published with the last device list update)
 * and inactive devices (set of devices that once were active, but are not included in recent list updates).
 * Both kinds are cached by the client. When a device that was active in the last update is not included in
 * a new update, it becomes an inactive device. Vice versa, inactive devices can also become active again, by
 * being included in the latest device list update.
 * <p>
 * The client ensures, that his own OmemoDevice elements is on the list of active devices, as soon as he gets online.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoCachedDeviceList implements Serializable {
    private static final long serialVersionUID = 3153579238321261203L;

    private final Set<OmemoDeviceElement> activeDevices;
    private final Set<OmemoDeviceElement> inactiveDevices;

    public OmemoCachedDeviceList() {
        this.activeDevices = new HashSet<>();
        this.inactiveDevices = new HashSet<>();
    }

    public OmemoCachedDeviceList(Set<OmemoDeviceElement> activeDevices, Set<OmemoDeviceElement> inactiveDevices) {
        this();
        this.activeDevices.addAll(activeDevices);
        this.inactiveDevices.addAll(inactiveDevices);
    }

    public OmemoCachedDeviceList(OmemoCachedDeviceList original) {
        this(original.getActiveDevices(), original.getInactiveDevices());
    }

    /**
     * Returns all active devices.
     * Active devices are all devices that were in the latest DeviceList update.
     *
     * @return active devices
     */
    public Set<OmemoDeviceElement> getActiveDevices() {
        return activeDevices;
    }

    /**
     * Return all inactive devices.
     * Inactive devices are devices which were in a past DeviceList update once, but were not included in
     * the latest update.
     *
     * @return inactive devices
     */
    public Set<OmemoDeviceElement> getInactiveDevices() {
        return inactiveDevices;
    }

    /**
     * Returns an OmemoDeviceListElement containing all devices (active and inactive).
     *
     * @return all devices
     */
    public Set<OmemoDeviceElement> getAllDevices() {
        Set<OmemoDeviceElement> all = new HashSet<>();
        all.addAll(activeDevices);
        all.addAll(inactiveDevices);
        return all;
    }

    /**
     * Merge a device list update into the CachedDeviceList.
     * The source code should be self explanatory.
     *
     * @param deviceListUpdate received device list update
     */
    public void merge(Set<OmemoDeviceElement> deviceListUpdate) {
        inactiveDevices.addAll(activeDevices);
        activeDevices.clear();
        activeDevices.addAll(deviceListUpdate);
        inactiveDevices.removeAll(activeDevices);
    }

    /**
     * Add a device to the list of active devices and remove it from inactive.
     *
     * @param device deviceId that will be added
     */
    public void addDevice(OmemoDeviceElement device) {
        activeDevices.add(device);
        inactiveDevices.remove(device);
    }

    public void addInactiveDevice(OmemoDeviceElement device) {
        activeDevices.remove(device);
        inactiveDevices.add(device);
    }

    /**
     * Returns true if deviceId is either in the list of active or inactive devices.
     *
     * @param deviceId id
     * @return true or false
     */
    public boolean contains(OmemoDeviceElement deviceId) {
        return activeDevices.contains(deviceId) || inactiveDevices.contains(deviceId);
    }

    public boolean isActive(OmemoDeviceElement deviceId) {
        return getActiveDevices().contains(deviceId);
    }

    @Override
    public String toString() {
        String out = "active: [";
        for (OmemoDeviceElement id : activeDevices) {
            out += id + " ";
        }
        out += "] inacitve: [";
        for (OmemoDeviceElement id : inactiveDevices) {
            out += id + " ";
        }
        out += "]";
        return out;
    }
}
