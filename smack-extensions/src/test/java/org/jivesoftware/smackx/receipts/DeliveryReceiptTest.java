/*
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
package org.jivesoftware.smackx.receipts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.jivesoftware.smack.test.util.CharSequenceEquals.equalsCharSequence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.test.util.ElementParserUtils;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.WaitForPacketListener;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.receipts.DeliveryReceiptManager.AutoReceiptMode;

import com.jamesmurty.utils.XMLBuilder;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.Jid;

public class DeliveryReceiptTest extends SmackTestSuite {

    private static Properties outputProperties = new Properties();
    static {
        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void receiptTest() throws Exception {
        XmlPullParser parser;
        String control;

        control = XMLBuilder.create("message")
            .a("from", "romeo@montague.com")
            .e("request")
                .a("xmlns", "urn:xmpp:receipts")
            .asString(outputProperties);

        parser = PacketParserUtils.getParserFor(control);
        Message p = ElementParserUtils.parseMessage(parser);

        DeliveryReceiptRequest drr = p.getExtension(DeliveryReceiptRequest.class);
        assertNotNull(drr);

        assertTrue(DeliveryReceiptManager.hasDeliveryReceiptRequest(p));

        MessageBuilder messageBuilder = StanzaBuilder.buildMessage("request-id")
                .to("romeo@montague.com")
                .ofType(Message.Type.normal)
                ;
        assertFalse(DeliveryReceiptManager.hasDeliveryReceiptRequest(messageBuilder.build()));
        DeliveryReceiptRequest.addTo(messageBuilder);
        assertTrue(DeliveryReceiptManager.hasDeliveryReceiptRequest(messageBuilder.build()));
    }

    @Test
    public void receiptManagerListenerTest() throws Exception {
        DummyConnection c = new DummyConnection();
        c.connect();
        DeliveryReceiptManager drm = DeliveryReceiptManager.getInstanceFor(c);

        TestReceiptReceivedListener rrl = new TestReceiptReceivedListener();
        drm.addReceiptReceivedListener(rrl);

        Message m = StanzaBuilder.buildMessage("reply-id")
                .from("julia@capulet.com")
                .to("romeo@montague.com")
                .ofType(Message.Type.normal)
                .addExtension(new DeliveryReceipt("original-test-id"))
                .build();
        c.processStanza(m);

        rrl.waitUntilInvocationOrTimeout();
    }

    private static final class TestReceiptReceivedListener extends WaitForPacketListener implements ReceiptReceivedListener {
        @Override
        public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
            assertThat("julia@capulet.com", equalsCharSequence(fromJid));
            assertThat("romeo@montague.com", equalsCharSequence(toJid));
            assertEquals("original-test-id", receiptId);
            reportInvoked();
        }
    }

    @Test
    public void receiptManagerAutoReplyTest() throws Exception {
        DummyConnection c = new DummyConnection();
        c.connect();
        DeliveryReceiptManager drm = DeliveryReceiptManager.getInstanceFor(c);

        drm.setAutoReceiptMode(AutoReceiptMode.always);
        assertEquals(AutoReceiptMode.always, drm.getAutoReceiptMode());

        // test auto-receipts
        MessageBuilder messageBuilder = StanzaBuilder.buildMessage("test-receipt-request")
                .to("julia@capulet.com")
                .from("romeo@montague.com")
                .ofType(Message.Type.normal)
                ;
        DeliveryReceiptRequest.addTo(messageBuilder);

        // the DRM will send a reply-packet
        c.processStanza(messageBuilder.build());

        Stanza reply = c.getSentPacket();
        DeliveryReceipt r = DeliveryReceipt.from((Message) reply);
        assertThat("romeo@montague.com", equalsCharSequence(reply.getTo()));
        assertEquals("test-receipt-request", r.getId());
    }
}
