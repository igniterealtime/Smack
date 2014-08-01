/**
 *
 * Copyright 2003-2007 Jive Software.
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

import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.HostAddress;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration to use while establishing the connection to the server. It is possible to
 * configure the path to the trustore file that keeps the trusted CA root certificates and
 * enable or disable all or some of the checkings done while verifying server certificates.<p>
 *
 * It is also possible to configure if TLS, SASL, and compression are used or not.
 *
 * @author Gaston Dombiak
 */
public class ConnectionConfiguration implements Cloneable {

    /**
     * Hostname of the XMPP server. Usually servers use the same service name as the name
     * of the server. However, there are some servers like google where host would be
     * talk.google.com and the serviceName would be gmail.com.
     */
    private String serviceName;

    protected List<HostAddress> hostAddresses;

    private String keystorePath;
    private String keystoreType;
    private String pkcs11Library;
    private SSLContext customSSLContext;

    private boolean compressionEnabled = false;

    /**
     * Used to get information from the user
     */
    private CallbackHandler callbackHandler;

    private boolean debuggerEnabled = SmackConfiguration.DEBUG_ENABLED;

    // Flag that indicates if a reconnection should be attempted when abruptly disconnected
    private boolean reconnectionAllowed = true;
    
    // Holds the socket factory that is used to generate the socket in the connection
    private SocketFactory socketFactory;
    
    // Holds the authentication information for future reconnections
    private String username;
    private String password;
    private String resource;
    private boolean sendPresence = true;
    private boolean rosterLoadedAtLogin = true;
    private boolean legacySessionDisabled = false;
    private boolean useDnsSrvRr = true;
    private SecurityMode securityMode = SecurityMode.enabled;

    private HostnameVerifier hostnameVerifier;

    /**
     * Permanent store for the Roster, needed for roster versioning
     */
    private RosterStore rosterStore;

    // Holds the proxy information (such as proxyhost, proxyport, username, password etc)
    protected ProxyInfo proxy;

    /**
     * Creates a new ConnectionConfiguration for the specified service name.
     * A DNS SRV lookup will be performed to find out the actual host address
     * and port to use for the connection.
     *
     * @param serviceName the name of the service provided by an XMPP server.
     */
    public ConnectionConfiguration(String serviceName) {
        init(serviceName, ProxyInfo.forDefaultProxy());
    }

     /**
     * Creates a new ConnectionConfiguration for the specified service name 
     * with specified proxy.
     * A DNS SRV lookup will be performed to find out the actual host address
     * and port to use for the connection.
     *
     * @param serviceName the name of the service provided by an XMPP server.
     * @param proxy the proxy through which XMPP is to be connected
     */
    public ConnectionConfiguration(String serviceName,ProxyInfo proxy) {
        init(serviceName, proxy);
    }

    /**
     * Creates a new ConnectionConfiguration using the specified host, port and
     * service name. This is useful for manually overriding the DNS SRV lookup
     * process that's used with the {@link #ConnectionConfiguration(String)}
     * constructor. For example, say that an XMPP server is running at localhost
     * in an internal network on port 5222 but is configured to think that it's
     * "example.com" for testing purposes. This constructor is necessary to connect
     * to the server in that case since a DNS SRV lookup for example.com would not
     * point to the local testing server.
     *
     * @param host the host where the XMPP server is running.
     * @param port the port where the XMPP is listening.
     * @param serviceName the name of the service provided by an XMPP server.
     */
    public ConnectionConfiguration(String host, int port, String serviceName) {
        initHostAddresses(host, port);
        init(serviceName, ProxyInfo.forDefaultProxy());
    }
	
	/**
     * Creates a new ConnectionConfiguration using the specified host, port and
     * service name. This is useful for manually overriding the DNS SRV lookup
     * process that's used with the {@link #ConnectionConfiguration(String)}
     * constructor. For example, say that an XMPP server is running at localhost
     * in an internal network on port 5222 but is configured to think that it's
     * "example.com" for testing purposes. This constructor is necessary to connect
     * to the server in that case since a DNS SRV lookup for example.com would not
     * point to the local testing server.
     *
     * @param host the host where the XMPP server is running.
     * @param port the port where the XMPP is listening.
     * @param serviceName the name of the service provided by an XMPP server.
     * @param proxy the proxy through which XMPP is to be connected
     */
    public ConnectionConfiguration(String host, int port, String serviceName, ProxyInfo proxy) {
        initHostAddresses(host, port);
        init(serviceName, proxy);
    }

