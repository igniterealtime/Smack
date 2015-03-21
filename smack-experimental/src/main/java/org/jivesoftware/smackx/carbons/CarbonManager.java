/**
 *
 * Copyright 2013-2014 Georg Lukas
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

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.carbons.packet.Carbon;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension.Private;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * Stanza(/Packet) extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering {@link CarbonExtension} support, enabling and disabling
 * message carbons.
 *
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas
 */
public class CarbonManager extends Manager {

    private static Map<XMPPConnection, CarbonManager> INSTANCES = new WeakHashMap<XMPPConnection, CarbonManager>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
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
    }

    /**
     * Obtain the CarbonManager responsible for a connection.
     *
     * @param connection the connection object.
     *
     * @return a CarbonManager instance
     */
    public static synchronized CarbonManager getInstanceFor(XMPPConnection connection) {
        CarbonManager carbonManager = INSTANCES.get(connection);

        if (carbonManager == null) {
            carbonManager = new CarbonManager(connection);
            INSTANCES.put(connection, carbonManager);
        }

        return carbonManager;
    }

    private static IQ carbonsEnabledIQ(final boolean new_state) {
        IQ request;
        if (new_state) {
            request = new Carbon.Enable();
        } else {
            request = new Carbon.Disable();
        }
        return request;
    }

    /**
     * Returns true if XMPP Carbons are supported by the server.
     * 
     * @return true if supported
     * @throws NotConnectedException 
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     */
    public boolean isSupportedByServer() throws NoResponseException, XMPPErrorException, NotConnectedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(CarbonExtension.NAMESPACE);
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

        connection().sendIqWithResponseCallback(setIQ, new StanzaListener() {
            public void processPacket(Stanza packet) {
                enabled_state = new_state;
            }
        });
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
     * Mark a message as "private", so it will not be carbon-copied.
     *
     * @param msg Message object to mark private
     * @deprecated use {@link Private#addTo(Message)}
     */
    @Deprecated
    public static void disableCarbons(Message msg) {
        msg.addExtension(Private.INSTANCE);
    }
}
