/**
 *
 * Copyright 2018-2020 Florian Schmaus
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
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.compression.CompressionModuleDescriptor;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModuleDescriptor;

import org.jivesoftware.smackx.admin.ServiceAdministrationManager;
import org.jivesoftware.smackx.iqregister.AccountManager;

import org.igniterealtime.smack.inttest.Configuration.AccountRegistration;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.AccountNum;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

public class XmppConnectionManager {

    private static final Logger LOGGER = Logger.getLogger(XmppConnectionManager.class.getName());

    private static final XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> DEFAULT_CONNECTION_DESCRIPTOR;

    private static final Map<String, XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>> NICKNAME_CONNECTION_DESCRIPTORS = new HashMap<>();

    private static final MultiMap<
        Class<? extends AbstractXMPPConnection>,
        XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>
    > CONNECTION_DESCRIPTORS = new MultiMap<>();

    static {
        try {
            DEFAULT_CONNECTION_DESCRIPTOR = XmppConnectionDescriptor.buildWith(XMPPTCPConnection.class, XMPPTCPConnectionConfiguration.class)
                            .withNickname("tcp")
                            .build();
            addConnectionDescriptor(DEFAULT_CONNECTION_DESCRIPTOR);

            addConnectionDescriptor(
                            XmppConnectionDescriptor.buildWith(ModularXmppClientToServerConnection.class, ModularXmppClientToServerConnectionConfiguration.class)
                            .withNickname("modular")
                            .build()
            );
            addConnectionDescriptor(
                            XmppConnectionDescriptor.buildWith(ModularXmppClientToServerConnection.class, ModularXmppClientToServerConnectionConfiguration.class, ModularXmppClientToServerConnectionConfiguration.Builder.class)
                            .withNickname("modular-nocompress")
                            .applyExtraConfguration(cb -> cb.removeModule(CompressionModuleDescriptor.class))
                            .build()
            );
            addConnectionDescriptor(
                    XmppConnectionDescriptor.buildWith(ModularXmppClientToServerConnection.class, ModularXmppClientToServerConnectionConfiguration.class, ModularXmppClientToServerConnectionConfiguration.Builder.class)
                    .withNickname("modular-websocket")
                    .applyExtraConfguration(cb -> {
                        cb.removeAllModules();
                        cb.addModule(XmppWebSocketTransportModuleDescriptor.class);
                    })
                    .build()
            );
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean addConnectionDescriptor(
                    XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor) {
        String nickname = connectionDescriptor.getNickname();
        Class<? extends AbstractXMPPConnection> connectionClass = connectionDescriptor.getConnectionClass();

        boolean alreadyExisted;
        synchronized (CONNECTION_DESCRIPTORS) {
            alreadyExisted = removeConnectionDescriptor(nickname);

            CONNECTION_DESCRIPTORS.put(connectionClass, connectionDescriptor);
            NICKNAME_CONNECTION_DESCRIPTORS.put(connectionDescriptor.getNickname(), connectionDescriptor);
        }
        return alreadyExisted;
    }

    public static boolean removeConnectionDescriptor(String nickname) {
        synchronized (CONNECTION_DESCRIPTORS) {
            XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor = NICKNAME_CONNECTION_DESCRIPTORS.remove(nickname);
            if (connectionDescriptor == null) {
                return false;
            }

            boolean removed = CONNECTION_DESCRIPTORS.removeOne(connectionDescriptor.getConnectionClass(), connectionDescriptor);
            assert removed;
        }

        return true;
    }

    private final XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> defaultConnectionDescriptor;

    private final Map<String, XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>> nicknameConnectionDescriptors;

    private final MultiMap<
        Class<? extends AbstractXMPPConnection>,
        XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>
    > connectionDescriptors;

    private final SmackIntegrationTestFramework sinttestFramework;
    private final Configuration sinttestConfiguration;
    private final String testRunId;

    private final AbstractXMPPConnection accountRegistrationConnection;
    private final ServiceAdministrationManager adminManager;
    private final AccountManager accountManager;

    /**
     * One of the three main connections. The type of the main connections is the default connection type.
     */
    AbstractXMPPConnection conOne, conTwo, conThree;

    /**
     * A pool of authenticated and free to use connections.
     */
    private final MultiMap<Class<? extends AbstractXMPPConnection>, AbstractXMPPConnection> connectionPool = new MultiMap<>();

    /**
     * A list of all ever created connections.
     */
    private final List<AbstractXMPPConnection> connections = new ArrayList<>();

    XmppConnectionManager(SmackIntegrationTestFramework sinttestFramework)
            throws SmackException, IOException, XMPPException, InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        synchronized (CONNECTION_DESCRIPTORS) {
            connectionDescriptors = CONNECTION_DESCRIPTORS.clone();
            nicknameConnectionDescriptors = new HashMap<>(NICKNAME_CONNECTION_DESCRIPTORS);
        }

        this.sinttestFramework = sinttestFramework;
        this.sinttestConfiguration = sinttestFramework.config;
        this.testRunId = sinttestFramework.testRunResult.testRunId;

        String configuredDefaultConnectionNickname = sinttestConfiguration.defaultConnectionNickname;
        if (configuredDefaultConnectionNickname != null) {
            defaultConnectionDescriptor = nicknameConnectionDescriptors.get(configuredDefaultConnectionNickname);
            if (defaultConnectionDescriptor == null) {
                throw new IllegalArgumentException("Could not find a connection descriptor for connection nickname '" + configuredDefaultConnectionNickname + "'");
            }
        } else {
            defaultConnectionDescriptor = DEFAULT_CONNECTION_DESCRIPTOR;
        }

        switch (sinttestConfiguration.accountRegistration) {
        case serviceAdministration:
        case inBandRegistration:
            accountRegistrationConnection = defaultConnectionDescriptor.construct(sinttestConfiguration);
            accountRegistrationConnection.connect();
            accountRegistrationConnection.login(sinttestConfiguration.adminAccountUsername,
                            sinttestConfiguration.adminAccountPassword);

            if (sinttestConfiguration.accountRegistration == AccountRegistration.inBandRegistration) {

                adminManager = null;
                accountManager = AccountManager.getInstance(accountRegistrationConnection);
            } else {
                adminManager = ServiceAdministrationManager.getInstanceFor(accountRegistrationConnection);
                accountManager = null;
            }
            break;
        case disabled:
            accountRegistrationConnection = null;
            adminManager = null;
            accountManager = null;
            break;
        default:
            throw new AssertionError();
        }
    }

    SmackIntegrationTestEnvironment prepareEnvironment() throws KeyManagementException, NoSuchAlgorithmException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            SmackException, IOException, XMPPException, InterruptedException {
        prepareMainConnections();
        return new SmackIntegrationTestEnvironment(conOne, conTwo, conThree,
                sinttestFramework.testRunResult.testRunId, sinttestConfiguration, this);
    }

    private void prepareMainConnections() throws KeyManagementException, NoSuchAlgorithmException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, SmackException, IOException,
            XMPPException, InterruptedException {
        final int mainAccountCount = AccountNum.values().length;
        List<AbstractXMPPConnection> connections = new ArrayList<>(mainAccountCount);
        for (AccountNum mainAccountNum : AccountNum.values()) {
            AbstractXMPPConnection mainConnection = getConnectedMainConnectionFor(mainAccountNum);
            connections.add(mainConnection);
        }
        conOne = connections.get(0);
        conTwo = connections.get(1);
        conThree = connections.get(2);
    }

    public XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> getDefaultConnectionDescriptor() {
        return defaultConnectionDescriptor;
    }

    public Collection<XmppConnectionDescriptor<? extends AbstractXMPPConnection, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>> getConnectionDescriptors() {
        return Collections.unmodifiableCollection(nicknameConnectionDescriptors.values());
    }

    @SuppressWarnings("unchecked")
    public <C extends AbstractXMPPConnection> XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> getConnectionDescriptorFor(
                    Class<C> connectionClass) {
        return (XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>) connectionDescriptors.getFirst(
                        connectionClass);
    }

    void disconnectAndCleanup() throws InterruptedException {
        int successfullyDeletedAccountsCount = 0;
        for (AbstractXMPPConnection connection : connections) {
            if (sinttestConfiguration.accountRegistration == AccountRegistration.inBandRegistration) {
                // Note that we use the account manager from the to-be-deleted connection.
                AccountManager accountManager = AccountManager.getInstance(connection);
                try {
                    accountManager.deleteAccount();
                    successfullyDeletedAccountsCount++;
                } catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "Could not delete dynamically registered account", e);
                }
            }

            connection.disconnect();

            if (sinttestConfiguration.accountRegistration == AccountRegistration.serviceAdministration) {
                String username = connection.getConfiguration().getUsername().toString();
                Localpart usernameAsLocalpart;
                try {
                    usernameAsLocalpart = Localpart.from(username);
                } catch (XmppStringprepException e) {
                    throw new AssertionError(e);
                }

                EntityBareJid connectionAddress = JidCreate.entityBareFrom(usernameAsLocalpart, sinttestConfiguration.service);

                try {
                    adminManager.deleteUser(connectionAddress);
                    successfullyDeletedAccountsCount++;
                } catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "Could not delete dynamically registered account", e);
                }
            }
        }

        if (sinttestConfiguration.isAccountRegistrationPossible()) {
            int unsuccessfullyDeletedAccountsCount = connections.size() - successfullyDeletedAccountsCount;
            if (unsuccessfullyDeletedAccountsCount == 0) {
                LOGGER.info("Successfully deleted all created accounts ✔");
            } else {
                LOGGER.warning("Could not delete all created accounts, " + unsuccessfullyDeletedAccountsCount + " remainaing");
            }
        }

        connections.clear();

        if (accountRegistrationConnection != null) {
            accountRegistrationConnection.disconnect();
        }
    }


    private static final String USERNAME_PREFIX = "smack-inttest";

    private AbstractXMPPConnection getConnectedMainConnectionFor(AccountNum accountNum) throws SmackException, IOException, XMPPException,
            InterruptedException, KeyManagementException, NoSuchAlgorithmException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String middlefix;
        String accountUsername;
        String accountPassword;
        switch (accountNum) {
        case One:
            accountUsername = sinttestConfiguration.accountOneUsername;
            accountPassword = sinttestConfiguration.accountOnePassword;
            middlefix = "one";
            break;
        case Two:
            accountUsername = sinttestConfiguration.accountTwoUsername;
            accountPassword = sinttestConfiguration.accountTwoPassword;
            middlefix = "two";
            break;
        case Three:
            accountUsername = sinttestConfiguration.accountThreeUsername;
            accountPassword = sinttestConfiguration.accountThreePassword;
            middlefix = "three";
            break;
        default:
            throw new IllegalStateException();
        }

        // Note that it is perfectly fine for account(Username|Password) to be 'null' at this point.
        final String finalAccountUsername = StringUtils.isNullOrEmpty(accountUsername) ? USERNAME_PREFIX + '-' + middlefix + '-' + testRunId : accountUsername;
        final String finalAccountPassword = StringUtils.isNullOrEmpty(accountPassword) ? StringUtils.insecureRandomString(16) : accountPassword;

        if (sinttestConfiguration.isAccountRegistrationPossible()) {
            registerAccount(finalAccountUsername, finalAccountPassword);
        }

        AbstractXMPPConnection mainConnection = defaultConnectionDescriptor.construct(sinttestConfiguration, builder -> {
            try {
                builder.setUsernameAndPassword(finalAccountUsername, finalAccountPassword)
                    .setResource(middlefix + '-' + testRunId);
            } catch (XmppStringprepException e) {
                throw new IllegalArgumentException(e);
            }
        });

        connections.add(mainConnection);

        mainConnection.connect();
        mainConnection.login();

        return mainConnection;
    }

    private void registerAccount(String username, String password) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, XmppStringprepException {
        if (accountRegistrationConnection == null) {
            throw new IllegalStateException("Account registration not configured");
        }

        switch (sinttestConfiguration.accountRegistration) {
        case serviceAdministration:
            EntityBareJid userJid = JidCreate.entityBareFrom(Localpart.from(username),
                            accountRegistrationConnection.getXMPPServiceDomain());
            adminManager.addUser(userJid, password);
            break;
        case inBandRegistration:
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
            Localpart usernameLocalpart = Localpart.from(username);
            accountManager.createAccount(usernameLocalpart, password, additionalAttributes);
            break;
        case disabled:
            throw new IllegalStateException("Account creation no possible");
        }
    }

    <C extends AbstractXMPPConnection> List<C> constructConnectedConnections(Class<C> connectionClass, int count)
                    throws InterruptedException, SmackException, IOException, XMPPException {
        List<C> connections = new ArrayList<>(count);

        synchronized (connectionPool) {
            @SuppressWarnings("unchecked")
            List<C> pooledConnections = (List<C>) connectionPool.getAll(connectionClass);
            while (count > 0 && !pooledConnections.isEmpty()) {
                C connection = pooledConnections.remove(pooledConnections.size() - 1);
                connections.add(connection);
                count--;
            }
        }

        @SuppressWarnings("unchecked")
        XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor = (XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>>) connectionDescriptors
                .getFirst(connectionClass);
        for (int i = 0; i < count; i++) {
            C connection = constructConnectedConnection(connectionDescriptor);
            connections.add(connection);
        }

        return connections;
    }

    private <C extends AbstractXMPPConnection> C constructConnectedConnection(
                    XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor)
                    throws InterruptedException, SmackException, IOException, XMPPException {
        C connection = constructConnection(connectionDescriptor, null);

        connection.connect();
        connection.login();

        return connection;
    }

    AbstractXMPPConnection constructConnection()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return constructConnection(defaultConnectionDescriptor);
    }

    <C extends AbstractXMPPConnection> C constructConnection(
                    XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return constructConnection(connectionDescriptor, null);
    }

    private <C extends AbstractXMPPConnection> C constructConnection(
            XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor,
            Collection<ConnectionConfigurationBuilderApplier> customConnectionConfigurationAppliers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        String username = "sinttest-" + testRunId + '-' + (connections.size() + 1);
        String password = StringUtils.randomString(24);

        return constructConnection(username, password, connectionDescriptor, customConnectionConfigurationAppliers);
    }

    private <C extends AbstractXMPPConnection> C constructConnection(final String username, final String password,
                    XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor,
                    Collection<ConnectionConfigurationBuilderApplier> customConnectionConfigurationAppliers)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        try {
            registerAccount(username, password);
        } catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }

        ConnectionConfigurationBuilderApplier usernameAndPasswordApplier = configurationBuilder -> {
            configurationBuilder.setUsernameAndPassword(username, password);
        };

        if (customConnectionConfigurationAppliers == null) {
            customConnectionConfigurationAppliers = Collections.singleton(usernameAndPasswordApplier);
        } else {
            customConnectionConfigurationAppliers.add(usernameAndPasswordApplier);
        }

        C connection;
        try {
            connection = connectionDescriptor.construct(sinttestConfiguration, customConnectionConfigurationAppliers);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        connections.add(connection);

        return connection;
    }

    void recycle(Collection<? extends AbstractXMPPConnection> connections) {
        for (AbstractXMPPConnection connection : connections) {
            recycle(connection);
        }
    }

    void recycle(AbstractXMPPConnection connection) {
        Class<? extends AbstractXMPPConnection> connectionClass = connection.getClass();
        if (!connectionDescriptors.containsKey(connectionClass)) {
            throw new IllegalStateException("Attempt to recycle unknown connection of class '" + connectionClass + "'");
        }

        if (connection.isAuthenticated()) {
            synchronized (connectionPool) {
                connectionPool.put(connectionClass, connection);
            }
        } else {
            connection.disconnect();
        }
        // Note that we do not delete the account of the unauthenticated connection here, as it is done at the end of
        // the test run together with all other dynamically created accounts.
    }

}
