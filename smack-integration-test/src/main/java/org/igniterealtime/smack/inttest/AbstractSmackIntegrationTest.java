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

import org.jivesoftware.smack.XMPPConnection;

public abstract class AbstractSmackIntegrationTest extends AbstractSmackIntTest {

    /**
     * The first connection.
     */
    protected final XMPPConnection conOne;

    /**
     * The second connection.
     */
    protected final XMPPConnection conTwo;

    /**
     * The third connection.
     */
    protected final XMPPConnection conThree;

    /**
     * An alias for the first connection {@link #conOne}.
     */
    protected final XMPPConnection connection;

    public AbstractSmackIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment.testRunId, environment.configuration);
        this.connection = this.conOne = environment.conOne;
        this.conTwo = environment.conTwo;
        this.conThree = environment.conThree;
    }
}
