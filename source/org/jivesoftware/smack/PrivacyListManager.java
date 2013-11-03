/**
 * $Revision$
 * $Date$
 *
 * Copyright 2006-2007 Jive Software.
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

import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Privacy;
import org.jivesoftware.smack.packet.PrivacyItem;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * A PrivacyListManager is used by XMPP clients to block or allow communications from other
 * users. Use the manager to: <ul>
 *      <li>Retrieve privacy lists.
 *      <li>Add, remove, and edit privacy lists.
 *      <li>Set, change, or decline active lists.
 *      <li>Set, change, or decline the default list (i.e., the list that is active by default).
 * </ul>
 * Privacy Items can handle different kind of permission communications based on JID, group, 
 * subscription type or globally (@see PrivacyItem).
 * 
 * @author Francisco Vives
 */
public class PrivacyListManager {

    // Keep the list of instances of this class.
    private static Map<Connection, PrivacyListManager> instances = Collections
            .synchronizedMap(new WeakHashMap<Connection, PrivacyListManager>());

	private WeakReference<Connection> connection;
	private final List<PrivacyListListener> listeners = new ArrayList<PrivacyListListener>();
	PacketFilter packetFilter = new AndFilter(new IQTypeFilter(IQ.Type.SET),
    		new PacketExtensionFilter("query", "jabber:iq:privacy"));

