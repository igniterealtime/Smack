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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.junit.Test;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Test for the Data class.
 * 
 * @author Henning Staib
 */
public class DataTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateWithInvalidArgument() {
        new Data(null);
    }

    @Test
    public void shouldBeOfIQTypeSET() {
        DataPacketExtension dpe = mock(DataPacketExtension.class);
        Data data = new Data(dpe);
        assertEquals(IQ.Type.set, data.getType());
    }

    private static Properties outputProperties = new Properties();
    {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void shouldReturnValidIQStanzaXML() throws Exception {
        String encodedData = StringUtils.encodeBase64("Test");
        
        String control = XMLBuilder.create("iq")
            .a("from", "romeo@montague.lit/orchard")
            .a("to", "juliet@capulet.lit/balcony")
            .a("id", "kr91n475")
            .a("type", "set")
            .e("data")
                .a("xmlns", "http://jabber.org/protocol/ibb")
                .a("seq", "0")
                .a("sid", "i781hf64")
                .t(encodedData)
            .asString(outputProperties);

        DataPacketExtension dpe = mock(DataPacketExtension.class);
        XmlStringBuilder dataTag = new XmlStringBuilder();
        dataTag.halfOpenElement(DataPacketExtension.ELEMENT);
        dataTag.xmlnsAttribute(DataPacketExtension.NAMESPACE);
        dataTag.attribute("seq", "0");
        dataTag.attribute("sid", "i781hf64");
        dataTag.rightAngleBracket();
        dataTag.escape(encodedData);
        dataTag.closeElement(DataPacketExtension.ELEMENT);
        when(dpe.toXML()).thenReturn(dataTag);
        Data data = new Data(dpe);
        data.setFrom("romeo@montague.lit/orchard");
        data.setTo("juliet@capulet.lit/balcony");
        data.setPacketID("kr91n475");
        
        assertXMLEqual(control, data.toXML().toString());
    }

}
