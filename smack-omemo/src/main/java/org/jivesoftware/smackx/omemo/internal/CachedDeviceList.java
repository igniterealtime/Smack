/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.internal;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;

import java.io.Serializable;

/**
 * This class is used to represent device lists of contacts.
 * There are active devices (a set of device ids, which was published with the last device list update)
 * and inactive devices (set of devices that once were active, but are not included in recent list updates).
 * Both kinds are cached by the client. When a device that was active in the last update is not included in
 * a new update, it becomes an inactive device. Vice versa, inactive devices can also become active again, by
 * being included in the latest device list update.
 * <p>
 * The client ensures, that his own device id is on the list of active devices, as soon as he gets online.
 *
 * @author Paul Schaub
 */
public class CachedDeviceList implements ExtensionElement, Serializable {
    private static final long serialVersionUID = 3153579238321261203L;

    private final OmemoDeviceListElement activeDevices;
    private final OmemoDeviceListElement inactiveDevices;

    public CachedDeviceList() {
        this.activeDevices = new OmemoDeviceListElement();
        this.inactiveDevices = new OmemoDeviceListElement();
    }

    /**
     * Returns all active devices
     * Active devices are all devices that were in the latest DeviceList update.
     *
     * @return active devices
     */
    public OmemoDeviceListElement getActiveDevices() {
        return activeDevices;
    }

    /**
     * Return all inactive devices.
     * Inactive devices are devices which were in a past DeviceList update once, but were not included in
     * the latest update.
     *
     * @return inactive devices
     */
    public OmemoDeviceListElement getInactiveDevices() {
        return inactiveDevices;
    }

    /**
     * Returns an OmemoDeviceListElement containing all devices (active and inactive)
     *
     * @return all devices
     */
    public OmemoDeviceListElement getAllDevices() {
        OmemoDeviceListElement all = new OmemoDeviceListElement();
        all.addAll(activeDevices);
        all.addAll(inactiveDevices);
        return all;
    }

    /**
     * Merge a device list update into the CachedDeviceList.
     * The source code should be self explanatory
     *
     * @param deviceListUpdate received device list update
     */
    public void merge(OmemoDeviceListElement deviceListUpdate) {
        inactiveDevices.addAll(activeDevices);
        activeDevices.clear();
        activeDevices.addAll(deviceListUpdate);
        inactiveDevices.removeAll(activeDevices);
    }

    /**
     * Add a device to the list of active devices.
     *
     * @param deviceId deviceId that will be added
     */
    public void addDevice(int deviceId) {
        activeDevices.add(deviceId);
    }

    @Override
    public String getElementName() {
        return activeDevices.getElementName();
    }

    @Override
    public CharSequence toXML() {
        return activeDevices.toXML();
    }

    @Override
    public String getNamespace() {
        return activeDevices.getNamespace();
    }

    /**
     * Returns true if deviceId is either in the list of active or inactive devices.
     *
     * @param deviceId id
     * @return true or false
     */
    public boolean contains(int deviceId) {
        return activeDevices.contains(deviceId) || inactiveDevices.contains(deviceId);
    }
}
