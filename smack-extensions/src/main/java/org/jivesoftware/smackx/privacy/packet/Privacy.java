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
package org.jivesoftware.smackx.privacy.packet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.packet.IQ;

/**
 * A Privacy IQ Packet, is used by the {@link org.jivesoftware.smackx.privacy.PrivacyListManager}
 * and {@link org.jivesoftware.smackx.privacy.provider.PrivacyProvider} to allow and block
 * communications from other users. It contains the appropriate structure to suit
 * user-defined privacy lists. Different configured Privacy packages are used in the
 * server & manager communication in order to:
 * <ul>
 * <li>Retrieving one's privacy lists. 
 * <li>Adding, removing, and editing one's privacy lists. 
 * <li>Setting, changing, or declining active lists. 
 * <li>Setting, changing, or declining the default list (i.e., the list that is active by default). 
 * </ul>
 * Privacy Items can handle different kind of blocking communications based on JID, group, 
 * subscription type or globally {@link PrivacyItem}
 * 
 * @author Francisco Vives
 */
public class Privacy extends IQ {
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:privacy";

	/** declineActiveList is true when the user declines the use of the active list **/
	private boolean declineActiveList=false;
	/** activeName is the name associated with the active list set for the session **/
	private String activeName;
	/** declineDefaultList is true when the user declines the use of the default list **/
	private boolean declineDefaultList=false;
	/** defaultName is the name of the default list that applies to the user as a whole **/
	private String defaultName;
	/** itemLists holds the set of privacy items classified in lists. It is a map where the 
	 * key is the name of the list and the value a collection with privacy items. **/
	private Map<String, List<PrivacyItem>> itemLists = new HashMap<String, List<PrivacyItem>>();

    public Privacy() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Set or update a privacy list with privacy items.
     *
     * @param listName the name of the new privacy list.
     * @param listItem the {@link PrivacyItem} that rules the list.
     * @return the privacy List.
     */
    public List<PrivacyItem> setPrivacyList(String listName, List<PrivacyItem> listItem) {
        // Add new list to the itemLists
        this.getItemLists().put(listName, listItem);
        return listItem;
    }

    /**
     * Set the active list based on the default list.
     *
     * @return the active List.
     */
    public List<PrivacyItem> setActivePrivacyList() {
        this.setActiveName(this.getDefaultName());
        return this.getItemLists().get(this.getActiveName());
    }

    /**
     * Deletes an existing privacy list. If the privacy list being deleted was the default list 
     * then the user will end up with no default list. Therefore, the user will have to set a new 
     * default list.
     *
     * @param listName the name of the list being deleted.
     */
    public void deletePrivacyList(String listName) {
        // Remove the list from the cache
        // CHECKSTYLE:OFF
    	this.getItemLists().remove(listName);
        // CHECKSTYLE:ON

        // Check if deleted list was the default list
        if (this.getDefaultName() != null && listName.equals(this.getDefaultName())) {
        // CHECKSTYLE:OFF
        	this.setDefaultName(null);
        // CHECKSTYLE:ON
        }
    }

    /**
     * Returns the active privacy list or <tt>null</tt> if none was found.
     *
     * @return list with {@link PrivacyItem} or <tt>null</tt> if none was found.
     */
    public List<PrivacyItem> getActivePrivacyList() {
        // Check if we have the default list
        // CHECKSTYLE:OFF
        if (this.getActiveName() == null) {
        	return null;
        } else {
        	return this.getItemLists().get(this.getActiveName());
        }
        // CHECKSTYLE:ON
    }

    /**
     * Returns the default privacy list or <tt>null</tt> if none was found.
     *
     * @return list with {@link PrivacyItem} or <tt>null</tt> if none was found.
     */
    public List<PrivacyItem> getDefaultPrivacyList() {
        // Check if we have the default list
        // CHECKSTYLE:OFF
        if (this.getDefaultName() == null) {
        	return null;
        } else {
        	return this.getItemLists().get(this.getDefaultName());
        }
        // CHECKSTYLE:ON
    }

    /**
     * Returns a specific privacy list.
     *
     * @param listName the name of the list to get.
     * @return a List with {@link PrivacyItem}
     */
    public List<PrivacyItem> getPrivacyList(String listName) {
        return this.getItemLists().get(listName);
    }

    /**
     * Returns the privacy item in the specified order.
     *
     * @param listName the name of the privacy list.
     * @param order the order of the element.
     * @return a List with {@link PrivacyItem}
     */
    public PrivacyItem getItem(String listName, int order) {
        // CHECKSTYLE:OFF
    	Iterator<PrivacyItem> values = getPrivacyList(listName).iterator();
    	PrivacyItem itemFound = null;
    	while (itemFound == null && values.hasNext()) {
    		PrivacyItem element = values.next();
			if (element.getOrder() == order) {
				itemFound = element;
			}
		}
    	return itemFound;
        // CHECKSTYLE:ON
    }

