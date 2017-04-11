/**
 *
 * Copyright 2013-2017 Florian Schmaus
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
package org.jivesoftware.smack.util.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;

/**
 * Implementations of this interface define a class that is capable of resolving DNS addresses.
 *
 */
public abstract class DNSResolver {

    protected static final Logger LOGGER = Logger.getLogger(DNSResolver.class.getName());

    private final boolean supportsDnssec;

    protected DNSResolver(boolean supportsDnssec) {
        this.supportsDnssec = supportsDnssec;
    }

    /**
     * Gets a list of service records for the specified service.
     * @param name The symbolic name of the service.
     * @return The list of SRV records mapped to the service name.
     */
    public final List<SRVRecord> lookupSRVRecords(String name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        checkIfDnssecRequestedAndSupported(dnssecMode);
        return lookupSRVRecords0(name, failedAddresses, dnssecMode);
    }

    protected abstract List<SRVRecord> lookupSRVRecords0(String name, List<HostAddress> failedAddresses, DnssecMode dnssecMode);

    public final HostAddress lookupHostAddress(String name, int port, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        checkIfDnssecRequestedAndSupported(dnssecMode);
        List<InetAddress> inetAddresses = lookupHostAddress0(name, failedAddresses, dnssecMode);
        if (inetAddresses == null || inetAddresses.isEmpty()) {
            return null;
        }
        return new HostAddress(name, port, inetAddresses);
    }

    protected List<InetAddress> lookupHostAddress0(String name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        // Default implementation of a DNS name lookup for A/AAAA records. It is assumed that this method does never
        // support DNSSEC. Subclasses are free to override this method.
        if (dnssecMode != DnssecMode.disabled) {
            throw new UnsupportedOperationException("This resolver does not support DNSSEC");
        }

        InetAddress[] inetAddressArray;
        try {
            inetAddressArray = InetAddress.getAllByName(name);
        } catch (UnknownHostException e) {
            failedAddresses.add(new HostAddress(name, e));
            return null;
        }

        return Arrays.asList(inetAddressArray);
    }

    private final void checkIfDnssecRequestedAndSupported(DnssecMode dnssecMode) {
        if (dnssecMode != DnssecMode.disabled && !supportsDnssec) {
            throw new UnsupportedOperationException("This resolver does not support DNSSEC");
        }
    }
}
