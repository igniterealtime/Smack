/**
 *
 * Copyright the original author or authors
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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;

import javax.security.auth.callback.CallbackHandler;

/**
 * Implementation of the SASL ANONYMOUS mechanism.
 *
 * @author Jay Kline
 */
public class SASLAnonymous extends SASLMechanism {

    public static final String NAME = "ANONYMOUS";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh)
                    throws SmackException {
        // Nothing to do here
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        // ANONYMOUS has no initial response, return null
        return null;
    }

    @Override
    public SASLAnonymous newInstance() {
        return new SASLAnonymous();
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        // SASL Anonymous is always successful :)
    }

}
