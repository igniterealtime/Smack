/**
 *
 * Copyright 2013-2018 Florian Schmaus
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
package org.jivesoftware.smack.util.dns.dnsjava;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

import org.minidns.dnsname.DNSName;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * This implementation uses the <a href="http://www.dnsjava.org/">dnsjava</a> implementation for resolving DNS addresses.
 *
 */
public class DNSJavaResolver extends DNSResolver implements SmackInitializer {

    private static final DNSJavaResolver instance = new DNSJavaResolver();

    public static DNSResolver getInstance() {
        return instance;
    }

    public DNSJavaResolver() {
        super(false);
    }

    @Override
    protected List<SRVRecord> lookupSRVRecords0(DNSName name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        List<SRVRecord> res = new ArrayList<>();

        Lookup lookup;
        try {
            lookup = new Lookup(name.ace, Type.SRV);
        }
        catch (TextParseException e) {
            throw new IllegalStateException(e);
        }

        Record[] recs = lookup.run();
        if (recs == null)
            return res;

        for (Record record : recs) {
            org.xbill.DNS.SRVRecord srvRecord = (org.xbill.DNS.SRVRecord) record;
            if (srvRecord != null && srvRecord.getTarget() != null) {
                DNSName host = DNSName.from(srvRecord.getTarget().toString());
                int port = srvRecord.getPort();
                int priority = srvRecord.getPriority();
                int weight = srvRecord.getWeight();

                List<InetAddress> hostAddresses = lookupHostAddress0(host, failedAddresses, dnssecMode);
                if (shouldContinue(name, host, hostAddresses)) {
                    continue;
                }

                SRVRecord r = new SRVRecord(host, port, priority, weight, hostAddresses);
                res.add(r);
            }
        }

        return res;
    }

    public static void setup() {
        DNSUtil.setDNSResolver(getInstance());
    }

    @Override
    public List<Exception> initialize() {
        setup();
        return null;
    }

}
