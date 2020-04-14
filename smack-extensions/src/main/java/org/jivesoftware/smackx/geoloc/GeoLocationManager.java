/**
 *
 * Copyright 2015-2017 Ishan Khanna, Fernando Ramirez 2019-2020 Florian Schmaus
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
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.geoloc.provider.GeoLocationProvider;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.xdata.provider.FormFieldChildElementProviderManager;

import org.jxmpp.jid.Jid;

/**
 * Entry point for Smacks API for XEP-0080: User Location.
 * <br>
 * To publish a UserLocation, please use {@link #publishGeoLocation(GeoLocation)} method. This will publish the node.
 * <br>
 * To stop publishing a UserLocation, please use {@link #stopPublishingGeolocation()} method. This will send a disble publishing signal.
 * <br>
 * To add a {@link PepEventListener} in order to remain updated with other users GeoLocation, use {@link #addGeoLocationListener(PepEventListener)} method.
 * <br>
 * To link a GeoLocation with {@link Message}, use `message.addExtension(geoLocation)`.
 * <br>
 * An example for illustration is provided inside GeoLocationTest inside the test package.
 * <br>
 * @see <a href="https://xmpp.org/extensions/xep-0080.html">
 *     XEP-0080: User Location</a>
 */
public final class GeoLocationManager extends Manager {

    public static final String GEOLOCATION_NODE = GeoLocation.NAMESPACE;

    private static final Map<XMPPConnection, GeoLocationManager> INSTANCES = new WeakHashMap<>();

    private final PepManager pepManager;

    static {
        FormFieldChildElementProviderManager.addFormFieldChildElementProvider(
                        GeoLocationProvider.GeoLocationFormFieldChildElementProvider.INSTANCE);

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Retrieves a {@link GeoLocationManager} for the specified {@link XMPPConnection}, creating one if it doesn't
     * already exist.
     *
     * @param connection The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public static synchronized GeoLocationManager getInstanceFor(XMPPConnection connection) {
        GeoLocationManager geoLocationManager = INSTANCES.get(connection);
        if (geoLocationManager == null) {
            geoLocationManager = new GeoLocationManager(connection);
            INSTANCES.put(connection, geoLocationManager);
        }
        return geoLocationManager;
    }

    private GeoLocationManager(XMPPConnection connection) {
        super(connection);
        pepManager = PepManager.getInstanceFor(connection);
    }

    public void sendGeoLocationToJid(GeoLocation geoLocation, Jid jid) throws InterruptedException,
                    NotConnectedException {

        final XMPPConnection connection = connection();

        Message geoLocationMessage = connection.getStanzaFactory().buildMessageStanza()
                .to(jid)
                .addExtension(geoLocation)
                .build();

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

    /**
     * Publish the user's geographic location through the Personal Eventing Protocol (PEP).
     *
     * @param geoLocation the geographic location to publish.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     */
    public void publishGeoLocation(GeoLocation geoLocation)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, NotALeafNodeException {
        pepManager.publish(GEOLOCATION_NODE, new PayloadItem<GeoLocation>(geoLocation));
    }

    /**
     * Send empty geolocation through the PubSub node.
     *
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     */
    public void stopPublishingGeolocation()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, NotALeafNodeException {
        pepManager.publish(GEOLOCATION_NODE, new PayloadItem<GeoLocation>(GeoLocation.EMPTY_GEO_LOCATION));
    }

    public boolean addGeoLocationListener(PepEventListener<GeoLocation> listener) {
        return pepManager.addPepEventListener(GEOLOCATION_NODE, GeoLocation.class, listener);
    }

    public boolean removeGeoLocationListener(PepEventListener<GeoLocation> listener) {
        return pepManager.removePepEventListener(listener);
    }
}