    /**
     * Creates a new ConnectionConfiguration for a connection that will connect
     * to the desired host and port.
     *
     * @param host the host where the XMPP server is running.
     * @param port the port where the XMPP is listening.
     */
    public ConnectionConfiguration(String host, int port) {
        initHostAddresses(host, port);
        init(host, ProxyInfo.forDefaultProxy());
    }
	
	/**
     * Creates a new ConnectionConfiguration for a connection that will connect
     * to the desired host and port with desired proxy.
     *
     * @param host the host where the XMPP server is running.
     * @param port the port where the XMPP is listening.
     * @param proxy the proxy through which XMPP is to be connected
     */
    public ConnectionConfiguration(String host, int port, ProxyInfo proxy) {
        initHostAddresses(host, port);
        init(host, proxy);
    }

    protected void init(String serviceName, ProxyInfo proxy) {
        if (StringUtils.isEmpty(serviceName)) {
            throw new IllegalArgumentException("serviceName must not be the empty String");
        }
        this.serviceName = serviceName;
        this.proxy = proxy;

        keystorePath = System.getProperty("javax.net.ssl.keyStore");
        keystoreType = "jks";
        pkcs11Library = "pkcs11.config";
		
		//Setting the SocketFactory according to proxy supplied
        socketFactory = proxy.getSocketFactory();
    }

    /**
     * Sets the server name, also known as XMPP domain of the target server.
     *
     * @param serviceName the XMPP domain of the target server.
     */
    void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Returns the server name of the target server.
     *
     * @return the server name of the target server.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the TLS security mode used when making the connection. By default,
     * the mode is {@link SecurityMode#enabled}.
     *
     * @return the security mode.
     */
    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    /**
     * Sets the TLS security mode used when making the connection. By default,
     * the mode is {@link SecurityMode#enabled}.
     *
     * @param securityMode the security mode.
     */
    public void setSecurityMode(SecurityMode securityMode) {
        this.securityMode = securityMode;
    }

    /**
     * Retuns the path to the keystore file. The key store file contains the 
     * certificates that may be used to authenticate the client to the server,
     * in the event the server requests or requires it.
     *
     * @return the path to the keystore file.
     */
    public String getKeystorePath() {
        return keystorePath;
    }

    /**
     * Sets the path to the keystore file. The key store file contains the 
     * certificates that may be used to authenticate the client to the server,
     * in the event the server requests or requires it.
     *
     * @param keystorePath the path to the keystore file.
     */
    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    /**
     * Returns the keystore type, or <tt>null</tt> if it's not set.
     *
     * @return the keystore type.
     */
    public String getKeystoreType() {
        return keystoreType;
    }

    /**
     * Sets the keystore type.
     *
     * @param keystoreType the keystore type.
     */
    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }


    /**
     * Returns the PKCS11 library file location, needed when the
     * Keystore type is PKCS11.
     *
     * @return the path to the PKCS11 library file
     */
    public String getPKCS11Library() {
        return pkcs11Library;
    }

    /**
     * Sets the PKCS11 library file location, needed when the
     * Keystore type is PKCS11
     *
     * @param pkcs11Library the path to the PKCS11 library file
     */
    public void setPKCS11Library(String pkcs11Library) {
        this.pkcs11Library = pkcs11Library;
    }

    /**
     * Gets the custom SSLContext previously set with {@link #setCustomSSLContext(SSLContext)} for
     * SSL sockets. This is null by default.
     *
     * @return the custom SSLContext or null.
     */
    public SSLContext getCustomSSLContext() {
        return this.customSSLContext;
    }

    /**
     * Sets a custom SSLContext for creating SSL sockets.
     * <p>
     * For more information on how to create a SSLContext see <a href=
     * "http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#X509TrustManager"
     * >Java Secure Socket Extension (JSEE) Reference Guide: Creating Your Own X509TrustManager</a>
     *
     * @param context the custom SSLContext for new sockets
     */
    public void setCustomSSLContext(SSLContext context) {
        this.customSSLContext = context;
    }

