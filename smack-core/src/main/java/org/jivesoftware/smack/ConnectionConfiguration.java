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

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

/**
 * Configuration to use while establishing the connection to the server.
 *
 * @author Gaston Dombiak
 */
public abstract class ConnectionConfiguration {

    static {
        // Ensure that Smack is initialized when ConnectionConfiguration is used, or otherwise e.g.
        // SmackConfiguration.DEBUG may not be initialized yet.
        SmackConfiguration.getVersion();
    }

    /**
     * Hostname of the XMPP server. Usually servers use the same service name as the name
     * of the server. However, there are some servers like google where host would be
     * talk.google.com and the serviceName would be gmail.com.
     */
    protected final String serviceName;
    protected final String host;
    protected final int port;

    private final String keystorePath;
    private final String keystoreType;
    private final String pkcs11Library;
    private final SSLContext customSSLContext;

    /**
     * Used to get information from the user
     */
    private final CallbackHandler callbackHandler;

    private final boolean debuggerEnabled;

    // Holds the socket factory that is used to generate the socket in the connection
    private final SocketFactory socketFactory;

    private final CharSequence username;
    private final String password;
    private final String resource;

    /**
     * Initial presence as of RFC 6121 § 4.2
     * @see <a href="http://xmpp.org/rfcs/rfc6121.html#presence-initial">RFC 6121 § 4.2 Initial Presence</a>
     */
    private final boolean sendPresence;

    private final boolean legacySessionDisabled;
    private final SecurityMode securityMode;

    /**
     * 
     */
    private final String[] enabledSSLProtocols;

    /**
     * 
     */
    private final String[] enabledSSLCiphers;

    private final HostnameVerifier hostnameVerifier;

    // Holds the proxy information (such as proxyhost, proxyport, username, password etc)
    protected final ProxyInfo proxy;

    protected final boolean allowNullOrEmptyUsername;

