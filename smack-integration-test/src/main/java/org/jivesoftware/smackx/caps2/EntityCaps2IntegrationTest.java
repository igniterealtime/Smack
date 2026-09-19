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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

public class EntityCaps2IntegrationTest extends AbstractSmackIntegrationTest {

    private final Caps2Manager caps2Man1;
    private final Caps2Manager caps2Man2;

    private final Set<String> dummyFeatures = new HashSet<>();
    private final AtomicInteger dummyFeatureId = new AtomicInteger();

    public EntityCaps2IntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        caps2Man1 = Caps2Manager.getInstanceFor(environment.conOne);
        caps2Man2 = Caps2Manager.getInstanceFor(environment.conTwo);
    }

    Caps2Element conOneCaps2ElementObtained = null;

    @SmackIntegrationTest
    public void tradeDiscoInfoBetweenDistinctECaps2ManagerTest() throws NoResponseException, XMPPErrorException, NotConnectedException, UnsupportedEncodingException, NoSuchAlgorithmException, InterruptedException, TimeoutException {
        caps2Man1.publishEntityCapabilities();

        DiscoverInfo discoInfoSentFromOne = ServiceDiscoveryManager.getInstanceFor(conOne).discoverInfo(conOne.getUser());

        List<Identity> identitiesSentFromOne = discoInfoSentFromOne.getIdentities();
        List<Feature> featuresSentFromOne = discoInfoSentFromOne.getFeatures();

        Caps2Element conOneCaps2Element = caps2Man1.getCurrentEnitityCapabilities();

        waitUntilTrue(new Condition() {
            @Override
            public boolean evaluate()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
                conOneCaps2ElementObtained = caps2Man2.getEntityCapabilitiesFrom(conOne.getUser());
                if (conOneCaps2ElementObtained != null) {
                    return true;
                }
                return false;
            }
        });
        assertEquals(conOneCaps2Element.toXML(), conOneCaps2ElementObtained.toXML());
        DiscoverInfo discoInfoReceivedByTwo = caps2Man2.getDiscoInfoFrom(conOne.getUser());

        List<Identity> identitiesReceivedByTwo = discoInfoReceivedByTwo.getIdentities();
        List<Feature> featuresReceivedByTwo = discoInfoReceivedByTwo.getFeatures();

        assertEquals(identitiesSentFromOne, identitiesReceivedByTwo);
        assertEquals(featuresSentFromOne, featuresReceivedByTwo);
    }

    @SmackIntegrationTest
    public void getEntityCapability() throws Exception {
        final SimpleResultSyncPoint entityCaps2Received = new SimpleResultSyncPoint();

        StanzaListener stanzaListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException, InterruptedException, NotLoggedInException {
                Presence presence = (Presence) packet;
                if (presence.hasExtension(Caps2Element.class)) {
                    entityCaps2Received.signal();
                }
            }
        };
        conTwo.addStanzaListener(stanzaListener, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                if (stanza instanceof Presence) {
                    return true;
                }
                return false;
            }
        });
        try {
            caps2Man1.publishEntityCapabilities();
            entityCaps2Received.waitForResult(timeout);
        } finally {
            conTwo.removeStanzaListener(stanzaListener);
        }
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
}
