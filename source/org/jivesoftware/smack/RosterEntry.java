package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.IQ;

import java.util.*;

/**
 * Each user in your roster is represented by a roster entry, which contains the user's
 * JID and a name or nickname you assign.
 *
 * @author Matt Tucker
 */
public class RosterEntry {

    private String user;
    private String name;
    private XMPPConnection connection;

    RosterEntry(String user, String name, XMPPConnection connection) {
        this.user = user;
        this.name = name;
        this.connection = connection;
    }

    /**
     * Returns the JID of the user associated with this entry.
     *
     * @return the user associated with this entry.
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the name associated with this entry.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.SET);
        packet.addRosterItem(toRosterItem(this));
        connection.sendPacket(packet);
    }

    /**
     * Returns an iterator for all the roster groups that this entry belongs to.
     *
     * @return an iterator for the groups this entry belongs to.
     */
    public Iterator getGroups() {
        Roster roster = connection.getRoster();
        List results = new ArrayList();
        // Loop through all roster groups and find the ones that contain this
        // entry. This algorithm should be fine
        for (Iterator i=roster.getGroups(); i.hasNext(); ) {
            RosterGroup group = (RosterGroup)i.next();
            if (group.contains(this)) {
                results.add(group);
            }
        }
        return results.iterator();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && object instanceof RosterEntry) {
            return user.equals(((RosterEntry)object).getUser());
        }
        else {
            return false;
        }
    }

    static RosterPacket.Item toRosterItem(RosterEntry entry) {
        RosterPacket.Item item = new RosterPacket.Item(entry.getUser(), entry.getName());
        item.setItemType(RosterPacket.ItemType.BOTH);
        // Set the correct group names for the item.
        for (Iterator j=entry.getGroups(); j.hasNext(); ) {
            RosterGroup group = (RosterGroup)j.next();
            item.addGroupName(group.getName());
        }
        return item;
    }
}