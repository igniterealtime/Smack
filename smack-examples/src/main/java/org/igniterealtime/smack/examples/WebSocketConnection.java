/**
 *
 * Copyright 2021 Florian Schmaus
 *
 * This file is part of smack-examples.
 *
 * smack-examples is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.examples;

import java.io.IOException;
import java.net.URISyntaxException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModuleDescriptor;

public class WebSocketConnection {

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

        TLSUtils.setDefaultTrustStoreTypeToJksIfRequired();

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
               ;

        XmppWebSocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebSocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebSocketEndpointAndDiscovery(websocketEndpoint, false);
        builder.addModule(websocketBuilder.build());

        ModularXmppClientToServerConnectionConfiguration config = builder.build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);

        connection.setReplyTimeout(5 * 60 * 1000);

        XmppTools.modularConnectionTest(connection, messageTo);
    }
}
