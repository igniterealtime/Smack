/**
 * $Revision: 1456 $
 * $Date: 2005-06-01 22:04:54 -0700 (Wed, 01 Jun 2005) $
 *
 * Copyright 2003-2005 Jive Software.
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

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Map;

/**
 * Utilty class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 */
public class DNSUtil {

    /**
     * Create a cache to hold the 100 most recently accessed DNS lookups for a period of
     * 10 minutes.
     */
    private static Map cache = new Cache(100, 1000*60*10);

    private static DirContext context;

    static {
        try {
            Hashtable env = new Hashtable();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            context = new InitialDirContext(env);
        }
        catch (Exception e) {
            // Ignore.
        }
    }

    /**
     * Returns the host name and port that the specified XMPP server can be
     * reached at for client-to-server communication. A DNS lookup for a SRV
     * record in the form "_xmpp-client._tcp.example.com" is attempted, according
     * to section 14.4 of RFC 3920. If that lookup fails, a lookup in the older form
     * of "_jabber._tcp.example.com" is attempted since servers that implement an
     * older version of the protocol may be listed using that notation. If that
     * lookup fails as well, it's assumed that the XMPP server lives at the
     * host resolved by a DNS lookup at the specified domain on the default port
     * of 5222.<p>
     *
     * As an example, a lookup for "example.com" may return "im.example.com:5269".
     *
     * @param domain the domain.
     * @return a HostAddress, which encompasses the hostname and port that the XMPP
     *      server can be reached at for the specified domain.
     */
    public static HostAddress resolveXMPPDomain(String domain) {
        if (context == null) {
            return new HostAddress(domain, 5222);
        }
        String key = "c" + domain;
        // Return item from cache if it exists.
        if (cache.containsKey(key)) {
            HostAddress address = (HostAddress)cache.get(key);
            if (address != null) {
                return address;
            }
        }
        String host = domain;
        int port = 5222;
        try {
            Attributes dnsLookup =
                    context.getAttributes("_xmpp-client._tcp." + domain, new String[]{"SRV"});
            String srvRecord = (String)dnsLookup.get("SRV").get();
            String [] srvRecordEntries = srvRecord.split(" ");
            port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length-2]);
            host = srvRecordEntries[srvRecordEntries.length-1];
        }
        catch (Exception e) {
            // Ignore.
        }
        // Host entries in DNS should end with a ".".
        if (host.endsWith(".")) {
            host = host.substring(0, host.length()-1);
        }
        HostAddress address = new HostAddress(host, port);
        // Add item to cache.
        cache.put(key, address);
        return address;
    }

    /**
     * Returns the host name and port that the specified XMPP server can be
     * reached at for server-to-server communication. A DNS lookup for a SRV
     * record in the form "_xmpp-server._tcp.example.com" is attempted, according
     * to section 14.4 of RFC 3920. If that lookup fails, a lookup in the older form
     * of "_jabber._tcp.example.com" is attempted since servers that implement an
     * older version of the protocol may be listed using that notation. If that
     * lookup fails as well, it's assumed that the XMPP server lives at the
     * host resolved by a DNS lookup at the specified domain on the default port
     * of 5269.<p>
     *
     * As an example, a lookup for "example.com" may return "im.example.com:5269".
     *
     * @param domain the domain.
     * @return a HostAddress, which encompasses the hostname and port that the XMPP
     *      server can be reached at for the specified domain.
     */
    public static HostAddress resolveXMPPServerDomain(String domain) {
        if (context == null) {
            return new HostAddress(domain, 5269);
        }
        String key = "s" + domain;
        // Return item from cache if it exists.
        if (cache.containsKey(key)) {
            HostAddress address = (HostAddress)cache.get(key);
            if (address != null) {
                return address;
            }
        }
        String host = domain;
        int port = 5269;
        try {
            Attributes dnsLookup =
                    context.getAttributes("_xmpp-server._tcp." + domain, new String[]{"SRV"});
            String srvRecord = (String)dnsLookup.get("SRV").get();
            String [] srvRecordEntries = srvRecord.split(" ");
            port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length-2]);
            host = srvRecordEntries[srvRecordEntries.length-1];
        }
        catch (Exception e) {
            // Attempt lookup with older "jabber" name.
            try {
                Attributes dnsLookup =
                        context.getAttributes("_jabber._tcp." + domain, new String[]{"SRV"});
                String srvRecord = (String)dnsLookup.get("SRV").get();
                String [] srvRecordEntries = srvRecord.split(" ");
                port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length-2]);
                host = srvRecordEntries[srvRecordEntries.length-1];
            }
            catch (Exception e2) {
                // Ignore.
            }
        }
        // Host entries in DNS should end with a ".".
        if (host.endsWith(".")) {
            host = host.substring(0, host.length()-1);
        }
        HostAddress address = new HostAddress(host, port);
        // Add item to cache.
        cache.put(key, address);
        return address;
    }

    /**
     * Encapsulates a hostname and port.
     */
    public static class HostAddress {

        private String host;
        private int port;

        private HostAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * Returns the hostname.
         *
         * @return the hostname.
         */
        public String getHost() {
            return host;
        }

        /**
         * Returns the port.
         *
         * @return the port.
         */
        public int getPort() {
            return port;
        }

        public String toString() {
            return host + ":" + port;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HostAddress)) {
                return false;
            }

            final HostAddress address = (HostAddress) o;

            if (!host.equals(address.host)) {
                return false;
            }
            return port == address.port;
        }
    }
}