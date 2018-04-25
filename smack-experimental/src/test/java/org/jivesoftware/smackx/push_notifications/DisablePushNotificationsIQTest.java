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

import org.jivesoftware.smackx.push_notifications.element.DisablePushNotificationsIQ;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class DisablePushNotificationsIQTest {

    private static final String disableAllNotificationsIQExample = "<iq id='x97' type='set'>"
            + "<disable xmlns='urn:xmpp:push:0' jid='push-5.client.example'>" + "</disable>" + "</iq>";

    private static final String disableNodeNotificationsIQExample = "<iq id='x97' type='set'>"
            + "<disable xmlns='urn:xmpp:push:0' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>" + "</disable>"
            + "</iq>";

    @Test
    public void checkDisableAllPushNotificationsIQ() throws Exception {
        DisablePushNotificationsIQ disablePushNotificationsIQ = new DisablePushNotificationsIQ(
                JidCreate.from("push-5.client.example"));
        disablePushNotificationsIQ.setStanzaId("x97");
        Assert.assertEquals(disableAllNotificationsIQExample, disablePushNotificationsIQ.toXML(null).toString());
    }

    @Test
    public void checkDisableNodePushNotificationsIQ() throws Exception {
        DisablePushNotificationsIQ disablePushNotificationsIQ = new DisablePushNotificationsIQ(
                JidCreate.from("push-5.client.example"), "yxs32uqsflafdk3iuqo");
        disablePushNotificationsIQ.setStanzaId("x97");
        Assert.assertEquals(disableNodeNotificationsIQExample, disablePushNotificationsIQ.toXML(null).toString());
    }

}
