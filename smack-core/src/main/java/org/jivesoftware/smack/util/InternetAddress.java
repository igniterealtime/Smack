/**
 *
 * Copyright 2019-2023 Florian Schmaus
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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.minidns.dnslabel.DnsLabel;
import org.minidns.dnsname.DnsName;
import org.minidns.dnsname.InvalidDnsNameException;
import org.minidns.util.InetAddressUtil;

/**
 * An internet address, can be given as IP or as DNS name.
 * <p>
 * This type is meant for strings that hold an internet address. The original string used to construct this type is
 * stored and returning in the {@link #toString()} method.
 * </p>
 *
 * @since 4.4.0
 */
public abstract class InternetAddress implements CharSequence {

    protected final String originalString;

    protected InternetAddress(String originalString) {
        this.originalString = Objects.requireNonNull(originalString, "The 'originalString' argument must not be null");
    }

    public abstract InetAddress asInetAddress() throws UnknownHostException;

    @Override
    public String toString() {
        return originalString;
    }

    @Override
    public int length() {
        return originalString.length();
    }

    @Override
    public char charAt(int index) {
        return originalString.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return originalString.subSequence(start, end);
    }

    public String getRaw() {
        return originalString;
    }

    public static InternetAddress fromIgnoringZoneId(String address) {
        return from(address, true);
    }

    public static InternetAddress from(String address) {
        return from(address, false);
    }

    private static InternetAddress from(String address, boolean ignoreZoneId) {
        String raw = address;
        if (ignoreZoneId) {
            int percentPosition = address.indexOf('%');
            if (percentPosition > 1) {
                address = address.substring(0, percentPosition);
            }
        }

        final InternetAddress internetAddress;
        if (InetAddressUtil.isIpV4Address(address)) {
            internetAddress = new InternetAddress.Ipv4(address, raw);
        } else if (InetAddressUtil.isIpV6Address(address)) {
            internetAddress = new InternetAddress.Ipv6(address, raw);
        } else if (address.contains(".")) {
            InternetAddress domainNameInternetAddress;
            try {
                DnsName dnsName = DnsName.from(address);
                domainNameInternetAddress = new InternetAddress.DomainName(address, dnsName);
            } catch (InvalidDnsNameException e) {
                domainNameInternetAddress = new InternetAddress.InvalidDomainName(address, e);
            }
            internetAddress = domainNameInternetAddress;
        } else {
            DnsLabel dnsLabel = DnsLabel.from(address);
            internetAddress = new InternetAddress.DomainNameLabel(address, dnsLabel);
        }
        return internetAddress;
    }

    public static InternetAddress from(InetAddress inetAddress) {
        if (inetAddress instanceof Inet4Address) {
            return new InternetAddress.Ipv4(inetAddress.getHostAddress(), (Inet4Address) inetAddress);
        } else if (inetAddress instanceof Inet6Address) {
            return new InternetAddress.Ipv6(inetAddress.getHostAddress(), (Inet6Address) inetAddress);
        } else {
            throw new IllegalArgumentException("Unknown type " + inetAddress.getClass() + " of " + inetAddress);
        }
    }

    private static class InetAddressInternetAddress extends InternetAddress {
        private final InetAddress inetAddress;
        private final String raw;

        protected InetAddressInternetAddress(String originalString, String raw, InetAddress inetAddress) {
            super(originalString);
            this.raw = raw;
            this.inetAddress = inetAddress;
        }

        @Override
        public InetAddress asInetAddress() {
            return inetAddress;
        }

        @Override
        public final String getRaw() {
            return raw;
        }
    }

    public static final class Ipv4 extends InetAddressInternetAddress {

        private final Inet4Address inet4Address;

        private Ipv4(String originalString, String raw) {
            this(originalString, raw, InetAddressUtil.ipv4From(originalString));
        }

        private Ipv4(String originalString, Inet4Address inet4Address) {
            this(originalString, originalString, inet4Address);
        }

        private Ipv4(String originalString, String raw, Inet4Address inet4Address) {
            super(originalString, raw, inet4Address);
            this.inet4Address = inet4Address;
        }

        public Inet4Address getInet4Address() {
            return inet4Address;
        }
    }

    public static final class Ipv6 extends InetAddressInternetAddress {

        private Inet6Address inet6Address;

        private Ipv6(String originalString, String raw) {
            this(originalString, raw, InetAddressUtil.ipv6From(originalString));
        }

        private Ipv6(String originalString, Inet6Address inet6Address) {
            this(originalString, originalString, inet6Address);
        }

        private Ipv6(String originalString, String raw, Inet6Address inet6Address) {
            super(originalString, raw, inet6Address);
            this.inet6Address = inet6Address;
        }

        public Inet6Address getInet6Address() {
            return inet6Address;
        }
    }

    private static class NonNumericInternetAddress extends InternetAddress {
        private boolean attemptedToResolveInetAddress;
        private InetAddress inetAddress;

        protected NonNumericInternetAddress(String originalString) {
            super(originalString);
        }

        @Override
        public InetAddress asInetAddress() throws UnknownHostException {
            if (inetAddress != null || attemptedToResolveInetAddress) {
                return inetAddress;
            }

            attemptedToResolveInetAddress = true;
            inetAddress = InetAddress.getByName(originalString);

            return inetAddress;
        }
    }

    public static final class DomainName extends NonNumericInternetAddress {

        private final DnsName dnsName;

        private DomainName(String originalString, DnsName dnsName) {
            super(originalString);
            this.dnsName = dnsName;
        }

        public DnsName getDnsName() {
            return dnsName;
        }

    }

    public static final class DomainNameLabel extends NonNumericInternetAddress {

        private final DnsLabel dnsLabel;

        private DomainNameLabel(String originalString, DnsLabel dnsLabel) {
            super(originalString);
            this.dnsLabel = dnsLabel;
        }

        public DnsLabel getDnsLabel() {
            return dnsLabel;
        }
    }

    public static final class InvalidDomainName extends NonNumericInternetAddress {

        private final InvalidDnsNameException invalidDnsNameException;

        private InvalidDomainName(String originalString, InvalidDnsNameException invalidDnsNameException) {
            super(originalString);
            this.invalidDnsNameException = invalidDnsNameException;
        }

        public InvalidDnsNameException getInvalidDnsNameException() {
            return invalidDnsNameException;
        }
    }
}
