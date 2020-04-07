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
package org.jivesoftware.smackx.bytestreams.ibb.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test for the OpenIQProvider class.
 *
 * @author Henning Staib
 */
public class OpenIQProviderTest extends InitExtensions {

    private static final Properties outputProperties = new Properties();
    {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void shouldCorrectlyParseIQStanzaAttribute() throws Exception {
        String control = XMLBuilder.create("open")
            .a("xmlns", "http://jabber.org/protocol/ibb")
            .a("block-size", "4096")
            .a("sid", "i781hf64")
            .a("stanza", "iq")
            .asString(outputProperties);

        OpenIQProvider oip = new OpenIQProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(control);
        Open open = oip.parse(parser);

        assertEquals(StanzaType.IQ, open.getStanza());
    }

    @Test
    public void shouldCorrectlyParseMessageStanzaAttribute() throws Exception {
        String control = XMLBuilder.create("open")
            .a("xmlns", "http://jabber.org/protocol/ibb")
            .a("block-size", "4096")
            .a("sid", "i781hf64")
            .a("stanza", "message")
            .asString(outputProperties);

        OpenIQProvider oip = new OpenIQProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(control);
        Open open = oip.parse(parser);

        assertEquals(StanzaType.MESSAGE, open.getStanza());
    }
}
