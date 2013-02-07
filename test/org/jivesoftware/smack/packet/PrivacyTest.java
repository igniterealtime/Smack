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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListListener;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PrivacyItem.PrivacyRule;
import org.jivesoftware.smack.test.SmackTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrivacyTest extends SmackTestCase {

    public PrivacyTest(String arg0) {
        super(arg0);
    }
   
    
    /**
     * Check when a client set a new active list.
     */
    public void testCreateActiveList() {
        try {
            String listName = "testCreateActiveList";
            
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
            PrivacyClient client = new PrivacyClient(privacyManager);
            privacyManager.addListener(client);
            
            // Add the list that will be set as the active
            ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
            PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName, items);
            
            Thread.sleep(500);
            
            // Set the active list
            privacyManager.setActiveListName(listName);
            
            Thread.sleep(500);
                        
            // Assert the list composition.
            assertEquals(listName, privacyManager.getActiveList().toString());
            List<PrivacyItem> privacyItems = privacyManager.getPrivacyList(listName).getItems();
            assertEquals(1, privacyItems.size());

            // Assert the privacy item composition
            PrivacyItem receivedItem = privacyItems.get(0);
            assertEquals(1, receivedItem.getOrder());
            assertEquals(PrivacyItem.Type.jid, receivedItem.getType());
            assertEquals(true, receivedItem.isAllow());
            
            privacyManager.deletePrivacyList(listName);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    }

    /**
     * Check when a client set more than one list.
     */
    public void testCreateTwoLists() {
        try {
            String listName1 = "1testCreateTwoLists";
            String listName2 = "2testCreateTwoLists";
            String groupName = "testCreateTwoListsGroup";
            
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
            PrivacyClient client = new PrivacyClient(privacyManager);
            privacyManager.addListener(client);
            
            // Add a list
            ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
            PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName1, items);
            
            Thread.sleep(500);
            
            // Add the another list
            ArrayList<PrivacyItem> itemsList2 = new ArrayList<PrivacyItem>();
            item = new PrivacyItem(PrivacyItem.Type.group.name(), false, 2);
            item.setValue(groupName);
            item.setFilterMessage(true);
            itemsList2.add(item);
            privacyManager.createPrivacyList(listName2, itemsList2);
            
            Thread.sleep(500);
                        
            // Assert the list composition.
            PrivacyList[] privacyLists = privacyManager.getPrivacyLists();
            PrivacyList receivedList1 = null;
            PrivacyList receivedList2 = null;
            for (PrivacyList privacyList : privacyLists) {
                if (listName1.equals(privacyList.toString())) {
                    receivedList1 = privacyList;
                }
                if (listName2.equals(privacyList.toString())) {
                    receivedList2 = privacyList;
                }
            }
            
            
            PrivacyItem receivedItem;
            // Assert on the list 1
            assertNotNull(receivedList1);
            assertEquals(1, receivedList1.getItems().size());
            receivedItem = receivedList1.getItems().get(0);
            assertEquals(1, receivedItem.getOrder());
            assertEquals(PrivacyItem.Type.jid, receivedItem.getType());
            assertEquals(true, receivedItem.isAllow());
            assertEquals(getConnection(0).getUser(), receivedItem.getValue());
            
            // Assert on the list 2
            assertNotNull(receivedList2);
            assertEquals(1, receivedList2.getItems().size());
            receivedItem = receivedList2.getItems().get(0);
            assertEquals(2, receivedItem.getOrder());
            assertEquals(PrivacyItem.Type.group, receivedItem.getType());
            assertEquals(groupName, receivedItem.getValue());
            assertEquals(false, receivedItem.isAllow());
            assertEquals(groupName, receivedItem.getValue());
            assertEquals(false, receivedItem.isFilterEverything());
            assertEquals(true, receivedItem.isFilterMessage());
            assertEquals(false, receivedItem.isFilterPresence_in());
            assertEquals(false, receivedItem.isFilterPresence_out());
            
            privacyManager.deletePrivacyList(listName1);
            privacyManager.deletePrivacyList(listName2);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    }
    
    /**
     * Check when a client set a new list and then update its content.
     */
    public void testCreateAndUpdateList() {
        try {
            String listName = "testCreateAndUpdateList";
            String user = "tybalt@example.com";
            
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
            PrivacyClient client = new PrivacyClient(privacyManager);
            privacyManager.addListener(client);
            
            // Add the list that will be set as the active
            ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
            PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName, items);
            
            Thread.sleep(500);
            
            // Remove the existing item and add a new one.
            items.remove(item);
            item = new PrivacyItem(PrivacyItem.Type.jid.name(), false, 2);
            item.setValue(user);
            item.setFilterPresence_out(true);
            item.setFilterPresence_in(true);
            item.setFilterMessage(true);
            items.add(item);
            
            // Update the list on server
            privacyManager.updatePrivacyList(listName, items);
            
            Thread.sleep(500);
                        
            // Assert the list composition.
            PrivacyList list = privacyManager.getPrivacyList(listName);
            assertEquals(1, list.getItems().size());

            // Assert the privacy item composition
            PrivacyItem receivedItem = list.getItems().get(0);
            assertEquals(2, receivedItem.getOrder());
            assertEquals(PrivacyItem.Type.jid, receivedItem.getType());
            assertEquals(false, receivedItem.isAllow());
            assertEquals(user, receivedItem.getValue());
            assertEquals(false, receivedItem.isFilterEverything());
            assertEquals(true, receivedItem.isFilterMessage());
            assertEquals(true, receivedItem.isFilterPresence_in());
            assertEquals(true, receivedItem.isFilterPresence_out());
            assertEquals(true, client.wasModified());

            privacyManager.deletePrivacyList(listName);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    }
    
    /**
     * Check when a client denies the use of a default list.
     */
    public void testDenyDefaultList() {
    	try {
    		PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
    		PrivacyClient client = new PrivacyClient(privacyManager);
    		privacyManager.addListener(client);
    		
    		privacyManager.declineDefaultList();
    		
    		Thread.sleep(500);
     	
            try {
                // The list should not exist and an error will be raised
                privacyManager.getDefaultList();
            } catch (XMPPException xmppException) {
                assertEquals(404, xmppException.getXMPPError().getCode());
            }
            
	    	assertEquals(null, null);
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail(e.getMessage());
	        }
    }

    /**
     * Check when a client denies the use of the active list.
     */
    public void testDenyActiveList() {
        try {
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
            PrivacyClient client = new PrivacyClient(privacyManager);
            privacyManager.addListener(client);
            
            privacyManager.declineActiveList();
            
            Thread.sleep(500);
        
            try {
                // The list should not exist and an error will be raised
                privacyManager.getActiveList();
            } catch (XMPPException xmppException) {
                assertEquals(404, xmppException.getXMPPError().getCode());
            }
            
            assertEquals(null, null);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    }
    
    /**
     * Check when a client set a new default list.
     */
    public void testCreateDefaultList() {
    	try {
    		String listName = "testCreateDefaultList";
    		
    		PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
    		PrivacyClient client = new PrivacyClient(privacyManager);
    		privacyManager.addListener(client);
    		
    		// Add the list that will be set as the Default
    		ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
    		PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, 1);
        	item.setValue(getConnection(0).getUser());
    		items.add(item);
    		privacyManager.createPrivacyList(listName, items);
    		
    		Thread.sleep(500);
    		
    		// Set the Default list
    		privacyManager.setDefaultListName(listName);
	    	
	    	Thread.sleep(500);
	    		    	
	    	// Assert the list composition.
	    	assertEquals(listName, privacyManager.getDefaultList().toString());
	    	List<PrivacyItem> privacyItems = privacyManager.getPrivacyList(listName).getItems();
	    	assertEquals(1, privacyItems.size());

	    	// Assert the privacy item composition
	    	PrivacyItem receivedItem = privacyItems.get(0);
	    	assertEquals(1, receivedItem.getOrder());
	    	assertEquals(PrivacyItem.Type.jid, receivedItem.getType());
	    	assertEquals(true, receivedItem.isAllow());
            
            privacyManager.deletePrivacyList(listName);
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail(e.getMessage());
	        }
    }

    /**
     * Check when a client add a new list and then remove it.
     */
    public void testRemoveList() {
    	try {
    		String listName = "testRemoveList";
    		
    		PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
    		PrivacyClient client = new PrivacyClient(privacyManager);
    		privacyManager.addListener(client);
    		
    		// Add the list that will be set as the Default
    		ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
    		PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, 1);
        	item.setValue(getConnection(0).getUser());
    		items.add(item);
    		privacyManager.createPrivacyList(listName, items);
    		
    		Thread.sleep(500);
    		
    		// Set the Default list
    		privacyManager.setDefaultListName(listName);
	    	
	    	Thread.sleep(500);

	    	privacyManager.deletePrivacyList(listName);
	    	
	    	Thread.sleep(500);

            try {
                // The list should not exist and an error will be raised
                privacyManager.getPrivacyList(listName);
            } catch (XMPPException xmppException) {
                assertEquals(404, xmppException.getXMPPError().getCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Check different types of privacy items.
     */
    public void testPrivacyItems() {
    	try {
    		String listName = "testPrivacyItems";
    		String user = "tybalt@example.com";
    		String groupName = "enemies";
    		
    		PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(getConnection(0));
    		PrivacyClient client = new PrivacyClient(privacyManager);
    		privacyManager.addListener(client);
    		
    		PrivacyItem[] originalPrivacyItems = new PrivacyItem[12];
    		int i=0;
    		
    		// Items to test JID
    		PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.name(), true, i);
        	item.setValue(i + "_" + user);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyItem.Type.jid.name(), false, i);
        	item.setValue(i + "_" + user);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

        	// Items to test suscription
    		item = new PrivacyItem(PrivacyItem.Type.subscription.name(), true, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_BOTH);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyItem.Type.subscription.name(), false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_FROM);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyItem.Type.subscription.name(), true, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_TO);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyItem.Type.subscription.name(), false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_NONE);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test Group
        	item = new PrivacyItem(PrivacyItem.Type.group.name(), false, i);
        	item.setValue(groupName);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test messages
        	item = new PrivacyItem(PrivacyItem.Type.group.name(), false, i);
        	item.setValue(groupName);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test presence notifications
        	item = new PrivacyItem(PrivacyItem.Type.group.name(), false, i);
        	item.setValue(groupName);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	item = new PrivacyItem(null, false, i);
        	item.setFilterPresence_in(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
    		item = new PrivacyItem(PrivacyItem.Type.subscription.name(), false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_TO);
        	item.setFilterPresence_out(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	item = new PrivacyItem(PrivacyItem.Type.jid.name(), false, i);
        	item.setValue(i + "_" + user);
        	item.setFilterPresence_out(true);
        	item.setFilterPresence_in(true);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;

        	// Set the new privacy list
    		privacyManager.createPrivacyList(listName, Arrays.asList(originalPrivacyItems));

    		Thread.sleep(500);

	    	// Assert the server list composition.
	    	List<PrivacyItem> privacyItems = privacyManager.getPrivacyList(listName).getItems();
	    	assertEquals(originalPrivacyItems.length, privacyItems.size());

	    	// Assert the local and server privacy item composition
	    	PrivacyItem originalItem;
	    	PrivacyItem receivedItem;
	    	int index;
	    	for (int j = 0; j < originalPrivacyItems.length; j++) {
	    		// Look for the same server and original items
	    		receivedItem = privacyItems.get(j);
	    		index = 0;
	    		while ((index < originalPrivacyItems.length) 
	    				&& (originalPrivacyItems[index].getOrder() != receivedItem.getOrder())) {
	    			index++;
				}
	    		originalItem = originalPrivacyItems[index];
	    		
	    		// Assert the items
	    		assertEquals(originalItem.getOrder(), receivedItem.getOrder());
		    	assertEquals(originalItem.getType(), receivedItem.getType());
		    	assertEquals(originalItem.isAllow(), receivedItem.isAllow());
		    	assertEquals(originalItem.getValue(), receivedItem.getValue());
		    	assertEquals(originalItem.isFilterEverything(), receivedItem.isFilterEverything());
		    	assertEquals(originalItem.isFilterIQ(), receivedItem.isFilterIQ());
		    	assertEquals(originalItem.isFilterMessage(), receivedItem.isFilterMessage());
		    	assertEquals(originalItem.isFilterPresence_in(), receivedItem.isFilterPresence_in());
		    	assertEquals(originalItem.isFilterPresence_out(), receivedItem.isFilterPresence_out());
		    	}
            
            privacyManager.deletePrivacyList(listName);
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail(e.getMessage());
	        }
    }
    
	protected int getMaxConnections() {
		return 1;
	}

    /**
     * This class supports automated tests about privacy communication from the
     * server to the client.
     * 
     * @author Francisco Vives
     */

    public class PrivacyClient implements PrivacyListListener {
        /**
         * holds if the receiver list was modified
         */
        private boolean wasModified = false;

        /**
         * holds a privacy to hold server requests Clients should not use Privacy
         * class since it is private for the smack framework.
         */
        private Privacy privacy = new Privacy();

        public PrivacyClient(PrivacyListManager manager) {
            super();
        }

        public void setPrivacyList(String listName, List<PrivacyItem> listItem) {
            privacy.setPrivacyList(listName, listItem);
        }

        public void updatedPrivacyList(String listName) {
            this.wasModified = true;
        }

        public boolean wasModified() {
            return this.wasModified;
        }
    }
}


