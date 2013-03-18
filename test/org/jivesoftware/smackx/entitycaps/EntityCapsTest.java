package org.jivesoftware.smackx.entitycaps;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

public class EntityCapsTest extends SmackTestCase {

    private static final String DISCOVER_TEST_FEATURE = "entityCapsTest";

    XMPPConnection con0;
    XMPPConnection con1;
    EntityCapsManager ecm0;
    EntityCapsManager ecm1;
    ServiceDiscoveryManager sdm0;
    ServiceDiscoveryManager sdm1;

    private boolean discoInfoSend = false;

    public EntityCapsTest(String arg0) {
        super(arg0);
    }

    @Override
    protected int getMaxConnections() {
        return 2;
    }

    protected void setUp() throws Exception {
        super.setUp();
        SmackConfiguration.setAutoEnableEntityCaps(true);
        SmackConfiguration.setPacketReplyTimeout(1000 * 60 * 5);
        con0 = getConnection(0);
        con1 = getConnection(1);
        ecm0 = EntityCapsManager.getInstanceFor(getConnection(0));
        ecm1 = EntityCapsManager.getInstanceFor(getConnection(1));
        sdm0 = ServiceDiscoveryManager.getInstanceFor(con0);
        sdm1 = ServiceDiscoveryManager.getInstanceFor(con1);
        letsAllBeFriends();
    }

    public void testLocalEntityCaps() throws InterruptedException {
        DiscoverInfo info = EntityCapsManager.getDiscoveryInfoByNodeVer(ecm1.getLocalNodeVer());
        assertFalse(info.containsFeature(DISCOVER_TEST_FEATURE));

        dropWholeEntityCapsCache();

        // This should cause a new presence stanza from con1 with and updated
        // 'ver' String
        sdm1.addFeature(DISCOVER_TEST_FEATURE);

        // Give the server some time to handle the stanza and send it to con0
        Thread.sleep(2000);

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
        info = EntityCapsManager.getDiscoveryInfoByNodeVer(ecm1.getLocalNodeVer());
        assertNotNull(info);
        assertTrue(info.containsFeature(DISCOVER_TEST_FEATURE));
    }

    /**
     * Test if entity caps actually prevent a disco info request and reply
     * 
     * @throws XMPPException
     * 
     */
    public void testPreventDiscoInfo() throws XMPPException {
        con0.addPacketSendingListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                discoInfoSend = true;
            }

        }, new AndFilter(new PacketTypeFilter(DiscoverInfo.class), new IQTypeFilter(IQ.Type.GET)));

        // add a bogus feature so that con1 ver won't match con0's
        sdm1.addFeature(DISCOVER_TEST_FEATURE);

        dropCapsCache();
        // discover that
        DiscoverInfo info = sdm0.discoverInfo(con1.getUser());
        // that discovery should cause a disco#info
        assertTrue(discoInfoSend);
        assertTrue(info.containsFeature(DISCOVER_TEST_FEATURE));
        discoInfoSend = false;

        // discover that
        info = sdm0.discoverInfo(con1.getUser());
        // that discovery shouldn't cause a disco#info
        assertFalse(discoInfoSend);
        assertTrue(info.containsFeature(DISCOVER_TEST_FEATURE));
    }

    public void testCapsChanged() {
        String nodeVerBefore = EntityCapsManager.getNodeVersionByJid(con1.getUser());
        sdm1.addFeature(DISCOVER_TEST_FEATURE);
        String nodeVerAfter = EntityCapsManager.getNodeVersionByJid(con1.getUser());

        assertFalse(nodeVerBefore.equals(nodeVerAfter));
    }

    public void testEntityCaps() throws XMPPException, InterruptedException {
        dropWholeEntityCapsCache();
        sdm1.addFeature(DISCOVER_TEST_FEATURE);

        Thread.sleep(3000);

        DiscoverInfo info = sdm0.discoverInfo(con1.getUser());
        assertTrue(info.containsFeature(DISCOVER_TEST_FEATURE));

        String u1ver = EntityCapsManager.getNodeVersionByJid(con1.getUser());
        assertNotNull(u1ver);

        DiscoverInfo entityInfo = EntityCapsManager.caps.get(u1ver);
        assertNotNull(entityInfo);

        assertEquals(info.toXML(), entityInfo.toXML());
    }

    private static void dropWholeEntityCapsCache() {
        EntityCapsManager.caps.clear();
        EntityCapsManager.jidCaps.clear();
    }

    private static void dropCapsCache() {
        EntityCapsManager.caps.clear();
    }
}
