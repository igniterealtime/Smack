/*
 * Created on 27/07/2003
 */
package org.jivesoftware.smackx.packet;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents XMPP Roster Item Exchange packets.
 * The 'jabber:x:roster' namespace (which is not to be confused with the 'jabber:iq:roster' namespace) is used to 
 * send roster items from one client to another. A roster item is sent by adding to the <message/> element an <x/> child 
 * scoped by the 'jabber:x:roster' namespace. This <x/> element may contain one or more <item/> children (one for each roster item to be sent).
 * 
 * Each <item/> element may possess the following attributes:
 * 
 * <jid/> -- The id of the contact being sent. This attribute is required.
 * <name/> -- A natural-language nickname for the contact. This attribute is optional.
 * 
 * Each <item/> element may also contain one or more <group/> children specifying the natural-language name 
 * of a user-specified group, for the purpose of categorizing this contact into one or more roster groups.
 *
 * @author Gaston Dombiak
 */
public class RosterExchange implements PacketExtension {

    private List rosterItems = new ArrayList();

	/**
	 * Creates a new empty roster exchange package.
	 *
	 */
    public RosterExchange(){
        super();
    }

	/**
	 * Creates a new roster exchange package with the entries specified in roster.
	 *
	 * @param roster the roster to send to other XMPP entity.
	 */
    public RosterExchange(Roster roster){
        RosterGroup rosterGroup = null;
        // Loop through all roster groups and add their entries to the new RosterExchange 
        for (Iterator groups = roster.getGroups(); groups.hasNext(); ) {
            rosterGroup = (RosterGroup) groups.next();
            for (Iterator entries = rosterGroup.getEntries(); entries.hasNext(); ) {
                this.addRosterItem((RosterEntry) entries.next());
            }
        }
        // Add the roster unfiled entries to the new RosterExchange 
        for (Iterator unfiledEntries = roster.getUnfiledEntries(); unfiledEntries.hasNext();) {
            this.addRosterItem((RosterEntry) unfiledEntries.next());
        }
    }
    
    /**
     * Adds a roster entry to the packet.
     *
     * @param rosterEntry a roster item.
     */
    public void addRosterItem(RosterEntry rosterEntry) {
        RosterGroup rosterGroup = null;  
        // Create a new Item based on the rosterEntry and add it to the packet
        Item item = new Item(rosterEntry.getUser(), rosterEntry.getName());
        // Add the entry groups to the item
        for (Iterator groups = rosterEntry.getGroups(); groups.hasNext(); ) {
            rosterGroup = (RosterGroup) groups.next();
            item.addGroupName(rosterGroup.getName());
        }
        addRosterItem(item);
    }

    /**
     * Adds a roster item to the packet.
     *
     * @param item a roster item.
     */
    public void addRosterItem(Item item) {
        synchronized (rosterItems) {
            rosterItems.add(item);
        }
    }
    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "x"
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return "x";
    }

    /** 
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "jabber:x:roster"
     * (which is not to be confused with the 'jabber:iq:roster' namespace
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return "jabber:x:roster";
    }

    /**
     * Returns an Iterator for the roster items in the packet.
     *
     * @return and Iterator for the roster items in the packet.
     */
    public Iterator getRosterItems() {
        synchronized (rosterItems) {
            List entries =
                Collections.unmodifiableList(new ArrayList(rosterItems));
            return entries.iterator();
        }
    }

    /**
     * Returns the XML representation of a Roster Item Exchange according the specification.
     *
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following example:
     * <message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack">
     *     <subject>Any subject you want</subject>
     *     <body>This message contains roster items.</body>
     *     <x xmlns="jabber:x:roster">
     *         <item jid="gato1@gato.home"/>
     *         <item jid="gato2@gato.home"/>
     *     </x>
     * </message>
     * 
     */
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
        // Loop through all roster items and append them to the string buffer
        for (Iterator i = getRosterItems(); i.hasNext();) {
           Item entry = (Item) i.next();
           buf.append(entry.toXML());
       }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }
    
    /**
     * A roster item, which consists of a JID and , their name and
     * the groups the roster item belongs to.
     */
    public static class Item {

        private String user;
        private String name;
        private List groupNames;

        /**
         * Creates a new roster item.
         *
         * @param user the user.
         * @param name the user's name.
         */
        public Item(String user, String name) {
            this.user = user;
            this.name = name;
            groupNames = new ArrayList();
        }

        /**
         * Returns the user.
         *
         * @return the user.
         */
        public String getUser() {
            return user;
        }

        /**
         * Returns the user's name.
         *
         * @return the user's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the user's name.
         *
         * @param name the user's name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns an Iterator for the group names (as Strings) that the roster item
         * belongs to.
         *
         * @return an Iterator for the group names.
         */
        public Iterator getGroupNames() {
            synchronized (groupNames) {
                return Collections.unmodifiableList(groupNames).iterator();
            }
        }

		/**
		 * Returns a String array for the group names that the roster item
		 * belongs to.
		 *
		 * @return a String[] for the group names.
		 */
		public String[] getGroupArrayNames() {
			synchronized (groupNames) {
				return (String[])(Collections.unmodifiableList(groupNames).toArray(new String[groupNames.size()]));
			}
		}

        /**
         * Adds a group name.
         *
         * @param groupName the group name.
         */
        public void addGroupName(String groupName) {
            synchronized (groupNames) {
                if (!groupNames.contains(groupName)) {
                    groupNames.add(groupName);
                }
            }
        }

        /**
         * Removes a group name.
         *
         * @param groupName the group name.
         */
        public void removeGroupName(String groupName) {
            synchronized (groupNames) {
                groupNames.remove(groupName);
            }
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<item jid=\"").append(user).append("\"");
            if (name != null) {
                buf.append(" name=\"").append(name).append("\"");
            }
            buf.append(">");
            synchronized (groupNames) {
                for (int i = 0; i < groupNames.size(); i++) {
                    String groupName = (String) groupNames.get(i);
                    buf.append("<group>").append(groupName).append("</group>");
                }
            }
            buf.append("</item>");
            return buf.toString();
        }
    }

}
