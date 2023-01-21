/**
 *
 * Copyright 2003-2007 Jive Software, 2017-2022 Florian Schmaus.
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.packet.id.StanzaIdSourceFactory;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.SslContextFactory;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.dns.SmackDaneProvider;
import org.jivesoftware.smack.util.dns.SmackDaneVerifier;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.minidns.dnsname.DnsName;
import org.minidns.dnsname.InvalidDnsNameException;
import org.minidns.util.InetAddressUtil;

/**
 * The connection configuration used for XMPP client-to-server connections. A well configured XMPP service will
 * typically only require you to provide two parameters: The XMPP address, also known as the JID, of the user and the
 * password. All other configuration parameters could ideally be determined automatically by Smack. Hence it is often
 * enough to call {@link Builder#setXmppAddressAndPassword(CharSequence, String)}.
 * <p>
 * Technically there are typically at least two parameters required: Some kind of credentials for authentication. And
 * the XMPP service domain. The credentials often consists of a username and password use for the SASL authentication.
 * But there are also other authentication mechanisms, like client side certificates, which do not require a particular
 * username and password.
 * </p>
 * <p>
 * There are some misconceptions about XMPP client-to-server parameters: The first is that the username used for
 * authentication will be equal to the localpart of the bound XMPP address after authentication. While this is usually
 * true, it is not required. Technically the username used for authentication and the resulting XMPP address are
 * completely independent from each other. The second common misconception steers from the terms "XMPP host" and "XMPP
 * service domain": An XMPP service host is a system which hosts one or multiple XMPP domains. The "XMPP service domain"
 * will be usually the domainpart of the bound JID. This domain is used to verify the remote endpoint, typically using
 * TLS. This third misconception is that the XMPP service domain is required to become the domainpart of the bound JID.
 * Again, while this is very common to be true, it is not strictly required.
 * </p>
 *
 * @author Gaston Dombiak
 * @author Florian Schmaus
 */
public abstract class ConnectionConfiguration {

    static {
        Smack.ensureInitialized();
    }

    private static final Logger LOGGER = Logger.getLogger(ConnectionConfiguration.class.getName());

    /**
     * The XMPP domain of the XMPP Service. Usually servers use the same service name as the name
     * of the server. However, there are some servers like google where host would be
     * talk.google.com and the serviceName would be gmail.com.
     */
    protected final DomainBareJid xmppServiceDomain;

    protected final DnsName xmppServiceDomainDnsName;

    protected final InetAddress hostAddress;
    protected final DnsName host;
    protected final UInt16 port;

    /**
     * Used to get information from the user
     */
    private final CallbackHandler callbackHandler;

    private final SmackDebuggerFactory debuggerFactory;

    // Holds the socket factory that is used to generate the socket in the connection
    private final SocketFactory socketFactory;

    private final CharSequence username;
    private final String password;
    private final Resourcepart resource;

    private final Locale language;

    /**
     * The optional SASL authorization identity (see RFC 6120 § 6.3.8).
     */
    private final EntityBareJid authzid;

    /**
     * Initial presence as of RFC 6121 § 4.2
     * @see <a href="http://xmpp.org/rfcs/rfc6121.html#presence-initial">RFC 6121 § 4.2 Initial Presence</a>
     */
    private final boolean sendPresence;

    private final SecurityMode securityMode;

    final SmackTlsContext smackTlsContext;

    private final DnssecMode dnssecMode;

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

    private final Set<String> enabledSaslMechanisms;

    private final boolean compressionEnabled;

    private final StanzaIdSourceFactory stanzaIdSourceFactory;

    protected ConnectionConfiguration(Builder<?, ?> builder) {
        try {
            smackTlsContext = getSmackTlsContext(builder.dnssecMode, builder.sslContextFactory,
                            builder.customX509TrustManager, builder.keyManagers, builder.sslContextSecureRandom, builder.keystoreType, builder.keystorePath,
                            builder.callbackHandler, builder.pkcs11Library);
        } catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | CertificateException
                        | KeyStoreException | NoSuchProviderException | IOException | NoSuchMethodException
                        | SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException | UnsupportedCallbackException e) {
            throw new IllegalArgumentException(e);
        }

        authzid = builder.authzid;
        username = builder.username;
        password = builder.password;
        callbackHandler = builder.callbackHandler;

        // Resource can be null, this means that the server must provide one
        resource = builder.resource;

        language = builder.language;

        xmppServiceDomain = builder.xmppServiceDomain;
        if (xmppServiceDomain == null) {
            throw new IllegalArgumentException("Must define the XMPP domain");
        }

