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

import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

import junit.framework.TestCase;

public class PacketReaderTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;

    private String user1 = null;
    private String user2 = null;

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
        iqPacket.setTo(user2);
        iqPacket.setType(IQ.Type.GET);

        // Send the IQ and wait for the answer
        PacketCollector collector = conn1.createPacketCollector(
                new PacketIDFilter(iqPacket.getPacketID()));
        conn1.sendPacket(iqPacket);
        IQ response = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (response == null) {
            fail("No response from the other user.");
        }
        assertEquals("The received IQ is not of type ERROR", response.getType(),IQ.Type.ERROR);
        assertEquals("The error code is not 501", response.getError().getCode(),501);
        collector.cancel();
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("localhost");
            conn2 = new XMPPConnection("localhost");

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        // Delete the created accounts for the test
        conn1.getAccountManager().deleteAccount();
        conn2.getAccountManager().deleteAccount();

        // Close all the connections
        conn1.close();
        conn2.close();
    }
}
