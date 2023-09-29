/**
 *
 * Copyright 2015-2021 Florian Schmaus
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
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.Function;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.SslContextFactory;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.debugger.EnhancedDebugger;

import eu.geekplace.javapinning.java7.Java7Pinning;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

// TODO: Rename to SinttestConfiguration.
public final class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    public enum AccountRegistration {
        disabled,
        inBandRegistration,
        serviceAdministration,
    }

    public enum Debugger {
        none,
        console,
        enhanced,
    }

    public enum DnsResolver {
        minidns,
        javax,
        dnsjava,
    }

    public final DomainBareJid service;

    public final String serviceTlsPin;

    public final SslContextFactory sslContextFactory;

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

    public final Debugger debugger;

    public final Set<String> enabledTests;

    private final Map<String, Set<String>> enabledTestsMap;

    public final Set<String> disabledTests;

    private final Map<String, Set<String>> disabledTestsMap;

    public final String defaultConnectionNickname;

    public final Set<String> enabledConnections;

    public final Set<String> disabledConnections;

    public final Set<String> testPackages;

    public final ConnectionConfigurationBuilderApplier configurationApplier;

    public final boolean verbose;

    public final DnsResolver dnsResolver;

    public enum CompatibilityMode {
        standardsCompliant,
        ejabberd,
    }

    public final CompatibilityMode compatibilityMode;

    private Configuration(Configuration.Builder builder) throws KeyManagementException, NoSuchAlgorithmException {
        service = Objects.requireNonNull(builder.service,
                        "'service' must be set. Either via 'properties' files or via system property 'sinttest.service'.");
        serviceTlsPin = builder.serviceTlsPin;
        if (serviceTlsPin != null) {
            SSLContext sslContext = Java7Pinning.forPin(serviceTlsPin);
            sslContextFactory = () -> sslContext;
        } else {
            sslContextFactory = null;
        }
        securityMode = builder.securityMode;
        if (builder.replyTimeout > 0) {
            replyTimeout = builder.replyTimeout;
        } else {
            replyTimeout = 60000;
        }
        debugger = builder.debugger;
        if (StringUtils.isNotEmpty(builder.adminAccountUsername, builder.adminAccountPassword)) {
            accountRegistration = AccountRegistration.serviceAdministration;
        }
        else if (StringUtils.isNotEmpty(builder.accountOneUsername, builder.accountOnePassword,
                        builder.accountTwoUsername, builder.accountTwoPassword, builder.accountThreeUsername,
                        builder.accountThreePassword)) {
            accountRegistration = AccountRegistration.disabled;
        }
        else {
            accountRegistration = AccountRegistration.inBandRegistration;
        }

        this.adminAccountUsername = builder.adminAccountUsername;
        this.adminAccountPassword = builder.adminAccountPassword;

        boolean accountOnePasswordSet = StringUtils.isNotEmpty(builder.accountOnePassword);
        if (accountOnePasswordSet != StringUtils.isNotEmpty(builder.accountTwoPassword) ||
                accountOnePasswordSet != StringUtils.isNotEmpty(builder.accountThreePassword)) {
            // Ensure the invariant that either all main accounts have a password set, or none.
            throw new IllegalArgumentException();
        }

        this.accountOneUsername = builder.accountOneUsername;
        this.accountOnePassword = builder.accountOnePassword;
        this.accountTwoUsername = builder.accountTwoUsername;
        this.accountTwoPassword = builder.accountTwoPassword;
        this.accountThreeUsername = builder.accountThreeUsername;
        this.accountThreePassword = builder.accountThreePassword;
        this.enabledTests = CollectionUtil.nullSafeUnmodifiableSet(builder.enabledTests);
        this.enabledTestsMap = convertTestsToMap(enabledTests);
        this.disabledTests =  CollectionUtil.nullSafeUnmodifiableSet(builder.disabledTests);
        this.disabledTestsMap = convertTestsToMap(disabledTests);
        this.defaultConnectionNickname = builder.defaultConnectionNickname;
        this.enabledConnections = builder.enabledConnections;
        this.disabledConnections = builder.disabledConnections;
        this.testPackages = builder.testPackages;

        this.configurationApplier = b -> {
            if (sslContextFactory != null) {
                b.setSslContextFactory(sslContextFactory);
            }
            b.setSecurityMode(securityMode);
            b.setXmppDomain(service);

            switch (debugger) {
            case enhanced:
                b.setDebuggerFactory(EnhancedDebugger.Factory.INSTANCE);
                break;
            case console:
                b.setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE);
                break;
            case none:
                // Nothing to do :).
                break;
            }
        };

        this.verbose = builder.verbose;

        this.dnsResolver = builder.dnsResolver;
        this.compatibilityMode = builder.compatibilityMode;
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

        private Debugger debugger = Debugger.none;

        private Set<String> enabledTests;

        private Set<String> disabledTests;

        private String defaultConnectionNickname;

        private Set<String> enabledConnections;

        private Set<String> disabledConnections;

        private Set<String> testPackages;

        private boolean verbose;

        private DnsResolver dnsResolver = DnsResolver.minidns;

        private CompatibilityMode compatibilityMode = CompatibilityMode.standardsCompliant;

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

        private void ensureTestPackagesIsSet(int length) {
            if (testPackages == null) {
                testPackages = new HashSet<>(length);
            }
        }

        public Builder addTestPackage(String testPackage) {
            ensureTestPackagesIsSet(4);
            testPackages.add(testPackage);
            return this;
        }

        public Builder setAdminAccountUsernameAndPassword(String adminAccountUsername, String adminAccountPassword) {
            this.adminAccountUsername = StringUtils.requireNotNullNorEmpty(adminAccountUsername, "adminAccountUsername must not be null nor empty");
            this.adminAccountPassword = StringUtils.requireNotNullNorEmpty(adminAccountPassword, "adminAccountPassword must no be null nor empty");
            return this;
        }

        public Builder setUsernamesAndPassword(String accountOneUsername, String accountOnePassword,
                        String accountTwoUsername, String accountTwoPassword, String accountThreeUsername, String accountThreePassword) {
            this.accountOneUsername = StringUtils.requireNotNullNorEmpty(accountOneUsername, "accountOneUsername must not be null nor empty");
            this.accountOnePassword = StringUtils.requireNotNullNorEmpty(accountOnePassword, "accountOnePassword must not be null nor empty");
            this.accountTwoUsername = StringUtils.requireNotNullNorEmpty(accountTwoUsername, "accountTwoUsername must not be null nor empty");
            this.accountTwoPassword = StringUtils.requireNotNullNorEmpty(accountTwoPassword, "accountTwoPasswordmust not be null nor empty");
            this.accountThreeUsername = StringUtils.requireNotNullNorEmpty(accountThreeUsername, "accountThreeUsername must not be null nor empty");
            this.accountThreePassword = StringUtils.requireNotNullNorEmpty(accountThreePassword, "accountThreePassword must not be null nor empty");
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

        @SuppressWarnings("fallthrough")
        public Builder setDebugger(String debuggerString) {
            if (debuggerString == null) {
                return this;
            }
            switch (debuggerString) {
            case "false": // For backwards compatibility settings with previous boolean setting.
                LOGGER.warning("Debug string \"" + debuggerString + "\" is deprecated, please use \"none\" instead");
            case "none":
                debugger = Debugger.none;
                break;
            case "true": // For backwards compatibility settings with previous boolean setting.
                LOGGER.warning("Debug string \"" + debuggerString + "\" is deprecated, please use \"console\" instead");
            case "console":
                debugger = Debugger.console;
                break;
            case "enhanced":
                debugger = Debugger.enhanced;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized debugger string: " + debuggerString);
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

        public Builder setDefaultConnection(String defaultConnectionNickname) {
            this.defaultConnectionNickname = defaultConnectionNickname;
            return this;
        }

        public Builder setEnabledConnections(String enabledConnectionsString) {
            enabledConnections = split(enabledConnectionsString);
            return this;
        }

        public Builder setDisabledConnections(String disabledConnectionsString) {
            disabledConnections = split(disabledConnectionsString);
            return this;
        }

        public Builder addTestPackages(String testPackagesString) {
            if (testPackagesString != null) {
                String[] testPackagesArray = testPackagesString.split(",");

                ensureTestPackagesIsSet(testPackagesArray.length);

                for (String s : testPackagesArray) {
                    testPackages.add(s.trim());
                }
            }
            return this;
        }

        public Builder addTestPackages(String[] testPackagesString) {
            if (testPackagesString == null) {
                return this;
            }

            ensureTestPackagesIsSet(testPackagesString.length);

            for (String testPackage : testPackagesString) {
                testPackages.add(testPackage);
            }
            return this;
        }

        public Builder setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder setVerbose(String verboseBooleanString) {
            if (verboseBooleanString == null) {
                return this;
            }

            boolean verbose = ParserUtils.parseXmlBoolean(verboseBooleanString);
            return setVerbose(verbose);
        }

        public Builder setDnsResolver(DnsResolver dnsResolver) {
            this.dnsResolver = Objects.requireNonNull(dnsResolver);
            return this;
        }

        public Builder setDnsResolver(String dnsResolverString) {
            if (dnsResolverString == null) {
                return this;
            }

            DnsResolver dnsResolver = DnsResolver.valueOf(dnsResolverString);
            return setDnsResolver(dnsResolver);
        }

        public Builder setCompatibilityMode(CompatibilityMode compatibilityMode) {
            this.compatibilityMode = compatibilityMode;
            return this;
        }

        public Builder setCompatibilityMode(String compatibilityModeString) {
            if (compatibilityModeString == null) {
                return this;
            }

            CompatibilityMode compatibilityMode = CompatibilityMode.valueOf(compatibilityModeString);
            return setCompatibilityMode(compatibilityMode);
        }

        public Configuration build() throws KeyManagementException, NoSuchAlgorithmException {
            return new Configuration(this);
        }
    }

    private static final String SINTTEST = "sinttest.";

    public static Configuration newConfiguration(String[] testPackages)
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
        for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.startsWith(SINTTEST)) {
                continue;
            }
            key = key.substring(SINTTEST.length());
            String value = (String) entry.getValue();
            properties.put(key.trim(), value.trim());
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

        String debugString = properties.getProperty("debug");
        if (debugString != null) {
            LOGGER.warning("Usage of depreacted 'debug' option detected, please use 'debugger' instead");
            builder.setDebugger(debugString);
        }
        builder.setDebugger(properties.getProperty("debugger"));
        builder.setEnabledTests(properties.getProperty("enabledTests"));
        builder.setDisabledTests(properties.getProperty("disabledTests"));
        builder.setDefaultConnection(properties.getProperty("defaultConnection"));
        builder.setEnabledConnections(properties.getProperty("enabledConnections"));
        builder.setDisabledConnections(properties.getProperty("disabledConnections"));

        builder.addTestPackages(properties.getProperty("testPackages"));
        builder.addTestPackages(testPackages);

        builder.setVerbose(properties.getProperty("verbose"));

        builder.setDnsResolver(properties.getProperty("dnsResolver"));

        builder.setCompatibilityMode(properties.getProperty("compatibilityMode"));

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

    private static Set<String> split(String input) {
        return split(input, Function.identity());
    }

    private static Set<String> split(String input, Function<String, String> transformer) {
        if (input == null) {
            return null;
        }

        String[] inputArray = input.split(",");
        Set<String> res = new HashSet<>(inputArray.length);
        for (String s : inputArray) {
            s = transformer.apply(s);
            boolean newElement = res.add(s);
            if (!newElement) {
                throw new IllegalArgumentException("The argument '" + s + "' was already provided.");
            }
        }

        return res;
    }

    private static Set<String> getTestSetFrom(String input) {
        return split(input, s -> {
            s = s.trim();
            if (s.startsWith("smackx.") || s.startsWith("smack.")) {
                s = "org.jivesoftware." + s;
            }
            return s;
        });
    }

    private static Map<String, Set<String>> convertTestsToMap(Set<String> tests) {
        Map<String, Set<String>> res = new HashMap<>();
        for (String test : tests) {
            String[] testParts = test.split("\\.");
            if (testParts.length == 1) {
                // The whole test specification does not contain a dot, assume it is a test class specification.
                res.put(test, Collections.emptySet());
                continue;
            }

            String lastTestPart = testParts[testParts.length - 1];
            if (lastTestPart.isEmpty()) {
                throw new IllegalArgumentException("Invalid test specifier: " + test);
            }

            char firstCharOfLastTestPart = lastTestPart.charAt(0);
            if (!Character.isLowerCase(firstCharOfLastTestPart)) {
                // The first character of the last test part is not lowercase, assume this is a fully qualified test
                // class specification, e.g. org.foo.bar.TestClass.
                res.put(test, Collections.emptySet());
            }

            // The first character of the last test part is lowercase, assume this is a test class *and* method name
            // specification.
            String testMethodName = lastTestPart;
            int classPartsCount = testParts.length - 1;
            String[] classParts = new String[classPartsCount];
            System.arraycopy(testParts, 0, classParts, 0, classPartsCount);
            String testClass = String.join(".", classParts);

            res.compute(testClass, (k, v) -> {
                if (v == null) {
                    v = new HashSet<>();
                }
                v.add(testMethodName);
                return v;
            });
        }
        return res;
    }

    private static Set<String> getKey(Class<?> testClass, Map<String, Set<String>> testsMap) {
        String className = testClass.getName();
        if (testsMap.containsKey(className)) {
            return testsMap.get(className);
        }

        String unqualifiedClassName = testClass.getSimpleName();
        if (testsMap.containsKey(unqualifiedClassName)) {
            return testsMap.get(unqualifiedClassName);
        }

        return null;
    }

    private static boolean contains(Class<? extends AbstractSmackIntTest> testClass, Map<String, Set<String>> testsMap) {
        Set<String> enabledMethods = getKey(testClass, testsMap);
        return enabledMethods != null;
   }

    public boolean isClassEnabled(Class<? extends AbstractSmackIntTest> testClass) {
        if (enabledTestsMap.isEmpty()) {
            return true;
        }

        return contains(testClass, enabledTestsMap);
    }

    public boolean isClassDisabled(Class<? extends AbstractSmackIntTest> testClass) {
        if (disabledTestsMap.isEmpty()) {
            return false;
        }

        return contains(testClass, disabledTestsMap);
    }

    private static boolean contains(Method method, Map<String, Set<String>> testsMap) {
        Class<?> testClass = method.getDeclaringClass();
        Set<String> methods = getKey(testClass, testsMap);

        if (methods == null) {
            return false;
        }

        if (methods.isEmpty()) {
            return true;
        }

        String methodName = method.getName();
        return methods.contains(methodName);
    }

    public boolean isMethodEnabled(Method method) {
        if (enabledTestsMap.isEmpty()) {
            return true;
        }

        return contains(method, enabledTestsMap);
    }

    public boolean isMethodDisabled(Method method) {
        if (disabledTestsMap.isEmpty()) {
            return false;
        }

        return contains(method, disabledTestsMap);
    }

}
