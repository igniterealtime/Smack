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

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.sasl.SASLAnonymous;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Success;

import javax.security.auth.callback.CallbackHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
public class SASLAuthentication {

    private static final Logger LOGGER = Logger.getLogger(SASLAuthentication.class.getName());

    private static final List<SASLMechanism> REGISTERED_MECHANISMS = new ArrayList<SASLMechanism>();

    private static final Set<String> BLACKLISTED_MECHANISMS = new HashSet<String>();

    /**
     * Registers a new SASL mechanism
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
        Map<String, String> answer = new HashMap<String, String>();
        synchronized (REGISTERED_MECHANISMS) {
            for (SASLMechanism mechanism : REGISTERED_MECHANISMS) {
                answer.put(mechanism.getClass().getName(), mechanism.getName());
            }
        }
        return answer;
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

    public static boolean blacklistSASLMechanism(String mechansim) {
        synchronized(BLACKLISTED_MECHANISMS) {
            return BLACKLISTED_MECHANISMS.add(mechansim);
        }
    }

    public static boolean unBlacklistSASLMechanism(String mechanism) {
        synchronized(BLACKLISTED_MECHANISMS) {
            return BLACKLISTED_MECHANISMS.remove(mechanism);
        }
    }

    public static Set<String> getBlacklistedSASLMechanisms() {
        synchronized(BLACKLISTED_MECHANISMS) {
            return new HashSet<String>(BLACKLISTED_MECHANISMS);
        }
    }

    private final AbstractXMPPConnection connection;
    private SASLMechanism currentMechanism = null;

    /**
     * Boolean indicating if SASL negotiation has finished and was successful.
     */
    private boolean authenticationSuccessful;

    /**
     * Either of type {@link SmackException} or {@link SASLErrorException}
     */
    private Exception saslException;

    SASLAuthentication(AbstractXMPPConnection connection) {
        this.connection = connection;
        this.init();
    }

    /**
     * Returns true if the server offered ANONYMOUS SASL as a way to authenticate users.
     *
     * @return true if the server offered ANONYMOUS SASL as a way to authenticate users.
     */
    public boolean hasAnonymousAuthentication() {
        return serverMechanisms().contains("ANONYMOUS");
    }

    /**
     * Returns true if the server offered SASL authentication besides ANONYMOUS SASL.
     *
     * @return true if the server offered SASL authentication besides ANONYMOUS SASL.
     */
    public boolean hasNonAnonymousAuthentication() {
        return !serverMechanisms().isEmpty() && (serverMechanisms().size() != 1 || !hasAnonymousAuthentication());
    }

