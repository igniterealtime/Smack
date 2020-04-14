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
package org.jivesoftware.smackx.iqversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.iqversion.packet.Version;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class VersionIntegrationTest extends AbstractSmackIntegrationTest {

    public VersionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void testVersion() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // TODO put into @BeforeClass method
        VersionManager.setAutoAppendSmackVersion(false);

        VersionManager versionManagerOne = VersionManager.getInstanceFor(conOne);
        VersionManager versionManagerTwo = VersionManager.getInstanceFor(conTwo);
        final String versionName = "Smack Integration Test " + testRunId;
        versionManagerTwo.setVersion(versionName, "1.0");

        assertTrue (versionManagerOne.isSupported(conTwo.getUser()));
        Version version = versionManagerOne.getVersion(conTwo.getUser());
        assertEquals(versionName, version.getName());
    }
}
