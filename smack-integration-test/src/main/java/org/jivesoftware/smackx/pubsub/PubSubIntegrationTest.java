/**
 *
 * Copyright 2015-2020 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PubSubIntegrationTest extends AbstractSmackIntegrationTest {

    private final PubSubManager pubSubManagerOne;
    private final PubSubManager pubSubManagerTwo;

    public PubSubIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws TestNotPossibleException, NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        super(environment);
        DomainBareJid pubSubService = PubSubManager.getPubSubService(conOne);
        if (pubSubService == null) {
            throw new TestNotPossibleException("No PubSub service found");
        }
        pubSubManagerOne = PubSubManager.getInstanceFor(conOne, pubSubService);
        if (!pubSubManagerOne.canCreateNodesAndPublishItems()) {
            throw new TestNotPossibleException("PubSub service does not allow node creation");
        }
        pubSubManagerTwo = PubSubManager.getInstanceFor(conTwo, pubSubService);
    }

    /**
     * Asserts that an item can be published to a node with default configuration.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest
    public void publishItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-publish-item-nodename-" + testRunId;
        final String needle = "test content " + Math.random();
        LeafNode node = pubSubManagerOne.createNode(nodename);
        try {
            // Publish a new item.
            node.publish( new PayloadItem<>( GeoLocation.builder().setDescription( needle ).build() ) );

            // Retrieve items and assert that the item that was just published is among them.
            final List<Item> items = node.getItems();
            assertTrue( items.stream().anyMatch( stanza -> stanza.toXML( "" ).toString().contains( needle ) ) );
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that one can subscribe to an existing node.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest
    public void subscribeTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodename);
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            final Subscription subscription = subscriberNode.subscribe( subscriber );
            assertNotNull( subscription );

            // Assert that subscription is correctly reported when the subscriber requests its subscriptions.
            final List<Subscription> subscriptions = pubSubManagerTwo.getNode( nodename ).getSubscriptions();
            assertNotNull( subscriptions );
            assertTrue( subscriptions.stream().anyMatch( s -> subscriber.equals(s.getJid())) );
        }
        catch ( PubSubException.NotAPubSubNodeException e )
        {
            throw new AssertionError("The published item was not received by the subscriber.", e);
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that the server returns a 'bad request' error to a subscription
     * request in which the JIDs do not match.
     *
     * <p>From XEP-0060 § 6.1.3.1:</p>
     * <blockquote>
     * If the specified JID is a bare JID or full JID, the service MUST at a
     * minimum check the bare JID portion against the bare JID portion of the
     * 'from' attribute on the received IQ request to make sure that the
     * requesting entity has the same identity as the JID which is being
     * requested to be added to the subscriber list.
     *
     * If the bare JID portions of the JIDs do not match as described above and
     * the requesting entity does not have some kind of admin or proxy privilege
     * as defined by the implementation, the service MUST return a
     * &lt;bad-request/&gt; error (...)
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XmppStringprepException if the hard-coded test JID cannot be instantiated.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest
    public void subscribeJIDsDoNotMatchTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, XmppStringprepException, PubSubException.NotAPubSubNodeException
    {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodename);
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = JidCreate.entityBareFrom( "this-jid-does-not-match@example.org" );
            subscriberNode.subscribe( subscriber );
            fail( "The server should have returned a <bad-request/> error, but did not." );
        }
        catch ( XMPPErrorException e )
        {
            assertEquals( StanzaError.Condition.bad_request, e.getStanzaError().getCondition() );
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request where required presence subscription is missing.
     *
     * <p>From XEP-0060 § 6.1.3.2:</p>
     * <blockquote>
     * For nodes with an access model of "presence", if the requesting entity is
     * not subscribed to the owner's presence then the pubsub service MUST
     * respond with a &lt;not-authorized/&gt; error (...)
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest
    public void subscribePresenceSubscriptionRequiredTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException
    {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        config.setAccessModel(AccessModel.presence);
        try {
            pubSubManagerOne.createNode( nodename, config );
        } catch ( XMPPErrorException e ) {
            throw new TestNotPossibleException( "Access model 'presence' not supported on the server." );
        }
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe( subscriber );
            fail( "The server should have returned a <not-authorized/> error, but did not." );
        }
        catch ( XMPPErrorException e )
        {
            assertEquals( StanzaError.Condition.not_authorized, e.getStanzaError().getCondition() );
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request where required roster items are missing.
     *
     * <p>From XEP-0060 § 6.1.3.3:</p>
     * <blockquote>
     * For nodes with an access model of "roster", if the requesting entity is
     * not in one of the authorized roster groups then the pubsub service MUST
     * respond with a &lt;not-authorized/&gt; error (...)
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest
    public void subscribeNotInRosterGroupTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException
    {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        config.setAccessModel(AccessModel.roster);
        try {
            pubSubManagerOne.createNode( nodename, config );
        } catch ( XMPPErrorException e ) {
            throw new TestNotPossibleException( "Access model 'roster' not supported on the server." );
        }
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe( subscriber );
            fail( "The server should have returned a <not-authorized/> error, but did not." );
        }
        catch ( XMPPErrorException e )
        {
            assertEquals( StanzaError.Condition.not_authorized, e.getStanzaError().getCondition() );
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that the server returns a 'not-allowed' error to a subscription
     * request where required whitelisting is missing.
     *
     * <p>From XEP-0060 § 6.1.3.4:</p>
     * <blockquote>
     * For nodes with a node access model of "whitelist", if the requesting
     * entity is not on the whitelist then the service MUST return a
     * &lt;not-allowed/&gt; error, specifying a pubsub-specific error condition
     * of &lt;closed-node/&gt;.
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest
    public void subscribeNotOnWhitelistTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException
    {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        config.setAccessModel(AccessModel.whitelist);
        try {
            pubSubManagerOne.createNode( nodename, config );
        } catch ( XMPPErrorException e ) {
            throw new TestNotPossibleException( "Access model 'whitelist' not supported on the server." );
        }
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe( subscriber );
            fail( "The server should have returned a <not-allowed/> error, but did not." );
        }
        catch ( XMPPErrorException e )
        {
            assertEquals( StanzaError.Condition.not_allowed, e.getStanzaError().getCondition() );
            assertNotNull( e.getStanzaError().getExtension( "closed-node", "http://jabber.org/protocol/pubsub#errors" ));
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that the server returns a 'not-authorized' error to a subscription
     * request when the subscriber already has a pending subscription.
     *
     * <p>From XEP-0060 § 6.1.3.7:</p>
     * <blockquote>
     * If the requesting entity has a pending subscription, the service MUST
     * return a &lt;not-authorized/&gt; error to the subscriber, specifying a
     * pubsub-specific error condition of &lt;pending-subscription/&gt;.
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     * @throws TestNotPossibleException if the server does not support the functionality required for this test.
     */
    @SmackIntegrationTest
    public void subscribePendingSubscriptionTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException, TestNotPossibleException
    {
        final String nodename = "sinttest-subscribe-nodename-" + testRunId;
        final ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        final ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        config.setAccessModel(AccessModel.authorize);
        try {
            pubSubManagerOne.createNode( nodename, config );
        } catch ( XMPPErrorException e ) {
            throw new TestNotPossibleException( "Access model 'authorize' not supported on the server." );
        }
        try {
            // Subscribe to the node, using a different user than the owner of the node.
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe( subscriber );
            subscriberNode.subscribe( subscriber );
            fail( "The server should have returned a <not-authorized/> error, but did not." );
        }
        catch ( XMPPErrorException e )
        {
            assertEquals( StanzaError.Condition.not_authorized, e.getStanzaError().getCondition() );
            assertNotNull( e.getStanzaError().getExtension( "pending-subscription", "http://jabber.org/protocol/pubsub#errors" ));
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that an empty subscriptions collection is returned when an entity
     * requests its subscriptions from a node that it is not subscribed to.
     *
     * <p>From XEP-0060 § 5.6:</p>
     * <blockquote>
     * If the requesting entity has no subscriptions, the pubsub service MUST
     * return an empty &lt;subscriptions/&gt; element.
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws PubSubException.NotAPubSubNodeException if the node cannot be accessed.
     */
    @SmackIntegrationTest
    public void getEmptySubscriptionsTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotAPubSubNodeException
    {
        final String nodename = "sinttest-get-empty-subscriptions-test-nodename-" + testRunId;
        pubSubManagerOne.createNode(nodename);
        try {
            // Assert that subscriptions for a non-subscriber is reported as an empty list.
            final List<Subscription> subscriptions = pubSubManagerTwo.getNode( nodename ).getSubscriptions();
            assertNotNull( subscriptions );
            assertTrue( subscriptions.isEmpty() );
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that one receives a published item, after subscribing to a node.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws ExecutionException if waiting for the response was interrupted.
     * @throws PubSubException if the involved node is not a pubsub node.
     */
    @SmackIntegrationTest
    public void receivePublishedItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, ExecutionException, PubSubException
    {
        final String nodename = "sinttest-receive-published-item-nodename-" + testRunId;
        final String needle = "test content " + Math.random();
        LeafNode publisherNode = pubSubManagerOne.createNode(nodename);
        try {
            final Node subscriberNode = pubSubManagerTwo.getNode(nodename);
            final EntityBareJid subscriber = conTwo.getUser().asEntityBareJid();
            subscriberNode.subscribe( subscriber );

            final CompletableFuture<Stanza> result = new CompletableFuture<>();
            conTwo.addAsyncStanzaListener( result::complete, stanza -> stanza.toXML( "" ).toString().contains( needle ) );

            publisherNode.publish( new PayloadItem<>( GeoLocation.builder().setDescription( needle ).build() ) );

            assertNotNull( result.get( conOne.getReplyTimeout(), TimeUnit.MILLISECONDS ) );
        }
        catch ( TimeoutException e )
        {
            throw new AssertionError("The published item was not received by the subscriber.", e);
        }
        finally {
            pubSubManagerOne.deleteNode( nodename );
        }
    }

    /**
     * Asserts that an event notification (publication without item) can be published to
     * a node that is both 'notification-only' as well as 'transient'.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SmackIntegrationTest
    public void transientNotificationOnlyNodeWithoutItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-transient-notificationonly-withoutitem-nodename-" + testRunId;
        ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        FillableConfigureForm config = defaultConfiguration.getFillableForm();
        // Configure the node as "Notification-Only Node".
        config.setDeliverPayloads(false);
        // Configure the node as "transient" (set persistent_items to 'false')
        config.setPersistentItems(false);
        Node node = pubSubManagerOne.createNode(nodename, config);
        try {
            LeafNode leafNode = (LeafNode) node;
            leafNode.publish();
        }
        finally {
            pubSubManagerOne.deleteNode(nodename);
        }
    }

    /**
     * Asserts that an error is returned when a publish request to a node that is both
     * 'notification-only' as well as 'transient' contains an item element.
     *
     * <p>From XEP-0060 § 7.1.3.6:</p>
     * <blockquote>
     * If the event type is notification + transient and the publisher provides an item,
     * the service MUST bounce the publication request with a &lt;bad-request/&gt; error
     * and a pubsub-specific error condition of &lt;item-forbidden/&gt;.
     * </blockquote>
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#publisher-publish-error-badrequest">
     *     7.1.3.6 Request Does Not Match Configuration</a>
     */
    @SmackIntegrationTest
    public void transientNotificationOnlyNodeWithItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-transient-notificationonly-withitem-nodename-" + testRunId;
        final String itemId = "sinttest-transient-notificationonly-withitem-itemid-" + testRunId;

        ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        FillableConfigureForm config = defaultConfiguration.getFillableForm();
        // Configure the node as "Notification-Only Node".
        config.setDeliverPayloads(false);
        // Configure the node as "transient" (set persistent_items to 'false')
        config.setPersistentItems(false);
        Node node = pubSubManagerOne.createNode(nodename, config);

        // Add a dummy payload. If there is no payload, but just an item ID, then ejabberd will *not* return an error,
        // which I believe to be non-compliant behavior (although, granted, the XEP is not very clear about this). A user
        // which sends an empty item with ID to an node that is configured to be notification-only and transient probably
        // does something wrong, as the item's ID will never appear anywhere. Hence it would be nice if the user would be
        // made aware of this issue by returning an error. Sadly ejabberd does not do so.
        // See also https://github.com/processone/ejabberd/issues/2864#issuecomment-500741915
        final StandardExtensionElement dummyPayload = StandardExtensionElement.builder("dummy-payload",
                        SmackConfiguration.SMACK_URL_STRING).setText(testRunId).build();

        try {
            XMPPErrorException e = assertThrows(XMPPErrorException.class, () -> {
                LeafNode leafNode = (LeafNode) node;

                Item item = new PayloadItem<>(itemId, dummyPayload);
                leafNode.publish(item);
            });
            assertEquals(StanzaError.Type.MODIFY, e.getStanzaError().getType());
            assertNotNull(e.getStanzaError().getExtension("item-forbidden", "http://jabber.org/protocol/pubsub#errors"));
        }
        finally {
            pubSubManagerOne.deleteNode(nodename);
        }
    }


}
