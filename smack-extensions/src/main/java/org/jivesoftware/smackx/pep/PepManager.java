/*
 *
 * Copyright 2003-2007 Jive Software, 2015-2025 Florian Schmaus
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.jidtype.FromJidTypeFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.MultiMap;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubFeature;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.filter.EventItemsExtensionFilter;

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

    private static final Logger LOGGER = Logger.getLogger(PepManager.class.getName());

    private static final Map<XMPPConnection, PepManager> INSTANCES = new WeakHashMap<>();

    public static synchronized PepManager getInstanceFor(XMPPConnection connection) {
        PepManager pepManager = INSTANCES.get(connection);
        if (pepManager == null) {
            pepManager = new PepManager(connection);
            INSTANCES.put(connection, pepManager);
        }
        return pepManager;
    }

    // TODO: Ideally PepManager would re-use PubSubManager for this. But the functionality in PubSubManager does not yet
    // exist.
    private static final StanzaFilter PEP_EVENTS_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_HEADLINE,
            FromJidTypeFilter.ENTITY_BARE_JID,
            EventItemsExtensionFilter.INSTANCE);

    private final Set<PepListener> pepListeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<EntityBareJid> asyncButOrdered = new AsyncButOrdered<>();

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private final PubSubManager pepPubSubManager;

    private final MultiMap<String, PepEventListenerCoupling<? extends ExtensionElement>> pepEventListeners = new MultiMap<>();

    private final Map<PepEventListener<?>, PepEventListenerCoupling<?>> listenerToCouplingMap = new HashMap<>();

    /**
     * Creates a new PEP exchange manager.
     *
     * @param connection an XMPPConnection which is used to send and receive messages.
     */
    private PepManager(XMPPConnection connection) {
        super(connection);

        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        pepPubSubManager = PubSubManager.getInstanceFor(connection, null);

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
                        ItemsExtension itemsExtension = (ItemsExtension) event.getEvent();
                        String node = itemsExtension.getNode();

                        for (PepListener listener : pepListeners) {
                            listener.eventReceived(from, event, message);
                        }

                        List<PepEventListenerCoupling<? extends ExtensionElement>> nodeListeners;
                        synchronized (pepEventListeners) {
                            nodeListeners = pepEventListeners.getAll(node);
                            if (nodeListeners.isEmpty()) {
                                return;
                            }

                            // Make a copy of the list. Note that it is important to do this within the synchronized
                            // block.
                            nodeListeners = CollectionUtil.newListWith(nodeListeners);
                        }

                        for (PepEventListenerCoupling<? extends ExtensionElement> listener : nodeListeners) {
                            // TODO: Can there be more than one item?
                            List<? extends NamedElement> items = itemsExtension.getItems();
                            for (NamedElement namedElementItem : items) {
                                Item item = (Item) namedElementItem;
                                String id = item.getId();
                                @SuppressWarnings("unchecked")
                                PayloadItem<ExtensionElement> payloadItem = (PayloadItem<ExtensionElement>) item;
                                ExtensionElement payload = payloadItem.getPayload();

                                listener.invoke(from, payload, id, message);
                            }
                        }
                    }
                });
            }
        };
        // TODO Add filter to check if from supports PubSub as per xep163 2 2.4
        connection.addSyncStanzaListener(packetListener, PEP_EVENTS_FILTER);
    }

    private static final class PepEventListenerCoupling<E extends ExtensionElement> {
        private final String node;
        private final Class<E> extensionElementType;
        private final PepEventListener<E> pepEventListener;

        private PepEventListenerCoupling(String node, Class<E> extensionElementType,
                        PepEventListener<E> pepEventListener) {
            this.node = node;
            this.extensionElementType = extensionElementType;
            this.pepEventListener = pepEventListener;
        }

        private void invoke(EntityBareJid from, ExtensionElement payload, String id, Message carrierMessage) {
            if (!extensionElementType.isInstance(payload)) {
                LOGGER.warning("Ignoring " + payload + " from " + carrierMessage + " as it is not of type "
                                + extensionElementType);
                return;
            }

            E extensionElementPayload = extensionElementType.cast(payload);
            pepEventListener.onPepEvent(from, extensionElementPayload, id, carrierMessage);
        }
    }

    public <E extends ExtensionElement> boolean addPepEventListener(String node, Class<E> extensionElementType,
                    PepEventListener<E> pepEventListener) {
        PepEventListenerCoupling<E> pepEventListenerCoupling = new PepEventListenerCoupling<>(node,
                        extensionElementType, pepEventListener);

        synchronized (pepEventListeners) {
            var currentPepEventListenerCoupling = listenerToCouplingMap.putIfAbsent(pepEventListener, pepEventListenerCoupling);
            if (currentPepEventListenerCoupling != null) return false;

            boolean listenerForNodeExisted = pepEventListeners.put(node, pepEventListenerCoupling);
            if (!listenerForNodeExisted) {
                serviceDiscoveryManager.addFeature(node + PubSubManager.PLUS_NOTIFY);
            }
        }
        return true;
    }

    public boolean removePepEventListener(PepEventListener<?> pepEventListener) {
        synchronized (pepEventListeners) {
            PepEventListenerCoupling<?> pepEventListenerCoupling = listenerToCouplingMap.remove(pepEventListener);
            if (pepEventListenerCoupling == null) {
                return false;
            }

            String node = pepEventListenerCoupling.node;

            boolean mappingExisted = pepEventListeners.removeOne(node, pepEventListenerCoupling);
            assert mappingExisted;

            if (!pepEventListeners.containsKey(pepEventListenerCoupling.node)) {
                // This was the last listener for the node. Remove the +notify feature.
                serviceDiscoveryManager.removeFeature(node + PubSubManager.PLUS_NOTIFY);
            }
        }

        return true;
    }

    public PubSubManager getPepPubSubManager() {
        return pepPubSubManager;
    }

    /**
     * Publish an event.
     *
     * @param nodeId the ID of the node to publish on.
     * @param item the item to publish.
     * @return the leaf node the item was published on.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     */
    public LeafNode publish(String nodeId, Item item) throws NotConnectedException, InterruptedException,
                    NoResponseException, XMPPErrorException, NotALeafNodeException {
        // PEP nodes are auto created if not existent. Hence, use PubSubManager.tryToPublishAndPossibleAutoCreate() here.
        return pepPubSubManager.tryToPublishAndPossibleAutoCreate(nodeId, item);
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

    public boolean isSupported()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        XMPPConnection connection = connection();
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        BareJid localBareJid = connection.getUser().asBareJid();
        return serviceDiscoveryManager.supportsFeatures(localBareJid, REQUIRED_FEATURES);
    }
}
