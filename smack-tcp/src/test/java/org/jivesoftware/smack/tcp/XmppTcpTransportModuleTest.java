/*
 *
 * Copyright 2026 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.jupiter.api.Test;

public class XmppTcpTransportModuleTest extends SmackTestSuite {

    private static final String SERVER_STREAM_OPEN_AND_FEATURES = "<stream:stream xmlns='jabber:client'"
                    + " xmlns:stream='http://etherx.jabber.org/streams' from='example.org' id='test' version='1.0'>"
                    + "<stream:features>"
                    + "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>PLAIN</mechanism></mechanisms>"
                    + "</stream:features>";

    @Test
    public void streamOpenIsWrittenWithOneWriteCall() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            FutureTask<Socket> server = acceptAndOpenStream(serverSocket);
            ModularXmppClientToServerConnection connection = createConnection(serverSocket);
            connection.connect();
            Socket socket = server.get();
            try {
                XmppTcpTransportModule tcpTransportModule = connection.getConnectionModuleFor(
                                XmppTcpTransportModuleDescriptor.class);
                // The stream open is the only top-level element sent so far.
                assertEquals(1, tcpTransportModule.getStats().networkWriteCalls);
            } finally {
                connection.instantShutdown();
                socket.close();
            }
        }
    }

    private static ModularXmppClientToServerConnection createConnection(ServerSocket serverSocket) throws IOException {
        ModularXmppClientToServerConnectionConfiguration configuration = ModularXmppClientToServerConnectionConfiguration.builder()
                        .setXmppAddressAndPassword("alice@example.org", "secret")
                        .setHostAddress(serverSocket.getInetAddress())
                        .setPort(serverSocket.getLocalPort())
                        .setSecurityMode(SecurityMode.disabled)
                        .build();
        return new ModularXmppClientToServerConnection(configuration);
    }

    private static FutureTask<Socket> acceptAndOpenStream(ServerSocket serverSocket) {
        FutureTask<Socket> server = new FutureTask<>(() -> {
            Socket socket = serverSocket.accept();

            // Consume the client's stream open.
            InputStream inputStream = socket.getInputStream();
            int b;
            do {
                b = inputStream.read();
                if (b < 0) {
                    throw new IOException("Unexpected end of stream");
                }
            } while (b != '>');

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(SERVER_STREAM_OPEN_AND_FEATURES.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return socket;
        });
        new Thread(server, "XmppTcpTransportModuleTest Server").start();
        return server;
    }
}
