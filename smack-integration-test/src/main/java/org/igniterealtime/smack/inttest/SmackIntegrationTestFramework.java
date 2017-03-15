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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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

import org.igniterealtime.smack.inttest.IntTestUtil.UsernameAndPassword;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration.Builder;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class SmackIntegrationTestFramework {

    private static final Logger LOGGER = Logger.getLogger(SmackIntegrationTestFramework.class.getName());
    private static final char CLASS_METHOD_SEP = '#';

    protected final Configuration config;
    protected TestRunResult testRunResult;
    private SmackIntegrationTestEnvironment environment;

    public enum TestType {
        Normal,
        LowLevel,
    }

    public static void main(String[] args) throws IOException, KeyManagementException,
                    NoSuchAlgorithmException, SmackException, XMPPException, InterruptedException {
        Configuration config = Configuration.newConfiguration();

        SmackIntegrationTestFramework sinttest = new SmackIntegrationTestFramework(config);
        TestRunResult testRunResult = sinttest.run();

        for (Entry<Class<? extends AbstractSmackIntTest>, String> entry : testRunResult.impossibleTestClasses.entrySet()) {
            LOGGER.info("Could not run " + entry.getKey().getName() + " because: "
                            + entry.getValue());
        }
        for (TestNotPossible testNotPossible : testRunResult.impossibleTestMethods) {
            LOGGER.info("Could not run " + testNotPossible.testMethod.getName() + " because: "
                            + testNotPossible.testNotPossibleException.getMessage());
        }
        final int successfulTests = testRunResult.successfulTests.size();
        final int availableTests = testRunResult.getNumberOfAvailableTests();
        final int possibleTests = testRunResult.getNumberOfPossibleTests();
        LOGGER.info("SmackIntegrationTestFramework[" + testRunResult.testRunId + ']' + ": Finished ["
                        + successfulTests + '/' + possibleTests + "] (of " + availableTests + " available tests)");
        if (!testRunResult.failedIntegrationTests.isEmpty()) {
            final int failedTests = testRunResult.failedIntegrationTests.size();
            LOGGER.warning("The following " + failedTests + " tests failed!");
            for (FailedTest failedTest : testRunResult.failedIntegrationTests) {
                final Method method = failedTest.testMethod;
                final String className = method.getDeclaringClass().getName();
                final String methodName = method.getName();
                final Throwable cause = failedTest.failureReason;
                LOGGER.severe(className + CLASS_METHOD_SEP + methodName + " failed: " + cause);
            }
            System.exit(2);
        } else {
            LOGGER.info("All possible Smack Integration Tests completed successfully. \\o/");
        }
        System.exit(0);
    }

    public SmackIntegrationTestFramework(Configuration configuration) {
        this.config = configuration;
    }

    public synchronized TestRunResult run() throws KeyManagementException, NoSuchAlgorithmException, SmackException,
                    IOException, XMPPException, InterruptedException {
        testRunResult = new TestRunResult();
        LOGGER.info("SmackIntegrationTestFramework [" + testRunResult.testRunId + ']' + ": Starting");
        if (config.debug) {
            // JUL Debugger will not print any information until configured to print log messages of
            // level FINE
            // TODO configure JUL for log?
            SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smack.debugger.JulDebugger");
            SmackConfiguration.DEBUG = true;
        }
        if (config.replyTimeout > 0) {
            SmackConfiguration.setDefaultReplyTimeout(config.replyTimeout);
        }
        if (config.securityMode != SecurityMode.required) {
            AccountManager.sensitiveOperationOverInsecureConnectionDefault(true);
        }
        // TODO print effective configuration

        String[] testPackages;
        if (config.testPackages == null) {
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
            disconnectAndMaybeDelete(environment.conOne);
            disconnectAndMaybeDelete(environment.conTwo);
            disconnectAndMaybeDelete(environment.conThree);
        }

        return testRunResult;
    }

    @SuppressWarnings({"unchecked", "Finally"})
    private void runTests(Set<Class<? extends AbstractSmackIntTest>> classes)
                    throws NoResponseException, InterruptedException {
        for (Class<? extends AbstractSmackIntTest> testClass : classes) {
            final String testClassName = testClass.getName();

            if (config.enabledTests != null && !isInSet(testClass, config.enabledTests)) {
                LOGGER.info("Skipping test class " + testClassName + " because it is not enabled");
                continue;
            }

            if (isInSet(testClass, config.disabledTests)) {
                LOGGER.info("Skipping test class " + testClassName + " because it is disalbed");
                continue;
            }

            TestType testType;
            if (AbstractSmackLowLevelIntegrationTest.class.isAssignableFrom(testClass)) {
                testType = TestType.LowLevel;
            } else if (AbstractSmackIntegrationTest.class.isAssignableFrom(testClass)) {
                testType = TestType.Normal;
            } else {
                throw new AssertionError();
            }
            List<Method> smackIntegrationTestMethods = new LinkedList<>();
            for (Method method : testClass.getMethods()) {
                if (!method.isAnnotationPresent(SmackIntegrationTest.class)) {
                    continue;
                }
                Class<?> retClass = method.getReturnType();
                if (!(retClass.equals(Void.TYPE))) {
                    LOGGER.warning("SmackIntegrationTest annotation on method that does not return void");
                    continue;
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                switch (testType) {
                case Normal:
                    if (method.getParameterTypes().length > 0) {
                        LOGGER.warning("SmackIntegrationTest annotaton on method that takes arguments ");
                        continue;
                    }
                    break;
                case LowLevel:
                    for (Class<?> parameterType : parameterTypes) {
                        if (!parameterType.isAssignableFrom(XMPPTCPConnection.class)) {
                            LOGGER.warning("SmackIntegrationTest low-level test method declares parameter that is not of type XMPPTCPConnection");
                        }
                    }
                    break;
                }
                smackIntegrationTestMethods.add(method);
            }

            if (smackIntegrationTestMethods.isEmpty()) {
                LOGGER.warning("No integration test methods found");
                continue;
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
            testRunResult.numberOfAvailableTests.addAndGet(detectedTestMethodsCount);
            testRunResult.numberOfPossibleTests.addAndGet(detectedTestMethodsCount);

            AbstractSmackIntTest test;
            switch (testType) {
            case Normal: {
                Constructor<? extends AbstractSmackIntegrationTest> cons;
                try {
                    cons = ((Class<? extends AbstractSmackIntegrationTest>) testClass).getConstructor(SmackIntegrationTestEnvironment.class);
                }
                catch (NoSuchMethodException | SecurityException e) {
                    LOGGER.log(Level.WARNING,
                                    "Smack Integration Test class could not get constructed (public Con)structor(SmackIntegrationTestEnvironment) missing?)",
                                    e);
                    continue;
                }

                try {
                    test = cons.newInstance(environment);
                }
                catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TestNotPossibleException) {
                        testRunResult.impossibleTestClasses.put(testClass, cause.getMessage());
                        testRunResult.numberOfPossibleTests.addAndGet(-detectedTestMethodsCount);
                    }
                    else {
                        throwFatalException(cause);
                        LOGGER.log(Level.WARNING, "Could not construct test class", e);
                    }
                    continue;
                }
                catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "todo", e);
                    continue;
                }
                } break;
            case LowLevel: {
                Constructor<? extends AbstractSmackLowLevelIntegrationTest> cons;
                try {
                    cons = ((Class<? extends AbstractSmackLowLevelIntegrationTest>) testClass).getConstructor(
                                    SmackIntegrationTestEnvironment.class);
                }
                catch (NoSuchMethodException | SecurityException e) {
                    LOGGER.log(Level.WARNING,
                                    "Smack Integration Test class could not get constructed (public Con)structor(SmackIntegrationTestEnvironment) missing?)",
                                    e);
                    continue;
                }

                try {
                    test = cons.newInstance(environment);
                }
                catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TestNotPossibleException) {
                        testRunResult.impossibleTestClasses.put(testClass, cause.getMessage());
                        testRunResult.numberOfPossibleTests.addAndGet(-detectedTestMethodsCount);
                    }
                    else {
                        throwFatalException(cause);
                        LOGGER.log(Level.WARNING, "Could not construct test class", e);
                    }
                    continue;
                }
                catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "todo", e);
                    continue;
                }
                } break;
                default:
                    throw new AssertionError();
            }

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
                    final String testPrefix = testClass.getSimpleName() + '.'
                                    + testMethod.getName() + " (" + testType + "): ";
                    // Invoke all test methods on the test instance
                    LOGGER.info(testPrefix + "Start");
                    long testStart = System.currentTimeMillis();
                    try {
                        switch (testType) {
                        case Normal:
                            testMethod.invoke(test);
                            break;
                        case LowLevel:
                            invokeLowLevel(testMethod, test);
                            break;
                        }
                        LOGGER.info(testPrefix + "Success");
                        long testEnd = System.currentTimeMillis();
                        testRunResult.successfulTests.add(new SuccessfulTest(testMethod, testStart, testEnd, null));
                    }
                    catch (InvocationTargetException e) {
                        long testEnd = System.currentTimeMillis();
                        Throwable cause = e.getCause();
                        if (cause instanceof TestNotPossibleException) {
                            LOGGER.info(testPrefix + "Not possible");
                            testRunResult.impossibleTestMethods.add(new TestNotPossible(testMethod, testStart, testEnd,
                                            null, (TestNotPossibleException) cause));
                            continue;
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
                        testRunResult.failedIntegrationTests.add(new FailedTest(testMethod, testStart, testEnd, null,
                                        nonFatalFailureReason));
                        LOGGER.log(Level.SEVERE, testPrefix + "Failed", e);
                    }
                    catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new AssertionError(e);
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

    private void invokeLowLevel(Method testMethod, AbstractSmackIntTest test) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        // We have checked before that every parameter, if any, is of type XMPPTCPConnection
        final int numberOfConnections = testMethod.getParameterTypes().length;
        XMPPTCPConnection[] connections = null;
        try {
            if (numberOfConnections > 0 && !config.isAccountRegistrationPossible()) {
                throw new TestNotPossibleException(
                                "Must create accounts for this test, but it's not enabled");
            }
            connections = new XMPPTCPConnection[numberOfConnections];
            for (int i = 0; i < numberOfConnections; ++i) {
                connections[i] = getConnectedConnection(environment, i);
            }
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            // Behave like this was an InvocationTargetException
            throw new InvocationTargetException(e);
        }
        try {
            testMethod.invoke(test, (Object[]) connections);
        }
        finally {
            for (int i = 0; i < numberOfConnections; ++i) {
                IntTestUtil.disconnectAndMaybeDelete(connections[i], config);
            }
        }
    }

    protected void disconnectAndMaybeDelete(XMPPTCPConnection connection) throws InterruptedException {
        IntTestUtil.disconnectAndMaybeDelete(connection, config);
    }

    protected SmackIntegrationTestEnvironment prepareEnvironment() throws SmackException,
                    IOException, XMPPException, InterruptedException, KeyManagementException,
                    NoSuchAlgorithmException {
        XMPPTCPConnection conOne = null;
        XMPPTCPConnection conTwo = null;
        XMPPTCPConnection conThree = null;
        try {
            conOne = getConnectedConnectionFor(AccountNum.One);
            conTwo = getConnectedConnectionFor(AccountNum.Two);
            conThree = getConnectedConnectionFor(AccountNum.Three);
        }
        catch (Exception e) {
            // TODO Reverse the order, i.e. conThree should be disconnected first.
            if (conOne != null) {
                conOne.disconnect();
            }
            if (conTwo != null) {
                conTwo.disconnect();
            }
            if (conThree != null) {
                conThree.disconnect();
            }
            throw e;
        }

        return new SmackIntegrationTestEnvironment(conOne, conTwo, conThree, testRunResult.testRunId, config);
    }

    enum AccountNum {
        One,
        Two,
        Three,
    }

    private static final String USERNAME_PREFIX = "smack-inttest";

    private XMPPTCPConnection getConnectedConnectionFor(AccountNum accountNum)
                    throws SmackException, IOException, XMPPException, InterruptedException,
                    KeyManagementException, NoSuchAlgorithmException {
        String middlefix;
        String accountUsername;
        String accountPassword;
        switch (accountNum) {
        case One:
            accountUsername = config.accountOneUsername;
            accountPassword = config.accountOnePassword;
            middlefix = "one";
            break;
        case Two:
            accountUsername = config.accountTwoUsername;
            accountPassword = config.accountTwoPassword;
            middlefix = "two";
            break;
        case Three:
            accountUsername = config.accountThreeUsername;
            accountPassword = config.accountThreePassword;
            middlefix = "three";
            break;
        default:
            throw new IllegalStateException();
        }
        if (StringUtils.isNullOrEmpty(accountUsername)) {
            accountUsername = USERNAME_PREFIX + '-' + middlefix + '-' +testRunResult.testRunId;
        }
        if (StringUtils.isNullOrEmpty(accountPassword)) {
            accountPassword = StringUtils.insecureRandomString(16);
        }
        // @formatter:off
        Builder builder = XMPPTCPConnectionConfiguration.builder()
                        .setXmppDomain(config.service)
                        .setUsernameAndPassword(accountUsername, accountPassword)
                        .setResource(middlefix + '-' + testRunResult.testRunId)
                        .setSecurityMode(config.securityMode);
        // @formatter:on
        if (config.tlsContext != null) {
            builder.setCustomSSLContext(config.tlsContext);
        }
        XMPPTCPConnection connection = new XMPPTCPConnection(builder.build());
        connection.connect();
        if (config.isAccountRegistrationPossible()) {
            UsernameAndPassword uap = IntTestUtil.registerAccount(connection, accountUsername, accountPassword, config);

            // TODO is this still required?
            // Some servers, e.g. Openfire, do not support a login right after the account was
            // created, so disconnect and re-connection the connection first.
            connection.disconnect();
            connection.connect();

            connection.login(uap.username, uap.password);
        } else {
            connection.login();
        }

        return connection;
    }

    static XMPPTCPConnection getConnectedConnection(SmackIntegrationTestEnvironment environment, int connectionId)
                    throws KeyManagementException, NoSuchAlgorithmException, InterruptedException,
                    SmackException, IOException, XMPPException {
        Configuration config = environment.configuration;
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        if (config.tlsContext != null) {
            builder.setCustomSSLContext(config.tlsContext);
        }
        builder.setSecurityMode(config.securityMode);
        builder.setXmppDomain(config.service);
        XMPPTCPConnection connection = new XMPPTCPConnection(builder.build());
        connection.connect();
        UsernameAndPassword uap = IntTestUtil.registerAccount(connection, environment, connectionId);
        connection.login(uap.username, uap.password);
        return connection;
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
        return (classes.contains(className) || classes.contains(unqualifiedClassName));
    }

    public static final class TestRunResult {

        /**
         * A short String of lowercase characters and numbers used to identify a integration test
         * run. We use lowercase characters because this string will eventually be part of the
         * localpart of the used JIDs (and the localpart is case insensitive).
         */
        public final String testRunId = StringUtils.insecureRandomString(5).toLowerCase(Locale.US);

        private final List<SuccessfulTest> successfulTests = Collections.synchronizedList(new LinkedList<SuccessfulTest>());
        private final List<FailedTest> failedIntegrationTests = Collections.synchronizedList(new LinkedList<FailedTest>());
        private final List<TestNotPossible> impossibleTestMethods = Collections.synchronizedList(new LinkedList<TestNotPossible>());
        private final Map<Class<? extends AbstractSmackIntTest>, String> impossibleTestClasses = new HashMap<>();
        private final AtomicInteger numberOfAvailableTests = new AtomicInteger();
        private final AtomicInteger numberOfPossibleTests = new AtomicInteger();

        private TestRunResult() {
        }

        public String getTestRunId() {
            return testRunId;
        }

        public int getNumberOfAvailableTests() {
            return numberOfAvailableTests.get();
        }

        public int getNumberOfPossibleTests() {
            return numberOfPossibleTests.get();
        }

        public List<SuccessfulTest> getSuccessfulTests() {
            return Collections.unmodifiableList(successfulTests);
        }

        public List<FailedTest> getFailedTests() {
            return Collections.unmodifiableList(failedIntegrationTests);
        }

        public List<TestNotPossible> getNotPossibleTests() {
            return Collections.unmodifiableList(impossibleTestMethods);
        }

        public Map<Class<? extends AbstractSmackIntTest>, String> getImpossibleTestClasses() {
            return Collections.unmodifiableMap(impossibleTestClasses);
        }
    }
}
