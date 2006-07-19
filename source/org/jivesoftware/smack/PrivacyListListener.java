package org.jivesoftware.smack;

import java.util.List;

/**
 * Interface to implement classes to listen for server events about privacy communication. 
 * Listeners are registered with the {@link PrivacyListManager}.
 *
 * @see {@link PrivacyListManager#addListener}
 * 
 * @author Francisco Vives
 */
public interface PrivacyListListener {

    /**
     * Set or update a privacy list with PrivacyItem.
     *
     * @param listName the name of the new or updated privacy list.
     * @param listItem the PrivacyItems that rules the list.
     */
    public void setPrivacyList(String listName, List listItem);

    /**
     * A privacy list has been modified by another. It gets notified.
     *
     * @param listName the name of the updated privacy list.
     */
    public void updatedPrivacyList(String listName);

}