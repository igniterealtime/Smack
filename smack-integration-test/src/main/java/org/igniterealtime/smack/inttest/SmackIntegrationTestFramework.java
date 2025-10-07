/*
 *
 * Copyright 2015-2025 Florian Schmaus
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

import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withReturnType;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.Smack;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.dns.dnsjava.DNSJavaResolver;
import org.jivesoftware.smack.util.dns.javax.JavaxResolver;
import org.jivesoftware.smack.util.dns.minidns.MiniDnsResolver;

import org.jivesoftware.smackx.iqregister.AccountManager;

import org.igniterealtime.smack.inttest.Configuration.AccountRegistration;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.debugger.SinttestDebugger;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class SmackIntegrationTestFramework {

    static {
        TLSUtils.setDefaultTrustStoreTypeToJksIfRequired();
    }

    private static final Logger LOGGER = Logger.getLogger(SmackIntegrationTestFramework.class.getName());

    public static boolean SINTTEST_UNIT_TEST = false;

    // TODO: Remove in Smack 4.6
    private static ConcreteTest TEST_UNDER_EXECUTION;

    protected final Configuration config;

    protected TestRunResult testRunResult;
    SinttestDebugger sinttestDebugger;

    private SmackIntegrationTestEnvironment environment;
    protected XmppConnectionManager connectionManager;

    public enum TestType {
        Normal,
        LowLevel,
        SpecificLowLevel,
    }

    public static void main(String[] args) throws IOException, KeyManagementException,
            NoSuchAlgorithmException, SmackException, XMPPException, InterruptedException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Configuration config = Configuration.newConfiguration(args);

        SmackIntegrationTestFramework sinttest = new SmackIntegrationTestFramework(config);
        TestRunResult testRunResult = sinttest.run();

        System.exit(testRunResult.getExitStatus());
    }

    public static class JulTestRunResultProcessor implements TestRunResultProcessor {

        @Override
        public void process(final TestRunResult testRunResult) {
            for (Map.Entry<Class<? extends AbstractSmackIntTest>, Throwable> entry : testRunResult.impossibleTestClasses.entrySet()) {
                LOGGER.info("Could not run " + entry.getKey().getName() + " because: "
                    + entry.getValue().getLocalizedMessage());
            }
            for (TestNotPossible testNotPossible : testRunResult.impossibleIntegrationTests) {
                LOGGER.info("Could not run " + testNotPossible.concreteTest + " because: "
                    + testNotPossible.testNotPossibleException.getMessage());
            }
            for (SuccessfulTest successfulTest : testRunResult.successfulIntegrationTests) {
                LOGGER.info(successfulTest.concreteTest + " ✔");
            }
            final int successfulTests = testRunResult.successfulIntegrationTests.size();
            final int failedTests = testRunResult.failedIntegrationTests.size();
            final int availableTests = testRunResult.getNumberOfAvailableTests();
            LOGGER.info("SmackIntegrationTestFramework[" + testRunResult.testRunId + ']' + " finished: "
                + successfulTests + '/' + availableTests + " [" + failedTests + " failed]");

            if (!testRunResult.impossibleTestClasses.isEmpty() || !testRunResult.impossibleIntegrationTests.isEmpty()) {
                LOGGER.info("It was not possible to run all Smack Integration tests.");
            }

            if (failedTests == 0) {
                LOGGER.info("All possible Smack Integration Tests completed successfully. \\o/");
                return;
            }

            StringBuilder sb = new StringBuilder("💀 The following " + failedTests + " tests failed! 💀\n- ");
            StringUtils.appendTo(testRunResult.failedIntegrationTests, "\n- ", sb, t -> sb.append(t.concreteTest));
            LOGGER.warning(sb.toString());

            final SortedSet<String> bySpecification = new TreeSet<>();
            for (FailedTest failedTest : testRunResult.failedIntegrationTests) {
                final Throwable cause = failedTest.failureReason;
                LOGGER.log(Level.SEVERE, failedTest.concreteTest + " failed: " + cause, cause);
                if (failedTest.concreteTest.method.getDeclaringClass().isAnnotationPresent(SpecificationReference.class)) {
                    final String specificationReference = getSpecificationReference(failedTest.concreteTest.method);
                    if (specificationReference != null) {
                        bySpecification.add("- " + specificationReference + " (as tested by '" + failedTest.concreteTest + "')");
                    }
                }
            }
            if (!bySpecification.isEmpty()) {
                 LOGGER.log(Level.SEVERE, "The failed tests correspond to the following specifications:" + System.lineSeparator() + String.join(System.lineSeparator(), bySpecification));
            }
        }
    }

    private static String getSpecificationReference(Method method) {
        final SpecificationReference spec = method.getDeclaringClass().getAnnotation(SpecificationReference.class);
        if (spec == null || spec.document().isBlank()) {
            return null;
        }
        String line = spec.document().trim();
        if (!spec.version().isBlank()) {
            line += " (version " + spec.version() + ")";
        }

        final SmackIntegrationTest test = method.getAnnotation(SmackIntegrationTest.class);
        if (!test.section().isBlank()) {
            line += " section " + test.section().trim();
        }
        if (!test.quote().isBlank()) {
            line += ":\t\"" + test.quote().trim() + "\"";
        }
        assert !line.isBlank();
        return line;
    }

    public SmackIntegrationTestFramework(Configuration configuration) {
        this.config = configuration;
    }

    public synchronized TestRunResult run()
            throws KeyManagementException, NoSuchAlgorithmException, SmackException, IOException, XMPPException,
            InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // The DNS resolver is not really a per sinttest run setting. It is not even a per connection setting. Instead
        // it is a global setting, but we treat it like a per sinttest run setting.
        switch (config.dnsResolver) {
        case minidns:
            MiniDnsResolver.setup();
            break;
        case javax:
            JavaxResolver.setup();
            break;
        case dnsjava:
            DNSJavaResolver.setup();
            break;
        }

        testRunResult = new TestRunResult(config);
        sinttestDebugger = config.createSinttestDebugger(testRunResult.testRunStart, testRunResult.testRunId);
        // Create a connection manager *after* we created the testRunId (in testRunResult).
        this.connectionManager = new XmppConnectionManager(this);

        LOGGER.info("SmackIntegrationTestFramework [" + testRunResult.testRunId + ']' + ": Starting\nSmack version: " + Smack.getVersion());
        if (sinttestDebugger != null) {
            // JUL Debugger will not print any information until configured to print log messages of
            // level FINE
            // TODO configure JUL for log?
            SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smack.debugger.JulDebugger");
            SmackConfiguration.DEBUG = true;
        }
        if (config.replyTimeout > 0) {
            SmackConfiguration.setDefaultReplyTimeout(config.replyTimeout);
        }
        if (config.securityMode != SecurityMode.required && config.accountRegistration == AccountRegistration.inBandRegistration) {
            AccountManager.sensitiveOperationOverInsecureConnectionDefault(true);
        }
        // TODO print effective configuration

        String[] testPackages;
        if (config.testPackages == null || config.testPackages.isEmpty()) {
            testPackages = new String[] { "org.jivesoftware.smackx", "org.jivesoftware.smack", "org.igniterealtime.smackx", "org.igniterealtime.smack" };
        }
        else {
            testPackages = config.testPackages.toArray(new String[config.testPackages.size()]);
        }
        Reflections reflections = new Reflections(testPackages, Scanners.SubTypes,
                        Scanners.TypesAnnotated, Scanners.MethodsAnnotated, Scanners.MethodsParameter);
        Set<Class<? extends AbstractSmackIntegrationTest>> inttestClasses = reflections.getSubTypesOf(AbstractSmackIntegrationTest.class);
        Set<Class<? extends AbstractSmackLowLevelIntegrationTest>> lowLevelInttestClasses = reflections.getSubTypesOf(AbstractSmackLowLevelIntegrationTest.class);

        final int builtInTestCount = inttestClasses.size() + lowLevelInttestClasses.size();
        Set<Class<? extends AbstractSmackIntTest>> classes = new HashSet<>(builtInTestCount);
        classes.addAll(inttestClasses);
        classes.addAll(lowLevelInttestClasses);

        {
            // Remove all abstract classes.
            // TODO: This may be a good candidate for Java stream filtering once Smack is Android API 24 or higher.
            Iterator<Class<? extends AbstractSmackIntTest>> it = classes.iterator();
            while (it.hasNext()) {
                Class<? extends AbstractSmackIntTest> clazz = it.next();
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    it.remove();
                }
            }
        }

        if (classes.isEmpty()) {
            throw new IllegalStateException("No test classes in " + Arrays.toString(testPackages) + " found");
        }

        LOGGER.info("SmackIntegrationTestFramework [" + testRunResult.testRunId
                        + "]: Finished scanning for tests, preparing environment\n"
                        + "\tJava SE Platform version: " + Runtime.version());
        environment = prepareEnvironment();

        try {
            runTests(classes);
        }
        catch (Throwable t) {
            // Log the thrown Throwable to prevent it being shadowed in case the finally block below also throws.
            LOGGER.log(Level.SEVERE, "Unexpected abort because runTests() threw throwable", t);
            throw t;
        }
        finally {
            // Ensure that the accounts are deleted and disconnected before we continue
            connectionManager.disconnectAndCleanup();
        }

        for (final TestRunResultProcessor testRunResultProcessor : config.testRunResultProcessors) {
            testRunResultProcessor.process(testRunResult);
        }

        if (sinttestDebugger != null) {
            sinttestDebugger.onSinttestFinished(testRunResult);
            sinttestDebugger = null;
        }

        return testRunResult;
    }

    /**
     * Get the test under execution.
     * @return the test under execution
     * @deprecated use {@link SinttestDebugger} instead.
     */
    // TODO: Remove in Smack 4.6
    @Deprecated
    public static ConcreteTest getTestUnderExecution() {
        return TEST_UNDER_EXECUTION;
    }

    @SuppressWarnings({"Finally"})
    private void runTests(Set<Class<? extends AbstractSmackIntTest>> classes)
            throws InterruptedException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, SmackException, IOException, XMPPException {
        List<PreparedTest> tests = new ArrayList<>(classes.size());
        int numberOfAvailableTests = 0;

        List<Class<? extends AbstractSmackIntTest>> possiblySorted = config.sortTestClasses(classes);
        for (Class<? extends AbstractSmackIntTest> testClass : possiblySorted) {
            final String testClassName = testClass.getName();

            // TODO: Move the whole "skipping section" below one layer up?

            // Skip pseudo integration tests from src/test
            // Although Smack's gradle build files do not state that the 'main' sources classpath also contains the
            // 'test' classes. Some IDEs like Eclipse include them. As result, a real integration test run encounters
            // pseudo integration tests like the DummySmackIntegrationTest which always throws from src/test.
            // It is unclear why this apparently does not happen in the 4.3 branch, one likely cause is
            // compile project(path: ":smack-omemo", configuration: "testRuntime")
            // in
            // smack-integration-test/build.gradle:17
            // added after 4.3 was branched out with
            // 1f731f6318785a84b9741280d586a61dc37ecb2e
            // Now "gradle integrationTest" appear to be never affected by this, i.e., they are executed with the
            // correct classpath. Plain Eclipse, i.e. Smack imported into Eclipse after "gradle eclipse", appear
            // to include *all* classes. Which means those runs sooner or later try to execute
            // DummySmackIntegrationTest. Eclipse with buildship, the gradle plugin for Eclipse, always excludes
            // *all* src/test classes, which means they do not encounter DummySmackIntegrationTest, but this means
            // that the "compile project(path: ":smack-omemo", configuration: "testRuntime")" is not respected,
            // which leads to
            // Exception in thread "main" java.lang.NoClassDefFoundError: org/jivesoftware/smack/test/util/FileTestUtil
            //   at org.jivesoftware.smackx.ox.OXSecretKeyBackupIntegrationTest.<clinit>(OXSecretKeyBackupIntegrationTest.java:66)
            // See
            // - https://github.com/eclipse/buildship/issues/354 (Remove test dependencies from runtime classpath)
            // - https://bugs.eclipse.org/bugs/show_bug.cgi?id=482315 (Runtime classpath includes test dependencies)
            // - https://discuss.gradle.org/t/main-vs-test-compile-vs-runtime-classpaths-in-eclipse-once-and-for-all-how/17403
            // - https://bugs.eclipse.org/bugs/show_bug.cgi?id=376616 (Scope of dependencies has no effect on Eclipse compilation)
            if (!SINTTEST_UNIT_TEST && testClassName.startsWith("org.igniterealtime.smack.inttest.unittest")) {
                LOGGER.warning("Skipping integration test '" + testClassName + "' from src/test classpath (should not be in classpath)");
                continue;
            }

            if (!config.isClassEnabled(testClass)) {
                DisabledTestClass disabledTestClass = new DisabledTestClass(testClass, "Skipping test class " + testClassName + " because it is not enabled");
                testRunResult.disabledTestClasses.add(disabledTestClass);
                continue;
            }

            if (config.isClassDisabled(testClass)) {
                DisabledTestClass disabledTestClass = new DisabledTestClass(testClass, "Skipping test class " + testClassName + " because it is disabled");
                testRunResult.disabledTestClasses.add(disabledTestClass);
                continue;
            }

            if (!config.isAccountRegistrationPossible() && AbstractSmackLowLevelIntegrationTest.class.isAssignableFrom(testClass)) {
                testRunResult.impossibleTestClasses.put(testClass, new IllegalStateException("This is a low-level test. These cannot run without account registration."));
                continue;
            }

            final String specification;
            if (testClass.isAnnotationPresent(SpecificationReference.class)) {
                final SpecificationReference specificationReferenceAnnotation = testClass.getAnnotation(SpecificationReference.class);
                specification = Configuration.normalizeSpecification(specificationReferenceAnnotation.document());
            } else {
                specification = null;
            }

            if (!config.isSpecificationEnabled(specification)) {
                DisabledTestClass disabledTestClass = new DisabledTestClass(testClass, "Skipping test method " + testClass + " because it tests a specification ('" + specification + "') that is not enabled");
                testRunResult.disabledTestClasses.add(disabledTestClass);
                continue;
            }

            if (config.isSpecificationDisabled(specification)) {
                DisabledTestClass disabledTestClass = new DisabledTestClass(testClass, "Skipping test method " + testClass + " because it tests a specification ('" + specification + "') that is disabled");
                testRunResult.disabledTestClasses.add(disabledTestClass);
                continue;
            }

            final Constructor<? extends AbstractSmackIntTest> cons;
            try {
                cons = testClass.getConstructor(SmackIntegrationTestEnvironment.class);
            }
            catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(
                                "Smack Integration Test class does not declare the correct constructor. Is a public Constructor(SmackIntegrationTestEnvironment) missing?",
                                e);
            }

            final List<Method> smackIntegrationTestMethods;
            {
                Method[] testClassMethods = testClass.getMethods();
                smackIntegrationTestMethods = new ArrayList<>(testClassMethods.length);
                for (Method method : testClassMethods) {
                    if (!method.isAnnotationPresent(SmackIntegrationTest.class)) {
                        continue;
                    }
                    smackIntegrationTestMethods.add(method);
                }
            }

            if (smackIntegrationTestMethods.isEmpty()) {
                LOGGER.warning("No Smack integration test methods found in " + testClass);
                continue;
            }

            if (sinttestDebugger != null) {
                sinttestDebugger.onTestClassConstruction(cons);
            }
            final AbstractSmackIntTest test;
            try {
                test = cons.newInstance(environment);
            }
            catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                throwFatalException(cause);

                testRunResult.impossibleTestClasses.put(testClass, cause);
                continue;
            }

            XmppConnectionDescriptor<?, ?, ?> specificLowLevelConnectionDescriptor = null;
            final TestType testType;
            if (test instanceof AbstractSmackSpecificLowLevelIntegrationTest) {
                AbstractSmackSpecificLowLevelIntegrationTest<?> specificLowLevelTest = (AbstractSmackSpecificLowLevelIntegrationTest<?>) test;
                specificLowLevelConnectionDescriptor = specificLowLevelTest.getConnectionDescriptor();
                testType = TestType.SpecificLowLevel;
            } else if (test instanceof AbstractSmackLowLevelIntegrationTest) {
                testType = TestType.LowLevel;
            } else if (test instanceof AbstractSmackIntegrationTest) {
                testType = TestType.Normal;
            } else {
                throw new AssertionError();
            }

            // Verify the method signatures, throw in case a signature is incorrect.
            for (Method method : smackIntegrationTestMethods) {
                Class<?> retClass = method.getReturnType();
                if (!retClass.equals(Void.TYPE)) {
                    throw new IllegalStateException(
                            "SmackIntegrationTest annotation on" + method + " that does not return void");
                }
                switch (testType) {
                case Normal:
                    final Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length > 0) {
                        throw new IllegalStateException(
                                "SmackIntegrationTest annotation on " + method + " that takes arguments ");
                    }
                    break;
                case LowLevel:
                    verifyLowLevelTestMethod(method, AbstractXMPPConnection.class);
                    break;
                case SpecificLowLevel:
                    Class<? extends AbstractXMPPConnection> specificLowLevelConnectionClass = specificLowLevelConnectionDescriptor.getConnectionClass();
                    verifyLowLevelTestMethod(method, specificLowLevelConnectionClass);
                    break;
                }
            }

            Iterator<Method> it = smackIntegrationTestMethods.iterator();
            while (it.hasNext()) {
                final Method method = it.next();
                final String methodName = method.getName();
                if (!config.isMethodEnabled(method)) {
                    DisabledTest disabledTest = new DisabledTest(method, "Skipping test method " + methodName + " because it is not enabled");
                    testRunResult.disabledTests.add(disabledTest);
                    it.remove();
                    continue;
                }
                if (config.isMethodDisabled(method)) {
                    DisabledTest disabledTest = new DisabledTest(method, "Skipping test method " + methodName + " because it is disabled");
                    testRunResult.disabledTests.add(disabledTest);
                    it.remove();
                }
            }

            if (smackIntegrationTestMethods.isEmpty()) {
                LOGGER.info("All tests in " + testClassName + " are disabled");
                continue;
            }

            List<ConcreteTest> concreteTests = new ArrayList<>(smackIntegrationTestMethods.size());

            final List<Method> possiblySortedMethods = config.sortTestMethods(smackIntegrationTestMethods);

            for (Method testMethod : possiblySortedMethods) {
                switch (testType) {
                case Normal: {
                    ConcreteTest.Executor concreteTestExecutor = () -> {
                        AbstractSmackIntegrationTest abstractTest = (AbstractSmackIntegrationTest) test;

                        throwIfDisconnectedConnections(abstractTest, testMethod, "Cannot execute test of");
                        testMethod.invoke(test);
                        throwIfDisconnectedConnections(abstractTest, testMethod, "There where disconnected connections after executing");
                    };
                    ConcreteTest concreteTest = new ConcreteTest(testType, testMethod, concreteTestExecutor);
                    concreteTests.add(concreteTest);
                }
                    break;
                case LowLevel:
                case SpecificLowLevel:
                    LowLevelTestMethod lowLevelTestMethod = new LowLevelTestMethod(testMethod);
                    switch (testType) {
                    case LowLevel:
                        List<ConcreteTest> concreteLowLevelTests = invokeLowLevel(lowLevelTestMethod, (AbstractSmackLowLevelIntegrationTest) test);
                        concreteTests.addAll(concreteLowLevelTests);
                        break;
                    case SpecificLowLevel: {
                        ConcreteTest.Executor concreteTestExecutor = () -> invokeSpecificLowLevel(
                                lowLevelTestMethod, (AbstractSmackSpecificLowLevelIntegrationTest<?>) test);
                        ConcreteTest concreteTest = new ConcreteTest(testType, testMethod, concreteTestExecutor);
                        concreteTests.add(concreteTest);
                        break;
                    }
                    default:
                        throw new AssertionError();
                    }
                    break;
                }
            }

            // Instantiate the prepared test early as this will check the before and after class annotations.
            PreparedTest preparedTest = new PreparedTest(test, concreteTests);
            tests.add(preparedTest);

            numberOfAvailableTests += concreteTests.size();
        }

        // Print status information.
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Smack Integration Test Framework\n");
        sb.append("################################\n");
        if (config.verbose) {
            sb.append('\n');
            if (!testRunResult.disabledTestClasses.isEmpty()) {
                sb.append("The following test classes are disabled:\n");
                for (DisabledTestClass disabledTestClass : testRunResult.disabledTestClasses) {
                    disabledTestClass.appendTo(sb).append('\n');
                }
            }
            if (!testRunResult.disabledTests.isEmpty()) {
                sb.append("The following tests are disabled:\n");
                for (DisabledTest disabledTest : testRunResult.disabledTests) {
                    disabledTest.appendTo(sb).append('\n');
                }
            }
            sb.append('\n');
        }

        if (numberOfAvailableTests == 0) {
            var message = new StringBuilder("No integration tests selected");
            if (!testRunResult.impossibleTestClasses.isEmpty()) {
                message.append(". The following tests are not possible to execute: " + testRunResult.impossibleTestClasses);
            }
            throw new IllegalArgumentException(message.toString());
        }

        sb.append("Available tests: ").append(numberOfAvailableTests);
        if (!testRunResult.disabledTestClasses.isEmpty() || !testRunResult.disabledTests.isEmpty()) {
            sb.append(" (Disabled ").append(testRunResult.disabledTestClasses.size()).append(" classes")
              .append(" and ").append(testRunResult.disabledTests.size()).append(" tests)");
        }
        sb.append('\n');
        LOGGER.info(sb.toString());

        for (PreparedTest test : tests) {
            boolean successful = test.run();
            // Will only be not successful if a test failed and config.failFast is enabled.
            if (!successful) {
                break;
            }
        }

        if (!config.failFast) {
            // Assert that all tests in the 'tests' list produced a result.
            assert numberOfAvailableTests == testRunResult.getNumberOfAvailableTests();
        }
    }

    private boolean runConcreteTest(ConcreteTest concreteTest)
            throws InterruptedException, XMPPException, IOException, SmackException {
        LOGGER.info(concreteTest + " Start");
        var testStart = ZonedDateTime.now();
        if (sinttestDebugger != null) {
            sinttestDebugger.onTestStart(concreteTest, testStart);
        }

        try {
            concreteTest.executor.execute();
        }
        catch (InvocationTargetException e) {
            ZonedDateTime testEnd = ZonedDateTime.now();
            Throwable cause = e.getCause();
            if (cause instanceof TestNotPossibleException) {
                LOGGER.info(concreteTest + " is not possible");
                testRunResult.impossibleIntegrationTests.add(new TestNotPossible(concreteTest, testStart, testEnd,
                                null, (TestNotPossibleException) cause));
                return true;
            }
            Throwable nonFatalFailureReason;
            // junit asserts throw an AssertionError if they fail, those should not be
            // thrown up, as it would be done by throwFatalException()
            if (cause instanceof AssertionError) {
                nonFatalFailureReason = cause;
            } else {
                nonFatalFailureReason = throwFatalException(cause);
            }
            // An integration test failed
            testRunResult.failedIntegrationTests.add(new FailedTest(concreteTest, testStart, testEnd, null,
                            nonFatalFailureReason));
            if (sinttestDebugger != null) {
                sinttestDebugger.onTestFailure(concreteTest, testEnd, nonFatalFailureReason);
            }
            LOGGER.log(Level.SEVERE, concreteTest + " Failed", e);
            return false;
        }
        catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }

        var testEnd = ZonedDateTime.now();
        if (sinttestDebugger != null) {
            sinttestDebugger.onTestSuccess(concreteTest, testEnd);
        }
        LOGGER.info(concreteTest + " Success");
        testRunResult.successfulIntegrationTests.add(new SuccessfulTest(concreteTest, testStart, testEnd, null));
        return true;
    }

    private static void verifyLowLevelTestMethod(Method method,
                    Class<? extends AbstractXMPPConnection> connectionClass) {
        if (determineTestMethodParameterType(method, connectionClass) != null) {
            return;
        }

        throw new IllegalArgumentException(method + " is not a valid low level test method");
    }

    private List<ConcreteTest> invokeLowLevel(LowLevelTestMethod lowLevelTestMethod, AbstractSmackLowLevelIntegrationTest test) {
        Collection<? extends XmppConnectionDescriptor<?, ?, ?>> connectionDescriptors;
        if (lowLevelTestMethod.smackIntegrationTestAnnotation.onlyDefaultConnectionType()) {
            XmppConnectionDescriptor<?, ?, ?> defaultConnectionDescriptor = connectionManager.getDefaultConnectionDescriptor();
            connectionDescriptors = Collections.singleton(defaultConnectionDescriptor);
        } else {
            connectionDescriptors = connectionManager.getConnectionDescriptors();
        }

        List<ConcreteTest> resultingConcreteTests = new ArrayList<>(connectionDescriptors.size());

        for (XmppConnectionDescriptor<?, ?, ?> connectionDescriptor : connectionDescriptors) {
            String connectionNick = connectionDescriptor.getNickname();

            if (config.enabledConnections != null && !config.enabledConnections.contains(connectionNick)) {
                DisabledTest disabledTest = new DisabledTest(lowLevelTestMethod.testMethod, "Not creating test for " + lowLevelTestMethod + " with connection '" + connectionNick
                                + "', as this connection type is not enabled");
                testRunResult.disabledTests.add(disabledTest);
                continue;
            }

            if (config.disabledConnections != null && config.disabledConnections.contains(connectionNick)) {
                DisabledTest disabledTest = new DisabledTest(lowLevelTestMethod.testMethod, "Not creating test for " + lowLevelTestMethod + " with connection '" + connectionNick
                                + ", as this connection type is disabled");
                testRunResult.disabledTests.add(disabledTest);
                continue;
            }

            ConcreteTest.Executor executor = () -> lowLevelTestMethod.invoke(test, connectionDescriptor);
            ConcreteTest concreteTest = new ConcreteTest(TestType.LowLevel, lowLevelTestMethod.testMethod, executor, connectionDescriptor.getNickname());
            resultingConcreteTests.add(concreteTest);
        }

        return resultingConcreteTests;
    }

    private static <C extends AbstractXMPPConnection> void invokeSpecificLowLevel(LowLevelTestMethod testMethod,
            AbstractSmackSpecificLowLevelIntegrationTest<C> test)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException,
            SmackException, IOException, XMPPException {
        if (testMethod.smackIntegrationTestAnnotation.onlyDefaultConnectionType()) {
            throw new IllegalArgumentException("SpecificLowLevelTests must not have set onlyDefaultConnectionType");
        }

        XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor = test.getConnectionDescriptor();
        testMethod.invoke(test, connectionDescriptor);
    }

    protected SmackIntegrationTestEnvironment prepareEnvironment() throws SmackException,
                    IOException, XMPPException, InterruptedException, KeyManagementException,
                    NoSuchAlgorithmException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return connectionManager.prepareEnvironment();
    }

    enum AccountNum {
        One,
        Two,
        Three,
    }

    private static Exception throwFatalException(Throwable e) throws Error, NoResponseException,
                    InterruptedException {
        if (e instanceof InterruptedException) {
            throw (InterruptedException) e;
        }

        // We handle NullPointerException as a non-fatal exception, as they are mostly caused by an invalid reply where
        // an extension element is missing. Consider for example
        // assertEquals(StanzaError.Condition.foo, response.getError().getCondition())
        // Otherwise NPEs could be caused by an internal bug in Smack, e.g. missing null handling.
        if (e instanceof NullPointerException) {
            return (NullPointerException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        return (Exception) e;
    }

    @FunctionalInterface
    public interface TestRunResultProcessor {
        void process(SmackIntegrationTestFramework.TestRunResult testRunResult);
    }

    public static final class TestRunResult {

        /**
         * A short String of lowercase characters and numbers used to identify an integration test
         * run. We use lowercase characters because this string will eventually be part of the
         * localpart of the used JIDs (and the localpart is case-insensitive).
         */
        public final String testRunId = StringUtils.insecureRandomString(5).toLowerCase(Locale.US);

        public final ZonedDateTime testRunStart = ZonedDateTime.now();

        private final Configuration config;

        private final List<SuccessfulTest> successfulIntegrationTests = Collections.synchronizedList(new ArrayList<SuccessfulTest>());
        private final List<FailedTest> failedIntegrationTests = Collections.synchronizedList(new ArrayList<FailedTest>());
        private final List<TestNotPossible> impossibleIntegrationTests = Collections.synchronizedList(new ArrayList<TestNotPossible>());

        // TODO: Ideally three would only be a list of disabledTests, but since we do not process a disabled test class
        // any further, we can not determine the concrete disabled tests.
        private final List<DisabledTestClass> disabledTestClasses = Collections.synchronizedList(new ArrayList<>());
        private final List<DisabledTest> disabledTests = Collections.synchronizedList(new ArrayList<>());

        private final Map<Class<? extends AbstractSmackIntTest>, Throwable> impossibleTestClasses = new HashMap<>();

        TestRunResult(Configuration config) {
            this.config = config;
        }

        public String getTestRunId() {
            return testRunId;
        }

        public int getNumberOfAvailableTests() {
            return successfulIntegrationTests.size() + failedIntegrationTests.size() + impossibleIntegrationTests.size();
        }

        public List<SuccessfulTest> getSuccessfulTests() {
            return Collections.unmodifiableList(successfulIntegrationTests);
        }

        public List<FailedTest> getFailedTests() {
            return Collections.unmodifiableList(failedIntegrationTests);
        }

        public List<TestNotPossible> getNotPossibleTests() {
            return Collections.unmodifiableList(impossibleIntegrationTests);
        }

        public Map<Class<? extends AbstractSmackIntTest>, Throwable> getImpossibleTestClasses() {
            return Collections.unmodifiableMap(impossibleTestClasses);
        }

        public int getExitStatus() {
          int exitStatus = failedIntegrationTests.isEmpty() ? 0 : 2;
          if (config.failOnImpossibleTest && (!impossibleTestClasses.isEmpty() || !impossibleIntegrationTests.isEmpty())) {
              exitStatus += 4;
          }

          return exitStatus;
        }
    }

    final class PreparedTest {
        private final AbstractSmackIntTest test;
        private final List<ConcreteTest> concreteTests;

        private final Method beforeClassMethod;
        private final Method afterClassMethod;

        private PreparedTest(AbstractSmackIntTest test, List<ConcreteTest> concreteTests) {
            this.test = test;
            this.concreteTests = concreteTests;
            Class<? extends AbstractSmackIntTest> testClass = test.getClass();

            beforeClassMethod = getSinttestSpecialMethod(testClass, BeforeClass.class);
            afterClassMethod = getSinttestSpecialMethod(testClass, AfterClass.class);
        }

        public boolean run() throws InterruptedException, XMPPException, IOException, SmackException {
            try {
                // Run the @BeforeClass methods (if any)
                executeSinttestSpecialMethod(beforeClassMethod);

                for (ConcreteTest concreteTest : concreteTests) {
                    TEST_UNDER_EXECUTION = concreteTest;
                    boolean successful;
                    try {
                        successful = runConcreteTest(concreteTest);
                    } finally {
                        TEST_UNDER_EXECUTION = null;
                    }

                    if (config.failFast && !successful) {
                        return false;
                    }
                }
            }
            finally {
                executeSinttestSpecialMethod(afterClassMethod);
            }
            return true;
        }

        private void executeSinttestSpecialMethod(Method method) {
            if (method == null) {
                return;
            }

            try {
                method.invoke(test);
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                LOGGER.log(Level.SEVERE, "Exception executing " + method, e);
            }
            catch (IllegalArgumentException e) {
                throw new AssertionError(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Method getSinttestSpecialMethod(Class<? extends AbstractSmackIntTest> testClass, Class<? extends Annotation> annotation) {
        Set<Method> specialClassMethods = getAllMethods(testClass,
                        withAnnotation(annotation), withReturnType(Void.TYPE),
                        withParametersCount(0), withModifier(Modifier.PUBLIC
                                        ));

        // See if there are any methods that have a special but a wrong signature
        Set<Method> allSpecialClassMethods = getAllMethods(testClass, withAnnotation(annotation));
        allSpecialClassMethods.removeAll(specialClassMethods);
        if (!allSpecialClassMethods.isEmpty()) {
            throw new IllegalArgumentException(annotation + " methods with wrong signature found");
        }

        if (specialClassMethods.size() == 1) {
            return specialClassMethods.iterator().next();
        }
        else if (specialClassMethods.size() > 1) {
            throw new IllegalArgumentException("Only one @BeforeClass method allowed");
        }

        return null;
    }

    public static final class ConcreteTest {
        private final TestType testType;
        private final Method method;
        private final Executor executor;
        private final List<String> subdescriptons;

        private ConcreteTest(TestType testType, Method method, Executor executor, String... subdescriptions) {
            this.testType = testType;
            this.method = method;
            this.executor = executor;
            this.subdescriptons = List.of(subdescriptions);
        }

        private transient String stringCache;

        public TestType getTestType() {
            return testType;
        }

        public Method getMethod() {
            return method;
        }

        public List<String> getSubdescriptons() {
            return subdescriptons;
        }

        @Override
        public String toString() {
            if (stringCache != null) {
                return stringCache;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(method.getDeclaringClass().getSimpleName())
                .append('.')
                .append(method.getName())
                .append(" (")
                .append(testType.name());
            if (!subdescriptons.isEmpty()) {
                sb.append(", ");
                StringUtils.appendTo(subdescriptons, sb);
            }
            sb.append(')');

            stringCache = sb.toString();
            return stringCache;
        }

        private interface Executor {

            /**
             * Execute the test.
             *
             * @throws IllegalAccessException if there was an illegal access.
             * @throws InterruptedException if the calling thread was interrupted.
             * @throws InvocationTargetException if the reflective invoked test throws an exception.
             * @throws XMPPException in case an XMPPException happens when <em>preparing</em> the test.
             * @throws IOException in case an IOException happens when <em>preparing</em> the test.
             * @throws SmackException in case an SmackException happens when <em>preparing</em> the test.
             */
            void execute() throws IllegalAccessException, InterruptedException, InvocationTargetException,
                    XMPPException, IOException, SmackException;
        }
    }

    public static final class DisabledTestClass {
        private final Class<? extends AbstractSmackIntTest> testClass;
        private final String reason;

        private DisabledTestClass(Class<? extends AbstractSmackIntTest> testClass, String reason) {
            this.testClass = testClass;
            this.reason = reason;
        }

        public Class<? extends AbstractSmackIntTest> getTestClass() {
            return testClass;
        }

        public String getReason() {
            return reason;
        }

        public StringBuilder appendTo(StringBuilder sb) {
            return sb.append("Disabled ").append(testClass).append(" because ").append(reason);
        }
    }

    public static final class DisabledTest {
        private final Method method;
        private final String reason;

        private DisabledTest(Method method, String reason) {
            this.method = method;
            this.reason = reason;
        }

        public Method getMethod() {
            return method;
        }

        public String getReason() {
            return reason;
        }

        public StringBuilder appendTo(StringBuilder sb) {
            return sb.append("Disabled ").append(method).append(" because ").append(reason);
        }
    }

    private final class LowLevelTestMethod {

        private final Method testMethod;
        private final SmackIntegrationTest smackIntegrationTestAnnotation;
        private final TestMethodParameterType parameterType;

        private LowLevelTestMethod(Method testMethod) {
            this.testMethod = testMethod;

            smackIntegrationTestAnnotation = testMethod.getAnnotation(SmackIntegrationTest.class);
            assert smackIntegrationTestAnnotation != null;
            parameterType = determineTestMethodParameterType(testMethod);
        }

        private void invoke(AbstractSmackLowLevelIntegrationTest test,
                        XmppConnectionDescriptor<?, ?, ?> connectionDescriptor)
                        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                        InterruptedException, SmackException, IOException, XMPPException {
            switch (parameterType) {
            case singleConnectedConnection:
            case collectionOfConnections:
            case parameterListOfConnections:
                final boolean collectionOfConnections = parameterType == TestMethodParameterType.collectionOfConnections;

                final int connectionCount;
                if (collectionOfConnections) {
                    connectionCount = smackIntegrationTestAnnotation.connectionCount();
                    if (connectionCount < 1) {
                        throw new IllegalArgumentException(testMethod + " is annotated to use less than one connection ('"
                                        + connectionCount + ')');
                    }
                } else {
                    connectionCount = testMethod.getParameterCount();
                }

                List<? extends AbstractXMPPConnection> connections = connectionManager.constructConnectedConnections(
                                connectionDescriptor, connectionCount);

                if (collectionOfConnections) {
                    testMethod.invoke(test, connections);
                } else {
                    Object[] connectionsArray = new Object[connectionCount];
                    for (int i = 0; i < connectionsArray.length; i++) {
                        connectionsArray[i] = connections.remove(0);
                    }
                    testMethod.invoke(test, connectionsArray);
                }

                connectionManager.recycle(connections);
                break;
            case unconnectedConnectionSource:
                AbstractSmackLowLevelIntegrationTest.UnconnectedConnectionSource source = () -> {
                    try {
                        return environment.connectionManager.constructConnection(connectionDescriptor);
                    } catch (NoResponseException | XMPPErrorException | NotConnectedException
                                    | InterruptedException e) {
                        // TODO: Ideally we would wrap the exceptions in an unchecked exceptions, catch those unchecked
                        // exceptions below and throw the wrapped checked exception.
                        throw new RuntimeException(e);
                    }
                };
                testMethod.invoke(test, source);
                break;
            case noParamSpecificLowLevel:
                testMethod.invoke(test);
                break;
            }
        }

        @Override
        public String toString() {
            return testMethod.toString();
        }
    }

    enum TestMethodParameterType {
        /**
         * testMethod(Connection connection)
         */
        singleConnectedConnection,

        /**
         * testMethod(Collection&lt;Connection&gt;)
         * <p> It can also be a subclass of collection like List. In fact, the type of the parameter being List is expected to be the common case.
         */
        collectionOfConnections,

        /**
         * testMethod(Connection a, Connection b, Connection c)
         */
        parameterListOfConnections,

        /**
         * testMethod(UnconnectedConnectionSource unconnectedConnectionSource)
         */
        unconnectedConnectionSource,

        /**
         * A no-parameter method of a {@link AbstractSmackSpecificLowLevelIntegrationTest}.
         */
        noParamSpecificLowLevel,
    };

    static TestMethodParameterType determineTestMethodParameterType(Method testMethod) {
        return determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
    }

    static TestMethodParameterType determineTestMethodParameterType(Method testMethod, Class<? extends AbstractXMPPConnection> connectionClass) {
        Class<?>[] parameterTypes = testMethod.getParameterTypes();
        if (parameterTypes.length == 0) {
            if (AbstractSmackSpecificLowLevelIntegrationTest.class.isAssignableFrom(testMethod.getDeclaringClass())) {
                return TestMethodParameterType.noParamSpecificLowLevel;
            }
            return null;
        }

        if (parameterTypes.length > 1) {
            // If there are more parameters, then all must be assignable from the connection class.
            for (Class<?> parameterType : parameterTypes) {
                if (!connectionClass.isAssignableFrom(parameterType)) {
                    return null;
                }
            }

            return TestMethodParameterType.parameterListOfConnections;
        }

        // This method has exactly a single parameter.
        Class<?> soleParameter = parameterTypes[0];

        if (Collection.class.isAssignableFrom(soleParameter)) {
            // The sole parameter is assignable from collection, which means that it is a parameterized generic type.
            ParameterizedType soleParameterizedType = (ParameterizedType) testMethod.getGenericParameterTypes()[0];
            Type[] actualTypeArguments = soleParameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length != 1) {
                // The parameter list of the Collection has more than one type.
                return null;
            }

            Type soleActualTypeArgument = actualTypeArguments[0];
            if (!(soleActualTypeArgument instanceof Class<?>)) {
                return null;
            }

            Class<?> soleActualTypeArgumentAsClass = (Class<?>) soleActualTypeArgument;
            if (!connectionClass.isAssignableFrom(soleActualTypeArgumentAsClass)) {
                return null;
            }

            return TestMethodParameterType.collectionOfConnections;
        } else if (connectionClass.isAssignableFrom(soleParameter)) {
            return TestMethodParameterType.singleConnectedConnection;
        } else if (AbstractSmackLowLevelIntegrationTest.UnconnectedConnectionSource.class.isAssignableFrom(soleParameter)) {
            return TestMethodParameterType.unconnectedConnectionSource;
        }

        return null;
    }

    private static void throwIfDisconnectedConnections(AbstractSmackIntegrationTest abstractTest, Method testMethod, String message) throws IOException {
        List<XMPPConnection> disconnectedConnections = abstractTest.connections.stream().filter(c -> !c.isConnected()).collect(Collectors.toList());
        if (disconnectedConnections.isEmpty()) return;

        throw new IOException(message + " " + testMethod.getDeclaringClass().getSimpleName() + "."
                        + testMethod.getName() + ", as not all connections are connected. Disconnected connections: "
                        + disconnectedConnections);
    }
}
