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
package org.jivesoftware.smackx.bytestreams.ibb.packet;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test for the DataPacketExtension class.
 *
 * @author Henning Staib
 */
public class DataPacketExtensionTest extends SmackTestSuite {

    @Test
    public void shouldNotInstantiateWithInvalidArgument1() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DataPacketExtension(null, 0, "data");
        });
    }

    @Test
    public void shouldNotInstantiateWithInvalidArgument2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DataPacketExtension("", 0, "data");
        });
    }

    @Test
    public void shouldNotInstantiateWithInvalidArgument3() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DataPacketExtension("sessionID", -1, "data");
        });
    }

    @Test
    public void shouldNotInstantiateWithInvalidArgument4() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DataPacketExtension("sessionID", 70000, "data");
        });
    }

    @Test
    public void shouldNotInstantiateWithInvalidArgument5() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DataPacketExtension("sessionID", 0, null);
        });
    }

    @Test
    public void shouldSetAllFieldsCorrectly() {
        DataPacketExtension data = new DataPacketExtension("sessionID", 0, "data");
        assertEquals("sessionID", data.getSessionID());
        assertEquals(0, data.getSeq());
        assertEquals("data", data.getData());
    }

    @Test
    public void shouldReturnNullIfDataIsInvalid() {
        // pad character is not at end of data
        DataPacketExtension data = new DataPacketExtension("sessionID", 0, "BBBB=CCC");
        assertNull(data.getDecodedData());

        // invalid Base64 character
        data = new DataPacketExtension("sessionID", 0, new String(new byte[] { 123 }, StandardCharsets.UTF_8));
        assertNull(data.getDecodedData());
    }

    private static final Properties outputProperties = new Properties();
    {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void shouldReturnValidIQStanzaXML() throws Exception {
        String control = XMLBuilder.create("data")
            .a("xmlns", "http://jabber.org/protocol/ibb")
            .a("seq", "0")
            .a("sid", "i781hf64")
            .t("DATA")
            .asString(outputProperties);

        DataPacketExtension data = new DataPacketExtension("i781hf64", 0, "DATA");
        assertXmlSimilar(control, data.toXML().toString());
    }

}
