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
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jxmpp.jid.DomainBareJid;

public class PubSubIntegrationTest extends AbstractSmackIntegrationTest {

    private final PubSubManager pubSubManagerOne;

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

     */

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
