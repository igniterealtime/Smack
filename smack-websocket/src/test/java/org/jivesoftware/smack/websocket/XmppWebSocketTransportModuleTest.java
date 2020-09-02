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
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModule.XmppWebSocketTransport.WebSocketEndpointsDiscoveryFailed;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup.Result;

import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class XmppWebSocketTransportModuleTest {
    @Test
    public void createWebSocketModuleConnectionInstanceTest() throws URISyntaxException, XmppStringprepException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration
                .builder();

        builder.removeAllModules();
        builder.addModule(XmppWebSocketTransportModuleDescriptor.class);
        builder.setXmppAddressAndPassword("user5@localhost.org", "user5");
        builder.setHost("localhost.org");

        XmppWebSocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebSocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebSocketEndpointAndDiscovery(new URI("wss://localhost.org:7443/ws/"), false);

        ModularXmppClientToServerConnectionConfiguration config = builder.build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);
        assertNotNull(connection);
    }

    @Test
    public void createDescriptorTest() throws URISyntaxException, XmppStringprepException {
        XmppWebSocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebSocketDescriptor();
        assertNotNull(websocketTransportModuleDescriptor);
    }

    @Test
    public void websocketEndpointDiscoveryTest() throws URISyntaxException {
        XmppWebSocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebSocketDescriptor();
        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        XmppWebSocketTransportModule transportModule
                        = new XmppWebSocketTransportModule(websocketTransportModuleDescriptor, connectionInternal);

        XmppWebSocketTransportModule.XmppWebSocketTransport transport = transportModule.getTransport();

        assertThrows(AssertionError.class, () -> transport.new DiscoveredWebSocketEndpoints(null));
        assertThrows(AssertionError.class, () -> transport.new WebSocketEndpointsDiscoveryFailed(null));

        WebSocketRemoteConnectionEndpoint endpoint = new WebSocketRemoteConnectionEndpoint("wss://localhost.org:7443/ws/");

        List<WebSocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints = new ArrayList<>();
        discoveredRemoteConnectionEndpoints.add(endpoint);

        HttpLookupFailure httpLookupFailure = new RemoteConnectionEndpointLookupFailure.HttpLookupFailure(null, null);
        List<RemoteConnectionEndpointLookupFailure> failureList = new ArrayList<>();
        failureList.add(httpLookupFailure);
        Result result = new Result(discoveredRemoteConnectionEndpoints, failureList);

        DiscoveredWebSocketEndpoints discoveredWebSocketEndpoints = transport.new DiscoveredWebSocketEndpoints(result);
        assertNotNull(discoveredWebSocketEndpoints.getResult());

        WebSocketEndpointsDiscoveryFailed endpointsDiscoveryFailed = transport.new WebSocketEndpointsDiscoveryFailed(result);
        assertNotNull(endpointsDiscoveryFailed.toString());
    }

    @Test
    public void websocketConnectedResultTest() throws URISyntaxException {
        WebSocketRemoteConnectionEndpoint connectedEndpoint = new WebSocketRemoteConnectionEndpoint("wss://localhost.org:7443/ws/");
        assertNotNull(new XmppWebSocketTransportModule.WebSocketConnectedResult(connectedEndpoint));
    }

    @Test
    public void lookupConnectionEndpointsTest() throws URISyntaxException {
        XmppWebSocketTransportModuleDescriptor websocketTransportModuleDescriptor = getWebSocketDescriptor();
        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        XmppWebSocketTransportModule transportModule
                        = new XmppWebSocketTransportModule(websocketTransportModuleDescriptor, connectionInternal);

        XmppWebSocketTransportModule.XmppWebSocketTransport transport = transportModule.getTransport();
        assertNotNull(transport.lookupConnectionEndpoints());

    }

    private static XmppWebSocketTransportModuleDescriptor getWebSocketDescriptor() throws URISyntaxException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration
                .builder();

        XmppWebSocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebSocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebSocketEndpointAndDiscovery(new URI("wss://localhost.org:7443/ws/"), false);
        return (XmppWebSocketTransportModuleDescriptor) websocketBuilder.build();
    }
}
