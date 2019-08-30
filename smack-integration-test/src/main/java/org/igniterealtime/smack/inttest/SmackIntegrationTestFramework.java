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
package org.igniterealtime.smack.inttest;

import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withReturnType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.debugger.EnhancedDebuggerWindow;
import org.jivesoftware.smackx.iqregister.AccountManager;

import org.igniterealtime.smack.inttest.Configuration.AccountRegistration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class SmackIntegrationTestFramework<DC extends AbstractXMPPConnection> {

    private static final Logger LOGGER = Logger.getLogger(SmackIntegrationTestFramework.class.getName());

    public static boolean SINTTEST_UNIT_TEST = false;

    private final Class<DC> defaultConnectionClass;

    protected final Configuration config;

    protected TestRunResult testRunResult;

    private SmackIntegrationTestEnvironment<DC> environment;
    protected XmppConnectionManager<DC> connectionManager;

    public enum TestType {
        Normal,
        LowLevel,
        SpecificLowLevel,
    }

    public static void main(String[] args) throws IOException, KeyManagementException,
            NoSuchAlgorithmException, SmackException, XMPPException, InterruptedException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Configuration config = Configuration.newConfiguration(args);

        SmackIntegrationTestFramework<XMPPTCPConnection> sinttest = new SmackIntegrationTestFramework<>(config, XMPPTCPConnection.class);
        TestRunResult testRunResult = sinttest.run();

        for (Entry<Class<? extends AbstractSmackIntTest>, Throwable> entry : testRunResult.impossibleTestClasses.entrySet()) {
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
        final int totalIntegrationTests = successfulTests + failedTests;
        final int availableTests = testRunResult.getNumberOfAvailableTests();
        final int possibleTests = testRunResult.getNumberOfPossibleTests();
        LOGGER.info("SmackIntegrationTestFramework[" + testRunResult.testRunId + ']' + ": Finished ["
                        + successfulTests + '/' + totalIntegrationTests + "] (" + possibleTests + " test methods of " + availableTests + " where possible)");

        final int exitStatus;
        if (failedTests > 0) {
            LOGGER.warning("💀 The following " + failedTests + " tests failed! 💀");
            for (FailedTest failedTest : testRunResult.failedIntegrationTests) {
                final Throwable cause = failedTest.failureReason;
                LOGGER.log(Level.SEVERE, failedTest.concreteTest + " failed: " + cause, cause);
            }
            exitStatus = 2;
        } else {
            LOGGER.info("All possible Smack Integration Tests completed successfully. \\o/");
            exitStatus = 0;
        }

        switch (config.debugger) {
        case enhanced:
            EnhancedDebuggerWindow.getInstance().waitUntilClosed();
            break;
        default:
            break;
        }

        System.exit(exitStatus);
    }

    public SmackIntegrationTestFramework(Configuration configuration, Class<DC> defaultConnectionClass)
            throws KeyManagementException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchAlgorithmException, SmackException, IOException, XMPPException,
            InterruptedException {
        this.config = configuration;
        this.defaultConnectionClass = defaultConnectionClass;
    }

    public synchronized TestRunResult run()
            throws KeyManagementException, NoSuchAlgorithmException, SmackException, IOException, XMPPException,
            InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        testRunResult = new TestRunResult();

        // Create a connection manager *after* we created the testRunId (in testRunResult).
        this.connectionManager = new XmppConnectionManager<>(this, defaultConnectionClass);

        LOGGER.info("SmackIntegrationTestFramework [" + testRunResult.testRunId + ']' + ": Starting");
        if (config.debugger != Configuration.Debugger.none) {
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
            testPackages = new String[] { "org.jivesoftware.smackx", "org.jivesoftware.smack" };
        }
        else {
            testPackages = config.testPackages.toArray(new String[config.testPackages.size()]);
        }
        Reflections reflections = new Reflections(testPackages, new SubTypesScanner(),
                        new TypeAnnotationsScanner(), new MethodAnnotationsScanner(), new MethodParameterScanner());
        Set<Class<? extends AbstractSmackIntegrationTest>> inttestClasses = reflections.getSubTypesOf(AbstractSmackIntegrationTest.class);
        Set<Class<? extends AbstractSmackLowLevelIntegrationTest>> lowLevelInttestClasses = reflections.getSubTypesOf(AbstractSmackLowLevelIntegrationTest.class);

        Set<Class<? extends AbstractSmackIntTest>> classes = new HashSet<>(inttestClasses.size()
                        + lowLevelInttestClasses.size());
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
            throw new IllegalStateException("No test classes found");
        }

        LOGGER.info("SmackIntegrationTestFramework [" + testRunResult.testRunId
                        + "]: Finished scanning for tests, preparing environment");
        environment = prepareEnvironment();

        try {
            runTests(classes);
        }
        finally {
            // Ensure that the accounts are deleted and disconnected before we continue
            connectionManager.disconnectAndCleanup();
        }

        return testRunResult;
    }

    @SuppressWarnings({"unchecked", "Finally"})
    private void runTests(Set<Class<? extends AbstractSmackIntTest>> classes)
            throws InterruptedException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, SmackException, IOException, XMPPException {
        for (Class<? extends AbstractSmackIntTest> testClass : classes) {
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
                LOGGER.finer("Skipping integration test '" + testClassName + "' from src/test classpath");
                continue;
            }

            if (config.enabledTests != null && !isInSet(testClass, config.enabledTests)) {
                LOGGER.info("Skipping test class " + testClassName + " because it is not enabled");
                continue;
            }

            if (isInSet(testClass, config.disabledTests)) {
                LOGGER.info("Skipping test class " + testClassName + " because it is disalbed");
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

            testRunResult.numberOfAvailableTestMethods.addAndGet(smackIntegrationTestMethods.size());

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

            Class<? extends AbstractXMPPConnection> specificLowLevelConnectionClass = null;
            final TestType testType;
            if (test instanceof AbstractSmackSpecificLowLevelIntegrationTest) {
                AbstractSmackSpecificLowLevelIntegrationTest<?> specificLowLevelTest = (AbstractSmackSpecificLowLevelIntegrationTest<?>) test;
                specificLowLevelConnectionClass = specificLowLevelTest.getConnectionClass();
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
                                "SmackIntegrationTest annotaton on " + method + " that takes arguments ");
                    }
                    break;
                case LowLevel:
                    verifyLowLevelTestMethod(method, AbstractXMPPConnection.class);
                    break;
                case SpecificLowLevel:
                    verifyLowLevelTestMethod(method, specificLowLevelConnectionClass);
                    break;
                }
            }

            Iterator<Method> it = smackIntegrationTestMethods.iterator();
            while (it.hasNext()) {
                final Method method = it.next();
                final String methodName = method.getName();
                if (config.enabledTests != null && !(config.enabledTests.contains(methodName)
                                || isInSet(testClass, config.enabledTests))) {
                    LOGGER.fine("Skipping test method " + methodName + " because it is not enabled");
                    it.remove();
                    continue;
                }
                if (config.disabledTests != null && config.disabledTests.contains(methodName)) {
                    LOGGER.info("Skipping test method " + methodName + " because it is disabled");
                    it.remove();
                    continue;
                }
            }

            if (smackIntegrationTestMethods.isEmpty()) {
                LOGGER.info("All tests in " + testClassName + " are disabled");
                continue;
            }

            final int detectedTestMethodsCount = smackIntegrationTestMethods.size();
            testRunResult.numberOfPossibleTestMethods.addAndGet(detectedTestMethodsCount);

            try {
                // Run the @BeforeClass methods (if any)
                Set<Method> beforeClassMethods = getAllMethods(testClass,
                                withAnnotation(BeforeClass.class), withReturnType(Void.TYPE),
                                withParametersCount(0), withModifier(Modifier.PUBLIC
                                                ));

                // See if there are any methods that have the @BeforeClassAnnotation but a wrong signature
                Set<Method> allBeforeClassMethods =  getAllMethods(testClass, withAnnotation(BeforeClass.class));
                allBeforeClassMethods.removeAll(beforeClassMethods);
                if (!allBeforeClassMethods.isEmpty()) {
                    throw new IllegalArgumentException("@BeforeClass methods with wrong signature found");
                }

                if (beforeClassMethods.size() == 1) {
                    Method beforeClassMethod = beforeClassMethods.iterator().next();
                    LOGGER.info("Executing @BeforeClass method of " + testClass);
                    try {
                        beforeClassMethod.invoke(test);
                    }
                    catch (InvocationTargetException | IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, "Exception executing @BeforeClass method", e);
                    }
                    catch (IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
                else if (beforeClassMethods.size() > 1) {
                    throw new IllegalArgumentException("Only one @BeforeClass method allowed");
                }

                for (Method testMethod : smackIntegrationTestMethods) {
                    List<ConcreteTest> concreteTests = null;
                    switch (testType) {
                    case Normal: {
                        ConcreteTest.Executor concreteTestExecutor = () -> testMethod.invoke(test);
                        ConcreteTest concreteTest = new ConcreteTest(testType, testMethod, concreteTestExecutor);
                        concreteTests = Collections.singletonList(concreteTest);
                    }
                        break;
                    case LowLevel:
                    case SpecificLowLevel:
                        LowLevelTestMethod lowLevelTestMethod = new LowLevelTestMethod(testMethod);
                        switch (testType) {
                        case LowLevel:
                            concreteTests = invokeLowLevel(lowLevelTestMethod, (AbstractSmackLowLevelIntegrationTest) test);
                            break;
                        case SpecificLowLevel: {
                            ConcreteTest.Executor concreteTestExecutor = () -> invokeSpecificLowLevel(
                                    lowLevelTestMethod, (AbstractSmackSpecificLowLevelIntegrationTest<?>) test);
                            ConcreteTest concreteTest = new ConcreteTest(testType, testMethod, concreteTestExecutor);
                            concreteTests = Collections.singletonList(concreteTest);
                            break;
                        }
                        default:
                            throw new AssertionError();
                        }
                        break;
                    }

                    for (ConcreteTest concreteTest : concreteTests) {
                        runConcreteTest(concreteTest);
                    }
                }
            }
            finally {
                // Run the @AfterClass method (if any)
                Set<Method> afterClassMethods = getAllMethods(testClass,
                                withAnnotation(AfterClass.class), withReturnType(Void.TYPE),
                                withParametersCount(0), withModifier(Modifier.PUBLIC
                                                ));

                // See if there are any methods that have the @AfterClassAnnotation but a wrong signature
                Set<Method> allAfterClassMethods =  getAllMethods(testClass, withAnnotation(AfterClass.class));
                allAfterClassMethods.removeAll(afterClassMethods);
                if (!allAfterClassMethods.isEmpty()) {
                    throw new IllegalArgumentException("@AfterClass methods with wrong signature found");
                }

                if (afterClassMethods.size() == 1) {
                    Method afterClassMethod = afterClassMethods.iterator().next();
                    LOGGER.info("Executing @AfterClass method of " + testClass);
                    try {
                        afterClassMethod.invoke(test);
                    }
                    catch (InvocationTargetException | IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, "Exception executing @AfterClass method", e);
                    }
                    catch (IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
                else if (afterClassMethods.size() > 1) {
                    throw new IllegalArgumentException("Only one @AfterClass method allowed");
                }
            }
        }
    }

    private void runConcreteTest(ConcreteTest concreteTest)
            throws InterruptedException, XMPPException, IOException, SmackException {
        LOGGER.info(concreteTest + " Start");
        long testStart = System.currentTimeMillis();
        try {
            concreteTest.executor.execute();
            long testEnd = System.currentTimeMillis();
            LOGGER.info(concreteTest + " Success");
            testRunResult.successfulIntegrationTests.add(new SuccessfulTest(concreteTest, testStart, testEnd, null));
        }
        catch (InvocationTargetException e) {
            long testEnd = System.currentTimeMillis();
            Throwable cause = e.getCause();
            if (cause instanceof TestNotPossibleException) {
                LOGGER.info(concreteTest + " is not possible");
                testRunResult.impossibleIntegrationTests.add(new TestNotPossible(concreteTest, testStart, testEnd,
                                null, (TestNotPossibleException) cause));
                return;
            }
            Throwable nonFatalFailureReason;
            // junit assert's throw an AssertionError if they fail, those should not be
            // thrown up, as it would be done by throwFatalException()
            if (cause instanceof AssertionError) {
                nonFatalFailureReason = cause;
            } else {
                nonFatalFailureReason = throwFatalException(cause);
            }
            // An integration test failed
            testRunResult.failedIntegrationTests.add(new FailedTest(concreteTest, testStart, testEnd, null,
                            nonFatalFailureReason));
            LOGGER.log(Level.SEVERE, concreteTest + " Failed", e);
        }
        catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static void verifyLowLevelTestMethod(Method method,
                    Class<? extends AbstractXMPPConnection> connectionClass) {
        if (!testMethodParametersIsListOfConnections(method, connectionClass)
                        && !testMethodParametersVarargsConnections(method, connectionClass)) {
            throw new IllegalArgumentException(method + " is not a valid low level test method");
        }
    }

    private List<ConcreteTest> invokeLowLevel(LowLevelTestMethod lowLevelTestMethod, AbstractSmackLowLevelIntegrationTest test) {
        Set<Class<? extends AbstractXMPPConnection>> connectionClasses;
        if (lowLevelTestMethod.smackIntegrationTestAnnotation.onlyDefaultConnectionType()) {
            Class<? extends AbstractXMPPConnection> defaultConnectionClass = connectionManager.getDefaultConnectionClass();
            connectionClasses = Collections.singleton(defaultConnectionClass);
        } else {
            connectionClasses = connectionManager.getConnectionClasses();
        }

        List<ConcreteTest> resultingConcreteTests = new ArrayList<>(connectionClasses.size());

        for (Class<? extends AbstractXMPPConnection> connectionClass : connectionClasses) {
            ConcreteTest.Executor executor = () -> lowLevelTestMethod.invoke(test, connectionClass);
            ConcreteTest concreteTest = new ConcreteTest(TestType.LowLevel, lowLevelTestMethod.testMethod, executor, connectionClass.getSimpleName());
            resultingConcreteTests.add(concreteTest);
        }

        return resultingConcreteTests;
    }

    private <C extends AbstractXMPPConnection> void invokeSpecificLowLevel(LowLevelTestMethod testMethod,
            AbstractSmackSpecificLowLevelIntegrationTest<C> test)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException,
            SmackException, IOException, XMPPException {
        if (testMethod.smackIntegrationTestAnnotation.onlyDefaultConnectionType()) {
            throw new IllegalArgumentException("SpecificLowLevelTests must not have set onlyDefaultConnectionType");
        }
        Class<C> connectionClass = test.getConnectionClass();
        testMethod.invoke(test, connectionClass);
    }

    protected SmackIntegrationTestEnvironment<DC> prepareEnvironment() throws SmackException,
                    IOException, XMPPException, InterruptedException, KeyManagementException,
                    NoSuchAlgorithmException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return connectionManager.prepareEnvironment();
    }

    enum AccountNum {
        One,
        Two,
        Three,
    }

    static XMPPTCPConnectionConfiguration.Builder getConnectionConfigurationBuilder(Configuration config) {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

        config.configurationApplier.applyConfigurationTo(builder);

        return builder;
    }

    private static Exception throwFatalException(Throwable e) throws Error, NoResponseException,
                    InterruptedException {
        if (e instanceof NoResponseException) {
            throw (NoResponseException) e;
        }
        if (e instanceof InterruptedException) {
            throw (InterruptedException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        return (Exception) e;
    }

    private static boolean isInSet(Class<?> clz, Set<String> classes) {
        if (classes == null) {
            return false;
        }
        final String className = clz.getName();
        final String unqualifiedClassName = clz.getSimpleName();
        return classes.contains(className) || classes.contains(unqualifiedClassName);
    }

    public static final class TestRunResult {

        /**
         * A short String of lowercase characters and numbers used to identify a integration test
         * run. We use lowercase characters because this string will eventually be part of the
         * localpart of the used JIDs (and the localpart is case insensitive).
         */
        public final String testRunId = StringUtils.insecureRandomString(5).toLowerCase(Locale.US);

        private final List<SuccessfulTest> successfulIntegrationTests = Collections.synchronizedList(new LinkedList<SuccessfulTest>());
        private final List<FailedTest> failedIntegrationTests = Collections.synchronizedList(new LinkedList<FailedTest>());
        private final List<TestNotPossible> impossibleIntegrationTests = Collections.synchronizedList(new LinkedList<TestNotPossible>());
        private final Map<Class<? extends AbstractSmackIntTest>, Throwable> impossibleTestClasses = new HashMap<>();
        private final AtomicInteger numberOfAvailableTestMethods = new AtomicInteger();
        private final AtomicInteger numberOfPossibleTestMethods = new AtomicInteger();

        TestRunResult() {
        }

        public String getTestRunId() {
            return testRunId;
        }

        public int getNumberOfAvailableTests() {
            return numberOfAvailableTestMethods.get();
        }

        public int getNumberOfPossibleTests() {
            return numberOfPossibleTestMethods.get();
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
    }

    static final class ConcreteTest {
        private final TestType testType;
        private final Method method;
        private final Executor executor;
        private final String[] subdescriptons;

        private ConcreteTest(TestType testType, Method method, Executor executor, String... subdescriptions) {
            this.testType = testType;
            this.method = method;
            this.executor = executor;
            this.subdescriptons = subdescriptions;
        }

        private transient String stringCache;

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
            final String SUBDESCRIPTION_DELIMITER = ", ";
            sb.append(SUBDESCRIPTION_DELIMITER);

            for (String subdescripton : subdescriptons) {
                sb.append(subdescripton).append(SUBDESCRIPTION_DELIMITER);
            }
            sb.setLength(sb.length() - SUBDESCRIPTION_DELIMITER.length());
            sb.append(')');

            stringCache = sb.toString();
            return stringCache;
        }

        private interface Executor {

            /**
             * Execute the test.
             *
             * @throws IllegalAccessException
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

    private final class LowLevelTestMethod {
        private final Method testMethod;
        private final SmackIntegrationTest smackIntegrationTestAnnotation;
        private final boolean parameterListOfConnections;

        private LowLevelTestMethod(Method testMethod) {
            this.testMethod = testMethod;

            smackIntegrationTestAnnotation = testMethod.getAnnotation(SmackIntegrationTest.class);
            assert smackIntegrationTestAnnotation != null;
            parameterListOfConnections = testMethodParametersIsListOfConnections(testMethod);
        }

        private void invoke(AbstractSmackLowLevelIntegrationTest test,
                        Class<? extends AbstractXMPPConnection> connectionClass)
                        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                        InterruptedException, SmackException, IOException, XMPPException {
            final int connectionCount;
            if (parameterListOfConnections) {
                connectionCount = smackIntegrationTestAnnotation.connectionCount();
                if (connectionCount < 1) {
                    throw new IllegalArgumentException(testMethod + " is annotated to use less than one connection ('"
                                    + connectionCount + ')');
                }
            } else {
                connectionCount = testMethod.getParameterCount();
            }

            List<? extends AbstractXMPPConnection> connections = connectionManager.constructConnectedConnections(
                            connectionClass, connectionCount);

            if (parameterListOfConnections) {
                testMethod.invoke(test, connections);
            } else {
                Object[] connectionsArray = new Object[connectionCount];
                for (int i = 0; i < connectionsArray.length; i++) {
                    connectionsArray[i] = connections.remove(0);
                }
                testMethod.invoke(test, connectionsArray);
            }
        }
    }

    private static boolean testMethodParametersIsListOfConnections(Method testMethod) {
        return testMethodParametersIsListOfConnections(testMethod, AbstractXMPPConnection.class);
    }

    static boolean testMethodParametersIsListOfConnections(Method testMethod, Class<? extends AbstractXMPPConnection> connectionClass) {
        Type[] parameterTypes = testMethod.getGenericParameterTypes();
        if (parameterTypes.length != 1) {
            return false;
        }
        Class<?> soleParameter = testMethod.getParameterTypes()[0];
        if (!Collection.class.isAssignableFrom(soleParameter)) {
            return false;
        }

        ParameterizedType soleParameterizedType = (ParameterizedType) parameterTypes[0];
        Type[] actualTypeArguments = soleParameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            return false;
        }

        Type soleActualTypeArgument = actualTypeArguments[0];
        if (!(soleActualTypeArgument instanceof Class<?>)) {
            return false;
        }
        Class<?> soleActualTypeArgumentAsClass = (Class<?>) soleActualTypeArgument;
        if (!connectionClass.isAssignableFrom(soleActualTypeArgumentAsClass)) {
            return false;
        }

        return true;
    }

    static boolean testMethodParametersVarargsConnections(Method testMethod, Class<? extends AbstractXMPPConnection> connectionClass) {
        Class<?>[] parameterTypes = testMethod.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            if (!parameterType.isAssignableFrom(connectionClass)) {
                return false;
            }
        }

        return true;
    }
}
