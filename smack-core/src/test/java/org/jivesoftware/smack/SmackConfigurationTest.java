/**
 *
 * Copyright the original author or authors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

public class SmackConfigurationTest {

    @Test
    public void testSmackConfiguration() {
        try {
            SmackConfiguration.getDefaultReplyTimeout();
        } catch (Throwable t) {
            fail("SmackConfiguration threw Throwable");
        }
    }

    // As there is currently no annotation/way to run a testclass/single test in a separate VM,
    // which is required for reliable results of this test, and we don't want to fork a VM for
    // *every* test, those tests are currently disabled. Hopefully this will change in the future.
    @Ignore
    @Test
    public void smackConfigurationShouldNotCauseInitializationTest() {
        SmackConfiguration.getDefaultReplyTimeout();

        // Only a call to SmackConfiguration.getVersion() should cause Smack to become initialized.
        assertFalse(SmackConfiguration.isSmackInitialized());
    }

    // As there is currently no annotation/way to run a testclass/single test in a separate VM,
    // which is required for reliable results of this test, and we don't want to fork a VM for
    // *every* test, those tests are currently disabled. Hopefully this will change in the future.
    @Ignore
    @Test
    public void smackconfigurationVersionShouldInitialzieSmacktTest() {
        SmackConfiguration.getVersion();

        // Only a call to SmackConfiguration.getVersion() should cause Smack to become initialized.
        assertTrue(SmackConfiguration.isSmackInitialized());
    }
}
