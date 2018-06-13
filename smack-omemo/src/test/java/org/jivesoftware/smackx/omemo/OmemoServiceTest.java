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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.util.Date;
import java.util.HashSet;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class OmemoServiceTest extends SmackTestSuite {

    private static final long ONE_HOUR = 1000L * 60 * 60;
    private static final int IGNORE_STALE = OmemoConfiguration.getIgnoreStaleDevicesAfterHours();
    private static final int DELETE_STALE = OmemoConfiguration.getDeleteStaleDevicesAfterHours();

    @Test(expected = IllegalStateException.class)
    public void getInstanceFailsWhenNullTest() {
        OmemoService.getInstance();
    }

    @Test
    public void isServiceRegisteredTest() {
        assertFalse(OmemoService.isServiceRegistered());
    }

    /**
     * Test correct functionality of isStale method.
     * @throws XmppStringprepException
     */
    @Test
    public void isStaleDeviceTest() throws XmppStringprepException {
        OmemoDevice user = new OmemoDevice(JidCreate.bareFrom("alice@wonderland.lit"), 123);
        OmemoDevice other = new OmemoDevice(JidCreate.bareFrom("bob@builder.tv"), 444);

        Date now = new Date();
        Date ignoreMe = new Date(now.getTime() - ((IGNORE_STALE + 1) * ONE_HOUR));
        Date deleteMe = new Date(now.getTime() - ((DELETE_STALE + 1) * ONE_HOUR));
        Date imFine = new Date(now.getTime() - ONE_HOUR);

        // One hour "old" devices are (probably) not not stale
        assertFalse(OmemoService.isStale(user, other, imFine, IGNORE_STALE));

        // Devices one hour "older" than max ages are stale
        assertTrue(OmemoService.isStale(user, other, ignoreMe, IGNORE_STALE));
        assertTrue(OmemoService.isStale(user, other, deleteMe, DELETE_STALE));

        // Own device is never stale, no matter how old
        assertFalse(OmemoService.isStale(user, user, deleteMe, DELETE_STALE));

        // Always return false if date is null.
        assertFalse(OmemoService.isStale(user, other, null, DELETE_STALE));
    }

    @Test
    public void removeOurDeviceTest() throws XmppStringprepException {
        OmemoDevice a = new OmemoDevice(JidCreate.bareFrom("a@b.c"), 123);
        OmemoDevice b = new OmemoDevice(JidCreate.bareFrom("a@b.c"), 124);

        HashSet<OmemoDevice> devices = new HashSet<>();
        devices.add(a); devices.add(b);

        assertTrue(devices.contains(a));
        assertTrue(devices.contains(b));
        OmemoService.removeOurDevice(a, devices);

        assertFalse(devices.contains(a));
        assertTrue(devices.contains(b));
    }
}
