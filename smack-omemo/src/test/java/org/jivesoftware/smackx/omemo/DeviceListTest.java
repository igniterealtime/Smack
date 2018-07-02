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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;

import org.junit.Test;


/**
 * Test behavior of device lists.
 *
 * @author Paul Schaub
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

        cached.getInactiveDevices().add(1);
        cached.getInactiveDevices().add(2);
        cached.getActiveDevices().add(3);

        Set<Integer> update = new HashSet<>();
        update.add(4);
        update.add(1);

        cached.merge(update);

        assertTrue(cached.getActiveDevices().contains(1) &&
                !cached.getActiveDevices().contains(2) &&
                !cached.getActiveDevices().contains(3) &&
                cached.getActiveDevices().contains(4));

        assertTrue(!cached.getInactiveDevices().contains(1) &&
                cached.getInactiveDevices().contains(2) &&
                cached.getInactiveDevices().contains(3) &&
                !cached.getInactiveDevices().contains(4));

        assertTrue(cached.getAllDevices().size() == 4);

        assertFalse(cached.contains(17));
        cached.addDevice(17);
        assertTrue(cached.getActiveDevices().contains(17));

        assertNotNull(cached.toString());
    }
}
