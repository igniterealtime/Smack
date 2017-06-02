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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.ClearTextMessage;
import org.jivesoftware.smackx.omemo.internal.IdentityKeyWrapper;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;

import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Test the identityKeyWrapper.
 */
public class WrapperObjectsTest {

    @Test
    public void identityKeyWrapperTest() {
        Object pseudoKey = new Object();
        IdentityKeyWrapper wrapper = new IdentityKeyWrapper(pseudoKey);
        assertEquals(pseudoKey, wrapper.getIdentityKey());
    }

    @Test
    public void ciphertextTupleTest() {
        byte[] c = OmemoMessageBuilder.generateIv();
        CiphertextTuple c1 = new CiphertextTuple(c, OmemoElement.TYPE_OMEMO_PREKEY_MESSAGE);
        assertTrue(c1.isPreKeyMessage());
        assertArrayEquals(c, c1.getCiphertext());
        assertEquals(OmemoElement.TYPE_OMEMO_PREKEY_MESSAGE, c1.getMessageType());

        CiphertextTuple c2 = new CiphertextTuple(c, OmemoElement.TYPE_OMEMO_MESSAGE);
        assertFalse(c2.isPreKeyMessage());
        assertEquals(OmemoElement.TYPE_OMEMO_MESSAGE, c2.getMessageType());
    }

    @Test
    public void clearTextMessageTest() throws Exception {
        Object pseudoKey = new Object();
        IdentityKeyWrapper wrapper = new IdentityKeyWrapper(pseudoKey);
        BareJid senderJid = JidCreate.bareFrom("bob@server.tld");
        OmemoDevice sender = new OmemoDevice(senderJid, 1234);
        OmemoMessageInformation information = new OmemoMessageInformation(wrapper, sender, OmemoMessageInformation.CARBON.NONE);

        assertTrue("OmemoInformation must state that the message is an OMEMO message.",
                information.isOmemoMessage());
        assertEquals(OmemoMessageInformation.CARBON.NONE, information.getCarbon());
        assertEquals(sender, information.getSenderDevice());
        assertEquals(wrapper, information.getSenderIdentityKey());

        String body = "Decrypted Body";
        Message message = new Message(senderJid, body);
        ClearTextMessage c = new ClearTextMessage(body, message, information);

        assertEquals(message, c.getOriginalMessage());
        assertEquals(information, c.getMessageInformation());
        assertEquals(body, c.getBody());
    }

    @Test
    public void cipherAndAuthTagTest() throws NoSuchAlgorithmException, CryptoFailedException {
        Security.addProvider(new BouncyCastleProvider());
        byte[] key = OmemoMessageBuilder.generateKey();
        byte[] iv = OmemoMessageBuilder.generateIv();
        byte[] authTag = OmemoMessageBuilder.generateIv();

        CipherAndAuthTag cat = new CipherAndAuthTag(key, iv, authTag);

        assertNotNull(cat.getCipher());
        assertArrayEquals(key, cat.getKey());
        assertArrayEquals(iv, cat.getIv());
        assertArrayEquals(authTag, cat.getAuthTag());
    }
}
