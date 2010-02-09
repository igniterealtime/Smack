/**
 * $RCSfile: PEPManager.java,v $
 * $Revision: 1.4 $
 * $Date: 2007/11/06 21:43:40 $
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.packet.PEPEvent;
import org.jivesoftware.smackx.packet.PEPItem;
import org.jivesoftware.smackx.packet.PEPPubSub;

/**
 *
 * Manages Personal Event Publishing (XEP-163). A PEPManager provides a high level access to
 * pubsub personal events. It also provides an easy way
 * to hook up custom logic when events are received from another XMPP client through PEPListeners.
 * 
 * Use example:
 * 
 * <pre>
 *   PEPManager pepManager = new PEPManager(smackConnection);
 *   pepManager.addPEPListener(new PEPListener() {
 *       public void eventReceived(String inFrom, PEPEvent inEvent) {
 *           LOGGER.debug("Event received: " + inEvent);
 *       }
 *   });
 *
 *   PEPProvider pepProvider = new PEPProvider();
 *   pepProvider.registerPEPParserExtension("http://jabber.org/protocol/tune", new TuneProvider());
 *   ProviderManager.getInstance().addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event", pepProvider);
 *   
 *   Tune tune = new Tune("jeff", "1", "CD", "My Title", "My Track");
 *   pepManager.publish(tune);
 * </pre>
 * 
 * @author Jeff Williams
 */
public class PEPManager {

    private List<PEPListener> pepListeners = new ArrayList<PEPListener>();

    private Connection connection;

    private PacketFilter packetFilter = new PacketExtensionFilter("event", "http://jabber.org/protocol/pubsub#event");
    private PacketListener packetListener;

    /**
     * Creates a new PEP exchange manager.
     *
     * @param connection a Connection which is used to send and receive messages.
     */
    public PEPManager(Connection connection) {
        this.connection = connection;
        init();
    }

    /**
     * Adds a listener to PEPs. The listener will be fired anytime PEP events
     * are received from remote XMPP clients.
     *
     * @param pepListener a roster exchange listener.
     */
    public void addPEPListener(PEPListener pepListener) {
        synchronized (pepListeners) {
            if (!pepListeners.contains(pepListener)) {
                pepListeners.add(pepListener);
            }
        }
    }

    /**
     * Removes a listener from PEP events.
     *
     * @param pepListener a roster exchange listener.
     */
    public void removePEPListener(PEPListener pepListener) {
        synchronized (pepListeners) {
            pepListeners.remove(pepListener);
        }
    }

    /**
     * Publish an event.
     * 
     * @param item the item to publish.
     */
    public void publish(PEPItem item) {
        // Create a new message to publish the event.
        PEPPubSub pubSub = new PEPPubSub(item);
        pubSub.setType(Type.SET);
        //pubSub.setFrom(connection.getUser());
 
        // Send the message that contains the roster
        connection.sendPacket(pubSub);
    }

    /**
     * Fires roster exchange listeners.
     */
    private void firePEPListeners(String from, PEPEvent event) {
        PEPListener[] listeners = null;
        synchronized (pepListeners) {
            listeners = new PEPListener[pepListeners.size()];
            pepListeners.toArray(listeners);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].eventReceived(from, event);
        }
    }

    private void init() {
        // Listens for all roster exchange packets and fire the roster exchange listeners.
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                PEPEvent event = (PEPEvent) message.getExtension("event", "http://jabber.org/protocol/pubsub#event");
                // Fire event for roster exchange listeners
                firePEPListeners(message.getFrom(), event);
            };

        };
        connection.addPacketListener(packetListener, packetFilter);
    }

    public void destroy() {
        if (connection != null)
            connection.removePacketListener(packetListener);

    }

    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
}