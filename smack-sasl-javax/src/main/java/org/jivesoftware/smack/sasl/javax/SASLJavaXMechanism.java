/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sasl.javax;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;

public abstract class SASLJavaXMechanism extends SASLMechanism {

    protected SaslClient sc;

    @Override
    public abstract String getName();

    @Override
    public final void checkIfSuccessfulOrThrow() throws SmackException {
        if (!sc.isComplete()) {
            throw new SmackException(getName() + " was not completed");
        }
    }

    @Override
    protected void authenticateInternal()
                    throws SmackException {
        String[] mechanisms = { getName() };
        Map<String, String> props = getSaslProps();
        try {
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", getServerName(), props,
                            new CallbackHandler() {
                                @Override
                                public void handle(Callback[] callbacks) throws IOException,
                                                UnsupportedCallbackException {
                                    for (int i = 0; i < callbacks.length; i++) {
                                        if (callbacks[i] instanceof NameCallback) {
                                            NameCallback ncb = (NameCallback) callbacks[i];
                                            ncb.setName(authenticationId);
                                        }
                                        else if (callbacks[i] instanceof PasswordCallback) {
                                            PasswordCallback pcb = (PasswordCallback) callbacks[i];
                                            pcb.setPassword(password.toCharArray());
                                        }
                                        else if (callbacks[i] instanceof RealmCallback) {
                                            RealmCallback rcb = (RealmCallback) callbacks[i];
                                            // Retrieve the REALM from the challenge response that
                                            // the server returned when the client initiated the
                                            // authentication exchange. If this value is not null or
                                            // empty, *this value* has to be sent back to the server
                                            // in the client's response to the server's challenge
                                            String text = rcb.getDefaultText();
                                            // The SASL client (sc) created in smack uses
                                            // rcb.getText when creating the negotiatedRealm to send
                                            // it back to the server. Make sure that this value
                                            // matches the server's realm
                                            rcb.setText(text);
                                        }
                                        else if (callbacks[i] instanceof RealmChoiceCallback) {
                                            // unused, prevents UnsupportedCallbackException
                                            // RealmChoiceCallback rccb =
                                            // (RealmChoiceCallback)callbacks[i];
                                        }
                                        else {
                                            throw new UnsupportedCallbackException(callbacks[i]);
                                        }
                                    }
                                }

                            });
        }
        catch (SaslException e) {
            throw new SmackException(e);
        }
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh)
                    throws SmackException {
        String[] mechanisms = { getName() };
        Map<String, String> props = getSaslProps();
        try {
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
        }
        catch (SaslException e) {
            throw new SmackException(e);
        }
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        if (sc.hasInitialResponse()) {
            try {
                return sc.evaluateChallenge(new byte[0]);
            }
            catch (SaslException e) {
                throw new SmackException(e);
            }
        }
        return null;
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackException {
        try {
            if (challenge != null) {
                return sc.evaluateChallenge(challenge);
            }
            else {
                return sc.evaluateChallenge(new byte[0]);
            }
        }
        catch (SaslException e) {
            throw new SmackException(e);
        }
    }

    protected Map<String,String> getSaslProps() {
        return new HashMap<String, String>();
    }

    protected String getServerName() {
        return serviceName;
    }
}
