/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.tcp;

import org.jivesoftware.smack.ConnectionConfiguration;

public class XMPPTCPConnectionConfiguration extends ConnectionConfiguration {

    private final boolean compressionEnabled;

    private XMPPTCPConnectionConfiguration(XMPPTCPConnectionConfigurationBuilder builder) {
        super(builder);
        compressionEnabled = builder.compressionEnabled;
    }

    /**
     * Returns true if the connection is going to use stream compression. Stream compression
     * will be requested after TLS was established (if TLS was enabled) and only if the server
     * offered stream compression. With stream compression network traffic can be reduced
     * up to 90%. By default compression is disabled.
     *
     * @return true if the connection is going to use stream compression.
     */
    @Override
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public static XMPPTCPConnectionConfigurationBuilder builder() {
        return new XMPPTCPConnectionConfigurationBuilder();
    }

    public static class XMPPTCPConnectionConfigurationBuilder extends ConnectionConfigurationBuilder<XMPPTCPConnectionConfigurationBuilder, XMPPTCPConnectionConfiguration> {
        private boolean compressionEnabled = false;

        private XMPPTCPConnectionConfigurationBuilder() {
        }

        /**
         * Sets if the connection is going to use stream compression. Stream compression
         * will be requested after TLS was established (if TLS was enabled) and only if the server
         * offered stream compression. With stream compression network traffic can be reduced
         * up to 90%. By default compression is disabled.
         *
         * @param compressionEnabled if the connection is going to use stream compression.
         */
        public XMPPTCPConnectionConfigurationBuilder setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
            return this;
        }

        @Override
        protected XMPPTCPConnectionConfigurationBuilder getThis() {
            return this;
        }

        @Override
        public XMPPTCPConnectionConfiguration build() {
            return new XMPPTCPConnectionConfiguration(this);
        }
    }
}
