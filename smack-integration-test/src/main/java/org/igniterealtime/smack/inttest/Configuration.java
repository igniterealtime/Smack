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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class Configuration {

    public final DomainBareJid service;

    public final String serviceTlsPin;

    public final SecurityMode securityMode;

    public final int replyTimeout;

    public final boolean registerAccounts;

    public final String accountOneUsername;

    public final String accountOnePassword;

    public final String accountTwoUsername;

    public final String accountTwoPassword;

    public final boolean debug;

    public final Set<String> enabledTests;

    public final Set<String> disabledTests;

    public final Set<String> testPackages;

    private Configuration(DomainBareJid service, String serviceTlsPin, SecurityMode securityMode, int replyTimeout,
                    boolean debug, String accountOneUsername, String accountOnePassword, String accountTwoUsername,
                    String accountTwoPassword, Set<String> enabledTests, Set<String> disabledTests,
                    Set<String> testPackages) {
        this.service = Objects.requireNonNull(service,
                        "'service' must be set. Either via 'properties' files or via system property 'sinttest.service'.");
        this.serviceTlsPin = serviceTlsPin;
        this.securityMode = securityMode;
        this.replyTimeout = replyTimeout;
        this.debug = debug;
        if (StringUtils.isNullOrEmpty(accountOneUsername) || StringUtils.isNullOrEmpty(accountOnePassword)
                        || StringUtils.isNullOrEmpty(accountTwoUsername)
                        || StringUtils.isNullOrEmpty(accountTwoPassword)) {
            registerAccounts = true;
        }
        else {
            registerAccounts = false;
        }
        this.accountOneUsername = accountOneUsername;
        this.accountOnePassword = accountOnePassword;
        this.accountTwoUsername = accountTwoUsername;
        this.accountTwoPassword = accountTwoPassword;
        this.enabledTests = enabledTests;
        this.disabledTests = disabledTests;
        this.testPackages = testPackages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private DomainBareJid service;

        private String serviceTlsPin;

        private SecurityMode securityMode;

        private int replyTimeout;

        private String accountOneUsername;

        private String accountOnePassword;

        private String accountTwoUsername;

        private String accountTwoPassword;

        private boolean debug;

        private Set<String> enabledTests;

        private Set<String> disabledTests;

        private Set<String> testPackages;

        private Builder() {
        }

        public Builder setService(String service) throws XmppStringprepException {
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

        public Builder setUsernamesAndPassword(String accountOneUsername, String accountOnePassword,
                        String accountTwoUsername, String accountTwoPassword) {
            this.accountOneUsername = accountOneUsername;
            this.accountOnePassword = accountOnePassword;
            this.accountTwoUsername = accountTwoUsername;
            this.accountTwoPassword = accountTwoPassword;
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

        public Configuration build() {
            return new Configuration(service, serviceTlsPin, securityMode, replyTimeout, debug, accountOneUsername,
                            accountOnePassword, accountTwoUsername, accountTwoPassword, enabledTests, disabledTests,
                            testPackages);
        }
    }

    private static final String SINTTEST = "sinttest.";

    public static Configuration newConfiguration() throws IOException {
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

        String accountOneUsername = properties.getProperty("accountOneUsername");
        String accountOnePassword = properties.getProperty("accountOnePassword");
        String accountTwoUsername = properties.getProperty("accountTwoUsername");
        String accountTwoPassword = properties.getProperty("accountTwoPassword");
        builder.setUsernamesAndPassword(accountOneUsername, accountOnePassword, accountTwoUsername, accountTwoPassword);

        builder.setDebug(properties.getProperty("debug"));
        builder.setEnabledTests(properties.getProperty("enabledTests"));
        builder.setDisabledTests(properties.getProperty("disabledTests"));
        builder.setTestPackages(properties.getProperty("testPackages"));

        return builder.build();
    }

    private static File findPropertiesFile() throws IOException {
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
