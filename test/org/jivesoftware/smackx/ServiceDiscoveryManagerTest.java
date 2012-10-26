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

import java.util.Iterator;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;


/**
 * Tests the service discovery functionality.
 * 
 * @author Gaston Dombiak
 */
public class ServiceDiscoveryManagerTest extends SmackTestCase {

    public ServiceDiscoveryManagerTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests info discovery of a Smack client. 
     */
    public void testSmackInfo() {

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager
                .getInstanceFor(getConnection(0));
        try {
            // Discover the information of another Smack client
            DiscoverInfo info = discoManager.discoverInfo(getFullJID(1));
            // Check the identity of the Smack client
            Iterator<Identity> identities = info.getIdentities();
            assertTrue("No identities were found", identities.hasNext());
            Identity identity = identities.next();
            assertEquals("Name in identity is wrong", ServiceDiscoveryManager.getIdentityName(),
                    identity.getName());
            assertEquals("Category in identity is wrong", "client", identity.getCategory());
            assertEquals("Type in identity is wrong", ServiceDiscoveryManager.getIdentityType(),
                    identity.getType());
            assertFalse("More identities were found", identities.hasNext());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Tests that ensures that Smack answers a 404 error when the disco#info includes a node.
     */
    public void testInfoWithNode() {

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager
                .getInstanceFor(getConnection(0));
        try {
            // Discover the information of another Smack client
            discoManager.discoverInfo(getFullJID(1), "some node");
            // Check the identity of the Smack client
            fail("Unexpected identities were returned instead of a 404 error");
        }
        catch (XMPPException e) {
            assertEquals("Incorrect error", 404, e.getXMPPError().getCode());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Tests service discovery of XHTML support. 
     */
    public void testXHTMLFeature() {
        // Check for local XHTML service support
        // By default the XHTML service support is enabled in all the connections
        assertTrue(XHTMLManager.isServiceEnabled(getConnection(0)));
        assertTrue(XHTMLManager.isServiceEnabled(getConnection(1)));
        // Check for XHTML support in connection1 from connection2
        // Must specify a full JID and not a bare JID. Ensure that the server is working ok. 
        assertFalse(XHTMLManager.isServiceEnabled(getConnection(1), getBareJID(0)));
        // Using a full JID check that the other client supports XHTML. 
        assertTrue(XHTMLManager.isServiceEnabled(getConnection(1), getFullJID(0)));

        // Disable the XHTML Message support in connection1
        XHTMLManager.setServiceEnabled(getConnection(0), false);
        // Check for local XHTML service support 
        assertFalse(XHTMLManager.isServiceEnabled(getConnection(0)));
        assertTrue(XHTMLManager.isServiceEnabled(getConnection(1)));
        // Check for XHTML support in connection1 from connection2 
        assertFalse(XHTMLManager.isServiceEnabled(getConnection(1), getFullJID(0)));
    }

    /**
     * Tests support for publishing items to another entity.
     */
    public void testDiscoverPublishItemsSupport() {
        try {
            boolean canPublish = ServiceDiscoveryManager.getInstanceFor(getConnection(0))
                    .canPublishItems(getServiceName());
            assertFalse("Wildfire does not support publishing...so far!!", canPublish);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Tests publishing items to another entity.
     */
    /*public void testPublishItems() {
        DiscoverItems itemsToPublish = new DiscoverItems();
        DiscoverItems.Item itemToPublish = new DiscoverItems.Item("pubsub.shakespeare.lit");
        itemToPublish.setName("Avatar");
        itemToPublish.setNode("romeo/avatar");
        itemToPublish.setAction(DiscoverItems.Item.UPDATE_ACTION);
        itemsToPublish.addItem(itemToPublish);
        
        try {
            ServiceDiscoveryManager.getInstanceFor(getConnection(0)).publishItems(getServiceName(),
                    itemsToPublish);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
        
    }*/

    protected int getMaxConnections() {
        return 2;
    }
}
