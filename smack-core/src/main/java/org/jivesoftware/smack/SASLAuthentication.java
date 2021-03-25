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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;
import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.ScramSha1PlusMechanism;
import org.jivesoftware.smack.sasl.packet.SaslNonza;
import org.jivesoftware.smack.sasl.packet.SaslNonza.SASLFailure;
import org.jivesoftware.smack.sasl.packet.SaslNonza.Success;
import org.jivesoftware.smack.util.StringUtils;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;

/**
 * <p>This class is responsible authenticating the user using SASL, binding the resource
 * to the connection and establishing a session with the server.</p>
 *
 * <p>Once TLS has been negotiated (i.e. the connection has been secured) it is possible to
 * register with the server or authenticate using SASL. If the
 * server supports SASL then Smack will try to authenticate using SASL..</p>
 *
 * <p>The server may support many SASL mechanisms to use for authenticating. Out of the box
 * Smack provides several SASL mechanisms, but it is possible to register new SASL Mechanisms. Use
 * {@link #registerSASLMechanism(SASLMechanism)} to register a new mechanisms.
 *
 * @see org.jivesoftware.smack.sasl.SASLMechanism
 *
 * @author Gaston Dombiak
 * @author Jay Kline
 */
public final class SASLAuthentication {

    private static final Logger LOGGER = Logger.getLogger(SASLAuthentication.class.getName());

    private static final List<SASLMechanism> REGISTERED_MECHANISMS = new ArrayList<>();

    private static final Set<String> BLACKLISTED_MECHANISMS = new HashSet<>();

    static {
        // Blacklist SCRAM-SHA-1-PLUS for now.
        blacklistSASLMechanism(ScramSha1PlusMechanism.NAME);
    }

    /**
     * Registers a new SASL mechanism.
     *
     * @param mechanism a SASLMechanism subclass.
     */
    public static void registerSASLMechanism(SASLMechanism mechanism) {
        synchronized (REGISTERED_MECHANISMS) {
            REGISTERED_MECHANISMS.add(mechanism);
            Collections.sort(REGISTERED_MECHANISMS);
        }
    }

    /**
     * Returns the registered SASLMechanism sorted by the level of preference.
     *
     * @return the registered SASLMechanism sorted by the level of preference.
     */
    public static Map<String, String> getRegisterdSASLMechanisms() {
        Map<String, String> answer = new LinkedHashMap<>();
        synchronized (REGISTERED_MECHANISMS) {
            for (SASLMechanism mechanism : REGISTERED_MECHANISMS) {
                answer.put(mechanism.getClass().getName(), mechanism.toString());
            }
        }
        return answer;
    }

