package org.jivesoftware.smack;

import java.util.List;

import org.jivesoftware.smack.packet.Privacy;
import org.jivesoftware.smack.packet.PrivacyItem;

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
