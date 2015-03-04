/**
 *
 * Copyright 2014-2015 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smack.sasl.core;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * The SASL X-OAUTH2 mechanism as described in <a
 * href="https://developers.google.com/talk/jep_extensions/oauth">https://developers.google
 * .com/talk/jep_extensions/oauth</a>
 * <p>
 * The given password will be used as OAUTH token.
 * </p>
 * <p>
 * Note that X-OAUTH2 is experimental in Smack. This is because Google defined, besides being a bad practice (XEP-134),
 * custom attributes to the 'auth' stanza, as can be seen here
 * </p>
 * 
 * <pre>
 * {@code
 * <auth xmlns="urn:ietf:params:xml:ns:xmpp-sasl" mechanism="X-OAUTH2"
 *    auth:service="chromiumsync" auth:allow-generated-jid="true"
 *    auth:client-uses-full-bind-result="true" xmlns:auth="http://www.google.com/talk/protocol/auth">
 * }
 * </pre>
 * 
 * from https://developers.google.com/cloud-print/docs/rawxmpp and here
 * 
 * <pre>
 * {@code
 * <auth xmlns="urn:ietf:params:xml:ns:xmpp-sasl"
 *   mechanism="X-OAUTH2"
 *   auth:service="oauth2"
 *   xmlns:auth="http://www.google.com/talk/protocol/auth">
 * base64("\0" + user_name + "\0" + oauth_token)
 * </auth>
 * }
 * </pre>
 * 
 * from https://developers.google.com/talk/jep_extensions/oauth
 * <p>
 * Those attribute extensions are currently not supported by Smack, and it's unclear how it affects authorization and
 * how widely they are used.
 * </p>
 */
public class SASLXOauth2Mechanism extends SASLMechanism {

    public static final String NAME = "X-OAUTH2";

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        // base64("\0" + user_name + "\0" + oauth_token)
        return Base64.encode(toBytes('\u0000' + authenticationId + '\u0000' + password));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        // Same priority as SASL PLAIN
        return 410;
    }

    @Override
    public SASLXOauth2Mechanism newInstance() {
        return new SASLXOauth2Mechanism();
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        // No check performed
    }
}
