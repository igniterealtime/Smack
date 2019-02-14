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

import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

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
        pubSubManagerOne = PubSubManager.getInstance(conOne, pubSubService);
        if (!pubSubManagerOne.canCreateNodesAndPublishItems()) {
            throw new TestNotPossibleException("PubSub service does not allow node creation");
        }
    }

    @SmackIntegrationTest
    public void simplePubSubNodeTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final String nodename = "sinttest-simple-nodename-" + testRunId;
        final String itemId = "sintest-simple-itemid-" + testRunId;
        ConfigureForm defaultConfiguration = pubSubManagerOne.getDefaultConfiguration();
        ConfigureForm config = new ConfigureForm(defaultConfiguration.createAnswerForm());
        // Configure the node as "Notification-Only Node", which in turn means that
        // items do not need payload, to prevent payload-required error responses when
        // publishing the item.
        config.setDeliverPayloads(false);
        config.setPersistentItems(true);
        Node node = pubSubManagerOne.createNode(nodename, config);
        try {
            LeafNode leafNode = (LeafNode) node;
            leafNode.publish(new Item(itemId));
            List<Item> items = leafNode.getItems();
            assertEquals(1, items.size());
            Item item = items.get(0);
            assertEquals(itemId, item.getId());
        }
        finally {
            pubSubManagerOne.deleteNode(nodename);
        }
    }
}
