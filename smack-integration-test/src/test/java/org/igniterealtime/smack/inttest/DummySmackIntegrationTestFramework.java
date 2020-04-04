/**
 *
 * Copyright 2015-2020 Florian Schmaus
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.DummyConnection.DummyConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

public class DummySmackIntegrationTestFramework extends SmackIntegrationTestFramework {

    public static final String DUMMY_CONNECTION_NICKNAME = "dummy";

    static {
        try {
            XmppConnectionManager.addConnectionDescriptor(
                            XmppConnectionDescriptor
                            .buildWith(DummyConnection.class, DummyConnectionConfiguration.class)
                            .withNickname(DUMMY_CONNECTION_NICKNAME)
                            .build()
            );
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
    }

    public DummySmackIntegrationTestFramework(Configuration configuration) throws KeyManagementException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchAlgorithmException, SmackException, IOException, XMPPException, InterruptedException {
        super(configuration);
        testRunResult = new TestRunResult();
    }

    @Override
    protected SmackIntegrationTestEnvironment prepareEnvironment() {
        DummyConnection dummyConnection = new DummyConnection();
        connectionManager.conOne = connectionManager.conTwo = connectionManager.conThree = dummyConnection;
        return new SmackIntegrationTestEnvironment(dummyConnection, dummyConnection, dummyConnection,
                        testRunResult.getTestRunId(), config, null);
    }

}
