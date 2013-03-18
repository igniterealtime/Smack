package org.jivesoftware.smackx.entitycaps;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.Base32Encoder;
import org.jivesoftware.smack.util.Base64Encoder;
import org.jivesoftware.smack.util.StringEncoder;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.entitycaps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.junit.Test;


public class EntityCapsManagerTest {

    /**
     * <a href="http://xmpp.org/extensions/xep-0115.html#ver-gen-complex">XEP-
     * 0115 Complex Generation Example</a>
     */
    @Test
    public void testComplexGenerationExample() {
        DiscoverInfo di = createComplexSamplePacket();

        String ver = EntityCapsManager.generateVerificationString(di, "sha-1");
        assertEquals("q07IKJEyjvHSyhy//CH0CxmKi8w=", ver);
    }

    @Test
    public void testSimpleDirectoryCacheBase64() throws IOException {
        EntityCapsManager.persistentCache = null;
        testSimpleDirectoryCache(Base64Encoder.getInstance());
    }

    @Test
    public void testSimpleDirectoryCacheBase32() throws IOException {
        EntityCapsManager.persistentCache = null;
        testSimpleDirectoryCache(Base32Encoder.getInstance());
    }

    @Test
    public void testVerificationDuplicateFeatures() {
        DiscoverInfo di = createMalformedDiscoverInfo();
        assertTrue(di.containsDuplicateFeatures());
    }

    @Test
    public void testVerificationDuplicateIdentities() {
        DiscoverInfo di = createMalformedDiscoverInfo();
        assertTrue(di.containsDuplicateIdentities());
    }

    @Test
    public void testVerificationDuplicateDataForm() {
        DiscoverInfo di = createMalformedDiscoverInfo();
        assertTrue(EntityCapsManager.verifyPacketExtensions(di));
    }

    private void testSimpleDirectoryCache(StringEncoder stringEncoder) throws IOException {

        EntityCapsPersistentCache cache = new SimpleDirectoryPersistentCache(createTempDirectory());
        EntityCapsManager.setPersistentCache(cache);

        DiscoverInfo di = createComplexSamplePacket();
        String nodeVer = di.getNode() + "#" + EntityCapsManager.generateVerificationString(di, "sha-1");

        // Save the data in EntityCapsManager
        EntityCapsManager.addDiscoverInfoByNode(nodeVer, di);

        // Lose all the data
        EntityCapsManager.caps.clear();

        // Restore the data from the persistent Cache
        cache.replay();

        DiscoverInfo restored_di = EntityCapsManager.getDiscoveryInfoByNodeVer(nodeVer);
        assertNotNull(restored_di);
        assertEquals(di.toXML(), restored_di.toXML());
    }

    private static DiscoverInfo createComplexSamplePacket() {
        DiscoverInfo di = new DiscoverInfo();
        di.setFrom("benvolio@capulet.lit/230193");
        di.setPacketID("disco1");
        di.setTo("juliet@capulet.lit/chamber");
        di.setType(IQ.Type.RESULT);

        Collection<DiscoverInfo.Identity> identities = new LinkedList<DiscoverInfo.Identity>();
        DiscoverInfo.Identity i = new DiscoverInfo.Identity("client", "Psi 0.11", "pc");
        i.setLanguage("en");
        identities.add(i);
        i = new DiscoverInfo.Identity("client", "Ψ 0.11", "pc");
        i.setLanguage("el");
        identities.add(i);
        di.addIdentities(identities);

        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature(EntityCapsManager.NAMESPACE);
        di.addFeature("http://jabber.org/protocol/muc");
        di.addFeature("http://jabber.org/protocol/disco#info");

        DataForm df = new DataForm("result");

        FormField ff = new FormField("os");
        ff.addValue("Mac");
        df.addField(ff);

        ff = new FormField("FORM_TYPE");
        ff.setType("hidden");
        ff.addValue("urn:xmpp:dataforms:softwareinfo");
        df.addField(ff);

        ff = new FormField("ip_version");
        ff.addValue("ipv4");
        ff.addValue("ipv6");
        df.addField(ff);

        ff = new FormField("os_version");
        ff.addValue("10.5.1");
        df.addField(ff);

        ff = new FormField("software");
        ff.addValue("Psi");
        df.addField(ff);

        ff = new FormField("software_version");
        ff.addValue("0.11");
        df.addField(ff);

        di.addExtension(df);
        return di;
    }

    private static DiscoverInfo createMalformedDiscoverInfo() {
        DiscoverInfo di = new DiscoverInfo();
        di.setFrom("benvolio@capulet.lit/230193");
        di.setPacketID("disco1");
        di.setTo(")juliet@capulet.lit/chamber");
        di.setType(IQ.Type.RESULT);

        Collection<DiscoverInfo.Identity> identities = new LinkedList<DiscoverInfo.Identity>();
        DiscoverInfo.Identity i = new DiscoverInfo.Identity("client", "Psi 0.11", "pc");
        i.setLanguage("en");
        identities.add(i);
        i = new DiscoverInfo.Identity("client", "Ψ 0.11", "pc");
        i.setLanguage("el");
        identities.add(i);
        di.addIdentities(identities);
        // Failure 1: Duplicate identities
        i = new DiscoverInfo.Identity("client", "Ψ 0.11", "pc");
        i.setLanguage("el");
        identities.add(i);
        di.addIdentities(identities);

        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature(EntityCapsManager.NAMESPACE);
        di.addFeature("http://jabber.org/protocol/muc");
        di.addFeature("http://jabber.org/protocol/disco#info");
        // Failure 2: Duplicate features
        di.addFeature("http://jabber.org/protocol/disco#info");

        DataForm df = new DataForm("result");

        FormField ff = new FormField("os");
        ff.addValue("Mac");
        df.addField(ff);

        ff = new FormField("FORM_TYPE");
        ff.setType("hidden");
        ff.addValue("urn:xmpp:dataforms:softwareinfo");
        df.addField(ff);

        ff = new FormField("ip_version");
        ff.addValue("ipv4");
        ff.addValue("ipv6");
        df.addField(ff);

        ff = new FormField("os_version");
        ff.addValue("10.5.1");
        df.addField(ff);

        ff = new FormField("software");
        ff.addValue("Psi");
        df.addField(ff);

        ff = new FormField("software_version");
        ff.addValue("0.11");
        df.addField(ff);

        di.addExtension(df);

        // Failure 3: Another service discovery information form with the same
        // FORM_TYPE
        df = new DataForm("result");

        ff = new FormField("FORM_TYPE");
        ff.setType("hidden");
        ff.addValue("urn:xmpp:dataforms:softwareinfo");
        df.addField(ff);

        ff = new FormField("software");
        ff.addValue("smack");
        df.addField(ff);

        di.addExtension(df);

        return di;
    }

    public static File createTempDirectory() throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        File tmp;
        tmp = File.createTempFile(tmpdir, "entityCaps");
        tmp.delete();
        tmp.mkdir();
        return tmp;
    }

}
