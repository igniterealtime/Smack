/**
 *
 * Copyright 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.amp;

import org.jivesoftware.smack.packet.PacketExtension;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AMPExtensionTest {

    private InputStream CORRECT_SENDING_STANZA_STREAM;
    private InputStream INCORRECT_RECEIVING_STANZA_STREAM;

    @Before
    public void setUp() throws IOException {
        CORRECT_SENDING_STANZA_STREAM = getClass().getResourceAsStream("correct_stanza_test.xml");
        INCORRECT_RECEIVING_STANZA_STREAM = getClass().getResourceAsStream("incorrect_stanza_test.xml");
    }

    @Test
    public void isCorrectToXmlTransform() throws IOException {
        String correctStanza = toString(CORRECT_SENDING_STANZA_STREAM);

        AMPExtension ext = new AMPExtension();
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.alert, new AMPDeliverCondition(AMPDeliverCondition.Value.direct)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.drop, new AMPDeliverCondition(AMPDeliverCondition.Value.forward)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.error, new AMPDeliverCondition(AMPDeliverCondition.Value.gateway)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPDeliverCondition(AMPDeliverCondition.Value.none)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPDeliverCondition(AMPDeliverCondition.Value.stored)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPExpireAtCondition("2004-09-10T08:33:14Z")));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPMatchResourceCondition(AMPMatchResourceCondition.Value.any)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPMatchResourceCondition(AMPMatchResourceCondition.Value.exact)));
        ext.addRule(new AMPExtension.Rule(AMPExtension.Action.notify, new AMPMatchResourceCondition(AMPMatchResourceCondition.Value.other)));

        assertEquals(correctStanza, ext.toXML());
    }

    @Test
    public void isCorrectFromXmlErrorHandling() throws Exception {
        AMPExtensionProvider ampProvider = new AMPExtensionProvider();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(INCORRECT_RECEIVING_STANZA_STREAM, "UTF-8");

        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(AMPExtension.ELEMENT, parser.getName());

        PacketExtension extension = ampProvider.parseExtension(parser);
        assertTrue(extension instanceof AMPExtension);
        AMPExtension amp = (AMPExtension) extension;

        assertEquals(0, amp.getRulesCount());
        assertEquals(AMPExtension.Status.alert, amp.getStatus());
        assertEquals("bernardo@hamlet.lit/elsinore", amp.getFrom());
        assertEquals("francisco@hamlet.lit", amp.getTo());
    }

    @Test
    public void isCorrectFromXmlDeserialization() throws Exception {
        AMPExtensionProvider ampProvider = new AMPExtensionProvider();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(CORRECT_SENDING_STANZA_STREAM, "UTF-8");

        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(AMPExtension.ELEMENT, parser.getName());
        PacketExtension extension = ampProvider.parseExtension(parser);
        assertTrue(extension instanceof AMPExtension);
        AMPExtension amp = (AMPExtension) extension;

        assertEquals(9, amp.getRulesCount());
    }


    private String toString(InputStream stream) throws IOException {
        byte[] data = new byte[stream.available()];
        stream.read(data);
        stream.close();

        return new String(data, Charset.defaultCharset());
    }
}
