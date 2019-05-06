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
package org.jivesoftware.smackx.forward;

import static org.jivesoftware.smack.test.util.CharSequenceEquals.equalsCharSequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.Test;

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

        parser = PacketParserUtils.getParserFor(control);
        fwd = new ForwardedProvider().parse(parser);

        // no delay in packet
        assertEquals(null, fwd.getDelayInformation());

        // check message
        assertThat("romeo@montague.com", equalsCharSequence(fwd.getForwardedStanza().getFrom()));

        // check end of tag
        assertEquals(XmlPullParser.Event.END_ELEMENT, parser.getEventType());
        assertEquals("forwarded", parser.getName());
    }

    @Test
    public void forwardedWithDelayTest() throws Exception {
        XmlPullParser parser;
        String control;
        Forwarded fwd;

        // @formatter:off
        control = XMLBuilder.create("forwarded").a("xmlns", "urn:xmpp:forwarded:0")
                        .e("message").a("from", "romeo@montague.com").up()
                        .e("delay").ns(DelayInformation.NAMESPACE).a("stamp", "2010-07-10T23:08:25Z")
                  .asString(outputProperties);
        // @formatter:on

        parser = PacketParserUtils.getParserFor(control);
        fwd = new ForwardedProvider().parse(parser);

        // assert there is delay information in packet
        DelayInformation delay = fwd.getDelayInformation();
        assertNotNull(delay);

        // check message
        assertThat("romeo@montague.com", equalsCharSequence(fwd.getForwardedStanza().getFrom()));

        // check end of tag
        assertEquals(XmlPullParser.Event.END_ELEMENT, parser.getEventType());
        assertEquals("forwarded", parser.getName());
    }

    @Test(expected = Exception.class)
    public void forwardedEmptyTest() throws Exception {
        XmlPullParser parser;
        String control;

        control = XMLBuilder.create("forwarded")
            .a("xmlns", "urn:xmpp:forwarded:0")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        new ForwardedProvider().parse(parser);
    }
}
