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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * HTTP Proxy Socket Connection which connects the socket using a HTTP Proxy.
 *
 * @author Atul Aggarwal
 */
class HTTPProxySocketConnection implements ProxySocketConnection {

    private final ProxyInfo proxy;

    HTTPProxySocketConnection(ProxyInfo proxy) {
        this.proxy = proxy;
    }

    @Override
    public void connect(Socket socket, String host, int port, int timeout)
                    throws IOException {
        String proxyhost = proxy.getProxyAddress();
        int proxyPort = proxy.getProxyPort();
        socket.connect(new InetSocketAddress(proxyhost, proxyPort));
        String hostport = "CONNECT " + host + ":" + port;
        String proxyLine;
        String username = proxy.getProxyUsername();
        if (username == null) {
            proxyLine = "";
        }
        else {
            String password = proxy.getProxyPassword();
            proxyLine = "\r\nProxy-Authorization: Basic " + Base64.encode(username + ":" + password);
        }
        socket.getOutputStream().write((hostport + " HTTP/1.1\r\nHost: "
            + host + ":" + port + proxyLine + "\r\n\r\n").getBytes("UTF-8"));

        InputStream in = socket.getInputStream();
        StringBuilder got = new StringBuilder(100);
        int nlchars = 0;

        while (true) {
            int inByte = in.read();
            if (inByte == -1) {
                throw new ProxyException(ProxyInfo.ProxyType.HTTP);
            }
            char c = (char) inByte;
            got.append(c);
            if (got.length() > 1024) {
                throw new ProxyException(ProxyInfo.ProxyType.HTTP, "Received " +
                    "header of >1024 characters from "
                    + proxyhost + ", cancelling connection");
            }
            if ((nlchars == 0 || nlchars == 2) && c == '\r') {
                nlchars++;
            }
            else if ((nlchars == 1 || nlchars == 3) && c == '\n') {
                nlchars++;
            }
            else {
                nlchars = 0;
            }
            if (nlchars == 4) {
                break;
            }
        }

        if (nlchars != 4) {
            throw new ProxyException(ProxyInfo.ProxyType.HTTP, "Never " +
                "received blank line from "
                + proxyhost + ", cancelling connection");
        }

        String gotstr = got.toString();

        BufferedReader br = new BufferedReader(new StringReader(gotstr));
        String response = br.readLine();

        if (response == null) {
            throw new ProxyException(ProxyInfo.ProxyType.HTTP, "Empty proxy " +
                "response from " + proxyhost + ", cancelling");
        }

        Matcher m = RESPONSE_PATTERN.matcher(response);
        if (!m.matches()) {
            throw new ProxyException(ProxyInfo.ProxyType.HTTP , "Unexpected " +
                "proxy response from " + proxyhost + ": " + response);
        }

        int code = Integer.parseInt(m.group(1));

        if (code != HttpURLConnection.HTTP_OK) {
            throw new ProxyException(ProxyInfo.ProxyType.HTTP);
        }
    }

    private static final Pattern RESPONSE_PATTERN
        = Pattern.compile("HTTP/\\S+\\s(\\d+)\\s(.*)\\s*");

}
