/**
 * Copyright 2013 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

public class HostAddress {
    private String fqdn;
    private int port;
    private Exception exception;

    /**
     * Creates a new HostAddress with the given FQDN. The port will be set to the default XMPP client port: 5222
     * 
     * @param fqdn Fully qualified domain name.
     * @throws IllegalArgumentException If the fqdn is null.
     */
    public HostAddress(String fqdn) {
        if (fqdn == null)
            throw new IllegalArgumentException("FQDN is null");
        if (fqdn.charAt(fqdn.length() - 1) == '.') {
            this.fqdn = fqdn.substring(0, fqdn.length() - 1);
        }
        else {
            this.fqdn = fqdn;
        }
        // Set port to the default port for XMPP client communication
        this.port = 5222;
    }

    /**
     * Creates a new HostAddress with the given FQDN. The port will be set to the default XMPP client port: 5222
     * 
     * @param fqdn Fully qualified domain name.
     * @param port The port to connect on.
     * @throws IllegalArgumentException If the fqdn is null or port is out of valid range (0 - 65535).
     */
    public HostAddress(String fqdn, int port) {
        this(fqdn);
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException(
                    "DNS SRV records weight must be a 16-bit unsiged integer (i.e. between 0-65535. Port was: " + port);

        this.port = port;
    }

    public String getFQDN() {
        return fqdn;
    }

    public int getPort() {
        return port;
    }

    public void setException(Exception e) {
        this.exception = e;
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
        String error;
        if (exception == null) {
            error = "No error logged";
        }
        else {
            error = exception.getMessage();
        }
        return toString() + " Exception: " + error;
    }
}
