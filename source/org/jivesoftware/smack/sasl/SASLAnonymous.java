/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import java.io.IOException;
import javax.security.auth.callback.CallbackHandler;

/**
 * Implementation of the SASL ANONYMOUS mechanism
 *
 * @author Jay Kline
 */
public class SASLAnonymous extends SASLMechanism {

    public SASLAnonymous(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
    }

    protected String getName() {
        return "ANONYMOUS";
    }

    public void authenticate(String username, String host, CallbackHandler cbh) throws IOException {
        authenticate();
    }

    public void authenticate(String username, String host, String password) throws IOException {
        authenticate();
    }

    protected void authenticate() throws IOException {
        // Send the authentication to the server
        getSASLAuthentication().send(new AuthMechanism(getName(), null));
    }

    public void challengeReceived(String challenge) throws IOException {
        // Build the challenge response stanza encoding the response text
        // and send the authentication to the server
        getSASLAuthentication().send(new Response());
    }


}
