/**
 *
 * Copyright 2015-2016 Ishan Khanna
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
package org.jivesoftware.smackx.geoloc;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jxmpp.jid.Jid;

public final class GeoLocationManager extends Manager {

    private static final Map<XMPPConnection, GeoLocationManager> INSTANCES = new WeakHashMap<XMPPConnection, GeoLocationManager>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    public GeoLocationManager(XMPPConnection connection) {
        super(connection);

    }

    /**
     * Retrieves a {@link GeoLocationManager} for the specified {@link XMPPConnection}, creating one if it doesn't
     * already exist.
     * 
     * @param connection The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public synchronized static GeoLocationManager getInstanceFor(XMPPConnection connection) {
        GeoLocationManager geoLocationManager = INSTANCES.get(connection);
        if (geoLocationManager == null) {
            geoLocationManager = new GeoLocationManager(connection);
            INSTANCES.put(connection, geoLocationManager);
        }
        return geoLocationManager;
    }

    public void sendGeoLocationToJid(GeoLocation geoLocation, Jid jid) throws InterruptedException,
                    NotConnectedException {

        final XMPPConnection connection = connection();

        Message geoLocationMessage = new Message(jid);
        geoLocationMessage.addExtension(geoLocation);

        connection.sendStanza(geoLocationMessage);

    }

    /**
     * Returns true if the message contains a GeoLocation extension.
     * 
     * @param message the message to check if contains a GeoLocation extension or not
     * @return a boolean indicating whether the message is a GeoLocation message
     */
    public static boolean isGeoLocationMessage(Message message) {
        return GeoLocation.from(message) != null;
    }

}