    /**
     * Sets a given privacy list as the new user default list.
     *
     * @param newDefault the new default privacy list.
     * @return if the default list was changed.
     */
    public boolean changeDefaultList(String newDefault) {
        if (this.getItemLists().containsKey(newDefault)) {
           this.setDefaultName(newDefault);
           return true;
        } else {
            // CHECKSTYLE:OFF
        	return false; 
            // CHECKSTYLE:ON
        }
    }

    /**
     * Remove the list.
     *
     * @param listName name of the list to remove.
     */
     public void deleteList(String listName) {
     // CHECKSTYLE:OFF
    	 this.getItemLists().remove(listName);
     // CHECKSTYLE:ON
     }

    /**
     * Returns the name associated with the active list set for the session. Communications
     * will be verified against the active list.
     *
     * @return the name of the active list.
     */
    // CHECKSTYLE:OFF
	public String getActiveName() {
		return activeName;
	}
    // CHECKSTYLE:ON

    /**
     * Sets the name associated with the active list set for the session. Communications
     * will be verified against the active list.
     * 
     * @param activeName is the name of the active list.
     */
    // CHECKSTYLE:OFF
	public void setActiveName(String activeName) {
		this.activeName = activeName;
	}
    // CHECKSTYLE:ON

    /**
     * Returns the name of the default list that applies to the user as a whole. Default list is 
     * processed if there is no active list set for the target session/resource to which a stanza 
     * is addressed, or if there are no current sessions for the user.
     * 
     * @return the name of the default list.
     */
    // CHECKSTYLE:OFF
	public String getDefaultName() {
		return defaultName;
	}
    // CHECKSTYLE:ON

    /**
     * Sets the name of the default list that applies to the user as a whole. Default list is 
     * processed if there is no active list set for the target session/resource to which a stanza 
     * is addressed, or if there are no current sessions for the user.
     * 
     * If there is no default list set, then all Privacy Items are processed.
     * 
     * @param defaultName is the name of the default list.
     */
    // CHECKSTYLE:OFF
	public void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}
    // CHECKSTYLE:ON

    /**
     * Returns the collection of privacy list that the user holds. A Privacy List contains a set of 
     * rules that define if communication with the list owner is allowed or denied. 
     * Users may have zero, one or more privacy items.
     * 
     * @return a map where the key is the name of the list and the value the 
     * collection of privacy items.
     */
    // CHECKSTYLE:OFF
	public Map<String, List<PrivacyItem>> getItemLists() {
		return itemLists;
	}
    // CHECKSTYLE:ON

    /** 
     * Returns whether the receiver allows or declines the use of an active list.
     * 
     * @return the decline status of the list.
     */
    // CHECKSTYLE:OFF
	public boolean isDeclineActiveList() {
		return declineActiveList;
	}
    // CHECKSYTLE:ON

    /** 
     * Sets whether the receiver allows or declines the use of an active list.
     * 
     * @param declineActiveList indicates if the receiver declines the use of an active list.
     */
    // CHECKSTYLE:OFF
	public void setDeclineActiveList(boolean declineActiveList) {
		this.declineActiveList = declineActiveList;
	}
    // CHECKSTYLE:ON

    /** 
     * Returns whether the receiver allows or declines the use of a default list.
     * 
     * @return the decline status of the list.
     */
    // CHECKSTYLE:OFF
	public boolean isDeclineDefaultList() {
		return declineDefaultList;
	}
    // CHECKSTYLE:ON

    /** 
     * Sets whether the receiver allows or declines the use of a default list.
     * 
     * @param declineDefaultList indicates if the receiver declines the use of a default list.
     */
    // CHECKSTYLE:OFF
	public void setDeclineDefaultList(boolean declineDefaultList) {
		this.declineDefaultList = declineDefaultList;
	}

	/** 
     * Returns all the list names the user has defined to group restrictions.
     * 
     * @return a Set with Strings containing every list names.
     */
	public Set<String> getPrivacyListNames() {
		return this.itemLists.keySet();
	}
    // CHECKSTYLE:ON

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        // CHECKSTYLE:OFF

        // Add the active tag
        if (this.isDeclineActiveList()) {
        	buf.append("<active/>");
        } else {
        	if (this.getActiveName() != null) {
                buf.append("<active name=\"").escape(getActiveName()).append("\"/>");
            }
        }
        // Add the default tag
        if (this.isDeclineDefaultList()) {
        	buf.append("<default/>");
        } else {
	        if (this.getDefaultName() != null) {
                buf.append("<default name=\"").escape(getDefaultName()).append("\"/>");
	        }
        }

        // Add the list with their privacy items
        for (Map.Entry<String, List<PrivacyItem>> entry : this.getItemLists().entrySet()) {
          String listName = entry.getKey();
          List<PrivacyItem> items = entry.getValue();
			// Begin the list tag
			if (items.isEmpty()) {
                buf.append("<list name=\"").escape(listName).append("\"/>");
			} else {
                buf.append("<list name=\"").escape(listName).append("\">");
			}
	        for (PrivacyItem item : items) {
	        	// Append the item xml representation
	        	buf.append(item.toXML());
	        }
	        // Close the list tag
	        if (!items.isEmpty()) {
				buf.append("</list>");
			}
		}
    // CHECKSTYLE:ON
        return buf;
    }

}
