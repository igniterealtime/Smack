/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.SASLPlainMechanism;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * This class is responsible authenticating the user using SASL, binding the resource
 * to the connection and establishing a session with the server.<p>
 *
 * Once TLS has been negotiated (i.e. the connection has been secured) it is possible to
 * register with the server, authenticate using Non-SASL or authenticate using SASL. If the
 * server supports SASL then Smack will first try to authenticate using SASL. But if that
 * fails then Non-SASL will be tried.<p>
 *
 * The server may support many SASL mechanisms to use for authenticating. Out of the box
 * Smack provides SASL PLAIN but it is possible to register new SASL Mechanisms. Use
 * {@link #registerSASLMechanism(int, String, Class)} to add new mechanisms. See
 * {@link SASLMechanism}.<p>
 *
 * Once the user has been authenticated with SASL, it is necessary to bind a resource for
 * the connection. If no resource is passed in {@link #authenticate(String, String, String)}
 * then the server will assign a resource for the connection. In case a resource is passed
 * then the server will receive the desired resource but may assign a modified resource for
 * the connection.<p>
 *
 * Once a resource has been binded and if the server supports sessions then Smack will establish
 * a session so that instant messaging and presence functionalities may be used.
 *
 * @author Gaston Dombiak
 */
public class SASLAuthentication implements UserAuthentication {

    private static Map implementedMechanisms = new HashMap();
    private static List mechanismsPreferences = new ArrayList();

    private XMPPConnection connection;
    private Collection serverMechanisms = new ArrayList();
    private SASLMechanism currentMechanism = null;
    /**
     * Boolean indicating if SASL negotiation has finished and was successful.
     */
    private boolean saslNegotiated = false;
    private boolean sessionSupported = false;

    static {
        // Register SASL mechanisms supported by Smack
        registerSASLMechanism(0, "PLAIN", SASLPlainMechanism.class);
    }

    /**
     * Registers a new SASL mechanism in the specified preference position. The client will try
     * to authenticate using the most prefered SASL mechanism that is also supported by the server.
     * <p/>
     * <p/>
     * Use the <tt>index</tt> parameter to set the level of preference of the new SASL mechanism.
     * A value of 0 means that the mechanism is the most prefered one.
     *
     * @param index  preference position amongst all the implemented SASL mechanism. Starts with 0.
     * @param name   common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     * @param mClass a SASLMechanism subclass.
     */
    public static void registerSASLMechanism(int index, String name, Class mClass) {
        implementedMechanisms.put(name, mClass);
        mechanismsPreferences.add(index, name);
    }

    /**
     * Unregisters an existing SASL mechanism. Once the mechanism has been unregistered it won't
     * be possible to authenticate users using the removed SASL mechanism.
     *
     * @param name common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     */
    public static void unregisterSASLMechanism(String name) {
        implementedMechanisms.remove(name);
        mechanismsPreferences.remove(name);
    }

    /**
     * Returns the registerd SASLMechanism classes sorted by the level of preference.
     *
     * @return the registerd SASLMechanism classes sorted by the level of preference.
     */
    public static List getRegisterSASLMechanisms() {
        List answer = new ArrayList();
        for (Iterator it = mechanismsPreferences.iterator(); it.hasNext();) {
            answer.add(implementedMechanisms.get(it.next()));
        }
        return answer;
    }

    SASLAuthentication(XMPPConnection connection) {
        super();
        this.connection = connection;
    }

    public String authenticate(String username, String password, String resource)
            throws XMPPException {
        // Locate the SASLMechanism to use
        Class selected = null;
        for (Iterator it = mechanismsPreferences.iterator(); it.hasNext();) {
            String mechanism = (String) it.next();
            if (implementedMechanisms.containsKey(mechanism) &&
                    serverMechanisms.contains(mechanism)) {
                selected = (Class) implementedMechanisms.get(mechanism);
                break;
            }
        }
        if (selected != null) {
            // A SASL mechanism was found. Authenticate using the selected mechanism and then
            // proceed to bind a resource
            try {
                Constructor constructor = selected
                        .getConstructor(new Class[]{SASLAuthentication.class});
                currentMechanism = (SASLMechanism) constructor.newInstance(new Object[]{this});
                // Trigger SASL authentication with the selected mechanism
                currentMechanism.authenticate(username, connection.getServiceName(), password);

                // Wait until SASL negotiation finishes
                synchronized (this) {
                    try {
                        wait(30000);
                    } catch (InterruptedException e) {
                    }
                }

                if (saslNegotiated) {
                    // We now need to bind a resource for the connection
                    // Open a new stream and wait for the response
                    connection.packetWriter.openStream();

                    // Wait until server sends response containing the <bind> element
                    synchronized (this) {
                        try {
                            wait(30000);
                        } catch (InterruptedException e) {
                        }
                    }

                    Bind bindResource = new Bind();
                    bindResource.setResource(resource);

                    PacketCollector collector = connection
                            .createPacketCollector(new PacketIDFilter(bindResource.getPacketID()));
                    // Send the packet
                    connection.sendPacket(bindResource);
                    // Wait up to a certain number of seconds for a response from the server.
                    Bind response = (Bind)collector.nextResult(
                            SmackConfiguration.getPacketReplyTimeout());
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
                        collector = connection
                                .createPacketCollector(new PacketIDFilter(session.getPacketID()));
                        // Send the packet
                        connection.sendPacket(session);
                        // Wait up to a certain number of seconds for a response from the server.
                        IQ ack = (IQ) collector
                                .nextResult(SmackConfiguration.getPacketReplyTimeout());
                        collector.cancel();
                        if (ack == null) {
                            throw new XMPPException("No response from the server.");
                        }
                        // If the server replied with an error, throw an exception.
                        else if (ack.getType() == IQ.Type.ERROR) {
                            throw new XMPPException(ack.getError());
                        }
                    }
                    return userJID;
                } else {
                    // SASL authentication failed so try a Non-SASL authentication
                    return new NonSASLAuthentication(connection)
                            .authenticate(username, password, resource);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // SASL authentication failed so try a Non-SASL authentication
                return new NonSASLAuthentication(connection)
                        .authenticate(username, password, resource);
            }
        } else {
            // No SASL method was found so try a Non-SASL authentication
            return new NonSASLAuthentication(connection).authenticate(username, password, resource);
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
    void setAvailableSASLMethods(Collection mechanisms) {
        this.serverMechanisms = mechanisms;
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
        saslNegotiated = true;
        synchronized (this) {
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
            // Wake up the thread that is waiting in the #authenticate method
            notify();
        }
    }

    public void send(String stanza) throws IOException {
        connection.writer.write(stanza);
        connection.writer.flush();
    }

    /**
     * Notification message saying that the server supports sessions. When a server supports
     * sessions the client needs to send a Session packet after successfully binding a resource
     * for the session.
     */
    void sessionsSupported() {
        sessionSupported = true;
    }
}
