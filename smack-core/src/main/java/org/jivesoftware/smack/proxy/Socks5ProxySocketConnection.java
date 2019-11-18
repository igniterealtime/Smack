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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.util.OutputStreamUtil;

/**
 * Socket factory for Socks5 proxy.
 *
 * @author Atul Aggarwal
 */
public class Socks5ProxySocketConnection implements ProxySocketConnection {

    private final ProxyInfo proxy;

    Socks5ProxySocketConnection(ProxyInfo proxy) {
        this.proxy = proxy;
    }

    @Override
    public void connect(Socket socket, String host, int port, int timeout)
                    throws IOException {
        String proxy_host = proxy.getProxyAddress();
        int proxy_port = proxy.getProxyPort();
        String user = proxy.getProxyUsername();
        String passwd = proxy.getProxyPassword();

        socket.connect(new InetSocketAddress(proxy_host, proxy_port), timeout);
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        OutputStream out = socket.getOutputStream();

        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        byte[] inBuf;

/*
                   +----+----------+----------+
                   |VER | NMETHODS | METHODS  |
                   +----+----------+----------+
                   | 1  |    1     | 1 to 255 |
                   +----+----------+----------+

   The VER field is set to X'05' for this version of the protocol.  The
   NMETHODS field contains the number of method identifier octets that
   appear in the METHODS field.

   The values currently defined for METHOD are:

          o  X'00' NO AUTHENTICATION REQUIRED
          o  X'01' GSSAPI
          o  X'02' USERNAME/PASSWORD
          o  X'03' to X'7F' IANA ASSIGNED
          o  X'80' to X'FE' RESERVED FOR PRIVATE METHODS
          o  X'FF' NO ACCEPTABLE METHODS
*/

        outBuf.write(5);

        outBuf.write(2);
        outBuf.write(0);           // NO AUTHENTICATION REQUIRED
        outBuf.write(2);           // USERNAME/PASSWORD

        OutputStreamUtil.writeResetAndFlush(outBuf, out);

/*
    The server selects from one of the methods given in METHODS, and
    sends a METHOD selection message:

                         +----+--------+
                         |VER | METHOD |
                         +----+--------+
                         | 1  |   1    |
                         +----+--------+
*/
        inBuf = new byte[2];
        dis.readFully(inBuf);

        boolean check = false;
        switch (inBuf[1] & 0xff) {
            case 0:                // NO AUTHENTICATION REQUIRED
                check = true;
                break;
            case 2:                // USERNAME/PASSWORD
                if (user == null || passwd == null) {
                    break;
                }

/*
   Once the SOCKS V5 server has started, and the client has selected the
   Username/Password Authentication protocol, the Username/Password
   subnegotiation begins.  This begins with the client producing a
   Username/Password request:

           +----+------+----------+------+----------+
           |VER | ULEN |  UNAME   | PLEN |  PASSWD  |
           +----+------+----------+------+----------+
           | 1  |  1   | 1 to 255 |  1   | 1 to 255 |
           +----+------+----------+------+----------+

   The VER field contains the current version of the subnegotiation,
   which is X'01'. The ULEN field contains the length of the UNAME field
   that follows. The UNAME field contains the username as known to the
   source operating system. The PLEN field contains the length of the
   PASSWD field that follows. The PASSWD field contains the password
   association with the given UNAME.
*/
                outBuf.write(1);
                byte[] userBytes = user.getBytes(StandardCharsets.UTF_8);
                OutputStreamUtil.writeByteSafe(outBuf, userBytes.length, "Username to long");
                outBuf.write(userBytes);

                byte[] passwordBytes = passwd.getBytes(StandardCharsets.UTF_8);
                OutputStreamUtil.writeByteSafe(outBuf, passwordBytes.length, "Password to long");
                outBuf.write(passwordBytes);

                OutputStreamUtil.writeResetAndFlush(outBuf, out);

/*
   The server verifies the supplied UNAME and PASSWD, and sends the
   following response:

                        +----+--------+
                        |VER | STATUS |
                        +----+--------+
                        | 1  |   1    |
                        +----+--------+

   A STATUS field of X'00' indicates success. If the server returns a
   `failure' (STATUS value other than X'00') status, it MUST close the
   connection.
*/
                inBuf = new byte[2];
                dis.readFully(inBuf);
                if (inBuf[1] == 0) {
                    check = true;
                }
                break;
            default:
        }

        if (!check) {
            throw new ProxyException(ProxyInfo.ProxyType.SOCKS5,
                "fail in SOCKS5 proxy");
        }

/*
      The SOCKS request is formed as follows:

        +----+-----+-------+------+----------+----------+
        |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+

      Where:

      o  VER    protocol version: X'05'
      o  CMD
         o  CONNECT X'01'
         o  BIND X'02'
         o  UDP ASSOCIATE X'03'
      o  RSV    RESERVED
         o  ATYP   address type of following address
         o  IP V4 address: X'01'
         o  DOMAINNAME: X'03'
         o  IP V6 address: X'04'
      o  DST.ADDR       desired destination address
      o  DST.PORT desired destination port in network octet
         order
*/

        outBuf.write(5);
        outBuf.write(1);       // CONNECT
        outBuf.write(0);

        byte[] hostb = host.getBytes(StandardCharsets.UTF_8);
        int len = hostb.length;
        outBuf.write(3);      // DOMAINNAME
        OutputStreamUtil.writeByteSafe(outBuf, len, "Hostname too long");
        outBuf.write(hostb);
        outBuf.write(port >>> 8);
        outBuf.write(port & 0xff);

        OutputStreamUtil.writeResetAndFlush(outBuf, out);

/*
   The SOCKS request information is sent by the client as soon as it has
   established a connection to the SOCKS server, and completed the
   authentication negotiations.  The server evaluates the request, and
   returns a reply formed as follows:

        +----+-----+-------+------+----------+----------+
        |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+

   Where:

   o  VER    protocol version: X'05'
   o  REP    Reply field:
      o  X'00' succeeded
      o  X'01' general SOCKS server failure
      o  X'02' connection not allowed by ruleset
      o  X'03' Network unreachable
      o  X'04' Host unreachable
      o  X'05' XMPPConnection refused
      o  X'06' TTL expired
      o  X'07' Command not supported
      o  X'08' Address type not supported
      o  X'09' to X'FF' unassigned
    o  RSV    RESERVED
    o  ATYP   address type of following address
      o  IP V4 address: X'01'
      o  DOMAINNAME: X'03'
      o  IP V6 address: X'04'
    o  BND.ADDR       server bound address
    o  BND.PORT       server bound port in network octet order
*/

        inBuf = new byte[4];
        dis.readFully(inBuf);

        if (inBuf[1] != 0) {
            throw new ProxyException(ProxyInfo.ProxyType.SOCKS5,
                "server returns " + inBuf[1]);
        }

        final int addressBytes;
        // TODO: Use Byte.toUnsignedInt() once Smack's minimum Android SDK level is 26 or higher.
        final int atyp = inBuf[3] & 0xff;
        switch (atyp) {
            case 1:
                addressBytes = 4;
                break;
            case 3:
                byte domainnameLengthByte = dis.readByte();
                // TODO: Use Byte.toUnsignedInt() once Smack's minimum Android SDK level is 26 or higher.
                addressBytes = domainnameLengthByte & 0xff;
                break;
            case 4:
                addressBytes = 16;
                break;
            default:
                throw new IOException("Unknown ATYP value: " + atyp);
        }
        inBuf = new byte[addressBytes + 2];
        dis.readFully(inBuf);
    }
}