    static {
        // Create a new PrivacyListManager on every established connection. In the init()
        // method of PrivacyListManager, we'll add a listener that will delete the
        // instance when the connection is closed.
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                getInstanceFor(connection);
            }
        });
    }
    /**
     * Creates a new privacy manager to maintain the communication privacy. Note: no
     * information is sent to or received from the server until you attempt to 
     * get or set the privacy communication.<p>
     *
     * @param connection the XMPP connection.
     */
	private PrivacyListManager(final Connection connection) {
        this.connection = new WeakReference<Connection>(connection);
        // Register the new instance and associate it with the connection 
        instances.put(connection, this);

        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {

                if (packet == null || packet.getError() != null) {
                    return;
                }
                // The packet is correct.
                Privacy privacy = (Privacy) packet;
                
                // Notifies the event to the listeners.
                synchronized (listeners) {
                    for (PrivacyListListener listener : listeners) {
                        // Notifies the created or updated privacy lists
                        for (Map.Entry<String,List<PrivacyItem>> entry : privacy.getItemLists().entrySet()) {
                            String listName = entry.getKey();
                            List<PrivacyItem> items = entry.getValue();
                            if (items.isEmpty()) {
                                listener.updatedPrivacyList(listName);
                            } else {
                                listener.setPrivacyList(listName, items);
                            }
                        }
                    }
                }
                
                // Send a result package acknowledging the reception of a privacy package.
                
                // Prepare the IQ packet to send
                IQ iq = new IQ() {
                    public String getChildElementXML() {
                        return "";
                    }
                };
                iq.setType(IQ.Type.RESULT);
                iq.setFrom(packet.getFrom());
                iq.setPacketID(packet.getPacketID());

                // Send create & join packet.
                connection.sendPacket(iq);
            }
        }, packetFilter);    }

	/** Answer the connection userJID that owns the privacy.
	 * @return the userJID that owns the privacy
	 */
	private String getUser() {
		return connection.get().getUser();
	}

    /**
     * Returns the PrivacyListManager instance associated with a given Connection.
     * 
     * @param connection the connection used to look for the proper PrivacyListManager.
     * @return the PrivacyListManager associated with a given Connection.
     */
    public static synchronized PrivacyListManager getInstanceFor(Connection connection) {
        PrivacyListManager plm = instances.get(connection);
        if (plm == null) plm = new PrivacyListManager(connection);
        return plm;
    }
    
	/**
	 * Send the {@link Privacy} packet to the server in order to know some privacy content and then 
	 * waits for the answer.
	 * 
	 * @param requestPrivacy is the {@link Privacy} packet configured properly whose XML
     *      will be sent to the server.
	 * @return a new {@link Privacy} with the data received from the server.
	 * @exception XMPPException if the request or the answer failed, it raises an exception.
	 */ 
	private Privacy getRequest(Privacy requestPrivacy) throws XMPPException {
        Connection connection = PrivacyListManager.this.connection.get();
        if (connection == null) throw new XMPPException("Connection instance already gc'ed");
		// The request is a get iq type
		requestPrivacy.setType(Privacy.Type.GET);
		requestPrivacy.setFrom(this.getUser());
		
		// Filter packets looking for an answer from the server.
		PacketFilter responseFilter = new PacketIDFilter(requestPrivacy.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        
        // Send create & join packet.
        connection.sendPacket(requestPrivacy);
        
        // Wait up to a certain number of seconds for a reply.
        Privacy privacyAnswer =
            (Privacy) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        // Stop queuing results
        response.cancel();

        // Interprete the result and answer the privacy only if it is valid
        if (privacyAnswer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (privacyAnswer.getError() != null) {
            throw new XMPPException(privacyAnswer.getError());
        }
        return privacyAnswer;
	}
	
	/**
	 * Send the {@link Privacy} packet to the server in order to modify the server privacy and 
	 * waits for the answer.
	 * 
	 * @param requestPrivacy is the {@link Privacy} packet configured properly whose xml will be sent
	 * to the server.
	 * @return a new {@link Privacy} with the data received from the server.
	 * @exception XMPPException if the request or the answer failed, it raises an exception.
	 */ 
	private Packet setRequest(Privacy requestPrivacy) throws XMPPException {
        Connection connection = PrivacyListManager.this.connection.get();
        if (connection == null) throw new XMPPException("Connection instance already gc'ed");
		// The request is a get iq type
		requestPrivacy.setType(Privacy.Type.SET);
		requestPrivacy.setFrom(this.getUser());
		
		// Filter packets looking for an answer from the server.
		PacketFilter responseFilter = new PacketIDFilter(requestPrivacy.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        
        // Send create & join packet.
        connection.sendPacket(requestPrivacy);
        
        // Wait up to a certain number of seconds for a reply.
        Packet privacyAnswer = response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        // Stop queuing results
        response.cancel();

        // Interprete the result and answer the privacy only if it is valid
        if (privacyAnswer == null) {
            throw new XMPPException("No response from server.");
        } else if (privacyAnswer.getError() != null) {
            throw new XMPPException(privacyAnswer.getError());
        }
        return privacyAnswer;
	}

	/**
	 * Answer a privacy containing the list structre without {@link PrivacyItem}.
	 * 
	 * @return a Privacy with the list names.
     * @throws XMPPException if an error occurs.
	 */ 
	private Privacy getPrivacyWithListNames() throws XMPPException {
		
		// The request of the list is an empty privacy message
		Privacy request = new Privacy();
		
		// Send the package to the server and get the answer
		return getRequest(request);
	}
	
    /**
     * Answer the active privacy list.
     * 
     * @return the privacy list of the active list.
     * @throws XMPPException if an error occurs.
     */ 
    public PrivacyList getActiveList() throws XMPPException {
        Privacy privacyAnswer = this.getPrivacyWithListNames();
        String listName = privacyAnswer.getActiveName();
        boolean isDefaultAndActive = privacyAnswer.getActiveName() != null
                && privacyAnswer.getDefaultName() != null
                && privacyAnswer.getActiveName().equals(
                privacyAnswer.getDefaultName());
        return new PrivacyList(true, isDefaultAndActive, listName, getPrivacyListItems(listName));
    }
    
    /**
     * Answer the default privacy list.
     * 
     * @return the privacy list of the default list.
     * @throws XMPPException if an error occurs.
     */ 
    public PrivacyList getDefaultList() throws XMPPException {
        Privacy privacyAnswer = this.getPrivacyWithListNames();
        String listName = privacyAnswer.getDefaultName();
        boolean isDefaultAndActive = privacyAnswer.getActiveName() != null
                && privacyAnswer.getDefaultName() != null
                && privacyAnswer.getActiveName().equals(
                privacyAnswer.getDefaultName());
        return new PrivacyList(isDefaultAndActive, true, listName, getPrivacyListItems(listName));
    }
    
    /**
     * Answer the privacy list items under listName with the allowed and blocked permissions.
     * 
     * @param listName the name of the list to get the allowed and blocked permissions.
     * @return a list of privacy items under the list listName.
     * @throws XMPPException if an error occurs.
     */ 
    private List<PrivacyItem> getPrivacyListItems(String listName) throws XMPPException {
        
        // The request of the list is an privacy message with an empty list
        Privacy request = new Privacy();
        request.setPrivacyList(listName, new ArrayList<PrivacyItem>());
        
        // Send the package to the server and get the answer
        Privacy privacyAnswer = getRequest(request);
        
        return privacyAnswer.getPrivacyList(listName);
    }
    
	/**
	 * Answer the privacy list items under listName with the allowed and blocked permissions.
	 * 
	 * @param listName the name of the list to get the allowed and blocked permissions.
	 * @return a privacy list under the list listName.
     * @throws XMPPException if an error occurs.
	 */ 
	public PrivacyList getPrivacyList(String listName) throws XMPPException {
		
        return new PrivacyList(false, false, listName, getPrivacyListItems(listName));
	}
	
    /**
     * Answer every privacy list with the allowed and blocked permissions.
     * 
     * @return an array of privacy lists.
     * @throws XMPPException if an error occurs.
     */ 
    public PrivacyList[] getPrivacyLists() throws XMPPException {
        Privacy privacyAnswer = this.getPrivacyWithListNames();
        Set<String> names = privacyAnswer.getPrivacyListNames();
        PrivacyList[] lists = new PrivacyList[names.size()];
        boolean isActiveList;
        boolean isDefaultList;
        int index=0;
        for (String listName : names) {
            isActiveList = listName.equals(privacyAnswer.getActiveName());
            isDefaultList = listName.equals(privacyAnswer.getDefaultName());
            lists[index] = new PrivacyList(isActiveList, isDefaultList,
                    listName, getPrivacyListItems(listName));
            index = index + 1;
        }
        return lists;
    }

    
	/**
	 * Set or change the active list to listName.
	 * 
	 * @param listName the list name to set as the active one.
	 * @exception XMPPException if the request or the answer failed, it raises an exception.
	 */ 
	public void setActiveListName(String listName) throws XMPPException {
		
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setActiveName(listName);
		
		// Send the package to the server
		setRequest(request);
	}

	/**
	 * Client declines the use of active lists.
     *
     * @throws XMPPException if an error occurs.
	 */ 
	public void declineActiveList() throws XMPPException {
		
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setDeclineActiveList(true);
		
		// Send the package to the server
		setRequest(request);
	}

	/**
	 * Set or change the default list to listName.
	 * 
	 * @param listName the list name to set as the default one.
	 * @exception XMPPException if the request or the answer failed, it raises an exception.
	 */ 
	public void setDefaultListName(String listName) throws XMPPException {
		
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setDefaultName(listName);
		
		// Send the package to the server
		setRequest(request);
	}
	
	/**
	 * Client declines the use of default lists.
     *
     * @throws XMPPException if an error occurs.
	 */ 
	public void declineDefaultList() throws XMPPException {
		
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setDeclineDefaultList(true);
		
		// Send the package to the server
		setRequest(request);
	}
	
	/**
	 * The client has created a new list. It send the new one to the server.
	 * 
     * @param listName the list that has changed its content.
     * @param privacyItems a List with every privacy item in the list.
     * @throws XMPPException if an error occurs.
	 */ 
	public void createPrivacyList(String listName, List<PrivacyItem> privacyItems) throws XMPPException {

		this.updatePrivacyList(listName, privacyItems);
	}

    /**
     * The client has edited an existing list. It updates the server content with the resulting 
     * list of privacy items. The {@link PrivacyItem} list MUST contain all elements in the 
     * list (not the "delta").
     * 
     * @param listName the list that has changed its content.
     * @param privacyItems a List with every privacy item in the list.
     * @throws XMPPException if an error occurs.
     */ 
    public void updatePrivacyList(String listName, List<PrivacyItem> privacyItems) throws XMPPException {

        // Build the privacy package to add or update the new list
        Privacy request = new Privacy();
        request.setPrivacyList(listName, privacyItems);

        // Send the package to the server
        setRequest(request);
    }
    
	/**
	 * Remove a privacy list.
	 * 
     * @param listName the list that has changed its content.
     * @throws XMPPException if an error occurs.
	 */ 
	public void deletePrivacyList(String listName) throws XMPPException {
		
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setPrivacyList(listName, new ArrayList<PrivacyItem>());

		// Send the package to the server
		setRequest(request);
	}
	
    /**
     * Adds a packet listener that will be notified of any new update in the user
     * privacy communication.
     *
     * @param listener a packet listener.
     */
    public void addListener(PrivacyListListener listener) {
        // Keep track of the listener so that we can manually deliver extra
        // messages to it later if needed.
        synchronized (listeners) {
            listeners.add(listener);
        }
    }    
}
