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

import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;

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
