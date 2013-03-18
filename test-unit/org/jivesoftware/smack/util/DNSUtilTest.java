package org.jivesoftware.smack.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;


import org.jivesoftware.smack.util.dns.DNSJavaResolver;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.JavaxResolver;
import org.jivesoftware.smack.util.dns.SRVRecord;
import org.junit.Test;

public class DNSUtilTest {
    private static final String igniterealtimeDomain = "igniterealtime.org";
    private static final String igniterealtimeXMPPServer = "xmpp." + igniterealtimeDomain;
    private static final int igniterealtimeClientPort = 5222;
    private static final int igniterealtimeServerPort = 5269;

    @Test
    public void xmppClientDomainJavaXTest() {
        DNSResolver resolver = JavaxResolver.maybeGetInstance();
        assertNotNull(resolver);
        DNSUtil.setDNSResolver(resolver);
        xmppClientDomainTest();
    }

    @Test
    public void xmppServerDomainJavaXTest() {
        DNSResolver resolver = JavaxResolver.maybeGetInstance();
        assertNotNull(resolver);
        DNSUtil.setDNSResolver(resolver);
        xmppServerDomainTest();
    }

    @Test
    public void xmppClientDomainDNSJavaTest() {
        DNSResolver resolver = DNSJavaResolver.getInstance();
        assertNotNull(resolver);
        DNSUtil.setDNSResolver(resolver);
        xmppClientDomainTest();
    }

    @Test
    public void xmppServerDomainDNSJavaTest() {
        DNSResolver resolver = DNSJavaResolver.getInstance();
        assertNotNull(resolver);
        DNSUtil.setDNSResolver(resolver);
        xmppServerDomainTest();
    }

    @Test
    public void sortSRVlowestPrioFirstTest() {
        List<HostAddress> sortedRecords = DNSUtil.sortSRVRecords(createSRVRecords());
        assertTrue(sortedRecords.get(0).getFQDN().equals("0.20.foo.bar"));
    }

    @Test
    public void sortSRVdistributeOverWeights() {
        int weight50 = 0;
        int weight20one = 0;
        int weight20two = 0;
        int weight10 = 0;
        for (int i = 0; i < 1000; i++) {
            List<HostAddress> sortedRecords = DNSUtil.sortSRVRecords(createSRVRecords());
            String host = sortedRecords.get(1).getFQDN();
            if (host.equals("5.20.one.foo.bar")) {
                weight20one++;
            } else if (host.equals("5.20.two.foo.bar")) {
                weight20two++;
            } else if (host.equals("5.10.foo.bar")) {
                weight10++;
            } else if (host.equals("5.50.foo.bar")) {
                weight50++;
            } else {
                fail("Wrong host after SRVRecord sorting");
            }
        }
        assertTrue(weight50 > 400 && weight50 < 600);
        assertTrue(weight20one > 100 && weight20one < 300);
        assertTrue(weight20two > 100 && weight20two < 300);
        assertTrue(weight10 > 0&& weight10 < 200);
    }

    @Test
    public void sortSRVdistributeZeroWeights() {
        int weightZeroOne = 0;
        int weightZeroTwo = 0;
        for (int i = 0; i < 1000; i++) {
            List<HostAddress> sortedRecords = DNSUtil.sortSRVRecords(createSRVRecords());
            // Remove the first 5 records with a lower priority
            for (int j = 0; j < 5; j++) {
                sortedRecords.remove(0);
            }
            String host = sortedRecords.remove(0).getFQDN();
            if (host.equals("10.0.one.foo.bar")) {
                weightZeroOne++;
            } else if (host.endsWith("10.0.two.foo.bar")) {
                weightZeroTwo++;
            } else {
                fail("Wrong host after SRVRecord sorting");
            }
        }
        assertTrue(weightZeroOne > 400 && weightZeroOne < 600);
        assertTrue(weightZeroTwo > 400 && weightZeroTwo < 600);
    }

    private void xmppClientDomainTest() {
        List<HostAddress> hostAddresses = DNSUtil.resolveXMPPDomain(igniterealtimeDomain);
        HostAddress ha = hostAddresses.get(0);
        assertEquals(ha.getFQDN(), igniterealtimeXMPPServer);
        assertEquals(ha.getPort(), igniterealtimeClientPort);
    }
    
    private void xmppServerDomainTest() {
        List<HostAddress> hostAddresses = DNSUtil.resolveXMPPServerDomain(igniterealtimeDomain);
        HostAddress ha = hostAddresses.get(0);
        assertEquals(ha.getFQDN(), igniterealtimeXMPPServer);
        assertEquals(ha.getPort(), igniterealtimeServerPort);
    }
    
    private static List<SRVRecord> createSRVRecords() {
        List<SRVRecord> records = new ArrayList<SRVRecord>();
        // We create one record with priority 0 that should also be tried first
        // Then 4 records with priority 5 and different weights (50, 20, 20, 10)
        // Then 2 records with priority 10 and weight 0 which should be treaded equal
        // These records are added in a 'random' way to the list
        try {
            records.add(new SRVRecord("5.20.one.foo.bar", 42, 5, 20));     // Priority 5, Weight 20
            records.add(new SRVRecord("10.0.one.foo.bar", 42, 10, 0)); // Priority 10, Weight 0
            records.add(new SRVRecord("5.10.foo.bar", 42, 5, 10));     // Priority 5, Weight 10
            records.add(new SRVRecord("10.0.two.foo.bar", 42, 10, 0)); // Priority 10, Weight 0
            records.add(new SRVRecord("5.20.two.foo.bar", 42, 5, 20));     // Priority 5, Weight 20
            records.add(new SRVRecord("0.20.foo.bar", 42, 0, 20));     // Priority 0, Weight 20
            records.add(new SRVRecord("5.50.foo.bar", 42, 5, 50));     // Priority 5, Weight 50
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        assertTrue(records.size() > 0);
        return records;
    }
}
