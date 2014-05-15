/**
 *
 * Copyright 2013-2014 Florian Schmaus
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.SRVRecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * This implementation uses the <a href="http://www.dnsjava.org/">dnsjava</a> implementation for resolving DNS addresses.
 *
 */
public class DNSJavaResolver implements DNSResolver {
    
    private static DNSJavaResolver instance = new DNSJavaResolver();
    
    private DNSJavaResolver() {
    }
    
    public static DNSResolver getInstance() {
        return instance;
    }

    @Override
    public List<SRVRecord> lookupSRVRecords(String name) throws TextParseException {
        List<SRVRecord> res = new ArrayList<SRVRecord>();

        Lookup lookup = new Lookup(name, Type.SRV);
        Record recs[] = lookup.run();
        if (recs == null)
            return res;

        for (Record record : recs) {
            org.xbill.DNS.SRVRecord srvRecord = (org.xbill.DNS.SRVRecord) record;
            if (srvRecord != null && srvRecord.getTarget() != null) {
                String host = srvRecord.getTarget().toString();
                int port = srvRecord.getPort();
                int priority = srvRecord.getPriority();
                int weight = srvRecord.getWeight();

                SRVRecord r = new SRVRecord(host, port, priority, weight);
                res.add(r);
            }
        }

        return res;
    }
}
