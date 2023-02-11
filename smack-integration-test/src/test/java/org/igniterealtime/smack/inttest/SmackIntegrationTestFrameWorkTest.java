/**
 *
 * Copyright 2019-2023 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.List;

import org.jivesoftware.smack.AbstractXMPPConnection;

import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.TestMethodParameterType;

import org.junit.jupiter.api.Test;

public class SmackIntegrationTestFrameWorkTest {

    private static class ValidLowLevelList {
        @SuppressWarnings("unused")
        public void test(List<AbstractXMPPConnection> connections) {
        }
    }

    private static class InvalidLowLevelList {
        @SuppressWarnings("unused")
        public void test(List<AbstractXMPPConnection> connections, boolean invalid) {
        }
    }

    private static class ValidLowLevelVarargs {
        @SuppressWarnings("unused")
        public void test(AbstractXMPPConnection connectionOne, AbstractXMPPConnection connectionTwo,
                        AbstractXMPPConnection connectionThree) {
        }
    }

    private static class InvalidLowLevelVarargs {
        @SuppressWarnings("unused")
        public void test(AbstractXMPPConnection connectionOne, Integer invalid, AbstractXMPPConnection connectionTwo,
                        AbstractXMPPConnection connectionThree) {
        }
    }

    private static Method getTestMethod(Class<?> testClass) {
        Method[] methods = testClass.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals("test")) {
                return method;
            }
        }

        throw new IllegalArgumentException("No test method found in " + testClass);
    }

    @Test
    public void testValidLowLevelList() {
        Method testMethod = getTestMethod(ValidLowLevelList.class);
        TestMethodParameterType determinedParameterType = SmackIntegrationTestFramework.determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
        assertEquals(TestMethodParameterType.collectionOfConnections, determinedParameterType);
    }

    @Test
    public void testInvalidLowLevelList() {
        Method testMethod = getTestMethod(InvalidLowLevelList.class);
        TestMethodParameterType determinedParameterType = SmackIntegrationTestFramework.determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
        assertNull(determinedParameterType);
    }

    @Test
    public void testValidLowLevelVarargs() {
        Method testMethod = getTestMethod(ValidLowLevelVarargs.class);
        TestMethodParameterType determinedParameterType = SmackIntegrationTestFramework.determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
        assertEquals(TestMethodParameterType.parameterListOfConnections, determinedParameterType);
    }

    @Test
    public void testInvalidLowLevelVargs() {
        Method testMethod = getTestMethod(InvalidLowLevelVarargs.class);
        TestMethodParameterType determinedParameterType = SmackIntegrationTestFramework.determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
        assertNull(determinedParameterType);
    }

    private static class ValidUnconnectedConnectionSource {
        @SuppressWarnings("unused")
        public void test(AbstractSmackLowLevelIntegrationTest.UnconnectedConnectionSource source) {
        }
    }

    @Test
    public void testValidUnconnectedConnectionSource() {
        Method testMethod = getTestMethod(ValidUnconnectedConnectionSource.class);
        TestMethodParameterType determinedParameterType = SmackIntegrationTestFramework.determineTestMethodParameterType(testMethod, AbstractXMPPConnection.class);
        assertEquals(TestMethodParameterType.unconnectedConnectionSource, determinedParameterType);
    }

}
