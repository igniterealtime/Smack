/**
 * $RCSfile$
 * $Revision: 3306 $
 * $Date: 2006-01-16 14:34:56 -0300 (Mon, 16 Jan 2006) $
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.DNSUtil;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;
import java.io.File;

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

    private String host;
    private int port;

    private String truststorePath;
    private String truststoreType;
    private String truststorePassword;
    private String keystorePath;
    private String keystoreType;
    private String pkcs11Library;
    private boolean verifyChainEnabled = false;
    private boolean verifyRootCAEnabled = false;
    private boolean selfSignedCertificateEnabled = false;
    private boolean expiredCertificatesCheckEnabled = false;
    private boolean notMatchingDomainCheckEnabled = false;
    private SSLContext customSSLContext;

    private boolean compressionEnabled = false;

    private boolean saslAuthenticationEnabled = true;
    /**
     * Used to get information from the user
     */
    private CallbackHandler callbackHandler;

    private boolean debuggerEnabled = Connection.DEBUG_ENABLED;

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
    private SecurityMode securityMode = SecurityMode.enabled;
	
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
        // Perform DNS lookup to get host and port to use
        DNSUtil.HostAddress address = DNSUtil.resolveXMPPDomain(serviceName);
        init(address.getHost(), address.getPort(), serviceName, 
			ProxyInfo.forDefaultProxy());
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
        // Perform DNS lookup to get host and port to use
        DNSUtil.HostAddress address = DNSUtil.resolveXMPPDomain(serviceName);
        init(address.getHost(), address.getPort(), serviceName, proxy);
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
        init(host, port, serviceName, ProxyInfo.forDefaultProxy());
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
        init(host, port, serviceName, proxy);
    }

    /**
     * Creates a new ConnectionConfiguration for a connection that will connect
     * to the desired host and port.
     *
     * @param host the host where the XMPP server is running.
     * @param port the port where the XMPP is listening.
     */
    public ConnectionConfiguration(String host, int port) {
        init(host, port, host, ProxyInfo.forDefaultProxy());
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
        init(host, port, host, proxy);
    }

    private void init(String host, int port, String serviceName, ProxyInfo proxy) {
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.proxy = proxy;

        // Build the default path to the cacert truststore file. By default we are
        // going to use the file located in $JREHOME/lib/security/cacerts.
        String javaHome = System.getProperty("java.home");
        StringBuilder buffer = new StringBuilder();
        buffer.append(javaHome).append(File.separator).append("lib");
        buffer.append(File.separator).append("security");
        buffer.append(File.separator).append("cacerts");
        truststorePath = buffer.toString();
        // Set the default store type
        truststoreType = "jks";
        // Set the default password of the cacert file that is "changeit"
        truststorePassword = "changeit";
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
    public void setServiceName(String serviceName) {
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
     * Returns the host to use when establishing the connection. The host and port to use
     * might have been resolved by a DNS lookup as specified by the XMPP spec (and therefore
     * may not match the {@link #getServiceName service name}.
     *
     * @return the host to use when establishing the connection.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port to use when establishing the connection. The host and port to use
     * might have been resolved by a DNS lookup as specified by the XMPP spec.
     *
     * @return the port to use when establishing the connection.
     */
    public int getPort() {
        return port;
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
     * Retuns the path to the trust store file. The trust store file contains the root
     * certificates of several well known CAs. By default, will attempt to use the
     * the file located in $JREHOME/lib/security/cacerts.
     *
     * @return the path to the truststore file.
     */
    public String getTruststorePath() {
        return truststorePath;
    }

    /**
     * Sets the path to the trust store file. The truststore file contains the root
     * certificates of several well?known CAs. By default Smack is going to use
     * the file located in $JREHOME/lib/security/cacerts.
     *
     * @param truststorePath the path to the truststore file.
     */
    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    /**
     * Returns the trust store type, or <tt>null</tt> if it's not set.
     *
     * @return the trust store type.
     */
    public String getTruststoreType() {
        return truststoreType;
    }

    /**
     * Sets the trust store type.
     *
     * @param truststoreType the trust store type.
     */
    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    /**
     * Returns the password to use to access the trust store file. It is assumed that all
     * certificates share the same password in the trust store.
     *
     * @return the password to use to access the truststore file.
     */
    public String getTruststorePassword() {
        return truststorePassword;
    }

    /**
     * Sets the password to use to access the trust store file. It is assumed that all
     * certificates share the same password in the trust store.
     *
     * @param truststorePassword the password to use to access the truststore file.
     */
    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
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
     * Returns true if the whole chain of certificates presented by the server are going to
     * be checked. By default the certificate chain is not verified.
     *
     * @return true if the whole chaing of certificates presented by the server are going to
     *         be checked.
     */
    public boolean isVerifyChainEnabled() {
        return verifyChainEnabled;
    }

    /**
     * Sets if the whole chain of certificates presented by the server are going to
     * be checked. By default the certificate chain is not verified.
     *
     * @param verifyChainEnabled if the whole chaing of certificates presented by the server
     *        are going to be checked.
     */
    public void setVerifyChainEnabled(boolean verifyChainEnabled) {
        this.verifyChainEnabled = verifyChainEnabled;
    }

    /**
     * Returns true if root CA checking is going to be done. By default checking is disabled.
     *
     * @return true if root CA checking is going to be done.
     */
    public boolean isVerifyRootCAEnabled() {
        return verifyRootCAEnabled;
    }

    /**
     * Sets if root CA checking is going to be done. By default checking is disabled.
     *
     * @param verifyRootCAEnabled if root CA checking is going to be done.
     */
    public void setVerifyRootCAEnabled(boolean verifyRootCAEnabled) {
        this.verifyRootCAEnabled = verifyRootCAEnabled;
    }

    /**
     * Returns true if self-signed certificates are going to be accepted. By default
     * this option is disabled.
     *
     * @return true if self-signed certificates are going to be accepted.
     */
    public boolean isSelfSignedCertificateEnabled() {
        return selfSignedCertificateEnabled;
    }

    /**
     * Sets if self-signed certificates are going to be accepted. By default
     * this option is disabled.
     *
     * @param selfSignedCertificateEnabled if self-signed certificates are going to be accepted.
     */
    public void setSelfSignedCertificateEnabled(boolean selfSignedCertificateEnabled) {
        this.selfSignedCertificateEnabled = selfSignedCertificateEnabled;
    }

    /**
     * Returns true if certificates presented by the server are going to be checked for their
     * validity. By default certificates are not verified.
     *
     * @return true if certificates presented by the server are going to be checked for their
     *         validity.
     */
    public boolean isExpiredCertificatesCheckEnabled() {
        return expiredCertificatesCheckEnabled;
    }

    /**
     * Sets if certificates presented by the server are going to be checked for their
     * validity. By default certificates are not verified.
     *
     * @param expiredCertificatesCheckEnabled if certificates presented by the server are going
     *        to be checked for their validity.
     */
    public void setExpiredCertificatesCheckEnabled(boolean expiredCertificatesCheckEnabled) {
        this.expiredCertificatesCheckEnabled = expiredCertificatesCheckEnabled;
    }

    /**
     * Returns true if certificates presented by the server are going to be checked for their
     * domain. By default certificates are not verified.
     *
     * @return true if certificates presented by the server are going to be checked for their
     *         domain.
     */
    public boolean isNotMatchingDomainCheckEnabled() {
        return notMatchingDomainCheckEnabled;
    }

    /**
     * Sets if certificates presented by the server are going to be checked for their
     * domain. By default certificates are not verified.
     *
     * @param notMatchingDomainCheckEnabled if certificates presented by the server are going
     *        to be checked for their domain.
     */
    public void setNotMatchingDomainCheckEnabled(boolean notMatchingDomainCheckEnabled) {
        this.notMatchingDomainCheckEnabled = notMatchingDomainCheckEnabled;
    }

    /**
     * Gets the custom SSLContext for SSL sockets. This is null by default.
     *
     * @return the SSLContext previously set with setCustomSSLContext() or null.
     */
    public SSLContext getCustomSSLContext() {
        return this.customSSLContext;
    }

    /**
     * Sets a custom SSLContext for creating SSL sockets. A custom Context causes all other
     * SSL/TLS realted settings to be ignored.
     *
     * @param context the custom SSLContext for new sockets; null to reset default behavior.
     */
    public void setCustomSSLContext(SSLContext context) {
        this.customSSLContext = context;
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
     * Returns true if the client is going to use SASL authentication when logging into the
     * server. If SASL authenticatin fails then the client will try to use non-sasl authentication.
     * By default SASL is enabled.
     *
     * @return true if the client is going to use SASL authentication when logging into the
     *         server.
     */
    public boolean isSASLAuthenticationEnabled() {
        return saslAuthenticationEnabled;
    }

    /**
     * Sets whether the client will use SASL authentication when logging into the
     * server. If SASL authenticatin fails then the client will try to use non-sasl authentication.
     * By default, SASL is enabled.
     *
     * @param saslAuthenticationEnabled if the client is going to use SASL authentication when
     *        logging into the server.
     */
    public void setSASLAuthenticationEnabled(boolean saslAuthenticationEnabled) {
        this.saslAuthenticationEnabled = saslAuthenticationEnabled;
    }

    /**
     * Returns true if the new connection about to be establish is going to be debugged. By
     * default the value of {@link Connection#DEBUG_ENABLED} is used.
     *
     * @return true if the new connection about to be establish is going to be debugged.
     */
    public boolean isDebuggerEnabled() {
        return debuggerEnabled;
    }

    /**
     * Sets if the new connection about to be establish is going to be debugged. By
     * default the value of {@link Connection#DEBUG_ENABLED} is used.
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
     * @param isAllowed if the reconnection mechanism is allowed to use.
     */
    public void setReconnectionAllowed(boolean isAllowed) {
        this.reconnectionAllowed = isAllowed;
    }
    /**
     * Returns if the reconnection mechanism is allowed to be used. By default
     * reconnection is allowed.
     *
     * @return if the reconnection mechanism is allowed to be used.
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
    String getUsername() {
        return this.username;
    }

    /**
     * Returns the password to use when trying to reconnect to the server.
     *
     * @return the password to use when trying to reconnect to the server.
     */
    String getPassword() {
        return this.password;
    }

    /**
     * Returns the resource to use when trying to reconnect to the server.
     *
     * @return the resource to use when trying to reconnect to the server.
     */
    String getResource() {
        return resource;
    }

    /**
     * Returns true if an available presence should be sent when logging in while reconnecting.
     *
     * @return true if an available presence should be sent when logging in while reconnecting
     */
    boolean isSendPresence() {
        return sendPresence;
    }

    void setLoginInfo(String username, String password, String resource) {
        this.username = username;
        this.password = password;
        this.resource = resource;
    }
}