    /**
     * Set the HostnameVerifier used to verify the hostname of SSLSockets used by XMPP connections
     * created with this ConnectionConfiguration.
     * 
     * @param verifier
     */
    public void setHostnameVerifier(HostnameVerifier verifier) {
        hostnameVerifier = verifier;
    }

    /**
     * Returns the configured HostnameVerifier of this ConnectionConfiguration or the Smack default
     * HostnameVerifier configured with
     * {@link SmackConfiguration#setDefaultHostnameVerifier(HostnameVerifier)}.
     * 
     * @return a configured HostnameVerifier or <code>null</code>
     */
    public HostnameVerifier getHostnameVerifier() {
        if (hostnameVerifier != null)
            return hostnameVerifier;
        return SmackConfiguration.getDefaultHostnameVerifier();
    }

    /**
     * Returns true if the connection is going to use stream compression. Stream compression
     * will be requested after TLS was established (if TLS was enabled) and only if the server
     * offered stream compression. With stream compression network traffic can be reduced
     * up to 90%. By default compression is disabled.
     *
     * @return true if the connection is going to use stream compression.
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * Sets if the connection is going to use stream compression. Stream compression
     * will be requested after TLS was established (if TLS was enabled) and only if the server
     * offered stream compression. With stream compression network traffic can be reduced
     * up to 90%. By default compression is disabled.
     *
     * @param compressionEnabled if the connection is going to use stream compression.
     */
    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    /**
     * Returns true if the new connection about to be establish is going to be debugged. By
     * default the value of {@link SmackConfiguration#DEBUG_ENABLED} is used.
     *
     * @return true if the new connection about to be establish is going to be debugged.
     */
    public boolean isDebuggerEnabled() {
        return debuggerEnabled;
    }

    /**
     * Sets if the new connection about to be establish is going to be debugged. By
     * default the value of {@link SmackConfiguration#DEBUG_ENABLED} is used.
     *
     * @param debuggerEnabled if the new connection about to be establish is going to be debugged.
     */
    public void setDebuggerEnabled(boolean debuggerEnabled) {
        this.debuggerEnabled = debuggerEnabled;
    }
    
    /**
     * Sets if the reconnection mechanism is allowed to be used. By default
     * reconnection is allowed.
     *
     * @param isAllowed if the reconnection mechanism should be enabled for this connection.
     */
    public void setReconnectionAllowed(boolean isAllowed) {
        this.reconnectionAllowed = isAllowed;
    }

    /**
     * Returns if the reconnection mechanism is allowed to be used. By default reconnection is
     * allowed. You can disable the reconnection mechanism with {@link
     * #setReconnectionAllowed(boolean)}.
     *
     * @return true, if the reconnection mechanism is enabled.
     */
    public boolean isReconnectionAllowed() {
        return this.reconnectionAllowed;
    }

    /**
     * Sets the socket factory used to create new xmppConnection sockets.
     * This is useful when connecting through SOCKS5 proxies.
     *
     * @param socketFactory used to create new sockets.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * Sets if an initial available presence will be sent to the server. By default
     * an available presence will be sent to the server indicating that this presence
     * is not online and available to receive messages. If you want to log in without
     * being 'noticed' then pass a <tt>false</tt> value.
     *
     * @param sendPresence true if an initial available presence will be sent while logging in.
     */
    public void setSendPresence(boolean sendPresence) {
        this.sendPresence = sendPresence;
    }

    /**
     * Returns true if the roster will be loaded from the server when logging in. This
     * is the common behaviour for clients but sometimes clients may want to differ this
     * or just never do it if not interested in rosters.
     *
     * @return true if the roster will be loaded from the server when logging in.
     */
    public boolean isRosterLoadedAtLogin() {
        return rosterLoadedAtLogin;
    }

