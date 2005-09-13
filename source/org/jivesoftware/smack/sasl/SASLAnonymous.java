/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2005 Jive Software.
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
 * Implementation of the SASL ANONYMOUS mechanisn as defined by the
 * <a href="http://www.ietf.org/internet-drafts/draft-ietf-sasl-anon-05.txt">IETF draft
 * document</a>.
 *
 * @author Gaston Dombiak
 */
public class SASLAnonymous extends SASLMechanism {

    public SASLAnonymous(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
    }

    protected String getName() {
        return "ANONYMOUS";
    }

    protected String getAuthenticationText(String username, String host, String password) {
        // Nothing to send in the <auth> body
        return null;
    }

    protected String getChallengeResponse(byte[] bytes) {
        // Some servers may send a challenge to gather more information such as
        // email address. Return any string value.
        return "anything";
    }
}
