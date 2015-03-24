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
package org.jivesoftware.smackx.delay.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.FactoryConfigurationError;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jxmpp.util.XmppDateTime;
import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.delay.DelayInformationManager;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jamesmurty.utils.XMLBuilder;

public class DelayInformationTest extends InitExtensions {

    private static final Calendar calendar = new GregorianCalendar(2002, 9 - 1, 10, 23, 8, 25);
    private static Properties outputProperties = new Properties();
    static {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void delayInformationTest() throws Exception {
        DelayInformationProvider p = new DelayInformationProvider();
        DelayInformation delayInfo;
        XmlPullParser parser;
        String control;
        GregorianCalendar calendar = new GregorianCalendar(2002, 9 - 1, 10, 23, 8, 25);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime(); 

        control = XMLBuilder.create("x")
            .a("xmlns", "jabber:x:delay")
            .a("from", "capulet.com")
            .a("stamp", "2002-09-10T23:08:25Z")
            .t("Offline Storage")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        delayInfo = p.parse(parser);

        assertEquals("capulet.com", delayInfo.getFrom());
        assertEquals(date, delayInfo.getStamp());
        assertEquals("Offline Storage", delayInfo.getReason());

        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("x", parser.getName());

        control = XMLBuilder.create("x")
            .a("xmlns", "jabber:x:delay")
            .a("from", "capulet.com")
            .a("stamp", "2002-09-10T23:08:25Z")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        delayInfo = p.parse(parser);

        assertEquals("capulet.com", delayInfo.getFrom());
        assertEquals(date, delayInfo.getStamp());
        assertNull(delayInfo.getReason());

        assertEquals(XmlPullParser.END_TAG, parser.getEventType());
        assertEquals("x", parser.getName());

    }

    @Test
    public void dateFormatsTest() throws Exception {
        DelayInformationProvider p = new DelayInformationProvider();
        DelayInformation delayInfo;
        String control;

        // XEP-0082 date format
        control = XMLBuilder.create("delay")
            .a("xmlns", "urn:xmpp:delay")
            .a("from", "capulet.com")
            .a("stamp", "2002-09-10T23:08:25.12Z")
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));

        GregorianCalendar cal = (GregorianCalendar) calendar.clone(); 
        cal.add(Calendar.MILLISECOND, 120);
        assertEquals(cal.getTime(), delayInfo.getStamp());

        // XEP-0082 date format without milliseconds
        control = XMLBuilder.create("delay")
            .a("xmlns", "urn:xmpp:delay")
            .a("from", "capulet.com")
            .a("stamp", "2002-09-10T23:08:25Z")
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));

        assertEquals(calendar.getTime(), delayInfo.getStamp());

        // XEP-0082 date format without milliseconds and leading 0 in month
        control = XMLBuilder.create("delay")
            .a("xmlns", "urn:xmpp:delay")
            .a("from", "capulet.com")
            .a("stamp", "2002-9-10T23:08:25Z")
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));

        assertEquals(calendar.getTime(), delayInfo.getStamp());
    }

    @Test
    public void legacyDateFormatsTest() throws FactoryConfigurationError, XmlPullParserException, IOException, Exception {
        LegacyDelayInformationProvider p = new LegacyDelayInformationProvider();
        DelayInformation delayInfo;

        String control;

        // XEP-0091 date format
        control = XMLBuilder.create("x")
            .a("xmlns", "jabber:x:delay")
            .a("from", "capulet.com")
            .a("stamp", "20020910T23:08:25")
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));

        assertEquals(calendar.getTime(), delayInfo.getStamp());

        // XEP-0091 date format without leading 0 in month
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        GregorianCalendar dateInPast = new GregorianCalendar();
        if (dateInPast.get(Calendar.MONTH) >= 10) {
            dateInPast.set(Calendar.MONTH, dateInPast.get(Calendar.MONTH) - 3);
        }
        dateInPast.add(Calendar.DAY_OF_MONTH, -3);
        dateInPast.set(Calendar.MILLISECOND, 0);

        control = XMLBuilder.create("x")
            .a("xmlns", "jabber:x:delay")
            .a("from", "capulet.com")
            .a("stamp", dateFormat.format(dateInPast.getTime()))
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));

        assertEquals(dateInPast.getTime(), delayInfo.getStamp());

        // XEP-0091 date format from SMACK-243
        control = XMLBuilder.create("x")
            .a("xmlns", "jabber:x:delay")
            .a("from", "capulet.com")
            .a("stamp", "200868T09:16:20")
            .asString(outputProperties);

        delayInfo = p.parse(PacketParserUtils.getParserFor(control));
        Date controlDate = XmppDateTime.parseDate("2008-06-08T09:16:20.0Z");

        assertEquals(controlDate, delayInfo.getStamp());
    }

    @Test
    public void validatePresenceWithDelayedDelivery() throws Exception {
        String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
                + "<delay xmlns='urn:xmpp:delay' stamp='2002-09-10T23:41:07Z'/></presence>";

        Presence presence = PacketParserUtils.parsePresence(PacketParserUtils.getParserFor(stanza));

        DelayInformation delay = DelayInformationManager.getXep203DelayInformation(presence);
        assertNotNull(delay);
        Date date = XmppDateTime.parseDate("2002-09-10T23:41:07Z");
        assertEquals(date, delay.getStamp());
    }

    @Test
    public void parsePresenceWithInvalidLegacyDelayed() throws Exception {
        String stanza = "<presence from='mercutio@example.com' to='juliet@example.com'>"
                        + "<x xmlns='jabber:x:delay'/></presence>";

        Presence presence = PacketParserUtils.parsePresence(PacketParserUtils.getParserFor(stanza));
        DelayInformation delay = DelayInformationManager.getXep203DelayInformation(presence);
        assertNull(delay);
    }
}
