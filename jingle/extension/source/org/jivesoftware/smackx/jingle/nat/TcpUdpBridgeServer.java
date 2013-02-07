/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingle.nat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.jivesoftware.smackx.jingle.SmackLogger;

/**
 * A Simple and Experimental Bridge.
 * It Creates a TCP Socket Listeners for Connections and forwards every packets received to an UDP Listener.
 * And forwards every packets received in UDP Socket, to the TCP Client
 */
public class TcpUdpBridgeServer {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(TcpUdpBridgeServer.class);

	private String remoteTcpHost = null;
    private String remoteUdpHost = null;
    private int remoteTcpPort = -1;
    private int remoteUdpPort = -1;
    private int localUdpPort = -1;

    private DatagramSocket localUdpSocket;
    private Socket localTcpSocket;
    private ServerSocket serverTcpSocket;

    public TcpUdpBridgeServer(String remoteTcpHost, String remoteUdpHost, int remoteTcpPort, int remoteUdpPort) {
        this.remoteTcpHost = remoteTcpHost;
        this.remoteUdpHost = remoteUdpHost;
        this.remoteTcpPort = remoteTcpPort;
        this.remoteUdpPort = remoteUdpPort;

        try {
            serverTcpSocket = new ServerSocket(remoteTcpPort);
            localUdpSocket = new DatagramSocket(0);
            localUdpPort = localUdpSocket.getLocalPort();
            LOGGER.debug("UDP: " + localUdpSocket.getLocalPort());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        startBridge();
    }

    public void startBridge() {

        final Thread process = new Thread(new Runnable() {

            public void run() {
                try {
                    OutputStream out = localTcpSocket.getOutputStream();

                    while (true) {

                        byte b[] = new byte[500];
                        DatagramPacket p = new DatagramPacket(b, 500);

                        localUdpSocket.receive(p);
                        if (p.getLength() == 0) continue;

                        LOGGER.debug("UDP Server Received and Sending to TCP Client:" + new String(p.getData(), 0, p.getLength(), "UTF-8"));

                        out.write(p.getData(), 0, p.getLength());
                        out.flush();
                        LOGGER.debug("Server Flush");
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        new Thread(new Runnable() {

            public void run() {
                try {

                    localTcpSocket = serverTcpSocket.accept();
                    process.start();
                    InputStream in = localTcpSocket.getInputStream();
                    InetAddress remoteHost = InetAddress.getByName(remoteUdpHost);

                    while (true) {
                        byte b[] = new byte[500];

                        int s = in.read(b);
                        //if (s == -1) continue;

                        LOGGER.debug("TCP Server:" + new String(b, 0, s, "UTF-8"));

                        DatagramPacket udpPacket = new DatagramPacket(b, s);

                        udpPacket.setAddress(remoteHost);
                        udpPacket.setPort(remoteUdpPort);

                        localUdpSocket.send(udpPacket);

                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    public Socket getLocalTcpSocket() {
        return localTcpSocket;
    }

    public DatagramSocket getLocalUdpSocket() {
        return localUdpSocket;
    }
}
