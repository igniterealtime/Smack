/**
 *
 * Copyright 2015-2019 Florian Schmaus
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jxmpp.jid.DomainBareJid;

public class PubSubIntegrationTest extends AbstractSmackIntegrationTest {

    private final PubSubManager pubSubManagerOne;

    public PubSubIntegrationTest(SmackIntegrationTestEnvironment<?> environment)
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
    }

    /**
     * Asserts that an event notification (publication without item) can be published to
     * a node that is both 'notification-only' as well as 'transient'.
     */
    @SmackIntegrationTest
    public void transientNotificationOnlyNodeWithoutItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-transient-notificationonly-withoutitem-nodename-" + testRunId;
        ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        // Configure the node as "Notification-Only Node".
        config.setDeliverPayloads(false);
        // Configure the node as "transient" (set persistent_items to 'false')
        config.setPersistentItems(false);
        Node node = pubSubManagerOne.createNode(nodename, config);
        try {
            LeafNode leafNode = (LeafNode) node;
            leafNode.publish();
            List<Item> items = leafNode.getItems();
            assertTrue(items.isEmpty());
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
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#publisher-publish-error-badrequest">
     *     7.1.3.6 Request Does Not Match Configuration</a>
     */
    @SmackIntegrationTest
    public void transientNotificationOnlyNodeWithItemTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-transient-notificationonly-withitem-nodename-" + testRunId;
        final String itemId = "sinttest-transient-notificationonly-withitem-itemid-" + testRunId;
        ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        // Configure the node as "Notification-Only Node".
        config.setDeliverPayloads(false);
        // Configure the node as "transient" (set persistent_items to 'false')
        config.setPersistentItems(false);
        Node node = pubSubManagerOne.createNode(nodename, config);
        try {
            LeafNode leafNode = (LeafNode) node;
            leafNode.publish(new Item(itemId));
            fail("An exception should have been thrown.");
        }
        catch (XMPPErrorException e) {
            assertEquals(StanzaError.Type.MODIFY, e.getStanzaError().getType());
            assertNotNull(e.getStanzaError().getExtension("item-forbidden", "http://jabber.org/protocol/pubsub#errors"));
        }
        finally {
            pubSubManagerOne.deleteNode(nodename);
        }
    }
}
