/**
 *
 * Copyright 2006-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.privacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.privacy.packet.Privacy;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;

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
 * @see <a href="http://xmpp.org/extensions/xep-0016.html">XEP-16: Privacy Lists</a>
 */
public class PrivacyListManager extends Manager {
    public static final String NAMESPACE = "jabber:iq:privacy";

    private static final PacketFilter PACKET_FILTER = new AndFilter(new IQTypeFilter(IQ.Type.SET),
                    new PacketExtensionFilter("query", "jabber:iq:privacy"));

    // Keep the list of instances of this class.
    private static final Map<XMPPConnection, PrivacyListManager> instances = Collections
            .synchronizedMap(new WeakHashMap<XMPPConnection, PrivacyListManager>());

	private final List<PrivacyListListener> listeners = new ArrayList<PrivacyListListener>();

    static {
        // Create a new PrivacyListManager on every established connection.
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
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
	private PrivacyListManager(final XMPPConnection connection) {
        super(connection);
        // Register the new instance and associate it with the connection 
        instances.put(connection, this);

        connection.addPacketListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) throws NotConnectedException {
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
                IQ iq = IQ.createResultIQ(privacy);
                connection.sendPacket(iq);
            }
        }, PACKET_FILTER);
    }

	/** Answer the connection userJID that owns the privacy.
	 * @return the userJID that owns the privacy
	 */
	private String getUser() {
		return connection().getUser();
	}

    /**
     * Returns the PrivacyListManager instance associated with a given XMPPConnection.
     * 
     * @param connection the connection used to look for the proper PrivacyListManager.
     * @return the PrivacyListManager associated with a given XMPPConnection.
     */
    public static synchronized PrivacyListManager getInstanceFor(XMPPConnection connection) {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	private Privacy getRequest(Privacy requestPrivacy) throws NoResponseException, XMPPErrorException, NotConnectedException  {
		// The request is a get iq type
		requestPrivacy.setType(Privacy.Type.GET);
		requestPrivacy.setFrom(this.getUser());

        Privacy privacyAnswer = (Privacy) connection().createPacketCollectorAndSend(requestPrivacy).nextResultOrThrow();
        return privacyAnswer;
	}

    /**
     * Send the {@link Privacy} packet to the server in order to modify the server privacy and waits
     * for the answer.
     * 
     * @param requestPrivacy is the {@link Privacy} packet configured properly whose xml will be
     *        sent to the server.
     * @return a new {@link Privacy} with the data received from the server.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    private Packet setRequest(Privacy requestPrivacy) throws NoResponseException, XMPPErrorException, NotConnectedException  {
        // The request is a get iq type
        requestPrivacy.setType(Privacy.Type.SET);
        requestPrivacy.setFrom(this.getUser());

        return connection().createPacketCollectorAndSend(requestPrivacy).nextResultOrThrow();
    }

	/**
	 * Answer a privacy containing the list structure without {@link PrivacyItem}.
	 * 
	 * @return a Privacy with the list names.
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	private Privacy getPrivacyWithListNames() throws NoResponseException, XMPPErrorException, NotConnectedException {
		// The request of the list is an empty privacy message
		Privacy request = new Privacy();
		
		// Send the package to the server and get the answer
		return getRequest(request);
	}

    /**
     * Answer the active privacy list.
     * 
     * @return the privacy list of the active list.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */ 
    public PrivacyList getActiveList() throws NoResponseException, XMPPErrorException, NotConnectedException  {
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
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */ 
    public PrivacyList getDefaultList() throws NoResponseException, XMPPErrorException, NotConnectedException {
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
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */ 
    private List<PrivacyItem> getPrivacyListItems(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException  {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public PrivacyList getPrivacyList(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException  {
        return new PrivacyList(false, false, listName, getPrivacyListItems(listName));
	}

    /**
     * Answer every privacy list with the allowed and blocked permissions.
     * 
     * @return an array of privacy lists.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */ 
    public PrivacyList[] getPrivacyLists() throws NoResponseException, XMPPErrorException, NotConnectedException {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void setActiveListName(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException {
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setActiveName(listName);
		
		// Send the package to the server
		setRequest(request);
	}

	/**
	 * Client declines the use of active lists.
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void declineActiveList() throws NoResponseException, XMPPErrorException, NotConnectedException {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void setDefaultListName(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException  {
		// The request of the list is an privacy message with an empty list
		Privacy request = new Privacy();
		request.setDefaultName(listName);
		
		// Send the package to the server
		setRequest(request);
	}

	/**
	 * Client declines the use of default lists.
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void declineDefaultList() throws NoResponseException, XMPPErrorException, NotConnectedException {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void createPrivacyList(String listName, List<PrivacyItem> privacyItems) throws NoResponseException, XMPPErrorException, NotConnectedException  {
		updatePrivacyList(listName, privacyItems);
	}

    /**
     * The client has edited an existing list. It updates the server content with the resulting 
     * list of privacy items. The {@link PrivacyItem} list MUST contain all elements in the 
     * list (not the "delta").
     * 
     * @param listName the list that has changed its content.
     * @param privacyItems a List with every privacy item in the list.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */ 
    public void updatePrivacyList(String listName, List<PrivacyItem> privacyItems) throws NoResponseException, XMPPErrorException, NotConnectedException  {
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */ 
	public void deletePrivacyList(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException {
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

    /**
     * Check if the user's server supports privacy lists.
     * 
     * @return true, if the server supports privacy lists, false otherwise.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public boolean isSupported() throws NoResponseException, XMPPErrorException, NotConnectedException{
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(
                        connection().getServiceName(), NAMESPACE);
    }
}
