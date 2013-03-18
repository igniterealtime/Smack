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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

/**
 * Utility class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 */
public class DNSUtil {

    /**
     * Create a cache to hold the 100 most recently accessed DNS lookups for a period of
     * 10 minutes.
     */
    private static Map<String, List<HostAddress>> cache = new Cache<String, List<HostAddress>>(100, 1000*60*10);

    private static DNSResolver dnsResolver = null;

    /**
     * Set the DNS resolver that should be used to perform DNS lookups.
     *
     * @param resolver
     */
    public static void setDNSResolver(DNSResolver resolver) {
        dnsResolver = resolver;
    }

    /**
     * Returns the current DNS resolved used to perform DNS lookups.
     *
     * @return
     */
    public static DNSResolver getDNSResolver() {
        return dnsResolver;
    }

    /**
     * Returns a list of HostAddresses under which the specified XMPP server can be
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
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<HostAddress> resolveXMPPDomain(String domain) {
        return resolveDomain(domain, 'c');
    }

    /**
     * Returns a list of HostAddresses under which the specified XMPP server can be
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
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<HostAddress> resolveXMPPServerDomain(String domain) {
        return resolveDomain(domain, 's');
    }

    private static List<HostAddress> resolveDomain(String domain, char keyPrefix) {
        // Prefix the key with 's' to distinguish him from the client domain lookups
        String key = keyPrefix + domain;
        // Return item from cache if it exists.
        if (cache.containsKey(key)) {
            List<HostAddress> addresses = cache.get(key);
            if (addresses != null) {
                return addresses;
            }
        }

        if (dnsResolver == null)
            throw new IllegalStateException("No DNS resolver active.");

        List<HostAddress> addresses = new ArrayList<HostAddress>();

        // Step one: Do SRV lookups
        String srvDomain;
        if (keyPrefix == 's') {
            srvDomain = "_xmpp-server._tcp." + domain;
        } else if (keyPrefix == 'c') {
            srvDomain = "_xmpp-client._tcp." + domain;
        } else {
            srvDomain = domain;
        }
        List<SRVRecord> srvRecords = dnsResolver.lookupSRVRecords(srvDomain);
        List<HostAddress> sortedRecords = sortSRVRecords(srvRecords);
        if (sortedRecords != null)
            addresses.addAll(sortedRecords);

        // Step two: Add the hostname to the end of the list
        addresses.add(new HostAddress(domain));

        // Add item to cache.
        cache.put(key, addresses);

        return addresses;
    }

    /**
     * Sort a given list of SRVRecords as described in RFC 2782
     * Note that we follow the RFC with one exception. In a group of the same priority, only the first entry
     * is calculated by random. The others are ore simply ordered by their priority.
     * 
     * @param records
     * @return
     */
    protected static List<HostAddress> sortSRVRecords(List<SRVRecord> records) {
        // RFC 2782, Usage rules: "If there is precisely one SRV RR, and its Target is "."
        // (the root domain), abort."
        if (records.size() == 1 && records.get(0).getFQDN().equals("."))
            return null;

        // sorting the records improves the performance of the bisection later
        Collections.sort(records);

        // create the priority buckets
        SortedMap<Integer, List<SRVRecord>> buckets = new TreeMap<Integer, List<SRVRecord>>();
        for (SRVRecord r : records) {
            Integer priority = r.getPriority();
            List<SRVRecord> bucket = buckets.get(priority);
            // create the list of SRVRecords if it doesn't exist
            if (bucket == null) {
                bucket = new LinkedList<SRVRecord>();
                buckets.put(priority, bucket);
            }
            bucket.add(r);
        }

        List<HostAddress> res = new ArrayList<HostAddress>(records.size());

        for (Integer priority : buckets.keySet()) {
            List<SRVRecord> bucket = buckets.get(priority);
            int bucketSize;
            while ((bucketSize = bucket.size()) > 0) {
                int[] totals = new int[bucket.size()];
                int running_total = 0;
                int count = 0;
                int zeroWeight = 1;

                for (SRVRecord r : bucket) {
                    if (r.getWeight() > 0)
                        zeroWeight = 0;
                }

                for (SRVRecord r : bucket) {
                    running_total += (r.getWeight() + zeroWeight);
                    totals[count] = running_total;
                    count++;
                }
                int selectedPos;
                if (running_total == 0) {
                    // If running total is 0, then all weights in this priority
                    // group are 0. So we simply select one of the weights randomly
                    // as the other 'normal' algorithm is unable to handle this case
                    selectedPos = (int) (Math.random() * bucketSize);
                } else {
                    double rnd = Math.random() * running_total;
                    selectedPos = bisect(totals, rnd);
                } 
                // add the SRVRecord that was randomly chosen on it's weight
                // to the start of the result list
                SRVRecord chosenSRVRecord = bucket.remove(selectedPos);
                res.add(chosenSRVRecord);
            }
        }

        return res;
    }

    // TODO this is not yet really bisection just a stupid linear search
    private static int bisect(int[] array, double value) {
        int pos = 0;
        for (int element : array) {
            if (value < element)
                break;
            pos++;
        }
        return pos;
    }
}