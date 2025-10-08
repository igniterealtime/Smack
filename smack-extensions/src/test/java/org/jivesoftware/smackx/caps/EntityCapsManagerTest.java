/*
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
package org.jivesoftware.smackx.caps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base32;
import org.jivesoftware.smack.util.stringencoder.StringEncoder;

import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.caps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfoBuilder;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextMultiFormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;


public class EntityCapsManagerTest extends SmackTestSuite {

    /**
     * <a href="https://xmpp.org/extensions/xep-0115.html#ver-gen-simple">XEP-
     * 0115 Simple Generation Example</a>.
     * @throws XmppStringprepException if the provided string is invalid.
     */
    @Test
    public void testSimpleGenerationExample() throws XmppStringprepException {
        DiscoverInfo di = createSimpleSamplePacket();

        CapsVersionAndHash versionAndHash = EntityCapsManager.generateVerificationString(di, StringUtils.SHA1);
        assertEquals("QgayPKawpkPSDYmwT/WM94uAlu0=", versionAndHash.version);
    }

    /**
     * Asserts that the order in which data forms are present in the disco/info does not affect the calculated
     * verification string, as the XEP mandates that these are ordered by FORM_TYPE (i.e., by the XML character data of
     * the <value/> element).
     * @throws XmppStringprepException if the provided string is invalid.
     */
    @Test
    public void testReversedDataFormOrder() throws XmppStringprepException {
        final DiscoverInfoBuilder builderA = createSimpleSampleBuilder();
        builderA.addExtension(createSampleServerInfoDataForm()); // This works, as the underlying MultiMap maintains insertion-order.
        builderA.addExtension(createSampleSoftwareInfoDataForm());

        final DiscoverInfoBuilder builderB = createSimpleSampleBuilder();
        builderB.addExtension(createSampleSoftwareInfoDataForm());
        builderB.addExtension(createSampleServerInfoDataForm());

        CapsVersionAndHash versionAndHashA = EntityCapsManager.generateVerificationString(builderA.build(), StringUtils.SHA1);
        CapsVersionAndHash versionAndHashB = EntityCapsManager.generateVerificationString(builderB.build(), StringUtils.SHA1);

        assertEquals(versionAndHashA.version, versionAndHashB.version);
    }

    /**
     * <a href="http://xmpp.org/extensions/xep-0115.html#ver-gen-complex">XEP-
     * 0115 Complex Generation Example</a>.
     * @throws XmppStringprepException if the provided string is invalid.
     */
    @Test
    public void testComplexGenerationExample() throws XmppStringprepException {
        DiscoverInfo di = createComplexSamplePacket();

        CapsVersionAndHash versionAndHash = EntityCapsManager.generateVerificationString(di, StringUtils.SHA1);
        assertEquals("q07IKJEyjvHSyhy//CH0CxmKi8w=", versionAndHash.version);
    }

    @Test
    public void testSimpleDirectoryCacheBase32() throws IOException {
        EntityCapsManager.persistentCache = null;
        testSimpleDirectoryCache(Base32.getStringEncoder());
    }

    @Test
    public void testVerificationDuplicateFeatures() throws XmppStringprepException {
        DiscoverInfo di = createMalformedDiscoverInfo();
        assertTrue(di.containsDuplicateFeatures());
    }

    @Test
    public void testVerificationDuplicateIdentities() throws XmppStringprepException {
        DiscoverInfo di = createMalformedDiscoverInfo();
        assertTrue(di.containsDuplicateIdentities());
    }

    @SuppressWarnings("UnusedVariable")
    private static void testSimpleDirectoryCache(StringEncoder<String> stringEncoder) throws IOException {

        EntityCapsPersistentCache cache = new SimpleDirectoryPersistentCache(createTempDirectory());
        EntityCapsManager.setPersistentCache(cache);

        DiscoverInfo di = createComplexSamplePacket();
        CapsVersionAndHash versionAndHash = EntityCapsManager.generateVerificationString(di, StringUtils.SHA1);
        String nodeVer = di.getNode() + "#" + versionAndHash.version;

        // Save the data in EntityCapsManager
        EntityCapsManager.addDiscoverInfoByNode(nodeVer, di);

        // Lose all the data
        EntityCapsManager.clearMemoryCache();

        DiscoverInfo restored_di = EntityCapsManager.getDiscoveryInfoByNodeVer(nodeVer);
        assertNotNull(restored_di);
        assertEquals(di.toXML().toString(), restored_di.toXML().toString());
    }

    private static DataForm createSampleSoftwareInfoDataForm() {
        DataForm.Builder df = DataForm.builder(DataForm.Type.result);

        {
            TextSingleFormField.Builder ff = FormField.builder("os");
            ff.setValue("Mac");
            df.addField(ff.build());
        }

        {
            TextSingleFormField.Builder ff = FormField.hiddenBuilder("FORM_TYPE");
            ff.setValue("urn:xmpp:dataforms:softwareinfo");
            df.addField(ff.build());
        }

        {
            TextMultiFormField.Builder ff = FormField.textMultiBuilder("ip_version");
            ff.addValue("ipv4");
            ff.addValue("ipv6");
            df.addField(ff.build());
        }

        {
            TextSingleFormField.Builder ff = FormField.builder("os_version");
            ff.setValue("10.5.1");
            df.addField(ff.build());
        }

        {
            TextSingleFormField.Builder ff = FormField.builder("software");
            ff.setValue("Psi");
            df.addField(ff.build());
        }

        {
            TextSingleFormField.Builder ff = FormField.builder("software_version");
            ff.setValue("0.11");
            df.addField(ff.build());
        }

        return df.build();
    }

    private static DataForm createSampleServerInfoDataForm() {
        DataForm.Builder df = DataForm.builder(DataForm.Type.result);

        {
            TextMultiFormField.Builder ff = FormField.textMultiBuilder("admin-addresses");
            ff.addValue("xmpp:admin@example.org");
            ff.addValue("mailto:admin@example.com");
            df.addField(ff.build());
        }

        {
            TextSingleFormField.Builder ff = FormField.hiddenBuilder("FORM_TYPE");
            ff.setValue("http://jabber.org/network/serverinfo");
            df.addField(ff.build());
        }

        return df.build();
    }

    private static DiscoverInfoBuilder createSimpleSampleBuilder() throws XmppStringprepException {
        DiscoverInfoBuilder di = DiscoverInfo.builder("disco1");
        di.ofType(IQ.Type.result);

        di.addIdentity(new DiscoverInfo.Identity("client", "Exodus 0.9.1", "pc"));
        di.addFeature("http://jabber.org/protocol/disco#info");
        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature("http://jabber.org/protocol/muc");
        di.addFeature("http://jabber.org/protocol/caps");

        return di;
    }
    private static DiscoverInfo createSimpleSamplePacket() throws XmppStringprepException {
        return createSimpleSampleBuilder().build();
    }

    private static DiscoverInfo createComplexSamplePacket() throws XmppStringprepException {
        DiscoverInfoBuilder di = DiscoverInfo.builder("disco1");
        di.from(JidCreate.from("benvolio@capulet.lit/230193"));
        di.to(JidCreate.from("juliet@capulet.lit/chamber"));
        di.ofType(IQ.Type.result);

        Collection<DiscoverInfo.Identity> identities = new ArrayList<DiscoverInfo.Identity>();
        DiscoverInfo.Identity i = new DiscoverInfo.Identity("client", "pc", "Psi 0.11", "en");
        identities.add(i);
        i = new DiscoverInfo.Identity("client", "pc", "Ψ 0.11", "el");
        identities.add(i);
        di.addIdentities(identities);

        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature(EntityCapsManager.NAMESPACE);
        di.addFeature("http://jabber.org/protocol/muc");
        di.addFeature("http://jabber.org/protocol/disco#info");

        DataForm softwareInfoDataForm = createSampleSoftwareInfoDataForm();
        di.addExtension(softwareInfoDataForm);
        return di.build();
    }

    private static DiscoverInfo createMalformedDiscoverInfo() throws XmppStringprepException {
        DiscoverInfoBuilder di = DiscoverInfo.builder("disco1");
        di.from("benvolio@capulet.lit/230193");
        di.to(")juliet@capulet.lit/chamber");
        di.ofType(IQ.Type.result);

        Collection<DiscoverInfo.Identity> identities = new ArrayList<DiscoverInfo.Identity>();
        DiscoverInfo.Identity i = new DiscoverInfo.Identity("client", "pc", "Psi 0.11", "en");
        identities.add(i);
        i = new DiscoverInfo.Identity("client", "pc", "Ψ 0.11", "el");
        identities.add(i);
        di.addIdentities(identities);
        // Failure 1: Duplicate identities
        i = new DiscoverInfo.Identity("client", "pc", "Ψ 0.11", "el");
        identities.add(i);
        di.addIdentities(identities);

        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature(EntityCapsManager.NAMESPACE);
        di.addFeature("http://jabber.org/protocol/muc");
        di.addFeature("http://jabber.org/protocol/disco#info");
        // Failure 2: Duplicate features
        di.addFeature("http://jabber.org/protocol/disco#info");

        DataForm softwareInfoDataForm = createSampleSoftwareInfoDataForm();
        di.addExtension(softwareInfoDataForm);

        DiscoverInfo discoverInfo = di.buildWithoutValidiation();
        return discoverInfo;
    }

    public static File createTempDirectory() throws IOException {
        File tmp = File.createTempFile("entity", "caps");
        tmp.delete();
        tmp.mkdir();
        return tmp;
    }

}
