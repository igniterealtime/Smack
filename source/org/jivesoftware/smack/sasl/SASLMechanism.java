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
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

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
        String authenticationText = null;
        try {
            if(sc.hasInitialResponse()) {
                byte[] response = sc.evaluateChallenge(new byte[0]);
                authenticationText = StringUtils.encodeBase64(response, false);
            }
        } catch (SaslException e) {
            throw new XMPPException("SASL authentication failed", e);
        }

        // Send the authentication to the server
        getSASLAuthentication().send(new AuthMechanism(getName(), authenticationText));
    }


    /**
     * The server is challenging the SASL mechanism for the stanza he just sent. Send a
     * response to the server's challenge.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @throws IOException if an exception sending the response occurs.
     */
    public void challengeReceived(String challenge) throws IOException {
        byte response[];
        if(challenge != null) {
            response = sc.evaluateChallenge(StringUtils.decodeBase64(challenge));
        } else {
            response = sc.evaluateChallenge(new byte[0]);
        }

        Packet responseStanza;
        if (response == null) {
            responseStanza = new Response();
        }
        else {
            responseStanza = new Response(StringUtils.encodeBase64(response, false));
        }

        // Send the authentication to the server
        getSASLAuthentication().send(responseStanza);
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

    /**
     * Initiating SASL authentication by select a mechanism.
     */
    public class AuthMechanism extends Packet {
        final private String name;
        final private String authenticationText;

        public AuthMechanism(String name, String authenticationText) {
            if (name == null) {
                throw new NullPointerException("SASL mechanism name shouldn't be null.");
            }
            this.name = name;
            this.authenticationText = authenticationText;
        }

        public String toXML() {
            StringBuilder stanza = new StringBuilder();
            stanza.append("<auth mechanism=\"").append(name);
            stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            if (authenticationText != null &&
                    authenticationText.trim().length() > 0) {
                stanza.append(authenticationText);
            }
            stanza.append("</auth>");
            return stanza.toString();
        }
    }

    /**
     * A SASL challenge stanza.
     */
    public static class Challenge extends Packet {
        final private String data;

        public Challenge(String data) {
            this.data = data;
        }

        public String toXML() {
            StringBuilder stanza = new StringBuilder();
            stanza.append("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            if (data != null &&
                    data.trim().length() > 0) {
                stanza.append(data);
            }
            stanza.append("</challenge>");
            return stanza.toString();
        }
    }

    /**
     * A SASL response stanza.
     */
    public class Response extends Packet {
        final private String authenticationText;

        public Response() {
            authenticationText = null;
        }

        public Response(String authenticationText) {
            if (authenticationText == null || authenticationText.trim().length() == 0) {
                this.authenticationText = null;
            }
            else {
                this.authenticationText = authenticationText;
            }
        }

        public String toXML() {
            StringBuilder stanza = new StringBuilder();
            stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            if (authenticationText != null) {
                stanza.append(authenticationText);
            }
            stanza.append("</response>");
            return stanza.toString();
        }
    }

    /**
     * A SASL success stanza.
     */
    public static class Success extends Packet {
        final private String data;

        public Success(String data) {
            this.data = data;
        }

        public String toXML() {
            StringBuilder stanza = new StringBuilder();
            stanza.append("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            if (data != null &&
                    data.trim().length() > 0) {
                stanza.append(data);
            }
            stanza.append("</success>");
            return stanza.toString();
        }
    }

    /**
     * A SASL failure stanza.
     */
    public static class Failure extends Packet {
        final private String condition;

        public Failure(String condition) {
            this.condition = condition;
        }

        /**
         * Get the SASL related error condition.
         * 
         * @return the SASL related error condition.
         */
        public String getCondition() {
            return condition;
        }

        public String toXML() {
            StringBuilder stanza = new StringBuilder();
            stanza.append("<failure xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            if (condition != null &&
                    condition.trim().length() > 0) {
                stanza.append("<").append(condition).append("/>");
            }
            stanza.append("</failure>");
            return stanza.toString();
        }
    }
}
