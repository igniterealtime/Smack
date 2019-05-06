/**
 *
 * Copyright 2014-2018 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub.provider;

import static org.jivesoftware.smack.test.util.CharSequenceEquals.equalsCharSequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.SubscriptionsExtension;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.junit.Test;

public class PubSubProviderTest {

    @Test
    public void subscriptionsOwnerResultTest() throws Exception {
        // @formatter:off
        final String resultStanza =
          "<iq from='pubsub.example.org' to='julia@example.org/Smack' id='HaT4m-13' type='result'>" +
            "<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'>" +
              "<subscriptions node='test'>" +
                "<subscription jid='foo@example.org/Smack' subscription='subscribed' subid='58C1A6F99F2A7'/>" +
                "<subscription jid='julia@example.org/Smack' subscription='subscribed' subid='58C18F8917321'/>" +
              "</subscriptions>" +
            "</pubsub>" +
          "</iq>";
        // @formatter:on
        XmlPullParser parser = TestUtils.getIQParser(resultStanza);
        PubSub pubsubResult = (PubSub) PacketParserUtils.parseIQ(parser);
        SubscriptionsExtension subElem = pubsubResult.getExtension(PubSubElementType.SUBSCRIPTIONS_OWNER);
        List<Subscription> subscriptions = subElem.getSubscriptions();
        assertEquals(2, subscriptions.size());

        Subscription sub1 = subscriptions.get(0);
        assertThat("foo@example.org/Smack", equalsCharSequence(sub1.getJid()));
        assertEquals(Subscription.State.subscribed, sub1.getState());
        assertEquals("58C1A6F99F2A7", sub1.getId());

        Subscription sub2 = subscriptions.get(1);
        assertThat("julia@example.org/Smack", equalsCharSequence(sub2.getJid()));
        assertEquals(Subscription.State.subscribed, sub2.getState());
        assertEquals("58C18F8917321", sub2.getId());
    }
}
