/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;

import java.util.Date;


public class PacketReaderTest extends SmackTestCase {

    /**
     * Constructor for PacketReaderTest.
     * @param arg0
     */
    public PacketReaderTest(String arg0) {
        super(arg0);
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
        IQ response = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
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
                //Ignore
            }
        };
        // Keep number of current listeners
        int listenersSize = getConnection(0).packetReader.listeners.size();
        // Add a new listener
        getConnection(0).addPacketListener(listener, new MockPacketFilter(true));
        // Check that the listener was added
        assertEquals("Listener was not added", listenersSize + 1,
                getConnection(0).packetReader.listeners.size());
        // Remove the listener
        getConnection(0).removePacketListener(listener);
        // Check that the number of listeners is correct (i.e. the listener was removed)
        assertEquals("Listener was not removed", listenersSize,
                getConnection(0).packetReader.listeners.size());
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

    protected int getMaxConnections() {
        return 2;
    }
}
