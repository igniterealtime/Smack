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

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import javax.security.sasl.Sasl;
import javax.security.auth.callback.CallbackHandler;

/**
 * Implementation of the SASL GSSAPI mechanism
 *
 * @author Jay Kline
 */
public class SASLGSSAPIMechanism extends SASLMechanism {

    public SASLGSSAPIMechanism(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);

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
     * @param cbh      the CallbackHandler (not used with GSSAPI)
     * @throws IOException If a network error occures while authenticating.
     */
    public void authenticate(String username, String host, CallbackHandler cbh) throws IOException, XMPPException {
        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String,String>();
        props.put(Sasl.SERVER_AUTH,"TRUE");
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", host, props, cbh);
        authenticate();
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
    public void authenticate(String username, String host, String password) throws IOException, XMPPException {
        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String, String>();
        props.put(Sasl.SERVER_AUTH,"TRUE");
        sc = Sasl.createSaslClient(mechanisms, username, "xmpp", host, props, this);
        authenticate();
    }


}
