/**
 *
 * Copyright 2003-2005 Jive Software, 2016 Florian Schmaus.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.SmackDaneProvider;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

/**
 * Utility class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 * @author Florian Schmaus
 */
public class DNSUtil {

    private static final Logger LOGGER = Logger.getLogger(DNSUtil.class.getName());
    private static DNSResolver dnsResolver = null;
    private static SmackDaneProvider daneProvider;

    /**
     * International Domain Name transformer.
     * <p>
     * Used to transform Unicode representations of the Domain Name to ASCII in
     * order to perform a DNS request with the ASCII representation.
     * 'java.net.IDN' is available since Android API 9, but as long as Smack
     * requires API 8, we are going to need this. This part is going to get
     * removed once Smack depends on Android API 9 or higher.
     * </p>
     */
    private static StringTransformer idnaTransformer = new StringTransformer() {
        @Override
        public String transform(String string) {
            return string;
        }
    };

    /**
     * Set the DNS resolver that should be used to perform DNS lookups.
     *
     * @param resolver
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
     * @param daneProvider
     */
    public static void setDaneProvider(SmackDaneProvider daneProvider) {
        daneProvider = Objects.requireNonNull(daneProvider);
    }

    /**
     * Returns the currently active DANE provider used when DANE is enabled.
     *
     * @return the active DANE provider
     */
    public static SmackDaneProvider getDaneProvider() {
        return daneProvider;
    }

    /**
     * Set the IDNA (Internationalizing Domain Names in Applications, RFC 3490) transformer.
     * <p>
     * You usually want to wrap 'java.net.IDN.toASCII()' into a StringTransformer here.
     * </p>
     * @param idnaTransformer
     */
    public static void setIdnaTransformer(StringTransformer idnaTransformer) {
        if (idnaTransformer == null) {
            throw new NullPointerException();
        }
        DNSUtil.idnaTransformer = idnaTransformer;
    }

    private static enum DomainType {
        Server,
        Client,
        ;
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
     * @param failedAddresses on optional list that will be populated with host addresses that failed to resolve.
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<HostAddress> resolveXMPPServiceDomain(String domain, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        domain = idnaTransformer.transform(domain);

        return resolveDomain(domain, DomainType.Client, failedAddresses, dnssecMode);
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
     * @param failedAddresses on optional list that will be populated with host addresses that failed to resolve.
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<HostAddress> resolveXMPPServerDomain(String domain, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        domain = idnaTransformer.transform(domain);

        return resolveDomain(domain, DomainType.Server, failedAddresses, dnssecMode);
    }

    /**
     * 
     * @param domain the domain.
     * @param domainType the XMPP domain type, server or client.
     * @param failedAddresses a list that will be populated with host addresses that failed to resolve.
     * @return a list of resolver host addresses for this domain.
     */
    private static List<HostAddress> resolveDomain(String domain, DomainType domainType,
                    List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        if (dnsResolver == null) {
            throw new IllegalStateException("No DNS Resolver active in Smack");
        }

        List<HostAddress> addresses = new ArrayList<HostAddress>();

        // Step one: Do SRV lookups
        String srvDomain;
        switch (domainType) {
        case Server:
            srvDomain = "_xmpp-server._tcp." + domain;
            break;
        case Client:
            srvDomain = "_xmpp-client._tcp." + domain;
            break;
        default:
            throw new AssertionError();
        }

        List<SRVRecord> srvRecords = dnsResolver.lookupSRVRecords(srvDomain, failedAddresses, dnssecMode);
        if (srvRecords != null && !srvRecords.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                String logMessage = "Resolved SRV RR for " + srvDomain + ":";
                for (SRVRecord r : srvRecords)
                    logMessage += " " + r;
                LOGGER.fine(logMessage);
            }
            List<HostAddress> sortedRecords = sortSRVRecords(srvRecords);
            addresses.addAll(sortedRecords);
        } else {
            LOGGER.info("Could not resolve DNS SRV resource records for " + srvDomain + ". Consider adding those.");
        }

        int defaultPort = -1;
        switch (domainType) {
        case Client:
            defaultPort = 5222;
            break;
        case Server:
            defaultPort = 5269;
            break;
        }
        // Step two: Add the hostname to the end of the list
        HostAddress hostAddress = dnsResolver.lookupHostAddress(domain, defaultPort, failedAddresses, dnssecMode);
        if (hostAddress != null) {
            addresses.add(hostAddress);
        }

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
                int[] totals = new int[bucketSize];
                int running_total = 0;
                int count = 0;
                int zeroWeight = 1;

                for (SRVRecord r : bucket) {
                    if (r.getWeight() > 0) {
                        zeroWeight = 0;
                        break;
                    }
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
