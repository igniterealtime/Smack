/**
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
package org.jivesoftware.smack.omemo;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.omemo.element.OmemoBundleVAxolotlElement;
import org.jivesoftware.smackx.omemo.provider.OmemoBundleVAxolotlProvider;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test serialization and parsing of the OmemoBundleVAxolotlElement.
 */
public class OmemoBundleVAxolotlElementTest extends SmackTestSuite {

    @Test
    public void serializationTest() throws Exception {
        int signedPreKeyId = 420;
        String signedPreKeyB64 = Base64.encodeToString("SignedPreKey".getBytes(StringUtils.UTF8));
        String signedPreKeySigB64 = Base64.encodeToString("SignedPreKeySignature".getBytes(StringUtils.UTF8));
        String identityKeyB64 = Base64.encodeToString("IdentityKey".getBytes(StringUtils.UTF8));
        int preKeyId1 = 220, preKeyId2 = 284;
        String preKey1B64 = Base64.encodeToString("FirstPreKey".getBytes(StringUtils.UTF8)),
                preKey2B64 = Base64.encodeToString("SecondPreKey".getBytes(StringUtils.UTF8));
        HashMap<Integer, String> preKeysB64 = new HashMap<>();
        preKeysB64.put(preKeyId1, preKey1B64);
        preKeysB64.put(preKeyId2, preKey2B64);

        OmemoBundleVAxolotlElement bundle = new OmemoBundleVAxolotlElement(signedPreKeyId,
                signedPreKeyB64, signedPreKeySigB64, identityKeyB64, preKeysB64);

        assertEquals("ElementName must match.", "bundle", bundle.getElementName());
        assertEquals("Namespace must match.", "eu.siacs.conversations.axolotl", bundle.getNamespace());

        String expected =
                "<bundle xmlns='eu.siacs.conversations.axolotl'>" +
                    "<signedPreKeyPublic signedPreKeyId='420'>" +
                        signedPreKeyB64 +
                    "</signedPreKeyPublic>" +
                    "<signedPreKeySignature>" +
                        signedPreKeySigB64 +
                    "</signedPreKeySignature>" +
                    "<identityKey>" +
                        identityKeyB64 +
                    "</identityKey>" +
                    "<prekeys>" +
                        "<preKeyPublic preKeyId='220'>" +
                            preKey1B64 +
                        "</preKeyPublic>" +
                        "<preKeyPublic preKeyId='284'>" +
                            preKey2B64 +
                        "</preKeyPublic>" +
                    "</prekeys>" +
                "</bundle>";
        String actual = bundle.toXML().toString();
        assertEquals("Bundles XML must match.", expected, actual);

        byte[] signedPreKey = "SignedPreKey".getBytes(StringUtils.UTF8);
        byte[] signedPreKeySig = "SignedPreKeySignature".getBytes(StringUtils.UTF8);
        byte[] identityKey = "IdentityKey".getBytes(StringUtils.UTF8);
        byte[] firstPreKey = "FirstPreKey".getBytes(StringUtils.UTF8);
        byte[] secondPreKey = "SecondPreKey".getBytes(StringUtils.UTF8);

        OmemoBundleVAxolotlElement parsed = new OmemoBundleVAxolotlProvider().parse(TestUtils.getParser(actual));

        assertTrue("B64-decoded signedPreKey must match.", Arrays.equals(signedPreKey, parsed.getSignedPreKey()));
        assertEquals("SignedPreKeyId must match", signedPreKeyId, parsed.getSignedPreKeyId());
        assertTrue("B64-decoded signedPreKey signature must match.", Arrays.equals(signedPreKeySig, parsed.getSignedPreKeySignature()));
        assertTrue("B64-decoded identityKey must match.", Arrays.equals(identityKey, parsed.getIdentityKey()));
        assertTrue("B64-decoded first preKey must match.", Arrays.equals(firstPreKey, parsed.getPreKey(220)));
        assertTrue("B64-decoded second preKey must match.", Arrays.equals(secondPreKey, parsed.getPreKey(284)));
        assertEquals("toString outputs must match.", bundle.toString(), parsed.toString());
    }
}
