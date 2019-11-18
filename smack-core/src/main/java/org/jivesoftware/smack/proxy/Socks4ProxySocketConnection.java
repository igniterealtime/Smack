/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.util.OutputStreamUtil;

/**
 * Socket factory for socks4 proxy.
 *
 * @author Atul Aggarwal
 */
public class Socks4ProxySocketConnection implements ProxySocketConnection {
    private final ProxyInfo proxy;

    Socks4ProxySocketConnection(ProxyInfo proxy) {
        this.proxy = proxy;
    }

    @Override
    public void connect(Socket socket, String host, int port, int timeout)
                    throws IOException {
        String proxy_host = proxy.getProxyAddress();
        int proxy_port = proxy.getProxyPort();
        String user = proxy.getProxyUsername();

        socket.connect(new InetSocketAddress(proxy_host, proxy_port), timeout);
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        OutputStream out = socket.getOutputStream();

        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        byte[] inBuf;

    /*
    1) CONNECT

    The client connects to the SOCKS server and sends a CONNECT request when
    it wants to establish a connection to an application server. The client
    includes in the request packet the IP address and the port number of the
    destination host, and userid, in the following format.

           +----+----+----+----+----+----+----+----+----+----+....+----+
           | VN | CD | DSTPORT |      DSTIP        | USERID       |NULL|
           +----+----+----+----+----+----+----+----+----+----+....+----+
    # of bytes:   1    1      2              4           variable       1

    VN is the SOCKS protocol version number and should be 4. CD is the
    SOCKS command code and should be 1 for CONNECT request. NULL is a byte
    of all zero bits.
    */

        outBuf.write(4);
        outBuf.write(1);

        outBuf.write(port >>> 8);
        outBuf.write(port & 0xff);

        InetAddress inetAddress = InetAddress.getByName(proxy_host);
        byte[] byteAddress = inetAddress.getAddress();
        outBuf.write(byteAddress);

        if (user != null) {
            byte[] userBytes = user.getBytes(StandardCharsets.UTF_8);
            outBuf.write(userBytes);
        }
        outBuf.write(0);
        OutputStreamUtil.writeResetAndFlush(outBuf, out);

    /*
    The SOCKS server checks to see whether such a request should be granted
    based on any combination of source IP address, destination IP address,
    destination port number, the userid, and information it may obtain by
    consulting IDENT, cf. RFC 1413.  If the request is granted, the SOCKS
    server makes a connection to the specified port of the destination host.
    A reply packet is sent to the client when this connection is established,
    or when the request is rejected or the operation fails.

           +----+----+----+----+----+----+----+----+
           | VN | CD | DSTPORT |      DSTIP        |
           +----+----+----+----+----+----+----+----+
    # of bytes:   1    1      2              4

    VN is the version of the reply code and should be 0. CD is the result
    code with one of the following values:

    90: request granted
    91: request rejected or failed
    92: request rejected because SOCKS server cannot connect to
    identd on the client
    93: request rejected because the client program and identd
    report different user-ids

    The remaining fields are ignored.
    */

        inBuf = new byte[6];
        dis.readFully(inBuf);
        if (inBuf[0] != 0) {
            throw new ProxyException(ProxyInfo.ProxyType.SOCKS4,
                "server returns VN " + inBuf[0]);
        }
        if (inBuf[1] != 90) {
            String message = "ProxySOCKS4: server returns CD " + inBuf[1];
            throw new ProxyException(ProxyInfo.ProxyType.SOCKS4, message);
        }
        inBuf = new byte[2];
        dis.readFully(inBuf);
    }
}
