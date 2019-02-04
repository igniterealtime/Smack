/**
 *
 * Copyright © 2013-2018 Florian Schmaus
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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smack.SmackException.ConnectionException;

import org.minidns.dnsname.DnsName;

public class HostAddress {
    private final DnsName fqdn;
    private final int port;
    private final Map<InetAddress, Exception> exceptions = new LinkedHashMap<>();
    private final List<InetAddress> inetAddresses;

    /**
     * Creates a new HostAddress with the given FQDN.
     *
     * @param fqdn the optional fully qualified domain name (FQDN).
     * @param port The port to connect on.
     * @param inetAddresses list of addresses.
     * @throws IllegalArgumentException If the port is out of valid range (0 - 65535).
     */
    public HostAddress(DnsName fqdn, int port, List<InetAddress> inetAddresses) {
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException(
                    "Port must be a 16-bit unsigned integer (i.e. between 0-65535. Port was: " + port);
        this.fqdn = fqdn;
        this.port = port;
        if (inetAddresses.isEmpty()) {
            throw new IllegalArgumentException("Must provide at least one InetAddress");
        }
        this.inetAddresses = inetAddresses;
    }

    public HostAddress(int port, InetAddress hostAddress) {
        this(null, port, Collections.singletonList(hostAddress));
    }

    /**
     * Constructs a new failed HostAddress. This constructor is usually used when the DNS resolution of the domain name
     * failed for some reason.
     *
     * @param fqdn the domain name of the host.
     * @param e the exception causing the failure.
     */
    public HostAddress(DnsName fqdn, Exception e) {
        this.fqdn = fqdn;
        this.port = 5222;
        inetAddresses = Collections.emptyList();
        setException(e);
    }

    public HostAddress(InetSocketAddress inetSocketAddress, Exception exception) {
        String hostString = inetSocketAddress.getHostString();
        this.fqdn = DnsName.from(hostString);
        this.port = inetSocketAddress.getPort();
        inetAddresses = Collections.emptyList();
        setException(exception);
    }

    public String getHost() {
        if (fqdn != null) {
            return fqdn.toString();
        }

        // In this case, the HostAddress(int, InetAddress) constructor must been used. We have no FQDN. And
        // inetAddresses.size() must be exactly one.
        assert inetAddresses.size() == 1;
        return inetAddresses.get(0).getHostAddress();
    }

    /**
     * Return the fully qualified domain name. This may return <code>null</code> in case there host address is only numeric, i.e. an IP address.
     *
     * @return the fully qualified domain name or <code>null</code>
     */
    public DnsName getFQDN() {
        return fqdn;
    }

    public int getPort() {
        return port;
    }

    public void setException(Exception exception) {
        setException(null, exception);
    }

    public void setException(InetAddress inetAddress, Exception exception) {
        Exception old = exceptions.put(inetAddress, exception);
        assert (old == null);
    }

    /**
     * Retrieve the Exception that caused a connection failure to this HostAddress. Every
     * HostAddress found in {@link ConnectionException} will have an Exception set,
     * which can be retrieved with this method.
     *
     * @return the Exception causing this HostAddress to fail
     */
    public Map<InetAddress, Exception> getExceptions() {
        return Collections.unmodifiableMap(exceptions);
    }

    public List<InetAddress> getInetAddresses() {
        return Collections.unmodifiableList(inetAddresses);
    }

    @Override
    public String toString() {
        return getHost() + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HostAddress)) {
            return false;
        }

        final HostAddress address = (HostAddress) o;

        if (!getHost().equals(address.getHost())) {
            return false;
        }
        return port == address.port;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + getHost().hashCode();
        return result * 37 + port;
    }

    public String getErrorMessage() {
        if (exceptions.isEmpty()) {
            return "No error logged";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\'').append(toString()).append("' failed because: ");
        Iterator<Entry<InetAddress, Exception>> iterator = exceptions.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<InetAddress, Exception> entry = iterator.next();
            InetAddress inetAddress = entry.getKey();
            if (inetAddress != null) {
                sb.append(entry.getKey()).append(" exception: ");
            }
            sb.append(entry.getValue());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
}
