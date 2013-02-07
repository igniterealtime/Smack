/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;

import java.util.Date;


public class PacketReaderTest extends SmackTestCase {

    private int counter;

    private final Object mutex = new Object();

    /**
     * Constructor for PacketReaderTest.
     *
     * @param arg0
     */
    public PacketReaderTest(String arg0) {
        super(arg0);
        resetCounter();
    }

    // Counter management

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    public void incCounter() {
        synchronized (mutex) {
            counter++;
        }
    }

    private int valCounter() {
        int val;
        synchronized (mutex) {
            val = counter;
        }
        return val;
    }

    /**
     * Verify that when Smack receives a "not implemented IQ" answers with an IQ packet
     * with error code 501.
     */
    public void testIQNotImplemented() {

        // Create a new type of IQ to send. The new IQ will include a
        // non-existant namespace to cause the "feature-not-implemented" answer
        IQ iqPacket = new IQ() {
            public String getChildElementXML() {
                return "<query xmlns=\"my:ns:test\"/>";
            }
        };
        iqPacket.setTo(getFullJID(1));
        iqPacket.setType(IQ.Type.GET);

        // Send the IQ and wait for the answer
        PacketCollector collector = getConnection(0).createPacketCollector(
                new PacketIDFilter(iqPacket.getPacketID()));
        getConnection(0).sendPacket(iqPacket);
        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (response == null) {
            fail("No response from the other user.");
        }
        assertEquals("The received IQ is not of type ERROR", IQ.Type.ERROR, response.getType());
        assertEquals("The error code is not 501", 501, response.getError().getCode());
        collector.cancel();
    }

    /**
     * Tests that PacketReader adds new listeners and also removes them correctly.
     */
    public void testRemoveListener() {

        PacketListener listener = new PacketListener() {
            public void processPacket(Packet packet) {
                // Do nothing
            }
        };
        // Keep number of current listeners
        int listenersSize = getConnection(0).getPacketListeners().size();
        // Add a new listener
        getConnection(0).addPacketListener(listener, new MockPacketFilter(true));
        // Check that the listener was added
        assertEquals("Listener was not added", listenersSize + 1,
                getConnection(0).getPacketListeners().size());

        Message msg = new Message(getConnection(0).getUser(), Message.Type.normal);

        getConnection(1).sendPacket(msg);

        // Remove the listener
        getConnection(0).removePacketListener(listener);
        // Check that the number of listeners is correct (i.e. the listener was removed)
        assertEquals("Listener was not removed", listenersSize,
                getConnection(0).getPacketListeners().size());
    }

    /**
     * Checks that parser still works when receiving an error text with no description.
     */
    public void testErrorWithNoText() {
        // Send a regular message from user0 to user1
        Message packet = new Message();
        packet.setFrom(getFullJID(0));
        packet.setTo(getFullJID(1));
        packet.setBody("aloha");

        // User1 will always reply to user0 when a message is received
        getConnection(1).addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                System.out.println(new Date() + " " + packet);

                Message message = new Message(packet.getFrom());
                message.setFrom(getFullJID(1));
                message.setBody("HELLO");
                getConnection(1).sendPacket(message);
            }
        }, new PacketTypeFilter(Message.class));

        // User0 listen for replies from user1
        PacketCollector collector = getConnection(0).createPacketCollector(
                new FromMatchesFilter(getFullJID(1)));
        // User0 sends the regular message to user1
        getConnection(0).sendPacket(packet);
        // Check that user0 got a reply from user1
        assertNotNull("No message was received", collector.nextResult(1000));

        // Send a message with an empty error text
        packet = new Message();
        packet.setFrom(getFullJID(0));
        packet.setTo(getFullJID(1));
        packet.setBody("aloha");
        packet.setError(new XMPPError(XMPPError.Condition.feature_not_implemented, null));
        getConnection(0).sendPacket(packet);
        // Check that user0 got a reply from user1
        assertNotNull("No message was received", collector.nextResult(1000));
    }

    /**
     * Tests that PacketReader adds new listeners and also removes them correctly.
     */
    public void testFiltersRemotion() {

        resetCounter();

        int repeat = 10;

        for (int j = 0; j < repeat; j++) {

            PacketListener listener0 = new PacketListener() {
                public void processPacket(Packet packet) {
                    System.out.println("Packet Captured");
                    incCounter();
                }
            };
            PacketFilter pf0 = new PacketFilter() {
                public boolean accept(Packet packet) {
                    System.out.println("Packet Filtered");
                    incCounter();
                    return true;
                }
            };

            PacketListener listener1 = new PacketListener() {
                public void processPacket(Packet packet) {
                    System.out.println("Packet Captured");
                    incCounter();
                }
            };
            PacketFilter pf1 = new PacketFilter() {
                public boolean accept(Packet packet) {
                    System.out.println("Packet Filtered");
                    incCounter();
                    return true;
                }
            };

            getConnection(0).addPacketListener(listener0, pf0);
            getConnection(1).addPacketListener(listener1, pf1);

            // Check that the listener was added

            Message msg0 = new Message(getConnection(0).getUser(), Message.Type.normal);
            Message msg1 = new Message(getConnection(1).getUser(), Message.Type.normal);


            for (int i = 0; i < 5; i++) {
                getConnection(1).sendPacket(msg0);
                getConnection(0).sendPacket(msg1);
            }

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Remove the listener
            getConnection(0).removePacketListener(listener0);
            getConnection(1).removePacketListener(listener1);

            try {
                Thread.sleep(300);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 10; i++) {
                getConnection(0).sendPacket(msg1);
                getConnection(1).sendPacket(msg0);
            }

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(valCounter());
        assertEquals(valCounter(), repeat * 2 * 10);
    }

    protected int getMaxConnections() {
        return 2;
    }
}
