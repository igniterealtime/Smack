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

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.omemo.element.OmemoElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoKeysElement_VOmemo;
import org.jivesoftware.smackx.omemo.provider.OmemoVAxolotlProvider;
import org.jivesoftware.smackx.omemo.provider.OmemoVOmemoProvider;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;

import org.junit.Test;

/**
 * Test serialization and parsing of OmemoVAxolotlElements.
 */
public class OmemoElementTest extends SmackTestSuite {

    @Test
    public void serializationTest() throws Exception {
        byte[] payload = "This is payload.".getBytes(StandardCharsets.UTF_8);
        int keyId1 = 8;
        int keyId2 = 33333;
        byte[] keyData1 = "KEYDATA".getBytes(StandardCharsets.UTF_8);
        byte[] keyData2 = "DATAKEY".getBytes(StandardCharsets.UTF_8);
        int sid = 12131415;
        byte[] iv = OmemoMessageBuilder.generateIv();

        // === Test with VAxolotl ===/
        ArrayList<OmemoKeyElement_VAxolotl> keys = new ArrayList<>();
        keys.add(new OmemoKeyElement_VAxolotl(keyData1, keyId1));
        keys.add(new OmemoKeyElement_VAxolotl(keyData2, keyId2, true));

        OmemoHeaderElement_VAxolotl header = new OmemoHeaderElement_VAxolotl(sid, keys, iv);
        OmemoElement_VAxolotl element = new OmemoElement_VAxolotl(header, payload);

        String expected =
                "<encrypted xmlns='eu.siacs.conversations.axolotl'>" +
                        "<header sid='12131415'>" +
                        "<key rid='8'>" + Base64.encodeToString(keyData1) + "</key>" +
                        "<key prekey='true' rid='33333'>" + Base64.encodeToString(keyData2) + "</key>" +
                        "<iv>" + Base64.encodeToString(iv) + "</iv>" +
                        "</header>" +
                        "<payload>" +
                        Base64.encodeToString(payload) +
                        "</payload>" +
                        "</encrypted>";

        String actual = element.toXML().toString();
        assertEquals("Serialized xml of OmemoElement must match.", expected, actual);

        OmemoElement_VAxolotl parsed = new OmemoVAxolotlProvider().parse(TestUtils.getParser(actual));
        assertEquals("Parsed OmemoElement must equal the original.",
                element.toXML().toString(),
                parsed.toXML().toString());

        // === Test with VOmemo === /
        int keySid = 27183;
        int keyRid1 = 31415;
        int keyRid2 = 1337;
        int keyRid3 = 12321;
        String sJid = "juliet@capulet.lit";
        String rJid = "romeo@montague.lit";

        List<OmemoKeyElement_VOmemo> keySids = new ArrayList<>(
                List.of(new OmemoKeyElement_VOmemo(keyData1, keyRid1))
        );
        OmemoKeysElement_VOmemo keysSid = new OmemoKeysElement_VOmemo(sJid, keySids);

        List<OmemoKeyElement_VOmemo> keyRids = new ArrayList<>(
                List.of(new OmemoKeyElement_VOmemo(keyData1, keyRid2),
                        new OmemoKeyElement_VOmemo(keyData2, keyRid3, true)
                ));
        OmemoKeysElement_VOmemo keysRid = new OmemoKeysElement_VOmemo(rJid, keyRids);

        List<OmemoKeysElement_VOmemo> keysElement = new ArrayList<>(
                List.of(keysSid, keysRid)
        );

        OmemoHeaderElement_VOmemo header2 = new OmemoHeaderElement_VOmemo(keySid, keysElement, iv);
        OmemoElement_VOmemo element2 = new OmemoElement_VOmemo(header2, payload);

        String expected2 =
                "<encrypted xmlns='urn:xmpp:omemo:2'>" +
                        "<header sid='27183'>" +
                        "<keys jid='juliet@capulet.lit'>" +
                        "<key rid='31415'>" + Base64.encodeToString(keyData1) + "</key>" +
                        "</keys>" +
                        "<keys jid='romeo@montague.lit'>" +
                        "<key rid='1337'>" + Base64.encodeToString(keyData1) + "</key>" +
                        "<key kex='true' rid='12321'>" + Base64.encodeToString(keyData2) + "</key>" +
                        "</keys>" +
                        "<iv>" + Base64.encodeToString(iv) + "</iv>" +
                        "</header>" +
                        "<payload>" +
                        Base64.encodeToString(payload) +
                        "</payload>" +
                        "</encrypted>";

        String actual2 = element2.toXML().toString();
        assertEquals("Serialized xml of OmemoElement must match.", expected2, actual2);

        OmemoElement_VOmemo parsed2 = new OmemoVOmemoProvider().parse(TestUtils.getParser(actual2));
        assertEquals("Parsed OmemoElement must equal the original.",
                element2.toXML().toString(),
                parsed2.toXML().toString());
    }


}
