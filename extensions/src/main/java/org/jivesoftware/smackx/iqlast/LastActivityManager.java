/**
 *
 * Copyright 2003-2006 Jive Software, 2014 Florian Schmaus
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

package org.jivesoftware.smackx.iqlast;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;

/**
 * A last activity manager for handling information about the last activity
 * associated with a Jabber ID. A manager handles incoming LastActivity requests
 * of existing Connections. It also allows to request last activity information
 * of other users.
 * <p>
 * 
 * LastActivity (XEP-0012) based on the sending JID's type allows for retrieval
 * of:
 * <ol>
 * <li>How long a particular user has been idle
 * <li>How long a particular user has been logged-out and the message the
 * specified when doing so.
 * <li>How long a host has been up.
 * </ol>
 * <p/>
 * 
 * For example to get the idle time of a user logged in a resource, simple send
 * the LastActivity packet to them, as in the following code:
 * <p>
 * 
 * <pre>
 * XMPPConnection con = new XMPPTCPConnection(&quot;jabber.org&quot;);
 * con.login(&quot;john&quot;, &quot;doe&quot;);
 * LastActivity activity = LastActivity.getLastActivity(con, &quot;xray@jabber.org/Smack&quot;);
 * </pre>
 * 
 * To get the lapsed time since the last user logout is the same as above but
 * with out the resource:
 * 
 * <pre>
 * LastActivity activity = LastActivity.getLastActivity(con, &quot;xray@jabber.org&quot;);
 * </pre>
 * 
 * To get the uptime of a host, you simple send the LastActivity packet to it,
 * as in the following code example:
 * <p>
 * 
 * <pre>
 * LastActivity activity = LastActivity.getLastActivity(con, &quot;jabber.org&quot;);
 * </pre>
 * 
 * @author Gabriel Guardincerri
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0012.html">XEP-0012: Last
 *      Activity</a>
 */

public class LastActivityManager extends Manager {
    private static final Map<XMPPConnection, LastActivityManager> instances = new WeakHashMap<XMPPConnection, LastActivityManager>();
    private static final PacketFilter IQ_GET_LAST_FILTER = new AndFilter(new IQTypeFilter(
                    IQ.Type.GET), new PacketTypeFilter(LastActivity.class));

    private static boolean enabledPerDefault = true;

    /**
     * Enable or disable Last Activity for new XMPPConnections.
     *
     * @param enabledPerDefault
     */
    public static void setEnabledPerDefault(boolean enabledPerDefault) {
        LastActivityManager.enabledPerDefault = enabledPerDefault;
    }

    // Enable the LastActivity support on every established connection
    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                LastActivityManager.getInstanceFor(connection);
            }
        });
    }

    public static synchronized LastActivityManager getInstanceFor(XMPPConnection connection) {
        LastActivityManager lastActivityManager = instances.get(connection);
        if (lastActivityManager == null)
            lastActivityManager = new LastActivityManager(connection);
        return lastActivityManager;
    }

    private volatile long lastMessageSent;
    private boolean enabled = false;

    /**
     * Creates a last activity manager to response last activity requests.
     * 
     * @param connection
     *            The XMPPConnection that the last activity requests will use.
     */
    private LastActivityManager(XMPPConnection connection) {
        super(connection);

        // Listen to all the sent messages to reset the idle time on each one
        connection.addPacketSendingListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Presence presence = (Presence) packet;
                Presence.Mode mode = presence.getMode();
                if (mode == null) return;
                switch (mode) {
                case available:
                case chat:
                    // We assume that only a switch to available and chat indicates user activity
                    // since other mode changes could be also a result of some sort of automatism
                    resetIdleTime();
                default:
                    break;
                }
            }
        }, PacketTypeFilter.PRESENCE);

        connection.addPacketSendingListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                // if it's not an error message, reset the idle time
                if (message.getType() == Message.Type.error) return;
                resetIdleTime();
            }
        }, PacketTypeFilter.MESSAGE);

        // Register a listener for a last activity query
        connection.addPacketListener(new PacketListener() {

            public void processPacket(Packet packet) throws NotConnectedException {
                if (!enabled) return;
                LastActivity message = new LastActivity();
                message.setType(IQ.Type.RESULT);
                message.setTo(packet.getFrom());
                message.setFrom(packet.getTo());
                message.setPacketID(packet.getPacketID());
                message.setLastActivity(getIdleTime());

                connection().sendPacket(message);
            }

        }, IQ_GET_LAST_FILTER);

        if (enabledPerDefault) {
            enable();
        }
        resetIdleTime();
        instances.put(connection, this);
    }

    public synchronized void enable() {
        ServiceDiscoveryManager.getInstanceFor(connection()).addFeature(LastActivity.NAMESPACE);
        enabled = true;
    }

    public synchronized void disable() {
        ServiceDiscoveryManager.getInstanceFor(connection()).removeFeature(LastActivity.NAMESPACE);
        enabled = false;
    }

    /**
     * Resets the idle time to 0, this should be invoked when a new message is
     * sent.
     */
    private void resetIdleTime() {
        lastMessageSent = System.currentTimeMillis();
    }

    /**
     * The idle time is the lapsed time between the last message sent and now.
     * 
     * @return the lapsed time between the last message sent and now.
     */
    private long getIdleTime() {
        long lms = lastMessageSent;
        long now = System.currentTimeMillis();
        return ((now - lms) / 1000);
    }

    /**
     * Returns the last activity of a particular jid. If the jid is a full JID
     * (i.e., a JID of the form of 'user@host/resource') then the last activity
     * is the idle time of that connected resource. On the other hand, when the
     * jid is a bare JID (e.g. 'user@host') then the last activity is the lapsed
     * time since the last logout or 0 if the user is currently logged in.
     * Moreover, when the jid is a server or component (e.g., a JID of the form
     * 'host') the last activity is the uptime.
     * 
     * @param jid
     *            the JID of the user.
     * @return the LastActivity packet of the jid.
     * @throws XMPPErrorException
     *             thrown if a server error has occured.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public LastActivity getLastActivity(String jid) throws NoResponseException, XMPPErrorException,
                    NotConnectedException {
        LastActivity activity = new LastActivity(jid);
        return (LastActivity) connection().createPacketCollectorAndSend(activity).nextResultOrThrow();
    }

    /**
     * Returns true if Last Activity (XEP-0012) is supported by a given JID
     * 
     * @param jid a JID to be tested for Last Activity support
     * @return true if Last Activity is supported, otherwise false
     * @throws NotConnectedException 
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     */
    public boolean isLastActivitySupported(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, LastActivity.NAMESPACE);
    }
}
