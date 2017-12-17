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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
        Open open = oip.parse(getParser(control));

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
        Open open = oip.parse(getParser(control));

        assertEquals(StanzaType.MESSAGE, open.getStanza());
    }

    private static XmlPullParser getParser(String control) throws XmlPullParserException,
                    IOException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader(control));
        while (true) {
            if (parser.next() == XmlPullParser.START_TAG
                            && parser.getName().equals("open")) {
                break;
            }
        }
        return parser;
    }

}
