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
package org.jivesoftware.smack;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;

public class WaitForClosingStreamElementTest extends AbstractSmackLowLevelIntegrationTest {

    public WaitForClosingStreamElementTest(SmackIntegrationTestEnvironment<?> environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void waitForClosingStreamElementTest(AbstractXMPPConnection connection)
                    throws NoSuchFieldException, SecurityException, IllegalArgumentException,
                    IllegalAccessException {
        connection.disconnect();

        Field closingStreamReceivedField = AbstractXMPPConnection.class.getDeclaredField("closingStreamReceived");
        closingStreamReceivedField.setAccessible(true);
        SynchronizationPoint<?> closingStreamReceived = (SynchronizationPoint<?>) closingStreamReceivedField.get(connection);
        Exception failureException = closingStreamReceived.getFailureException();
        if (failureException != null) {
            throw new AssertionError("Sync poing yielded failure exception", failureException);
        }
        assertTrue(closingStreamReceived.wasSuccessful());
    }
}