    protected ConnectionConfiguration(Builder<?,?> builder) {
        username = builder.username;
        password = builder.password;
        callbackHandler = builder.callbackHandler;

        // Resource can be null, this means that the server must provide one
        resource = builder.resource;

        serviceName = builder.serviceName;
        if (serviceName == null) {
            throw new IllegalArgumentException("Must provide XMPP service name");
        }
        host = builder.host;
        port = builder.port;

        proxy = builder.proxy;
        if (proxy != null) {
            if (builder.socketFactory != null) {
                throw new IllegalArgumentException("Can not use proxy together with custom socket factory");
            }
            socketFactory = proxy.getSocketFactory();
        } else {
            socketFactory = builder.socketFactory;
        }

        securityMode = builder.securityMode;
        keystoreType = builder.keystoreType;
        keystorePath = builder.keystorePath;
        pkcs11Library = builder.pkcs11Library;
        customSSLContext = builder.customSSLContext;
        enabledSSLProtocols = builder.enabledSSLProtocols;
        enabledSSLCiphers = builder.enabledSSLCiphers;
        hostnameVerifier = builder.hostnameVerifier;
        sendPresence = builder.sendPresence;
        legacySessionDisabled = builder.legacySessionDisabled;
        debuggerEnabled = builder.debuggerEnabled;
        allowNullOrEmptyUsername = builder.allowEmptyOrNullUsername;
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
     * the mode is {@link SecurityMode#ifpossible}.
     *
     * @return the security mode.
     */
    public SecurityMode getSecurityMode() {
        return securityMode;
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
     * Returns the keystore type, or <tt>null</tt> if it's not set.
     *
     * @return the keystore type.
     */
    public String getKeystoreType() {
        return keystoreType;
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
     * Gets the custom SSLContext previously set with {@link ConnectionConfiguration.Builder#setCustomSSLContext(SSLContext)} for
     * SSL sockets. This is null by default.
     *
     * @return the custom SSLContext or null.
     */
    public SSLContext getCustomSSLContext() {
        return this.customSSLContext;
    }

    /**
     * Return the enabled SSL/TLS protocols.
     *
     * @return the enabled SSL/TLS protocols
     */
    public String[] getEnabledSSLProtocols() {
        return enabledSSLProtocols;
    }

    /**
     * Return the enabled SSL/TLS ciphers.
     *
     * @return the enabled SSL/TLS ciphers
     */
    public String[] getEnabledSSLCiphers() {
        return enabledSSLCiphers;
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
     * Returns true if the new connection about to be establish is going to be debugged. By
     * default the value of {@link SmackConfiguration#DEBUG} is used.
     *
     * @return true if the new connection about to be establish is going to be debugged.
     */
    public boolean isDebuggerEnabled() {
        return debuggerEnabled;
    }

    /**
     * Returns true if a {@link Session} will be requested on login if the server
     * supports it. Although this was mandatory on RFC 3921, RFC 6120/6121 don't
     * even mention this part of the protocol.
     *
     * @return true if a session has to be requested when logging in.
     * @deprecated Smack processes the 'optional' element of the session stream feature.
     * @see Builder#setLegacySessionDisabled(boolean)
     */
    @Deprecated
    public boolean isLegacySessionDisabled() {
        return legacySessionDisabled;
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
         * <p>
         * <b>Do not use this setting</b> unless you can't use {@link #required}. An attacker could easily perform a
         * Man-in-the-middle attack and prevent TLS from being used, leaving you with an unencrypted (and
         * unauthenticated) connection.
         * </p>
         */
        ifpossible,

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
    public CharSequence getUsername() {
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

    /**
     * Returns true if the connection is going to use stream compression. Stream compression
     * will be requested after TLS was established (if TLS was enabled) and only if the server
     * offered stream compression. With stream compression network traffic can be reduced
     * up to 90%. By default compression is disabled.
     *
     * @return true if the connection is going to use stream compression.
     */
    public boolean isCompressionEnabled() {
        // Compression for non-TCP connections is always disabled
        return false;
    }

    /**
     * A builder for XMPP connection configurations.
     * <p>
     * This is an abstract class that uses the builder design pattern and the "getThis() trick" to recover the type of
     * the builder in a class hierarchies with a self-referential generic supertype. Otherwise chaining of build
     * instructions from the superclasses followed by build instructions of a sublcass would not be possible, because
     * the superclass build instructions would return the builder of the superclass and not the one of the subclass. You
     * can read more about it a Angelika Langer's Generics FAQ, especially the entry <a
     * href="http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ206">What is the
     * "getThis()" trick?</a>.
     * </p>
     *
     * @param <B> the builder type parameter.
     * @param <C> the resulting connection configuration type parameter.
     */
    public static abstract class Builder<B extends Builder<B, C>, C extends ConnectionConfiguration> {
        private SecurityMode securityMode = SecurityMode.ifpossible;
        private String keystorePath = System.getProperty("javax.net.ssl.keyStore");
        private String keystoreType = "jks";
        private String pkcs11Library = "pkcs11.config";
        private SSLContext customSSLContext;
        private String[] enabledSSLProtocols;
        private String[] enabledSSLCiphers;
        private HostnameVerifier hostnameVerifier;
        private CharSequence username;
        private String password;
        private String resource = "Smack";
        private boolean sendPresence = true;
        private boolean legacySessionDisabled = false;
        private ProxyInfo proxy;
        private CallbackHandler callbackHandler;
        private boolean debuggerEnabled = SmackConfiguration.DEBUG;
        private SocketFactory socketFactory;
        private String serviceName;
        private String host;
        private int port = 5222;
        private boolean allowEmptyOrNullUsername = false;

        protected Builder() {
        }

        /**
         * Set the XMPP entities username and password.
         * <p>
         * The username is usually the localpart of the clients JID. But some SASL mechanisms or services may require a different
         * format (e.g. the full JID) as used authorization identity.
         * </p>
         *
         * @param username the username or authorization identity
         * @param password the password or token used to authenticate
         * @return a reference to this builder.
         */
        public B setUsernameAndPassword(CharSequence username, String password) {
            this.username = username;
            this.password = password;
            return getThis();
        }

        /**
         * Set the service name of this XMPP service (i.e., the XMPP domain).
         *
         * @param serviceName the service name
         * @return a reference to this builder.
         */
        public B setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return getThis();
        }

        /**
         * Set the resource to use.
         * <p>
         * If <code>resource</code> is <code>null</code>, then the server will automatically create a resource for the
         * client. Default resource is "Smack".
         * </p>
         *
         * @param resource the resource to use.
         * @return a reference to this builder.
         */
        public B setResource(String resource) {
            this.resource = resource;
            return getThis();
        }

        public B setHost(String host) {
            this.host = host;
            return getThis();
        }

        public B setPort(int port) {
            this.port = port;
            return getThis();
        }

        /**
         * Sets a CallbackHandler to obtain information, such as the password or
         * principal information during the SASL authentication. A CallbackHandler
         * will be used <b>ONLY</b> if no password was specified during the login while
         * using SASL authentication.
         *
         * @param callbackHandler to obtain information, such as the password or
         * principal information during the SASL authentication.
         * @return a reference to this builder.
         */
        public B setCallbackHandler(CallbackHandler callbackHandler) {
            this.callbackHandler = callbackHandler;
            return getThis();
        }

        /**
         * Sets the TLS security mode used when making the connection. By default,
         * the mode is {@link SecurityMode#ifpossible}.
         *
         * @param securityMode the security mode.
         * @return a reference to this builder.
         */
        public B setSecurityMode(SecurityMode securityMode) {
            this.securityMode = securityMode;
            return getThis();
        }

        /**
         * Sets the path to the keystore file. The key store file contains the 
         * certificates that may be used to authenticate the client to the server,
         * in the event the server requests or requires it.
         *
         * @param keystorePath the path to the keystore file.
         * @return a reference to this builder.
         */
        public B setKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return getThis();
        }

        /**
         * Sets the keystore type.
         *
         * @param keystoreType the keystore type.
         * @return a reference to this builder.
         */
        public B setKeystoreType(String keystoreType) {
            this.keystoreType = keystoreType;
            return getThis();
        }

        /**
         * Sets the PKCS11 library file location, needed when the
         * Keystore type is PKCS11
         *
         * @param pkcs11Library the path to the PKCS11 library file.
         * @return a reference to this builder.
         */
        public B setPKCS11Library(String pkcs11Library) {
            this.pkcs11Library = pkcs11Library;
            return getThis();
        }

        /**
         * Sets a custom SSLContext for creating SSL sockets.
         * <p>
         * For more information on how to create a SSLContext see <a href=
         * "http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#X509TrustManager"
         * >Java Secure Socket Extension (JSEE) Reference Guide: Creating Your Own X509TrustManager</a>
         *
         * @param context the custom SSLContext for new sockets.
         * @return a reference to this builder.
         */
        public B setCustomSSLContext(SSLContext context) {
            this.customSSLContext = context;
            return getThis();
        }

        /**
         * Set the enabled SSL/TLS protocols.
         *
         * @param enabledSSLProtocols
         * @return a reference to this builder.
         */
        public B setEnabledSSLProtocols(String[] enabledSSLProtocols) {
            this.enabledSSLProtocols = enabledSSLProtocols;
            return getThis();
        }

        /**
         * Set the enabled SSL/TLS ciphers.
         * 
         * @param enabledSSLCiphers the enabled SSL/TLS ciphers 
         * @return a reference to this builder.
         */
        public B setEnabledSSLCiphers(String[] enabledSSLCiphers) {
            this.enabledSSLCiphers = enabledSSLCiphers;
            return getThis();
        }

        /**
         * Set the HostnameVerifier used to verify the hostname of SSLSockets used by XMPP connections
         * created with this ConnectionConfiguration.
         * 
         * @param verifier
         * @return a reference to this builder.
         */
        public B setHostnameVerifier(HostnameVerifier verifier) {
            hostnameVerifier = verifier;
            return getThis();
        }

        /**
         * Sets if a {@link Session} will be requested on login if the server supports
         * it. Although this was mandatory on RFC 3921, RFC 6120/6121 don't even
         * mention this part of the protocol.
         * <p>
         * Deprecation notice: This setting is no longer required in most cases because Smack processes the 'optional'
         * element eventually found in the session stream feature. See also <a
         * href="https://tools.ietf.org/html/draft-cridland-xmpp-session-01">Here Lies Extensible Messaging and Presence
         * Protocol (XMPP) Session Establishment</a>
         * </p>
         *
         * @param legacySessionDisabled if a session has to be requested when logging in.
         * @return a reference to this builder.
         * @deprecated Smack processes the 'optional' element of the session stream feature.
         */
        @Deprecated
        public B setLegacySessionDisabled(boolean legacySessionDisabled) {
            this.legacySessionDisabled = legacySessionDisabled;
            return getThis();
        }

        /**
         * Sets if an initial available presence will be sent to the server. By default
         * an available presence will be sent to the server indicating that this presence
         * is not online and available to receive messages. If you want to log in without
         * being 'noticed' then pass a <tt>false</tt> value.
         *
         * @param sendPresence true if an initial available presence will be sent while logging in.
         * @return a reference to this builder.
         */
        public B setSendPresence(boolean sendPresence) {
            this.sendPresence = sendPresence;
            return getThis();
        }

        /**
         * Sets if the new connection about to be establish is going to be debugged. By
         * default the value of {@link SmackConfiguration#DEBUG} is used.
         *
         * @param debuggerEnabled if the new connection about to be establish is going to be debugged.
         * @return a reference to this builder.
         */
        public B setDebuggerEnabled(boolean debuggerEnabled) {
            this.debuggerEnabled = debuggerEnabled;
            return getThis();
        }

        /**
         * Sets the socket factory used to create new xmppConnection sockets.
         * This is useful when connecting through SOCKS5 proxies.
         *
         * @param socketFactory used to create new sockets.
         * @return a reference to this builder.
         */
        public B setSocketFactory(SocketFactory socketFactory) {
            this.socketFactory = socketFactory;
            return getThis();
        }

        /**
         * Allow <code>null</code> or the empty String as username.
         *
         * Some SASL mechanisms (e.g. SASL External) may also signal the username (as "authorization identity"), in
         * which case Smack should not throw an IllegalArgumentException when the username is not set.
         * 
         * @return a reference to this builder.
         */
        public B allowEmptyOrNullUsernames() {
            allowEmptyOrNullUsername = true;
            return getThis();
        }

        public abstract C build();

        protected abstract B getThis();
    }
}
