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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.omemo.element.OmemoElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement;
import org.jivesoftware.smackx.omemo.provider.OmemoVAxolotlProvider;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;

import org.junit.Test;

/**
 * Test serialization and parsing of OmemoVAxolotlElements.
 */
public class OmemoVAxolotlElementTest extends SmackTestSuite {

    @Test
    public void serializationTest() throws Exception {
        byte[] payload = "This is payload.".getBytes(StringUtils.UTF8);
        int keyId1 = 8;
        int keyId2 = 33333;
        byte[] keyData1 = "KEYDATA".getBytes(StringUtils.UTF8);
        byte[] keyData2 = "DATAKEY".getBytes(StringUtils.UTF8);
        int sid = 12131415;
        byte[] iv = OmemoMessageBuilder.generateIv();

        ArrayList<OmemoKeyElement> keys = new ArrayList<>();
        keys.add(new OmemoKeyElement(keyData1, keyId1));
        keys.add(new OmemoKeyElement(keyData2, keyId2, true));

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
    }
}