    public static boolean isSaslMechanismRegistered(String saslMechanism) {
        synchronized (REGISTERED_MECHANISMS) {
            for (SASLMechanism mechanism : REGISTERED_MECHANISMS) {
                if (mechanism.getName().equals(saslMechanism)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Unregister a SASLMechanism by it's full class name. For example
     * "org.jivesoftware.smack.sasl.javax.SASLCramMD5Mechanism".
     *
     * @param clazz the SASLMechanism class's name
     * @return true if the given SASLMechanism was removed, false otherwise
     */
    public static boolean unregisterSASLMechanism(String clazz) {
        synchronized (REGISTERED_MECHANISMS) {
            Iterator<SASLMechanism> it = REGISTERED_MECHANISMS.iterator();
            while (it.hasNext()) {
                SASLMechanism mechanism = it.next();
                if (mechanism.getClass().getName().equals(clazz)) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean blacklistSASLMechanism(String mechanism) {
        synchronized (BLACKLISTED_MECHANISMS) {
            return BLACKLISTED_MECHANISMS.add(mechanism);
        }
    }

    public static boolean unBlacklistSASLMechanism(String mechanism) {
        synchronized (BLACKLISTED_MECHANISMS) {
            return BLACKLISTED_MECHANISMS.remove(mechanism);
        }
    }

    public static Set<String> getBlacklistedSASLMechanisms() {
        return Collections.unmodifiableSet(BLACKLISTED_MECHANISMS);
    }

    private final AbstractXMPPConnection connection;
    private final ConnectionConfiguration configuration;
    private SASLMechanism currentMechanism = null;

    SASLAuthentication(AbstractXMPPConnection connection, ConnectionConfiguration configuration) {
        this.configuration = configuration;
        this.connection = connection;
    }

    /**
     * Performs SASL authentication of the specified user. If SASL authentication was successful
     * then resource binding and session establishment will be performed. This method will return
     * the full JID provided by the server while binding a resource to the connection.<p>
     *
     * The server may assign a full JID with a username or resource different than the requested
     * by this method.
     *
     * @param username the username that is authenticating with the server.
     * @param password the password to send to the server.
     * @param authzid the authorization identifier (typically null).
     * @param sslSession the optional SSL/TLS session (if one was established)
     * @return the used SASLMechanism.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws SASLErrorException if a SASL protocol error was returned.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackSaslException if a SASL specific error occurred.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NoResponseException if there was no response from the remote entity.
     */
    SASLMechanism authenticate(String username, String password, EntityBareJid authzid, SSLSession sslSession)
                    throws XMPPErrorException, SASLErrorException, IOException,
                    InterruptedException, SmackSaslException, NotConnectedException, NoResponseException {
        final SASLMechanism mechanism = selectMechanism(authzid, password);
        final CallbackHandler callbackHandler = configuration.getCallbackHandler();
        final String host = connection.getHost();
        final DomainBareJid xmppServiceDomain = connection.getXMPPServiceDomain();

        synchronized (this) {
            currentMechanism = mechanism;

            if (callbackHandler != null) {
                currentMechanism.authenticate(host, xmppServiceDomain, callbackHandler, authzid, sslSession);
            }
            else {
                currentMechanism.authenticate(username, host, xmppServiceDomain, password, authzid, sslSession);
            }

            final long deadline = System.currentTimeMillis() + connection.getReplyTimeout();
            while (!mechanism.isFinished()) {
                final long now = System.currentTimeMillis();
                if (now >= deadline) break;
                // Wait until SASL negotiation finishes
                wait(deadline - now);
            }
        }

        mechanism.throwExceptionIfRequired();

        return mechanism;
    }

    /**
     * Wrapper for {@link #challengeReceived(String, boolean)}, with <code>finalChallenge</code> set
     * to <code>false</code>.
     *
     * @param challenge the challenge Nonza.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    void challengeReceived(SaslNonza.Challenge challenge) throws SmackException, InterruptedException {
        challengeReceived(challenge.getData(), false);
    }

    /**
     * The server is challenging the SASL authentication we just sent. Forward the challenge
     * to the current SASLMechanism we are using. The SASLMechanism will eventually send a response to
     * the server. The length of the challenge-response sequence varies according to the
     * SASLMechanism in use.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @param finalChallenge true if this is the last challenge send by the server within the success stanza
     * @throws SmackSaslException if a SASL specific error occurred.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private void challengeReceived(String challenge, boolean finalChallenge) throws SmackSaslException, NotConnectedException, InterruptedException {
        SASLMechanism mechanism;
        synchronized (this) {
            mechanism = currentMechanism;
        }
        mechanism.challengeReceived(challenge, finalChallenge);
    }

    /**
     * Notification message saying that SASL authentication was successful. The next step
     * would be to bind the resource.
     * @param success result of the authentication.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws SmackSaslException if a SASL specific error occurred.
     */
    void authenticated(Success success) throws InterruptedException, SmackSaslException, NotConnectedException {
        // RFC6120 6.3.10 "At the end of the authentication exchange, the SASL server (the XMPP
        // "receiving entity") can include "additional data with success" if appropriate for the
        // SASL mechanism in use. In XMPP, this is done by including the additional data as the XML
        // character data of the <success/> element." The used SASL mechanism should be able to
        // verify the data send by the server in the success stanza, if any.
        if (success.getData() != null) {
            challengeReceived(success.getData(), true);
        }

        // Wake up the thread that is waiting in the #authenticate method
        synchronized (this) {
            currentMechanism.afterFinalSaslChallenge();

            notify();
        }
    }

    /**
     * Notification message saying that SASL authentication has failed. The server may have
     * closed the connection depending on the number of possible retries.
     *
     * @param saslFailure the SASL failure as reported by the server
     * @see <a href="https://tools.ietf.org/html/rfc6120#section-6.5">RFC6120 6.5</a>
     */
    void authenticationFailed(SASLFailure saslFailure) {
        SASLErrorException saslErrorException;
        synchronized (this) {
            saslErrorException = new SASLErrorException(currentMechanism.getName(), saslFailure);
        }
        authenticationFailed(saslErrorException);
    }

    void authenticationFailed(Exception exception) {
        // Wake up the thread that is waiting in the #authenticate method
        synchronized (this) {
            currentMechanism.setException(exception);
            notify();
        }
    }

    public boolean authenticationSuccessful() {
        synchronized (this) {
            if (currentMechanism == null) {
                return false;
            }
            return currentMechanism.isAuthenticationSuccessful();
        }
    }

    String getNameOfLastUsedSaslMechansism() {
        SASLMechanism lastUsedMech = currentMechanism;
        if (lastUsedMech == null) {
            return null;
        }
        return lastUsedMech.getName();
    }

    private SASLMechanism selectMechanism(EntityBareJid authzid, String password) throws SmackException.SmackSaslException {
        final boolean passwordAvailable = StringUtils.isNotEmpty(password);

        Iterator<SASLMechanism> it = REGISTERED_MECHANISMS.iterator();
        final List<String> serverMechanisms = getServerMechanisms();
        if (serverMechanisms.isEmpty()) {
            LOGGER.warning("Server did not report any SASL mechanisms");
        }

        List<String> skipReasons = new ArrayList<>();

        // Iterate in SASL Priority order over registered mechanisms
        while (it.hasNext()) {
            SASLMechanism mechanism = it.next();
            String mechanismName = mechanism.getName();

            if (!serverMechanisms.contains(mechanismName)) {
                continue;
            }

            synchronized (BLACKLISTED_MECHANISMS) {
                if (BLACKLISTED_MECHANISMS.contains(mechanismName)) {
                    continue;
                }
            }

            if (!configuration.isEnabledSaslMechanism(mechanismName)) {
                continue;
            }

            if (authzid != null && !mechanism.authzidSupported()) {
                skipReasons.add("Skipping " + mechanism + " because authzid is required by not supported by this SASL mechanism");
                continue;
            }

            if (mechanism.requiresPassword() && !passwordAvailable) {
                skipReasons.add("Skipping " + mechanism + " because a password is required for it, but none was provided to the connection configuration");
                continue;
            }

            // Create a new instance of the SASLMechanism for every authentication attempt.
            return mechanism.instanceForAuthentication(connection, configuration);
        }

        synchronized (BLACKLISTED_MECHANISMS) {
            // @formatter:off
            throw new SmackException.SmackSaslException(
                            "No supported and enabled SASL Mechanism provided by server. " +
                            "Server announced mechanisms: " + serverMechanisms + ". " +
                            "Registered SASL mechanisms with Smack: " + REGISTERED_MECHANISMS + ". " +
                            "Enabled SASL mechanisms for this connection: " + configuration.getEnabledSaslMechanisms() + ". " +
                            "Blacklisted SASL mechanisms: " + BLACKLISTED_MECHANISMS + ". " +
                            "Skip reasons: " + skipReasons
                            );
            // @formatter;on
        }
    }

    private List<String> getServerMechanisms() {
        Mechanisms mechanisms = connection.getFeature(Mechanisms.class);
        if (mechanisms == null) {
            return Collections.emptyList();
        }
        return mechanisms.getMechanisms();
    }
}
