package org.jivesoftware.smack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.packet.PrivacyItem;
import org.jivesoftware.smack.packet.PrivacyItem.PrivacyRule;
import org.jivesoftware.smack.test.SmackTestCase;

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
            ArrayList items = new ArrayList();
            PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName, items);
            
            Thread.sleep(500);
            
            // Set the active list
            privacyManager.setActiveListName(listName);
            
            Thread.sleep(500);
                        
            // Assert the list composition.
            assertEquals(listName, privacyManager.getActiveList().toString());
            List privacyItems = privacyManager.getPrivacyList(listName).getItems();
            assertEquals(1, privacyItems.size());

            // Assert the privacy item composition
            PrivacyItem receivedItem = (PrivacyItem) privacyItems.get(0);
            assertEquals(1, receivedItem.getOrder());
            assertEquals(PrivacyRule.JID, receivedItem.getType());
            assertEquals(true, receivedItem.isAllow());
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
            ArrayList items = new ArrayList();
            PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName1, items);
            
            // Add the another list
            ArrayList itemsList2 = new ArrayList();
            item = new PrivacyItem(PrivacyRule.GROUP, false, 2);
            item.setValue(groupName);
            item.setFilterMessage(true);
            itemsList2.add(item);
            privacyManager.createPrivacyList(listName2, itemsList2);
            
            Thread.sleep(500);
                        
            // Assert the list composition.
            PrivacyList[] privacyItems = privacyManager.getPrivacyLists();
            PrivacyList receivedList1 = null;
            PrivacyList receivedList2 = null;
            for (int i = 0; i < privacyItems.length; i++) {
                if (listName1.equals(privacyItems[i].toString())) {
                    receivedList1 = privacyItems[i];
                }
                if (listName2.equals(privacyItems[i].toString())) {
                    receivedList2 = privacyItems[i];
                }
            }
            
            
            PrivacyItem receivedItem;
            // Assert the list 1
            assertNotNull(receivedList1);
            assertEquals(1, receivedList1.getItems().size());
            receivedItem = (PrivacyItem) receivedList1.getItems().get(0);
            assertEquals(1, receivedItem.getOrder());
            assertEquals(PrivacyRule.JID, receivedItem.getType());
            assertEquals(true, receivedItem.isAllow());
            
            // Assert the list 2
            assertNotNull(receivedList2);
            assertEquals(1, receivedList2.getItems().size());
            receivedItem = (PrivacyItem) receivedList2.getItems().get(0);
            assertEquals(2, receivedItem.getOrder());
            assertEquals(PrivacyRule.GROUP, receivedItem.getType());
            assertEquals(false, receivedItem.isAllow());
            assertEquals(groupName, receivedItem.getValue());
            assertEquals(false, receivedItem.isFilterEverything());
            assertEquals(true, receivedItem.isFilterMessage());
            assertEquals(false, receivedItem.isFilterPresence_in());
            assertEquals(false, receivedItem.isFilterPresence_out());
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
            ArrayList items = new ArrayList();
            PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, 1);
            item.setValue(getConnection(0).getUser());
            items.add(item);
            privacyManager.createPrivacyList(listName, items);
            
            Thread.sleep(500);
            
            // Remove the existing item and add a new one.
            items.remove(item);
            item = new PrivacyItem(PrivacyRule.JID, false, 2);
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
            PrivacyItem receivedItem = (PrivacyItem) list.getItems().get(0);
            assertEquals(2, receivedItem.getOrder());
            assertEquals(PrivacyRule.JID, receivedItem.getType());
            assertEquals(false, receivedItem.isAllow());
            assertEquals(user, receivedItem.getValue());
            assertEquals(false, receivedItem.isFilterEverything());
            assertEquals(true, receivedItem.isFilterMessage());
            assertEquals(true, receivedItem.isFilterPresence_in());
            assertEquals(true, receivedItem.isFilterPresence_out());
            assertEquals(true, client.wasModified());

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
    		ArrayList items = new ArrayList();
    		PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, 1);
        	item.setValue(getConnection(0).getUser());
    		items.add(item);
    		privacyManager.createPrivacyList(listName, items);
    		
    		Thread.sleep(500);
    		
    		// Set the Default list
    		privacyManager.setDefaultListName(listName);
	    	
	    	Thread.sleep(500);
	    		    	
	    	// Assert the list composition.
	    	assertEquals(listName, privacyManager.getDefaultList().toString());
	    	List privacyItems = privacyManager.getPrivacyList(listName).getItems();
	    	assertEquals(1, privacyItems.size());

	    	// Assert the privacy item composition
	    	PrivacyItem receivedItem = (PrivacyItem) privacyItems.get(0);
	    	assertEquals(1, receivedItem.getOrder());
	    	assertEquals(PrivacyRule.JID, receivedItem.getType());
	    	assertEquals(true, receivedItem.isAllow());
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
    		ArrayList items = new ArrayList();
    		PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, 1);
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
    		PrivacyItem item = new PrivacyItem(PrivacyRule.JID, true, i);
        	item.setValue(i + "_" + user);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyRule.JID, false, i);
        	item.setValue(i + "_" + user);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

        	// Items to test suscription
    		item = new PrivacyItem(PrivacyRule.SUBSCRIPTION, true, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_BOTH);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyRule.SUBSCRIPTION, false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_FROM);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyRule.SUBSCRIPTION, true, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_TO);
        	originalPrivacyItems[i] = item;
        	i = i + 1;

    		item = new PrivacyItem(PrivacyRule.SUBSCRIPTION, false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_NONE);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test Group
        	item = new PrivacyItem(PrivacyRule.GROUP, false, i);
        	item.setValue(groupName);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test messages
        	item = new PrivacyItem(PrivacyRule.GROUP, false, i);
        	item.setValue(groupName);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	// Items to test presence notifications
        	item = new PrivacyItem(PrivacyRule.GROUP, false, i);
        	item.setValue(groupName);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	item = new PrivacyItem(null, false, i);
        	item.setFilterPresence_in(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
    		item = new PrivacyItem(PrivacyRule.SUBSCRIPTION, false, i);
        	item.setValue(PrivacyRule.SUBSCRIPTION_TO);
        	item.setFilterPresence_out(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	item = new PrivacyItem(PrivacyRule.JID, false, i);
        	item.setValue(i + "_" + user);
        	item.setFilterPresence_out(true);
        	item.setFilterPresence_in(true);
        	item.setFilterMessage(true);
        	originalPrivacyItems[i] = item;
        	i = i + 1;
        	
        	
        	// Set the new privacy list
    		privacyManager.createPrivacyList(listName, Arrays.asList(originalPrivacyItems));

    		Thread.sleep(500);

	    	// Assert the server list composition.
	    	List privacyItems = privacyManager.getPrivacyList(listName).getItems();
	    	assertEquals(originalPrivacyItems.length, privacyItems.size());

	    	// Assert the local and server privacy item composition
	    	PrivacyItem originalItem;
	    	PrivacyItem receivedItem;
	    	int index = 0;
	    	for (int j = 0; j < originalPrivacyItems.length; j++) {
	    		// Look for the same server and original items
	    		receivedItem = (PrivacyItem) privacyItems.get(j);
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
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail(e.getMessage());
	        }
    }
    
	protected int getMaxConnections() {
		return 1;
	}
}
