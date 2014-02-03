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
package org.jivesoftware.smackx.forward;

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
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;

public class ForwardedTest {

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
        
        parser = TestUtils.getParser(control, "forwarded");
        fwd = (Forwarded) new ForwardedProvider().parseExtension(parser);

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
        
        parser = TestUtils.getParser(control, "forwarded");
        new ForwardedProvider().parseExtension(parser);
    }
}
