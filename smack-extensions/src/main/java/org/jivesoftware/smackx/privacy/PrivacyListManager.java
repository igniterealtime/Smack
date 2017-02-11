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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQResultReplyFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.privacy.filter.SetActiveListFilter;
import org.jivesoftware.smackx.privacy.filter.SetDefaultListFilter;
import org.jivesoftware.smackx.privacy.packet.Privacy;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;

/**
 * A PrivacyListManager is used by XMPP clients to block or allow communications from other
 * users. Use the manager to:
 * <ul>
 *      <li>Retrieve privacy lists.
 *      <li>Add, remove, and edit privacy lists.
 *      <li>Set, change, or decline active lists.
 *      <li>Set, change, or decline the default list (i.e., the list that is active by default).
 * </ul>
 * Privacy Items can handle different kind of permission communications based on JID, group, 
 * subscription type or globally (see {@link PrivacyItem}).
 * 
 * @author Francisco Vives
 * @see <a href="http://xmpp.org/extensions/xep-0016.html">XEP-16: Privacy Lists</a>
 */
public final class PrivacyListManager extends Manager {
    public static final String NAMESPACE = Privacy.NAMESPACE;

    public static final StanzaFilter PRIVACY_FILTER = new StanzaTypeFilter(Privacy.class);

    private static final StanzaFilter PRIVACY_RESULT = new AndFilter(IQTypeFilter.RESULT, PRIVACY_FILTER);

    // Keep the list of instances of this class.
    private static final Map<XMPPConnection, PrivacyListManager> INSTANCES = new WeakHashMap<XMPPConnection, PrivacyListManager>();

    private final Set<PrivacyListListener> listeners = new CopyOnWriteArraySet<PrivacyListListener>();

