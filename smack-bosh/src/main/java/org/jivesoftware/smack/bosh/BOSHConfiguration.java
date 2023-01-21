/**
 *
 * Copyright 2009 Jive Software.
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

package org.jivesoftware.smack.bosh;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.proxy.ProxyInfo;

/**
 * Configuration to use while establishing the connection to the XMPP server via
 * HTTP binding.
 *
 * @see XMPPBOSHConnection
 * @author Guenther Niess
 */
public final class BOSHConfiguration extends ConnectionConfiguration {

    private final boolean https;
    private final String file;
    private Map<String, String> httpHeaders;

    private BOSHConfiguration(Builder builder) {
        super(builder);
        if (proxy != null) {
            if (proxy.getProxyType() != ProxyInfo.ProxyType.HTTP) {
                throw new IllegalArgumentException(
                                "Only HTTP proxies are support with BOSH connections");
            }
        }
        https = builder.https;
        if (builder.file.charAt(0) != '/') {
            file = '/' + builder.file;
        } else {
            file = builder.file;
        }
        httpHeaders = builder.httpHeaders;
    }

    public boolean isProxyEnabled() {
        return proxy != null;
    }

    @Override
    public ProxyInfo getProxyInfo() {
        return proxy;
    }

    public String getProxyAddress() {
        return proxy != null ? proxy.getProxyAddress() : null;
    }

    public int getProxyPort() {
        return proxy != null ? proxy.getProxyPort() : 8080;
    }

    public boolean isUsingHTTPS() {
        return https;
    }

    public URI getURI() throws URISyntaxException {
        return new URI((https ? "https://" : "http://") + this.host + ":" + this.port + file);
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ConnectionConfiguration.Builder<Builder, BOSHConfiguration> {
        private boolean https;
        private String file;
        private Map<String, String> httpHeaders = new HashMap<>();

        private Builder() {
        }

        public Builder setUseHttps(boolean useHttps) {
            https = useHttps;
            return this;
        }

        public Builder useHttps() {
            return setUseHttps(true);
        }

        public Builder setFile(String file) {
            this.file = file;
            return this;
        }

        public Builder addHttpHeader(String name, String value) {
            httpHeaders.put(name, value);
            return this;
        }

        @Override
        public BOSHConfiguration build() {
            return new BOSHConfiguration(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
