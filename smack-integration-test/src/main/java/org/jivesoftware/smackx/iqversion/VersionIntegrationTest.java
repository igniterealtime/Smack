/*
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

import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.iqversion.packet.Version;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

@SpecificationReference(document = "XEP-0092", version = "1.1")
public class VersionIntegrationTest extends AbstractSmackIntegrationTest {

    public VersionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @BeforeClass
    public void subscribe() throws Exception {
        VersionManager.setAutoAppendSmackVersion(false);

        // RFC6120 10.5.4 and RFC 6121 8.5.3.1 are at odds with each-other in regard to full-JID IQ delivery. Best possible chance of that happening is with mutual subscription.
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);
    }

    @AfterClass
    public void unsubscribe() throws SmackException.NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    @SmackIntegrationTest
    public void testVersion() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        VersionManager versionManagerOne = VersionManager.getInstanceFor(conOne);
        VersionManager versionManagerTwo = VersionManager.getInstanceFor(conTwo);
        final String versionName = "Smack Integration Test " + testRunId;
        versionManagerTwo.setVersion(versionName, "1.0");

        assertTrue(versionManagerOne.isSupported(conTwo.getUser()), "Expected " + conTwo.getUser() + " to support " + Version.NAMESPACE + " (but it does not).");
        Version version = versionManagerOne.getVersion(conTwo.getUser());
        assertEquals(versionName, version.getName(), "Unexpected version name reported by " + conTwo.getUser());
    }
}
