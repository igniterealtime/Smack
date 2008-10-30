/**
 * $RCSfile: TcpUdpBridgeClient.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
package org.jivesoftware.smackx.jingle.nat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.jivesoftware.smackx.jingle.SmackLogger;

/**
 * A Simple and Experimental Bridge.
 * It Creates a TCP Socket That Connects to another TCP Socket Listener and forwards every packets received to an UDP Listener.
 * And forwards every packets received in UDP Socket, to the TCP Server
 */
public class TcpUdpBridgeClient {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(TcpUdpBridgeClient.class);

	private String remoteTcpHost = null;
    private String remoteUdpHost = null;
    private int remoteTcpPort = -1;
    private int remoteUdpPort = -1;
    private int localUdpPort = -1;

    private DatagramSocket localUdpSocket;
    private Socket localTcpSocket;

    public TcpUdpBridgeClient(String remoteTcpHost, String remoteUdpHost, int remoteTcpPort, int remoteUdpPort) {
        this.remoteTcpHost = remoteTcpHost;
        this.remoteUdpHost = remoteUdpHost;
        this.remoteTcpPort = remoteTcpPort;
        this.remoteUdpPort = remoteUdpPort;

        try {
            localTcpSocket = new Socket(remoteTcpHost, remoteTcpPort);
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

                        LOGGER.debug("UDP Client Received and Sending to TCP Server:"+new String(p.getData(),0,p.getLength(),"UTF-8"));

                        out.write(p.getData(), 0, p.getLength());
                        out.flush();
                        LOGGER.debug("Client Flush");

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
                                       
                    InputStream in = localTcpSocket.getInputStream();
                    InetAddress remoteHost = InetAddress.getByName(remoteUdpHost);
                    process.start();                    

                    while (true) {
                        byte b[] = new byte[500];

                        int s = in.read(b);
                        //if (s == -1) continue;

                        LOGGER.debug("TCP Client:" +new String(b,0,s,"UTF-8"));

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
