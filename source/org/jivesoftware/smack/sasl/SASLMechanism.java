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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.util.Base64;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

/**
 * Base class for SASL mechanisms. Subclasses must implement these methods:
 * <ul>
 *  <li>{@link #getName()} -- returns the common name of the SASL mechanism.</li>
 * </ul>
 * Subclasses will likely want to implement their own versions of these mthods:
 *  <li>{@link #authenticate(String, String, String)} -- Initiate authentication stanza using the
 *  deprecated method.</li>
 *  <li>{@link #authenticate(String, String, CallbackHandler)} -- Initiate authentication stanza
 *  using the CallbackHandler method.</li>
 *  <li>{@link #challengeReceived(String)} -- Handle a challenge from the server.</li>
 * </ul>
 *
 * @author Jay Kline
 */
public abstract class SASLMechanism implements CallbackHandler {

    private SASLAuthentication saslAuthentication;
    protected SaslClient sc;
    protected String authenticationId;
    protected String password;
    protected String hostname;


    public SASLMechanism(SASLAuthentication saslAuthentication) {
        this.saslAuthentication = saslAuthentication;
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. Note that this method of
     * authentication is not recommended, since it is very inflexable.  Use
     * {@link #authenticate(String, String, CallbackHandler)} whenever possible.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param password the password for this account.
     * @throws IOException If a network error occurs while authenticating.
     * @throws XMPPException If a protocol error occurs or the user is not authenticated.
     */
    public void authenticate(String username, String host, String password) throws IOException, XMPPException {
        //Since we were not provided with a CallbackHandler, we will use our own with the given
        //information

        //Set the authenticationID as the username, since they must be the same in this case.
        this.authenticationId = username;
        this.password = password;
        this.hostname = host;

        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String,String>();
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", host, props, this);
        authenticate();
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. The callback handler will handle
     * any additional information, such as the authentication ID or realm, if it is needed.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param cbh      the CallbackHandler to obtain user information.
     * @throws IOException If a network error occures while authenticating.
     * @throws XMPPException If a protocol error occurs or the user is not authenticated.
     */
    public void authenticate(String username, String host, CallbackHandler cbh) throws IOException, XMPPException {
        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String,String>();
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", host, props, cbh);
        authenticate();
    }

    protected void authenticate() throws IOException, XMPPException {
        StringBuffer stanza = new StringBuffer();
        stanza.append("<auth mechanism=\"").append(getName());
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        try {
            if(sc.hasInitialResponse()) {
                byte[] response = sc.evaluateChallenge(new byte[0]);
                String authenticationText = Base64.encodeBytes(response,Base64.DONT_BREAK_LINES);
                if(authenticationText != null && !authenticationText.equals("")) {                 
                    stanza.append(authenticationText);
                }
            }
        } catch (SaslException e) {
            throw new XMPPException("SASL authentication failed", e);
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
        StringBuffer stanza = new StringBuffer();

        byte response[];
        if(challenge != null) {
            response = sc.evaluateChallenge(Base64.decode(challenge));
        } else {
            response = sc.evaluateChallenge(null);
        }

        String authenticationText = Base64.encodeBytes(response,Base64.DONT_BREAK_LINES);
        if(authenticationText.equals("")) {
            authenticationText = "=";
        }

        stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        stanza.append(authenticationText);
        stanza.append("</response>");

        // Send the authentication to the server
        getSASLAuthentication().send(stanza.toString());
    }

    /**
     * Returns the common name of the SASL mechanism. E.g.: PLAIN, DIGEST-MD5 or GSSAPI.
     *
     * @return the common name of the SASL mechanism.
     */
    protected abstract String getName();


    protected SASLAuthentication getSASLAuthentication() {
        return saslAuthentication;
    }

    /**
     * 
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback ncb = (NameCallback)callbacks[i];
                ncb.setName(authenticationId);
            } else if(callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback)callbacks[i];
                pcb.setPassword(password.toCharArray());
            } else if(callbacks[i] instanceof RealmCallback) {
                RealmCallback rcb = (RealmCallback)callbacks[i];
                rcb.setText(hostname);
            } else if(callbacks[i] instanceof RealmChoiceCallback){
                //unused
                //RealmChoiceCallback rccb = (RealmChoiceCallback)callbacks[i];
            } else {
               throw new UnsupportedCallbackException(callbacks[i]);
            }
         }
    }
}
