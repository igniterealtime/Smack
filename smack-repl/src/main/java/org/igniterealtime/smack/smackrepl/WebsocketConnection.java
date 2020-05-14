/**
 *
 * Copyright 2020 Aditya Borikar
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
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
package org.igniterealtime.smack.smackrepl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModuleDescriptor;

public class WebsocketConnection {

    public static void main(String[] args) throws SmackException, IOException, XMPPException, InterruptedException, URISyntaxException {
        ModularXmppClientToServerConnectionConfiguration.Builder builder = ModularXmppClientToServerConnectionConfiguration.builder();
        builder.removeAllModules();
        builder.setXmppAddressAndPassword(args[0], args[1]);

        // Set a fallback uri into websocket transport descriptor and add this descriptor into connection builder.
        XmppWebsocketTransportModuleDescriptor.Builder websocketBuilder = XmppWebsocketTransportModuleDescriptor.getBuilder(builder);
        websocketBuilder.explicitlySetWebsocketEndpointAndDiscovery(new URI(args[2]), false);
        builder.addModule(websocketBuilder.build());

        ModularXmppClientToServerConnectionConfiguration config = builder.build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);

        connection.connect();
        connection.login();
        connection.disconnect();
    }
}
