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
package org.igniterealtime.smack.inttest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;

public class IntTestUtil {

    private static final Logger LOGGER = Logger.getLogger(IntTestUtil.class.getName());

    public static UsernameAndPassword registerAccount(XMPPConnection connection)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        return registerAccount(connection, StringUtils.randomString(12),
                        StringUtils.randomString(12));
    }

    public static UsernameAndPassword registerAccount(XMPPConnection connection, String username,
                    String password) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        AccountManager accountManager = AccountManager.getInstance(connection);
        if (!accountManager.supportsAccountCreation()) {
            throw new UnsupportedOperationException("Account creation/registation is not supported");
        }
        Set<String> requiredAttributes = accountManager.getAccountAttributes();
        if (requiredAttributes.size() > 4) {
            throw new IllegalStateException("Unkown required attributes");
        }
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("name", "Smack Integration Test");
        additionalAttributes.put("email", "flow@igniterealtime.org");
        accountManager.createAccount(username, password, additionalAttributes);

        return new UsernameAndPassword(username, password);
    }

    public static final class UsernameAndPassword {
        public final String username;
        public final String password;

        private UsernameAndPassword(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static void disconnectAndMaybeDelete(XMPPTCPConnection connection, boolean delete)
                    throws InterruptedException {
        // If the connection is disconnected, then re-reconnect and login. This could happen when
        // (low-level) integration tests disconnect the connection, e.g. to test disconnection
        // mechanisms
        if (!connection.isConnected()) {
            try {
                connection.connect().login();
            }
            catch (XMPPException | SmackException | IOException e) {
                LOGGER.log(Level.WARNING, "Exception reconnection account for deletion", e);
            }
        }
        try {
            if (delete) {
                final int maxAttempts = 3;
                AccountManager am = AccountManager.getInstance(connection);
                int attempts;
                for (attempts = 0; attempts < maxAttempts; attempts++) {
                    try {
                        am.deleteAccount();
                    }
                    catch (XMPPErrorException | NoResponseException e) {
                        LOGGER.log(Level.WARNING, "Exception deleting account for " + connection, e);
                        continue;
                    }
                    catch (NotConnectedException e) {
                        LOGGER.log(Level.WARNING, "Exception deleting account for " + connection, e);
                        try {
                            connection.connect().login();
                        }
                        catch (XMPPException | SmackException | IOException e2) {
                            LOGGER.log(Level.WARNING, "Exception while trying to re-connect " + connection, e);
                        }
                        continue;
                    }
                    LOGGER.info("Successfully deleted account of " + connection);
                    break;
                }
                if (attempts > maxAttempts) {
                    LOGGER.log(Level.SEVERE, "Could not delete account for connection: " + connection);
                }
            }
        }
        finally {
            connection.disconnect();
        }
    }
}
