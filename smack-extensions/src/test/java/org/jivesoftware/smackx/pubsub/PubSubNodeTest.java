/**
 *
 * Copyright 2018 Timothy Pitt, Florian Schmaus
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.pubsub.Affiliation.AffiliationNamespace;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;
import org.jxmpp.jid.impl.JidCreate;

public class PubSubNodeTest extends SmackTestSuite {

    @Test
    public void modifySubscriptionsAsOwnerTest() throws InterruptedException, SmackException, IOException, XMPPException, Exception {
        ThreadedDummyConnection con = ThreadedDummyConnection.newInstance();
        PubSubManager mgr = new PubSubManager(con, JidTestUtil.PUBSUB_EXAMPLE_ORG);
        Node testNode = new LeafNode(mgr, "princely_musings");

        List<Subscription> ChangeSubs = Arrays.asList(
            new Subscription(JidCreate.from("romeo@montague.org"), Subscription.State.subscribed),
            new Subscription(JidCreate.from("juliet@capulet.org"), Subscription.State.none)
        );
        testNode.modifySubscriptionsAsOwner(ChangeSubs);

        PubSub request = con.getSentPacket();

        assertEquals("http://jabber.org/protocol/pubsub#owner", request.getChildElementNamespace());
        assertEquals("pubsub", request.getChildElementName());

        XmlPullParser parser = TestUtils.getIQParser(request.toXML().toString());
        PubSub pubsubResult = (PubSub) PacketParserUtils.parseIQ(parser);
        SubscriptionsExtension subElem = pubsubResult.getExtension(PubSubElementType.SUBSCRIPTIONS_OWNER);
        List<Subscription> subscriptions = subElem.getSubscriptions();
        assertEquals(2, subscriptions.size());

        Subscription sub1 = subscriptions.get(0);
        assertEquals("romeo@montague.org", sub1.getJid().toString());
        assertEquals(Subscription.State.subscribed, sub1.getState());

        Subscription sub2 = subscriptions.get(1);
        assertEquals("juliet@capulet.org", sub2.getJid().toString());
        assertEquals(Subscription.State.none, sub2.getState());
    }

    @Test
    public void getAffiliationsAsOwnerTest() throws InterruptedException, SmackException, IOException, XMPPException, Exception {
        Protocol protocol = new Protocol();
        XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, JidTestUtil.FULL_JID_1_RESOURCE_1);

        PubSubManager mgr = new PubSubManager(connection, JidTestUtil.PUBSUB_EXAMPLE_ORG);
        Node testNode = new LeafNode(mgr, "princely_musings");

        List<Affiliation> affiliations = Arrays.asList(
            new Affiliation(JidTestUtil.BARE_JID_1, Affiliation.Type.member),
            new Affiliation(JidTestUtil.BARE_JID_2, Affiliation.Type.publisher)
        );
        AffiliationsExtension affiliationsExtension = new AffiliationsExtension(AffiliationNamespace.owner, affiliations);
        PubSub response = new PubSub(JidTestUtil.PUBSUB_EXAMPLE_ORG, Type.result, PubSubNamespace.owner);
        response.addExtension(affiliationsExtension);
        protocol.addResponse(response);

        List<Affiliation> returnedAffiliations = testNode.getAffiliationsAsOwner();

        PubSub request = (PubSub) protocol.getRequests().get(0);
        assertEquals("http://jabber.org/protocol/pubsub#owner", request.getChildElementNamespace());
        assertEquals("pubsub", request.getChildElementName());

        Affiliation affiliationOne = returnedAffiliations.get(0);
        assertEquals(affiliationOne.getJid(), JidTestUtil.BARE_JID_1);
        assertEquals(affiliationOne.getAffiliation(), Affiliation.Type.member);

        Affiliation affiliationTwo = returnedAffiliations.get(1);
        assertEquals(affiliationTwo.getJid(), JidTestUtil.BARE_JID_2);
        assertEquals(affiliationTwo.getAffiliation(), Affiliation.Type.publisher);
    }
}
