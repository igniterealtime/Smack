/**
 *
 * Copyright 2024 Florian Schmaus
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
package org.igniterealtime.smack.inttest.debugger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.ZonedDateTime;

import org.jivesoftware.smack.debugger.SmackDebuggerFactory;

import org.igniterealtime.smack.inttest.AbstractSmackIntTest;
import org.igniterealtime.smack.inttest.ConnectionConfigurationBuilderApplier;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.ConcreteTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.TestRunResult;

public interface SinttestDebugger {

    SmackDebuggerFactory getSmackDebuggerFactory();

    default void onTestClassConstruction(Constructor<? extends AbstractSmackIntTest> cons) throws IOException { };

    default void onTestStart(ConcreteTest test, ZonedDateTime testStart) throws IOException { };

    default void onTestSuccess(ConcreteTest test, ZonedDateTime endTime) throws IOException { };

    default void onTestFailure(ConcreteTest test, ZonedDateTime endTime, Throwable nonFatalFailureReason) throws IOException { };

    default void onSinttestFinished(TestRunResult testRunResult) throws IOException { };

    default ConnectionConfigurationBuilderApplier getConnectionConfigurationBuilderApplier() {
        return b -> {
            var factory = getSmackDebuggerFactory();
            b.setDebuggerFactory(factory);
        };
    }

}
