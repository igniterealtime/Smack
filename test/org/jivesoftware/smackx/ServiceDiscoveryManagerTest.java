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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.DiscoverItems;

import junit.framework.TestCase;

/**
 * Tests the service discovery functionality.
 * 
 * @author Gaston Dombiak
 */
public class ServiceDiscoveryManagerTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;

    /**
     * Constructor for ServiceDiscoveryManagerTest.
     * @param arg0
     */
    public ServiceDiscoveryManagerTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests service discovery of XHTML support. 
     */
    public void testXHTMLFeature() {
        // TODO Remove these two lines when the "additional services for extensions" are 
        // implemented 
        new ServiceDiscoveryManager(conn1);
        new ServiceDiscoveryManager(conn2);

        // Enable the XHTML Message support in connection1
        XHTMLManager.setServiceEnabled(conn1, true);
        // Check for local XHTML service support 
        assertTrue(XHTMLManager.isServiceEnabled(conn1));
        assertFalse(XHTMLManager.isServiceEnabled(conn2));
        // Check for XHTML support in connection1 from connection2 
        assertTrue(XHTMLManager.isServiceEnabled(conn2, "gato10@" + conn1.getHost()));
        
        // Disable the XHTML Message support in connection1
        XHTMLManager.setServiceEnabled(conn1, false);
        // Check for local XHTML service support 
        assertFalse(XHTMLManager.isServiceEnabled(conn1));
        assertFalse(XHTMLManager.isServiceEnabled(conn2));
        // Check for XHTML support in connection1 from connection2 
        assertFalse(XHTMLManager.isServiceEnabled(conn2, "gato10@" + conn1.getHost()));
    }

    /**
     * Tests publishing items to another entity. 
     */
    /*public void testPublishItems() {
        // TODO Remove this line when the "additional services for extensions" are 
        // implemented 
        new ServiceDiscoveryManager(conn1);

        DiscoverItems itemsToPublish = new DiscoverItems();
        DiscoverItems.Item itemToPublish = new DiscoverItems.Item("pubsub.shakespeare.lit");
        itemToPublish.setName("Avatar");
        itemToPublish.setNode("romeo/avatar");
        itemToPublish.setAction(DiscoverItems.Item.UPDATE_ACTION);
        itemsToPublish.addItem(itemToPublish);
        
        try {
            ServiceDiscoveryManager.getInstanceFor(conn1).publishItems("host", itemsToPublish);
        }
        catch (XMPPException e) {
            fail(e.getMessage());
        }
        
    }*/

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
            conn1.getAccountManager().createAccount("gato10", "gato10");
            conn2.getAccountManager().createAccount("gato11", "gato11");

            // Login with the test accounts
            conn1.login("gato10", "gato10");
            conn2.login("gato11", "gato11");

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
