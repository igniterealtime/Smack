/*
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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceElement;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;

import org.junit.Test;

/**
 * Test behavior of device lists.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class DeviceListTest {
    /**
     * Test, whether deviceList updates are correctly merged into the cached device list.
     * IDs in the update become active devices, active IDs that were not in the update become inactive.
     * Inactive IDs that were not in the update stay inactive.
     */
    @Test
    public void mergeDeviceListsTest() {
        OmemoCachedDeviceList cached = new OmemoCachedDeviceList();
        assertNotNull(cached.getActiveDevices());
        assertNotNull(cached.getInactiveDevices());

        cached.getInactiveDevices().add(new OmemoDeviceElement(1));
        cached.getInactiveDevices().add(new OmemoDeviceElement(2));
        cached.getActiveDevices().add(new OmemoDeviceElement(3));

        Set<OmemoDeviceElement> update = new HashSet<>();
        update.add(new OmemoDeviceElement(1));
        update.add(new OmemoDeviceElement(4));

        cached.merge(update);

        assertTrue(cached.getActiveDevices().contains(new OmemoDeviceElement(1)) &&
                !cached.getActiveDevices().contains(new OmemoDeviceElement(2)) &&
                !cached.getActiveDevices().contains(new OmemoDeviceElement(3)) &&
                cached.getActiveDevices().contains(new OmemoDeviceElement(4)));

        assertTrue(!cached.getInactiveDevices().contains(new OmemoDeviceElement(1)) &&
                cached.getInactiveDevices().contains(new OmemoDeviceElement(2)) &&
                cached.getInactiveDevices().contains(new OmemoDeviceElement(3)) &&
                !cached.getInactiveDevices().contains(new OmemoDeviceElement(4)));

        assertTrue(cached.getAllDevices().size() == 4);

        assertFalse(cached.contains(new OmemoDeviceElement(17)));
        cached.addDevice(new OmemoDeviceElement(17));
        assertTrue(cached.getActiveDevices().contains(new OmemoDeviceElement(17)));

        assertNotNull(cached.toString());
    }
}
