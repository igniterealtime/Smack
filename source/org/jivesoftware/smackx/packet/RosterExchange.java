/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.packet;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.PacketExtension;

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

    private List remoteRosterEntries = new ArrayList();

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
        for (Iterator rosterEntries = roster.getEntries(); rosterEntries.hasNext();) {
            this.addRosterEntry((RosterEntry) rosterEntries.next());
        }
    }

    /**
     * Adds a roster entry to the packet.
     *
     * @param rosterEntry a roster entry to add.
     */
    public void addRosterEntry(RosterEntry rosterEntry) {
        RosterGroup rosterGroup = null;
        // Create a new Entry based on the rosterEntry and add it to the packet
        RemoteRosterEntry remoteRosterEntry = new RemoteRosterEntry(rosterEntry.getUser(), rosterEntry.getName());
        // Add the entry groups to the Entry
        for (Iterator groups = rosterEntry.getGroups(); groups.hasNext();) {
            rosterGroup = (RosterGroup) groups.next();
            remoteRosterEntry.addGroupName(rosterGroup.getName());
        }
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
    public Iterator getRosterEntries() {
        synchronized (remoteRosterEntries) {
            List entries = Collections.unmodifiableList(new ArrayList(remoteRosterEntries));
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
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        // Loop through all roster entries and append them to the string buffer
        for (Iterator i = getRosterEntries(); i.hasNext();) {
            RemoteRosterEntry remoteRosterEntry = (RemoteRosterEntry) i.next();
            buf.append(remoteRosterEntry.toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

}
