/**
 *
 * Copyright 2014-2020 Florian Schmaus
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
package org.jivesoftware.smack.util.dns.minidns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;

import org.minidns.dnsmessage.DnsMessage.RESPONSE_CODE;
import org.minidns.dnsmessage.Question;
import org.minidns.dnsname.DnsName;
import org.minidns.dnssec.DnssecResultNotAuthenticException;
import org.minidns.hla.DnssecResolverApi;
import org.minidns.hla.ResolutionUnsuccessfulException;
import org.minidns.hla.ResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.hla.SrvResolverResult;
import org.minidns.record.A;
import org.minidns.record.AAAA;
import org.minidns.record.SRV;


/**
 * This implementation uses the <a href="https://github.com/rtreffer/minidns/">MiniDNS</a> implementation for
 * resolving DNS addresses.
 */
public class MiniDnsResolver extends DNSResolver implements SmackInitializer {

    private static final MiniDnsResolver INSTANCE = new MiniDnsResolver();

    private static final ResolverApi DNSSEC_RESOLVER = DnssecResolverApi.INSTANCE;

    private static final ResolverApi NON_DNSSEC_RESOLVER = ResolverApi.INSTANCE;

    public static DNSResolver getInstance() {
        return INSTANCE;
    }

    public MiniDnsResolver() {
        super(true);
    }

    @Override
    protected Set<SRV> lookupSrvRecords0(final DnsName name, List<RemoteConnectionEndpointLookupFailure> lookupFailures,
                    DnssecMode dnssecMode) {
        final ResolverApi resolver = getResolver(dnssecMode);

        SrvResolverResult result;
        try {
            result = resolver.resolveSrv(name);
        } catch (IOException e) {
            RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, e);
            lookupFailures.add(failure);
            return null;
        }

        ResolutionUnsuccessfulException resolutionUnsuccessfulException = result.getResolutionUnsuccessfulException();
        if (resolutionUnsuccessfulException != null) {
            RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, resolutionUnsuccessfulException);
            lookupFailures.add(failure);
            return null;
        }

        if (shouldAbortIfNotAuthentic(name, dnssecMode, result, lookupFailures)) {
            return null;
        }

        return result.getAnswers();
    }

    @Override
    protected List<InetAddress> lookupHostAddress0(final DnsName name,
                    List<RemoteConnectionEndpointLookupFailure> lookupFailures, DnssecMode dnssecMode) {
        final ResolverApi resolver = getResolver(dnssecMode);

        final ResolverResult<A> aResult;
        final ResolverResult<AAAA> aaaaResult;

        try {
            aResult = resolver.resolve(name, A.class);
            aaaaResult = resolver.resolve(name, AAAA.class);
        } catch (IOException e) {
            RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, e);
            lookupFailures.add(failure);
            return null;
        }

        if (!aResult.wasSuccessful() && !aaaaResult.wasSuccessful()) {
            // Both results where not successful.
            RemoteConnectionEndpointLookupFailure failureA = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, getExceptionFrom(aResult));
            lookupFailures.add(failureA);
            RemoteConnectionEndpointLookupFailure failureAaaa = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                            name, getExceptionFrom(aaaaResult));
            lookupFailures.add(failureAaaa);
            return null;
        }

        if (shouldAbortIfNotAuthentic(name, dnssecMode, aResult, lookupFailures)
                        || shouldAbortIfNotAuthentic(name, dnssecMode, aaaaResult, lookupFailures)) {
            return null;
        }

        // TODO: Use ResolverResult.getAnswersOrEmptySet() once we updated MiniDNS.
        Set<A> aResults;
        if (aResult.wasSuccessful()) {
            aResults = aResult.getAnswers();
        }
        else {
            aResults = Collections.emptySet();
        }

        // TODO: Use ResolverResult.getAnswersOrEmptySet() once we updated MiniDNS.
        Set<AAAA> aaaaResults;
        if (aaaaResult.wasSuccessful()) {
            aaaaResults = aaaaResult.getAnswers();
        }
        else {
            aaaaResults = Collections.emptySet();
        }

        List<InetAddress> inetAddresses = new ArrayList<>(aResults.size()
                        + aaaaResults.size());

        for (A a : aResults) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByAddress(a.getIp());
            }
            catch (UnknownHostException e) {
                continue;
            }
            inetAddresses.add(inetAddress);
        }
        for (AAAA aaaa : aaaaResults) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByAddress(name.ace, aaaa.getIp());
            }
            catch (UnknownHostException e) {
                continue;
            }
            inetAddresses.add(inetAddress);
        }

        return inetAddresses;
    }

    public static void setup() {
        DNSUtil.setDNSResolver(getInstance());
    }

    @Override
    public List<Exception> initialize() {
        setup();
        MiniDnsDane.setup();
        return null;
    }

    private static ResolverApi getResolver(DnssecMode dnssecMode) {
        if (dnssecMode == DnssecMode.disabled) {
            return NON_DNSSEC_RESOLVER;
        } else {
            return DNSSEC_RESOLVER;
        }
    }

    private static boolean shouldAbortIfNotAuthentic(DnsName name, DnssecMode dnssecMode,
                    ResolverResult<?> result, List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
        switch (dnssecMode) {
        case needsDnssec:
        case needsDnssecAndDane:
            // Check if the result is authentic data, i.e. there a no reasons the result is unverified.
            DnssecResultNotAuthenticException exception = result.getDnssecResultNotAuthenticException();
            if (exception != null) {
                RemoteConnectionEndpointLookupFailure failure = new RemoteConnectionEndpointLookupFailure.DnsLookupFailure(
                                name, exception);
                lookupFailures.add(failure);
                return true;
            }
            break;
        case disabled:
            break;
        default:
            throw new IllegalStateException("Unknown DnssecMode: " + dnssecMode);
        }
        return false;
    }

    private static ResolutionUnsuccessfulException getExceptionFrom(ResolverResult<?> result) {
        Question question = result.getQuestion();
        RESPONSE_CODE responseCode = result.getResponseCode();
        ResolutionUnsuccessfulException resolutionUnsuccessfulException = new ResolutionUnsuccessfulException(question, responseCode);
        return resolutionUnsuccessfulException;
    }
}
