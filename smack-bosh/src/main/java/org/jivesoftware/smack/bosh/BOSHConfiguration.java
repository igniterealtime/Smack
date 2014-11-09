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

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.proxy.ProxyInfo;

/**
 * Configuration to use while establishing the connection to the XMPP server via
 * HTTP binding.
 * 
 * @see XMPPBOSHConnection
 * @author Guenther Niess
 */
public class BOSHConfiguration extends ConnectionConfiguration {

    private final boolean https;
    private final String file;

    private BOSHConfiguration(BOSHConfigurationBuilder builder) {
        super(builder);
        https = builder.https;
        if (builder.file.charAt(0) != '/') {
            file = '/' + builder.file;
        } else {
            file = builder.file;
        }
    }

    public boolean isProxyEnabled() {
        return (proxy != null && proxy.getProxyType() != ProxyInfo.ProxyType.NONE);
    }

    public ProxyInfo getProxyInfo() {
        return proxy;
    }

    public String getProxyAddress() {
        return (proxy != null ? proxy.getProxyAddress() : null);
    }

    public int getProxyPort() {
        return (proxy != null ? proxy.getProxyPort() : 8080);
    }

    public boolean isUsingHTTPS() {
        return https;
    }

    public URI getURI() throws URISyntaxException {
        return new URI((https ? "https://" : "http://") + this.host + ":" + this.port + file);
    }

    public static BOSHConfigurationBuilder builder() {
        return new BOSHConfigurationBuilder();
    }

    public static class BOSHConfigurationBuilder extends ConnectionConfigurationBuilder<BOSHConfigurationBuilder, BOSHConfiguration> {
        private boolean https;
        private String file;

        private BOSHConfigurationBuilder() {
        }

        public BOSHConfigurationBuilder setUseHttps(boolean useHttps) {
            https = useHttps;
            return this;
        }

        public BOSHConfigurationBuilder useHttps() {
            return setUseHttps(true);
        }

        public BOSHConfigurationBuilder setFile(String file) {
            this.file = file;
            return this;
        }

        @Override
        public BOSHConfiguration build() {
            return new BOSHConfiguration(this);
        }

        @Override
        protected BOSHConfigurationBuilder getThis() {
            return this;
        }
    }
}
