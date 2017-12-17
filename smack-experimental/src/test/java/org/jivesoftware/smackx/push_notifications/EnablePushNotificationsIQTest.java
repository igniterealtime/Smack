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

import java.util.HashMap;

import org.jivesoftware.smackx.push_notifications.element.EnablePushNotificationsIQ;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class EnablePushNotificationsIQTest {

    private static final String exampleEnableIQ = "<iq id='x42' type='set'>"
            + "<enable xmlns='urn:xmpp:push:0' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>" + "</enable>"
            + "</iq>";

    private static final String exampleEnableIQWithPublishOptions = "<iq id='x42' type='set'>"
            + "<enable xmlns='urn:xmpp:push:0' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>"
            + "<x xmlns='jabber:x:data' type='submit'>"
            + "<field var='FORM_TYPE'><value>http://jabber.org/protocol/pubsub#publish-options</value></field>"
            + "<field var='secret'><value>eruio234vzxc2kla-91</value></field>" + "</x>" + "</enable>" + "</iq>";

    @Test
    public void checkEnablePushNotificationsIQ() throws Exception {
        EnablePushNotificationsIQ enablePushNotificationsIQ = new EnablePushNotificationsIQ(
                JidCreate.from("push-5.client.example"), "yxs32uqsflafdk3iuqo");
        enablePushNotificationsIQ.setStanzaId("x42");
        Assert.assertEquals(exampleEnableIQ, enablePushNotificationsIQ.toXML().toString());
    }

    @Test
    public void checkEnablePushNotificationsIQWithPublishOptions() throws Exception {
        HashMap<String, String> publishOptions = new HashMap<>();
        publishOptions.put("secret", "eruio234vzxc2kla-91");

        EnablePushNotificationsIQ enablePushNotificationsIQ = new EnablePushNotificationsIQ(
                JidCreate.from("push-5.client.example"), "yxs32uqsflafdk3iuqo", publishOptions);
        enablePushNotificationsIQ.setStanzaId("x42");

        Assert.assertEquals(exampleEnableIQWithPublishOptions, enablePushNotificationsIQ.toXML().toString());
    }

}
