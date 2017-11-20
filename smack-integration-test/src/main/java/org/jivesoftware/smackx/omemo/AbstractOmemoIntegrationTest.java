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

import java.io.File;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Super class for OMEMO integration tests.
 */
public abstract class AbstractOmemoIntegrationTest extends AbstractSmackIntegrationTest {

    static final File storePath;

    static {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            storePath = new File(f, ".config/smack-integration-test/store");
        } else {
            storePath = new File("int_test_omemo_store");
        }
    }

    public AbstractOmemoIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
        if (OmemoConfiguration.getFileBasedOmemoStoreDefaultPath() == null) {
            OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(storePath);
        }
        // Test for server support
        if (!OmemoManager.serverSupportsOmemo(connection, connection.getXMPPServiceDomain())) {
            throw new TestNotPossibleException("Server does not support OMEMO (PubSub)");
        }

        // Check for OmemoService
        if (!OmemoService.isServiceRegistered()) {
            throw new TestNotPossibleException("No OmemoService registered.");
        }
    }

    @BeforeClass
    public void beforeTest() {
        LOGGER.log(Level.INFO, "START EXECUTION");
        OmemoIntegrationTestHelper.deletePath(storePath);
        before();
    }

    @AfterClass
    public void afterTest() {
        after();
        OmemoIntegrationTestHelper.deletePath(storePath);
        LOGGER.log(Level.INFO, "END EXECUTION");
    }

    public abstract void before();

    public abstract void after();
}
