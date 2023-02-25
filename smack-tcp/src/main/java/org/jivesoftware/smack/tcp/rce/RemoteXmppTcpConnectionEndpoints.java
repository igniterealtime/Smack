/**
 *
 * Copyright 2015-2020 Florian Schmaus.
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
package org.jivesoftware.smack.tcp.rce;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;

import org.minidns.dnsname.DnsName;
import org.minidns.record.InternetAddressRR;
import org.minidns.record.SRV;
import org.minidns.util.SrvUtil;

public class RemoteXmppTcpConnectionEndpoints {

    private static final Logger LOGGER = Logger.getLogger(RemoteXmppTcpConnectionEndpoints.class.getName());

    public static final String XMPP_CLIENT_DNS_SRV_PREFIX = "_xmpp-client._tcp";
    public static final String XMPP_SERVER_DNS_SRV_PREFIX = "_xmpp-server._tcp";

    /**
     * Lookups remote connection endpoints on the server for XMPP connections over TCP taking A, AAAA and SRV resource
     * records into account. If no host address was configured and all lookups failed, for example with NX_DOMAIN, then
     * result will be populated with the empty list.
     *
     * @param config the connection configuration to lookup the endpoints for.
     * @return a lookup result.
     */
    public static Result<Rfc6120TcpRemoteConnectionEndpoint> lookup(ConnectionConfiguration config) {
        List<Rfc6120TcpRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints;
        List<RemoteConnectionEndpointLookupFailure> lookupFailures;

        final InetAddress hostAddress = config.getHostAddress();
        final DnsName host = config.getHost();

        if (hostAddress != null) {
            lookupFailures = Collections.emptyList();

            IpTcpRemoteConnectionEndpoint<InternetAddressRR<?>> connectionEndpoint = IpTcpRemoteConnectionEndpoint.from(
                            hostAddress.toString(), config.getPort(), hostAddress);
            discoveredRemoteConnectionEndpoints = Collections.singletonList(connectionEndpoint);
        } else if (host != null) {
            lookupFailures = new ArrayList<>(1);

            List<InetAddress> hostAddresses = DNSUtil.getDNSResolver().lookupHostAddress(host,
                            lookupFailures, config.getDnssecMode());

            if (hostAddresses != null) {
                discoveredRemoteConnectionEndpoints = new ArrayList<>(hostAddresses.size());
                UInt16 port = config.getPort();
                for (InetAddress inetAddress : hostAddresses) {
                    IpTcpRemoteConnectionEndpoint<InternetAddressRR<?>> connectionEndpoint = IpTcpRemoteConnectionEndpoint.from(
                                    host, port, inetAddress);
                    discoveredRemoteConnectionEndpoints.add(connectionEndpoint);
                }
            } else {
                discoveredRemoteConnectionEndpoints = Collections.emptyList();
            }
        } else {
            lookupFailures = new ArrayList<>();

            // N.B.: Important to use config.serviceName and not AbstractXMPPConnection.serviceName
            DnsName dnsName = config.getXmppServiceDomainAsDnsNameIfPossible();
            if (dnsName == null) {
                // TODO: ConnectionConfiguration should check on construction time that either the given XMPP service
                // name is also a valid DNS name, or that a host is explicitly configured.
                throw new IllegalStateException();
            }
            discoveredRemoteConnectionEndpoints = resolveXmppServiceDomain(dnsName, lookupFailures, config.getDnssecMode());
        }

        // Either the populated host addresses are not empty *or* there must be at least one failed address.
        assert !discoveredRemoteConnectionEndpoints.isEmpty() || !lookupFailures.isEmpty();

        return new Result<>(discoveredRemoteConnectionEndpoints, lookupFailures);
    }

    public static final class Result<RCE extends RemoteConnectionEndpoint> {
        public final List<RCE> discoveredRemoteConnectionEndpoints;
        public final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

        private Result(List<RCE> discoveredRemoteConnectionEndpoints, List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
            this.discoveredRemoteConnectionEndpoints = discoveredRemoteConnectionEndpoints;
            this.lookupFailures = lookupFailures;
        }
    }

    @SuppressWarnings("ImmutableEnumChecker")
    enum DomainType {
        server(XMPP_SERVER_DNS_SRV_PREFIX),
        client(XMPP_CLIENT_DNS_SRV_PREFIX),
        ;
        public final DnsName srvPrefix;

        DomainType(String srvPrefixString) {
            srvPrefix = DnsName.from(srvPrefixString);
        }
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
     * @param lookupFailures on optional list that will be populated with host addresses that failed to resolve.
     * @param dnssecMode DNSSec mode.
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<Rfc6120TcpRemoteConnectionEndpoint> resolveXmppServiceDomain(DnsName domain,
                    List<RemoteConnectionEndpointLookupFailure> lookupFailures, DnssecMode dnssecMode) {
        DNSResolver dnsResolver = getDnsResolverOrThrow();
        return resolveDomain(domain, DomainType.client, lookupFailures, dnssecMode, dnsResolver);
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
     * @param lookupFailures a list that will be populated with host addresses that failed to resolve.
     * @param dnssecMode DNSSec mode.
     * @return List of HostAddress, which encompasses the hostname and port that the
     *      XMPP server can be reached at for the specified domain.
     */
    public static List<Rfc6120TcpRemoteConnectionEndpoint> resolveXmppServerDomain(DnsName domain,
                    List<RemoteConnectionEndpointLookupFailure> lookupFailures, DnssecMode dnssecMode) {
        DNSResolver dnsResolver = getDnsResolverOrThrow();
        return resolveDomain(domain, DomainType.server, lookupFailures, dnssecMode, dnsResolver);
    }

    /**
     *
     * @param domain the domain.
     * @param domainType the XMPP domain type, server or client.
     * @param lookupFailures a list that will be populated with all failures that oocured during lookup.
     * @param dnssecMode the DNSSEC mode.
     * @param dnsResolver the DNS resolver to use.
     * @return a list of resolved host addresses for this domain.
     */
    private static List<Rfc6120TcpRemoteConnectionEndpoint> resolveDomain(DnsName domain, DomainType domainType,
                    List<RemoteConnectionEndpointLookupFailure> lookupFailures, DnssecMode dnssecMode, DNSResolver dnsResolver) {
        List<Rfc6120TcpRemoteConnectionEndpoint> endpoints = new ArrayList<>();

        // Step one: Do SRV lookups
        DnsName srvDomain = DnsName.from(domainType.srvPrefix, domain);

        Collection<SRV> srvRecords = dnsResolver.lookupSrvRecords(srvDomain, lookupFailures, dnssecMode);
        if (srvRecords != null && !srvRecords.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                String logMessage = "Resolved SRV RR for " + srvDomain + ":";
                for (SRV r : srvRecords)
                    logMessage += " " + r;
                LOGGER.fine(logMessage);
            }

            List<SRV> sortedSrvRecords = SrvUtil.sortSrvRecords(srvRecords);

            for (SRV srv : sortedSrvRecords) {
                List<InetAddress> targetInetAddresses = dnsResolver.lookupHostAddress(srv.target, lookupFailures, dnssecMode);
                if (targetInetAddresses != null) {
                    SrvXmppRemoteConnectionEndpoint endpoint = new SrvXmppRemoteConnectionEndpoint(srv, targetInetAddresses);
                    endpoints.add(endpoint);
                }
            }
        } else {
            LOGGER.info("Could not resolve DNS SRV resource records for " + srvDomain + ". Consider adding those.");
        }

        UInt16 defaultPort;
        switch (domainType) {
        case client:
            defaultPort = UInt16.from(5222);
            break;
        case server:
            defaultPort = UInt16.from(5269);
            break;
        default:
            throw new AssertionError();
        }

        // Step two: Add the hostname to the end of the list
        List<InetAddress> hostAddresses = dnsResolver.lookupHostAddress(domain, lookupFailures, dnssecMode);
        if (hostAddresses != null) {
            for (InetAddress inetAddress : hostAddresses) {
                IpTcpRemoteConnectionEndpoint<InternetAddressRR<?>> endpoint = IpTcpRemoteConnectionEndpoint.from(domain, defaultPort, inetAddress);
                endpoints.add(endpoint);
            }
        }

        return endpoints;
    }

    private static DNSResolver getDnsResolverOrThrow() {
        final DNSResolver dnsResolver = DNSUtil.getDNSResolver();
        if (dnsResolver == null) {
            throw new IllegalStateException("No DNS resolver configured in Smack");
        }
        return dnsResolver;
    }
}
