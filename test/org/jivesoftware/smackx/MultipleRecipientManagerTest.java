/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
 *
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.MultipleAddresses;

import java.util.Arrays;
import java.util.List;

/**
 * Tests that JEP-33 support in Smack is correct.
 *
 * @author Gaston Dombiak
 */
public class MultipleRecipientManagerTest extends SmackTestCase {

    public MultipleRecipientManagerTest(String arg0) {
        super(arg0);
    }

    /**
     * Ensures that sending and receiving of packets is ok.
     */
    public void testSending() throws XMPPException {

        PacketCollector collector1 =
                getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector2 =
                getConnection(2).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector3 =
                getConnection(3).createPacketCollector(new MessageTypeFilter(Message.Type.normal));

        Message message = new Message();
        message.setBody("Hola");
        List<String> to = Arrays.asList(new String[]{getBareJID(1)});
        List<String> cc = Arrays.asList(new String[]{getBareJID(2)});
        List<String> bcc = Arrays.asList(new String[]{getBareJID(3)});
        MultipleRecipientManager.send(getConnection(0), message, to, cc, bcc);

        Packet message1 = collector1.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 1 never received the message", message1);
        MultipleRecipientInfo info1 = MultipleRecipientManager.getMultipleRecipientInfo(message1);
        assertNotNull("Message 1 does not contain MultipleRecipientInfo", info1);
        assertFalse("Message 1 should be 'replyable'", info1.shouldNotReply());
        List<?> addresses1 = info1.getTOAddresses();
        assertEquals("Incorrect number of TO addresses", 1, addresses1.size());
        String address1 = ((MultipleAddresses.Address) addresses1.get(0)).getJid();
        assertEquals("Incorrect TO address", getBareJID(1), address1);
        addresses1 = info1.getCCAddresses();
        assertEquals("Incorrect number of CC addresses", 1, addresses1.size());
        address1 = ((MultipleAddresses.Address) addresses1.get(0)).getJid();
        assertEquals("Incorrect CC address", getBareJID(2), address1);

        Packet message2 = collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 2 never received the message", message2);
        MultipleRecipientInfo info2 = MultipleRecipientManager.getMultipleRecipientInfo(message2);
        assertNotNull("Message 2 does not contain MultipleRecipientInfo", info2);
        assertFalse("Message 2 should be 'replyable'", info2.shouldNotReply());
        List<MultipleAddresses.Address> addresses2 = info2.getTOAddresses();
        assertEquals("Incorrect number of TO addresses", 1, addresses2.size());
        String address2 = ((MultipleAddresses.Address) addresses2.get(0)).getJid();
        assertEquals("Incorrect TO address", getBareJID(1), address2);
        addresses2 = info2.getCCAddresses();
        assertEquals("Incorrect number of CC addresses", 1, addresses2.size());
        address2 = ((MultipleAddresses.Address) addresses2.get(0)).getJid();
        assertEquals("Incorrect CC address", getBareJID(2), address2);

        Packet message3 = collector3.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 3 never received the message", message3);
        MultipleRecipientInfo info3 = MultipleRecipientManager.getMultipleRecipientInfo(message3);
        assertNotNull("Message 3 does not contain MultipleRecipientInfo", info3);
        assertFalse("Message 3 should be 'replyable'", info3.shouldNotReply());
        List<MultipleAddresses.Address> addresses3 = info3.getTOAddresses();
        assertEquals("Incorrect number of TO addresses", 1, addresses3.size());
        String address3 = ((MultipleAddresses.Address) addresses3.get(0)).getJid();
        assertEquals("Incorrect TO address", getBareJID(1), address3);
        addresses3 = info3.getCCAddresses();
        assertEquals("Incorrect number of CC addresses", 1, addresses3.size());
        address3 = ((MultipleAddresses.Address) addresses3.get(0)).getJid();
        assertEquals("Incorrect CC address", getBareJID(2), address3);

        collector1.cancel();
        collector2.cancel();
        collector3.cancel();
    }

