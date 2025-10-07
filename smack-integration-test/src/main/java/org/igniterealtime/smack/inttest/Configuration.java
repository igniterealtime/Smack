/*
 *
 * Copyright 2015-2024 Florian Schmaus
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.SslContextFactory;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;

import org.jivesoftware.smackx.iqregister.AccountManager;

import org.igniterealtime.smack.inttest.debugger.EnhancedSinttestDebugger;
import org.igniterealtime.smack.inttest.debugger.SinttestDebugger;
import org.igniterealtime.smack.inttest.debugger.SinttestDebuggerFactory;
import org.igniterealtime.smack.inttest.debugger.SinttestDebuggerMetaFactory;
import org.igniterealtime.smack.inttest.debugger.StandardSinttestDebugger;

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

    public enum ExecutionOrder {
        alphabetic,
        reversed
    }

    public enum DnsResolver {
        minidns,
        javax,
        dnsjava,
    }

    public final DomainBareJid service;

    public final String host;

    public final boolean acceptAllCertificates;

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

    private final SinttestDebuggerFactory debuggerFactory;

    public final Set<String> enabledTests;

    private final Map<String, Set<String>> enabledTestsMap;

    public final Set<String> disabledTests;

    private final Map<String, Set<String>> disabledTestsMap;

    public final ExecutionOrder executionOrder;

    public final Set<String> enabledSpecifications;

    public final Set<String> disabledSpecifications;

    public final String defaultConnectionNickname;

    public final Set<String> enabledConnections;

    public final Set<String> disabledConnections;

    public final Set<String> testPackages;

    public final ConnectionConfigurationBuilderApplier configurationApplier;

    public final boolean failOnImpossibleTest;

    public final boolean failFast;

    public final boolean verbose;

    public final DnsResolver dnsResolver;

    public enum CompatibilityMode {
        standardsCompliant,
        ejabberd,
    }

    public final CompatibilityMode compatibilityMode;

    public final List<? extends SmackIntegrationTestFramework.TestRunResultProcessor> testRunResultProcessors;

    private Configuration(Configuration.Builder builder) throws KeyManagementException, NoSuchAlgorithmException {
        service = Objects.requireNonNull(builder.service,
                        "'service' must be set. Either via 'properties' files or via system property 'sinttest.service'.");
        host = builder.host;
        acceptAllCertificates = builder.acceptAllCertificates;
        serviceTlsPin = builder.serviceTlsPin;
        if (serviceTlsPin != null && acceptAllCertificates) {
            throw new IllegalArgumentException("TLS Pin specified while accept all TLS certificates is also set");
        }

        if (serviceTlsPin != null) {
            SSLContext sslContext = Java7Pinning.forPin(serviceTlsPin);
            sslContextFactory = () -> sslContext;
        } else if (acceptAllCertificates) {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,  new TrustManager[] { TLSUtils.ACCEPT_ALL_TRUST_MANAGER }, null);
            sslContextFactory = () -> sslContext;
        } else {
            sslContextFactory = null;
        }
        securityMode = builder.securityMode;
        AccountManager.sensitiveOperationOverInsecureConnectionDefault(securityMode == SecurityMode.disabled);
        if (builder.replyTimeout > 0) {
            replyTimeout = builder.replyTimeout;
        } else {
            replyTimeout = 47000;
        }
        debuggerFactory = builder.debuggerFactory;
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
        this.executionOrder = builder.executionOrder;
        this.enabledSpecifications = CollectionUtil.nullSafeUnmodifiableSet(builder.enabledSpecifications);
        this.disabledSpecifications = CollectionUtil.nullSafeUnmodifiableSet(builder.disabledSpecifications);
        this.defaultConnectionNickname = builder.defaultConnectionNickname;
        this.enabledConnections = builder.enabledConnections;
        this.disabledConnections = builder.disabledConnections;
        this.testPackages = builder.testPackages;

        this.configurationApplier = b -> {
            if (sslContextFactory != null) {
                b.setSslContextFactory(sslContextFactory);
            }
            if (acceptAllCertificates) {
                TLSUtils.acceptAllCertificates(b);
                TLSUtils.disableHostnameVerificationForTlsCertificates(b);
            }
            b.setSecurityMode(securityMode);
            b.setXmppDomain(service);
            if (host != null) {
                b.setHost(host);
            }
        };

        this.failOnImpossibleTest = builder.failOnImpossibleTest;
        this.failFast = builder.failFast;
        this.verbose = builder.verbose;

        this.dnsResolver = builder.dnsResolver;
        this.compatibilityMode = builder.compatibilityMode;
        this.testRunResultProcessors = builder.testRunResultProcessors;
    }

    public boolean isAccountRegistrationPossible() {
        return accountRegistration != AccountRegistration.disabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private DomainBareJid service;

        private String host;

        public boolean acceptAllCertificates;

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

        private SinttestDebuggerFactory debuggerFactory;

        private Set<String> enabledTests;

        private Set<String> disabledTests;

        private ExecutionOrder executionOrder;

        private Set<String> enabledSpecifications;

        private Set<String> disabledSpecifications;

        private String defaultConnectionNickname;

        private Set<String> enabledConnections;

        private Set<String> disabledConnections;

        private Set<String> testPackages;

        private boolean failOnImpossibleTest;

        private boolean failFast;

        private boolean verbose;

        private DnsResolver dnsResolver = DnsResolver.minidns;

        private CompatibilityMode compatibilityMode = CompatibilityMode.standardsCompliant;

        private List<? extends SmackIntegrationTestFramework.TestRunResultProcessor> testRunResultProcessors = Collections.emptyList();

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

        private Builder setHost(String host) {
            this.host = host;
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
            this.accountTwoPassword = StringUtils.requireNotNullNorEmpty(accountTwoPassword, "accountTwoPassword must not be null nor empty");
            this.accountThreeUsername = StringUtils.requireNotNullNorEmpty(accountThreeUsername, "accountThreeUsername must not be null nor empty");
            this.accountThreePassword = StringUtils.requireNotNullNorEmpty(accountThreePassword, "accountThreePassword must not be null nor empty");
            return this;
        }

        public Builder setAcceptAllCertificates(String acceptAllCertificatesBooleanString) {
            if (acceptAllCertificatesBooleanString == null) {
                return this;
            }

            boolean acceptAllCertificates = ParserUtils.parseXmlBoolean(acceptAllCertificatesBooleanString);
            return setAcceptAllCertificates(acceptAllCertificates);
        }

        public Builder setAcceptAllCertificates() {
            return setAcceptAllCertificates(true);
        }

        public Builder setAcceptAllCertificates(boolean acceptAllCertificates) {
            this.acceptAllCertificates = acceptAllCertificates;
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

            int firstComma = debuggerString.indexOf(',');

            final String debugger;
            final String debuggerOptions;
            if (firstComma >= 0) {
                if (firstComma + 1 >= debuggerString.length()) {
                    throw new IllegalArgumentException("No debugger options provided after comma");
                }

                debugger = debuggerString.substring(0, firstComma);
                debuggerOptions = debuggerString.substring(firstComma + 1);
            } else {
                debugger = debuggerString;
                debuggerOptions = null;
            }

            switch (debugger) {
            case "false": // For backwards compatibility settings with previous boolean setting.
                LOGGER.warning("Debug string \"" + debuggerString + "\" is deprecated, please use \"none\" instead");
            case "none":
                debuggerFactory = null;
                break;
            case "true": // For backwards compatibility settings with previous boolean setting.
                LOGGER.warning("Debug string \"" + debuggerString + "\" is deprecated, please use \"console\" instead");
            case "console":
                debuggerFactory = (testRunStart, testRunId) -> new StandardSinttestDebugger(testRunStart, testRunId, "dir=off");
                break;
            case "enhanced":
                debuggerFactory = (testRunStart, testRunId) -> new EnhancedSinttestDebugger();
                break;
            case "standard":
                debuggerFactory = (testRunStart, testRunId) -> new StandardSinttestDebugger(testRunStart, testRunId, debuggerOptions);
                break;
            default:
                try {
                    debuggerFactory = Class.forName(debugger)
                                    .asSubclass(SinttestDebuggerMetaFactory.class)
                                    .getDeclaredConstructor().newInstance()
                                    .create(debuggerOptions);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to construct debugger from value: " + debuggerString, e);
                }
            }
            return this;
        }

        public Builder setDebugger(SinttestDebuggerFactory factory) {
            debuggerFactory = factory;
            return this;
        }

        public Builder setEnabledTests(String enabledTestsString) {
            enabledTests = getTestSetFrom(enabledTestsString, "enabled");
            return this;
        }

        public Builder setDisabledTests(String disabledTestsString) {
            disabledTests = getTestSetFrom(disabledTestsString, "disabled");
            return this;
        }

        public Builder setEnabledSpecifications(String enabledSpecificationsString) {
            enabledSpecifications = getSpecificationSetFrom(enabledSpecificationsString);
            return this;
        }

        public Builder setDisabledSpecifications(String disabledSpecificationsString) {
            disabledSpecifications = getSpecificationSetFrom(disabledSpecificationsString);
            return this;
        }

        public Builder setExecutionOrder(ExecutionOrder executionOrder) {
            this.executionOrder = executionOrder;
            return this;
        }

        public Builder setExecutionOrder(String executionOrderString) {
            if (executionOrderString == null) {
                return this;
            }

            ExecutionOrder executionOrder = ExecutionOrder.valueOf(executionOrderString);
            return setExecutionOrder(executionOrder);
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

        public Builder setFailOnImpossibleTest(boolean failOnImpossibleTest) {
            this.failOnImpossibleTest = failOnImpossibleTest;
            return this;
        }

        public Builder setFailOnImpossibleTest(String failOnImpossibleTestBooleanString) {
            if (failOnImpossibleTestBooleanString == null) {
                return this;
            }

            boolean failOnImpossibleTest = ParserUtils.parseXmlBoolean(failOnImpossibleTestBooleanString);
            return setFailOnImpossibleTest(failOnImpossibleTest);
        }

        public Builder setFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder setFailFast(String failFastBooleanString) {
            if (failFastBooleanString == null) {
                return this;
            }

            boolean failFast = ParserUtils.parseXmlBoolean(failFastBooleanString);
            return setFailFast(failFast);
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

        public Builder setTestRunResultProcessors(String testRunResultProcessorsString) {
            if (testRunResultProcessorsString == null) {
                return this;
            }

            testRunResultProcessors = getTestRunProcessorListFrom(testRunResultProcessorsString);
            return this;
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
        builder.setHost(properties.getProperty("host"));
        builder.setAcceptAllCertificates(properties.getProperty("acceptAllCertificates"));
        builder.setServiceTlsPin(properties.getProperty("serviceTlsPin"));
        builder.setSecurityMode(properties.getProperty("securityMode"));
        builder.setReplyTimeout(properties.getProperty("replyTimeout", "47000"));

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
            LOGGER.warning("Usage of deprecated 'debug' option detected, please use 'debugger' instead");
            builder.setDebugger(debugString);
        }
        builder.setDebugger(properties.getProperty("debugger"));
        builder.setEnabledTests(properties.getProperty("enabledTests"));
        builder.setDisabledTests(properties.getProperty("disabledTests"));
        builder.setExecutionOrder(properties.getProperty("executionOrder"));
        builder.setEnabledSpecifications(properties.getProperty("enabledSpecifications"));
        builder.setDisabledSpecifications(properties.getProperty("disabledSpecifications"));
        builder.setDefaultConnection(properties.getProperty("defaultConnection"));
        builder.setEnabledConnections(properties.getProperty("enabledConnections"));
        builder.setDisabledConnections(properties.getProperty("disabledConnections"));

        builder.addTestPackages(properties.getProperty("testPackages"));
        builder.addTestPackages(testPackages);

        builder.setFailOnImpossibleTest(properties.getProperty("failOnImpossibleTest"));
        builder.setFailFast(properties.getProperty("failFast"));
        builder.setVerbose(properties.getProperty("verbose"));

        builder.setDnsResolver(properties.getProperty("dnsResolver"));

        builder.setCompatibilityMode(properties.getProperty("compatibilityMode"));

        builder.setTestRunResultProcessors(properties.getProperty("testRunResultProcessors",
            SmackIntegrationTestFramework.JulTestRunResultProcessor.class.getName()));

        return builder.build();
    }

    private static File findPropertiesFile() {
        List<String> possibleLocations = new ArrayList<>();
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

    private static Set<String> getTestSetFrom(String input, String name) {
        StringUtils.requireNullOrNotEmpty(input, "Most provide a value for " + name + " tests");
        return split(input, s -> {
            s = s.trim();
            if (s.startsWith("smackx.") || s.startsWith("smack.")) {
                s = "org.jivesoftware." + s;
            }
            return s;
        });
    }

    private static Set<String> getSpecificationSetFrom(String input) {
        return split(input, Configuration::normalizeSpecification);
    }

    private static List<? extends SmackIntegrationTestFramework.TestRunResultProcessor> getTestRunProcessorListFrom(String input) {
        return Arrays.stream(input.split(","))
            .map(element -> {
                try {
                    final Class<? extends SmackIntegrationTestFramework.TestRunResultProcessor> aClass = Class.forName(element).asSubclass(SmackIntegrationTestFramework.TestRunResultProcessor.class);
                    return aClass.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to construct TestRunResultProcessor from value: " + element, e);
                }
            })
            .collect(Collectors.toList());
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

    public List<Class<? extends AbstractSmackIntTest>> sortTestClasses(Collection<Class<? extends AbstractSmackIntTest>> classes) {
        return sort(classes, Comparator.comparing(Class::getName));
    }

    public List<Method> sortTestMethods(Collection<Method> methods) {
        return sort(methods, Comparator.comparing(Method::getName));
    }

    private <T> List<T> sort(Collection<T> collection, Comparator<T> comparator) {
        final List<T> result = new ArrayList<>(collection);
        if (executionOrder == null) {
            return result;
        }

        switch (executionOrder) {
            case alphabetic:
                result.sort(comparator);
                break;

            case reversed:
                result.sort(Collections.reverseOrder(comparator));
                break;
        }
        return result;
    }

    public boolean isSpecificationEnabled(String specification) {
        if (enabledSpecifications.isEmpty()) {
            return true;
        }

        if (specification == null) {
            return false;
        }

        return enabledSpecifications.contains(normalizeSpecification(specification));
    }

    public boolean isSpecificationDisabled(String specification) {
        if (disabledSpecifications.isEmpty()) {
            return false;
        }

        if (specification == null) {
            return false;
        }

        return disabledSpecifications.contains(normalizeSpecification(specification));
    }

    public static String normalizeSpecification(String specification) {
        if (specification == null || specification.isBlank()) {
            return null;
        }
        return specification.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    public SinttestDebugger createSinttestDebugger(ZonedDateTime testRunStart, String testRunId) {
        if (debuggerFactory == null) {
            return null;
        }

        return debuggerFactory.create(testRunStart, testRunId);
    }
}
