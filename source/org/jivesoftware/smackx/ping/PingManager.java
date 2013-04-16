/**
 * Copyright 2012-2013 Florian Schmaus
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

package org.jivesoftware.smackx.ping;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackError;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.keepalive.KeepAliveManager;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.ping.packet.Ping;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

/**
 * Implements the XMPP Ping as defined by XEP-0199.  The XMPP Ping protocol 
 * allows one entity to 'ping' any other entity by simply sending a ping to 
 * the appropriate JID.
 * <p>
 * NOTE: The {@link KeepAliveManager} already provides a keepalive functionality 
 * for regularly pinging the server to keep the underlying transport connection
 * alive.  This class is specifically intended to do manual pings of other 
 * entities.  
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0199.html">XEP-0199:XMPP
 *      Ping</a>
 */
public class PingManager {
    private static Map<Connection, PingManager> instances = Collections
            .synchronizedMap(new WeakHashMap<Connection, PingManager>());
    
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new PingManager(connection);
            }
        });
    }

    private Connection connection;

    /**
     * Retrieves a {@link PingManager} for the specified {@link Connection}, creating one if it doesn't already
     * exist.
     * 
     * @param connection
     * The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public synchronized static PingManager getInstanceFor(Connection connection) {
        PingManager pingManager = instances.get(connection);

        if (pingManager == null) {
            pingManager = new PingManager(connection);
        }
        return pingManager;
    }

    private PingManager(Connection con) {
        this.connection = con;
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        
        // The ServiceDiscoveryManager was not pre-initialized
        if (sdm == null)
            sdm = new ServiceDiscoveryManager(connection);
        
        sdm.addFeature(Ping.NAMESPACE);
        
        PacketFilter pingPacketFilter = new AndFilter(new PacketTypeFilter(Ping.class), new IQTypeFilter(Type.GET));
        
        connection.addPacketListener(new PacketListener() {
            /**
             * Sends a Pong for every Ping
             */
            public void processPacket(Packet packet) {
                IQ pong = IQ.createResultIQ((Ping) packet);
                connection.sendPacket(pong);
            }
        }, pingPacketFilter);
    }

    /**
     * Pings the given jid. This method will return false if an error occurs.  The exception 
     * to this, is a server ping, which will always return true if the server is reachable, 
     * event if there is an error on the ping itself (i.e. ping not supported).
     * <p>
     * Use {@link #isPingSupported(String)} to determine if XMPP Ping is supported 
     * by the entity.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @param pingTimeout The time to wait for a reply
     * @return true if a reply was received from the entity, false otherwise.
     */
    public boolean ping(String jid, long pingTimeout) {
        Ping ping = new Ping(jid);
        
        try {
            SyncPacketSend.getReply(connection, ping);
        }
        catch (XMPPException exc) {
            
            return (jid.equals(connection.getServiceName()) && (exc.getSmackError() != SmackError.NO_RESPONSE_FROM_SERVER));
        }
        return true;
    }
    
    /**
     * Same as calling {@link #ping(String, long)} with the defaultpacket reply 
     * timeout.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @return true if a reply was received from the entity, false otherwise.
     */
    public boolean ping(String jid) {
        return ping(jid, SmackConfiguration.getPacketReplyTimeout());
    }
    
    /**
     * Query the specified entity to see if it supports the Ping protocol (XEP-0199)
     * 
     * @param jid The id of the entity the query is being sent to
     * @return true if it supports ping, false otherwise.
     * @throws XMPPException An XMPP related error occurred during the request 
     */
    public boolean isPingSupported(String jid) throws XMPPException {
        DiscoverInfo result = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(jid);
        return result.containsFeature(Ping.NAMESPACE);
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(String)} case, this method will return true even if 
     * {@link #isPingSupported(String)} is false.
     * 
     * @return true if a reply was received from the server, false otherwise.
     */
    public boolean pingMyServer() {
        return ping(connection.getServiceName());
    }
}
