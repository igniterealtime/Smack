/**
 *
 * Copyright 2015-2017 Florian Schmaus
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import eu.geekplace.javapinning.java7.Java7Pinning;

public final class Configuration {

    public enum AccountRegistration {
        disabled,
        inBandRegistration,
        serviceAdministration,
    }

    public final DomainBareJid service;

    public final String serviceTlsPin;

    public final SSLContext tlsContext;

    public final SecurityMode securityMode;

    public final int replyTimeout;

    public final AccountRegistration accountRegistration;

    public final String adminAccountUsername;

    public final String adminAccountPassword;

    public final String accountOneUsername;

    public final String accountOnePassword;

    public final String accountTwoUsername;

    public final String accountTwoPassword;

    public final String accountThreeUsername;

    public final String accountThreePassword;

    public final boolean debug;

    public final Set<String> enabledTests;

    public final Set<String> disabledTests;

    public final Set<String> testPackages;

    private Configuration(DomainBareJid service, String serviceTlsPin, SecurityMode securityMode, int replyTimeout,
                    boolean debug, String accountOneUsername, String accountOnePassword, String accountTwoUsername,
                    String accountTwoPassword, String accountThreeUsername, String accountThreePassword, Set<String> enabledTests, Set<String> disabledTests,
                    Set<String> testPackages, String adminAccountUsername, String adminAccountPassword)
                    throws KeyManagementException, NoSuchAlgorithmException {
        this.service = Objects.requireNonNull(service,
                        "'service' must be set. Either via 'properties' files or via system property 'sinttest.service'.");
        this.serviceTlsPin = serviceTlsPin;
        if (serviceTlsPin != null) {
            tlsContext = Java7Pinning.forPin(serviceTlsPin);
        } else {
            tlsContext = null;
        }
        this.securityMode = securityMode;
        if (replyTimeout > 0) {
            this.replyTimeout = replyTimeout;
        } else {
            this.replyTimeout = 60000;
        }
        this.debug = debug;
        if (StringUtils.isNotEmpty(adminAccountUsername, adminAccountPassword)) {
            accountRegistration = AccountRegistration.serviceAdministration;
        }
        else if (StringUtils.isNotEmpty(accountOneUsername, accountOnePassword, accountTwoUsername, accountTwoPassword,
                        accountThreeUsername, accountThreePassword)) {
            accountRegistration = AccountRegistration.disabled;
        }
        else {
            accountRegistration = AccountRegistration.inBandRegistration;
        }

        this.adminAccountUsername = adminAccountUsername;
        this.adminAccountPassword = adminAccountPassword;

        this.accountOneUsername = accountOneUsername;
        this.accountOnePassword = accountOnePassword;
        this.accountTwoUsername = accountTwoUsername;
        this.accountTwoPassword = accountTwoPassword;
        this.accountThreeUsername = accountThreeUsername;
        this.accountThreePassword = accountThreePassword;
        this.enabledTests = enabledTests;
        this.disabledTests = disabledTests;
        this.testPackages = testPackages;
    }

    public boolean isAccountRegistrationPossible() {
        return accountRegistration != AccountRegistration.disabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private DomainBareJid service;

        private String serviceTlsPin;

        private SecurityMode securityMode;

        private int replyTimeout;

        private String adminAccountUsername;

        private String adminAccountPassword;

        private String accountOneUsername;

        private String accountOnePassword;

        private String accountTwoUsername;

        private String accountTwoPassword;

        public String accountThreeUsername;

        public String accountThreePassword;

        private boolean debug;

        private Set<String> enabledTests;

        private Set<String> disabledTests;

        private Set<String> testPackages;

        private Builder() {
        }

        public Builder setService(String service) throws XmppStringprepException {
            if (service == null) {
                // Do nothing if user did not specify the XMPP service domain. When the builder
                // builds a configuration using build() it will throw a meaningful exception.
                return this;
            }
            return setService(JidCreate.domainBareFrom(service));
        }

        public Builder setService(DomainBareJid service) {
            this.service = service;
            return this;
        }

        public Builder addEnabledTest(Class<? extends AbstractSmackIntTest> enabledTest) {
            if (enabledTests == null) {
                enabledTests = new HashSet<>();
            }
            enabledTests.add(enabledTest.getName());
            // Also add the package of the test as test package
            return addTestPackage(enabledTest.getPackage().getName());
        }

        public Builder addTestPackage(String testPackage) {
            if (testPackages == null) {
                testPackages = new HashSet<>();
            }
            testPackages.add(testPackage);
            return this;
        }

        public Builder setAdminAccountUsernameAndPassword(String adminAccountUsername, String adminAccountPassword) {
            this.adminAccountUsername = StringUtils.requireNotNullOrEmpty(adminAccountUsername, "adminAccountUsername must not be null or empty");
            this.adminAccountPassword = StringUtils.requireNotNullOrEmpty(adminAccountPassword, "adminAccountPassword must no be null or empty");
            return this;
        }

        public Builder setUsernamesAndPassword(String accountOneUsername, String accountOnePassword,
                        String accountTwoUsername, String accountTwoPassword, String accountThreeUsername, String accountThreePassword) {
            this.accountOneUsername = StringUtils.requireNotNullOrEmpty(accountOneUsername, "accountOneUsername must not be null or empty");
            this.accountOnePassword = StringUtils.requireNotNullOrEmpty(accountOnePassword, "accountOnePassword must not be null or empty");
            this.accountTwoUsername = StringUtils.requireNotNullOrEmpty(accountTwoUsername, "accountTwoUsername must not be null or empty");
            this.accountTwoPassword = StringUtils.requireNotNullOrEmpty(accountTwoPassword, "accountTwoPasswordmust not be null or empty");
            this.accountThreeUsername = StringUtils.requireNotNullOrEmpty(accountThreeUsername, "accountThreeUsername must not be null or empty");
            this.accountThreePassword = StringUtils.requireNotNullOrEmpty(accountThreePassword, "accountThreePassword must not be null or empty");
            return this;
        }

        public Builder setServiceTlsPin(String tlsPin) {
            this.serviceTlsPin = tlsPin;
            return this;
        }

        public Builder setSecurityMode(String securityModeString) {
            if (securityModeString != null) {
                securityMode = SecurityMode.valueOf(securityModeString);
            }
            else {
                securityMode = SecurityMode.required;
            }
            return this;
        }

        public Builder setReplyTimeout(String timeout) {
            if (timeout != null) {
                replyTimeout = Integer.valueOf(timeout);
            }
            return this;
        }

        public Builder setDebug(String debugString) {
            if (debugString != null) {
                debug = Boolean.valueOf(debugString);
            }
            return this;
        }

        public Builder setEnabledTests(String enabledTestsString) {
            enabledTests = getTestSetFrom(enabledTestsString);
            return this;
        }

        public Builder setDisabledTests(String disabledTestsString) {
            disabledTests = getTestSetFrom(disabledTestsString);
            return this;
        }

        public Builder setTestPackages(String testPackagesString) {
            if (testPackagesString != null) {
                String[] testPackagesArray = testPackagesString.split(",");
                testPackages = new HashSet<>(testPackagesArray.length);
                for (String s : testPackagesArray) {
                    testPackages.add(s.trim());
                }
            }
            return this;
        }

        public Configuration build() throws KeyManagementException, NoSuchAlgorithmException {
            return new Configuration(service, serviceTlsPin, securityMode, replyTimeout, debug, accountOneUsername,
                            accountOnePassword, accountTwoUsername, accountTwoPassword, accountThreeUsername, accountThreePassword, enabledTests, disabledTests,
                            testPackages, adminAccountUsername, adminAccountPassword);
        }
    }

    private static final String SINTTEST = "sinttest.";

    public static Configuration newConfiguration()
                    throws IOException, KeyManagementException, NoSuchAlgorithmException {
        Properties properties = new Properties();

        File propertiesFile = findPropertiesFile();
        if (propertiesFile != null) {
            try (FileInputStream in = new FileInputStream(propertiesFile)) {
                properties.load(in);
            }
        }

        // Properties set via the system override the file properties
        Properties systemProperties = System.getProperties();
        for (Entry<Object, Object> entry : systemProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.startsWith(SINTTEST)) {
                continue;
            }
            key = key.substring(SINTTEST.length());
            String value = (String) entry.getValue();
            properties.put(key, value);
        }

        Builder builder = builder();
        builder.setService(properties.getProperty("service"));
        builder.setServiceTlsPin(properties.getProperty("serviceTlsPin"));
        builder.setSecurityMode(properties.getProperty("securityMode"));
        builder.setReplyTimeout(properties.getProperty("replyTimeout", "60000"));

        String adminAccountUsername = properties.getProperty("adminAccountUsername");
        String adminAccountPassword = properties.getProperty("adminAccountPassword");
        if (StringUtils.isNotEmpty(adminAccountUsername, adminAccountPassword)) {
            builder.setAdminAccountUsernameAndPassword(adminAccountUsername, adminAccountPassword);
        }

        String accountOneUsername = properties.getProperty("accountOneUsername");
        String accountOnePassword = properties.getProperty("accountOnePassword");
        String accountTwoUsername = properties.getProperty("accountTwoUsername");
        String accountTwoPassword = properties.getProperty("accountTwoPassword");
        String accountThreeUsername = properties.getProperty("accountThreeUsername");
        String accountThreePassword = properties.getProperty("accountThreePassword");
        if (accountOneUsername != null || accountOnePassword != null || accountTwoUsername != null
                        || accountTwoPassword != null || accountThreeUsername != null || accountThreePassword != null) {
            builder.setUsernamesAndPassword(accountOneUsername, accountOnePassword, accountTwoUsername,
                            accountTwoPassword, accountThreeUsername, accountThreePassword);
        }

        builder.setDebug(properties.getProperty("debug"));
        builder.setEnabledTests(properties.getProperty("enabledTests"));
        builder.setDisabledTests(properties.getProperty("disabledTests"));
        builder.setTestPackages(properties.getProperty("testPackages"));

        return builder.build();
    }

    private static File findPropertiesFile() {
        List<String> possibleLocations = new LinkedList<>();
        possibleLocations.add("properties");
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            possibleLocations.add(userHome + "/.config/smack-integration-test/properties");
        }

        for (String possibleLocation : possibleLocations) {
            File res = new File(possibleLocation);
            if (res.isFile())
                return res;
        }
        return null;
    }

    private static Set<String> getTestSetFrom(String string) {
        if (string == null) {
            return null;
        }
        String[] stringArray = string.split(",");
        Set<String> res = new HashSet<>(stringArray.length);
        for (String s : stringArray) {
            res.add(getFullTestStringFrom(s));
        }
        return res;
    }

    private static String getFullTestStringFrom(String string) {
        string = string.trim();
        if (string.startsWith("smackx.") || string.startsWith("smack.")) {
            string = "org.jivesoftware." + string;
        }
        return string;
    }
}
