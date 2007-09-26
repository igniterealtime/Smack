/**
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

/**
 * Implementation of the SASL PLAIN mechanisn as defined by the
 * <a href="http://www.ietf.org/internet-drafts/draft-ietf-sasl-plain-08.txt">IETF draft
 * document</a>.
 *
 * @author Gaston Dombiak
 */
public class SASLPlainMechanism extends SASLMechanism {

    public SASLPlainMechanism(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
    }

    protected String getName() {
        return "PLAIN";
    }

    protected String getAuthenticationText(String username, String host, String password) {
        // Build the text containing the "authorization identity" + NUL char +
        // "authentication identity" + NUL char + "clear-text password"
        StringBuilder text = new StringBuilder();
        // Commented out line below due to SMACK-224. This part of PLAIN auth seems to be
        // optional, and just removing it should increase compatability.  
//         text.append(username).append("@").append(host);
        text.append('\0');
        text.append(username);
        text.append('\0');
        text.append(password);
        return text.toString();
    }

    protected String getChallengeResponse(byte[] bytes) {
        // Return null since this mechanism will never get a challenge from the server
        return null;
    }
}
