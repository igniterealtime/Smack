/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smack.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IpAddressUtilTest {

    @Test
    public void isIpV4AddressTest() {
        String ipv4 = "122.20.221.11";
        boolean isIpV4 = IpAddressUtil.isIPv4LiteralAddress(ipv4);
        assertTrue(isIpV4);
    }

    @Test
    public void isInvalidIpV4AddressTest() {
        String ipv4 = "122.20.221.11.1";
        boolean isIpV4 = IpAddressUtil.isIPv4LiteralAddress(ipv4);
        assertFalse(isIpV4);
    }

    @Test
    public void isInvalidIpV4AddressTest2() {
        String ipv4 = "122.20.256.11";
        boolean isIpV4 = IpAddressUtil.isIPv4LiteralAddress(ipv4);
        assertFalse(isIpV4);
    }
}
