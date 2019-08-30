/**
 *
 * Copyright 2015-2019 Florian Schmaus
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
package org.jivesoftware.smack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;

public class LoginIntegrationTest extends AbstractSmackLowLevelIntegrationTest {

    public LoginIntegrationTest(SmackIntegrationTestEnvironment<?> environment) {
        super(environment);
    }

    /**
     * Check that the server is returning the correct error when trying to login using an invalid
     * (i.e. non-existent) user.
     *
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws IOException if an I/O error occured.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws NoSuchAlgorithmException if no such algorithm is available.
     * @throws KeyManagementException if there was a key mangement error.
     */
    @SmackIntegrationTest
    public void testInvalidLogin() throws SmackException, IOException, XMPPException,
                    InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        final String nonExistentUserString = StringUtils.insecureRandomString(24);
        final String invalidPassword = "invalidPassword";

        AbstractXMPPConnection connection = getUnconnectedConnection();
        connection.connect();

        try {
            connection.login(nonExistentUserString, invalidPassword);
            fail("Exception expected");
        }
        catch (SASLErrorException e) {
            assertEquals(SASLError.not_authorized, e.getSASLFailure().getSASLError());
        }
    }

}
