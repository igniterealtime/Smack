/**
 *
 * Copyright 2013 Georg Lukas
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
package org.jivesoftware.smackx.carbons;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering {@link CarbonExtension} support, enabling and disabling
 * message carbons.
 *
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas
 */
public class CarbonManager extends Manager {

    private static Map<XMPPConnection, CarbonManager> instances =
            Collections.synchronizedMap(new WeakHashMap<XMPPConnection, CarbonManager>());

    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }
    
    private volatile boolean enabled_state = false;

    private CarbonManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(CarbonExtension.NAMESPACE);
        instances.put(connection, this);
    }

    /**
     * Obtain the CarbonManager responsible for a connection.
     *
     * @param connection the connection object.
     *
     * @return a CarbonManager instance
     */
    public static synchronized CarbonManager getInstanceFor(XMPPConnection connection) {
        CarbonManager carbonManager = instances.get(connection);

        if (carbonManager == null) {
            carbonManager = new CarbonManager(connection);
        }

        return carbonManager;
    }

    private IQ carbonsEnabledIQ(final boolean new_state) {
        IQ setIQ = new IQ() {
            public String getChildElementXML() {
                return "<" + (new_state? "enable" : "disable") + " xmlns='" + CarbonExtension.NAMESPACE + "'/>";
            }
        };
        setIQ.setType(IQ.Type.SET);
        return setIQ;
    }

    /**
     * Returns true if XMPP Carbons are supported by the server.
     * 
     * @return true if supported
     * @throws SmackException if there was no response from the server.
     * @throws XMPPException 
     */
    public boolean isSupportedByServer() throws XMPPException, SmackException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(
                        connection().getServiceName(), CarbonExtension.NAMESPACE);
    }

    /**
     * Notify server to change the carbons state. This method returns
     * immediately and changes the variable when the reply arrives.
     *
     * You should first check for support using isSupportedByServer().
     *
     * @param new_state whether carbons should be enabled or disabled
     * @throws NotConnectedException 
     */
    public void sendCarbonsEnabled(final boolean new_state) throws NotConnectedException {
        IQ setIQ = carbonsEnabledIQ(new_state);

        connection().addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                IQ result = (IQ)packet;
                if (result.getType() == IQ.Type.RESULT) {
                    enabled_state = new_state;
                }
                connection().removePacketListener(this);
            }
        }, new IQReplyFilter(setIQ, connection()));

        connection().sendPacket(setIQ);
    }

    /**
     * Notify server to change the carbons state. This method blocks
     * some time until the server replies to the IQ and returns true on
     * success.
     *
     * You should first check for support using isSupportedByServer().
     *
     * @param new_state whether carbons should be enabled or disabled
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     *
     */
    public synchronized void setCarbonsEnabled(final boolean new_state) throws NoResponseException,
                    XMPPErrorException, NotConnectedException {
        if (enabled_state == new_state)
            return;

        IQ setIQ = carbonsEnabledIQ(new_state);

        connection().createPacketCollectorAndSend(setIQ).nextResultOrThrow();
        enabled_state = new_state;
    }

    /**
     * Helper method to enable carbons.
     *
     * @throws XMPPException 
     * @throws SmackException if there was no response from the server.
     */
    public void enableCarbons() throws XMPPException, SmackException {
        setCarbonsEnabled(true);
    }

    /**
     * Helper method to disable carbons.
     *
     * @throws XMPPException 
     * @throws SmackException if there was no response from the server.
     */
    public void disableCarbons() throws XMPPException, SmackException {
        setCarbonsEnabled(false);
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
    public static CarbonExtension getCarbon(Message msg) {
        CarbonExtension cc = (CarbonExtension)msg.getExtension("received", CarbonExtension.NAMESPACE);
        if (cc == null)
            cc = (CarbonExtension)msg.getExtension("sent", CarbonExtension.NAMESPACE);
        return cc;
    }

    /**
     * Mark a message as "private", so it will not be carbon-copied.
     *
     * @param msg Message object to mark private
     */
    public static void disableCarbons(Message msg) {
        msg.addExtension(new CarbonExtension.Private());
    }
}
