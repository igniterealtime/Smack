/**
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
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smack.util.ByteUtils;

/**
 * Implementation of the SASL-PLAIN authentication mechanism
 * 
 * @author Ralf Bommersbach
 */
public class SASLPlainMechanism extends SASLMechanism {

    public static final String NAME = PLAIN;

    @Override
    protected void authenticateInternal(CallbackHandler arg0) throws SmackException {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected String getAuthenticationText() throws SmackException {
        // concatenate and encode username (authcid) and password
        byte[] authcid = ("\u0000" + this.authenticationId).getBytes();
        byte[] passw = ("\u0000" + this.password).getBytes();

        return Base64.encodeBytes(ByteUtils.concact(authcid, passw));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 410;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new SASLPlainMechanism();
    }

}
