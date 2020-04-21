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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.ox.crypto.OpenPgpElementAndMetadata;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.collection.PGPKeyRing;
import org.pgpainless.key.protection.UnprotectedKeysProtector;

public class PainlessOpenPgpProviderTest extends SmackTestSuite {

    private static final File storagePath;
    private static final BareJid alice = JidTestUtil.BARE_JID_1;
    private static final BareJid bob = JidTestUtil.BARE_JID_2;

    static {
        storagePath = new File(org.apache.commons.io.FileUtils.getTempDirectory(), "smack-painlessprovidertest");
    }

    @BeforeClass
    @AfterClass
    public static void deletePath() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(storagePath);
    }

    @Test
    public void encryptDecryptTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException, XmlPullParserException {

        // Initialize

        OpenPgpStore aliceStore = new FileBasedOpenPgpStore(storagePath);
        OpenPgpStore bobStore = new FileBasedOpenPgpStore(storagePath);

        aliceStore.setKeyRingProtector(new UnprotectedKeysProtector());
        bobStore.setKeyRingProtector(new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(new DummyConnection(), aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(new DummyConnection(), bobStore);

        PGPKeyRing aliceKeys = aliceStore.generateKeyRing(alice);
        PGPKeyRing bobKeys = bobStore.generateKeyRing(bob);

        OpenPgpV4Fingerprint aliceFingerprint = new OpenPgpV4Fingerprint(aliceKeys.getPublicKeys());
        OpenPgpV4Fingerprint bobFingerprint = new OpenPgpV4Fingerprint(bobKeys.getPublicKeys());

        aliceStore.importSecretKey(alice, aliceKeys.getSecretKeys());
        bobStore.importSecretKey(bob, bobKeys.getSecretKeys());

        aliceStore.setAnnouncedFingerprintsOf(alice, Collections.singletonMap(new OpenPgpV4Fingerprint(aliceKeys.getPublicKeys()), new Date()));
        bobStore.setAnnouncedFingerprintsOf(bob, Collections.singletonMap(new OpenPgpV4Fingerprint(bobKeys.getPublicKeys()), new Date()));

        OpenPgpSelf aliceSelf = new OpenPgpSelf(alice, aliceStore);
        aliceSelf.trust(aliceFingerprint);
        OpenPgpSelf bobSelf = new OpenPgpSelf(bob, bobStore);
        bobSelf.trust(bobFingerprint);

        // Exchange keys

        aliceStore.importPublicKey(bob, bobKeys.getPublicKeys());
        bobStore.importPublicKey(alice, aliceKeys.getPublicKeys());

        aliceStore.setAnnouncedFingerprintsOf(bob, Collections.singletonMap(new OpenPgpV4Fingerprint(bobKeys.getPublicKeys()), new Date()));
        bobStore.setAnnouncedFingerprintsOf(alice, Collections.singletonMap(new OpenPgpV4Fingerprint(aliceKeys.getPublicKeys()), new Date()));

        OpenPgpContact aliceForBob = new OpenPgpContact(alice, bobStore);
        aliceForBob.trust(aliceFingerprint);
        OpenPgpContact bobForAlice = new OpenPgpContact(bob, aliceStore);
        bobForAlice.trust(bobFingerprint);

        // Prepare message

        Message.Body body = new Message.Body(null, "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        List<ExtensionElement> payload = Collections.<ExtensionElement>singletonList(body);


        OpenPgpElementAndMetadata encrypted;
        OpenPgpMessage decrypted;

        /*
        test signcrypt
         */

        SigncryptElement signcryptElement = new SigncryptElement(Collections.<Jid>singleton(bob), payload);

        // Encrypt and Sign
        encrypted = aliceProvider.signAndEncrypt(signcryptElement, aliceSelf, Collections.singleton(bobForAlice));

        // Decrypt and Verify
        decrypted = bobProvider.decryptAndOrVerify(encrypted.getElement(), bobSelf, aliceForBob);

        OpenPgpV4Fingerprint decryptionFingerprint = decrypted.getMetadata().getDecryptionFingerprint();
        assertTrue(bobSelf.getSecretKeys().contains(decryptionFingerprint.getKeyId()));
        assertTrue(decrypted.getMetadata().getVerifiedSignatureKeyFingerprints().contains(aliceFingerprint));

        assertEquals(OpenPgpMessage.State.signcrypt, decrypted.getState());
        SigncryptElement decryptedSignCrypt = (SigncryptElement) decrypted.getOpenPgpContentElement();

        assertEquals(body.getMessage(), decryptedSignCrypt.<Message.Body>getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE).getMessage());

        /*
        test crypt
         */

        CryptElement cryptElement = new CryptElement(Collections.<Jid>singleton(bob), payload);

        // Encrypt
        encrypted = aliceProvider.encrypt(cryptElement, aliceSelf, Collections.singleton(bobForAlice));

        decrypted = bobProvider.decryptAndOrVerify(encrypted.getElement(), bobSelf, aliceForBob);

        decryptionFingerprint = decrypted.getMetadata().getDecryptionFingerprint();
        assertTrue(bobSelf.getSecretKeys().contains(decryptionFingerprint.getKeyId()));
        assertTrue(decrypted.getMetadata().getVerifiedSignatureKeyFingerprints().isEmpty());

        assertEquals(OpenPgpMessage.State.crypt, decrypted.getState());
        CryptElement decryptedCrypt = (CryptElement) decrypted.getOpenPgpContentElement();

        assertEquals(body.getMessage(), decryptedCrypt.<Message.Body>getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE).getMessage());

        /*
        test sign
         */

        SignElement signElement = new SignElement(Collections.<Jid>singleton(bob), new Date(), payload);

        // Sign
        encrypted = aliceProvider.sign(signElement, aliceSelf);

        decrypted = bobProvider.decryptAndOrVerify(encrypted.getElement(), bobSelf, aliceForBob);

        assertNull(decrypted.getMetadata().getDecryptionFingerprint());
        assertTrue(decrypted.getMetadata().getVerifiedSignatureKeyFingerprints().contains(aliceFingerprint));

        assertEquals(OpenPgpMessage.State.sign, decrypted.getState());
        SignElement decryptedSign = (SignElement) decrypted.getOpenPgpContentElement();

        assertEquals(body.getMessage(), decryptedSign.<Message.Body>getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE).getMessage());
    }
}