    static {
        // Create a new PrivacyListManager on every established connection.
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    // TODO implement: private final Map<String, PrivacyList> cachedPrivacyLists = new HashMap<>();
    private volatile String cachedActiveListName;
    private volatile String cachedDefaultListName;

    /**
     * Creates a new privacy manager to maintain the communication privacy. Note: no
     * information is sent to or received from the server until you attempt to 
     * get or set the privacy communication.<p>
     *
     * @param connection the XMPP connection.
     */
    private PrivacyListManager(XMPPConnection connection) {
        super(connection);

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(Privacy.ELEMENT, Privacy.NAMESPACE,
                        IQ.Type.set, Mode.sync) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                Privacy privacy = (Privacy) iqRequest;

                // Notifies the event to the listeners.
                for (PrivacyListListener listener : listeners) {
                    // Notifies the created or updated privacy lists
                    for (Map.Entry<String, List<PrivacyItem>> entry : privacy.getItemLists().entrySet()) {
                        String listName = entry.getKey();
                        List<PrivacyItem> items = entry.getValue();
                        if (items.isEmpty()) {
                            listener.updatedPrivacyList(listName);
                        }
                        else {
                            listener.setPrivacyList(listName, items);
                        }
                    }
                }

                return IQ.createResultIQ(privacy);
            }
        });

        // cached(Active|Default)ListName handling
        connection.addPacketSendingListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException {
                XMPPConnection connection = connection();
                Privacy privacy = (Privacy) packet;
                StanzaFilter iqResultReplyFilter = new IQResultReplyFilter(privacy, connection);
                final String activeListName = privacy.getActiveName();
                final boolean declinceActiveList = privacy.isDeclineActiveList();
                connection.addOneTimeSyncCallback(new StanzaListener() {
                    @Override
                    public void processStanza(Stanza packet) throws NotConnectedException {
                            if (declinceActiveList) {
                                cachedActiveListName = null;
                            }
                            else {
                                cachedActiveListName = activeListName;
                            }
                            return;
                    }
                }, iqResultReplyFilter);
            }
        }, SetActiveListFilter.INSTANCE);
        connection.addPacketSendingListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException {
                XMPPConnection connection = connection();
                Privacy privacy = (Privacy) packet;
                StanzaFilter iqResultReplyFilter = new IQResultReplyFilter(privacy, connection);
                final String defaultListName = privacy.getDefaultName();
                final boolean declinceDefaultList = privacy.isDeclineDefaultList();
                connection.addOneTimeSyncCallback(new StanzaListener() {
                    @Override
                    public void processStanza(Stanza packet) throws NotConnectedException {
                            if (declinceDefaultList) {
                                cachedDefaultListName = null;
                            }
                            else {
                                cachedDefaultListName = defaultListName;
                            }
                            return;
                    }
                }, iqResultReplyFilter);
            }
        }, SetDefaultListFilter.INSTANCE);
        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException {
                Privacy privacy = (Privacy) packet;
                // If a privacy IQ result stanza has an active or default list name set, then we use that
                // as cached list name.
                String activeList = privacy.getActiveName();
                if (activeList != null) {
                    cachedActiveListName = activeList;
                }
                String defaultList = privacy.getDefaultName();
                if (defaultList != null) {
                    cachedDefaultListName = defaultList;
                }
            }
        }, PRIVACY_RESULT);
        connection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                // No need to reset the cache if the connection got resumed.
                if (resumed) {
                    return;
                }
                cachedActiveListName = cachedDefaultListName = null;
            }
        });

        // XEP-0016 § 3.
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
    }

    /**
     * Returns the PrivacyListManager instance associated with a given XMPPConnection.
     * 
     * @param connection the connection used to look for the proper PrivacyListManager.
     * @return the PrivacyListManager associated with a given XMPPConnection.
     */
    public static synchronized PrivacyListManager getInstanceFor(XMPPConnection connection) {
        PrivacyListManager plm = INSTANCES.get(connection);
        if (plm == null) {
            plm = new PrivacyListManager(connection);
            // Register the new instance and associate it with the connection
            INSTANCES.put(connection, plm);
        }
        return plm;
    }

    /**
     * Send the {@link Privacy} stanza(/packet) to the server in order to know some privacy content and then 
     * waits for the answer.
     * 
     * @param requestPrivacy is the {@link Privacy} stanza(/packet) configured properly whose XML
     *      will be sent to the server.
     * @return a new {@link Privacy} with the data received from the server.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    private Privacy getRequest(Privacy requestPrivacy) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        // The request is a get iq type
        requestPrivacy.setType(Privacy.Type.get);

        return connection().createStanzaCollectorAndSend(requestPrivacy).nextResultOrThrow();
    }

    /**
     * Send the {@link Privacy} stanza(/packet) to the server in order to modify the server privacy and waits
     * for the answer.
     * 
     * @param requestPrivacy is the {@link Privacy} stanza(/packet) configured properly whose xml will be
     *        sent to the server.
     * @return a new {@link Privacy} with the data received from the server.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    private Stanza setRequest(Privacy requestPrivacy) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        // The request is a get iq type
        requestPrivacy.setType(Privacy.Type.set);

        return connection().createStanzaCollectorAndSend(requestPrivacy).nextResultOrThrow();
    }

    /**
     * Answer a privacy containing the list structure without {@link PrivacyItem}.
     * 
     * @return a Privacy with the list names.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    private Privacy getPrivacyWithListNames() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // The request of the list is an empty privacy message
        Privacy request = new Privacy();

        // Send the package to the server and get the answer
        return getRequest(request);
    }

    /**
     * Answer the active privacy list. Returns <code>null</code> if there is no active list.
     * 
     * @return the privacy list of the active list.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    public PrivacyList getActiveList() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        Privacy privacyAnswer = this.getPrivacyWithListNames();
        String listName = privacyAnswer.getActiveName();
        if (StringUtils.isNullOrEmpty(listName)) {
            return null;
        }
        boolean isDefaultAndActive = listName != null && listName.equals(privacyAnswer.getDefaultName());
        return new PrivacyList(true, isDefaultAndActive, listName, getPrivacyListItems(listName));
    }

    /**
     * Get the name of the active list.
     * 
     * @return the name of the active list or null if there is none set.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @since 4.1
     */
    public String getActiveListName() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (cachedActiveListName != null) {
            return cachedActiveListName;
        }
        return getPrivacyWithListNames().getActiveName();
    }

    /**
     * Answer the default privacy list. Returns <code>null</code> if there is no default list.
     * 
     * @return the privacy list of the default list.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    public PrivacyList getDefaultList() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Privacy privacyAnswer = this.getPrivacyWithListNames();
        String listName = privacyAnswer.getDefaultName();
        if (StringUtils.isNullOrEmpty(listName)) {
            return null;
        }
        boolean isDefaultAndActive = listName.equals(privacyAnswer.getActiveName());
        return new PrivacyList(isDefaultAndActive, true, listName, getPrivacyListItems(listName));
    }

    /**
     * Get the name of the default list.
     *
     * @return the name of the default list or null if there is none set.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @since 4.1
     */
    public String getDefaultListName() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (cachedDefaultListName != null) {
            return cachedDefaultListName;
        }
        return getPrivacyWithListNames().getDefaultName();
    }

    /**
     * Returns the name of the effective privacy list.
     * <p>
     * The effective privacy list is the one that is currently enforced on the connection. It's either the active
     * privacy list, or, if the active privacy list is not set, the default privacy list.
     * </p>
     *
     * @return the name of the effective privacy list or null if there is none set.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @since 4.1
     */
    public String getEffectiveListName() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        String activeListName = getActiveListName();
        if (activeListName != null) {
            return activeListName;
        }
        return getDefaultListName();
    }

    /**
     * Answer the privacy list items under listName with the allowed and blocked permissions.
     * 
     * @param listName the name of the list to get the allowed and blocked permissions.
     * @return a list of privacy items under the list listName.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    private List<PrivacyItem> getPrivacyListItems(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        assert StringUtils.isNotEmpty(listName);
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
     * @throws InterruptedException 
     */ 
    public PrivacyList getPrivacyList(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        listName = StringUtils.requireNotNullOrEmpty(listName, "List name must not be null");
        return new PrivacyList(false, false, listName, getPrivacyListItems(listName));
    }

    /**
     * Answer every privacy list with the allowed and blocked permissions.
     * 
     * @return an array of privacy lists.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */ 
    public List<PrivacyList> getPrivacyLists() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Privacy privacyAnswer = getPrivacyWithListNames();
        Set<String> names = privacyAnswer.getPrivacyListNames();
        List<PrivacyList> lists = new ArrayList<>(names.size());
        for (String listName : names) {
            boolean isActiveList = listName.equals(privacyAnswer.getActiveName());
            boolean isDefaultList = listName.equals(privacyAnswer.getDefaultName());
            lists.add(new PrivacyList(isActiveList, isDefaultList, listName,
                            getPrivacyListItems(listName)));
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
     * @throws InterruptedException 
     */ 
    public void setActiveListName(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
     * @throws InterruptedException 
     */ 
    public void declineActiveList() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
     * @throws InterruptedException 
     */ 
    public void setDefaultListName(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
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
     * @throws InterruptedException 
     */ 
    public void declineDefaultList() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
     * @throws InterruptedException 
     */ 
    public void createPrivacyList(String listName, List<PrivacyItem> privacyItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
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
     * @throws InterruptedException 
     */ 
    public void updatePrivacyList(String listName, List<PrivacyItem> privacyItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
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
     * @throws InterruptedException 
     */ 
    public void deletePrivacyList(String listName) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // The request of the list is an privacy message with an empty list
        Privacy request = new Privacy();
        request.setPrivacyList(listName, new ArrayList<PrivacyItem>());

        // Send the package to the server
        setRequest(request);
    }

    /**
     * Adds a privacy list listener that will be notified of any new update in the user
     * privacy communication.
     *
     * @param listener a privacy list listener.
     * @return true, if the listener was not already added.
     */
    public boolean addListener(PrivacyListListener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes the privacy list listener.
     *
     * @param listener
     * @return true, if the listener was removed.
     */
    public boolean removeListener(PrivacyListListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Check if the user's server supports privacy lists.
     * 
     * @return true, if the server supports privacy lists, false otherwise.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean isSupported() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException{
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(NAMESPACE);
    }
}
