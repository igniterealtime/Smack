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
import java.util.logging.Level;
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
     * @param failedAddresses list of failed addresses.
     * @param dnssecMode security mode.
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

    /**
     * Lookup the IP addresses of a given host name. Returns <code>null</code> if there was an error, in which the error
     * reason will be added in form of a <code>HostAddress</code> to <code>failedAddresses</code>. Returns a empty list
     * in case the DNS name exists but has no associated A or AAAA resource records. Otherwise, if the resolution was
     * successful <em>and</em> there is at least one A or AAAA resource record, then a non-empty list will be returned.
     * <p>
     * Concrete DNS resolver implementations are free to overwrite this, but have to stick to the interface contract.
     * </p>
     *
     * @param name the DNS name to lookup
     * @param failedAddresses a list with the failed addresses
     * @param dnssecMode the selected DNSSEC mode
     * @return A list, either empty or non-empty, or <code>null</code>
     */
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

    protected final boolean shouldContinue(CharSequence name, CharSequence hostname, List<InetAddress> hostAddresses) {
        if (hostAddresses == null) {
            return true;
        }

        // If hostAddresses is not null but empty, then the DNS resolution was successful but the domain did not
        // have any A or AAAA resource records.
        if (hostAddresses.isEmpty()) {
            LOGGER.log(Level.INFO, "The DNS name " + name + ", points to a hostname (" + hostname
                            + ") which has neither A or AAAA resource records. This is an indication of a broken DNS setup.");
            return true;
        }

        return false;
    }

    private final void checkIfDnssecRequestedAndSupported(DnssecMode dnssecMode) {
        if (dnssecMode != DnssecMode.disabled && !supportsDnssec) {
            throw new UnsupportedOperationException("This resolver does not support DNSSEC");
        }
    }
}
