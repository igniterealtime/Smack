/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

package org.jivesoftware.smack.sasl;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.util.StringUtils;

import java.io.IOException;

/**
 * Base class for SASL mechanisms. Subclasses must implement three methods:
 * <ul>
 *  <li>{@link #getName()} -- returns the common name of the SASL mechanism.</li>
 *  <li>{@link #getAuthenticationText(String, String, String)} -- authentication text to include
 *      in the initial <tt>auth</tt> stanza.</li>
 *  <li>{@link #getChallengeResponse(byte[])} -- to respond challenges made by the server.</li>
 * </ul>
 *
 * @author Gaston Dombiak
 */
public abstract class SASLMechanism {

    private SASLAuthentication saslAuthentication;

    public SASLMechanism(SASLAuthentication saslAuthentication) {
        super();
        this.saslAuthentication = saslAuthentication;
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param password the password of the user.
     * @throws IOException If a network error occures while authenticating.
     */
    public void authenticate(String username, String host, String password) throws IOException {
        // Build the authentication stanza encoding the authentication text
        StringBuilder stanza = new StringBuilder();
        stanza.append("<auth mechanism=\"").append(getName());
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        String authenticationText = getAuthenticationText(username, host, password);
        if (authenticationText != null) {
            stanza.append(StringUtils.encodeBase64(authenticationText));
        }
        stanza.append("</auth>");

        // Send the authentication to the server
        getSASLAuthentication().send(stanza.toString());
    }

    /**
     * The server is challenging the SASL mechanism for the stanza he just sent. Send a
     * response to the server's challenge.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @throws IOException if an exception sending the response occurs.
     */
    public void challengeReceived(String challenge) throws IOException {
        // Build the challenge response stanza encoding the response text
        StringBuilder stanza = new StringBuilder();
        stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        String authenticationText = getChallengeResponse(StringUtils.decodeBase64(challenge));
        if (authenticationText != null) {
            stanza.append(StringUtils.encodeBase64(authenticationText));
        }
        stanza.append("</response>");

        // Send the authentication to the server
        getSASLAuthentication().send(stanza.toString());
    }

    /**
     * Returns the response text to send answering the challenge sent by the server. Mechanisms
     * that will never receive a challenge may redefine this method returning <tt>null</tt>.
     *
     * @param bytes the challenge sent by the server.
     * @return the response text to send to answer the challenge sent by the server.
     */
    protected abstract String getChallengeResponse(byte[] bytes);

    /**
     * Returns the common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or KERBEROS_V4.
     *
     * @return the common name of the SASL mechanism.
     */
    protected abstract String getName();

    /**
     * Returns the authentication text to include in the initial <tt>auth</tt> stanza
     * or <tt>null</tt> if nothing should be added.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param password the password of the user.
     * @return the authentication text to include in the initial <tt>auth</tt> stanza
     *         or <tt>null</tt> if nothing should be added.
     */
    protected abstract String getAuthenticationText(String username, String host, String password);

    protected SASLAuthentication getSASLAuthentication() {
        return saslAuthentication;
    }
}
