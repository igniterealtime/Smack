/*
 *
 * Copyright 2013-2024 Florian Schmaus
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
package org.jivesoftware.smack.util.dns.javax;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;

import org.minidns.dnsname.DnsName;
import org.minidns.record.SRV;

/**
 * A DNS resolver (mostly for SRV records), which makes use of the API provided in the javax.* namespace.
 * Note that using JavaxResolver requires applications using newer Java versions (at least 11) to declare a dependency on the "sun.jdk" module.
 *
 * @author Florian Schmaus
 *
 */
@SuppressWarnings("JdkObsolete")
public class JavaxResolver extends DNSResolver implements SmackInitializer {

    private static JavaxResolver instance;
    private static DirContext dirContext;

    static {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            dirContext = new InitialDirContext(env);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Could not construct InitialDirContext", e);
        }

        // Try to set this DNS resolver as primary one
        setup();
    }

    public static synchronized DNSResolver getInstance() {
        if (instance == null && isSupported()) {
            instance = new JavaxResolver();
        }
        return instance;
    }

    public static boolean isSupported() {
        return dirContext != null;
    }

    public static void setup() {
        DNSUtil.setDNSResolver(getInstance());
    }

    public JavaxResolver() {
         super(false);
    }

    @Override
    @SuppressWarnings("BanJNDI")
    protected List<SRV> lookupSrvRecords0(DnsName name, List<RemoteConnectionEndpointLookupFailure> lookupFailures,
                    DnssecMode dnssecMode) {
        Attribute srvAttribute;
        try {
            Attributes dnsLookup = dirContext.getAttributes(name.ace, new String[] { "SRV" });
            srvAttribute = dnsLookup.get("SRV");
            if (srvAttribute == null)
               return null;
        } catch (NameNotFoundException e) {
            LOGGER.log(Level.FINEST, "No DNS SRV RR found for " + name, e);
            return null;
        } catch (NamingException e) {
            RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, e);
            lookupFailures.add(failure);
            return null;
        }

        List<SRV> res = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            NamingEnumeration<String> srvRecords = (NamingEnumeration<String>) srvAttribute.getAll();
            while (srvRecords.hasMore()) {
                String srvRecordString = srvRecords.next();
                String[] srvRecordEntries = srvRecordString.split(" ");
                int priority = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 4]);
                int port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 2]);
                int weight = Integer.parseInt(srvRecordEntries[srvRecordEntries.length - 3]);
                String srvTarget = srvRecordEntries[srvRecordEntries.length - 1];
                // Strip trailing '.' from srvTarget.
                // Later MiniDNS version may do the right thing when DnsName.from() is called with a DNS name string
                // having a trailing dot, so this can possibly be removed in future Smack versions.
                if (srvTarget.length() > 0 && srvTarget.charAt(srvTarget.length() - 1) == '.') {
                    srvTarget = srvTarget.substring(0, srvTarget.length() - 1);
                }

                SRV srvRecord = new SRV(priority, weight, port, srvTarget);
                res.add(srvRecord);
            }
        }
        catch (NamingException e) {
            RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, e);
            lookupFailures.add(failure);
        }

        return res;
    }

    @Override
    public List<Exception> initialize() {
        setup();
        return null;
    }

}
