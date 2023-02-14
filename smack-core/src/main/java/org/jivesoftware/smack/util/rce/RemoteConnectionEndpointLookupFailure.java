/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smack.util.rce;

import org.jivesoftware.smack.util.ToStringUtil;

import org.jxmpp.jid.DomainBareJid;
import org.minidns.dnsname.DnsName;

public abstract class RemoteConnectionEndpointLookupFailure {

    private final String description;
    private final Exception exception;

    public RemoteConnectionEndpointLookupFailure(String description, Exception exception) {
        this.description = description;
        this.exception = exception;
    }

    public final String getDescription() {
        return description;
    }

    public final Exception getException() {
        return exception;
    }

    public String getErrorMessage() {
        return description + " because: " + exception;
    }

    private transient String toStringCache;

    @Override
    public String toString() {
        if (toStringCache == null) {
            toStringCache = ToStringUtil.builderFor(RemoteConnectionEndpointLookupFailure.class)
                .addValue("description", description)
                .addValue("exception", exception)
                .build();
        }
        return toStringCache;
    }

    public static class DnsLookupFailure extends RemoteConnectionEndpointLookupFailure {
        private final DnsName dnsName;

        public DnsLookupFailure(DnsName dnsName, Exception exception) {
            super("DNS lookup exception for " + dnsName, exception);
            this.dnsName = dnsName;
        }

        public DnsName getDnsName() {
            return dnsName;
        }
    }

    public static class HttpLookupFailure extends RemoteConnectionEndpointLookupFailure {
        private final DomainBareJid host;

        public HttpLookupFailure(DomainBareJid host, Exception exception) {
            super("Http lookup exception for " + host, exception);
            this.host = host;
        }

        public DomainBareJid getHost() {
            return host;
        }
    }
}
