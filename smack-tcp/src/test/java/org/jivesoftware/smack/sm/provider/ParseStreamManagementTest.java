/**
 *
 * Copyright 2014 Vyacheslav Blinov, 2017-2018 Florian Schmaus
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
package org.jivesoftware.smack.sm.provider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaErrorTextElement;
import org.jivesoftware.smack.sm.packet.StreamManagement;
import org.jivesoftware.smack.util.PacketParserUtils;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ParseStreamManagementTest {
    private static final Properties outputProperties = initOutputProperties();

    @Test
    public void testParseEnabled() throws Exception {
        String stanzaID = "zid615d9";
        boolean resume = true;
        String location = "test";
        int max = 42;

        String enabledStanza = XMLBuilder.create("enabled")
                .a("xmlns", "urn:xmpp:sm:3")
                .a("id", "zid615d9")
                .a("resume", String.valueOf(resume))
                .a("location", location)
                .a("max", String.valueOf(max))
                .asString(outputProperties);

        StreamManagement.Enabled enabledPacket = ParseStreamManagement.enabled(
                PacketParserUtils.getParserFor(enabledStanza));

        assertThat(enabledPacket, is(notNullValue()));
        assertThat(enabledPacket.getId(), equalTo(stanzaID));
        assertThat(enabledPacket.getLocation(), equalTo(location));
        assertThat(enabledPacket.isResumeSet(), equalTo(resume));
        assertThat(enabledPacket.getMaxResumptionTime(), equalTo(max));
    }


    @Test
    public void testParseEnabledInvariant() throws XmlPullParserException, IOException {
        String enabledString = new StreamManagement.Enabled("stream-id", false).toXML(null).toString();
        XmlPullParser parser = PacketParserUtils.getParserFor(enabledString);
        StreamManagement.Enabled enabled = ParseStreamManagement.enabled(parser);

        assertEquals(enabledString, enabled.toXML(null).toString());
    }

    @Test
    public void testParseFailed() throws Exception {
        String failedStanza = XMLBuilder.create("failed")
                .a("xmlns", "urn:xmpp:sm:3")
                .asString(outputProperties);

        StreamManagement.Failed failedPacket = ParseStreamManagement.failed(
                PacketParserUtils.getParserFor(failedStanza));

        assertThat(failedPacket, is(notNullValue()));
        assertTrue(failedPacket.getStanzaErrorCondition() == null);
    }

    @Test
    public void testParseFailedError() throws Exception {
        StanzaError.Condition errorCondition = StanzaError.Condition.unexpected_request;

        String failedStanza = XMLBuilder.create("failed")
                .a("xmlns", "urn:xmpp:sm:3")
                .element(errorCondition.toString(), StanzaError.ERROR_CONDITION_AND_TEXT_NAMESPACE)
                .asString(outputProperties);

        StreamManagement.Failed failedPacket = ParseStreamManagement.failed(
                PacketParserUtils.getParserFor(failedStanza));

        assertThat(failedPacket, is(notNullValue()));
        assertTrue(failedPacket.getStanzaErrorCondition() == errorCondition);
    }

    @Test
    public void testParseFailedWithTExt() throws XmlPullParserException, IOException {
        // @formatter:off
        final String failedNonza = "<failed h='20' xmlns='urn:xmpp:sm:3'>"
                                   +  "<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                                   +  "<text xml:lang='en' xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'>"
                                   +    "Previous session timed out"
                                   +  "</text>"
                                   + "</failed>";
        // @formatter:on
        XmlPullParser parser = PacketParserUtils.getParserFor(failedNonza);

        StreamManagement.Failed failed = ParseStreamManagement.failed(parser);

        assertEquals(StanzaError.Condition.item_not_found, failed.getStanzaErrorCondition());

        List<StanzaErrorTextElement> textElements = failed.getTextElements();
        assertEquals(1, textElements.size());

        StanzaErrorTextElement textElement = textElements.get(0);
        assertEquals("Previous session timed out", textElement.getText());
        assertEquals("en", textElement.getLang());
    }

    @Test
    public void testParseResumed() throws Exception {
        long handledPackets = 42;
        String previousID = "zid615d9";

        String resumedStanza = XMLBuilder.create("resumed")
                .a("xmlns", "urn:xmpp:sm:3")
                .a("h", String.valueOf(handledPackets))
                .a("previd", previousID)
                .asString(outputProperties);

        StreamManagement.Resumed resumedPacket = ParseStreamManagement.resumed(
                PacketParserUtils.getParserFor(resumedStanza));

        assertThat(resumedPacket, is(notNullValue()));
        assertThat(resumedPacket.getHandledCount(), equalTo(handledPackets));
        assertThat(resumedPacket.getPrevId(), equalTo(previousID));
    }

    @Test
    public void testParseAckAnswer() throws Exception {
        long handledPackets = 42 + 42;

        String ackStanza = XMLBuilder.create("a")
                .a("xmlns", "urn:xmpp:sm:3")
                .a("h", String.valueOf(handledPackets))
                .asString(outputProperties);

        StreamManagement.AckAnswer acknowledgementPacket = ParseStreamManagement.ackAnswer(
                PacketParserUtils.getParserFor(ackStanza));

        assertThat(acknowledgementPacket, is(notNullValue()));
        assertThat(acknowledgementPacket.getHandledCount(), equalTo(handledPackets));
    }

    private static Properties initOutputProperties() {
        Properties properties = new Properties();
        properties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        return properties;
    }
}
