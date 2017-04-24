/**
 *
 * Copyright 2016 Fernando Ramirez
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

public class SASLXOAUTHMechanism extends SASLMechanism {

    public static final String NAME = XOAUTH;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean authzidSupported() {
        return true;
    }

    @Override
    public int getPriority() {
        return 600;
    }

    @Override
    public SASLXOAUTHMechanism newInstance() {
        return new SASLXOAUTHMechanism();
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        // Nothing to do here
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        // Nothing to do here
        return null;
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        // No check performed
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackException {
        String refreshToken = Base64.encodeToString(challenge);
        connection.setXOAUTHLastRefreshToken(refreshToken);
        return null;
    }

}
