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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

/**
 * Class which stores proxy information such as proxy type, host, port,
 * authentication etc.
 *
 * @author Atul Aggarwal
 */

public class ProxyInfo {
    public enum ProxyType {
        HTTP,
        SOCKS4,
        SOCKS5
    }

    private String proxyAddress;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private ProxyType proxyType;
    private final ProxySocketConnection proxySocketConnection;

    public ProxyInfo(ProxyType pType, String pHost, int pPort, String pUser,
                        String pPass) {
        this.proxyType = pType;
        this.proxyAddress = pHost;
        this.proxyPort = pPort;
        this.proxyUsername = pUser;
        this.proxyPassword = pPass;
        this.proxySocketConnection =
                ProxySocketConnection
                        .forProxyType(proxyType)
                        .apply(this);
    }

    public static ProxyInfo forHttpProxy(String pHost, int pPort, String pUser,
                                    String pPass) {
        return new ProxyInfo(ProxyType.HTTP, pHost, pPort, pUser, pPass);
    }

    public static ProxyInfo forSocks4Proxy(String pHost, int pPort, String pUser,
                                    String pPass) {
        return new ProxyInfo(ProxyType.SOCKS4, pHost, pPort, pUser, pPass);
    }

    public static ProxyInfo forSocks5Proxy(String pHost, int pPort, String pUser,
                                    String pPass) {
        return new ProxyInfo(ProxyType.SOCKS5, pHost, pPort, pUser, pPass);
    }

    public Proxy.Type getJavaProxyType() {
        switch (proxyType) {
            case HTTP:
                return Proxy.Type.HTTP;
            case SOCKS4:
            case SOCKS5:
                return Proxy.Type.SOCKS;
            default:
                throw new AssertionError("Invalid proxy type: " + proxyType);
        }
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public ProxySocketConnection getProxySocketConnection() {
        return proxySocketConnection;
    }

    public Proxy toJavaProxy() {
        final SocketAddress proxySocketAddress = new InetSocketAddress(proxyAddress, proxyPort);
        final Proxy.Type type = getJavaProxyType();
        return new Proxy(type, proxySocketAddress);
    }

}