    /**
     * Sets if the roster will be loaded from the server when logging in. This
     * is the common behaviour for clients but sometimes clients may want to differ this
     * or just never do it if not interested in rosters.
     *
     * @param rosterLoadedAtLogin if the roster will be loaded from the server when logging in.
     */
    public void setRosterLoadedAtLogin(boolean rosterLoadedAtLogin) {
        this.rosterLoadedAtLogin = rosterLoadedAtLogin;
    }

    /**
     * Returns true if a {@link Session} will be requested on login if the server
     * supports it. Although this was mandatory on RFC 3921, RFC 6120/6121 don't
     * even mention this part of the protocol.
     *
     * @return true if a session has to be requested when logging in.
     */
    public boolean isLegacySessionDisabled() {
        return legacySessionDisabled;
    }

    /**
     * Sets if a {@link Session} will be requested on login if the server supports
     * it. Although this was mandatory on RFC 3921, RFC 6120/6121 don't even
     * mention this part of the protocol.
     *
     * @param legacySessionDisabled if a session has to be requested when logging in.
     */
    public void setLegacySessionDisabled(boolean legacySessionDisabled) {
        this.legacySessionDisabled = legacySessionDisabled;
    }

    /**
     * Returns a CallbackHandler to obtain information, such as the password or
     * principal information during the SASL authentication. A CallbackHandler
     * will be used <b>ONLY</b> if no password was specified during the login while
     * using SASL authentication.
     *
     * @return a CallbackHandler to obtain information, such as the password or
     * principal information during the SASL authentication.
     */
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    /**
     * Sets a CallbackHandler to obtain information, such as the password or
     * principal information during the SASL authentication. A CallbackHandler
     * will be used <b>ONLY</b> if no password was specified during the login while
     * using SASL authentication.
     *
     * @param callbackHandler to obtain information, such as the password or
     * principal information during the SASL authentication.
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     * Returns the socket factory used to create new xmppConnection sockets.
     * This is useful when connecting through SOCKS5 proxies.
     * 
     * @return socketFactory used to create new sockets.
     */
    public SocketFactory getSocketFactory() {
        return this.socketFactory;
    }

    public List<HostAddress> getHostAddresses() {
        return Collections.unmodifiableList(hostAddresses);
    }

    /**
     * Set the permanent roster store
     */
    public void setRosterStore(RosterStore store) {
        rosterStore = store;
    }

    /**
     * Get the permanent roster store
     */
    public RosterStore getRosterStore() {
        return rosterStore;
    }


    /**
     * An enumeration for TLS security modes that are available when making a connection
     * to the XMPP server.
     */
    public static enum SecurityMode {

        /**
         * Securirty via TLS encryption is required in order to connect. If the server
         * does not offer TLS or if the TLS negotiaton fails, the connection to the server
         * will fail.
         */
        required,

        /**
         * Security via TLS encryption is used whenever it's available. This is the
         * default setting.
         */
        enabled,

        /**
         * Security via TLS encryption is disabled and only un-encrypted connections will
         * be used. If only TLS encryption is available from the server, the connection
         * will fail.
         */
        disabled
    }

    /**
     * Returns the username to use when trying to reconnect to the server.
     *
     * @return the username to use when trying to reconnect to the server.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Returns the password to use when trying to reconnect to the server.
     *
     * @return the password to use when trying to reconnect to the server.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Returns the resource to use when trying to reconnect to the server.
     *
     * @return the resource to use when trying to reconnect to the server.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns true if an available presence should be sent when logging in while reconnecting.
     *
     * @return true if an available presence should be sent when logging in while reconnecting
     */
    public boolean isSendPresence() {
        return sendPresence;
    }

    void setLoginInfo(String username, String password, String resource) {
        this.username = username;
        this.password = password;
        this.resource = resource;
    }

    void maybeResolveDns() throws Exception {
        if (!useDnsSrvRr) return;
        hostAddresses = DNSUtil.resolveXMPPDomain(serviceName);
    }

    private void initHostAddresses(String host, int port) {
        if (StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("host must not be the empty String");
        }
        hostAddresses = new ArrayList<HostAddress>(1);
        HostAddress hostAddress;
        hostAddress = new HostAddress(host, port);
        hostAddresses.add(hostAddress);
        useDnsSrvRr = false;
    }
}
