/**
 *
 * Copyright 2015-2016 Florian Schmaus
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
import org.igniterealtime.smack.inttest.Configuration.AccountRegistration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.admin.ServiceAdministrationManager;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

public class IntTestUtil {

    private static final Logger LOGGER = Logger.getLogger(IntTestUtil.class.getName());


    public static UsernameAndPassword registerAccount(XMPPTCPConnection connection, SmackIntegrationTestEnvironment environment, int connectionId) throws InterruptedException, XMPPException, SmackException, IOException {
        String username = "sinttest-" + environment.testRunId + "-" + connectionId;
        return registerAccount(connection, username, StringUtils.insecureRandomString(12), environment.configuration);
    }

    public static UsernameAndPassword registerAccount(XMPPTCPConnection connection, String accountUsername, String accountPassword,
                    Configuration config) throws InterruptedException, XMPPException, SmackException, IOException {
        switch (config.accountRegistration) {
        case inBandRegistration:
            return registerAccountViaIbr(connection, accountUsername, accountPassword);
        case serviceAdministration:
            return registerAccountViaAdmin(connection, accountUsername, accountPassword, config.adminAccountUsername, config.adminAccountPassword);
        default:
            throw new AssertionError();
        }
    }

//    public static UsernameAndPassword registerAccountViaAdmin(XMPPTCPConnection connection) throws XmppStringprepException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
//        return registerAccountViaAdmin(connection, StringUtils.insecureRandomString(12),
//                        StringUtils.insecureRandomString(12));
//    }

    public static UsernameAndPassword registerAccountViaAdmin(XMPPTCPConnection connection, String username,
                    String password, String adminAccountUsername, String adminAccountPassword) throws InterruptedException, XMPPException, SmackException, IOException {
        connection.login(adminAccountUsername, adminAccountPassword);

        ServiceAdministrationManager adminManager = ServiceAdministrationManager.getInstanceFor(connection);

        EntityBareJid userJid = JidCreate.entityBareFrom(Localpart.from(username), connection.getServiceName());
        adminManager.addUser(userJid, password);

        connection.disconnect();
        connection.connect();

        return new UsernameAndPassword(username, password);

    }

    public static UsernameAndPassword registerAccountViaIbr(XMPPConnection connection)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        return registerAccountViaIbr(connection, StringUtils.insecureRandomString(12),
                        StringUtils.insecureRandomString(12));
    }

    public static UsernameAndPassword registerAccountViaIbr(XMPPConnection connection, String username,
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
        Localpart usernameLocalpart;
        try {
            usernameLocalpart = Localpart.from(username);
        }
        catch (XmppStringprepException e) {
            throw new IllegalArgumentException("Invalid username: " + username, e);
        }
        accountManager.createAccount(usernameLocalpart, password, additionalAttributes);

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


    public static void disconnectAndMaybeDelete(XMPPTCPConnection connection, Configuration config) throws InterruptedException {
        try {
            if (!config.isAccountRegistrationPossible()) {
                return;
            }

            Configuration.AccountRegistration accountDeletionMethod = config.accountRegistration;

            AccountManager accountManager = AccountManager.getInstance(connection);
            try {
                if (accountManager.isSupported()) {
                    accountDeletionMethod = AccountRegistration.inBandRegistration;
                }
            }
            catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Could not test if XEP-0077 account deletion is possible", e);
            }

            switch (accountDeletionMethod) {
            case inBandRegistration:
                deleteViaIbr(connection);
                break;
            case serviceAdministration:
                deleteViaServiceAdministration(connection, config);
                break;
            default:
                throw new AssertionError();
            }
        }
        finally {
            connection.disconnect();
        }
    }

    public static void deleteViaServiceAdministration(XMPPTCPConnection connection, Configuration config) {
        EntityBareJid accountToDelete = connection.getUser().asEntityBareJid();

        final int maxAttempts = 3;

        int attempts;
        for (attempts = 0; attempts < maxAttempts; attempts++) {
            connection.disconnect();

            try {
                connection.connect().login(config.adminAccountUsername, config.adminAccountPassword);
            }
            catch (XMPPException | SmackException | IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Exception deleting account for " + connection, e);
                continue;
            }

            ServiceAdministrationManager adminManager = ServiceAdministrationManager.getInstanceFor(connection);
            try {
                adminManager.deleteUser(accountToDelete);
                break;
            }
            catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Exception deleting account for " + connection, e);
                continue;
            }
        }
        if (attempts > maxAttempts) {
            LOGGER.log(Level.SEVERE, "Could not delete account for connection: " + connection);
        }
    }

    public static void deleteViaIbr(XMPPTCPConnection connection)
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
