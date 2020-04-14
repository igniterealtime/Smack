/**
 *
 * Copyright 2009 Robin Collier.
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
package org.jivesoftware.smackx.pubsub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.delay.DelayInformationManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.Affiliation.AffiliationNamespace;
import org.jivesoftware.smackx.pubsub.SubscriptionsExtension.SubscriptionsNamespace;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;
import org.jivesoftware.smackx.xdata.Form;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public abstract class Node {
    protected final PubSubManager pubSubManager;
    protected final String id;

    protected ConcurrentHashMap<ItemEventListener<Item>, StanzaListener> itemEventToListenerMap = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<ItemDeleteListener, StanzaListener> itemDeleteToListenerMap = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<NodeConfigListener, StanzaListener> configEventToListenerMap = new ConcurrentHashMap<>();

    /**
     * Construct a node associated to the supplied connection with the specified
     * node id.
     *
     * @param pubSubManager The PubSubManager for the connection the node is associated with
     * @param nodeId The node id
     */
    Node(PubSubManager pubSubManager, String nodeId) {
        this.pubSubManager = pubSubManager;
        id = nodeId;
    }

    /**
     * Get the NodeId.
     *
     * @return the node id
     */
    public String getId() {
        return id;
    }
    /**
     * Returns a configuration form, from which you can create an answer form to be submitted
     * via the {@link #sendConfigurationForm(Form)}.
     *
     * @return the configuration form
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ConfigureForm getNodeConfiguration() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(
                        PubSubElementType.CONFIGURE_OWNER, getId()));
        Stanza reply = sendPubsubPacket(pubSub);
        return NodeUtils.getFormFromPacket(reply, PubSubElementType.CONFIGURE_OWNER);
    }

    /**
     * Update the configuration with the contents of the new {@link Form}.
     *
     * @param submitForm TODO javadoc me please
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendConfigurationForm(Form submitForm) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub packet = createPubsubPacket(Type.set, new FormNode(FormNodeType.CONFIGURE_OWNER,
                        getId(), submitForm));
        pubSubManager.getConnection().createStanzaCollectorAndSend(packet).nextResultOrThrow();
    }

    /**
     * Discover node information in standard {@link DiscoverInfo} format.
     *
     * @return The discovery information about the node.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public DiscoverInfo discoverInfo() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        XMPPConnection connection = pubSubManager.getConnection();
        DiscoverInfo discoverInfoRequest = DiscoverInfo.builder(connection)
                .to(pubSubManager.getServiceJid())
                .setNode(getId())
                .build();
        return connection.createStanzaCollectorAndSend(discoverInfoRequest).nextResultOrThrow();
    }

    /**
     * Get the subscriptions currently associated with this node.
     *
     * @return List of {@link Subscription}
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public List<Subscription> getSubscriptions() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getSubscriptions(null, null);
    }

    /**
     * Get the subscriptions currently associated with this node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions TODO javadoc me please
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions
     * @return List of {@link Subscription}
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Subscription> getSubscriptions(List<ExtensionElement> additionalExtensions, Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getSubscriptions(SubscriptionsNamespace.basic, additionalExtensions, returnedExtensions);
    }

    /**
     * Get the subscriptions currently associated with this node as owner.
     *
     * @return List of {@link Subscription}
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see #getSubscriptionsAsOwner(List, Collection)
     * @since 4.1
     */
    public List<Subscription> getSubscriptionsAsOwner() throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        return getSubscriptionsAsOwner(null, null);
    }

    /**
     * Get the subscriptions currently associated with this node as owner.
     * <p>
     * Unlike {@link #getSubscriptions(List, Collection)}, which only retrieves the subscriptions of the current entity
     * ("user"), this method returns a list of <b>all</b> subscriptions. This requires the entity to have the sufficient
     * privileges to manage subscriptions.
     * </p>
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions TODO javadoc me please
     * @param returnedExtensions a collection that will be filled with the returned stanza extensions
     * @return List of {@link Subscription}
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-subscriptions-retrieve">XEP-60 § 8.8.1 -
     *      Retrieve Subscriptions List</a>
     * @since 4.1
     */
    public List<Subscription> getSubscriptionsAsOwner(List<ExtensionElement> additionalExtensions,
                    Collection<ExtensionElement> returnedExtensions) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        return getSubscriptions(SubscriptionsNamespace.owner, additionalExtensions, returnedExtensions);
    }

    private List<Subscription> getSubscriptions(SubscriptionsNamespace subscriptionsNamespace, List<ExtensionElement> additionalExtensions,
                    Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSubElementType pubSubElementType = subscriptionsNamespace.type;

        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(pubSubElementType, getId()));
        if (additionalExtensions != null) {
            for (ExtensionElement pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = sendPubsubPacket(pubSub);
        if (returnedExtensions != null) {
            returnedExtensions.addAll(reply.getExtensions());
        }
        SubscriptionsExtension subElem = reply.getExtension(pubSubElementType);
        return subElem.getSubscriptions();
    }

    /**
     * Modify the subscriptions for this PubSub node as owner.
     * <p>
     * Note that the subscriptions are _not_ checked against the existing subscriptions
     * since these are not cached (and indeed could change asynchronously)
     * </p>
     *
     * @param changedSubs subscriptions that have changed
     * @return <code>null</code> or a PubSub stanza with additional information on success.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#owner-subscriptions-modify">XEP-60 § 8.8.2 Modify Subscriptions</a>
     * @since 4.3
     */
    public PubSub modifySubscriptionsAsOwner(List<Subscription> changedSubs)
        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        PubSub pubSub = createPubsubPacket(Type.set,
            new SubscriptionsExtension(SubscriptionsNamespace.owner, getId(), changedSubs));
        return sendPubsubPacket(pubSub);
    }

    /**
     * Get the affiliations of this node.
     *
     * @return List of {@link Affiliation}
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Affiliation> getAffiliations() throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        return getAffiliations(null, null);
    }

    /**
     * Get the affiliations of this node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions additional {@code PacketExtensions} add to the request
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions
     * @return List of {@link Affiliation}
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Affiliation> getAffiliations(List<ExtensionElement> additionalExtensions, Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        return getAffiliations(AffiliationNamespace.basic, additionalExtensions, returnedExtensions);
    }

    /**
     * Retrieve the affiliation list for this node as owner.
     *
     * @return list of entities whose affiliation is not 'none'.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see #getAffiliations(List, Collection)
     * @since 4.2
     */
    public List<Affiliation> getAffiliationsAsOwner()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        return getAffiliationsAsOwner(null, null);
    }

    /**
     * Retrieve the affiliation list for this node as owner.
     * <p>
     * Note that this is an <b>optional</b> PubSub feature ('pubsub#modify-affiliations').
     * </p>
     *
     * @param additionalExtensions optional additional extension elements add to the request.
     * @param returnedExtensions an optional collection that will be filled with the returned
     *        extension elements.
     * @return list of entities whose affiliation is not 'none'.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-affiliations-retrieve">XEP-60 § 8.9.1 Retrieve Affiliations List</a>
     * @since 4.2
     */
    public List<Affiliation> getAffiliationsAsOwner(List<ExtensionElement> additionalExtensions, Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        return getAffiliations(AffiliationNamespace.owner, additionalExtensions, returnedExtensions);
    }

    private List<Affiliation> getAffiliations(AffiliationNamespace affiliationsNamespace, List<ExtensionElement> additionalExtensions,
                    Collection<ExtensionElement> returnedExtensions) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        PubSubElementType pubSubElementType = affiliationsNamespace.type;

        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(pubSubElementType, getId()));
        if (additionalExtensions != null) {
            for (ExtensionElement pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = sendPubsubPacket(pubSub);
        if (returnedExtensions != null) {
            returnedExtensions.addAll(reply.getExtensions());
        }
        AffiliationsExtension affilElem = reply.getExtension(pubSubElementType);
        return affilElem.getAffiliations();
    }

    /**
     * Modify the affiliations for this PubSub node as owner. The {@link Affiliation}s given must be created with the
     * {@link Affiliation#Affiliation(org.jxmpp.jid.BareJid, Affiliation.Type)} constructor.
     * <p>
     * Note that this is an <b>optional</b> PubSub feature ('pubsub#modify-affiliations').
     * </p>
     *
     * @param affiliations TODO javadoc me please
     * @return <code>null</code> or a PubSub stanza with additional information on success.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-affiliations-modify">XEP-60 § 8.9.2 Modify Affiliation</a>
     * @since 4.2
     */
    public PubSub modifyAffiliationAsOwner(List<Affiliation> affiliations) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        for (Affiliation affiliation : affiliations) {
            if (affiliation.getPubSubNamespace() != PubSubNamespace.owner) {
                throw new IllegalArgumentException("Must use Affiliation(BareJid, Type) affiliations");
            }
        }

        PubSub pubSub = createPubsubPacket(Type.set, new AffiliationsExtension(AffiliationNamespace.owner, affiliations, getId()));
        return sendPubsubPacket(pubSub);
    }

    /**
     * The user subscribes to the node using the supplied jid.  The
     * bare jid portion of this one must match the jid for the connection.
     *
     * Please note that the {@link Subscription.State} should be checked
     * on return since more actions may be required by the caller.
     * {@link Subscription.State#pending} - The owner must approve the subscription
     * request before messages will be received.
     * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true,
     * the caller must configure the subscription before messages will be received.  If it is false
     * the caller can configure it but is not required to do so.
     * @param jid The jid to subscribe as.
     * @return The subscription
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Subscription subscribe(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub pubSub = createPubsubPacket(Type.set, new SubscribeExtension(jid, getId()));
        PubSub reply = sendPubsubPacket(pubSub);
        return reply.getExtension(PubSubElementType.SUBSCRIPTION);
    }

    /**
     * The user subscribes to the node using the supplied jid.  The
     * bare jid portion of this one must match the jid for the connection.
     *
     * Please note that the {@link Subscription.State} should be checked
     * on return since more actions may be required by the caller.
     * {@link Subscription.State#pending} - The owner must approve the subscription
     * request before messages will be received.
     * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true,
     * the caller must configure the subscription before messages will be received.  If it is false
     * the caller can configure it but is not required to do so.
     *
     * @param jidString The jid to subscribe as.
     * @return The subscription
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws IllegalArgumentException if the provided string is not a valid JID.
     * @deprecated use {@link #subscribe(Jid)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Subscription subscribe(String jidString) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Jid jid;
        try {
            jid = JidCreate.from(jidString);
        } catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }
        return subscribe(jid);
    }

    /**
     * The user subscribes to the node using the supplied jid and subscription
     * options.  The bare jid portion of this one must match the jid for the
     * connection.
     *
     * Please note that the {@link Subscription.State} should be checked
     * on return since more actions may be required by the caller.
     * {@link Subscription.State#pending} - The owner must approve the subscription
     * request before messages will be received.
     * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true,
     * the caller must configure the subscription before messages will be received.  If it is false
     * the caller can configure it but is not required to do so.
     *
     * @param jid The jid to subscribe as.
     * @param subForm TODO javadoc me please
     *
     * @return The subscription
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Subscription subscribe(Jid jid, SubscribeForm subForm) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(Type.set, new SubscribeExtension(jid, getId()));
        request.addExtension(new FormNode(FormNodeType.OPTIONS, subForm));
        PubSub reply = sendPubsubPacket(request);
        return reply.getExtension(PubSubElementType.SUBSCRIPTION);
    }

    /**
     * The user subscribes to the node using the supplied jid and subscription
     * options.  The bare jid portion of this one must match the jid for the
     * connection.
     *
     * Please note that the {@link Subscription.State} should be checked
     * on return since more actions may be required by the caller.
     * {@link Subscription.State#pending} - The owner must approve the subscription
     * request before messages will be received.
     * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true,
     * the caller must configure the subscription before messages will be received.  If it is false
     * the caller can configure it but is not required to do so.
     *
     * @param jidString The jid to subscribe as.
     * @param subForm TODO javadoc me please
     *
     * @return The subscription
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws IllegalArgumentException if the provided string is not a valid JID.
     * @deprecated use {@link #subscribe(Jid, SubscribeForm)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Subscription subscribe(String jidString, SubscribeForm subForm) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Jid jid;
        try {
            jid = JidCreate.from(jidString);
        } catch (XmppStringprepException e) {
            throw new IllegalArgumentException(e);
        }
        return subscribe(jid, subForm);
    }

    /**
     * Remove the subscription related to the specified JID.  This will only
     * work if there is only 1 subscription.  If there are multiple subscriptions,
     * use {@link #unsubscribe(String, String)}.
     *
     * @param jid The JID used to subscribe to the node
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public void unsubscribe(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        unsubscribe(jid, null);
    }

    /**
     * Remove the specific subscription related to the specified JID.
     *
     * @param jid The JID used to subscribe to the node
     * @param subscriptionId The id of the subscription being removed
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void unsubscribe(String jid, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        sendPubsubPacket(createPubsubPacket(Type.set, new UnsubscribeExtension(jid, getId(), subscriptionId)));
    }

    /**
     * Returns a SubscribeForm for subscriptions, from which you can create an answer form to be submitted
     * via the {@link #sendConfigurationForm(Form)}.
     *
     * @param jid TODO javadoc me please
     *
     * @return A subscription options form
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public SubscribeForm getSubscriptionOptions(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getSubscriptionOptions(jid, null);
    }


    /**
     * Get the options for configuring the specified subscription.
     *
     * @param jid JID the subscription is registered under
     * @param subscriptionId The subscription id
     *
     * @return The subscription option form
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public SubscribeForm getSubscriptionOptions(String jid, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub packet = sendPubsubPacket(createPubsubPacket(Type.get, new OptionsExtension(jid, getId(), subscriptionId)));
        FormNode ext = packet.getExtension(PubSubElementType.OPTIONS);
        return new SubscribeForm(ext.getForm());
    }

    /**
     * Register a listener for item publication events.  This
     * listener will get called whenever an item is published to
     * this node.
     *
     * @param listener The handler for the event
     */
    @SuppressWarnings("unchecked")
    public void addItemEventListener(@SuppressWarnings("rawtypes") ItemEventListener listener) {
        StanzaListener conListener = new ItemEventTranslator(listener);
        itemEventToListenerMap.put(listener, conListener);
        pubSubManager.getConnection().addSyncStanzaListener(conListener, new EventContentFilter(EventElementType.items.toString(), "item"));
    }

    /**
     * Unregister a listener for publication events.
     *
     * @param listener The handler to unregister
     */
    public void removeItemEventListener(@SuppressWarnings("rawtypes") ItemEventListener listener) {
        StanzaListener conListener = itemEventToListenerMap.remove(listener);

        if (conListener != null)
            pubSubManager.getConnection().removeSyncStanzaListener(conListener);
    }

    /**
     * Register a listener for configuration events.  This listener
     * will get called whenever the node's configuration changes.
     *
     * @param listener The handler for the event
     */
    public void addConfigurationListener(NodeConfigListener listener) {
        StanzaListener conListener = new NodeConfigTranslator(listener);
        configEventToListenerMap.put(listener, conListener);
        pubSubManager.getConnection().addSyncStanzaListener(conListener, new EventContentFilter(EventElementType.configuration.toString()));
    }

    /**
     * Unregister a listener for configuration events.
     *
     * @param listener The handler to unregister
     */
    public void removeConfigurationListener(NodeConfigListener listener) {
        StanzaListener conListener = configEventToListenerMap .remove(listener);

        if (conListener != null)
            pubSubManager.getConnection().removeSyncStanzaListener(conListener);
    }

    /**
     * Register an listener for item delete events.  This listener
     * gets called whenever an item is deleted from the node.
     *
     * @param listener The handler for the event
     */
    public void addItemDeleteListener(ItemDeleteListener listener) {
        StanzaListener delListener = new ItemDeleteTranslator(listener);
        itemDeleteToListenerMap.put(listener, delListener);
        EventContentFilter deleteItem = new EventContentFilter(EventElementType.items.toString(), "retract");
        EventContentFilter purge = new EventContentFilter(EventElementType.purge.toString());

        // TODO: Use AsyncButOrdered (with Node as Key?)
        pubSubManager.getConnection().addSyncStanzaListener(delListener, new OrFilter(deleteItem, purge));
    }

    /**
     * Unregister a listener for item delete events.
     *
     * @param listener The handler to unregister
     */
    public void removeItemDeleteListener(ItemDeleteListener listener) {
        StanzaListener conListener = itemDeleteToListenerMap .remove(listener);

        if (conListener != null)
            pubSubManager.getConnection().removeSyncStanzaListener(conListener);
    }

    @Override
    public String toString() {
        return super.toString() + " " + getClass().getName() + " id: " + id;
    }

    protected PubSub createPubsubPacket(Type type, NodeExtension ext) {
        return PubSub.createPubsubPacket(pubSubManager.getServiceJid(), type, ext);
    }

    protected PubSub sendPubsubPacket(PubSub packet) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return pubSubManager.sendPubsubPacket(packet);
    }


    private static List<String> getSubscriptionIds(Stanza packet) {
        HeadersExtension headers = packet.getExtension(HeadersExtension.class);
        List<String> values = null;

        if (headers != null) {
            values = new ArrayList<>(headers.getHeaders().size());

            for (Header header : headers.getHeaders()) {
                values.add(header.getValue());
            }
        }
        return values;
    }

    /**
     * This class translates low level item publication events into api level objects for
     * user consumption.
     *
     * @author Robin Collier
     */
    public static class ItemEventTranslator implements StanzaListener {
        @SuppressWarnings("rawtypes")
        private final ItemEventListener listener;

        public ItemEventTranslator(@SuppressWarnings("rawtypes") ItemEventListener eventListener) {
            listener = eventListener;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void processStanza(Stanza packet) {
            EventElement event = (EventElement) packet.getExtensionElement("event", PubSubNamespace.event.getXmlns());
            ItemsExtension itemsElem = (ItemsExtension) event.getEvent();
            ItemPublishEvent eventItems = new ItemPublishEvent(itemsElem.getNode(), itemsElem.getItems(), getSubscriptionIds(packet), DelayInformationManager.getDelayTimestamp(packet));
            // TODO: Use AsyncButOrdered (with Node as Key?)
            listener.handlePublishedItems(eventItems);
        }
    }

    /**
     * This class translates low level item deletion events into api level objects for
     * user consumption.
     *
     * @author Robin Collier
     */
    public static class ItemDeleteTranslator implements StanzaListener {
        private final ItemDeleteListener listener;

        public ItemDeleteTranslator(ItemDeleteListener eventListener) {
            listener = eventListener;
        }

        @Override
        public void processStanza(Stanza packet) {
// CHECKSTYLE:OFF
            EventElement event = (EventElement) packet.getExtensionElement("event", PubSubNamespace.event.getXmlns());

            List<ExtensionElement> extList = event.getExtensions();

            if (extList.get(0).getElementName().equals(PubSubElementType.PURGE_EVENT.getElementName())) {
                listener.handlePurge();
            }
            else {
                ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
                @SuppressWarnings("unchecked")
                Collection<RetractItem> pubItems = (Collection<RetractItem>) itemsElem.getItems();
                List<String> items = new ArrayList<>(pubItems.size());

                for (RetractItem item : pubItems) {
                    items.add(item.getId());
                }

                ItemDeleteEvent eventItems = new ItemDeleteEvent(itemsElem.getNode(), items, getSubscriptionIds(packet));
                listener.handleDeletedItems(eventItems);
            }
// CHECKSTYLE:ON
        }
    }

    /**
     * This class translates low level node configuration events into api level objects for
     * user consumption.
     *
     * @author Robin Collier
     */
    public static class NodeConfigTranslator implements StanzaListener {
        private final NodeConfigListener listener;

        public NodeConfigTranslator(NodeConfigListener eventListener) {
            listener = eventListener;
        }

        @Override
        public void processStanza(Stanza packet) {
            EventElement event = (EventElement) packet.getExtensionElement("event", PubSubNamespace.event.getXmlns());
            ConfigurationEvent config = (ConfigurationEvent) event.getEvent();

            // TODO: Use AsyncButOrdered (with Node as Key?)
            listener.handleNodeConfiguration(config);
        }
    }

    /**
     * Filter for {@link StanzaListener} to filter out events not specific to the
     * event type expected for this node.
     *
     * @author Robin Collier
     */
    class EventContentFilter extends FlexibleStanzaTypeFilter<Message> {
        private final String firstElement;
        private final String secondElement;
        private final boolean allowEmpty;

        EventContentFilter(String elementName) {
            this(elementName, null);
        }

        EventContentFilter(String firstLevelElement, String secondLevelElement) {
            firstElement = firstLevelElement;
            secondElement = secondLevelElement;
            allowEmpty = firstElement.equals(EventElementType.items.toString())
                            && "item".equals(secondLevelElement);
        }

        @Override
        public boolean acceptSpecific(Message message) {
            EventElement event = EventElement.from(message);

            if (event == null)
                return false;

            NodeExtension embedEvent = event.getEvent();

            if (embedEvent == null)
                return false;

            if (embedEvent.getElementName().equals(firstElement)) {
                if (!embedEvent.getNode().equals(getId()))
                    return false;

                if (secondElement == null)
                    return true;

                if (embedEvent instanceof EmbeddedPacketExtension) {
                    List<ExtensionElement> secondLevelList = ((EmbeddedPacketExtension) embedEvent).getExtensions();

                    // XEP-0060 allows no elements on second level for notifications. See schema or
                    // for example § 4.3:
                    // "although event notifications MUST include an empty <items/> element;"
                    if (allowEmpty && secondLevelList.isEmpty()) {
                        return true;
                    }

                    if (secondLevelList.size() > 0 && secondLevelList.get(0).getElementName().equals(secondElement))
                        return true;
                }
            }
            return false;
        }
    }
}
