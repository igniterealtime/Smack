/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.sid;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.sid.element.OriginIdElement;
import org.jivesoftware.smackx.sid.element.StanzaIdElement;
import org.jivesoftware.smackx.sid.provider.OriginIdProvider;
import org.jivesoftware.smackx.sid.provider.StanzaIdProvider;

import org.junit.Test;

public class StableUniqueStanzaIdTest extends SmackTestSuite {

    @Test
    public void stanzaIdProviderTest() throws Exception {
        String xml = "<stanza-id xmlns='urn:xmpp:sid:0' id='de305d54-75b4-431b-adb2-eb6b9e546013' by='alice@wonderland.lit' />";
        StanzaIdElement element = new StanzaIdElement("de305d54-75b4-431b-adb2-eb6b9e546013", "alice@wonderland.lit");
        assertEquals("de305d54-75b4-431b-adb2-eb6b9e546013", element.getId());
        assertEquals("alice@wonderland.lit", element.getBy());
        assertXMLEqual(xml, element.toXML(null).toString());

        StanzaIdElement parsed = StanzaIdProvider.TEST_INSTANCE.parse(TestUtils.getParser(xml));
        assertEquals(element.getId(), parsed.getId());
        assertEquals(element.getBy(), parsed.getBy());
    }

    @Test
    public void originIdProviderTest() throws Exception {
        String xml = "<origin-id xmlns='urn:xmpp:sid:0' id='de305d54-75b4-431b-adb2-eb6b9e546013' />";
        OriginIdElement element = new OriginIdElement("de305d54-75b4-431b-adb2-eb6b9e546013");
        assertEquals("de305d54-75b4-431b-adb2-eb6b9e546013", element.getId());
        assertXMLEqual(xml, element.toXML(null).toString());

        OriginIdElement parsed = OriginIdProvider.TEST_INSTANCE.parse(TestUtils.getParser(xml));
        assertEquals(element.getId(), parsed.getId());
    }

    @Test
    public void createOriginIdTest() {
        OriginIdElement element = new OriginIdElement();
        assertNotNull(element);
        assertEquals(StableUniqueStanzaIdManager.NAMESPACE, element.getNamespace());
        assertEquals(36, element.getId().length());
    }

    @Test
    public void fromMessageTest() {
        Message message = new Message();
        assertFalse(OriginIdElement.hasOriginId(message));
        assertFalse(StanzaIdElement.hasStanzaId(message));

        OriginIdElement.addOriginId(message);

        assertTrue(OriginIdElement.hasOriginId(message));

        StanzaIdElement stanzaId = new StanzaIdElement("alice@wonderland.lit");
        message.addExtension(stanzaId);
        assertTrue(StanzaIdElement.hasStanzaId(message));
        assertEquals(stanzaId, StanzaIdElement.getStanzaId(message));
    }
}
