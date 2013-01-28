/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.forward.Forwarded;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;

public class CarbonForwardedTest {

    private static Properties outputProperties = new Properties();
    static {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void forwardedTest() throws Exception {
        XmlPullParser parser;
        String control;
        Forwarded fwd;
        
        control = XMLBuilder.create("forwarded")
            .a("xmlns", "urn:xmpp:forwarded:0")
            .e("message")
                    .a("from", "romeo@montague.com")
            .asString(outputProperties);
        
        parser = getParser(control, "forwarded");
        fwd = (Forwarded) new Forwarded.Provider().parseExtension(parser);

        // no delay in packet
        assertEquals(null, fwd.getDelayInfo());
        
        // check message
        assertEquals("romeo@montague.com", fwd.getForwardedPacket().getFrom());

        // check end of tag
        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("forwarded", parser.getName());

    }

    @Test(expected=Exception.class)
    public void forwardedEmptyTest() throws Exception {
        XmlPullParser parser;
        String control;
        
        control = XMLBuilder.create("forwarded")
            .a("xmlns", "urn:xmpp:forwarded:0")
            .asString(outputProperties);
        
        parser = getParser(control, "forwarded");
        new Forwarded.Provider().parseExtension(parser);
    }

    @Test
    public void carbonSentTest() throws Exception {
        XmlPullParser parser;
        String control;
        Carbon cc;
        Forwarded fwd;
        
        control = XMLBuilder.create("sent")
            .e("forwarded")
                .a("xmlns", "urn:xmpp:forwarded:0")
                .e("message")
                    .a("from", "romeo@montague.com")
            .asString(outputProperties);
        
        parser = getParser(control, "sent");
        cc = (Carbon) new Carbon.Provider().parseExtension(parser);
        fwd = cc.getForwarded();

        // meta
        assertEquals(Carbon.Direction.sent, cc.getDirection());
        
        // no delay in packet
        assertEquals(null, fwd.getDelayInfo());
        
        // check message
        assertEquals("romeo@montague.com", fwd.getForwardedPacket().getFrom());

        // check end of tag
        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("sent", parser.getName());
    }

    @Test
    public void carbonReceivedTest() throws Exception {
        XmlPullParser parser;
        String control;
        Carbon cc;
        
        control = XMLBuilder.create("received")
            .e("forwarded")
                .a("xmlns", "urn:xmpp:forwarded:0")
                .e("message")
                    .a("from", "romeo@montague.com")
            .asString(outputProperties);
        
        parser = getParser(control, "received");
        cc = (Carbon) new Carbon.Provider().parseExtension(parser);

        assertEquals(Carbon.Direction.received, cc.getDirection());
        
        // check end of tag
        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("received", parser.getName());
    }

    @Test(expected=Exception.class)
    public void carbonEmptyTest() throws Exception {
        XmlPullParser parser;
        String control;
        
        control = XMLBuilder.create("sent")
            .a("xmlns", "urn:xmpp:forwarded:0")
            .asString(outputProperties);
        
        parser = getParser(control, "sent");
        new Carbon.Provider().parseExtension(parser);
    }

    private XmlPullParser getParser(String control, String startTag)
                    throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(control));

        while (true) {
            if (parser.next() == XmlPullParser.START_TAG
                            && parser.getName().equals(startTag)) {
                break;
            }
        }
        return parser;
    }

}
