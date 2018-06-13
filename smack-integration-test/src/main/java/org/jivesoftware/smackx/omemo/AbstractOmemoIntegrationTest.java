/**
 *
 * Copyright 2017 Florian Schmaus, Paul Schaub
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
package org.jivesoftware.smackx.omemo;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * Super class for OMEMO integration tests.
 */
public abstract class AbstractOmemoIntegrationTest extends AbstractSmackIntegrationTest {

    public AbstractOmemoIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException {
        super(environment);

        // Test for server support
        if (!OmemoManager.serverSupportsOmemo(connection, connection.getXMPPServiceDomain())) {
            throw new TestNotPossibleException("Server does not support OMEMO (PubSub)");
        }

        // Check for OmemoService
        if (!OmemoService.isServiceRegistered()) {
            throw new TestNotPossibleException("No OmemoService registered.");
        }

        OmemoConfiguration.setCompleteSessionWithEmptyMessage(true);
        OmemoConfiguration.setRepairBrokenSessionsWithPrekeyMessages(true);
    }
}
