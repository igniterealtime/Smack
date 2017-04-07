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

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.InitExtensions;
import org.junit.Test;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Test for the DataPacketExtension class.
 * 
 * @author Henning Staib
 */
public class DataPacketExtensionTest extends InitExtensions {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument1() {
        new DataPacketExtension(null, 0, "data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument2() {
        new DataPacketExtension("", 0, "data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument3() {
        new DataPacketExtension("sessionID", -1, "data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument4() {
        new DataPacketExtension("sessionID", 70000, "data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument5() {
        new DataPacketExtension("sessionID", 0, null);
    }

    @Test
    public void shouldSetAllFieldsCorrectly() {
        DataPacketExtension data = new DataPacketExtension("sessionID", 0, "data");
        assertEquals("sessionID", data.getSessionID());
        assertEquals(0, data.getSeq());
        assertEquals("data", data.getData());
    }

    @Test
    public void shouldReturnNullIfDataIsInvalid() throws UnsupportedEncodingException {
        // pad character is not at end of data
        DataPacketExtension data = new DataPacketExtension("sessionID", 0, "BBBB=CCC");
        assertNull(data.getDecodedData());

        // invalid Base64 character
        data = new DataPacketExtension("sessionID", 0, new String(new byte[] { 123 }, StringUtils.UTF8));
        assertNull(data.getDecodedData());
    }

    private static Properties outputProperties = new Properties();
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
        assertXMLEqual(control, data.toXML().toString());
    }

}
