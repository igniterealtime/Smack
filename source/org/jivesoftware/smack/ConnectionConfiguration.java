/**
 * $RCSfile$
 * $Revision: 3306 $
 * $Date: 2006-01-16 14:34:56 -0300 (Mon, 16 Jan 2006) $
 *
 * Copyright 2003-2004 Jive Software.
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

import java.io.File;

/**
 * Configuration to use while establishing the connection to the server. It is possible to
 * configure the path to the trustore file that keeps the trusted CA root certificates and
 * enable or disable all or some of the checkings done while verifying server certificates.<p>
 *
 * It is also possible to configure it TLs, SASL or compression are going to be used or not.
 *
 * @author Gaston Dombiak
 */
public class ConnectionConfiguration implements Cloneable {

    private String serviceName;

    private String host;
    private int port;

    private String truststorePath;
    private String truststoreType;
    private String truststorePassword;
    private boolean tlsEnabled = true;
    private boolean verifyChainEnabled = false;
    private boolean verifyRootCAEnabled = false;
    private boolean selfSignedCertificateEnabled = false;
    private boolean expiredCertificatesCheckEnabled = false;
    private boolean notMatchingDomainCheckEnabled = false;

    private boolean compressionEnabled = false;

    private boolean saslAuthenticationEnabled = true;

    private boolean debuggerEnabled = XMPPConnection.DEBUG_ENABLED;

    public ConnectionConfiguration(String host, int port, String serviceName) {
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;

        // Build the default path to the cacert truststore file. By default we are
        // going to use the file located in $JREHOME/lib/security/cacerts.
        String javaHome =  System.getProperty("java.home");
        StringBuilder buffer = new StringBuilder();
        buffer.append(javaHome).append(File.separator).append("lib");
        buffer.append(File.separator).append("security");
        buffer.append(File.separator).append("cacerts");
        truststorePath = buffer.toString();
        // Set the default store type
        truststoreType = "jks";
        // Set the default password of the cacert file that is "changeit"
        truststorePassword = "changeit";
    }

    public ConnectionConfiguration(String host, int port) {
        this(host, port, host);
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
     * might have been resolved by a DNS lookup as specified by the XMPP spec.
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
     * Returns true if the client is going to try to secure the connection using TLS after
     * the connection has been established.
     *
     * @return true if the client is going to try to secure the connection using TLS after
     *         the connection has been established.
     */
    public boolean isTLSEnabled() {
        return tlsEnabled;
    }

    /**
     * Sets if the client is going to try to secure the connection using TLS after
     * the connection has been established.
     *
     * @param tlsEnabled if the client is going to try to secure the connection using TLS after
     *         the connection has been established.
     */
    public void setTLSEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    /**
     * Retuns the path to the truststore file. The truststore file contains the root
     * certificates of several well?known CAs. By default Smack is going to use
     * the file located in $JREHOME/lib/security/cacerts.
     *
     * @return the path to the truststore file.
     */
    public String getTruststorePath() {
        return truststorePath;
    }

    /**
     * Sets the path to the truststore file. The truststore file contains the root
     * certificates of several well?known CAs. By default Smack is going to use
     * the file located in $JREHOME/lib/security/cacerts.
     *
     * @param truststorePath the path to the truststore file.
     */
    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    /**
     * Returns the password to use to access the truststore file. It is assumed that all
     * certificates share the same password of the truststore file.
     *
     * @return the password to use to access the truststore file.
     */
    public String getTruststorePassword() {
        return truststorePassword;
    }

    /**
     * Sets the password to use to access the truststore file. It is assumed that all
     * certificates share the same password of the truststore file.
     *
     *
     * @param truststorePassword the password to use to access the truststore file.
     */
    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
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
     * Sets if the client is going to use SASL authentication when logging into the
     * server. If SASL authenticatin fails then the client will try to use non-sasl authentication.
     * By default SASL is enabled.
     *
     * @param saslAuthenticationEnabled if the client is going to use SASL authentication when
     *        logging into the server.
     */
    public void setSASLAuthenticationEnabled(boolean saslAuthenticationEnabled) {
        this.saslAuthenticationEnabled = saslAuthenticationEnabled;
    }

    /**
     * Returns true if the new connection about to be establish is going to be debugged. By
     * default the value of {@link XMPPConnection#DEBUG_ENABLED} is used.
     *
     * @return true if the new connection about to be establish is going to be debugged.
     */
    public boolean isDebuggerEnabled() {
        return debuggerEnabled;
    }

    /**
     * Sets if the new connection about to be establish is going to be debugged. By
     * default the value of {@link XMPPConnection#DEBUG_ENABLED} is used.
     *
     * @param debuggerEnabled if the new connection about to be establish is going to be debugged.
     */
    public void setDebuggerEnabled(boolean debuggerEnabled) {
        this.debuggerEnabled = debuggerEnabled;
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
