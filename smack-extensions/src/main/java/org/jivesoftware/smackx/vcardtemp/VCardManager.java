/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smackx.vcardtemp;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

public class VCardManager extends Manager {
    public static final String NAMESPACE = VCard.NAMESPACE;
    public static final String ELEMENT = VCard.ELEMENT;

    private static final Map<XMPPConnection, VCardManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Retrieves a {@link VCardManager} for the specified {@link XMPPConnection}, creating one if it doesn't already
     * exist.
     * 
     * @param connection the connection the manager is attached to.
     * @return The new or existing manager.
     */
    public static synchronized VCardManager getInstanceFor(XMPPConnection connection) {
        VCardManager vcardManager = INSTANCES.get(connection);
        if (vcardManager == null) {
            vcardManager = new VCardManager(connection);
            INSTANCES.put(connection, vcardManager);
        }
        return vcardManager;
    }

    /**
     * Returns true if the given entity understands the vCard-XML format and allows the exchange of such.
     * 
     * @param jid
     * @param connection
     * @return true if the given entity understands the vCard-XML format and exchange.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @deprecated use {@link #isSupported(String)} instead.
     */
    @Deprecated
    public static boolean isSupported(String jid, XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        return VCardManager.getInstanceFor(connection).isSupported(jid);
    }

    private VCardManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
    }

    /**
     * Save this vCard for the user connected by 'connection'. XMPPConnection should be authenticated
     * and not anonymous.
     *
     * @throws XMPPErrorException thrown if there was an issue setting the VCard in the server.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void saveVCard(VCard vcard) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        vcard.setType(IQ.Type.set);
        connection().createPacketCollectorAndSend(vcard).nextResultOrThrow();
    }

    /**
     * Load the VCard of the current user.
     *
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public VCard loadVCard() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return loadVCard(null);
    }

    /**
     * Load VCard information for a given user.
     *
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public VCard loadVCard(String bareJid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        VCard vcardRequest = new VCard();
        vcardRequest.setTo(bareJid);
        VCard result = connection().createPacketCollectorAndSend(vcardRequest).nextResultOrThrow();
        return result;
    }

    /**
     * Returns true if the given entity understands the vCard-XML format and allows the exchange of such.
     * 
     * @param jid
     * @return true if the given entity understands the vCard-XML format and exchange.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean isSupported(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, NAMESPACE);
    }
}
