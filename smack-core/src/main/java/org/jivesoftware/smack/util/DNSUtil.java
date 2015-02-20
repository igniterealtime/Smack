/**
 *
 * Copyright 2003-2005 Jive Software.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

/**
 * Utility class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 */
public class DNSUtil {

    private static final Logger LOGGER = Logger.getLogger(DNSUtil.class.getName());
    private static DNSResolver dnsResolver = null;

    /**
     * Initializes DNSUtil. This method is automatically called by SmackConfiguration, you don't
     * have to call it manually.
     */
    public static void init() {
        final String[] RESOLVERS = new String[] { "javax.JavaxResolver", "minidns.MiniDnsResolver",
                        "dnsjava.DNSJavaResolver" };
        for (String resolver :RESOLVERS) {
            DNSResolver availableResolver = null;
            String resolverFull = "org.jivesoftware.smack.util.dns." + resolver;
            try {
                Class<?> resolverClass = Class.forName(resolverFull);
                Method getInstanceMethod = resolverClass.getMethod("getInstance");
                availableResolver = (DNSResolver) getInstanceMethod.invoke(null);
                if (availableResolver != null) {
                    setDNSResolver(availableResolver);
                    break;
                }
            }
            catch (ClassNotFoundException|NoSuchMethodException|SecurityException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
                LOGGER.log(Level.FINE, "Exception on init", e);
            }
        }
    }

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
     * @return the active DNSResolver
     */
    public static DNSResolver getDNSResolver() {
        return dnsResolver;
    }

    /**
     * Returns a list of HostAddresses under which the specified XMPP server can be reached at for client-to-server
     * communication. A DNS lookup for a SRV record in the form "_xmpp-client._tcp.example.com" is attempted, according
     * to section 3.2.1 of RFC 6120. If that lookup fails, it's assumed that the XMPP server lives at the host resolved
     * by a DNS lookup at the specified domain on the default port of 5222.
     * <p>
     * As an example, a lookup for "example.com" may return "im.example.com:5269".
     * </p>
     *
     * @param domain the domain.
     * @return List of HostAddress, which encompasses the hostname and port that the XMPP server can be reached at for
     *         the specified domain.
     */
    public static List<HostAddress> resolveXMPPDomain(final String domain) {
        if (dnsResolver == null) {
            List<HostAddress> addresses = new ArrayList<HostAddress>(1);
            addresses.add(new HostAddress(domain, 5222));
            return addresses;
        }
        return resolveDomain(domain, 'c');
    }

    /**
     * Returns a list of HostAddresses under which the specified XMPP server can be reached at for server-to-server
     * communication. A DNS lookup for a SRV record in the form "_xmpp-server._tcp.example.com" is attempted, according
     * to section 3.2.1 of RFC 6120. If that lookup fails , it's assumed that the XMPP server lives at the host resolved
     * by a DNS lookup at the specified domain on the default port of 5269.
     * <p>
     * As an example, a lookup for "example.com" may return "im.example.com:5269".
     * </p>
     *
     * @param domain the domain.
     * @return List of HostAddress, which encompasses the hostname and port that the XMPP server can be reached at for
     *         the specified domain.
     */
    public static List<HostAddress> resolveXMPPServerDomain(final String domain) {
        if (dnsResolver == null) {
            List<HostAddress> addresses = new ArrayList<HostAddress>(1);
            addresses.add(new HostAddress(domain, 5269));
            return addresses;
        }
        return resolveDomain(domain, 's');
    }

    private static List<HostAddress> resolveDomain(String domain, char keyPrefix) {
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
        try {
            List<SRVRecord> srvRecords = dnsResolver.lookupSRVRecords(srvDomain);
            if (LOGGER.isLoggable(Level.FINE)) {
                String logMessage = "Resolved SRV RR for " + srvDomain + ":";
                for (SRVRecord r : srvRecords)
                    logMessage += " " + r;
                LOGGER.fine(logMessage);
            }
            List<HostAddress> sortedRecords = sortSRVRecords(srvRecords);
            addresses.addAll(sortedRecords);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception while resovling SRV records for " + domain
                            + ". Consider adding '_xmpp-(server|client)._tcp' DNS SRV Records");
        }

        // Step two: Add the hostname to the end of the list
        addresses.add(new HostAddress(domain));

        return addresses;
    }

    /**
     * Sort a given list of SRVRecords as described in RFC 2782
     * Note that we follow the RFC with one exception. In a group of the same priority, only the first entry
     * is calculated by random. The others are ore simply ordered by their priority.
     * 
     * @param records
     * @return the list of resolved HostAddresses
     */
    private static List<HostAddress> sortSRVRecords(List<SRVRecord> records) {
        // RFC 2782, Usage rules: "If there is precisely one SRV RR, and its Target is "."
        // (the root domain), abort."
        if (records.size() == 1 && records.get(0).getFQDN().equals("."))
            return Collections.emptyList();

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
