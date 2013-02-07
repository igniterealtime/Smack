/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
