/**
 *
 * Copyright 2015 Florian Schmaus
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
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;

public class LoginIntegrationTest extends AbstractSmackLowLevelIntegrationTest {

    public LoginIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    /**
     * Check that the server is returning the correct error when trying to login using an invalid
     * (i.e. non-existent) user.
     *
     * @throws InterruptedException 
     * @throws XMPPException 
     * @throws IOException 
     * @throws SmackException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyManagementException 
     */
    @SmackIntegrationTest
    public void testInvalidLogin() throws SmackException, IOException, XMPPException,
                    InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        final String nonExistentUserString = StringUtils.insecureRandomString(24);
        XMPPTCPConnectionConfiguration conf = getConnectionConfiguration().setUsernameAndPassword(
                        nonExistentUserString, "invalidPassword").build();

        XMPPTCPConnection connection = new XMPPTCPConnection(conf);
        connection.connect();

        try {
            connection.login();
            fail("Exception expected");
        }
        catch (SASLErrorException e) {
            assertEquals(SASLError.not_authorized, e.getSASLFailure().getSASLError());
        }
    }

}
