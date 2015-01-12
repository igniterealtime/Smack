/**
 *
 * Copyright © 2014 Daniele Ricci
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
package org.jivesoftware.smack.sasl.provided;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.util.XmppStringUtils;

/**
 * Implementation of the SASL-EXTERNAL authentication mechanism
 * 
 * @author Daniele Ricci
 */
public class SASLExternalMechanism extends SASLMechanism {

    public static final String NAME = EXTERNAL;

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        // Do nothing. Auth will be done external to Smack and which will receive the localpart
        // after the resource binding
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        if (StringUtils.isNullOrEmpty(authenticationId)) {
            return null;
        }

        return toBytes(XmppStringUtils.completeJidFrom(authenticationId, serviceName));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 510;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new SASLExternalMechanism();
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        // No check performed
    }

}
