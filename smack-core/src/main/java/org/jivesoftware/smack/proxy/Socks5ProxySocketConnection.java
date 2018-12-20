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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Socket factory for Socks5 proxy.
 *
 * @author Atul Aggarwal
 */
public class Socks5ProxySocketConnection implements ProxySocketConnection {
    private static final Logger LOGGER = Logger.getLogger(Socks5ProxySocketConnection.class.getName());

    private final ProxyInfo proxy;

    Socks5ProxySocketConnection(ProxyInfo proxy) {
        this.proxy = proxy;
    }

    @Override
    public void connect(Socket socket, String host, int port, int timeout)
                    throws IOException {
        InputStream in = null;
        OutputStream out = null;
        String proxy_host = proxy.getProxyAddress();
        int proxy_port = proxy.getProxyPort();
        String user = proxy.getProxyUsername();
        String passwd = proxy.getProxyPassword();

        try {
            socket.connect(new InetSocketAddress(proxy_host, proxy_port), timeout);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            socket.setTcpNoDelay(true);

            byte[] buf = new byte[1024];
            int index = 0;

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

            buf[index++] = 5;

            buf[index++] = 2;
            buf[index++] = 0;           // NO AUTHENTICATION REQUIRED
            buf[index++] = 2;           // USERNAME/PASSWORD

            out.write(buf, 0, index);

/*
    The server selects from one of the methods given in METHODS, and
    sends a METHOD selection message:

                         +----+--------+
                         |VER | METHOD |
                         +----+--------+
                         | 1  |   1    |
                         +----+--------+
*/
            fill(in, buf, 2);

            boolean check = false;
            switch (buf[1] & 0xff) {
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
                    index = 0;
                    buf[index++] = 1;
                    buf[index++] = (byte) user.length();
                    byte[] userBytes = user.getBytes(StringUtils.UTF8);
                    System.arraycopy(userBytes, 0, buf, index,
                        user.length());
                    index += user.length();
                    byte[] passwordBytes = passwd.getBytes(StringUtils.UTF8);
                    buf[index++] = (byte) passwordBytes.length;
                    System.arraycopy(passwordBytes, 0, buf, index,
                        passwd.length());
                    index += passwd.length();

                    out.write(buf, 0, index);

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
                    fill(in, buf, 2);
                    if (buf[1] == 0) {
                        check = true;
                    }
                    break;
                default:
            }

            if (!check) {
                CloseableUtil.maybeClose(socket, LOGGER);
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

            index = 0;
            buf[index++] = 5;
            buf[index++] = 1;       // CONNECT
            buf[index++] = 0;

            byte[] hostb = host.getBytes(StringUtils.UTF8);
            int len = hostb.length;
            buf[index++] = 3;      // DOMAINNAME
            buf[index++] = (byte) len;
            System.arraycopy(hostb, 0, buf, index, len);
            index += len;
            buf[index++] = (byte) (port >>> 8);
            buf[index++] = (byte) (port & 0xff);

            out.write(buf, 0, index);

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

            fill(in, buf, 4);

            if (buf[1] != 0) {
                CloseableUtil.maybeClose(socket, LOGGER);
                throw new ProxyException(ProxyInfo.ProxyType.SOCKS5,
                    "server returns " + buf[1]);
            }

            switch (buf[3] & 0xff) {
                case 1:
                    fill(in, buf, 6);
                    break;
                case 3:
                    fill(in, buf, 1);
                    fill(in, buf, (buf[0] & 0xff) + 2);
                    break;
                case 4:
                    fill(in, buf, 18);
                    break;
                default:
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            CloseableUtil.maybeClose(socket, LOGGER);
            // TODO convert to IOException(e) when minimum Android API level is 9 or higher
            throw new IOException(e.getLocalizedMessage());
        }
    }

    private static void fill(InputStream in, byte[] buf, int len)
      throws IOException {
        int s = 0;
        while (s < len) {
            int i = in.read(buf, s, len - s);
            if (i <= 0) {
                throw new ProxyException(ProxyInfo.ProxyType.SOCKS5, "stream " +
                    "is closed");
            }
            s += i;
        }
    }

}
