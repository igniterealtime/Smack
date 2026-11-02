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
package org.jivesoftware.smackx.omemo;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement_VOmemo;
import org.jivesoftware.smackx.omemo.provider.OmemoBundleVAxolotlProvider;
import org.jivesoftware.smackx.omemo.provider.OmemoBundleVOmemoProvider;

import org.junit.Test;

/**
 * Test serialization and parsing of the Omemo Bundle Element.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoBundleElementTest extends SmackTestSuite {
    int signedPreKeyId = 0;
    static int preKeyId1 = 54, preKeyId2 = 98;
    static String preKey1B64 = Base64.encodeToString("FirstPreKey".getBytes(StandardCharsets.UTF_8));
    static String preKey2B64 = Base64.encodeToString("SecondPreKey".getBytes(StandardCharsets.UTF_8));

    // Preserve the order of the pk's in prekeys.
    static Map<Integer, String> preKeysB64 = new LinkedHashMap<>();
    static {
        preKeysB64.put(preKeyId1, preKey1B64);
        preKeysB64.put(preKeyId2, preKey2B64);
    }

    String signedPreKeyB64 = Base64.encodeToString("SignedPreKey".getBytes(StandardCharsets.UTF_8));
    String signedPreKeySignatureB64 = Base64.encodeToString("SignedPreKeySignature".getBytes(StandardCharsets.UTF_8));
    String identityKeyB64 = Base64.encodeToString("IdentityKey".getBytes(StandardCharsets.UTF_8));

    byte[] signedPreKey = "SignedPreKey".getBytes(StandardCharsets.UTF_8);
    byte[] signedPreKeySignature = "SignedPreKeySignature".getBytes(StandardCharsets.UTF_8);
    byte[] identityKey = "IdentityKey".getBytes(StandardCharsets.UTF_8);
    byte[] firstPreKey = "FirstPreKey".getBytes(StandardCharsets.UTF_8);
    byte[] secondPreKey = "SecondPreKey".getBytes(StandardCharsets.UTF_8);

    // ===== NameSpace: eu.siacs.conversations.axolotl Test =====/
    @Test
    public void serializationAxolotlTest() throws Exception {
        OmemoBundleElement_VAxolotl bundle = new OmemoBundleElement_VAxolotl(signedPreKeyId,
                signedPreKeyB64, signedPreKeySignatureB64, identityKeyB64, preKeysB64);

        assertEquals("ElementName must match.", "bundle", bundle.getElementName());
        assertEquals("Namespace must match.", "eu.siacs.conversations.axolotl", bundle.getNamespace());

        String expected = "<bundle xmlns='eu.siacs.conversations.axolotl'>" +
                "<signedPreKeyPublic signedPreKeyId='0'>" + signedPreKeyB64 + "</signedPreKeyPublic>" +
                "<signedPreKeySignature>" + signedPreKeySignatureB64 + "</signedPreKeySignature>" +
                "<identityKey>" + identityKeyB64 + "</identityKey>" +
                "<prekeys>" +
                "<preKeyPublic preKeyId='54'>" + preKey1B64 + "</preKeyPublic>" +
                "<preKeyPublic preKeyId='98'>" + preKey2B64 + "</preKeyPublic>" +
                "</prekeys>" +
                "</bundle>";
        String actual = bundle.toXML().toString();
        assertEquals("Bundles XML must match.", expected, actual);

        OmemoBundleElement_VAxolotl parsed = new OmemoBundleVAxolotlProvider().parse(TestUtils.getParser(actual));

        assertArrayEquals("B64-decoded signedPreKey must match.", signedPreKey, parsed.getSignedPreKey());
        assertEquals("SignedPreKeyId must match", signedPreKeyId, parsed.getSignedPreKeyId());
        assertArrayEquals("B64-decoded signedPreKey signature must match.", signedPreKeySignature, parsed.getSignedPreKeySignature());
        assertArrayEquals("B64-decoded identityKey must match.", identityKey, parsed.getIdentityKey());
        assertArrayEquals("B64-decoded first preKey must match.", firstPreKey, parsed.getPreKey(preKeyId1));
        assertArrayEquals("B64-decoded second preKey must match.", secondPreKey, parsed.getPreKey(preKeyId2));
        assertEquals("toString outputs must match.", bundle.toString(), parsed.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPreKeysAxolotlShouldFailTest() throws Exception {
        String s = "<bundle xmlns='eu.siacs.conversations.axolotl'><signedPreKeyPublic signedPreKeyId='1'>BU4bJ18+rqbSnBblZU8pR/s+impyhoL9AJssJIE59fZb</signedPreKeyPublic><signedPreKeySignature>MaQtv7ySqHpPr0gkVtMp4KmWC61Hnfs5a7/cKEhrX8n12evGdkg4fNf3Q/ufgmJu5dnup9pkTA1pj00dTbtXjw==</signedPreKeySignature><identityKey>BWO0QOem1YXIJuT61cxXpG/mKlvISDwZxQJHW2/7eVki</identityKey><prekeys></prekeys></bundle>";
        XmlPullParser parser = TestUtils.getParser(s);
        new OmemoBundleVAxolotlProvider().parse(parser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPreKeysAxolotlShouldAlsoFailTest() throws Exception {
        String s = "<bundle xmlns='eu.siacs.conversations.axolotl'><signedPreKeyPublic signedPreKeyId='1'>BU4bJ18+rqbSnBblZU8pR/s+impyhoL9AJssJIE59fZb</signedPreKeyPublic><signedPreKeySignature>MaQtv7ySqHpPr0gkVtMp4KmWC61Hnfs5a7/cKEhrX8n12evGdkg4fNf3Q/ufgmJu5dnup9pkTA1pj00dTbtXjw==</signedPreKeySignature><identityKey>BWO0QOem1YXIJuT61cxXpG/mKlvISDwZxQJHW2/7eVki</identityKey></bundle>";
        XmlPullParser parser = TestUtils.getParser(s);
        new OmemoBundleVAxolotlProvider().parse(parser);
    }

    // ===== NameSpace: urn:xmpp:omemo:2 Test =====/
    @Test
    public void serializationOmemoTest() throws Exception {
        OmemoBundleElement_VOmemo bundle = new OmemoBundleElement_VOmemo(signedPreKeyId, signedPreKeyB64, signedPreKeySignatureB64, identityKeyB64, preKeysB64);

        assertEquals("ElementName must match.", "bundle", bundle.getElementName());
        assertEquals("Namespace must match.", "urn:xmpp:omemo:2", bundle.getNamespace());

        String expected = "<bundle xmlns='urn:xmpp:omemo:2'>" +
                "<spk id='0'>" + signedPreKeyB64 + "</spk>" +
                "<spks>" + signedPreKeySignatureB64 + "</spks>" +
                "<ik>" + identityKeyB64 + "</ik>" +
                "<prekeys>" +
                "<pk id='54'>" + preKey1B64 + "</pk>" +
                "<pk id='98'>" + preKey2B64 + "</pk>" +
                "</prekeys>" +
                "</bundle>";
        String actual = bundle.toXML().toString();
        assertEquals("Bundles XML must match.", expected, actual);

        OmemoBundleElement_VOmemo parsed = new OmemoBundleVOmemoProvider().parse(TestUtils.getParser(actual));

        assertArrayEquals("B64-decoded signedPreKey must match.", signedPreKey, parsed.getSignedPreKey());
        assertEquals("SignedPreKeyId must match", signedPreKeyId, parsed.getSignedPreKeyId());
        assertArrayEquals("B64-decoded signedPreKey signature must match.", signedPreKeySignature, parsed.getSignedPreKeySignature());
        assertArrayEquals("B64-decoded identityKey must match.", identityKey, parsed.getIdentityKey());
        assertArrayEquals("B64-decoded first preKey must match.", firstPreKey, parsed.getPreKey(preKeyId1));
        assertArrayEquals("B64-decoded second preKey must match.", secondPreKey, parsed.getPreKey(preKeyId2));
        assertEquals("toString outputs must match.", bundle.toString(), parsed.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPreKeysOmemoShouldFailTest() throws Exception {
        String s = "<bundle xmlns='urn:xmpp:omemo:2'><spk id='1'>BU4bJ18+rqbSnBblZU8pR/s+impyhoL9AJssJIE59fZb</spk><spks>MaQtv7ySqHpPr0gkVtMp4KmWC61Hnfs5a7/cKEhrX8n12evGdkg4fNf3Q/ufgmJu5dnup9pkTA1pj00dTbtXjw==</spks><ik>BWO0QOem1YXIJuT61cxXpG/mKlvISDwZxQJHW2/7eVki</ik><prekeys></prekeys></bundle>";
        XmlPullParser parser = TestUtils.getParser(s);
        new OmemoBundleVOmemoProvider().parse(parser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPreKeysOmemoShouldAlsoFailTest() throws Exception {
        String s = "<bundle xmlns='urn:xmpp:omemo:2'><spk id='1'>BU4bJ18+rqbSnBblZU8pR/s+impyhoL9AJssJIE59fZb</spk><spks>MaQtv7ySqHpPr0gkVtMp4KmWC61Hnfs5a7/cKEhrX8n12evGdkg4fNf3Q/ufgmJu5dnup9pkTA1pj00dTbtXjw==</spks><ik>BWO0QOem1YXIJuT61cxXpG/mKlvISDwZxQJHW2/7eVki</ik></bundle>";
        XmlPullParser parser = TestUtils.getParser(s);
        new OmemoBundleVOmemoProvider().parse(parser);
    }
}
