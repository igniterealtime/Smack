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
package org.igniterealtime.smack.inttest.unittest;

import static org.igniterealtime.smack.inttest.SmackIntegrationTestUnitTestUtil.getFrameworkForUnitTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StanzaError;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.DummySmackIntegrationTestFramework;
import org.igniterealtime.smack.inttest.FailedTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.TestRunResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SmackIntegrationTestFrameworkUnitTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static boolean beforeClassInvoked;
    private static boolean afterClassInvoked;

    @BeforeClass
    public static void prepareSinttestUnitTest() {
        SmackIntegrationTestFramework.SINTTEST_UNIT_TEST = true;
    }

    @AfterClass
    public static void disallowSinntestUnitTest() {
        SmackIntegrationTestFramework.SINTTEST_UNIT_TEST = false;
    }

    @Test
    public void throwsRuntimeExceptionsTest() throws KeyManagementException, NoSuchAlgorithmException, SmackException,
                    IOException, XMPPException, InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(ThrowsRuntimeExceptionDummyTest.RUNTIME_EXCEPTION_MESSAGE);
        DummySmackIntegrationTestFramework sinttest = getFrameworkForUnitTest(ThrowsRuntimeExceptionDummyTest.class);
        sinttest.run();
    }

    public static class ThrowsRuntimeExceptionDummyTest extends AbstractSmackIntegrationTest {

        public ThrowsRuntimeExceptionDummyTest(SmackIntegrationTestEnvironment<?> environment) {
            super(environment);
        }

        public static final String RUNTIME_EXCEPTION_MESSAGE = "Dummy RuntimeException";

        @SmackIntegrationTest
        public void throwRuntimeExceptionTest() {
            throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE);
        }
    }

    @Test
    public void logsNonFatalExceptionTest() throws KeyManagementException, NoSuchAlgorithmException, SmackException,
            IOException, XMPPException, InterruptedException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        DummySmackIntegrationTestFramework sinttest = getFrameworkForUnitTest(ThrowsNonFatalExceptionDummyTest.class);
        TestRunResult testRunResult = sinttest.run();
        List<FailedTest> failedTests = testRunResult.getFailedTests();
        assertEquals(1, failedTests.size());
        FailedTest failedTest = failedTests.get(0);
        assertTrue(failedTest.failureReason instanceof XMPPErrorException);
        XMPPErrorException ex = (XMPPErrorException) failedTest.failureReason;
        assertEquals(StanzaError.Condition.bad_request, ex.getStanzaError().getCondition());
        assertEquals(ThrowsNonFatalExceptionDummyTest.DESCRIPTIVE_TEXT, ex.getStanzaError().getDescriptiveText());
    }

    public static class ThrowsNonFatalExceptionDummyTest extends AbstractSmackIntegrationTest {

        public static final String DESCRIPTIVE_TEXT = "I'm not fatal";

        public ThrowsNonFatalExceptionDummyTest(SmackIntegrationTestEnvironment<?> environment) {
            super(environment);
        }

        @SmackIntegrationTest
        public void throwRuntimeExceptionTest() throws XMPPErrorException {
            Message message = new Message();
            throw new XMPPException.XMPPErrorException(message,
                            StanzaError.from(StanzaError.Condition.bad_request, DESCRIPTIVE_TEXT).build());
        }
    }

    @Test
    public void testInvoking() throws KeyManagementException, NoSuchAlgorithmException, SmackException, IOException,
            XMPPException, InterruptedException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        beforeClassInvoked = false;
        afterClassInvoked = false;

        DummySmackIntegrationTestFramework sinttest = getFrameworkForUnitTest(BeforeAfterClassTest.class);
        sinttest.run();

        assertTrue("A before class method should have been executed to this time", beforeClassInvoked);
        assertTrue("A after class method should have been executed to this time", afterClassInvoked);
    }

    public static class BeforeAfterClassTest extends AbstractSmackIntegrationTest {

        public BeforeAfterClassTest(SmackIntegrationTestEnvironment<?> environment) {
            super(environment);
        }

        @BeforeClass
        public void setUp() {
            beforeClassInvoked = true;
        }

        @AfterClass
        public void tearDown() {
            afterClassInvoked = true;
        }

        @SmackIntegrationTest
        public void test() {
            assertTrue("A before class method should have been executed to this time", beforeClassInvoked);
            assertFalse("A after class method shouldn't have been executed to this time", afterClassInvoked);
        }
    }
}
