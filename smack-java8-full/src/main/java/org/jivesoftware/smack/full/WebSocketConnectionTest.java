/**
 *
 * Copyright 2021 Florian Schmaus.
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
package org.jivesoftware.smack.full;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModuleDescriptor;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.util.XmppDateTime;

public class WebSocketConnectionTest {

    static {
        SmackConfiguration.DEBUG = true;
    }

    public static void main(String[] args)
                    throws URISyntaxException, SmackException, IOException, XMPPException, InterruptedException {
        String jid, password, websocketEndpoint, messageTo = null;
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException();
        }

        jid = args[0];
        password = args[1];
        websocketEndpoint = args[2];
        if (args.length >= 4) {
            messageTo = args[3];
        }

        testWebSocketConnection(jid, password, websocketEndpoint, messageTo);
    }

    public static void testWebSocketConnection(String jid, String password, String websocketEndpoint)
                    throws URISyntaxException, SmackException, IOException, XMPPException, InterruptedException {
        testWebSocketConnection(jid, password, websocketEndpoint, null);
    }

    public static void testWebSocketConnection(String jid, String password, String websocketEndpoint, String messageTo)
                    throws URISyntaxException, SmackException, IOException, XMPPException, InterruptedException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration.builder();
        builder.removeAllModules()
               .setXmppAddressAndPassword(jid, password)
               .setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE)
               ;

        XmppWebSocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebSocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebSocketEndpointAndDiscovery(websocketEndpoint, false);
        builder.addModule(websocketBuilder.build());

        ModularXmppClientToServerConnectionConfiguration config = builder.build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);

        connection.setReplyTimeout(5 * 60 * 1000);

        connection.addConnectionStateMachineListener((event, c) -> {
            Logger.getAnonymousLogger().info("Connection event: " + event);
        });

        connection.connect();

        connection.login();

        if (messageTo != null) {
            Message message = connection.getStanzaFactory().buildMessageStanza()
                    .to(messageTo)
                    .setBody("It is alive! " + XmppDateTime.formatXEP0082Date(new Date()))
                    .build()
                    ;
            connection.sendStanza(message);
        }

        Thread.sleep(1000);

        connection.disconnect();

        ModularXmppClientToServerConnection.Stats connectionStats = connection.getStats();
        ServiceDiscoveryManager.Stats serviceDiscoveryManagerStats = ServiceDiscoveryManager.getInstanceFor(connection).getStats();

        // CHECKSTYLE:OFF
        System.out.println("WebSocket successfully finished, yeah!\n" + connectionStats + '\n' + serviceDiscoveryManagerStats);
        // CHECKSTYLE:ON
    }
}
