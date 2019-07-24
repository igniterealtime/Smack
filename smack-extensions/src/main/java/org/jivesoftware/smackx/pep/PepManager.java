/**
 *
 * Copyright 2003-2007 Jive Software, 2015-2018 Florian Schmaus
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

package org.jivesoftware.smackx.pep;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.jidtype.AbstractJidTypeFilter.JidType;
import org.jivesoftware.smack.filter.jidtype.FromJidTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubFeature;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.filter.EventExtensionFilter;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

/**
 *
 * Manages Personal Event Publishing (XEP-163). A PEPManager provides a high level access to
 * PubSub personal events. It also provides an easy way
 * to hook up custom logic when events are received from another XMPP client through PEPListeners.
 *
 * Use example:
 *
 * <pre>
 *   PepManager pepManager = PepManager.getInstanceFor(smackConnection);
 *   pepManager.addPepListener(new PepListener() {
 *       public void eventReceived(EntityBareJid from, EventElement event, Message message) {
 *           LOGGER.debug("Event received: " + event);
 *       }
 *   });
 * </pre>
 *
 * @author Jeff Williams
 * @author Florian Schmaus
 */
public final class PepManager extends Manager {

    private static final Map<XMPPConnection, PepManager> INSTANCES = new WeakHashMap<>();

    public static synchronized PepManager getInstanceFor(XMPPConnection connection) {
        PepManager pepManager = INSTANCES.get(connection);
        if (pepManager == null) {
            pepManager = new PepManager(connection);
            INSTANCES.put(connection, pepManager);
        }
        return pepManager;
    }

    private static final StanzaFilter FROM_BARE_JID_WITH_EVENT_EXTENSION_FILTER = new AndFilter(
            new FromJidTypeFilter(JidType.BareJid),
            EventExtensionFilter.INSTANCE);

    private final Set<PepListener> pepListeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<EntityBareJid> asyncButOrdered = new AsyncButOrdered<>();

    private final PubSubManager pepPubSubManager;

    /**
     * Creates a new PEP exchange manager.
     *
     * @param connection an XMPPConnection which is used to send and receive messages.
     */
    private PepManager(XMPPConnection connection) {
        super(connection);
        StanzaListener packetListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                final Message message = (Message) stanza;
                final EventElement event = EventElement.from(stanza);
                assert event != null;
                final EntityBareJid from = message.getFrom().asEntityBareJidIfPossible();
                assert from != null;
                asyncButOrdered.performAsyncButOrdered(from, new Runnable() {
                    @Override
                    public void run() {
                        for (PepListener listener : pepListeners) {
                            listener.eventReceived(from, event, message);
                        }
                    }
                });
            }
        };
        // TODO Add filter to check if from supports PubSub as per xep163 2 2.4
        connection.addSyncStanzaListener(packetListener, FROM_BARE_JID_WITH_EVENT_EXTENSION_FILTER);

        pepPubSubManager = PubSubManager.getInstanceFor(connection, null);
    }

    public PubSubManager getPepPubSubManager() {
        return pepPubSubManager;
    }

    /**
     * Adds a listener to PEPs. The listener will be fired anytime PEP events
     * are received from remote XMPP clients.
     *
     * @param pepListener a roster exchange listener.
     * @return true if pepListener was added.
     */
    public boolean addPepListener(PepListener pepListener) {
        return pepListeners.add(pepListener);
    }

    /**
     * Removes a listener from PEP events.
     *
     * @param pepListener a roster exchange listener.
     * @return true, if pepListener was removed.
     */
    public boolean removePepListener(PepListener pepListener) {
        return pepListeners.remove(pepListener);
    }

    /**
     * Publish an event.
     *
     * @param item the item to publish.
     * @param node the node to publish on.
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotAPubSubNodeException
     * @throws NotALeafNodeException
     */
    public void publish(Item item, String node) throws NotConnectedException, InterruptedException,
                    NoResponseException, XMPPErrorException, NotAPubSubNodeException, NotALeafNodeException {
        LeafNode pubSubNode = pepPubSubManager.getLeafNode(node);
        pubSubNode.publish(item);
    }

    /**
     * XEP-163 5.
     */
    private static final PubSubFeature[] REQUIRED_FEATURES = new PubSubFeature[] {
        // @formatter:off
        PubSubFeature.auto_create,
        PubSubFeature.auto_subscribe,
        PubSubFeature.filtered_notifications,
        // @formatter:on
    };

    public boolean isSupported() throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        XMPPConnection connection = connection();
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        BareJid localBareJid = connection.getUser().asBareJid();
        return serviceDiscoveryManager.supportsFeatures(localBareJid, REQUIRED_FEATURES);
    }
}
