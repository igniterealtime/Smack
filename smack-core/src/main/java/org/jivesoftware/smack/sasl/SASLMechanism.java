/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
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
package org.jivesoftware.smack.sasl;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.AuthMechanism;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Response;
import org.jivesoftware.smack.util.StringTransformer;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jxmpp.jid.DomainBareJid;

import javax.security.auth.callback.CallbackHandler;

/**
 * Base class for SASL mechanisms. Subclasses must implement these methods:
 * <ul>
 *  <li>{@link #getName()} -- returns the common name of the SASL mechanism.</li>
 * </ul>
 * Subclasses will likely want to implement their own versions of these methods:
 *  <li>{@link #authenticate(String, String, DomainBareJid, String)} -- Initiate authentication stanza using the
 *  deprecated method.</li>
 *  <li>{@link #authenticate(String, DomainBareJid, CallbackHandler)} -- Initiate authentication stanza
 *  using the CallbackHandler method.</li>
 *  <li>{@link #challengeReceived(String, boolean)} -- Handle a challenge from the server.</li>
 * </ul>
 * 
 * Basic XMPP SASL authentication steps:
 * 1. Client authentication initialization, stanza sent to the server (Base64 encoded): 
 *    <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5'/>
 * 2. Server sends back to the client the challenge response (Base64 encoded)
 *    sample: 
 *    realm=<sasl server realm>,nonce="OA6MG9tEQGm2hh",qop="auth",charset=utf-8,algorithm=md5-sess
 * 3. The client responds back to the server (Base 64 encoded):
 *    sample:
 *    username=<userid>,realm=<sasl server realm from above>,nonce="OA6MG9tEQGm2hh",
 *    cnonce="OA6MHXh6VqTrRk",nc=00000001,qop=auth,
 *    digest-uri=<digesturi>,
 *    response=d388dad90d4bbd760a152321f2143af7,
 *    charset=utf-8,
 *    authzid=<id>
 * 4. The server evaluates if the user is present and contained in the REALM
 *    if successful it sends: <response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/> (Base64 encoded)
 *    if not successful it sends:
 *    sample:
 *    <challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>
 *        cnNwYXV0aD1lYTQwZjYwMzM1YzQyN2I1NTI3Yjg0ZGJhYmNkZmZmZA==
 *    </challenge>
 *
 * @author Jay Kline
 */
public abstract class SASLMechanism implements Comparable<SASLMechanism> {

    public static final String CRAMMD5 = "CRAM-MD5";
    public static final String DIGESTMD5 = "DIGEST-MD5";
    public static final String EXTERNAL = "EXTERNAL";
    public static final String GSSAPI = "GSSAPI";
    public static final String PLAIN = "PLAIN";

    // TODO Remove once Smack's min Android API is 9, where java.text.Normalizer is available
    private static StringTransformer saslPrepTransformer;

    /**
     * Set the SASLPrep StringTransformer.
     * <p>
     * A simple SASLPrep StringTransformer would be for example: <code>java.text.Normalizer.normalize(string, Form.NFKC);</code>
     * </p>
     * 
     * @param stringTransformer set StringTransformer to use for SASLPrep.
     * @see <a href="http://tools.ietf.org/html/rfc4013">RFC 4013 - SASLprep: Stringprep Profile for User Names and Passwords</a>
     */
    public static void setSaslPrepTransformer(StringTransformer stringTransformer) {
        saslPrepTransformer = stringTransformer;
    }

    protected XMPPConnection connection;

    /**
     * Then authentication identity (authcid). RFC 6120 § 6.3.7 informs us that some SASL mechanisms use this as a
     * "simple user name". But the exact form is a matter of the mechanism and that it does not necessarily map to an
     * localpart. But it usually is the localpart of the client JID, although sometimes other formats are used (e.g. the
     * full JID).
     * <p>
     * Not to be confused with the authzid (see RFC 6120 § 6.3.8).
     * </p>
     */
    protected String authenticationId;

    /**
     * The name of the XMPP service
     */
    protected DomainBareJid serviceName;

