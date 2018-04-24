/**
 *
 * Copyright 2014-2017 Florian Schmaus
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

import org.minidns.dnsmessage.DNSMessage.RESPONSE_CODE;
import org.minidns.dnsmessage.Question;
import org.minidns.dnsname.DNSName;
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
    protected List<SRVRecord> lookupSRVRecords0(final DNSName name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        final ResolverApi resolver = getResolver(dnssecMode);

        SrvResolverResult result;
        try {
            result = resolver.resolveSrv(name);
        } catch (IOException e) {
            failedAddresses.add(new HostAddress(name, e));
            return null;
        }

        ResolutionUnsuccessfulException resolutionUnsuccessfulException = result.getResolutionUnsuccessfulException();
        if (resolutionUnsuccessfulException != null) {
            failedAddresses.add(new HostAddress(name, resolutionUnsuccessfulException));
            return null;
        }

        if (shouldAbortIfNotAuthentic(name, dnssecMode, result, failedAddresses)) {
            return null;
        }

        List<SRVRecord> res = new LinkedList<>();
        for (SRV srv : result.getAnswers()) {
            DNSName hostname = srv.target;
            List<InetAddress> hostAddresses = lookupHostAddress0(hostname, failedAddresses, dnssecMode);
            if (shouldContinue(name, hostname, hostAddresses)) {
                continue;
            }

            SRVRecord srvRecord = new SRVRecord(hostname, srv.port, srv.priority, srv.weight, hostAddresses);
            res.add(srvRecord);
        }

        return res;
    }

    @Override
    protected List<InetAddress> lookupHostAddress0(final DNSName name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        final ResolverApi resolver = getResolver(dnssecMode);

        final ResolverResult<A> aResult;
        final ResolverResult<AAAA> aaaaResult;

        try {
            aResult = resolver.resolve(name, A.class);
            aaaaResult = resolver.resolve(name, AAAA.class);
        } catch (IOException e) {
            failedAddresses.add(new HostAddress(name, e));
            return null;
        }

        if (!aResult.wasSuccessful() && !aaaaResult.wasSuccessful()) {
            // Both results where not successful.
            failedAddresses.add(new HostAddress(name, getExceptionFrom(aResult)));
            failedAddresses.add(new HostAddress(name, getExceptionFrom(aaaaResult)));
            return null;
        }

        if (shouldAbortIfNotAuthentic(name, dnssecMode, aResult, failedAddresses)
                        || shouldAbortIfNotAuthentic(name, dnssecMode, aaaaResult, failedAddresses)) {
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

    private static boolean shouldAbortIfNotAuthentic(DNSName name, DnssecMode dnssecMode,
                    ResolverResult<?> result, List<HostAddress> failedAddresses) {
        switch (dnssecMode) {
        case needsDnssec:
        case needsDnssecAndDane:
            // Check if the result is authentic data, i.e. there a no reasons the result is unverified.
            // TODO: Use ResolverResult.getDnssecResultNotAuthenticException() of newer MiniDNS versions.
            if (!result.isAuthenticData()) {
                Exception exception = new Exception("DNSSEC verification failed: " + result.getUnverifiedReasons().iterator().next().getReasonString());
                failedAddresses.add(new HostAddress(name, exception));
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
