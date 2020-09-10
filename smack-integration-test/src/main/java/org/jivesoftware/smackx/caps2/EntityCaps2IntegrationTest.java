/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.caps2;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;

public class EntityCaps2IntegrationTest extends AbstractSmackIntegrationTest {

    @SuppressWarnings("unused")
    private final Caps2Manager caps2Man1;
    @SuppressWarnings("unused")
    private final ServiceDiscoveryManager sdm1;

    private final Set<String> dummyFeatures = new HashSet<>();
    private final AtomicInteger dummyFeatureId = new AtomicInteger();

    public EntityCaps2IntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        caps2Man1 = Caps2Manager.getInstanceFor(environment.conOne);
        sdm1 = ServiceDiscoveryManager.getInstanceFor(conOne);
    }

    @BeforeClass
    public void setUp() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);
    }

    @AfterClass
    public void tearDown() throws NotConnectedException, InterruptedException, NotLoggedInException, NoResponseException, XMPPErrorException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    @SuppressWarnings("unused")
    private String getNewDummyFeature() {
        String dummyFeature = "entityCapsTest" + dummyFeatureId.incrementAndGet();
        dummyFeatures.add(dummyFeature);
        Logger.getAnonymousLogger().warning("adiaholic: dummy feature created: " + dummyFeature);
        return dummyFeature;
    }

    @SmackIntegrationTest
    public void waitToPublishFeature() throws Exception {
        Logger.getAnonymousLogger().warning("adiaholic: publishing feature");

        Logger.getAnonymousLogger().warning("adiaholic: feature published");
    }
}
