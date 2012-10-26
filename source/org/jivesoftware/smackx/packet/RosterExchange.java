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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.RemoteRosterEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents XMPP Roster Item Exchange packets.<p>
 * 
 * The 'jabber:x:roster' namespace (which is not to be confused with the 'jabber:iq:roster' 
 * namespace) is used to send roster items from one client to another. A roster item is sent by 
 * adding to the &lt;message/&gt; element an &lt;x/&gt; child scoped by the 'jabber:x:roster' namespace. This 
 * &lt;x/&gt; element may contain one or more &lt;item/&gt; children (one for each roster item to be sent).<p>
 * 
 * Each &lt;item/&gt; element may possess the following attributes:<p>
 * 
 * &lt;jid/&gt; -- The id of the contact being sent. This attribute is required.<br>
 * &lt;name/&gt; -- A natural-language nickname for the contact. This attribute is optional.<p>
 * 
 * Each &lt;item/&gt; element may also contain one or more &lt;group/&gt; children specifying the 
 * natural-language name of a user-specified group, for the purpose of categorizing this contact 
 * into one or more roster groups.
 *
 * @author Gaston Dombiak
 */
public class RosterExchange implements PacketExtension {

    private List<RemoteRosterEntry> remoteRosterEntries = new ArrayList<RemoteRosterEntry>();

    /**
     * Creates a new empty roster exchange package.
     *
     */
    public RosterExchange() {
        super();
    }

    /**
     * Creates a new roster exchange package with the entries specified in roster.
     *
     * @param roster the roster to send to other XMPP entity.
     */
    public RosterExchange(Roster roster) {
        // Add all the roster entries to the new RosterExchange 
        for (RosterEntry rosterEntry : roster.getEntries()) {
            this.addRosterEntry(rosterEntry);
        }
    }

    /**
     * Adds a roster entry to the packet.
     *
     * @param rosterEntry a roster entry to add.
     */
    public void addRosterEntry(RosterEntry rosterEntry) {
		// Obtain a String[] from the roster entry groups name 
		List<String> groupNamesList = new ArrayList<String>();
		String[] groupNames;
		for (RosterGroup group : rosterEntry.getGroups()) {
			groupNamesList.add(group.getName());
		}
		groupNames = groupNamesList.toArray(new String[groupNamesList.size()]);

        // Create a new Entry based on the rosterEntry and add it to the packet
        RemoteRosterEntry remoteRosterEntry = new RemoteRosterEntry(rosterEntry.getUser(),
                rosterEntry.getName(), groupNames);
		
        addRosterEntry(remoteRosterEntry);
    }

    /**
     * Adds a remote roster entry to the packet.
     *
     * @param remoteRosterEntry a remote roster entry to add.
     */
    public void addRosterEntry(RemoteRosterEntry remoteRosterEntry) {
        synchronized (remoteRosterEntries) {
            remoteRosterEntries.add(remoteRosterEntry);
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
     * Returns an Iterator for the roster entries in the packet.
     *
     * @return an Iterator for the roster entries in the packet.
     */
    public Iterator<RemoteRosterEntry> getRosterEntries() {
        synchronized (remoteRosterEntries) {
            List<RemoteRosterEntry> entries = Collections.unmodifiableList(new ArrayList<RemoteRosterEntry>(remoteRosterEntries));
            return entries.iterator();
        }
    }

    /**
     * Returns a count of the entries in the roster exchange.
     *
     * @return the number of entries in the roster exchange.
     */
    public int getEntryCount() {
        return remoteRosterEntries.size();
    }

    /**
     * Returns the XML representation of a Roster Item Exchange according the specification.
     * 
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following example:
     * <pre>
     * &lt;message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack"&gt;
     *     &lt;subject&gt;Any subject you want&lt;/subject&gt;
     *     &lt;body&gt;This message contains roster items.&lt;/body&gt;
     *     &lt;x xmlns="jabber:x:roster"&gt;
     *         &lt;item jid="gato1@gato.home"/&gt;
     *         &lt;item jid="gato2@gato.home"/&gt;
     *     &lt;/x&gt;
     * &lt;/message&gt;
     * </pre>
     * 
     */
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        // Loop through all roster entries and append them to the string buffer
        for (Iterator<RemoteRosterEntry> i = getRosterEntries(); i.hasNext();) {
            RemoteRosterEntry remoteRosterEntry = i.next();
            buf.append(remoteRosterEntry.toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

}
