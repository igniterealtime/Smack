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

import org.jivesoftware.smack.AbstractXMPPConnection;

public class SmackIntegrationTestEnvironment<C extends AbstractXMPPConnection> {

    public final C conOne, conTwo, conThree;

    public final String testRunId;

    public final Configuration configuration;

    public final XmppConnectionManager<C> connectionManager;

    SmackIntegrationTestEnvironment(C conOne, C conTwo, C conThree, String testRunId,
                    Configuration configuration, XmppConnectionManager<C> connectionManager) {
        this.conOne = conOne;
        this.conTwo = conTwo;
        this.conThree = conThree;
        this.testRunId = testRunId;
        this.configuration = configuration;
        this.connectionManager = connectionManager;
    }
}
