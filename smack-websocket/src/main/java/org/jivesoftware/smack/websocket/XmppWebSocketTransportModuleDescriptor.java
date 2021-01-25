/**
 *
 * Copyright 2020 Aditya Borikar, 2020-2021 Florian Schmaus
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

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModule.EstablishingWebSocketConnectionStateDescriptor;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

/**
 * The descriptor class for {@link XmppWebSocketTransportModule}.
 * <br>
 * To add {@link XmppWebSocketTransportModule} to {@link ModularXmppClientToServerConnection},
 * use {@link ModularXmppClientToServerConnectionConfiguration.Builder#addModule(ModularXmppClientToServerConnectionModuleDescriptor)}.
 */
public final class XmppWebSocketTransportModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {
    private final boolean performWebSocketEndpointDiscovery;
    private final boolean implicitWebSocketEndpoint;
    private final URI uri;
    private final WebSocketRemoteConnectionEndpoint wsRce;

    public XmppWebSocketTransportModuleDescriptor(Builder builder) {
        this.performWebSocketEndpointDiscovery = builder.performWebSocketEndpointDiscovery;
        this.implicitWebSocketEndpoint = builder.implicitWebSocketEndpoint;

        this.uri = builder.uri;
        if (uri != null) {
            wsRce = WebSocketRemoteConnectionEndpoint.from(uri);
        } else {
            wsRce = null;
        }
    }

    @Override
    @SuppressWarnings({"incomplete-switch", "MissingCasesInEnumSwitch"})
    protected void validateConfiguration(ModularXmppClientToServerConnectionConfiguration configuration) {
        if (wsRce == null) {
            return;
        }

        SecurityMode securityMode = configuration.getSecurityMode();
        switch (securityMode) {
        case required:
            if (!wsRce.isSecureEndpoint()) {
                throw new IllegalArgumentException("The provided WebSocket endpoint " + wsRce + " is not a secure endpoint, but the connection configuration requires secure endpoints");
            }
            break;
        case disabled:
            if (wsRce.isSecureEndpoint()) {
                throw new IllegalArgumentException("The provided WebSocket endpoint " + wsRce + " is a secure endpoint, but the connection configuration has security disabled");
            }
            break;
        }
    }

    /**
     * Returns true if websocket endpoint discovery is true, returns false otherwise.
     * @return boolean
     */
    public boolean isWebSocketEndpointDiscoveryEnabled() {
        return performWebSocketEndpointDiscovery;
    }

    public boolean isImplicitWebSocketEndpointEnabled() {
        return implicitWebSocketEndpoint;
    }

    /**
     * Returns explicitly configured websocket endpoint uri.
     * @return uri
     */
    public URI getExplicitlyProvidedUri() {
        return uri;
    }

    WebSocketRemoteConnectionEndpoint getExplicitlyProvidedEndpoint() {
        return wsRce;
    }

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        Set<Class<? extends StateDescriptor>> res = new HashSet<>();
        res.add(EstablishingWebSocketConnectionStateDescriptor.class);
        return res;
    }

    @Override
    protected ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor> constructXmppConnectionModule(
            ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new XmppWebSocketTransportModule(this, connectionInternal);
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
     * Builder class for {@link XmppWebSocketTransportModuleDescriptor}.
     * <br>
     * To obtain an instance of {@link XmppWebSocketTransportModuleDescriptor.Builder}, use {@link XmppWebSocketTransportModuleDescriptor#getBuilder(ModularXmppClientToServerConnectionConfiguration.Builder)} method.
     * <br>
     * Use {@link Builder#explicitlySetWebSocketEndpoint(URI)} to configure the URI of an endpoint as a backup in case connection couldn't be established with endpoints through http lookup.
     * <br>
     * Use {@link Builder#explicitlySetWebSocketEndpointAndDiscovery(URI, boolean)} to configure endpoint and disallow websocket endpoint discovery through http lookup.
     * By default, {@link Builder#performWebSocketEndpointDiscovery} is set to true.
     * <br>
     * Use {@link Builder#build()} to obtain {@link XmppWebSocketTransportModuleDescriptor}.
     */
    public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {
        private boolean performWebSocketEndpointDiscovery = true;
        private boolean implicitWebSocketEndpoint = true;
        private URI uri;

        private Builder(
                ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        public Builder explicitlySetWebSocketEndpoint(URI endpoint) {
            return explicitlySetWebSocketEndpointAndDiscovery(endpoint, true);
        }

        public Builder explicitlySetWebSocketEndpointAndDiscovery(URI endpoint, boolean performWebSocketEndpointDiscovery) {
            Objects.requireNonNull(endpoint, "Provided endpoint URI must not be null");
            this.uri = endpoint;
            this.performWebSocketEndpointDiscovery = performWebSocketEndpointDiscovery;
            return this;
        }

        public Builder explicitlySetWebSocketEndpoint(CharSequence endpoint) throws URISyntaxException {
            URI endpointUri = new URI(endpoint.toString());
            return explicitlySetWebSocketEndpoint(endpointUri);
        }

        public Builder explicitlySetWebSocketEndpointAndDiscovery(CharSequence endpoint, boolean performWebSocketEndpointDiscovery)
                throws URISyntaxException {
            URI endpointUri = new URI(endpoint.toString());
            return explicitlySetWebSocketEndpointAndDiscovery(endpointUri, performWebSocketEndpointDiscovery);
        }

        public Builder disableImplicitWebsocketEndpoint() {
            implicitWebSocketEndpoint = false;
            return this;
        }

        @Override
        public ModularXmppClientToServerConnectionModuleDescriptor build() {
            return new XmppWebSocketTransportModuleDescriptor(this);
        }
    }
}