        DnsName xmppServiceDomainDnsName;
        try {
            xmppServiceDomainDnsName = DnsName.from(xmppServiceDomain);
        } catch (InvalidDnsNameException e) {
            LOGGER.log(Level.INFO,
                            "Could not transform XMPP service domain '" + xmppServiceDomain
                          + "' to a DNS name. TLS X.509 certificate validiation may not be possible.",
                            e);
            xmppServiceDomainDnsName = null;
        }
        this.xmppServiceDomainDnsName = xmppServiceDomainDnsName;

        hostAddress = builder.hostAddress;
        host = builder.host;
        port = builder.port;

        proxy = builder.proxy;
        socketFactory = builder.socketFactory;

        dnssecMode = builder.dnssecMode;

        securityMode = builder.securityMode;
        enabledSSLProtocols = builder.enabledSSLProtocols;
        enabledSSLCiphers = builder.enabledSSLCiphers;
        hostnameVerifier = builder.hostnameVerifier;
        sendPresence = builder.sendPresence;
        debuggerFactory = builder.debuggerFactory;
        allowNullOrEmptyUsername = builder.allowEmptyOrNullUsername;
        enabledSaslMechanisms = builder.enabledSaslMechanisms;

        compressionEnabled = builder.compressionEnabled;

        stanzaIdSourceFactory = builder.stanzaIdSourceFactory;

