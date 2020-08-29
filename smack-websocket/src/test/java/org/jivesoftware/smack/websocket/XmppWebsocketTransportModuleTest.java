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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure.HttpLookupFailure;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.WebsocketEndpointsDiscoveryFailed;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpointLookup.Result;

import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class XmppWebsocketTransportModuleTest {
    @Test
    public void createWebsocketModuleConnectionInstanceTest() throws URISyntaxException, XmppStringprepException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration
                .builder();

        builder.removeAllModules();
        builder.addModule(XmppWebsocketTransportModuleDescriptor.class);
        builder.setXmppAddressAndPassword("user5@localhost.org", "user5");
        builder.setHost("localhost.org");

        XmppWebsocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebsocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebsocketEndpointAndDiscovery(new URI("wss://localhost.org:7443/ws/"), false);

        ModularXmppClientToServerConnectionConfiguration config = builder.build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);
        assertNotNull(connection);
    }

    @Test
    public void createDescriptorTest() throws URISyntaxException, XmppStringprepException {
        XmppWebsocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebsocketDescriptor();
        assertNotNull(websocketTransportModuleDescriptor);
    }

    @Test
    public void websocketEndpointDiscoveryTest() throws URISyntaxException {
        XmppWebsocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebsocketDescriptor();
        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        XmppWebsocketTransportModule transportModule
                        = new XmppWebsocketTransportModule(websocketTransportModuleDescriptor, connectionInternal);

        XmppWebsocketTransportModule.XmppWebsocketTransport transport = transportModule.getTransport();

        assertThrows(AssertionError.class, () -> transport.new DiscoveredWebsocketEndpoints(null));
        assertThrows(AssertionError.class, () -> transport.new WebsocketEndpointsDiscoveryFailed(null));

        WebsocketRemoteConnectionEndpoint endpoint = new WebsocketRemoteConnectionEndpoint("wss://localhost.org:7443/ws/");

        List<WebsocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints = new ArrayList<>();
        discoveredRemoteConnectionEndpoints.add(endpoint);

        HttpLookupFailure httpLookupFailure = new RemoteConnectionEndpointLookupFailure.HttpLookupFailure(null, null);
        List<RemoteConnectionEndpointLookupFailure> failureList = new ArrayList<>();
        failureList.add(httpLookupFailure);
        Result result = new Result(discoveredRemoteConnectionEndpoints, failureList);

        DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints = transport.new DiscoveredWebsocketEndpoints(result);
        assertNotNull(discoveredWebsocketEndpoints.getResult());

        WebsocketEndpointsDiscoveryFailed endpointsDiscoveryFailed = transport.new WebsocketEndpointsDiscoveryFailed(result);
        assertNotNull(endpointsDiscoveryFailed.toString());
    }

    @Test
    public void websocketConnectedResultTest() throws URISyntaxException {
        WebsocketRemoteConnectionEndpoint connectedEndpoint = new WebsocketRemoteConnectionEndpoint("wss://localhost.org:7443/ws/");
        assertNotNull(new XmppWebsocketTransportModule.WebsocketConnectedResult(connectedEndpoint));
    }

    @Test
    public void lookupConnectionEndpointsTest() throws URISyntaxException {
        XmppWebsocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebsocketDescriptor();
        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        XmppWebsocketTransportModule transportModule
                        = new XmppWebsocketTransportModule(websocketTransportModuleDescriptor, connectionInternal);

        XmppWebsocketTransportModule.XmppWebsocketTransport transport = transportModule.getTransport();
        assertNotNull(transport.lookupConnectionEndpoints());

    }

    private static XmppWebsocketTransportModuleDescriptor getWebsocketDescriptor() throws URISyntaxException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration
                .builder();

        XmppWebsocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebsocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebsocketEndpointAndDiscovery(new URI("wss://localhost.org:7443/ws/"), false);
        return (XmppWebsocketTransportModuleDescriptor) websocketBuilder.build();
    }
}