    /**
     * The users password
     */
    protected String password;
    protected String host;

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. Note that this method of
     * authentication is not recommended, since it is very inflexible. Use
     * {@link #authenticate(String, DomainBareJid, CallbackHandler)} whenever possible.
     * 
     * Explanation of auth stanza:
     * 
     * The client authentication stanza needs to include the digest-uri of the form: xmpp/serviceName 
     * From RFC-2831: 
     * digest-uri = "digest-uri" "=" digest-uri-value
     * digest-uri-value = serv-type "/" host [ "/" serv-name ]
     * 
     * digest-uri: 
     * Indicates the principal name of the service with which the client 
     * wishes to connect, formed from the serv-type, host, and serv-name. 
     * For example, the FTP service
     * on "ftp.example.com" would have a "digest-uri" value of "ftp/ftp.example.com"; the SMTP
     * server from the example above would have a "digest-uri" value of
     * "smtp/mail3.example.com/example.com".
     * 
     * host:
     * The DNS host name or IP address for the service requested. The DNS host name
     * must be the fully-qualified canonical name of the host. The DNS host name is the
     * preferred form; see notes on server processing of the digest-uri.
     * 
     * serv-name: 
     * Indicates the name of the service if it is replicated. The service is
     * considered to be replicated if the client's service-location process involves resolution
     * using standard DNS lookup operations, and if these operations involve DNS records (such
     * as SRV, or MX) which resolve one DNS name into a set of other DNS names. In this case,
     * the initial name used by the client is the "serv-name", and the final name is the "host"
     * component. For example, the incoming mail service for "example.com" may be replicated
     * through the use of MX records stored in the DNS, one of which points at an SMTP server
     * called "mail3.example.com"; it's "serv-name" would be "example.com", it's "host" would be
     * "mail3.example.com". If the service is not replicated, or the serv-name is identical to
     * the host, then the serv-name component MUST be omitted
     * 
     * digest-uri verification is needed for ejabberd 2.0.3 and higher   
     * 
     * @param username the username of the user being authenticated.
     * @param host the hostname where the user account resides.
     * @param serviceName the xmpp service location - used by the SASL client in digest-uri creation
     * serviceName format is: host [ "/" serv-name ] as per RFC-2831
     * @param password the password for this account.
     * @throws SmackException If a network error occurs while authenticating.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public final void authenticate(String username, String host, DomainBareJid serviceName, String password)
                    throws SmackException, NotConnectedException, InterruptedException {
        this.authenticationId = username;
        this.host = host;
        this.serviceName = serviceName;
        this.password = password;
        authenticateInternal();
        authenticate();
    }

    protected void authenticateInternal() throws SmackException {
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. The callback handler will handle
     * any additional information, such as the authentication ID or realm, if it is needed.
     *
     * @param host     the hostname where the user account resides.
     * @param serviceName the xmpp service location
     * @param cbh      the CallbackHandler to obtain user information.
     * @throws SmackException
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void authenticate(String host, DomainBareJid serviceName, CallbackHandler cbh)
                    throws SmackException, NotConnectedException, InterruptedException {
        this.host = host;
        this.serviceName = serviceName;
        authenticateInternal(cbh);
        authenticate();
    }

    protected abstract void authenticateInternal(CallbackHandler cbh) throws SmackException;

    private final void authenticate() throws SmackException, NotConnectedException, InterruptedException {
        byte[] authenticationBytes = getAuthenticationText();
        String authenticationText;
        if (authenticationBytes != null) {
            authenticationText = Base64.encodeToString(authenticationBytes);
        } else {
            // RFC6120 6.4.2 "If the initiating entity needs to send a zero-length initial response,
            // it MUST transmit the response as a single equals sign character ("="), which
            // indicates that the response is present but contains no data."
            authenticationText = "=";
        }
        // Send the authentication to the server
        connection.send(new AuthMechanism(getName(), authenticationText));
    }

    /**
     * Should return the initial response of the SASL mechanism. The returned byte array will be
     * send base64 encoded to the server. SASL mechanism are free to return <code>null</code> here.
     * 
     * @return the initial response or null
     * @throws SmackException
     */
    protected abstract byte[] getAuthenticationText() throws SmackException;

    /**
     * The server is challenging the SASL mechanism for the stanza he just sent. Send a
     * response to the server's challenge.
     *
     * @param challengeString a base64 encoded string representing the challenge.
     * @param finalChallenge true if this is the last challenge send by the server within the success stanza
     * @throws NotConnectedException
     * @throws SmackException
     * @throws InterruptedException 
     */
    public final void challengeReceived(String challengeString, boolean finalChallenge) throws SmackException, NotConnectedException, InterruptedException {
        byte[] challenge = Base64.decode(challengeString);
        byte[] response = evaluateChallenge(challenge);
        if (finalChallenge) {
            return;
        }

        Response responseStanza;
        if (response == null) {
            responseStanza = new Response();
        }
        else {
            responseStanza = new Response(Base64.encodeToString(response));
        }

        // Send the authentication to the server
        connection.send(responseStanza);
    }

    protected byte[] evaluateChallenge(byte[] challenge) throws SmackException {
        return null;
    }

    public final int compareTo(SASLMechanism other) {
        return getPriority() - other.getPriority();
    }

    /**
     * Returns the common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or GSSAPI.
     *
     * @return the common name of the SASL mechanism.
     */
    public abstract String getName();

    public abstract int getPriority();

    public abstract void checkIfSuccessfulOrThrow() throws SmackException;

    public SASLMechanism instanceForAuthentication(XMPPConnection connection) {
        SASLMechanism saslMechansim = newInstance();
        saslMechansim.connection = connection;
        return saslMechansim;
    }

    protected abstract SASLMechanism newInstance();

    protected static byte[] toBytes(String string) {
        return StringUtils.toBytes(string);
    }

    /**
     * SASLprep the given String.
     * 
     * @param string the String to sasl prep.
     * @return the given String SASL preped
     * @see <a href="http://tools.ietf.org/html/rfc4013">RFC 4013 - SASLprep: Stringprep Profile for User Names and Passwords</a>
     */
    protected static String saslPrep(String string) {
        StringTransformer stringTransformer = saslPrepTransformer;
        if (stringTransformer != null) {
            return stringTransformer.transform(string);
        }
        return string;
    }
}