    /**
     * Ensures that replying to packets is ok.
     */
    public void testReplying() throws XMPPException {
        PacketCollector collector0 =
                getConnection(0).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector1 =
                getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector2 =
                getConnection(2).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector3 =
                getConnection(3).createPacketCollector(new MessageTypeFilter(Message.Type.normal));

        // Send the intial message with multiple recipients
        Message message = new Message();
        message.setBody("Hola");
        List<String> to = Arrays.asList(new String[]{getBareJID(1)});
        List<String> cc = Arrays.asList(new String[]{getBareJID(2)});
        List<String> bcc = Arrays.asList(new String[]{getBareJID(3)});
        MultipleRecipientManager.send(getConnection(0), message, to, cc, bcc);

        // Get the message and ensure it's ok
        Message message1 =
                (Message) collector1.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 1 never received the message", message1);
        MultipleRecipientInfo info = MultipleRecipientManager.getMultipleRecipientInfo(message1);
        assertNotNull("Message 1 does not contain MultipleRecipientInfo", info);
        assertFalse("Message 1 should be 'replyable'", info.shouldNotReply());
        assertEquals("Incorrect number of TO addresses", 1, info.getTOAddresses().size());
        assertEquals("Incorrect number of CC addresses", 1, info.getCCAddresses().size());

        // Prepare and send the reply
        Message reply1 = new Message();
        reply1.setBody("This is my reply");
        MultipleRecipientManager.reply(getConnection(1), message1, reply1);

        // Get the reply and ensure it's ok
        reply1 = (Message) collector0.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 0 never received the reply", reply1);
        info = MultipleRecipientManager.getMultipleRecipientInfo(reply1);
        assertNotNull("Replied message does not contain MultipleRecipientInfo", info);
        assertFalse("Replied message should be 'replyable'", info.shouldNotReply());
        assertEquals("Incorrect number of TO addresses", 1, info.getTOAddresses().size());
        assertEquals("Incorrect number of CC addresses", 1, info.getCCAddresses().size());

        // Send a reply to the reply
        Message reply2 = new Message();
        reply2.setBody("This is my reply to your reply");
        reply2.setFrom(getBareJID(0));
        MultipleRecipientManager.reply(getConnection(0), reply1, reply2);

        // Get the reply and ensure it's ok
        reply2 = (Message) collector1.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 1 never received the reply", reply2);
        info = MultipleRecipientManager.getMultipleRecipientInfo(reply2);
        assertNotNull("Replied message does not contain MultipleRecipientInfo", info);
        assertFalse("Replied message should be 'replyable'", info.shouldNotReply());
        assertEquals("Incorrect number of TO addresses", 1, info.getTOAddresses().size());
        assertEquals("Incorrect number of CC addresses", 1, info.getCCAddresses().size());

        // Check that connection2 recevied 3 messages
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection2 didn't receive the 1 message", message1);
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection2 didn't receive the 2 message", message1);
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection2 didn't receive the 3 message", message1);
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNull("Connection2 received 4 messages", message1);

        // Check that connection3 recevied only 1 message (was BCC in the first message)
        message1 = (Message) collector3.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection3 didn't receive the 1 message", message1);
        message1 = (Message) collector3.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNull("Connection2 received 2 messages", message1);

        collector0.cancel();
        collector1.cancel();
        collector2.cancel();
        collector3.cancel();
    }

    /**
     * Ensures that replying is not allowed when disabled.
     */
    public void testNoReply() throws XMPPException {
        PacketCollector collector1 =
                getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector2 =
                getConnection(2).createPacketCollector(new MessageTypeFilter(Message.Type.normal));
        PacketCollector collector3 =
                getConnection(3).createPacketCollector(new MessageTypeFilter(Message.Type.normal));

        // Send the intial message with multiple recipients
        Message message = new Message();
        message.setBody("Hola");
        List<String> to = Arrays.asList(new String[]{getBareJID(1)});
        List<String> cc = Arrays.asList(new String[]{getBareJID(2)});
        List<String> bcc = Arrays.asList(new String[]{getBareJID(3)});
        MultipleRecipientManager.send(getConnection(0), message, to, cc, bcc, null, null, true);

        // Get the message and ensure it's ok
        Message message1 =
                (Message) collector1.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection 1 never received the message", message1);
        MultipleRecipientInfo info = MultipleRecipientManager.getMultipleRecipientInfo(message1);
        assertNotNull("Message 1 does not contain MultipleRecipientInfo", info);
        assertTrue("Message 1 should be not 'replyable'", info.shouldNotReply());
        assertEquals("Incorrect number of TO addresses", 1, info.getTOAddresses().size());
        assertEquals("Incorrect number of CC addresses", 1, info.getCCAddresses().size());

        // Prepare and send the reply
        Message reply1 = new Message();
        reply1.setBody("This is my reply");
        try {
            MultipleRecipientManager.reply(getConnection(1), message1, reply1);
            fail("It was possible to send a reply to a not replyable message");
        }
        catch (XMPPException e) {
            // Exception was expected since replying was not allowed
        }

        // Check that connection2 recevied 1 messages
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection2 didn't receive the 1 message", message1);
        message1 = (Message) collector2.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNull("Connection2 received 2 messages", message1);

        // Check that connection3 recevied only 1 message (was BCC in the first message)
        message1 = (Message) collector3.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNotNull("Connection3 didn't receive the 1 message", message1);
        message1 = (Message) collector3.nextResult(SmackConfiguration.getPacketReplyTimeout());
        assertNull("Connection2 received 2 messages", message1);

        collector1.cancel();
        collector2.cancel();
        collector3.cancel();
    }

    protected int getMaxConnections() {
        return 4;
    }
}
