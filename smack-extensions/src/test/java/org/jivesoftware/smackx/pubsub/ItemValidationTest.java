/**
 *
 * Copyright 2011 Robin Collier
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
package org.jivesoftware.smackx.pubsub;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Item validation test.
 * @author Robin Collier
 *
 */
public class ItemValidationTest extends SmackTestSuite {
    private ThreadedDummyConnection connection;

    @BeforeEach
    public void setUp() throws Exception {
        // Uncomment this to enable debug output
        // SmackConfiguration.DEBUG = true;

        connection = new ThreadedDummyConnection();
        connection.connect();
        connection.login();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null)
            connection.disconnect();
    }

    @Test
    public void verifyBasicItem() throws Exception {
        Item simpleItem = new Item();
        String simpleCtrl = "<item xmlns='http://jabber.org/protocol/pubsub' />";
        assertXmlSimilar(simpleCtrl, simpleItem.toXML().toString());

        Item idItem = new Item("uniqueid");
        String idCtrl = "<item xmlns='http://jabber.org/protocol/pubsub' id='uniqueid'/>";
        assertXmlSimilar(idCtrl, idItem.toXML().toString());

        Item itemWithNodeId = new Item("testId", "testNode");
        String nodeIdCtrl = "<item xmlns='http://jabber.org/protocol/pubsub' id='testId' node='testNode' />";
        assertXmlSimilar(nodeIdCtrl, itemWithNodeId.toXML().toString());
    }

    @Test
    public void verifyPayloadItem() throws Exception {
        SimplePayload payload = new SimplePayload("<data xmlns='https://example.org'>This is the payload</data>");

        PayloadItem<SimplePayload> simpleItem = new PayloadItem<>(payload);
        String simpleCtrl = "<item xmlns='http://jabber.org/protocol/pubsub'>" + payload.toXML() + "</item>";
        assertXmlSimilar(simpleCtrl, simpleItem.toXML().toString());

        PayloadItem<SimplePayload> idItem = new PayloadItem<>("uniqueid", payload);
        String idCtrl = "<item xmlns='http://jabber.org/protocol/pubsub' id='uniqueid'>" + payload.toXML() + "</item>";
        assertXmlSimilar(idCtrl, idItem.toXML().toString());

        PayloadItem<SimplePayload> itemWithNodeId = new PayloadItem<>("testId", "testNode", payload);
        String nodeIdCtrl = "<item xmlns='http://jabber.org/protocol/pubsub' id='testId' node='testNode'>" + payload.toXML() + "</item>";
        assertXmlSimilar(nodeIdCtrl, itemWithNodeId.toXML().toString());
    }

    @Test
    public void parseBasicItem() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(
            "<message from='pubsub.myserver.com' to='francisco@denmark.lit' id='foo'>" +
                "<event xmlns='http://jabber.org/protocol/pubsub#event'>" +
                    "<items node='testNode'>" +
                        "<item id='testid1' />" +
                    "</items>" +
                 "</event>" +
            "</message>");

        Stanza message = PacketParserUtils.parseMessage(parser);
        XmlElement eventExt = message.getExtension(PubSubNamespace.event.getXmlns());

        assertTrue(eventExt instanceof EventElement);
        EventElement event = (EventElement) eventExt;
        assertEquals(EventElementType.items, event.getEventType());
        assertEquals(1, event.getExtensions().size());
        assertTrue(event.getExtensions().get(0) instanceof ItemsExtension);
        assertEquals(1, ((ItemsExtension) event.getExtensions().get(0)).items.size());

        NamedElement itemExt = ((ItemsExtension) event.getExtensions().get(0)).items.get(0);
        assertTrue(itemExt instanceof Item);
        assertEquals("testid1", ((Item) itemExt).getId());
    }

    @Test
    public void parseSimplePayloadItem() throws Exception {
        String itemContent = "<foo xmlns='smack:test'>Some text</foo>";

        XmlPullParser parser = PacketParserUtils.getParserFor(
            "<message from='pubsub.myserver.com' to='francisco@denmark.lit' id='foo'>" +
                "<event xmlns='http://jabber.org/protocol/pubsub#event'>" +
                    "<items node='testNode'>" +
                        "<item id='testid1' >" +
                            itemContent +
                        "</item>" +
                    "</items>" +
                 "</event>" +
            "</message>");

        Stanza message = PacketParserUtils.parseMessage(parser);
        XmlElement eventExt = message.getExtension(PubSubNamespace.event.getXmlns());
        EventElement event = (EventElement) eventExt;
        NamedElement itemExt = ((ItemsExtension) event.getExtensions().get(0)).items.get(0);

        assertTrue(itemExt instanceof PayloadItem<?>);
        PayloadItem<?> item = (PayloadItem<?>) itemExt;

        assertEquals("testid1", item.getId());
        assertTrue(item.getPayload() instanceof SimplePayload);

        SimplePayload payload = (SimplePayload) item.getPayload();
        assertEquals("foo", payload.getElementName());
        assertEquals("smack:test", payload.getNamespace());
        assertXmlSimilar(itemContent, payload.toXML().toString());
    }

    @Test
    public void parseComplexItem() throws Exception {
        String itemContent =
                "<entry xmlns='http://www.w3.org/2005/Atom'>" +
                    "<title>Soliloquy</title>" +
                    "<summary>" +
                        "To be, or not to be: that is the question:" +
                        "Whether 'tis nobler in the mind to suffer" +
                        "The slings and arrows of outrageous fortune," +
                        "Or to take arms against a sea of troubles," +
                        "And by opposing end them?" +
                    "</summary>" +
                    "<link rel='alternate' type='text/html' href='http://denmark.lit/2003/12/13/atom03'/>" +
                    "<id>tag:denmark.lit,2003:entry-32397</id>" +
                    "<published>2003-12-13T18:30:02Z</published>" +
                    "<updated>2003-12-13T18:30:02Z</updated>" +
                "</entry>";

        XmlPullParser parser = PacketParserUtils.getParserFor(
            "<message from='pubsub.myserver.com' to='francisco@denmark.lit' id='foo'>" +
                "<event xmlns='http://jabber.org/protocol/pubsub#event'>" +
                    "<items node='testNode'>" +
                        "<item id='testid1' >" +
                            itemContent +
                        "</item>" +
                    "</items>" +
                 "</event>" +
            "</message>");

        Stanza message = PacketParserUtils.parseMessage(parser);
        XmlElement eventExt = message.getExtension(PubSubNamespace.event.getXmlns());
        EventElement event = (EventElement) eventExt;
        NamedElement itemExt = ((ItemsExtension) event.getExtensions().get(0)).items.get(0);

        assertTrue(itemExt instanceof PayloadItem<?>);
        PayloadItem<?> item = (PayloadItem<?>) itemExt;

        assertEquals("testid1", item.getId());
        assertTrue(item.getPayload() instanceof SimplePayload);

        SimplePayload payload = (SimplePayload) item.getPayload();
        assertEquals("entry", payload.getElementName());
        assertEquals("http://www.w3.org/2005/Atom", payload.getNamespace());
        assertXmlSimilar(itemContent, payload.toXML().toString());
    }

    @Test
    public void parseEmptyTag() throws Exception {
        String itemContent = "<foo xmlns='smack:test'><bar/></foo>";

        XmlPullParser parser = PacketParserUtils.getParserFor(
            "<message from='pubsub.myserver.com' to='francisco@denmark.lit' id='foo'>" +
                "<event xmlns='http://jabber.org/protocol/pubsub#event'>" +
                    "<items node='testNode'>" +
                        "<item id='testid1' >" +
                            itemContent +
                        "</item>" +
                    "</items>" +
                 "</event>" +
            "</message>");

        Stanza message = PacketParserUtils.parseMessage(parser);
        XmlElement eventExt = message.getExtension(PubSubNamespace.event.getXmlns());

        assertTrue(eventExt instanceof EventElement);
        EventElement event = (EventElement) eventExt;
        assertEquals(EventElementType.items, event.getEventType());
        assertEquals(1, event.getExtensions().size());
        assertTrue(event.getExtensions().get(0) instanceof ItemsExtension);
        assertEquals(1, ((ItemsExtension) event.getExtensions().get(0)).items.size());

        NamedElement itemExt = ((ItemsExtension) event.getExtensions().get(0)).items.get(0);
        assertTrue(itemExt instanceof PayloadItem<?>);
        PayloadItem<?> item = (PayloadItem<?>) itemExt;

        assertEquals("testid1", item.getId());
        assertTrue(item.getPayload() instanceof SimplePayload);

        assertXmlSimilar(itemContent, item.getPayload().toXML().toString());
    }
}
