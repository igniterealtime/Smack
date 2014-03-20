/**
 *
 * Copyright 2009 Jive Software.
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

package org.jivesoftware.smack;

import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.dns.HostAddress;

/**
 * Configuration to use while establishing the connection to the XMPP server via
 * HTTP binding.
 * 
 * @see BOSHConnection
 * @author Guenther Niess
 */
public class BOSHConfiguration extends ConnectionConfiguration {

    private boolean ssl;
    private String file;

    public BOSHConfiguration(String xmppDomain) {
        super(xmppDomain, 7070);
        ssl = false;
        file = "/http-bind/";
    }

    public BOSHConfiguration(String xmppDomain, int port) {
        super(xmppDomain, port);
        ssl = false;
        file = "/http-bind/";
    }

    /**
     * Create a HTTP Binding configuration.
     * 
     * @param https true if you want to use SSL
     *             (e.g. false for http://domain.lt:7070/http-bind).
     * @param host the hostname or IP address of the connection manager
     *             (e.g. domain.lt for http://domain.lt:7070/http-bind).
     * @param port the port of the connection manager
     *             (e.g. 7070 for http://domain.lt:7070/http-bind).
     * @param filePath the file which is described by the URL
     *             (e.g. /http-bind for http://domain.lt:7070/http-bind).
     * @param xmppDomain the XMPP service name
     *             (e.g. domain.lt for the user alice@domain.lt)
     */
    public BOSHConfiguration(boolean https, String host, int port, String filePath, String xmppDomain) {
        super(host, port, xmppDomain);
        ssl = https;
        file = (filePath != null ? filePath : "/");
    }

    /**
     * Create a HTTP Binding configuration.
     * 
     * @param https true if you want to use SSL
     *             (e.g. false for http://domain.lt:7070/http-bind).
     * @param host the hostname or IP address of the connection manager
     *             (e.g. domain.lt for http://domain.lt:7070/http-bind).
     * @param port the port of the connection manager
     *             (e.g. 7070 for http://domain.lt:7070/http-bind).
     * @param filePath the file which is described by the URL
     *             (e.g. /http-bind for http://domain.lt:7070/http-bind).
     * @param proxy the configuration of a proxy server.
     * @param xmppDomain the XMPP service name
     *             (e.g. domain.lt for the user alice@domain.lt)
     */
    public BOSHConfiguration(boolean https, String host, int port, String filePath, ProxyInfo proxy, String xmppDomain) {
        super(host, port, xmppDomain, proxy);
        ssl = https;
        file = (filePath != null ? filePath : "/");
    }

    public boolean isProxyEnabled() {
        return (proxy != null && proxy.getProxyType() != ProxyInfo.ProxyType.NONE);
    }

    public ProxyInfo getProxyInfo() {
        return proxy;
    }

    public String getProxyAddress() {
        return (proxy != null ? proxy.getProxyAddress() : null);
    }

    public int getProxyPort() {
        return (proxy != null ? proxy.getProxyPort() : 8080);
    }

    public boolean isUsingSSL() {
        return ssl;
    }

    public URI getURI() throws URISyntaxException {
        if (file.charAt(0) != '/') {
            file = '/' + file;
        }
        String host;
        int port;
        if (hostAddresses != null) {
            HostAddress hostAddress = hostAddresses.get(0);
            host = hostAddress.getFQDN();
            port = hostAddress.getPort();
        } else {
            host = getServiceName();
            port = 80;
        }
        return new URI((ssl ? "https://" : "http://") + host + ":" + port + file);
    }
}
