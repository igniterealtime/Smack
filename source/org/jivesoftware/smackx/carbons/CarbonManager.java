/**
 * Copyright 2013 Georg Lukas
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

package org.jivesoftware.smackx.carbons;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering {@link Carbon} support, enabling and disabling
 * message carbons.
 *
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas
 */
public class CarbonManager {

    private static Map<Connection, CarbonManager> instances =
            Collections.synchronizedMap(new WeakHashMap<Connection, CarbonManager>());

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new CarbonManager(connection);
            }
        });
    }
    
    private Connection connection;
    private volatile boolean enabled_state = false;

    private CarbonManager(Connection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(Carbon.NAMESPACE);
        this.connection = connection;
        instances.put(connection, this);
    }

    /**
     * Obtain the CarbonManager responsible for a connection.
     *
     * @param connection the connection object.
     *
     * @return a CarbonManager instance
     */
    public static CarbonManager getInstanceFor(Connection connection) {
        CarbonManager carbonManager = instances.get(connection);

        if (carbonManager == null) {
            carbonManager = new CarbonManager(connection);
        }

        return carbonManager;
    }

    private IQ carbonsEnabledIQ(final boolean new_state) {
        IQ setIQ = new IQ() {
            public String getChildElementXML() {
                return "<" + (new_state? "enable" : "disable") + " xmlns='" + Carbon.NAMESPACE + "'/>";
            }
        };
        setIQ.setType(IQ.Type.SET);
        return setIQ;
    }

    /**
     * Returns true if XMPP Carbons are supported by the server.
     * 
     * @return true if supported
     */
    public boolean isSupportedByServer() {
        try {
            DiscoverInfo result = ServiceDiscoveryManager
                .getInstanceFor(connection).discoverInfo(connection.getServiceName());
            return result.containsFeature(Carbon.NAMESPACE);
        }
        catch (XMPPException e) {
            return false;
        }
    }

    /**
     * Notify server to change the carbons state. This method returns
     * immediately and changes the variable when the reply arrives.
     *
     * You should first check for support using isSupportedByServer().
     *
     * @param new_state whether carbons should be enabled or disabled
     */
    public void sendCarbonsEnabled(final boolean new_state) {
        IQ setIQ = carbonsEnabledIQ(new_state);

        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                IQ result = (IQ)packet;
                if (result.getType() == IQ.Type.RESULT) {
                    enabled_state = new_state;
                }
                connection.removePacketListener(this);
            }
        }, new PacketIDFilter(setIQ.getPacketID()));

        connection.sendPacket(setIQ);
    }

    /**
     * Notify server to change the carbons state. This method blocks
     * some time until the server replies to the IQ and returns true on
     * success.
     *
     * You should first check for support using isSupportedByServer().
     *
     * @param new_state whether carbons should be enabled or disabled
     *
     * @return true if the operation was successful
     */
    public boolean setCarbonsEnabled(final boolean new_state) {
        if (enabled_state == new_state)
            return true;

        IQ setIQ = carbonsEnabledIQ(new_state);

        PacketCollector collector =
                connection.createPacketCollector(new PacketIDFilter(setIQ.getPacketID()));
        connection.sendPacket(setIQ);
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();

        if (result != null && result.getType() == IQ.Type.RESULT) {
            enabled_state = new_state;
            return true;
        }
        return false;
    }

    /**
     * Helper method to enable carbons.
     *
     * @return true if the operation was successful
     */
    public boolean enableCarbons() {
        return setCarbonsEnabled(true);
    }

    /**
     * Helper method to disable carbons.
     *
     * @return true if the operation was successful
     */
    public boolean disableCarbons() {
        return setCarbonsEnabled(false);
    }

    /**
     * Check if carbons are enabled on this connection.
     */
    public boolean getCarbonsEnabled() {
        return this.enabled_state;
    }

    /**
     * Obtain a Carbon from a message, if available.
     *
     * @param msg Message object to check for carbons
     *
     * @return a Carbon if available, null otherwise.
     */
    public static Carbon getCarbon(Message msg) {
        Carbon cc = (Carbon)msg.getExtension("received", Carbon.NAMESPACE);
        if (cc == null)
            cc = (Carbon)msg.getExtension("sent", Carbon.NAMESPACE);
        return cc;
    }

    /**
     * Mark a message as "private", so it will not be carbon-copied.
     *
     * @param msg Message object to mark private
     */
    public static void disableCarbons(Message msg) {
        msg.addExtension(new Carbon.Private());
    }
}
