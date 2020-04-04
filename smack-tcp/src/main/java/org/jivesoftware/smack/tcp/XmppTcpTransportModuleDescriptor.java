/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.tcp.XmppTcpTransportModule.EstablishTlsStateDescriptor;
import org.jivesoftware.smack.tcp.XmppTcpTransportModule.EstablishingTcpConnectionStateDescriptor;

public class XmppTcpTransportModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    private final boolean startTls;
    private final boolean directTls;

    public XmppTcpTransportModuleDescriptor(Builder builder) {
        startTls = builder.startTls;
        directTls = builder.directTls;
    }

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        Set<Class<? extends StateDescriptor>> res = new HashSet<>();
        res.add(EstablishingTcpConnectionStateDescriptor.class);
        if (startTls) {
            res.add(EstablishTlsStateDescriptor.class);
        }
        if (directTls) {
            // TODO: Add direct TLS.
            throw new IllegalArgumentException("DirectTLS is not implemented yet");
        }
        return res;
    }

    @Override
    protected XmppTcpTransportModule constructXmppConnectionModule(ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new XmppTcpTransportModule(this, connectionInternal);
    }

    public boolean isStartTlsEnabled() {
        return startTls;
    }

    public boolean isDirectTlsEnabled() {
        return directTls;
    }

    public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        private Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        private boolean startTls = true;

        private boolean directTls = false;

        public Builder disableDirectTls() {
            directTls = false;
            return this;
        }

        public Builder disableStartTls() {
            startTls = false;
            return this;
        }

        @Override
        protected XmppTcpTransportModuleDescriptor build() {
            return new XmppTcpTransportModuleDescriptor(this);
        }
    }
}
