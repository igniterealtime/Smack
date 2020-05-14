/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.EstablishingWebsocketConnectionStateDescriptor;

/**
 * The descriptor class for {@link XmppWebsocketTransportModule}.
 * <br>
 * To add {@link XmppWebsocketTransportModule} to {@link ModularXmppClientToServerConnection},
 * use {@link ModularXmppClientToServerConnectionConfiguration.Builder#addModule(ModularXmppClientToServerConnectionModuleDescriptor)}.
 */
public final class XmppWebsocketTransportModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {
    private boolean performWebsocketEndpointDiscovery;
    private URI uri;

    public XmppWebsocketTransportModuleDescriptor(Builder builder) {
        this.performWebsocketEndpointDiscovery = builder.performWebsocketEndpointDiscovery;
        this.uri = builder.uri;
    }

    /**
     * Returns true if websocket endpoint discovery is true, returns false otherwise.
     * @return boolean
     */
    public boolean isWebsocketEndpointDiscoveryEnabled() {
        return performWebsocketEndpointDiscovery;
    }

    /**
     * Returns explicitly configured websocket endpoint uri.
     * @return uri
     */
    public URI getExplicitlyProvidedUri() {
        return uri;
    }

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        Set<Class<? extends StateDescriptor>> res = new HashSet<>();
        res.add(EstablishingWebsocketConnectionStateDescriptor.class);
        return res;
    }

    @Override
    protected ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor> constructXmppConnectionModule(
            ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new XmppWebsocketTransportModule(this, connectionInternal);
    }

    /**
     * Returns a new instance of {@link Builder}.
     * <br>
     * @return Builder
     * @param connectionConfigurationBuilder {@link ModularXmppClientToServerConnectionConfiguration.Builder}.
     */
    public static Builder getBuilder(
            ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
        return new Builder(connectionConfigurationBuilder);
    }

    /**
     * Builder class for {@link XmppWebsocketTransportModuleDescriptor}.
     * <br>
     * To obtain an instance of {@link XmppWebsocketTransportModuleDescriptor.Builder}, use {@link XmppWebsocketTransportModuleDescriptor#getBuilder(ModularXmppClientToServerConnectionConfiguration.Builder)} method.
     * <br>
     * Use {@link Builder#explicitlySetWebsocketEndpoint(URI)} to configure the URI of an endpoint as a backup in case connection couldn't be established with endpoints through http lookup.
     * <br>
     * Use {@link Builder#explicitlySetWebsocketEndpointAndDiscovery(URI, boolean)} to configure endpoint and disallow websocket endpoint discovery through http lookup.
     * By default, {@link Builder#performWebsocketEndpointDiscovery} is set to true.
     * <br>
     * Use {@link Builder#build()} to obtain {@link XmppWebsocketTransportModuleDescriptor}.
     */
    public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {
        private boolean performWebsocketEndpointDiscovery = true;
        private URI uri;

        private Builder(
                ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        public Builder explicitlySetWebsocketEndpoint(URI endpoint) {
            return explicitlySetWebsocketEndpointAndDiscovery(endpoint, true);
        }

        public Builder explicitlySetWebsocketEndpointAndDiscovery(URI endpoint, boolean performWebsocketEndpointDiscovery) {
            Objects.requireNonNull(endpoint, "Provided endpoint URI must not be null");
            this.uri = endpoint;
            this.performWebsocketEndpointDiscovery = performWebsocketEndpointDiscovery;
            return this;
        }

        public Builder explicitlySetWebsocketEndpoint(CharSequence endpoint) throws URISyntaxException {
            URI endpointUri = new URI(endpoint.toString());
            return explicitlySetWebsocketEndpointAndDiscovery(endpointUri, true);
        }

        public Builder explicitlySetWebsocketEndpoint(CharSequence endpoint, boolean performWebsocketEndpointDiscovery)
                throws URISyntaxException {
            URI endpointUri = new URI(endpoint.toString());
            return explicitlySetWebsocketEndpointAndDiscovery(endpointUri, performWebsocketEndpointDiscovery);
        }

        @Override
        public ModularXmppClientToServerConnectionModuleDescriptor build() {
            return new XmppWebsocketTransportModuleDescriptor(this);
        }
    }
}
