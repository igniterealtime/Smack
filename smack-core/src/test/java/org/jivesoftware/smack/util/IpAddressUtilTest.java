/**
 *
 * Copyright 2017 Khaliulin Roman
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.junit.Assert;
import org.junit.Test;

public class IpAddressUtilTest {
    @Test
    public void isIPv4LiteralAddressTest() {
        Assert.assertTrue(IpAddressUtil.isIPv4LiteralAddress("122.20.221.11")); // valid IPv4
        Assert.assertFalse(IpAddressUtil.isIPv4LiteralAddress("127.0.0")); // too short
        Assert.assertFalse(IpAddressUtil.isIPv4LiteralAddress("127.0.0.0.0")); // too long
        Assert.assertFalse(IpAddressUtil.isIPv4LiteralAddress("127.0.0.d")); // illegal character
        Assert.assertFalse(IpAddressUtil.isIPv4LiteralAddress("127.0.0.256")); // segment value greater than 255
    }
}