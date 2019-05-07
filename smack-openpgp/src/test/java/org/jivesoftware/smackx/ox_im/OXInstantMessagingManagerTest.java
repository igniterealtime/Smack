/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox_im;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.Date;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.FileTestUtil;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpSelf;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.decryption_verification.OpenPgpMetadata;

public class OXInstantMessagingManagerTest extends SmackTestSuite {

    private static final File basePath;

    static {
        basePath = FileTestUtil.getTempDir("ox_im_test_" + StringUtils.randomString(10));
    }

    @Test
    public void test() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, SmackException, MissingUserIdOnKeyException, InterruptedException, XMPPException,
            XmlPullParserException {
        DummyConnection aliceCon = new DummyConnection(
                DummyConnection.DummyConnectionConfiguration.builder()
                        .setXmppDomain(JidTestUtil.EXAMPLE_ORG)
                        .setUsernameAndPassword("alice", "dummypass").build());
        aliceCon.connect().login();

        DummyConnection bobCon = new DummyConnection(
                DummyConnection.DummyConnectionConfiguration.builder()
                        .setXmppDomain(JidTestUtil.EXAMPLE_ORG)
                        .setUsernameAndPassword("bob", "dummypass").build());
        bobCon.connect().login();

        FileBasedOpenPgpStore aliceStore = new FileBasedOpenPgpStore(new File(basePath, "alice"));
        FileBasedOpenPgpStore bobStore = new FileBasedOpenPgpStore(new File(basePath, "bob"));

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(aliceCon, aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(bobCon, bobStore);

        OpenPgpManager aliceOpenPgp = OpenPgpManager.getInstanceFor(aliceCon);
        OpenPgpManager bobOpenPgp = OpenPgpManager.getInstanceFor(bobCon);

        aliceOpenPgp.setOpenPgpProvider(aliceProvider);
        bobOpenPgp.setOpenPgpProvider(bobProvider);

        OXInstantMessagingManager aliceOxim = OXInstantMessagingManager.getInstanceFor(aliceCon);

        OpenPgpSelf aliceSelf = aliceOpenPgp.getOpenPgpSelf();
        OpenPgpSelf bobSelf = bobOpenPgp.getOpenPgpSelf();

        assertFalse(aliceSelf.hasSecretKeyAvailable());
        assertFalse(bobSelf.hasSecretKeyAvailable());

        // Generate keys
        aliceOpenPgp.generateAndImportKeyPair(aliceSelf.getJid());
        bobOpenPgp.generateAndImportKeyPair(bobSelf.getJid());

        assertTrue(aliceSelf.hasSecretKeyAvailable());
        assertTrue(bobSelf.hasSecretKeyAvailable());

        assertTrue(aliceSelf.isTrusted(aliceSelf.getSigningKeyFingerprint()));
        assertTrue(bobSelf.isTrusted(bobSelf.getSigningKeyFingerprint()));

        assertTrue(aliceSelf.getTrustedFingerprints().contains(aliceSelf.getSigningKeyFingerprint()));

        // Exchange keys
        aliceStore.importPublicKey(bobSelf.getJid(), bobSelf.getAnnouncedPublicKeys().iterator().next());
        bobStore.importPublicKey(aliceSelf.getJid(), aliceSelf.getAnnouncedPublicKeys().iterator().next());

        // Simulate key announcement
        bobStore.setAnnouncedFingerprintsOf(bobSelf.getJid(), Collections.singletonMap(bobSelf.getSigningKeyFingerprint(), new Date()));
        bobStore.setAnnouncedFingerprintsOf(aliceSelf.getJid(), Collections.singletonMap(aliceSelf.getSigningKeyFingerprint(), new Date()));
        aliceStore.setAnnouncedFingerprintsOf(aliceSelf.getJid(), Collections.singletonMap(aliceSelf.getSigningKeyFingerprint(), new Date()));
        aliceStore.setAnnouncedFingerprintsOf(bobSelf.getJid(), Collections.singletonMap(bobSelf.getSigningKeyFingerprint(), new Date()));

        OpenPgpContact aliceForBob = bobOpenPgp.getOpenPgpContact((EntityBareJid) aliceSelf.getJid());
        OpenPgpContact bobForAlice = aliceOpenPgp.getOpenPgpContact((EntityBareJid) bobSelf.getJid());

        assertTrue(aliceForBob.hasUndecidedKeys());
        assertTrue(bobForAlice.hasUndecidedKeys());

        assertTrue(aliceForBob.getUndecidedFingerprints().contains(aliceSelf.getSigningKeyFingerprint()));
        assertTrue(bobForAlice.getUndecidedFingerprints().contains(bobSelf.getSigningKeyFingerprint()));

        bobForAlice.trust(bobSelf.getSigningKeyFingerprint());
        aliceForBob.trust(aliceSelf.getSigningKeyFingerprint());

        assertFalse(aliceForBob.hasUndecidedKeys());
        assertFalse(bobForAlice.hasUndecidedKeys());

        Message message = new Message();
        assertFalse(ExplicitMessageEncryptionElement.hasProtocol(message, ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));

        aliceOxim.addOxMessage(message, bobForAlice,
                Collections.<ExtensionElement>singletonList(new Message.Body(null, "Hello World!")));
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message, ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        assertNotNull(OpenPgpElement.fromStanza(message));

        OpenPgpMessage decrypted = bobOpenPgp.decryptOpenPgpElement(OpenPgpElement.fromStanza(message), aliceForBob);
        assertEquals(OpenPgpMessage.State.signcrypt, decrypted.getState());

        SigncryptElement signcryptElement = (SigncryptElement) decrypted.getOpenPgpContentElement();

        Message.Body body = signcryptElement.getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE);
        assertNotNull(body);
        assertEquals("Hello World!", body.getMessage());

        OpenPgpMetadata metadata = decrypted.getMetadata();
        assertTrue(metadata.isSigned() && metadata.isEncrypted());

        // Check, if one of Bobs keys was used for decryption
        assertNotNull(bobSelf.getSigningKeyRing().getPublicKey(metadata.getDecryptionFingerprint().getKeyId()));

        // Check if one of Alice' keys was used for signing
        assertTrue(metadata.containsVerifiedSignatureFrom(
                aliceForBob.getTrustedAnnouncedKeys().iterator().next()));
    }

    @AfterClass
    @BeforeClass
    public static void deleteDirs() {
        FileTestUtil.deleteDirectory(basePath);
    }
}
