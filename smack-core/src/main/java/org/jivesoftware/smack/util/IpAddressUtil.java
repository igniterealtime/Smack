/**
 *
 * Copyright 2015 Florian Schmaus
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpAddressUtil {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public static boolean isIPv4LiteralAddress(String string) {
        Matcher matcher = IPV4_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return false;
        }

        for (int i = 0; i < 3; i++) {
            String ipSegment = matcher.group(i);
            int ipSegmentInt;
            try {
                ipSegmentInt = Integer.valueOf(ipSegment);
            } catch (NumberFormatException e) {
                throw new AssertionError(e);
            }
            if (ipSegmentInt > 255) {
                return false;
            }
        }
        return true;
    }

    public static boolean isIPv6LiteralAddress(final String string) {
        final String[] octets = string.split(":");
        if (octets.length != 8) {
            return false;
        }
        // TODO handle compressed zeros and validate octets
        return true;
    }

    public static boolean isIpAddress(String string) {
        return isIPv4LiteralAddress(string) || isIPv6LiteralAddress(string);
    }
}
