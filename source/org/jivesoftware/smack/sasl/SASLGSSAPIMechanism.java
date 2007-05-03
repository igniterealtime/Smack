/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
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

import javax.security.sasl.*;
import java.util.*;
import java.io.IOException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.util.Base64;

/**
 * Implementation of the SASL GSSAPI mechanisn
 * 
 *
 * @author Jay Kline
 */
public class SASLGSSAPIMechanism extends SASLMechanism {

    private static final String protocol = "xmpp";
    private static final String[] mechanisms = {"GSSAPI"};    
    private SaslClient sc;

    public SASLGSSAPIMechanism(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
        
        System.setProperty("java.security.krb5.debug","true");
        System.setProperty("javax.security.auth.useSubjectCredsOnly","false");
        System.setProperty("java.security.auth.login.config","gss.conf");

    }

    protected String getName() {
        return "GSSAPI";
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server.
     * This overrides from the abstract class because the initial token
     * needed for GSSAPI is binary, and not safe to put in a string, thus
     * getAuthenticationText() cannot be used.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param password the password of the user (ignored for GSSAPI)
     * @throws IOException If a network error occures while authenticating.
     */
    public void authenticate(String username, String host, String password) throws IOException {
        // Build the authentication stanza encoding the authentication text
        StringBuffer stanza = new StringBuffer();
        Map props = new HashMap();
	
        sc = Sasl.createSaslClient(mechanisms, username, protocol, host, props, null);

	stanza.append("<auth mechanism=\"").append(getName());
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        if(sc.hasInitialResponse()) {
            byte[] response = sc.evaluateChallenge(new byte[0]);
            String authenticationText = Base64.encodeBytes(response,Base64.DONT_BREAK_LINES);
            if(authenticationText != null && !authenticationText.equals("")) {
                stanza.append(authenticationText);
            }
        }
        stanza.append("</auth>");

        // Send the authentication to the server
        getSASLAuthentication().send(stanza.toString());
    }


    protected String getAuthenticationText(String username, String host, String password) {
        // Unused, see authenticate
        return null;
    }

    /**
     * The server is challenging the SASL mechanism for the stanza he just sent. Send a
     * response to the server's challenge. This overrieds from the abstract class because the
     * tokens needed for GSSAPI are binary, and not safe to put in a string, thus 
     * getChallengeResponse() cannot be used.
     *
     * @param challenge a base64 encoded string representing the challenge.
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

    protected String getChallengeResponse(byte[] bytes) {
        // Unused, see challengeReceived
        return null;
    }
}
