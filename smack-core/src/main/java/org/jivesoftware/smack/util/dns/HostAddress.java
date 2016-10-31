/**
 *
 * Copyright © 2013-2016 Florian Schmaus
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.util.Objects;

public class HostAddress {
    private final String fqdn;
    private final int port;
    private final Map<InetAddress, Exception> exceptions = new LinkedHashMap<>();
    private final List<InetAddress> inetAddresses;

    /**
     * Creates a new HostAddress with the given FQDN. The port will be set to the default XMPP client port: 5222
     * 
     * @param fqdn Fully qualified domain name.
     * @throws IllegalArgumentException If the fqdn is null.
     */
    public HostAddress(String fqdn, List<InetAddress> inetAddresses) {
        // Set port to the default port for XMPP client communication
        this(fqdn, 5222, inetAddresses);
    }

    /**
     * Creates a new HostAddress with the given FQDN. The port will be set to the default XMPP client port: 5222
     * 
     * @param fqdn Fully qualified domain name.
     * @param port The port to connect on.
     * @throws IllegalArgumentException If the fqdn is null or port is out of valid range (0 - 65535).
     */
    public HostAddress(String fqdn, int port, List<InetAddress> inetAddresses) {
        Objects.requireNonNull(fqdn, "FQDN is null");
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException(
                    "Port must be a 16-bit unsiged integer (i.e. between 0-65535. Port was: " + port);
        if (fqdn.charAt(fqdn.length() - 1) == '.') {
            this.fqdn = fqdn.substring(0, fqdn.length() - 1);
        }
        else {
            this.fqdn = fqdn;
        }
        this.port = port;
        if (inetAddresses.isEmpty()) {
            throw new IllegalArgumentException("Must provide at least one InetAddress");
        }
        this.inetAddresses = inetAddresses;
    }

    /**
     * Constructs a new failed HostAddress. This constructor is usually used when the DNS resolution of the domain name
     * failed for some reason.
     *
     * @param fqdn the domain name of the host.
     * @param e the exception causing the failure.
     */
    public HostAddress(String fqdn, Exception e) {
        this.fqdn = fqdn;
        this.port = 5222;
        inetAddresses = Collections.emptyList();
        setException(e);
    }

    public String getFQDN() {
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
        assert(old == null);
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
        return fqdn + ":" + port;
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

        if (!fqdn.equals(address.fqdn)) {
            return false;
        }
        return port == address.port;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + fqdn.hashCode();
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
