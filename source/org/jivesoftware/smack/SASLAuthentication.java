/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.sasl.*;

import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

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
public class SASLAuthentication implements UserAuthentication {

    private static Map<String, Class<? extends SASLMechanism>> implementedMechanisms = new HashMap<String, Class<? extends SASLMechanism>>();
    private static List<String> mechanismsPreferences = new ArrayList<String>();

    private Connection connection;
    private Collection<String> serverMechanisms = new ArrayList<String>();
    private SASLMechanism currentMechanism = null;
    /**
     * Boolean indicating if SASL negotiation has finished and was successful.
     */
    private boolean saslNegotiated;
    /**
     * Boolean indication if SASL authentication has failed. When failed the server may end
     * the connection.
     */
    private boolean saslFailed;
    private boolean resourceBinded;
    private boolean sessionSupported;
    /**
     * The SASL related error condition if there was one provided by the server.
     */
    private String errorCondition;

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

    SASLAuthentication(Connection connection) {
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
     * @param username the username that is authenticating with the server.
     * @param resource the desired resource.
     * @param cbh the CallbackHandler used to get information from the user
     * @return the full JID provided by the server while binding a resource to the connection.
     * @throws XMPPException if an error occures while authenticating.
     */
    public String authenticate(String username, String resource, CallbackHandler cbh) 
            throws XMPPException {
        // Locate the SASLMechanism to use
        String selectedMechanism = null;
        for (String mechanism : mechanismsPreferences) {
            if (implementedMechanisms.containsKey(mechanism) &&
                    serverMechanisms.contains(mechanism)) {
                selectedMechanism = mechanism;
                break;
            }
        }
        if (selectedMechanism != null) {
            // A SASL mechanism was found. Authenticate using the selected mechanism and then
            // proceed to bind a resource
            try {
                Class<? extends SASLMechanism> mechanismClass = implementedMechanisms.get(selectedMechanism);
                Constructor<? extends SASLMechanism> constructor = mechanismClass.getConstructor(SASLAuthentication.class);
                currentMechanism = constructor.newInstance(this);
                // Trigger SASL authentication with the selected mechanism. We use
                // connection.getHost() since GSAPI requires the FQDN of the server, which
                // may not match the XMPP domain.
                currentMechanism.authenticate(username, connection.getHost(), cbh);

                // Wait until SASL negotiation finishes
                synchronized (this) {
                    if (!saslNegotiated && !saslFailed) {
                        try {
                            wait(30000);
                        }
                        catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                }

                if (saslFailed) {
                    // SASL authentication failed and the server may have closed the connection
                    // so throw an exception
                    if (errorCondition != null) {
                        throw new XMPPException("SASL authentication " +
                                selectedMechanism + " failed: " + errorCondition);
                    }
                    else {
                        throw new XMPPException("SASL authentication failed using mechanism " +
                                selectedMechanism);
                    }
                }

                if (saslNegotiated) {
                    // Bind a resource for this connection and
                    return bindResourceAndEstablishSession(resource);
                } else {
                    // SASL authentication failed
                }
            }
            catch (XMPPException e) {
                throw e;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            throw new XMPPException("SASL Authentication failed. No known authentication mechanisims.");
        }
        throw new XMPPException("SASL authentication failed");
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
     * @return the full JID provided by the server while binding a resource to the connection.
     * @throws XMPPException if an error occures while authenticating.
     */
    public String authenticate(String username, String password, String resource)
            throws XMPPException {
        // Locate the SASLMechanism to use
        String selectedMechanism = null;
        for (String mechanism : mechanismsPreferences) {
            if (implementedMechanisms.containsKey(mechanism) &&
                    serverMechanisms.contains(mechanism)) {
                selectedMechanism = mechanism;
                break;
            }
        }
        if (selectedMechanism != null) {
            // A SASL mechanism was found. Authenticate using the selected mechanism and then
            // proceed to bind a resource
            try {
                Class<? extends SASLMechanism> mechanismClass = implementedMechanisms.get(selectedMechanism);
                Constructor<? extends SASLMechanism> constructor = mechanismClass.getConstructor(SASLAuthentication.class);
                currentMechanism = constructor.newInstance(this);
                // Trigger SASL authentication with the selected mechanism. We use
                // connection.getHost() since GSAPI requires the FQDN of the server, which
                // may not match the XMPP domain.
                currentMechanism.authenticate(username, connection.getServiceName(), password);

                // Wait until SASL negotiation finishes
                synchronized (this) {
                    if (!saslNegotiated && !saslFailed) {
                        try {
                            wait(30000);
                        }
                        catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                }

                if (saslFailed) {
                    // SASL authentication failed and the server may have closed the connection
                    // so throw an exception
                    if (errorCondition != null) {
                        throw new XMPPException("SASL authentication " +
                                selectedMechanism + " failed: " + errorCondition);
                    }
                    else {
                        throw new XMPPException("SASL authentication failed using mechanism " +
                                selectedMechanism);
                    }
                }

                if (saslNegotiated) {
                    // Bind a resource for this connection and
                    return bindResourceAndEstablishSession(resource);
                }
                else {
                    // SASL authentication failed so try a Non-SASL authentication
                    return new NonSASLAuthentication(connection)
                            .authenticate(username, password, resource);
                }
            }
            catch (XMPPException e) {
                throw e;
            }
            catch (Exception e) {
                e.printStackTrace();
                // SASL authentication failed so try a Non-SASL authentication
                return new NonSASLAuthentication(connection)
                        .authenticate(username, password, resource);
            }
        }
        else {
            // No SASL method was found so try a Non-SASL authentication
            return new NonSASLAuthentication(connection).authenticate(username, password, resource);
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
     * @return the full JID provided by the server while binding a resource to the connection.
     * @throws XMPPException if an error occures while authenticating.
     */
    public String authenticateAnonymously() throws XMPPException {
        try {
            currentMechanism = new SASLAnonymous(this);
            currentMechanism.authenticate(null,null,"");

            // Wait until SASL negotiation finishes
            synchronized (this) {
                if (!saslNegotiated && !saslFailed) {
                    try {
                        wait(5000);
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }

            if (saslFailed) {
                // SASL authentication failed and the server may have closed the connection
                // so throw an exception
                if (errorCondition != null) {
                    throw new XMPPException("SASL authentication failed: " + errorCondition);
                }
                else {
                    throw new XMPPException("SASL authentication failed");
                }
            }

            if (saslNegotiated) {
                // Bind a resource for this connection and
                return bindResourceAndEstablishSession(null);
            }
            else {
                return new NonSASLAuthentication(connection).authenticateAnonymously();
            }
        } catch (IOException e) {
            return new NonSASLAuthentication(connection).authenticateAnonymously();
        }
    }

    private String bindResourceAndEstablishSession(String resource) throws XMPPException {
        // Wait until server sends response containing the <bind> element
        synchronized (this) {
            if (!resourceBinded) {
                try {
                    wait(30000);
                }
                catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        if (!resourceBinded) {
            // Server never offered resource binding
            throw new XMPPException("Resource binding not offered by server");
        }

        Bind bindResource = new Bind();
        bindResource.setResource(resource);

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(bindResource.getPacketID()));
        // Send the packet
        connection.sendPacket(bindResource);
        // Wait up to a certain number of seconds for a response from the server.
        Bind response = (Bind) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        String userJID = response.getJid();

        if (sessionSupported) {
            Session session = new Session();
            collector = connection.createPacketCollector(new PacketIDFilter(session.getPacketID()));
            // Send the packet
            connection.sendPacket(session);
            // Wait up to a certain number of seconds for a response from the server.
            IQ ack = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
            collector.cancel();
            if (ack == null) {
                throw new XMPPException("No response from the server.");
            }
            // If the server replied with an error, throw an exception.
            else if (ack.getType() == IQ.Type.ERROR) {
                throw new XMPPException(ack.getError());
            }
        }
        else {
            // Server never offered session establishment
            throw new XMPPException("Session establishment not offered by server");
        }
        return userJID;
    }

    /**
     * Sets the available SASL mechanism reported by the server. The server will report the
     * available SASL mechanism once the TLS negotiation was successful. This information is
     * stored and will be used when doing the authentication for logging in the user.
     *
     * @param mechanisms collection of strings with the available SASL mechanism reported
     *                   by the server.
     */
    void setAvailableSASLMethods(Collection<String> mechanisms) {
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
     */
    void challengeReceived(String challenge) throws IOException {
        currentMechanism.challengeReceived(challenge);
    }

    /**
     * Notification message saying that SASL authentication was successful. The next step
     * would be to bind the resource.
     */
    void authenticated() {
        synchronized (this) {
            saslNegotiated = true;
            // Wake up the thread that is waiting in the #authenticate method
            notify();
        }
    }

    /**
     * Notification message saying that SASL authentication has failed. The server may have
     * closed the connection depending on the number of possible retries.
     * 
     * @deprecated replaced by {@see #authenticationFailed(String)}.
     */
    void authenticationFailed() {
        authenticationFailed(null);
    }

    /**
     * Notification message saying that SASL authentication has failed. The server may have
     * closed the connection depending on the number of possible retries.
     * 
     * @param condition the error condition provided by the server.
     */
    void authenticationFailed(String condition) {
        synchronized (this) {
            saslFailed = true;
            errorCondition = condition;
            // Wake up the thread that is waiting in the #authenticate method
            notify();
        }
    }

    /**
     * Notification message saying that the server requires the client to bind a
     * resource to the stream.
     */
    void bindingRequired() {
        synchronized (this) {
            resourceBinded = true;
            // Wake up the thread that is waiting in the #authenticate method
            notify();
        }
    }

    public void send(Packet stanza) {
        connection.sendPacket(stanza);
    }

    /**
     * Notification message saying that the server supports sessions. When a server supports
     * sessions the client needs to send a Session packet after successfully binding a resource
     * for the session.
     */
    void sessionsSupported() {
        sessionSupported = true;
    }
    
    /**
     * Initializes the internal state in order to be able to be reused. The authentication
     * is used by the connection at the first login and then reused after the connection
     * is disconnected and then reconnected.
     */
    protected void init() {
        saslNegotiated = false;
        saslFailed = false;
        resourceBinded = false;
        sessionSupported = false;
    }
}