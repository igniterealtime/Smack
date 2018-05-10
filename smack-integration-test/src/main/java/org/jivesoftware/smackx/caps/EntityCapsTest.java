/**
 *
 * Copyright 2013-2018 Florian Schmaus
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
package org.jivesoftware.smackx.caps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.RosterUtil;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class EntityCapsTest extends AbstractSmackIntegrationTest {

    private final EntityCapsManager ecmTwo;
    private final ServiceDiscoveryManager sdmOne;
    private final ServiceDiscoveryManager sdmTwo;

    public EntityCapsTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        ecmTwo = EntityCapsManager.getInstanceFor(environment.conTwo);
        sdmOne = ServiceDiscoveryManager.getInstanceFor(environment.conOne);
        sdmTwo = ServiceDiscoveryManager.getInstanceFor(environment.conTwo);
    }

    private final AtomicInteger dummyFeatureId = new AtomicInteger();
    private final Set<String> dummyFeatures = new HashSet<>();

    private String getNewDummyFeature() {
        String dummyFeature = "entityCapsTest" + dummyFeatureId.incrementAndGet();
        dummyFeatures.add(dummyFeature);
        return dummyFeature;
    }

    @BeforeClass
    public void setUp() throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        RosterUtil.ensureSubscribed(conOne, conTwo, timeout);
    }

    @AfterClass
    public void tearDown() throws NotConnectedException, InterruptedException {
        RosterUtil.ensureNotSubscribedToEachOther(conOne, conTwo);
        ServiceDiscoveryManager[] sdms = new ServiceDiscoveryManager[] { sdmOne, sdmTwo };
        for (ServiceDiscoveryManager sdm : sdms) {
            for (String dummyFeature : dummyFeatures) {
                sdm.removeFeature(dummyFeature);
            }
        }
    }

    @SmackIntegrationTest
    public void testLocalEntityCaps() throws InterruptedException, NoResponseException, XMPPErrorException, NotConnectedException {
        final String dummyFeature = getNewDummyFeature();
        DiscoverInfo info = EntityCapsManager.getDiscoveryInfoByNodeVer(ecmTwo.getLocalNodeVer());
        assertFalse(info.containsFeature(dummyFeature));

        dropWholeEntityCapsCache();

        performActionAndWaitUntilStanzaReceived(new Runnable() {
            @Override
            public void run() {
                // This should cause a new presence stanza from con1 with and updated
                // 'ver' String
                sdmTwo.addFeature(dummyFeature);
            }
        }, conOne, new AndFilter(PresenceTypeFilter.AVAILABLE, FromMatchesFilter.create(conTwo.getUser())));

        // The presence stanza should get received by con0 and the data should
        // be recorded in the map
        // Note that while both connections use the same static Entity Caps
        // cache,
        // it's assured that *not* con1 added the data to the Entity Caps cache.
        // Every time the entities features
        // and identities change only a new caps 'ver' is calculated and send
        // with the presence stanza
        // The other connection has to receive this stanza and record the
        // information in order for this test to succeed.
        info = EntityCapsManager.getDiscoveryInfoByNodeVer(ecmTwo.getLocalNodeVer());
        assertNotNull(info);
        assertTrue(info.containsFeature(dummyFeature));
    }

    /**
     * Test if entity caps actually prevent a disco info request and reply.
     *
     * @throws XMPPException
     * @throws InterruptedException
     * @throws NotConnectedException
     * @throws NoResponseException
     *
     */
    @SmackIntegrationTest
    public void testPreventDiscoInfo() throws Exception {
        final String dummyFeature = getNewDummyFeature();
        final AtomicBoolean discoInfoSend = new AtomicBoolean();
        conOne.addStanzaSendingListener(new StanzaListener() {

            @Override
            public void processStanza(Stanza stanza) {
                discoInfoSend.set(true);
            }

        }, new AndFilter(new StanzaTypeFilter(DiscoverInfo.class), IQTypeFilter.GET));

        final SimpleResultSyncPoint presenceReceivedSyncPoint = new SimpleResultSyncPoint();
        final StanzaListener presenceListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                presenceReceivedSyncPoint.signal();
            }
        };

        // Add a stanzaListener to listen for incoming presence
        conOne.addAsyncStanzaListener(presenceListener, PresenceTypeFilter.AVAILABLE);

        // add a bogus feature so that con1 ver won't match con0's
        sdmTwo.addFeature(dummyFeature);

        try {
            // wait for the dummy feature to get sent via presence
            presenceReceivedSyncPoint.waitForResult(timeout);
        } finally {
            conOne.removeAsyncStanzaListener(presenceListener);
        }

        dropCapsCache();
        // discover that
        DiscoverInfo info = sdmOne.discoverInfo(conTwo.getUser());
        // that discovery should cause a disco#info
        assertTrue(discoInfoSend.get());
        assertTrue(info.containsFeature(dummyFeature));
        discoInfoSend.set(false);

        // discover that
        info = sdmOne.discoverInfo(conTwo.getUser());
        // that discovery shouldn't cause a disco#info
        assertFalse(discoInfoSend.get());
        assertTrue(info.containsFeature(dummyFeature));
    }

    @SmackIntegrationTest
    public void testCapsChanged() {
        final String dummyFeature = getNewDummyFeature();
        String nodeVerBefore = EntityCapsManager.getNodeVersionByJid(conTwo.getUser());
        sdmTwo.addFeature(dummyFeature);
        String nodeVerAfter = EntityCapsManager.getNodeVersionByJid(conTwo.getUser());

        assertFalse(nodeVerBefore.equals(nodeVerAfter));
    }

    @SmackIntegrationTest
    public void testEntityCaps() throws XMPPException, InterruptedException, NoResponseException, NotConnectedException, TimeoutException {
        final String dummyFeature = getNewDummyFeature();

        dropWholeEntityCapsCache();

        performActionAndWaitUntilStanzaReceived(new Runnable() {
            @Override
            public void run() {
                sdmTwo.addFeature(dummyFeature);
            }
        }, connection, new AndFilter(PresenceTypeFilter.AVAILABLE, FromMatchesFilter.create(conTwo.getUser())));

        waitUntilTrue(new Condition() {
            @Override
            public boolean evaluate() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
                DiscoverInfo info = sdmOne.discoverInfo(conTwo.getUser());
                return info.containsFeature(dummyFeature);
            }
        });
        DiscoverInfo info = sdmOne.discoverInfo(conTwo.getUser());

        String u1ver = EntityCapsManager.getNodeVersionByJid(conTwo.getUser());
        assertNotNull(u1ver);

        DiscoverInfo entityInfo = EntityCapsManager.CAPS_CACHE.lookup(u1ver);
        assertNotNull(entityInfo);

        assertEquals(info.toXML(null), entityInfo.toXML(null));
    }

    private static void dropWholeEntityCapsCache() {
        EntityCapsManager.CAPS_CACHE.clear();
        EntityCapsManager.JID_TO_NODEVER_CACHE.clear();
    }

    private static void dropCapsCache() {
        EntityCapsManager.CAPS_CACHE.clear();
    }
}