        // If the enabledSaslmechanisms are set, then they must not be empty
        assert enabledSaslMechanisms == null || !enabledSaslMechanisms.isEmpty();
    }

    private static SmackTlsContext getSmackTlsContext(DnssecMode dnssecMode, SslContextFactory sslContextFactory,
                    X509TrustManager trustManager, KeyManager[] keyManagers, SecureRandom secureRandom, String keystoreType, String keystorePath,
                    CallbackHandler callbackHandler, String pkcs11Library) throws NoSuchAlgorithmException,
                    CertificateException, IOException, KeyStoreException, NoSuchProviderException,
                    UnrecoverableKeyException, KeyManagementException, UnsupportedCallbackException,
                    NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException,
                    IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final SSLContext context;
        if (sslContextFactory != null) {
            context = sslContextFactory.createSslContext();
        } else {
            // If the user didn't specify a SslContextFactory, use the default one
            context = SSLContext.getInstance("TLS");
        }

        // TODO: Remove the block below once we removed setKeystorePath(), setKeystoreType(), setCallbackHanlder() and
        // setPKCS11Library() in the builder, and all related fields and the parameters of this function.
        if (keyManagers == null) {
            keyManagers = Builder.getKeyManagersFrom(keystoreType, keystorePath, callbackHandler, pkcs11Library);
        }

        SmackDaneVerifier daneVerifier = null;
        if (dnssecMode == DnssecMode.needsDnssecAndDane) {
            SmackDaneProvider daneProvider = DNSUtil.getDaneProvider();
            if (daneProvider == null) {
                throw new UnsupportedOperationException("DANE enabled but no SmackDaneProvider configured");
            }
            daneVerifier = daneProvider.newInstance();
            if (daneVerifier == null) {
                throw new IllegalStateException("DANE requested but DANE provider did not return a DANE verifier");
            }

            // User requested DANE verification.
            daneVerifier.init(context, keyManagers, trustManager, secureRandom);
        } else {
            final TrustManager[] trustManagers;
            if (trustManager != null) {
                trustManagers = new TrustManager[] { trustManager };
            } else {
                // Ensure trustManagers is null in case there was no explicit trust manager provided, so that the
                // default one is used.
                trustManagers = null;
            }

            context.init(keyManagers, trustManagers, secureRandom);
        }

        return new SmackTlsContext(context, daneVerifier);
    }

    public DnsName getHost() {
        return host;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public UInt16 getPort() {
        return port;
    }

    /**
     * Returns the server name of the target server.
     *
     * @return the server name of the target server.
     * @deprecated use {@link #getXMPPServiceDomain()} instead.
     */
    @Deprecated
    public DomainBareJid getServiceName() {
        return xmppServiceDomain;
    }

    /**
     * Returns the XMPP domain used by this configuration.
     *
     * @return the XMPP domain.
     */
    public DomainBareJid getXMPPServiceDomain() {
        return xmppServiceDomain;
    }

    /**
     * Returns the XMPP service domain as DNS name if possible. Note that since not every XMPP address domainpart is a
     * valid DNS name, this method may return <code>null</code>.
     *
     * @return the XMPP service domain as DNS name or <code>null</code>.
     * @since 4.3.4
     */
    public DnsName getXmppServiceDomainAsDnsNameIfPossible() {
        return xmppServiceDomainDnsName;
    }

    /**
     * Returns the TLS security mode used when making the connection. By default,
     * the mode is {@link SecurityMode#required}.
     *
     * @return the security mode.
     */
    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    public DnssecMode getDnssecMode() {
        return dnssecMode;
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
     * Returns the Smack debugger factory.
     *
     * @return the Smack debugger factory or <code>null</code>
     */
    public SmackDebuggerFactory getDebuggerFactory() {
        return debuggerFactory;
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
     * Get the configured proxy information (if any).
     *
     * @return the configured proxy information or <code>null</code>.
     */
    public ProxyInfo getProxyInfo() {
        return proxy;
    }

    /**
     * An enumeration for TLS security modes that are available when making a connection
     * to the XMPP server.
     */
    public enum SecurityMode {

        /**
         * Security via TLS encryption is required in order to connect. If the server
         * does not offer TLS or if the TLS negotiation fails, the connection to the server
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
     * Determines the requested DNSSEC security mode.
     * <b>Note that Smack's support for DNSSEC/DANE is experimental!</b>
     * <p>
     * The default '{@link #disabled}' means that neither DNSSEC nor DANE verification will be performed. When
     * '{@link #needsDnssec}' is used, then the connection will not be established if the resource records used to connect
     * to the XMPP service are not authenticated by DNSSEC. Additionally, if '{@link #needsDnssecAndDane}' is used, then
     * the XMPP service's TLS certificate is verified using DANE.
     *
     */
    public enum DnssecMode {

        /**
         * Do not perform any DNSSEC authentication or DANE verification.
         */
        disabled,

        /**
         * <b>Experimental!</b>
         * Require all DNS information to be authenticated by DNSSEC.
         */
        needsDnssec,

        /**
         * <b>Experimental!</b>
         * Require all DNS information to be authenticated by DNSSEC and require the XMPP service's TLS certificate to be verified using DANE.
         */
        needsDnssecAndDane,

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
    public Resourcepart getResource() {
        return resource;
    }

    /**
     * Returns the stream language to use when connecting to the server.
     *
     * @return the stream language to use when connecting to the server.
     */
    public Locale getLanguage() {
        return language;
    }

    /**
     * Returns the xml:lang string of the stream language to use when connecting to the server.
     *
     * <p>If the developer sets the language to null, this will also return null, leading to
     * the removal of the xml:lang tag from the stream. If a Locale("") is configured, this will
     * return "", which can be used as an override.</p>
     *
     * @return the stream language to use when connecting to the server.
     */
    public String getXmlLang() {
        // TODO: Change to Locale.toLanguageTag() once Smack's minimum Android API level is 21 or higher.
        // This will need a workaround for new Locale("").getLanguageTag() returning "und". Expected
        // behavior of this function:
        //  - returns null if language is null
        //  - returns "" if language.getLanguage() returns the empty string
        //  - returns language.toLanguageTag() otherwise
        return language != null ? language.toString().replace("_", "-") : null;
    }

    /**
     * Returns the optional XMPP address to be requested as the SASL authorization identity.
     *
     * @return the authorization identifier.
     * @see <a href="http://tools.ietf.org/html/rfc6120#section-6.3.8">RFC 6120 § 6.3.8. Authorization Identity</a>
     * @since 4.2
     */
    public EntityBareJid getAuthzid() {
        return authzid;
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
        return compressionEnabled;
    }

    /**
     * Check if the given SASL mechansism is enabled in this connection configuration.
     *
     * @param saslMechanism TODO javadoc me please
     * @return true if the given SASL mechanism is enabled, false otherwise.
     */
    public boolean isEnabledSaslMechanism(String saslMechanism) {
        // If enabledSaslMechanisms is not set, then all mechanisms which are not blacklisted are enabled per default.
        if (enabledSaslMechanisms == null) {
            return !SASLAuthentication.getBlacklistedSASLMechanisms().contains(saslMechanism);
        }
        return enabledSaslMechanisms.contains(saslMechanism);
    }

    /**
     * Return the explicitly enabled SASL mechanisms. May return <code>null</code> if no SASL mechanisms where
     * explicitly enabled, i.e. all SALS mechanisms supported and announced by the service will be considered.
     *
     * @return the enabled SASL mechanisms or <code>null</code>.
     */
    public Set<String> getEnabledSaslMechanisms() {
        if (enabledSaslMechanisms == null) {
            return null;
        }
        return Collections.unmodifiableSet(enabledSaslMechanisms);
    }

    StanzaIdSource constructStanzaIdSource() {
        return stanzaIdSourceFactory.constructStanzaIdSource();
    }

    /**
     * A builder for XMPP connection configurations.
     * <p>
     * This is an abstract class that uses the builder design pattern and the "getThis() trick" to recover the type of
     * the builder in a class hierarchies with a self-referential generic supertype. Otherwise chaining of build
     * instructions from the superclasses followed by build instructions of a subclass would not be possible, because
     * the superclass build instructions would return the builder of the superclass and not the one of the subclass. You
     * can read more about it a Angelika Langer's Generics FAQ, especially the entry <a
     * href="http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ206">What is the
     * "getThis()" trick?</a>.
     * </p>
     *
     * @param <B> the builder type parameter.
     * @param <C> the resulting connection configuration type parameter.
     */
    public abstract static class Builder<B extends Builder<B, C>, C extends ConnectionConfiguration> {
        private SecurityMode securityMode = SecurityMode.required;
        private DnssecMode dnssecMode = DnssecMode.disabled;
        private KeyManager[] keyManagers;
        private SecureRandom sslContextSecureRandom;
        private String keystorePath;
        private String keystoreType;
        private String pkcs11Library = "pkcs11.config";
        private SslContextFactory sslContextFactory;
        private String[] enabledSSLProtocols;
        private String[] enabledSSLCiphers;
        private HostnameVerifier hostnameVerifier;
        private EntityBareJid authzid;
        private CharSequence username;
        private String password;
        private Resourcepart resource;
        private Locale language = Locale.getDefault();
        private boolean sendPresence = true;
        private ProxyInfo proxy;
        private CallbackHandler callbackHandler;
        private SmackDebuggerFactory debuggerFactory;
        private SocketFactory socketFactory;
        private DomainBareJid xmppServiceDomain;
        private InetAddress hostAddress;
        private DnsName host;
        private UInt16 port = UInt16.from(5222);
        private boolean allowEmptyOrNullUsername = false;
        private boolean saslMechanismsSealed;
        private Set<String> enabledSaslMechanisms;
        private X509TrustManager customX509TrustManager;
        private boolean compressionEnabled = false;
        private StanzaIdSourceFactory stanzaIdSourceFactory = new StandardStanzaIdSource.Factory();

        protected Builder() {
            if (SmackConfiguration.DEBUG) {
                enableDefaultDebugger();
            }
        }

        /**
         * Convenience method to configure the username, password and XMPP service domain.
         *
         * @param jid the XMPP address of the user.
         * @param password the password of the user.
         * @return a reference to this builder.
         * @throws XmppStringprepException in case the XMPP address is not valid.
         * @see #setXmppAddressAndPassword(EntityBareJid, String)
         * @since 4.4.0
         */
        public B setXmppAddressAndPassword(CharSequence jid, String password) throws XmppStringprepException {
            return setXmppAddressAndPassword(JidCreate.entityBareFrom(jid), password);
        }

        /**
         * Convenience method to configure the username, password and XMPP service domain. The localpart of the provided
         * JID is used as username and the domanipart is used as XMPP service domain.
         * <p>
         * Please note that this does and can not configure the client XMPP address. XMPP services are not required to
         * assign bound JIDs where the localpart matches the username and the domainpart matches the verified domainpart.
         * Although most services will follow that pattern.
         * </p>
         *
         * @param jid TODO javadoc me please
         * @param password TODO javadoc me please
         * @return a reference to this builder.
         * @since 4.4.0
         */
        public B setXmppAddressAndPassword(EntityBareJid jid, String password) {
            setUsernameAndPassword(jid.getLocalpart(), password);
            return setXmppDomain(jid.asDomainBareJid());
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
         * Set the XMPP domain. The XMPP domain is what follows after the '@' sign in XMPP addresses (JIDs).
         *
         * @param serviceName the service name
         * @return a reference to this builder.
         * @deprecated use {@link #setXmppDomain(DomainBareJid)} instead.
         */
        @Deprecated
        public B setServiceName(DomainBareJid serviceName) {
            return setXmppDomain(serviceName);
        }

        /**
         * Set the XMPP domain. The XMPP domain is what follows after the '@' sign in XMPP addresses (JIDs).
         *
         * @param xmppDomain the XMPP domain.
         * @return a reference to this builder.
         */
        public B setXmppDomain(DomainBareJid xmppDomain) {
            this.xmppServiceDomain = xmppDomain;
            return getThis();
        }

        /**
         * Set the XMPP domain. The XMPP domain is what follows after the '@' sign in XMPP addresses (JIDs).
         *
         * @param xmppServiceDomain the XMPP domain.
         * @return a reference to this builder.
         * @throws XmppStringprepException if the given string is not a domain bare JID.
         */
        public B setXmppDomain(String xmppServiceDomain) throws XmppStringprepException {
            this.xmppServiceDomain = JidCreate.domainBareFrom(xmppServiceDomain);
            return getThis();
        }

        /**
         * Set the resource we are requesting from the server.
         * <p>
         * If <code>resource</code> is <code>null</code>, the default, then the server will automatically create a
         * resource for the client. Note that XMPP clients only suggest this resource to the server. XMPP servers are
         * allowed to ignore the client suggested resource and instead assign a completely different
         * resource (see RFC 6120 § 7.7.1).
         * </p>
         *
         * @param resource the resource to use.
         * @return a reference to this builder.
         * @see <a href="https://tools.ietf.org/html/rfc6120#section-7.7.1">RFC 6120 § 7.7.1</a>
         */
        public B setResource(Resourcepart resource) {
            this.resource = resource;
            return getThis();
        }

        /**
         * Set the stream language.
         *
         * @param language the language to use.
         * @return a reference to this builder.
         * @see <a href="https://tools.ietf.org/html/rfc6120#section-4.7.4">RFC 6120 § 4.7.4</a>
         * @see <a href="https://www.w3.org/TR/xml/#sec-lang-tag">XML 1.0 § 2.12 Language Identification</a>
         */
        public B setLanguage(Locale language) {
            this.language = language;
            return getThis();
        }

        /**
         * Set the resource we are requesting from the server.
         *
         * @param resource the non-null CharSequence to use a resource.
         * @return a reference ot this builder.
         * @throws XmppStringprepException if the CharSequence is not a valid resourcepart.
         * @see #setResource(Resourcepart)
         */
        public B setResource(CharSequence resource) throws XmppStringprepException {
            Objects.requireNonNull(resource, "resource must not be null");
            return setResource(Resourcepart.from(resource.toString()));
        }

        /**
         * Set the Internet address of the host providing the XMPP service. If set, then this will overwrite anything
         * set via {@link #setHost(CharSequence)}.
         *
         * @param address the Internet address of the host providing the XMPP service.
         * @return a reference to this builder.
         * @since 4.2
         */
        public B setHostAddress(InetAddress address) {
            this.hostAddress = address;
            return getThis();
        }

        /**
         * Set the name of the host providing the XMPP service. This method takes DNS names and
         * IP addresses.
         *
         * @param host the DNS name of the host providing the XMPP service.
         * @return a reference to this builder.
         */
        public B setHost(CharSequence host) {
            String fqdnOrIpString = host.toString();
            if (InetAddressUtil.isIpAddress(fqdnOrIpString)) {
                InetAddress hostInetAddress;
                try {
                    hostInetAddress = InetAddress.getByName(fqdnOrIpString);
                }
                catch (UnknownHostException e) {
                    // Should never happen.
                    throw new AssertionError(e);
                }
                setHostAddress(hostInetAddress);
            } else {
                DnsName dnsName = DnsName.from(fqdnOrIpString);
                setHost(dnsName);
            }
            return getThis();
        }

        /**
         * Set the name of the host providing the XMPP service. Note that this method does only allow DNS names and not
         * IP addresses. Use {@link #setHostAddress(InetAddress)} if you want to explicitly set the Internet address of
         * the host providing the XMPP service.
         *
         * @param host the DNS name of the host providing the XMPP service.
         * @return a reference to this builder.
         */
        public B setHost(DnsName host) {
            this.host = host;
            return getThis();
        }

        /**
         * Set the host to connect to by either its fully qualified domain name (FQDN) or its IP.
         *
         * @param fqdnOrIp a CharSequence either representing the FQDN or the IP of the host.
         * @return a reference to this builder.
         * @see #setHost(DnsName)
         * @see #setHostAddress(InetAddress)
         * @since 4.3.2
         * @deprecated use {@link #setHost(CharSequence)} instead.
         */
        @Deprecated
        // TODO: Remove in Smack 4.5.
        public B setHostAddressByNameOrIp(CharSequence fqdnOrIp) {
            return setHost(fqdnOrIp);
        }

        public B setPort(int port) {
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException(
                        "Port must be a 16-bit unsigned integer (i.e. between 0-65535. Port was: " + port);
            }
            UInt16 portUint16 = UInt16.from(port);
            return setPort(portUint16);
        }

        public B setPort(UInt16 port) {
            this.port = Objects.requireNonNull(port);
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
         * @deprecated set a callback-handler aware {@link KeyManager} via {@link #setKeyManager(KeyManager)} or
         *             {@link #setKeyManagers(KeyManager[])}, created by
         *             {@link #getKeyManagersFrom(String, String, CallbackHandler, String)}, instead.
         */
        // TODO: Remove in Smack 4.6.
        @Deprecated
        public B setCallbackHandler(CallbackHandler callbackHandler) {
            this.callbackHandler = callbackHandler;
            return getThis();
        }

        public B setDnssecMode(DnssecMode dnssecMode) {
            this.dnssecMode = Objects.requireNonNull(dnssecMode, "DNSSEC mode must not be null");
            return getThis();
        }

        public B setCustomX509TrustManager(X509TrustManager x509TrustManager) {
            this.customX509TrustManager = x509TrustManager;
            return getThis();
        }

        /**
         * Sets the TLS security mode used when making the connection. By default,
         * the mode is {@link SecurityMode#required}.
         *
         * @param securityMode the security mode.
         * @return a reference to this builder.
         */
        public B setSecurityMode(SecurityMode securityMode) {
            this.securityMode = securityMode;
            return getThis();
        }

        /**
         * Set the {@link KeyManager}s to initialize the {@link SSLContext} used by Smack to establish the XMPP connection.
         *
         * @param keyManagers an array of {@link KeyManager}s to initialize the {@link SSLContext} with.
         * @return a reference to this builder.
         * @since 4.4.5
         */
        public B setKeyManagers(KeyManager[] keyManagers) {
            this.keyManagers = keyManagers;
            return getThis();
        }

        /**
         * Set the {@link KeyManager}s to initialize the {@link SSLContext} used by Smack to establish the XMPP connection.
         *
         * @param keyManager the {@link KeyManager}s to initialize the {@link SSLContext} with.
         * @return a reference to this builder.
         * @see #setKeyManagers(KeyManager[])
         * @since 4.4.5
         */
        public B setKeyManager(KeyManager keyManager) {
            KeyManager[] keyManagers = new KeyManager[] { keyManager };
            return setKeyManagers(keyManagers);
        }

        /**
         * Set the {@link SecureRandom} used to initialize the {@link SSLContext} used by Smack to establish the XMPP
         * connection. Note that you usually do not need (nor want) to set this. Because if the {@link SecureRandom} is
         * not explicitly set, Smack will initialize the {@link SSLContext} with <code>null</code> as
         * {@link SecureRandom} argument. And all sane {@link SSLContext} implementations will then select a safe secure
         * random source by default.
         *
         * @param secureRandom the {@link SecureRandom} to initialize the {@link SSLContext} with.
         * @return a reference to this builder.
         * @since 4.4.5
         */
        public B setSslContextSecureRandom(SecureRandom secureRandom) {
            this.sslContextSecureRandom = secureRandom;
            return getThis();
        }

        /**
         * Sets the path to the keystore file. The key store file contains the
         * certificates that may be used to authenticate the client to the server,
         * in the event the server requests or requires it.
         *
         * @param keystorePath the path to the keystore file.
         * @return a reference to this builder.
         * @deprecated set a keystore-path aware {@link KeyManager} via {@link #setKeyManager(KeyManager)} or
         *             {@link #setKeyManagers(KeyManager[])}, created by
         *             {@link #getKeyManagersFrom(String, String, CallbackHandler, String)}, instead.
         */
        // TODO: Remove in Smack 4.6.
        @Deprecated
        public B setKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return getThis();
        }

        /**
         * Sets the keystore type.
         *
         * @param keystoreType the keystore type.
         * @return a reference to this builder.
         * @deprecated set a key-type aware {@link KeyManager} via {@link #setKeyManager(KeyManager)} or
         *             {@link #setKeyManagers(KeyManager[])}, created by
         *             {@link #getKeyManagersFrom(String, String, CallbackHandler, String)}, instead.
         */
        // TODO: Remove in Smack 4.6.
        @Deprecated
        public B setKeystoreType(String keystoreType) {
            this.keystoreType = keystoreType;
            return getThis();
        }

        /**
         * Sets the PKCS11 library file location, needed when the
         * Keystore type is PKCS11.
         *
         * @param pkcs11Library the path to the PKCS11 library file.
         * @return a reference to this builder.
         * @deprecated set a PKCS11-library aware {@link KeyManager} via {@link #setKeyManager(KeyManager)} or
         *             {@link #setKeyManagers(KeyManager[])}, created by
         *             {@link #getKeyManagersFrom(String, String, CallbackHandler, String)}, instead.
         */
        // TODO: Remove in Smack 4.6.
        @Deprecated
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
         * @deprecated use {@link #setSslContextFactory(SslContextFactory)} instead}.
         */
        // TODO: Remove in Smack 4.5.
        @Deprecated
        public B setCustomSSLContext(SSLContext context) {
            return setSslContextFactory(() -> {
                return context;
            });
        }

        /**
         * Sets a custom SSLContext for creating SSL sockets.
         * <p>
         * For more information on how to create a SSLContext see <a href=
         * "http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#X509TrustManager"
         * >Java Secure Socket Extension (JSEE) Reference Guide: Creating Your Own X509TrustManager</a>
         *
         * @param sslContextFactory the custom SSLContext for new sockets.
         * @return a reference to this builder.
         */
        public B setSslContextFactory(SslContextFactory sslContextFactory) {
            this.sslContextFactory = Objects.requireNonNull(sslContextFactory, "The provided SslContextFactory must not be null");
            return getThis();
        }

        /**
         * Set the enabled SSL/TLS protocols.
         *
         * @param enabledSSLProtocols TODO javadoc me please
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
         * @param verifier TODO javadoc me please
         * @return a reference to this builder.
         */
        public B setHostnameVerifier(HostnameVerifier verifier) {
            hostnameVerifier = verifier;
            return getThis();
        }

        /**
         * Sets if an initial available presence will be sent to the server. By default
         * an available presence will be sent to the server indicating that this presence
         * is not online and available to receive messages. If you want to log in without
         * being 'noticed' then pass a <code>false</code> value.
         *
         * @param sendPresence true if an initial available presence will be sent while logging in.
         * @return a reference to this builder.
         */
        public B setSendPresence(boolean sendPresence) {
            this.sendPresence = sendPresence;
            return getThis();
        }

        public B enableDefaultDebugger() {
            this.debuggerFactory = SmackConfiguration.getDefaultSmackDebuggerFactory();
            assert this.debuggerFactory != null;
            return getThis();
        }

        /**
         * Set the Smack debugger factory used to construct Smack debuggers.
         *
         * @param debuggerFactory the Smack debugger factory.
         * @return a reference to this builder.
         */
        public B setDebuggerFactory(SmackDebuggerFactory debuggerFactory) {
            this.debuggerFactory = debuggerFactory;
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
         * Set the information about the Proxy used for the connection.
         *
         * @param proxyInfo the Proxy information.
         * @return a reference to this builder.
         */
        public B setProxyInfo(ProxyInfo proxyInfo) {
            this.proxy = proxyInfo;
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

        /**
         * Perform anonymous authentication using SASL ANONYMOUS. Your XMPP service must support this authentication
         * mechanism. This method also calls {@link #addEnabledSaslMechanism(String)} with "ANONYMOUS" as argument.
         *
         * @return a reference to this builder.
         */
        public B performSaslAnonymousAuthentication() {
            if (!SASLAuthentication.isSaslMechanismRegistered(SASLAnonymous.NAME)) {
                throw new IllegalArgumentException("SASL " + SASLAnonymous.NAME + " is not registered");
            }
            throwIfEnabledSaslMechanismsSet();

            allowEmptyOrNullUsernames();
            addEnabledSaslMechanism(SASLAnonymous.NAME);
            saslMechanismsSealed = true;
            return getThis();
        }

        /**
         * Perform authentication using SASL EXTERNAL. Your XMPP service must support this
         * authentication mechanism. This method also calls {@link #addEnabledSaslMechanism(String)} with "EXTERNAL" as
         * argument. It also calls {@link #allowEmptyOrNullUsernames()} and {@link #setSecurityMode(ConnectionConfiguration.SecurityMode)} to
         * {@link SecurityMode#required}.
         *
         * @param sslContext custom SSLContext to be used.
         * @return a reference to this builder.
         */
        public B performSaslExternalAuthentication(SSLContext sslContext) {
            if (!SASLAuthentication.isSaslMechanismRegistered(SASLMechanism.EXTERNAL)) {
                throw new IllegalArgumentException("SASL " + SASLMechanism.EXTERNAL + " is not registered");
            }
            setCustomSSLContext(sslContext);
            throwIfEnabledSaslMechanismsSet();

            allowEmptyOrNullUsernames();
            setSecurityMode(SecurityMode.required);
            addEnabledSaslMechanism(SASLMechanism.EXTERNAL);
            saslMechanismsSealed = true;
            return getThis();
        }

        private void throwIfEnabledSaslMechanismsSet() {
            if (enabledSaslMechanisms != null) {
                throw new IllegalStateException("Enabled SASL mechanisms found");
            }
        }

        /**
         * Add the given mechanism to the enabled ones. See {@link #addEnabledSaslMechanism(Collection)} for a discussion about enabled SASL mechanisms.
         *
         * @param saslMechanism the name of the mechanism to enable.
         * @return a reference to this builder.
         */
        public B addEnabledSaslMechanism(String saslMechanism) {
            return addEnabledSaslMechanism(Arrays.asList(StringUtils.requireNotNullNorEmpty(saslMechanism,
                            "saslMechanism must not be null nor empty")));
        }

        /**
         * Enable the given SASL mechanisms. If you never add a mechanism to the set of enabled ones, <b>all mechanisms
         * known to Smack</b> will be enabled. Only explicitly enable particular SASL mechanisms if you want to limit
         * the used mechanisms to the enabled ones.
         *
         * @param saslMechanisms a collection of names of mechanisms to enable.
         * @return a reference to this builder.
         */
        public B addEnabledSaslMechanism(Collection<String> saslMechanisms) {
            if (saslMechanismsSealed) {
                throw new IllegalStateException("The enabled SASL mechanisms are sealed, you can not add new ones");
            }
            CollectionUtil.requireNotEmpty(saslMechanisms, "saslMechanisms");
            Set<String> blacklistedMechanisms = SASLAuthentication.getBlacklistedSASLMechanisms();
            for (String mechanism : saslMechanisms) {
                if (!SASLAuthentication.isSaslMechanismRegistered(mechanism)) {
                    throw new IllegalArgumentException("SASL " + mechanism + " is not available. Consider registering it with Smack");
                }
                if (blacklistedMechanisms.contains(mechanism)) {
                    throw new IllegalArgumentException("SALS " + mechanism + " is blacklisted.");
                }
            }
            if (enabledSaslMechanisms == null) {
                enabledSaslMechanisms = new HashSet<>(saslMechanisms.size());
            }
            enabledSaslMechanisms.addAll(saslMechanisms);
            return getThis();
        }

        /**
         * Set the XMPP address to be used as authorization identity.
         * <p>
         * In XMPP, authorization identities are bare jids. In general, callers should allow the server to select the
         * authorization identifier automatically, and not call this. Note that setting the authzid does not set the XMPP
         * service domain, which should typically match.
         * Calling this will also SASL CRAM, since this mechanism does not support authzid.
         * </p>
         *
         * @param authzid The BareJid to be requested as the authorization identifier.
         * @return a reference to this builder.
         * @see <a href="http://tools.ietf.org/html/rfc6120#section-6.3.8">RFC 6120 § 6.3.8. Authorization Identity</a>
         * @since 4.2
         */
        public B setAuthzid(EntityBareJid authzid) {
            this.authzid = authzid;
            return getThis();
        }

        /**
         * Sets if the connection is going to use compression (default false).
         *
         * Compression is only activated if the server offers compression. With compression network
         * traffic can be reduced up to 90%. By default compression is disabled.
         *
         * @param compressionEnabled if the connection is going to use compression on the HTTP level.
         * @return a reference to this object.
         */
        public B setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
            return getThis();
        }

        /**
         * Set the factory for stanza ID sources to use.
         *
         * @param stanzaIdSourceFactory the factory for stanza ID sources to use.
         * @return a reference to this builder.
         * @since 4.4
         */
        public B setStanzaIdSourceFactory(StanzaIdSourceFactory stanzaIdSourceFactory) {
            this.stanzaIdSourceFactory = Objects.requireNonNull(stanzaIdSourceFactory);
            return getThis();
        }

        public abstract C build();

        protected abstract B getThis();

        public static KeyManager[] getKeyManagersFrom(String keystoreType, String keystorePath,
                        CallbackHandler callbackHandler, String pkcs11Library)
                        throws NoSuchMethodException, SecurityException, ClassNotFoundException, KeyStoreException,
                        NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, UnsupportedCallbackException, UnrecoverableKeyException {
            KeyManager[] keyManagers = null;
            KeyStore ks = null;
            PasswordCallback pcb = null;

            if ("PKCS11".equals(keystoreType)) {
                    Constructor<?> c = Class.forName("sun.security.pkcs11.SunPKCS11").getConstructor(InputStream.class);
                    String pkcs11Config = "name = SmartCard\nlibrary = " + pkcs11Library;
                    ByteArrayInputStream config = new ByteArrayInputStream(pkcs11Config.getBytes(StandardCharsets.UTF_8));
                    Provider p = (Provider) c.newInstance(config);
                    Security.addProvider(p);
                    ks = KeyStore.getInstance("PKCS11", p);
                    pcb = new PasswordCallback("PKCS11 Password: ", false);
                    callbackHandler.handle(new Callback[] { pcb });
                    ks.load(null, pcb.getPassword());
            } else if ("Apple".equals(keystoreType)) {
                ks = KeyStore.getInstance("KeychainStore", "Apple");
                ks.load(null, null);
                // pcb = new PasswordCallback("Apple Keychain",false);
                // pcb.setPassword(null);
            } else if (keystoreType != null) {
                ks = KeyStore.getInstance(keystoreType);
                if (callbackHandler != null && StringUtils.isNotEmpty(keystorePath)) {
                    pcb = new PasswordCallback("Keystore Password: ", false);
                    callbackHandler.handle(new Callback[] { pcb });
                    ks.load(new FileInputStream(keystorePath), pcb.getPassword());
                } else {
                    InputStream stream = TLSUtils.getDefaultTruststoreStreamIfPossible();
                    try {
                        // Note that PKCS12 keystores need a password one some Java platforms. Hence we try the famous
                        // 'changeit' here. See https://bugs.openjdk.java.net/browse/JDK-8194702
                        char[] password = "changeit".toCharArray();
                        try {
                            ks.load(stream, password);
                        } finally {
                            CloseableUtil.maybeClose(stream);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, "KeyStore load() threw, attempting 'jks' fallback", e);

                        ks = KeyStore.getInstance("jks");
                        // Open the stream again, so that we read it from the beginning.
                        stream = TLSUtils.getDefaultTruststoreStreamIfPossible();
                        try {
                            ks.load(stream, null);
                        } finally {
                            CloseableUtil.maybeClose(stream);
                        }
                    }
                }
            }

            if (ks != null) {
                String keyManagerFactoryAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
                if (kmf != null) {
                    if (pcb == null) {
                        kmf.init(ks, null);
                    } else {
                        kmf.init(ks, pcb.getPassword());
                        pcb.clearPassword();
                    }
                    keyManagers = kmf.getKeyManagers();
                }
            }

            return keyManagers;
        }
    }
}
