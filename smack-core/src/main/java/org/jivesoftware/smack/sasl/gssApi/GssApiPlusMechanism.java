/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.sasl.gssApi;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.sasl.SASLMechanism;

/**
 * The plus inside the GSS-API-plus mechanism name suggests that the server supports channel binding.
 *
 * @author adiaholic
 */
public class GssApiPlusMechanism extends SASLMechanism{

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackSaslException {
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackSaslException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    protected void checkIfSuccessfulOrThrow() throws SmackSaslException {
    }

    @Override
    protected SASLMechanism newInstance() {
        return null;
    }
}