    /**
     * Performs SASL authentication of the specified user. If SASL authentication was successful
     * then resource binding and session establishment will be performed. This method will return
     * the full JID provided by the server while binding a resource to the connection.<p>
     *
     * The server may assign a full JID with a username or resource different than the requested
     * by this method.
     *
     * @param resource the desired resource.
     * @param cbh the CallbackHandler used to get information from the user
     * @throws IOException 
     * @throws XMPPErrorException 
     * @throws SASLErrorException 
     * @throws SmackException 
     */
    public void authenticate(String resource, CallbackHandler cbh) throws IOException,
                    XMPPErrorException, SASLErrorException, SmackException {
        SASLMechanism selectedMechanism = selectMechanism();
        if (selectedMechanism != null) {
            currentMechanism = selectedMechanism;
            synchronized (this) {
                currentMechanism.authenticate(connection.getHost(), connection.getServiceName(), cbh);
                try {
                    // Wait until SASL negotiation finishes
                    wait(connection.getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore
                }
            }

            maybeThrowException();

            if (!authenticationSuccessful) {
                throw NoResponseException.newWith(connection);
            }
        }
        else {
            throw new SmackException(
                            "SASL Authentication failed. No known authentication mechanisims.");
        }
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
     * @param resource the desired resource.
     * @throws XMPPErrorException 
     * @throws SASLErrorException 
     * @throws IOException 
     * @throws SmackException 
     */
    public void authenticate(String username, String password, String resource)
                    throws XMPPErrorException, SASLErrorException, IOException,
                    SmackException {
        SASLMechanism selectedMechanism = selectMechanism();
        if (selectedMechanism != null) {
            currentMechanism = selectedMechanism;

            synchronized (this) {
                currentMechanism.authenticate(username, connection.getHost(),
                                connection.getServiceName(), password);
                try {
                    // Wait until SASL negotiation finishes
                    wait(connection.getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore
                }
            }

            maybeThrowException();

            if (!authenticationSuccessful) {
                throw NoResponseException.newWith(connection);
            }
        }
        else {
            throw new SmackException(
                            "SASL Authentication failed. No known authentication mechanisims.");
        }
    }

    /**
     * Performs ANONYMOUS SASL authentication. If SASL authentication was successful
     * then resource binding and session establishment will be performed. This method will return
     * the full JID provided by the server while binding a resource to the connection.<p>
     *
     * The server will assign a full JID with a randomly generated resource and possibly with
     * no username.
     *
     * @throws SASLErrorException 
     * @throws XMPPErrorException if an error occures while authenticating.
     * @throws SmackException if there was no response from the server.
     */
    public void authenticateAnonymously() throws SASLErrorException,
                    SmackException, XMPPErrorException {
        currentMechanism = (new SASLAnonymous()).instanceForAuthentication(connection);

        // Wait until SASL negotiation finishes
        synchronized (this) {
            currentMechanism.authenticate(null, null, null, "");
            try {
                wait(connection.getPacketReplyTimeout());
            }
            catch (InterruptedException e) {
                // Ignore
            }
        }

        maybeThrowException();

        if (!authenticationSuccessful) {
            throw NoResponseException.newWith(connection);
        }
    }

    private void maybeThrowException() throws SmackException, SASLErrorException {
        if (saslException != null){
            if (saslException instanceof SmackException) {
                throw (SmackException) saslException;
            } else if (saslException instanceof SASLErrorException) {
                throw (SASLErrorException) saslException;
            } else {
                throw new IllegalStateException("Unexpected exception type" , saslException);
            }
        }
    }

    /**
     * Wrapper for {@link #challengeReceived(String, boolean)}, with <code>finalChallenge</code> set
     * to <code>false</code>.
     * 
     * @param challenge a base64 encoded string representing the challenge.
     * @throws SmackException
     */
    public void challengeReceived(String challenge) throws SmackException {
        challengeReceived(challenge, false);
    }

    /**
     * The server is challenging the SASL authentication we just sent. Forward the challenge
     * to the current SASLMechanism we are using. The SASLMechanism will eventually send a response to
     * the server. The length of the challenge-response sequence varies according to the
     * SASLMechanism in use.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @param finalChallenge true if this is the last challenge send by the server within the success stanza
     * @throws SmackException
     */
    public void challengeReceived(String challenge, boolean finalChallenge) throws SmackException {
        try {
            currentMechanism.challengeReceived(challenge, finalChallenge);
        } catch (SmackException e) {
            authenticationFailed(e);
            throw e;
        }
    }

    /**
     * Notification message saying that SASL authentication was successful. The next step
     * would be to bind the resource.
     * @throws SmackException 
     */
    public void authenticated(Success success) throws SmackException {
        // RFC6120 6.3.10 "At the end of the authentication exchange, the SASL server (the XMPP
        // "receiving entity") can include "additional data with success" if appropriate for the
        // SASL mechanism in use. In XMPP, this is done by including the additional data as the XML
        // character data of the <success/> element." The used SASL mechanism should be able to
        // verify the data send by the server in the success stanza, if any.
        if (success.getData() != null) {
            challengeReceived(success.getData(), true);
        }
        currentMechanism.checkIfSuccessfulOrThrow();
        authenticationSuccessful = true;
        // Wake up the thread that is waiting in the #authenticate method
        synchronized (this) {
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
    public void authenticationFailed(SASLFailure saslFailure) {
        authenticationFailed(new SASLErrorException(currentMechanism.getName(), saslFailure));
    }

    public void authenticationFailed(Exception exception) {
        saslException = exception;
        // Wake up the thread that is waiting in the #authenticate method
        synchronized (this) {
            notify();
        }
    }

    public boolean authenticationSuccessful() {
        return authenticationSuccessful;
    }

    /**
     * Initializes the internal state in order to be able to be reused. The authentication
     * is used by the connection at the first login and then reused after the connection
     * is disconnected and then reconnected.
     */
    protected void init() {
        authenticationSuccessful = false;
        saslException = null;
    }

    private SASLMechanism selectMechanism() {
        // Locate the SASLMechanism to use
        SASLMechanism selectedMechanism = null;
        Iterator<SASLMechanism> it = REGISTERED_MECHANISMS.iterator();
        // Iterate in SASL Priority order over registered mechanisms
        while (it.hasNext()) {
            SASLMechanism mechanism = it.next();
            String mechanismName = mechanism.getName();
            synchronized (BLACKLISTED_MECHANISMS) {
                if (BLACKLISTED_MECHANISMS.contains(mechanismName)) {
                    continue;
                }
            }
            if (serverMechanisms().contains(mechanismName)) {
                // Create a new instance of the SASLMechanism for every authentication attempt.
                selectedMechanism = mechanism.instanceForAuthentication(connection);
                break;
            }
        }
        return selectedMechanism;
    }

    private List<String> serverMechanisms() {
        Mechanisms mechanisms = connection.getFeature(Mechanisms.ELEMENT, Mechanisms.NAMESPACE);
        if (mechanisms == null) {
            LOGGER.warning("Server did not report any SASL mechanisms");
            return Collections.emptyList();
        }
        return mechanisms.getMechanisms();
    }
}
