/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.push_notifications;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.push_notifications.element.PushNotificationsElements.RemoteDisablingExtension;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class RemoteDisablingPushNotificationsTest {

    String remoteDisablingExample = "<message from='push-5.client.example' to='user@example.com'>"
            + "<pubsub xmlns='http://jabber.org/protocol/pubsub' node='yxs32uqsflafdk3iuqo'>"
            + "<affiliation jid='user@example.com' affiliation='none' />" + "</pubsub>" + "</message>";

    String wrongRemoteDisabling1 = "<message from='push-5.client.example' to='user@example.com'>"
            + "<pubsub xmlns='http://jabber.org/protocol/pubsub' node='yxs32uqsflafdk3iuqo'>"
            + "<affiliation jid='user@example.com'/>" + "</pubsub>" + "</message>";

    String wrongRemoteDisabling2 = "<message from='push-5.client.example' to='user@example.com'>"
            + "<pubsub xmlns='http://jabber.org/protocol/pubsub' node='yxs32uqsflafdk3iuqo'>"
            + "<affiliation jid='user@example.com' affiliation='member' />" + "</pubsub>" + "</message>";

    String wrongRemoteDisabling3 = "<message from='push-5.client.example' to='user@example.com'>"
            + "<pubsub xmlns='http://jabber.org/protocol/pubsub'>" + "</pubsub>" + "</message>";

    @Test
    public void checkRemoteDisablingPushNotificationsParse() throws Exception {
        Message message = (Message) PacketParserUtils.parseStanza(remoteDisablingExample);
        RemoteDisablingExtension remoteDisablingExtension = RemoteDisablingExtension.from(message);

        Assert.assertEquals("yxs32uqsflafdk3iuqo", remoteDisablingExtension.getNode());
        Assert.assertEquals(JidCreate.from("user@example.com"), remoteDisablingExtension.getUserJid());
    }

    @Test
    public void checkWrongRemoteDisablighPushNotifications() throws Exception {
        Message message1 = (Message) PacketParserUtils.parseStanza(wrongRemoteDisabling1);
        RemoteDisablingExtension remoteDisablingExtension1 = RemoteDisablingExtension.from(message1);
        Assert.assertNull(remoteDisablingExtension1);

        Message message2 = (Message) PacketParserUtils.parseStanza(wrongRemoteDisabling1);
        RemoteDisablingExtension remoteDisablingExtension2 = RemoteDisablingExtension.from(message2);
        Assert.assertNull(remoteDisablingExtension2);

        Message message3 = (Message) PacketParserUtils.parseStanza(wrongRemoteDisabling1);
        RemoteDisablingExtension remoteDisablingExtension3 = RemoteDisablingExtension.from(message3);
        Assert.assertNull(remoteDisablingExtension3);
    }

}
