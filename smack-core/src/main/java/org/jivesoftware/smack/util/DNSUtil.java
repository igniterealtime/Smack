/**
 *
 * Copyright 2003-2005 Jive Software, 2016-2020 Florian Schmaus.
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

import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.SmackDaneProvider;

/**
 * Utility class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 * @author Florian Schmaus
 */
public class DNSUtil {

    private static DNSResolver dnsResolver = null;
    private static SmackDaneProvider daneProvider;

    /**
     * Set the DNS resolver that should be used to perform DNS lookups.
     *
     * @param resolver TODO javadoc me please
     */
    public static void setDNSResolver(DNSResolver resolver) {
        dnsResolver = Objects.requireNonNull(resolver);
    }

    /**
     * Returns the current DNS resolved used to perform DNS lookups.
     *
     * @return the active DNSResolver
     */
    public static DNSResolver getDNSResolver() {
        return dnsResolver;
    }

    /**
     * Set the DANE provider that should be used when DANE is enabled.
     *
     * @param daneProvider TODO javadoc me please
     */
    public static void setDaneProvider(SmackDaneProvider daneProvider) {
        DNSUtil.daneProvider = Objects.requireNonNull(daneProvider);
    }

    /**
     * Returns the currently active DANE provider used when DANE is enabled.
     *
     * @return the active DANE provider
     */
    public static SmackDaneProvider getDaneProvider() {
        return daneProvider;
    }

}
