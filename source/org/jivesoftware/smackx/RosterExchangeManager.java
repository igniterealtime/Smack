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

package org.jivesoftware.smackx;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.RosterExchange;

/**
 *
 * Manages Roster exchanges. A RosterExchangeManager provides a high level access to send 
 * rosters, roster groups and roster entries to XMPP clients. It also provides an easy way
 * to hook up custom logic when entries are received from another XMPP client through 
 * RosterExchangeListeners.
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeManager {

    private List rosterExchangeListeners = new ArrayList();

    private XMPPConnection con;

    private PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:roster");
    private PacketListener packetListener;

    /**
     * Creates a new roster exchange manager.
     *
     * @param con an XMPPConnection.
     */
    public RosterExchangeManager(XMPPConnection con) {
        this.con = con;
        init();
    }

    /**
     * Adds a listener to roster exchanges. The listener will be fired anytime roster entries
     * are received from remote XMPP clients.
     *
     * @param rosterExchangeListener a roster exchange listener.
     */
    public void addRosterListener(RosterExchangeListener rosterExchangeListener) {
        synchronized (rosterExchangeListeners) {
            if (!rosterExchangeListeners.contains(rosterExchangeListener)) {
                rosterExchangeListeners.add(rosterExchangeListener);
            }
        }
    }

    /**
     * Removes a listener from roster exchanges. The listener will be fired anytime roster 
     * entries are received from remote XMPP clients.
     *
     * @param rosterExchangeListener a roster exchange listener..
     */
    public void removeRosterListener(RosterExchangeListener rosterExchangeListener) {
        synchronized (rosterExchangeListeners) {
            rosterExchangeListeners.remove(rosterExchangeListener);
        }
    }

    /**
     * Sends a roster to userID. All the entries of the roster will be sent to the
     * target user.
     * 
     * @param roster the roster to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(Roster roster, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange(roster);
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        con.sendPacket(msg);
    }

    /**
     * Sends a roster entry to userID.
     * 
     * @param rosterEntry the roster entry to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(RosterEntry rosterEntry, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange();
        rosterExchange.addRosterEntry(rosterEntry);
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        con.sendPacket(msg);
    }

    /**
     * Sends a roster group to userID. All the entries of the group will be sent to the 
     * target user.
     * 
     * @param rosterGroup the roster group to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(RosterGroup rosterGroup, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange();
        for (Iterator it = rosterGroup.getEntries(); it.hasNext();)
            rosterExchange.addRosterEntry((RosterEntry) it.next());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        con.sendPacket(msg);
    }

    /**
     * Fires roster exchange listeners.
     */
    private void fireRosterExchangeListeners(String from, Iterator remoteRosterEntries) {
        RosterExchangeListener[] listeners = null;
        synchronized (rosterExchangeListeners) {
            listeners = new RosterExchangeListener[rosterExchangeListeners.size()];
            rosterExchangeListeners.toArray(listeners);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].entriesReceived(from, remoteRosterEntries);
        }
    }

    private void init() {
        // Listens for all roster exchange packets and fire the roster exchange listeners.
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                RosterExchange rosterExchange =
                    (RosterExchange) message.getExtension("x", "jabber:x:roster");
                // Fire event for roster exchange listeners
                fireRosterExchangeListeners(message.getFrom(), rosterExchange.getRosterEntries());
            };

        };
        con.addPacketListener(packetListener, packetFilter);
    }

    public void destroy() {
        if (con != null)
            con.removePacketListener(packetListener);

    }
    public void finalize() {
        destroy();
    }
}