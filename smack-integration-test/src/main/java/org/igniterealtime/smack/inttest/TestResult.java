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

import java.util.List;

public abstract class TestResult {

    public final SmackIntegrationTestFramework.ConcreteTest concreteTest;
    public final long startTime;
    public final long endTime;
    public final long duration;
    public final List<String> logMessages;

    public TestResult(SmackIntegrationTestFramework.ConcreteTest concreteTest, long startTime, long endTime, List<String> logMessages) {
        this.concreteTest = concreteTest;
        assert (endTime >= startTime);
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = endTime - startTime;
        this.logMessages = logMessages;
    }
}
