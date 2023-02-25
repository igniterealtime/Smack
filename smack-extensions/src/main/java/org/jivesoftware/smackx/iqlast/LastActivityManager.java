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

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError.Condition;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;

import org.jxmpp.jid.Jid;

/**
 * A last activity manager for handling information about the last activity
 * associated with a Jabber ID. A manager handles incoming LastActivity requests
 * of existing Connections. It also allows to request last activity information
 * of other users.
 *
 * LastActivity (XEP-0012) based on the sending JID's type allows for retrieval
 * of:
 * <ol>
 * <li>How long a particular user has been idle
 * <li>How long a particular user has been logged-out and the message the
 * specified when doing so.
 * <li>How long a host has been up.
 * </ol>
 *
 * For example to get the idle time of a user logged in a resource, simple send
 * the LastActivity stanza to them, as in the following code:
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
 * To get the uptime of a host, you simple send the LastActivity stanza to it,
 * as in the following code example:
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

public final class LastActivityManager extends Manager {
    private static final Map<XMPPConnection, LastActivityManager> instances = new WeakHashMap<>();
//    private static final PacketFilter IQ_GET_LAST_FILTER = new AndFilter(IQTypeFilter.GET,
//                    new StanzaTypeFilter(LastActivity.class));

    private static boolean enabledPerDefault = true;

    /**
     * Enable or disable Last Activity for new XMPPConnections.
     *
     * @param enabledPerDefault TODO javadoc me please
     */
    public static void setEnabledPerDefault(boolean enabledPerDefault) {
        LastActivityManager.enabledPerDefault = enabledPerDefault;
    }

    // Enable the LastActivity support on every established connection
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
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
     * @param connection TODO javadoc me please
     *            The XMPPConnection that the last activity requests will use.
     */
    private LastActivityManager(XMPPConnection connection) {
        super(connection);

        // Listen to all the sent messages to reset the idle time on each one
        connection.addStanzaSendingListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Presence presence = (Presence) packet;
                Presence.Mode mode = presence.getMode();
                if (mode == null) return;
                switch (mode) {
                case available:
                case chat:
                    // We assume that only a switch to available and chat indicates user activity
                    // since other mode changes could be also a result of some sort of automatism
                    resetIdleTime();
                    break;
                default:
                    break;
                }
            }
        }, StanzaTypeFilter.PRESENCE);

        connection.addStanzaSendingListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Message message = (Message) packet;
                // if it's not an error message, reset the idle time
                if (message.getType() == Message.Type.error) return;
                resetIdleTime();
            }
        }, StanzaTypeFilter.MESSAGE);

        // Register a listener for a last activity query
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(LastActivity.ELEMENT, LastActivity.NAMESPACE,
                        IQ.Type.get, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                if (!enabled)
                    return IQ.createErrorResponse(iqRequest, Condition.not_acceptable);
                LastActivity message = new LastActivity();
                message.setType(IQ.Type.result);
                message.setTo(iqRequest.getFrom());
                message.setFrom(iqRequest.getTo());
                message.setStanzaId(iqRequest.getStanzaId());
                message.setLastActivity(getIdleTime());

                return message;
            }
        });

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
        return (now - lms) / 1000;
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
     * @param jid TODO javadoc me please
     *            the JID of the user.
     * @return the LastActivity stanza of the jid.
     * @throws XMPPErrorException if there was an XMPP error returned.
     *             thrown if a server error has occurred.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public LastActivity getLastActivity(Jid jid) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        LastActivity activity = new LastActivity(jid);
        return (LastActivity) connection().sendIqRequestAndWaitForResponse(activity);
    }

    /**
     * Returns true if Last Activity (XEP-0012) is supported by a given JID.
     *
     * @param jid a JID to be tested for Last Activity support
     * @return true if Last Activity is supported, otherwise false
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public boolean isLastActivitySupported(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, LastActivity.NAMESPACE);
    }
}
