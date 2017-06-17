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
package org.jivesoftware.smackx.carbons;

import static org.jivesoftware.smack.test.util.CharsequenceEquals.equalsCharSequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.ExperimentalInitializerTest;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.carbons.provider.CarbonManagerProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class CarbonTest extends ExperimentalInitializerTest {

    private static Properties outputProperties = new Properties();
    static {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void carbonSentTest() throws Exception {
        XmlPullParser parser;
        String control;
        CarbonExtension cc;
        Forwarded fwd;

        control = XMLBuilder.create("sent")
            .e("forwarded")
                .a("xmlns", "urn:xmpp:forwarded:0")
                .e("message")
                    .a("from", "romeo@montague.com")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        cc = new CarbonManagerProvider().parse(parser);
        fwd = cc.getForwarded();

        // meta
        assertEquals(CarbonExtension.Direction.sent, cc.getDirection());

        // no delay in packet
        assertEquals(null, fwd.getDelayInformation());

        // check message
        assertThat("romeo@montague.com", equalsCharSequence(fwd.getForwardedStanza().getFrom()));

        // check end of tag
        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("sent", parser.getName());
    }

    @Test
    public void carbonReceivedTest() throws Exception {
        XmlPullParser parser;
        String control;
        CarbonExtension cc;

        control = XMLBuilder.create("received")
            .e("forwarded")
                .a("xmlns", "urn:xmpp:forwarded:0")
                .e("message")
                    .a("from", "romeo@montague.com")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        cc = new CarbonManagerProvider().parse(parser);

        assertEquals(CarbonExtension.Direction.received, cc.getDirection());

        // check end of tag
        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("received", parser.getName());
    }

    @Test(expected = Exception.class)
    public void carbonEmptyTest() throws Exception {
        XmlPullParser parser;
        String control;

        control = XMLBuilder.create("sent")
            .a("xmlns", "urn:xmpp:forwarded:0")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        new CarbonManagerProvider().parse(parser);
    }
}
