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
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ResourceBindingNotOfferedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLAnonymous;
import org.jivesoftware.smack.sasl.SASLCramMD5Mechanism;
import org.jivesoftware.smack.sasl.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLExternalMechanism;
import org.jivesoftware.smack.sasl.SASLGSSAPIMechanism;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.SASLMechanism.SASLFailure;
import org.jivesoftware.smack.sasl.SASLPlainMechanism;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>This class is responsible authenticating the user using SASL, binding the resource
 * to the connection and establishing a session with the server.</p>
 *
 * <p>Once TLS has been negotiated (i.e. the connection has been secured) it is possible to
 * register with the server, authenticate using Non-SASL or authenticate using SASL. If the
 * server supports SASL then Smack will first try to authenticate using SASL. But if that
 * fails then Non-SASL will be tried.</p>
 *
 * <p>The server may support many SASL mechanisms to use for authenticating. Out of the box
 * Smack provides several SASL mechanisms, but it is possible to register new SASL Mechanisms. Use
 * {@link #registerSASLMechanism(String, Class)} to register a new mechanisms. A registered
 * mechanism wont be used until {@link #supportSASLMechanism(String, int)} is called. By default,
 * the list of supported SASL mechanisms is determined from the {@link SmackConfiguration}. </p>
 *
 * <p>Once the user has been authenticated with SASL, it is necessary to bind a resource for
 * the connection. If no resource is passed in {@link #authenticate(String, String, String)}
 * then the server will assign a resource for the connection. In case a resource is passed
 * then the server will receive the desired resource but may assign a modified resource for
 * the connection.</p>
 *
 * <p>Once a resource has been binded and if the server supports sessions then Smack will establish
 * a session so that instant messaging and presence functionalities may be used.</p>
 *
 * @see org.jivesoftware.smack.sasl.SASLMechanism
 *
 * @author Gaston Dombiak
 * @author Jay Kline
 */
public class SASLAuthentication {

    private static Map<String, Class<? extends SASLMechanism>> implementedMechanisms = new HashMap<String, Class<? extends SASLMechanism>>();
    private static List<String> mechanismsPreferences = new ArrayList<String>();

    private XMPPConnection connection;
    private Collection<String> serverMechanisms = new ArrayList<String>();
    private SASLMechanism currentMechanism = null;
    /**
     * Boolean indicating if SASL negotiation has finished and was successful.
     */
    private boolean saslNegotiated;

    /**
     * The SASL related error condition if there was one provided by the server.
     */
    private SASLFailure saslFailure;

    static {

        // Register SASL mechanisms supported by Smack
        registerSASLMechanism("EXTERNAL", SASLExternalMechanism.class);
        registerSASLMechanism("GSSAPI", SASLGSSAPIMechanism.class);
        registerSASLMechanism("DIGEST-MD5", SASLDigestMD5Mechanism.class);
        registerSASLMechanism("CRAM-MD5", SASLCramMD5Mechanism.class);
        registerSASLMechanism("PLAIN", SASLPlainMechanism.class);
        registerSASLMechanism("ANONYMOUS", SASLAnonymous.class);

        supportSASLMechanism("GSSAPI",0);
        supportSASLMechanism("DIGEST-MD5",1);
        supportSASLMechanism("CRAM-MD5",2);
        supportSASLMechanism("PLAIN",3);
        supportSASLMechanism("ANONYMOUS",4);

    }

    /**
     * Registers a new SASL mechanism
     *
     * @param name   common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     * @param mClass a SASLMechanism subclass.
     */
    public static void registerSASLMechanism(String name, Class<? extends SASLMechanism> mClass) {
        implementedMechanisms.put(name, mClass);
    }

    /**
     * Unregisters an existing SASL mechanism. Once the mechanism has been unregistered it won't
     * be possible to authenticate users using the removed SASL mechanism. It also removes the
     * mechanism from the supported list.
     *
     * @param name common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     */
    public static void unregisterSASLMechanism(String name) {
        implementedMechanisms.remove(name);
        mechanismsPreferences.remove(name);
    }


    /**
     * Registers a new SASL mechanism in the specified preference position. The client will try
     * to authenticate using the most prefered SASL mechanism that is also supported by the server.
     * The SASL mechanism must be registered via {@link #registerSASLMechanism(String, Class)}
     *
     * @param name common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     */
    public static void supportSASLMechanism(String name) {
        mechanismsPreferences.add(0, name);
    }

    /**
     * Registers a new SASL mechanism in the specified preference position. The client will try
     * to authenticate using the most prefered SASL mechanism that is also supported by the server.
     * Use the <tt>index</tt> parameter to set the level of preference of the new SASL mechanism.
     * A value of 0 means that the mechanism is the most prefered one. The SASL mechanism must be
     * registered via {@link #registerSASLMechanism(String, Class)}
     *
     * @param name common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     * @param index preference position amongst all the implemented SASL mechanism. Starts with 0.
     */
    public static void supportSASLMechanism(String name, int index) {
        mechanismsPreferences.add(index, name);
    }

    /**
     * Un-supports an existing SASL mechanism. Once the mechanism has been unregistered it won't
     * be possible to authenticate users using the removed SASL mechanism. Note that the mechanism
     * is still registered, but will just not be used.
     *
     * @param name common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     */
    public static void unsupportSASLMechanism(String name) {
        mechanismsPreferences.remove(name);
    }

    /**
     * Returns the registerd SASLMechanism classes sorted by the level of preference.
     *
     * @return the registerd SASLMechanism classes sorted by the level of preference.
     */
    public static List<Class<? extends SASLMechanism>> getRegisterSASLMechanisms() {
        List<Class<? extends SASLMechanism>> answer = new ArrayList<Class<? extends SASLMechanism>>();
        for (String mechanismsPreference : mechanismsPreferences) {
            answer.add(implementedMechanisms.get(mechanismsPreference));
        }
        return answer;
    }

    SASLAuthentication(XMPPConnection connection) {
        super();
        this.connection = connection;
        this.init();
    }

    /**
     * Returns true if the server offered ANONYMOUS SASL as a way to authenticate users.
     *
     * @return true if the server offered ANONYMOUS SASL as a way to authenticate users.
     */
    public boolean hasAnonymousAuthentication() {
        return serverMechanisms.contains("ANONYMOUS");
    }

    /**
     * Returns true if the server offered SASL authentication besides ANONYMOUS SASL.
     *
     * @return true if the server offered SASL authentication besides ANONYMOUS SASL.
     */
    public boolean hasNonAnonymousAuthentication() {
        return !serverMechanisms.isEmpty() && (serverMechanisms.size() != 1 || !hasAnonymousAuthentication());
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
     * @throws NoResponseException 
     * @throws SASLErrorException 
     * @throws ResourceBindingNotOfferedException
     * @throws NotConnectedException 
     */
    public void authenticate(String resource, CallbackHandler cbh) throws IOException,
                    NoResponseException, XMPPErrorException, SASLErrorException, ResourceBindingNotOfferedException, NotConnectedException {
        // Locate the SASLMechanism to use
        String selectedMechanism = null;
        for (String mechanism : mechanismsPreferences) {
            if (implementedMechanisms.containsKey(mechanism)
                            && serverMechanisms.contains(mechanism)) {
                selectedMechanism = mechanism;
                break;
            }
        }
        if (selectedMechanism != null) {
            // A SASL mechanism was found. Authenticate using the selected mechanism and then
            // proceed to bind a resource
            Class<? extends SASLMechanism> mechanismClass = implementedMechanisms.get(selectedMechanism);

            Constructor<? extends SASLMechanism> constructor;
            try {
                constructor = mechanismClass.getConstructor(SASLAuthentication.class);
                currentMechanism = constructor.newInstance(this);
            }
            catch (Exception e) {
                throw new SaslException("Exception when creating the SASLAuthentication instance",
                                e);
            }

            synchronized (this) {
                // Trigger SASL authentication with the selected mechanism. We use
                // connection.getHost() since GSAPI requires the FQDN of the server, which
                // may not match the XMPP domain.
                currentMechanism.authenticate(connection.getHost(), cbh);
                try {
                    // Wait until SASL negotiation finishes
                    wait(connection.getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (saslFailure != null) {
                // SASL authentication failed and the server may have closed the connection
                // so throw an exception
                throw new SASLErrorException(selectedMechanism, saslFailure);
            }

            if (!saslNegotiated) {
                throw new NoResponseException();
            }
        }
        else {
            throw new SaslException(
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
     * @throws SaslException 
     * @throws SmackException 
     */
    public void authenticate(String username, String password, String resource)
                    throws XMPPErrorException, SASLErrorException, SaslException, IOException,
                    SmackException {
        // Locate the SASLMechanism to use
        String selectedMechanism = null;
        for (String mechanism : mechanismsPreferences) {
            if (implementedMechanisms.containsKey(mechanism)
                            && serverMechanisms.contains(mechanism)) {
                selectedMechanism = mechanism;
                break;
            }
        }
        if (selectedMechanism != null) {
            // A SASL mechanism was found. Authenticate using the selected mechanism and then
            // proceed to bind a resource
            Class<? extends SASLMechanism> mechanismClass = implementedMechanisms.get(selectedMechanism);
            try {
                Constructor<? extends SASLMechanism> constructor = mechanismClass.getConstructor(SASLAuthentication.class);
                currentMechanism = constructor.newInstance(this);
            }
            catch (Exception e) {
                throw new SaslException("Exception when creating the SASLAuthentication instance",
                                e);
            }

            synchronized (this) {
                // Trigger SASL authentication with the selected mechanism. We use
                // connection.getHost() since GSAPI requires the FQDN of the server, which
                // may not match the XMPP domain.

                // The serviceName is basically the value that XMPP server sends to the client as
                // being
                // the location of the XMPP service we are trying to connect to. This should have
                // the
                // format: host ["/" serv-name ] as per RFC-2831 guidelines
                String serviceName = connection.getServiceName();
                currentMechanism.authenticate(username, connection.getHost(), serviceName, password);

                try {
                    // Wait until SASL negotiation finishes
                    wait(connection.getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore
                }

            }

            if (saslFailure != null) {
                // SASL authentication failed and the server may have closed the connection
                // so throw an exception
                throw new SASLErrorException(selectedMechanism, saslFailure);
            }

            if (!saslNegotiated) {
                throw new NoResponseException();
            }
        }
        else {
            throw new SaslException(
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
     * @throws IOException 
     * @throws SaslException 
     * @throws XMPPErrorException if an error occures while authenticating.
     * @throws SmackException if there was no response from the server.
     */
    public void authenticateAnonymously() throws SASLErrorException, SaslException, IOException,
                    SmackException, XMPPErrorException {
        currentMechanism = new SASLAnonymous(this);

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

        if (saslFailure != null) {
            // SASL authentication failed and the server may have closed the connection
            // so throw an exception
            throw new SASLErrorException(currentMechanism.toString(), saslFailure);
        }

        if (!saslNegotiated) {
            throw new NoResponseException();
        }
    }

    /**
     * Sets the available SASL mechanism reported by the server. The server will report the
     * available SASL mechanism once the TLS negotiation was successful. This information is
     * stored and will be used when doing the authentication for logging in the user.
     *
     * @param mechanisms collection of strings with the available SASL mechanism reported
     *                   by the server.
     */
    public void setAvailableSASLMethods(Collection<String> mechanisms) {
        this.serverMechanisms = mechanisms;
    }

    /**
     * Returns true if the user was able to authenticate with the server usins SASL.
     *
     * @return true if the user was able to authenticate with the server usins SASL.
     */
    public boolean isAuthenticated() {
        return saslNegotiated;
    }

    /**
     * The server is challenging the SASL authentication we just sent. Forward the challenge
     * to the current SASLMechanism we are using. The SASLMechanism will send a response to
     * the server. The length of the challenge-response sequence varies according to the
     * SASLMechanism in use.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @throws IOException If a network error occures while authenticating.
     * @throws NotConnectedException 
     */
    public void challengeReceived(String challenge) throws IOException, NotConnectedException {
        currentMechanism.challengeReceived(challenge);
    }

    /**
     * Notification message saying that SASL authentication was successful. The next step
     * would be to bind the resource.
     */
    public void authenticated() {
        saslNegotiated = true;
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
        this.saslFailure = saslFailure;
        // Wake up the thread that is waiting in the #authenticate method
        synchronized (this) {
            notify();
        }
    }

    public void send(Packet stanza) throws NotConnectedException {
        connection.sendPacket(stanza);
    }

    
    /**
     * Initializes the internal state in order to be able to be reused. The authentication
     * is used by the connection at the first login and then reused after the connection
     * is disconnected and then reconnected.
     */
    protected void init() {
        saslNegotiated = false;
        saslFailure = null;
    }
}
